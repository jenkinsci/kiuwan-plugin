package com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult.ANALYSIS_STATUS_FINISHED;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getAgentBinDir;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getLocalAnalyzerCommandFilePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getMasks;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getRemoteFileAbsolutePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.readAnalysisResult;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.roundDouble;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanBuildSummaryAction;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteEnvironment;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerInstaller;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class KiuwanRunnable implements Runnable {
    
    private static final int KLA_RETURN_CODE_AUDIT_FAILED = 10;
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";
	
	private KiuwanRecorder recorder;
	private FilePath workspace;
	private KiuwanConnectionProfile connectionProfile;
	private Node node;
	private Run<?, ?> run;
	private Launcher launcher;
	private TaskListener listener;
	private AtomicReference<Throwable> exceptionReference;
	private AtomicReference<Result> resultReference;
	private KiuwanAnalyzerCommandBuilder commandBuilder;
	private PrintStream loggerPrintStream;
	
	public KiuwanRunnable(KiuwanRecorder recorder, FilePath workspace, KiuwanConnectionProfile connectionProfile,
			KiuwanGlobalConfigDescriptor descriptor, Node node, Run<?, ?> run, 
			Launcher launcher, TaskListener listener,
			AtomicReference<Throwable> exceptionReference, AtomicReference<Result> resultReference) {
		super();
		
		this.recorder = recorder;
		this.workspace = workspace;
		this.connectionProfile = connectionProfile;
		this.node = node;
		this.run = run;
		this.launcher = launcher;
		this.listener = listener;
		this.exceptionReference = exceptionReference;
		this.resultReference = resultReference;
		this.commandBuilder = new KiuwanAnalyzerCommandBuilder(
			recorder, workspace, connectionProfile, descriptor, run, launcher, listener);
		this.loggerPrintStream = listener.getLogger();
	}

	public void run() {
		
		// There is no need to check connection here (the plugin did in previous versions), 
		// we will handle any error that comes from KLA including connection errors
		try {
			if (connectionProfile == null) {
				String uuid = recorder.getConnectionProfileUuid();
				
				if (StringUtils.isEmpty(uuid)) {
					loggerPrintStream.println("A connection profile has not been set. Please set a connection profile before analyzing.");
					
				} else {
					loggerPrintStream.print("Could not find the specified connection profile (" + uuid + "). Verify your ");
					listener.hyperlink("/configure", "Kiuwan Global Settings");
					loggerPrintStream.println(".");
				}
				
				resultReference.set(Result.NOT_BUILT);
			
			} else {
				performAnalysis();
			}
		
		} catch (IOException e) {
			loggerPrintStream.println(e.toString());
			exceptionReference.set(e);
			resultReference.set(Result.NOT_BUILT);
			
		} catch (InterruptedException e) {
			loggerPrintStream.println("Analysis interrupted.");
			exceptionReference.set(e);
			resultReference.set(Result.ABORTED);
			
		} catch (Throwable throwable) {
			loggerPrintStream.println(ExceptionUtils.getFullStackTrace(throwable));
			resultReference.set(Result.NOT_BUILT);
		}
	}
	
	private void performAnalysis() throws IOException, InterruptedException {
		
		// 1 - Install Local Analyzer if not already installed
		FilePath localAnalyzerHome = installLocalAnalyzer();
		
		// 2 - Extract environment variables
		EnvVars envVars = buildEnvVars();
		
		// 3 - Print execution information to console
		printExecutionConfiguration(localAnalyzerHome);
		
		// 4 - Get the command line arguments to use
		List<String> args = commandBuilder.buildLocalAnalyzerCommand(localAnalyzerHome, envVars);
		
		// 5 - Execute Kiuwan Local Analyzer
		int klaReturnCode = runKiuwanLocalAnalyzer(args, envVars, localAnalyzerHome);
		
		// 6 - Read analysis results
		AnalysisResult analysisResult = loadAnalysisResults();
		
		// 8 - Process results
		if (klaReturnCode == 0 && analysisResult == null) {
			loggerPrintStream.println("Kiuwan Local Analyzer returned a success status code but " + 
				"the plugin could not read the analysis results file and check the remote status of the analysis");
			resultReference.set(Result.NOT_BUILT);
			
		} else {
			if (Mode.STANDARD_MODE.getValue().equals(recorder.getSelectedMode())) {
				onAnalysisFinishedStandardMode(klaReturnCode, analysisResult);
			
			} else if (Mode.DELIVERY_MODE.getValue().equals(recorder.getSelectedMode())) {
				onAnalysisFinishedDeliveryMode(klaReturnCode);
				
			} else if (Mode.EXPERT_MODE.getValue().equals(recorder.getSelectedMode())) {
				onAnalysisFinishedExpertMode(klaReturnCode);
			}

			// Add the results action to this run
			KiuwanBuildSummaryAction resultsSummaryAction = new KiuwanBuildSummaryAction(analysisResult);
			run.addAction(resultsSummaryAction);
		}
		
	}
	
	private FilePath installLocalAnalyzer() throws IOException, InterruptedException {
		FilePath nodeJenkinsDir = KiuwanUtils.getNodeJenkinsDirectory(node, workspace); 
		FilePath agentHome = KiuwanAnalyzerInstaller.installKiuwanLocalAnalyzer(nodeJenkinsDir, listener, connectionProfile);
		return agentHome;
	}
	
	private EnvVars buildEnvVars() throws IOException, InterruptedException {
		EnvVars environment = run.getEnvironment(listener);
		EnvVars envVars = new EnvVars(environment);
		
		if (run instanceof AbstractBuild) {
			AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
			Map<String, String> buildVariables = build.getBuildVariables();
			envVars.putAll(buildVariables);
		}
		
		// Just in case this job is running on a slave node that has not JAVA_HOME declared, avoid
		// passing the master's JAVA_HOME variable to KLA so the launch script can resolve the
		// java executable location by itself
		EnvVars remoteEnv = workspace.act(new KiuwanRemoteEnvironment());
		if (!remoteEnv.containsKey(JAVA_HOME_ENV_VAR)) {
			envVars.remove(JAVA_HOME_ENV_VAR);
		}
		envVars.putAll(remoteEnv);
		
		return envVars;
	}

	private void printExecutionConfiguration(FilePath agentHome) throws IOException, InterruptedException {
		FilePath script = getLocalAnalyzerCommandFilePath(launcher, agentHome);
		loggerPrintStream.println("Script: " + getRemoteFileAbsolutePath(script, listener));
		loggerPrintStream.println("Connection profile: " + connectionProfile.getDisplayName());
		
		if (Mode.STANDARD_MODE.getValue().equals(recorder.getSelectedMode())) {
			loggerPrintStream.println("Threshold measure: " + recorder.getMeasure());
			
			if (!Measure.NONE.getValue().equals(recorder.getMeasure())) {
				loggerPrintStream.println("Unstable threshold: " + recorder.getUnstableThreshold());
				loggerPrintStream.println("Failure threshold: " + recorder.getFailureThreshold());
			}
		}
	}
	
	private int runKiuwanLocalAnalyzer(List<String> args, EnvVars envVars, FilePath localAnalyzerHome)
			throws IOException, InterruptedException {
		
		FilePath localAnalyzerBinDir = getAgentBinDir(localAnalyzerHome);
		boolean[] masks = getMasks(args);
		
		ProcStarter procStarter = launcher.launch()
			.cmds(args)
			.masks(masks)
			.envs(envVars)
			.pwd(localAnalyzerBinDir)
			.stdout(listener);
		
		Proc process = procStarter.start();
		int klaReturnCode = process.join();
		loggerPrintStream.println("Result code: " + klaReturnCode);

		return klaReturnCode;
	}
	
	private AnalysisResult loadAnalysisResults() {
		AnalysisResult analysisResults = null;
		try {
			FilePath outputReportFilePath = new FilePath(workspace, recorder.getOutputFilename());
			if (outputReportFilePath != null && outputReportFilePath.exists()) {
				try (InputStream is = outputReportFilePath.read()) {
			  		analysisResults = readAnalysisResult(is);
			  	}
			} else {
				loggerPrintStream.println("Analysis results file not found: " + outputReportFilePath);
			}
		} catch (IOException | InterruptedException e) {
			loggerPrintStream.println("Could not read analysis results: " + e);
		}
		return analysisResults;
	}
	
	private void onAnalysisFinishedStandardMode(int klaReturnCode, AnalysisResult analysisResult) {
		if (klaReturnCode != 0) {
			loggerPrintStream.println("Kiuwan Local Analyzer has returned a failure status.");
			resultReference.set(Result.NOT_BUILT);
		
		} else if (!Measure.NONE.getValue().equals(recorder.getMeasure())) {
			if (ANALYSIS_STATUS_FINISHED.equalsIgnoreCase(analysisResult.getAnalysisStatus())) {
				Double qualityIndicator = null;
				if (analysisResult.getQualityIndicator() != null) {
					qualityIndicator = roundDouble(analysisResult.getQualityIndicator().getValue());
				}

				Double effortToTarget = null;
				if (analysisResult.getEffortToTarget() != null) {
					effortToTarget = roundDouble(analysisResult.getEffortToTarget().getValue());
				}
				
				Double riskIndex = null;
				if (analysisResult.getRiskIndex() != null) {
					riskIndex = roundDouble(analysisResult.getRiskIndex().getValue());
				}
					
				printStandardModeConsoleSummary(qualityIndicator, effortToTarget, riskIndex);
				checkThresholds(qualityIndicator, effortToTarget, riskIndex);
			
			// In any other case, an error happened when processing the results in Kiuwan
			// (note that in case of timeout the parent thread would kill this one, and
			// in case of being the KLA the one that returns an error, the previous branch of
			// the if statement would have been followed.
			} else {
				loggerPrintStream.println("Build failed in Kiuwan");
				resultReference.set(Result.NOT_BUILT);
			}
		}
	}
	
	private void onAnalysisFinishedDeliveryMode(int klaReturnCode) {
		if (recorder.getWaitForAuditResults_dm() && klaReturnCode == KLA_RETURN_CODE_AUDIT_FAILED) {
			String markBuildWhenNoPass = recorder.getMarkBuildWhenNoPass_dm();
			loggerPrintStream.println("Audit not passed. Marking build as " + markBuildWhenNoPass);
			resultReference.set(Result.fromString(markBuildWhenNoPass));
		
		} else if (klaReturnCode != 0) {
			loggerPrintStream.println("Kiuwan Local Analyzer has returned a failure status.");
			resultReference.set(Result.NOT_BUILT);
		}
	}
	
	private void onAnalysisFinishedExpertMode(int klaReturnCode) {
		Set<Integer> successCodes = parseErrorCodes(recorder.getSuccessResultCodes_em());
		Set<Integer> failureCodes = parseErrorCodes(recorder.getFailureResultCodes_em());
		Set<Integer> unstableCodes = parseErrorCodes(recorder.getUnstableResultCodes_em());
		Set<Integer> abortedCodes = parseErrorCodes(recorder.getAbortedResultCodes_em());
		Set<Integer> notBuiltCodes = parseErrorCodes(recorder.getNotBuiltResultCodes_em());

		if (successCodes.contains(klaReturnCode)) {
			// continue
		
		} else if (failureCodes.contains(klaReturnCode)) {
			resultReference.set(Result.FAILURE);
			
		} else if (unstableCodes.contains(klaReturnCode)) {
			resultReference.set(Result.UNSTABLE);
		
		} else if (abortedCodes.contains(klaReturnCode)) {
			resultReference.set(Result.ABORTED);
		
		} else if (notBuiltCodes.contains(klaReturnCode)) {
			resultReference.set(Result.NOT_BUILT);
		
		} else {
			String markAsInOtherCases = recorder.getMarkAsInOtherCases_em();
			loggerPrintStream.println("Kiuwan Local Analyzer has returned an exit code not configured: " + 
				klaReturnCode + ". Marking build as " + markAsInOtherCases + ".");
			resultReference.set(Result.fromString(markAsInOtherCases));
		}
	}
	
	private void printStandardModeConsoleSummary(Double qualityIndicator, Double effortToTarget, Double riskIndex) {
		loggerPrintStream.println("==========================================================================");
		loggerPrintStream.println("                    Kiuwan Static Analysis Summary                        ");
		loggerPrintStream.println("==========================================================================");
		loggerPrintStream.println(" - Global indicator: " + (qualityIndicator != null ? qualityIndicator : "unknown"));
		loggerPrintStream.println(" - Effort to target: " + (effortToTarget != null ? effortToTarget : "unknown"));
		loggerPrintStream.println(" - Risk index: " + (riskIndex != null ? riskIndex : "unknown"));
	}

	private void checkThresholds(Double qualityIndicator, Double effortToTarget, Double riskIndex) {
		String measure = recorder.getMeasure();

		if (Measure.QUALITY_INDICATOR.getValue().equals(measure)) {
			if (qualityIndicator == null) {
				loggerPrintStream.println("Global indicator value is unknown, cannot check configured value");
				
			} else if (qualityIndicator < recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Global indicator is lower than " + recorder.getFailureThreshold());
			
			} else if (qualityIndicator < recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Global indicator is lower than " + recorder.getUnstableThreshold());
			}
		
		} else if (Measure.EFFORT_TO_TARGET.getValue().equals(measure)) {
			if (effortToTarget == null) {
				loggerPrintStream.println("Effort to target value is unknown, cannot check configured value");
			
			} else if (effortToTarget > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Effort to target is greater than " + recorder.getFailureThreshold());
			
			} else if (effortToTarget > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Effort to target is greater than " + recorder.getUnstableThreshold());
			}
		
		} else if (Measure.RISK_INDEX.getValue().equals(measure)) {
			if (riskIndex == null) {
				loggerPrintStream.println("Risk index value is unknown, cannot check configured value");
			
			} else if (riskIndex > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Risk index is greater than " + recorder.getFailureThreshold());
			
			} else if (riskIndex > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Risk index is greater than " + recorder.getUnstableThreshold());
			}
		}
	}
	
}
