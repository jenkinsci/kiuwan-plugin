package com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getRemoteFileAbsolutePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.instantiateClient;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanBuildSummaryAction;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDownloadable;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteEnvironment;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanReportCI;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci.CiReport;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerCommandBuilder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanException;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.api.ApplicationApi;
import com.kiuwan.rest.client.model.ApplicationBean;

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
	
	public static final String AGENT_DIRECTORY = "tools/kiuwan";
    public static final String AGENT_HOME = "KiuwanLocalAnalyzer";
	
	private KiuwanDescriptor descriptor;
	private KiuwanRecorder recorder;
	private Node node;
	private AbstractBuild<?, ?> build;
	private Launcher launcher;
	private BuildListener listener;
	private AtomicReference<Result> resultReference;
	private AtomicReference<Throwable> exceptionReference;
	private KiuwanAnalyzerCommandBuilder commandBuilder;
	
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
		this.commandBuilder = new KiuwanAnalyzerCommandBuilder(descriptor, recorder);
	}

	public void run() {
		try {
			FormValidation connectionTestResult = descriptor.doTestConnection(
				descriptor.getUsername(), descriptor.getPassword(), 
				descriptor.isConfigureKiuwanURL(), descriptor.getKiuwanURL(),
				descriptor.isConfigureProxy(), descriptor.getProxyHost(),
				descriptor.getProxyPort(), descriptor.getProxyProtocol(),
				descriptor.getProxyAuthentication(), descriptor.getProxyUsername(),
				descriptor.getProxyPassword());

			if (Kind.OK.equals(connectionTestResult.kind)) {
				performScan(node, build, launcher, listener, resultReference);
				
			} else {
				listener.getLogger().print("Could not get authorization from Kiuwan. Verify your ");
				listener.hyperlink("/configure", "Kiuwan account settings");
				listener.getLogger().println(".");
				listener.getLogger().println(connectionTestResult.getMessage());
				resultReference.set(Result.NOT_BUILT);
			}
		
		} catch (KiuwanException e) {
			listener.getLogger().println(e.getMessage());
			listener.fatalError(e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			listener.getLogger().println(sw.toString());
			resultReference.set(Result.NOT_BUILT);
			exceptionReference.set(e);
			
		} catch (IOException e) {
			listener.getLogger().println(e.toString());
			exceptionReference.set(e);
			resultReference.set(Result.NOT_BUILT);
			
		} catch (InterruptedException e) {
			listener.getLogger().println("Analysis interrupted.");
			exceptionReference.set(e);
			resultReference.set(Result.ABORTED);
			
		} catch (Throwable throwable) {
			listener.getLogger().println(ExceptionUtils.getFullStackTrace(throwable));
			resultReference.set(Result.NOT_BUILT);
		}
	}
	
	private void performScan(Node node, AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener,
			AtomicReference<Result> resultReference) throws KiuwanException, IOException, InterruptedException {
		
		String name = null;
		String analysisLabel = null;
		String analysisEncoding = null;
		
		if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			name = recorder.getApplicationName_dm();
			analysisLabel = recorder.getLabel_dm();
			analysisEncoding = recorder.getEncoding_dm();

		} else if (Mode.CI_MODE.equals(recorder.getSelectedMode())) {
			name = recorder.getApplicationName_ci();
			analysisLabel = recorder.getLabel_ci();
			analysisEncoding = recorder.getEncoding_ci();

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

		EnvVars environment = build.getEnvironment(listener);
		
		FilePath workspace = build.getWorkspace();
		EnvVars remoteEnv = workspace.act(new KiuwanRemoteEnvironment());
		Map<String, String> builtVariables = build.getBuildVariables();

		EnvVars envVars = new EnvVars(environment);
		envVars.putAll(builtVariables);
		envVars.putAll(remoteEnv);

		FilePath jenkinsRootDir = null;
		FilePath rootPath = node.getRootPath();
		if (workspace.isRemote()) {
			jenkinsRootDir = new FilePath(workspace.getChannel(), rootPath.getRemote());
		} else {
			jenkinsRootDir = new FilePath(new File(rootPath.getRemote()));
		}
		
		String agentDirectory = KiuwanUtils.getPathFromConfiguredKiuwanURL(AGENT_DIRECTORY, descriptor);
		FilePath installDir = jenkinsRootDir.child(agentDirectory);
		FilePath agentHome = installDir.child(AGENT_HOME);
		
		String command = launcher.isUnix() ? "agent.sh" : "agent.cmd";
		FilePath agentBinDir = agentHome.child("bin");
		FilePath script = agentBinDir.child(command);
		
		final PrintStream loggerStream = listener.getLogger();
		if (agentHome.act(new KiuwanRemoteFilePath()) == null) {
			installLocalAnalyzer(jenkinsRootDir, listener);
		}

		if (Mode.CI_MODE.equals(recorder.getSelectedMode())) {
			// master's CI Report file located at $JENKINS_HOME/work/jobs/$JOBNAME/builds/#BUILD/kiuwan/ci_report.json
			File ciReportFile = new File(build.getRootDir(), "kiuwan/ci_report.json");
			CiReport ciReport = createCiReport(build, listener, envVars);
			KiuwanUtils.dumpCiReport(ciReport, ciReportFile);
			envVars.addLine(KiuwanAnalyzerCommandBuilder.KIUWAN_CI_BRANCH_ENV + "=" + ciReport.getBranch());
			KiuwanReportCI kiuwanReportCI = new KiuwanReportCI(ciReport);
			// slave's CI Report file located at $AGENT_HOME/temp/ci/$JOBNAME.#BUILD/ci_report.json
			String buildUniqueFolderName = build.getProject().getName() + '.' + build.getNumber();
			FilePath ciReportSlave = agentHome.child("temp/ci").child(buildUniqueFolderName).child("ci_report.json");
			ciReportSlave.act(kiuwanReportCI);
			envVars.addLine(KiuwanAnalyzerCommandBuilder.KIUWAN_CI_REPORT_ENV + "=" + ciReportSlave.getRemote());
		}

		if (launcher.isUnix()) {
			loggerStream.println("Changing " + command + " permission");
			agentBinDir.child("agent.sh").chmod(0755);
		}

		printExecutionConfiguration(listener, script);

		String analysisCode = null;
		int result = -1;

		List<String> args = commandBuilder.buildAgentCommand(launcher, name, analysisLabel, analysisEncoding, 
			srcFolder, command, agentBinDir, listener, envVars);

		boolean[] masks = new boolean[args.size()];
		boolean mask = false;
		for (int i = 1; i < masks.length; i++) {
			masks[i] = mask;
			if ("--user".equals(args.get(i)) || "--pass".equals(args.get(i))) {
				mask = true;
			} else if (args.get(i).contains("username") || args.get(i).contains("password")) {
				masks[i] = true;
			} else {
				mask = false;
			}
		}

		ProcStarter procStarter = launcher.launch().cmds(args).masks(masks);

		procStarter = procStarter.envs(envVars).readStdout().pwd(script.getParent());
		Proc process = procStarter.start();

		BufferedReader bufferedReader = null;
		Pattern pattern = Pattern.compile(".*Analysis created in Kiuwan with code: (.*)$");
		bufferedReader = new BufferedReader(new InputStreamReader(process.getStdout()));
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			Matcher matcher = pattern.matcher(line);
			boolean found = matcher.find();
			if (found) {
				analysisCode = matcher.group(1);
			}
			listener.getLogger().println(line);
		}
		result = process.join();
		loggerStream.println("Result code: "+result);
		
		if (analysisCode == null) {
			listener.getLogger().println("Could not retrieve analysis code.");
			resultReference.set(Result.NOT_BUILT);
			
		} else if (Mode.STANDARD_MODE.equals(recorder.getSelectedMode())) {
			if (result != 0) {
				listener.getLogger().println("Kiuwan Analyzer not finalized with success.");
				resultReference.set(Result.NOT_BUILT);
			} else {
				double qualityIndicator = -1d;
				double effortToTarget = -1d;
				double riskIndex = -1d;
				boolean buildFailedInKiuwan = false;

				boolean end = false;
				ApiClient client = instantiateClient(descriptor);
				ApplicationApi api = new ApplicationApi(client);
				int retries = 3;
				String analysisUrl = null;
				do {
					try {
						loggerStream.println("Query for result: " + analysisCode);
						ApplicationBean results = api.getAnalysis(analysisCode);
						loggerStream.println("Analysis status in Kiuwan: " + results.getAnalysisStatus());
						if ("FINISHED".equalsIgnoreCase(results.getAnalysisStatus())) {
							qualityIndicator = results.getQualityIndicator().getValue();
							effortToTarget = results.getEffortToTarget().getValue();
							BigDecimal rawRiskIndex = new BigDecimal(results.getRiskIndex().getValue());
							rawRiskIndex = rawRiskIndex.setScale(2, RoundingMode.HALF_UP);
							riskIndex = rawRiskIndex.doubleValue();
							analysisUrl = results.getAnalysisURL();
							end = true;
						} else if ("FINISHED_WITH_ERROR".equalsIgnoreCase(results.getAnalysisStatus())) {
							buildFailedInKiuwan = true;
							end = true;
						}
						
					} catch (ApiException e) {
						if (retries > 0) {
							// Re-initializes the client.
							client = instantiateClient(descriptor);
							retries--;
						} else {
							loggerStream.println("Could not get analysis results from Kiuwan: " + e);
							buildFailedInKiuwan = true;
							end = true;
						}
					}

					if (!end) {
						Thread.sleep(30000);
					}
				} while (!end);

				if (buildFailedInKiuwan) {
					loggerStream.println("Build failed in Kiuwan");
					resultReference.set(Result.NOT_BUILT);
				} else {
					printAnalysisSummary(listener, qualityIndicator, effortToTarget, riskIndex);
					checkThresholds(listener, qualityIndicator, effortToTarget, riskIndex, resultReference);
					KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(analysisUrl);
					build.addAction(link);
				}
			}
			
		} else if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			if (recorder.getWaitForAuditResults_dm()) {
				if (result == 10) {// Audit not passed
					String markBuildWhenNoPass = recorder.getMarkBuildWhenNoPass_dm();
					listener.getLogger().println("Audit not passed. Marking build as " + markBuildWhenNoPass);
					resultReference.set(Result.fromString(markBuildWhenNoPass));
				}
			}

			String auditResultURL = getAuditResultURL(descriptor, analysisCode);
			if (auditResultURL != null) {
				addLink(build, auditResultURL);
			}
			
		} else if (Mode.CI_MODE.equals(recorder.getSelectedMode())) {
			if (recorder.getWaitForAuditResults_ci()) {
				if (result == 10) {// Audit not passed
					String markBuildWhenNoPass = recorder.getMarkBuildWhenNoPass_ci();
					listener.getLogger().println("Audit not passed. Marking build as " + markBuildWhenNoPass);
					resultReference.set(Result.fromString(markBuildWhenNoPass));
				}
			}

			String auditResultURL = getAuditResultURL(descriptor, analysisCode);
			if (auditResultURL != null) {
				addLink(build, auditResultURL);
			}
			
		} else if (Mode.EXPERT_MODE.equals(recorder.getSelectedMode())) {
			Set<Integer> successCodes = parseErrorCodes(recorder.getSuccessResultCodes_em());
			Set<Integer> failureCodes = parseErrorCodes(recorder.getFailureResultCodes_em());
			Set<Integer> unstableCodes = parseErrorCodes(recorder.getUnstableResultCodes_em());
			Set<Integer> abortedCodes = parseErrorCodes(recorder.getAbortedResultCodes_em());
			Set<Integer> notBuiltCodes = parseErrorCodes(recorder.getNotBuiltResultCodes_em());

			if (successCodes.contains(result)) {
				// continue
			} else if (failureCodes.contains(result)) {
				resultReference.set(Result.FAILURE);
			} else if (unstableCodes.contains(result)) {
				resultReference.set(Result.UNSTABLE);
			} else if (abortedCodes.contains(result)) {
				resultReference.set(Result.ABORTED);
			} else if (notBuiltCodes.contains(result)) {
				resultReference.set(Result.NOT_BUILT);
			} else {
				String markAsInOtherCases = recorder.getMarkAsInOtherCases_em();
				loggerStream.println("Not configured result code received: " + result + 
					". Marking build as " + markAsInOtherCases + ".");
				resultReference.set(Result.fromString(markAsInOtherCases));
			}

			String auditResultURL = getAuditResultURL(descriptor, analysisCode);

			if (auditResultURL != null) {
				addLink(build, auditResultURL);
			} else {
				String analysisURL = getAnalysisURL(descriptor, analysisCode);
				if (analysisURL != null) {
					addLink(build, analysisURL);
				} else {
					addDefaultAnalysisLink(build, name, analysisLabel);
				}
			}
			
		} else {
			addDefaultAnalysisLink(build, name, analysisLabel);
		}
	}

	/**
	 * Extract CI Tool and SCM info from input parameters (only GIT supported for now), create model bean and return it.
	 */
	private CiReport createCiReport(AbstractBuild<?, ?> build, BuildListener listener, EnvVars envVars) {
		// FIXME rellenar CiReport, eliminar lineas debug de pobres al terminar
		listener.getLogger().println("--- KIUWAN --- Build change set is: " + build.getChangeSet());
		envVars.forEach((k, v) -> {
			listener.getLogger().println("--- KIUWAN --- EnvVar key '" + k + ", value '" + v + "'");
		});

		// Create and populate CI report
		CiReport report = new CiReport();
		String branchWithOrigin = envVars.get("GIT_BRANCH");
		if (StringUtils.isBlank(branchWithOrigin)) {
			throw new IllegalStateException("CI mode selected but env variable 'GIT_BRANCH' not found");
		}
		int indexOfSlash = branchWithOrigin.indexOf('/');
		report.setBranch(branchWithOrigin.substring(indexOfSlash + 1, branchWithOrigin.length()));

		return report;
	}

	private void addLink(AbstractBuild<?, ?> build, String url) {
		KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(url);
		build.addAction(link);
	}

	private void addDefaultAnalysisLink(AbstractBuild<?, ?> build, String name, String analysisLabel) {
		KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(buildKiuwanResultUrl(name, analysisLabel));
		build.addAction(link);
	}

	private String getAuditResultURL(KiuwanDescriptor descriptor, String analysisCode) {
		ApiClient client = instantiateClient(descriptor);
		ApplicationApi api = new ApplicationApi(client);

		int retries = 3;
		String auditResultURL = null;
		do {
			try {
				ApplicationBean results = api.getAnalysis(analysisCode);
				auditResultURL = results.getAuditResultURL();
				retries = 0;
			
			} catch (ApiException e) {
				retries--;
			}
			
		} while (retries > 0);
		return auditResultURL;
	}

	private String getAnalysisURL(KiuwanDescriptor descriptor, String analysisCode) {
		ApiClient client = instantiateClient(descriptor);
		ApplicationApi api = new ApplicationApi(client);

		int retries = 3;
		String analysisReslultURL = null;
		do {
			try {
				ApplicationBean results = api.getAnalysis(analysisCode);
				analysisReslultURL = results.getAnalysisURL();
				retries = 0;
			
			} catch (ApiException e) {
				if (retries > 0) {
					client = instantiateClient(descriptor);
				}
				retries--;
			}
		} while (retries > 0);
		return analysisReslultURL;
	}
	
	private void printExecutionConfiguration(BuildListener listener, FilePath script) throws IOException, InterruptedException {
		listener.getLogger().println("Script: " + getRemoteFileAbsolutePath(script, listener));
		
		if (Mode.STANDARD_MODE.equals(recorder.getSelectedMode())) {
			listener.getLogger().println("Threshold measure: " + recorder.getMeasure());
			listener.getLogger().println("Unstable threshold: " + recorder.getUnstableThreshold());
			listener.getLogger().println("Failure threshold: " + recorder.getFailureThreshold());
		}
	}

	private void checkThresholds(BuildListener listener, double qualityIndicator,
	        double effortToTarget, double riskIndex, AtomicReference<Result> resultReference) {
	    
		String measure = recorder.getMeasure();
		if (Measure.QUALITY_INDICATOR.name().equals(measure)) {
			if (qualityIndicator < recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Global indicator is lower than " + recorder.getFailureThreshold());
			} else if (qualityIndicator < recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Global indicator is lower than " + recorder.getUnstableThreshold());
			}
		} else if (Measure.EFFORT_TO_TARGET.name().equals(measure)) {
			if (effortToTarget > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Effort to target is greater than " + recorder.getFailureThreshold());
			} else if (effortToTarget > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Effort to target is greater than " + recorder.getUnstableThreshold());
			}
		} else if (Measure.RISK_INDEX.name().equals(measure)) {
			if (riskIndex > recorder.getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Risk index is greater than " + recorder.getFailureThreshold());
			} else if (riskIndex > recorder.getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Risk index is greater than " + recorder.getUnstableThreshold());
			}
		}
	}

	private void printAnalysisSummary(BuildListener listener, double qualityIndicator, double effortToTarget, double riskIndex) {
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println("                    Kiuwan Static Analysis Summary                        ");
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println(" - Global indicator: " + qualityIndicator);
		listener.getLogger().println(" - Effort to target: " + effortToTarget);
		listener.getLogger().println(" - Risk index: " + riskIndex);
		listener.getLogger().println();
	}

	private String buildKiuwanResultUrl(String applicationName, String analysisLabel) {
		return descriptor.getKiuwanURL() + "/application?app=" + applicationName + "&label=" + analysisLabel;
	}

	private void installLocalAnalyzer(FilePath root, BuildListener listener) throws IOException, InterruptedException {
		KiuwanDownloadable kiuwanDownloadable = new KiuwanDownloadable();
		String installDir = KiuwanUtils.getPathFromConfiguredKiuwanURL(KiuwanRunnable.AGENT_DIRECTORY, descriptor);
		FilePath remoteDir = root.child(installDir);
		listener.getLogger().println("Installing KiuwanLocalAnalyzer in " + remoteDir);
		File zip = kiuwanDownloadable.resolve(listener, descriptor);
		remoteDir.mkdirs();
		new FilePath(zip).unzip(remoteDir);
	}

}
