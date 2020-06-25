package com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult.ANALYSIS_STATUS_FINISHED;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getAgentBinDir;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getLocalAnalyzerCommandFilePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder.getMasks;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getOutputFile;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getRemoteFileAbsolutePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.readAnalysisResult;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.roundDouble;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDownloadable;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.action.KiuwanBuildSummaryAction;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.action.KiuwanBuildSummaryView;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteEnvironment;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanException;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Result;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

public class KiuwanRunnable implements Runnable {
	
	public static final String LOCAL_ANALYZER_PARENT_DIRECTORY = "tools/kiuwan";
    public static final String LOCAL_ANALYZER_DIRECTORY = "KiuwanLocalAnalyzer";
    
    private static final int KLA_RETURN_CODE_AUDIT_FAILED = 10;
	
	private KiuwanDescriptor descriptor;
	private KiuwanRecorder recorder;
	private Node node;
	private AbstractBuild<?, ?> build;
	private Launcher launcher;
	private BuildListener listener;
	private AtomicReference<Result> resultReference;
	private AtomicReference<Throwable> exceptionReference;
	private KiuwanAnalyzerCommandBuilder commandBuilder;
	private PrintStream loggerPrintStream;
	
	public KiuwanRunnable(KiuwanDescriptor descriptor, KiuwanRecorder recorder, 
			Node node, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener,
			AtomicReference<Result> resultReference, AtomicReference<Throwable> exceptionReference) {
		super();
		
		this.descriptor = descriptor;
		this.recorder = recorder;
		this.node = node;
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		this.resultReference = resultReference;
		this.exceptionReference = exceptionReference;
		this.commandBuilder = new KiuwanAnalyzerCommandBuilder(descriptor, recorder, build);
		this.loggerPrintStream = listener.getLogger();
	}

	public void run() {
		try {
			FormValidation connectionTestResult = descriptor.doTestConnection(
				descriptor.getUsername(), descriptor.getPassword(), descriptor.getDomain(), 
				descriptor.isConfigureKiuwanURL(), descriptor.getKiuwanURL(),
				descriptor.isConfigureProxy(), descriptor.getProxyHost(),
				descriptor.getProxyPort(), descriptor.getProxyProtocol(),
				descriptor.getProxyAuthentication(), descriptor.getProxyUsername(),
				descriptor.getProxyPassword());

			if (Kind.OK.equals(connectionTestResult.kind)) {
				performAnalysis();
				
			} else {
				loggerPrintStream.print("Could not get authorization from Kiuwan. Verify your ");
				listener.hyperlink("/configure", "Kiuwan account settings");
				loggerPrintStream.println(".");
				loggerPrintStream.println(connectionTestResult.getMessage());
				resultReference.set(Result.NOT_BUILT);
			}
		
		} catch (KiuwanException e) {
			loggerPrintStream.println(e.getMessage());
			listener.fatalError(e.getMessage());
			// TODO: check this
			/*
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			loggerPrintStream.println(sw.toString());
			 */
			e.printStackTrace(loggerPrintStream);
			resultReference.set(Result.NOT_BUILT);
			exceptionReference.set(e);
			
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
	
	private void performAnalysis() throws KiuwanException, IOException, InterruptedException {
		
		// 1 - Install Local Analyzer if not already installed
		FilePath localAnalyzerHome = installLocalAnalyzer();
		
		// 2 - Extract environment variables
		EnvVars envVars = buildEnvVars();
		
		// 3 - Print execution information to console
		printExecutionConfiguration(localAnalyzerHome);
		
		// 4 - Get the command line arguments to use
		List<String> args = buildLocalAnalyzerCommand(localAnalyzerHome, envVars);
		
		// 5 - Execute Kiuwan Local Analyzer
		int klaReturnCode = runKiuwanLocalAnalyzer(args, envVars, localAnalyzerHome);
		
		// 6 - Read analysis results
		AnalysisResult analysisResult = loadAnalysisResults();
		
		// 7 - Process results
		String analysisCode = analysisResult != null ? analysisResult.getAnalysisCode() : null;
		if (analysisCode == null) {
			onNoCodeAnalysisFinished();
			
		} else if (Mode.STANDARD_MODE.equals(recorder.getSelectedMode())) {
			onAnalysisFinishedStandardMode(klaReturnCode, analysisResult);
		
		} else if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			onAnalysisFinishedDeliveryMode(klaReturnCode, analysisResult);
			
		} else if (Mode.EXPERT_MODE.equals(recorder.getSelectedMode())) {
			onAnalysisFinishedExpertMode(klaReturnCode, analysisResult);
			
		} 
		/* else {
			onAnalysisFinishedFallback();
		} */
		
		KiuwanBuildSummaryView summaryView = new KiuwanBuildSummaryView(analysisResult);
		KiuwanBuildSummaryAction resultsSummaryAction = new KiuwanBuildSummaryAction(summaryView);
		build.addAction(resultsSummaryAction);
	}
	
	private FilePath installLocalAnalyzer() throws IOException, InterruptedException {
		FilePath rootPath = node.getRootPath();
		FilePath workspace = build.getWorkspace();
		
		FilePath jenkinsRootDir = null;
		if (workspace.isRemote()) {
			jenkinsRootDir = new FilePath(workspace.getChannel(), rootPath.getRemote());
		} else {
			jenkinsRootDir = new FilePath(new File(rootPath.getRemote()));
		}
		
		String agentDirectory = KiuwanUtils.getPathFromConfiguredKiuwanURL(LOCAL_ANALYZER_PARENT_DIRECTORY, descriptor);
		FilePath installDir = jenkinsRootDir.child(agentDirectory);
		FilePath agentHome = installDir.child(LOCAL_ANALYZER_DIRECTORY);
		
		// If installDir does not exist, install KLA
		if (agentHome.act(new KiuwanRemoteFilePath()) == null) {
			KiuwanDownloadable kiuwanDownloadable = new KiuwanDownloadable();
			loggerPrintStream.println("Installing KiuwanLocalAnalyzer in " + installDir);
			File zip = kiuwanDownloadable.resolve(listener, descriptor);
			installDir.mkdirs();
			new FilePath(zip).unzip(installDir);
			
			FilePath agentBinDir = agentHome.child("bin");
			if (launcher.isUnix()) {
				loggerPrintStream.println("Changing agent.sh permission");
				agentBinDir.child("agent.sh").chmod(0755);
			}
		}
		
		return agentHome;
	}
	
	private EnvVars buildEnvVars() throws IOException, InterruptedException {
		EnvVars environment = build.getEnvironment(listener);
		EnvVars envVars = new EnvVars(environment);
		
		Map<String, String> buildVariables = build.getBuildVariables();
		envVars.putAll(buildVariables);

		EnvVars remoteEnv = build.getWorkspace().act(new KiuwanRemoteEnvironment());
		envVars.putAll(remoteEnv);
		
		return envVars;
	}

	private void printExecutionConfiguration(FilePath agentHome) throws IOException, InterruptedException {
		FilePath script = getLocalAnalyzerCommandFilePath(launcher, agentHome);
		loggerPrintStream.println("Script: " + getRemoteFileAbsolutePath(script, listener));
		
		if (Mode.STANDARD_MODE.equals(recorder.getSelectedMode())) {
			loggerPrintStream.println("Threshold measure: " + recorder.getMeasure());
			
			if (!Measure.NONE.name().equalsIgnoreCase(recorder.getMeasure())) {
				loggerPrintStream.println("Unstable threshold: " + recorder.getUnstableThreshold());
				loggerPrintStream.println("Failure threshold: " + recorder.getFailureThreshold());
			}
		}
	}
	
	private List<String> buildLocalAnalyzerCommand(FilePath localAnalyzerHome, EnvVars envVars) 
			throws IOException, InterruptedException {
		
		String name = null;
		String analysisLabel = null;
		String analysisEncoding = null;
		
		if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			name = recorder.getApplicationName_dm();
			analysisLabel = recorder.getLabel_dm();
			analysisEncoding = recorder.getEncoding_dm();
		
		} else {
			name = recorder.getApplicationName();
			analysisLabel = recorder.getLabel();
			analysisEncoding = recorder.getEncoding();
		}
		
		if (StringUtils.isEmpty(name)) {
			name = build.getProject().getName();
		}
			
		if (StringUtils.isEmpty(analysisLabel)) {
			analysisLabel = "#" + build.getNumber();
		}
				
		if (StringUtils.isEmpty(analysisEncoding)) {
			analysisEncoding = "UTF-8";
		}

		FilePath srcFolder = build.getModuleRoot();
		List<String> args = commandBuilder.buildLocalAnalyzerCommand(launcher, listener, 
			localAnalyzerHome, srcFolder, name, analysisLabel, analysisEncoding, envVars);
		
		return args;
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
		File outputReportFile = getOutputFile(build);
		if (outputReportFile != null && outputReportFile.exists() && outputReportFile.canRead()) {
			try (InputStream is = new FileInputStream(outputReportFile)) {
		  		analysisResults = readAnalysisResult(is);
		  	} catch (IOException e) {
		  		loggerPrintStream.println("Could not read analysis results: " + e);
		  	}
		}
		return analysisResults;
	}
	
	private void onNoCodeAnalysisFinished() {
		loggerPrintStream.println("Could not retrieve analysis code.");
		resultReference.set(Result.NOT_BUILT);
	}
	
	private void onAnalysisFinishedStandardMode(int klaReturnCode, AnalysisResult analysisResult) {
		if (klaReturnCode != 0) {
			loggerPrintStream.println("Kiuwan Analyzer not finalized with success.");
			resultReference.set(Result.NOT_BUILT);
		
		} else {
			// String analysisUrl = null;
			double qualityIndicator = -1d;
			double effortToTarget = -1d;
			double riskIndex = -1d;

			String analysisStatus = analysisResult != null ? analysisResult.getAnalysisStatus() : null;
			if (ANALYSIS_STATUS_FINISHED.equalsIgnoreCase(analysisStatus)) {
				
				qualityIndicator = roundDouble(analysisResult.getQualityIndicator().getValue());
				effortToTarget = roundDouble(analysisResult.getEffortToTarget().getValue());
				riskIndex = roundDouble(analysisResult.getRiskIndex().getValue());
				
				// TODO: is this still needed?
				printStandardModeConsoleSummary(qualityIndicator, effortToTarget, riskIndex);
				
				checkThresholds(qualityIndicator, effortToTarget, riskIndex);
				// addLink(analysisUrl);

			} else {
				loggerPrintStream.println("Build failed in Kiuwan");
				resultReference.set(Result.NOT_BUILT);
			}
		}
	}
	
	private void onAnalysisFinishedDeliveryMode(int klaReturnCode, AnalysisResult analysisResult) {
		if (recorder.getWaitForAuditResults_dm()) {
			if (klaReturnCode == KLA_RETURN_CODE_AUDIT_FAILED) {
				String markBuildWhenNoPass = recorder.getMarkBuildWhenNoPass_dm();
				loggerPrintStream.println("Audit not passed. Marking build as " + markBuildWhenNoPass);
				resultReference.set(Result.fromString(markBuildWhenNoPass));
			}
		}

		/*
		String auditResultURL = analysisResult != null ? analysisResult.getAuditResultURL() : null;
		if (auditResultURL != null) {
			addLink(auditResultURL);
		}
		*/
	}
	
	private void onAnalysisFinishedExpertMode(int klaReturnCode, AnalysisResult analysisResult) {
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

		/*
		String auditResultURL = analysisResult != null ? analysisResult.getAuditResultURL() : null;
		
		if (auditResultURL != null) {
			addLink(auditResultURL);
			
		} else {
			String analysisURL = analysisResult != null ? analysisResult.getAnalysisURL() : null;
			if (analysisURL != null) {
				addLink(analysisURL);

			} else {
				addLink(buildKiuwanResultUrl(name, analysisLabel));
			}
		}
		*/
	}
	
	/*
	private void onAnalysisFinishedFallback() {
		addLink(buildKiuwanResultUrl(name, analysisLabel));
	}
	*/
	
	private void printStandardModeConsoleSummary(double qualityIndicator, double effortToTarget, double riskIndex) {
		loggerPrintStream.println("==========================================================================");
		loggerPrintStream.println("                    Kiuwan Static Analysis Summary                        ");
		loggerPrintStream.println("==========================================================================");
		loggerPrintStream.println(" - Global indicator: " + qualityIndicator);
		loggerPrintStream.println(" - Effort to target: " + effortToTarget);
		loggerPrintStream.println(" - Risk index: " + riskIndex);
		loggerPrintStream.println();
	}

	/*
	private void addLink(String url) {
		KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(url);
		build.addAction(link);
	}

	private String buildKiuwanResultUrl(String applicationName, String analysisLabel) {
		return descriptor.getKiuwanURL() + "/application?app=" + applicationName + "&label=" + analysisLabel;
	}
	*/
	
	private void checkThresholds(double qualityIndicator, double effortToTarget, double riskIndex) {
		String measure = recorder.getMeasure();

		if (Measure.QUALITY_INDICATOR.name().equals(measure)) {
			if (qualityIndicator < recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Global indicator is lower than " + recorder.getFailureThreshold());
			} else if (qualityIndicator < recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Global indicator is lower than " + recorder.getUnstableThreshold());
			}
		} else if (Measure.EFFORT_TO_TARGET.name().equals(measure)) {
			if (effortToTarget > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Effort to target is greater than " + recorder.getFailureThreshold());
			} else if (effortToTarget > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Effort to target is greater than " + recorder.getUnstableThreshold());
			}
		} else if (Measure.RISK_INDEX.name().equals(measure)) {
			if (riskIndex > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				loggerPrintStream.println("Risk index is greater than " + recorder.getFailureThreshold());
			} else if (riskIndex > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				loggerPrintStream.println("Risk index is greater than " + recorder.getUnstableThreshold());
			}
		}
	}
	
}
