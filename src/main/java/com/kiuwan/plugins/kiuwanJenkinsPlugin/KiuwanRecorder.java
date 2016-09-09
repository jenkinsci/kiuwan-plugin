package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.kiuwan.client.KiuwanClientException;
import com.kiuwan.client.KiuwanRestApiClient;
import com.kiuwan.client.model.ApplicationResults;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONObject;

public class KiuwanRecorder extends Recorder {

	private static final String PROXY_PASSWORD = "proxy.password";

	private static final String PROXY_USERNAME = "proxy.username";

	private static final String PROXY_AUTHENTICATION = "proxy.authentication";

	private static final String PROXY_PROTOCOL = "proxy.protocol";

	private static final String PROXY_PORT = "proxy.port";

	private static final String PROXY_HOST = "proxy.host";

	private static final String AGENT_PROPERTIES_FILE_NAME = "agent.properties";

	private static final String AGENT_CONF_DIR_NAME = "conf";

	public static final String QUALITY_INDICATOR = "QUALITY_INDICATOR";

	public static final String EFFORT_TO_TARGET = "EFFORT_TO_TARGET";

	public static final String RISK_INDEX = "RISK_INDEX";

	public static final String PROXY_TYPE_HTTP = "http";

	public static final String PROXY_TYPE_SOCKS = "socks";

	public static final String PROXY_AUTHENTICATION_BASIC = "Basic";

	public static final String PROXY_AUTHENTICATION_NONE = "None";

	private final static Long TIMEOUT_MARGIN = 5000L;

	private final static String proxyHostRegExp = "^\\s*proxy\\.host\\s*=.*$";
	private final static String proxyPortRegExp = "^\\s*proxy\\.port\\s*=.*$";
	private final static String proxyProtocolRegExp = "^\\s*proxy\\.protocol\\s*=.*$";
	private final static String proxyAuthenticationRegExp = "^\\s*proxy\\.authentication\\s*=.*$";
	private final static String proxyUsernameRegExp = "^\\s*proxy\\.username\\s*=.*$";
	private final static String proxyPasswordRegExp = "^\\s*proxy\\.password\\s*=.*$";
	
	public final static Mode DEFAULT_MODE = Mode.STANDARD_MODE;
	
	private Mode selectedMode;

	private String applicationName;
	private String applicationName_dm;
	private String label;
	private String label_dm;
	private String encoding;
	private String encoding_dm;
	private String includes;
	private String includes_dm;
	private String excludes;
	private String excludes_dm;
	private Integer timeout;
	private Integer timeout_dm;
	private Integer timeout_em;
	private Boolean indicateLanguages;
	private String languages;
	private Boolean indicateLanguages_dm;
	private String languages_dm;
	private String measure;
	private Double unstableThreshold;
	private Double failureThreshold;
	private String changeRequest_dm;
	private String analysisScope_dm;
	private Boolean waitForAuditResults_dm;
	private String branch_dm;
	private String changeRequestStatus_dm;
	private String commandArgs_em;
	private String extraParameters_em;
	private String markBuildWhenNoPass_dm;
	private String successResultCodes_em;
	private String unstableResultCodes_em;
	private String failureResultCodes_em;
	private String notBuiltResultCodes_em;
	private String abortedResultCodes_em;
	private String markAsInOtherCases_em;
	
	@DataBoundConstructor
	public KiuwanRecorder(String mode, String applicationName, String label, String encoding, String includes, String excludes, Integer timeout, Boolean indicateLanguages, String languages, String measure, Double unstableThreshold, Double failureThreshold,
			String applicationName_dm, String label_dm, String encoding_dm, String includes_dm, String excludes_dm, Integer timeout_dm, Boolean indicateLanguages_dm, String languages_dm, String changeRequest_dm, String changeRequestStatus_dm, String branch_dm, String analysisScope_dm, Boolean waitForAuditResults_dm, String markBuildWhenNoPass_dm, 
			Integer timeout_em, String commandArgs_em, String extraParameters_em, String successResultCodes_em, String unstableResultCodes_em, String failureResultCodes_em, String notBuiltResultCodes_em, String abortedResultCodes_em, String markAsInOtherCases_em) {
		if(mode == null){
			mode = DEFAULT_MODE.name();
		}
		
		this.selectedMode = Mode.valueOf(mode);
		
		this.applicationName = applicationName;
		this.label = label;
		this.encoding = encoding;
		this.includes = includes;
		this.excludes = excludes;
		this.timeout = timeout;
		this.indicateLanguages = indicateLanguages;
		this.languages = languages;
		this.measure = measure;
		this.unstableThreshold = unstableThreshold;
		this.failureThreshold = failureThreshold;
		
		this.applicationName_dm = applicationName_dm;
		this.label_dm = label_dm;
		this.encoding_dm = encoding_dm;
		this.includes_dm = includes_dm;
		this.excludes_dm = excludes_dm;
		this.timeout_dm = timeout_dm;
		this.indicateLanguages_dm = indicateLanguages_dm;
		this.languages_dm = languages_dm;
		this.changeRequest_dm = changeRequest_dm;
		this.analysisScope_dm = analysisScope_dm;
		this.waitForAuditResults_dm = waitForAuditResults_dm;
		this.changeRequestStatus_dm = changeRequestStatus_dm;
		this.branch_dm = branch_dm;
		this.markBuildWhenNoPass_dm = markBuildWhenNoPass_dm;
		
		this.timeout_em = timeout_em;
		this.commandArgs_em = commandArgs_em;
		this.extraParameters_em = extraParameters_em;
		this.successResultCodes_em = successResultCodes_em;
		this.unstableResultCodes_em = unstableResultCodes_em;
		this.failureResultCodes_em = failureResultCodes_em;
		this.notBuiltResultCodes_em = notBuiltResultCodes_em;
		this.abortedResultCodes_em = abortedResultCodes_em;
		this.markAsInOtherCases_em = markAsInOtherCases_em;
		
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		String mode = null;
		if (this.selectedMode == null) {
			this.selectedMode = DEFAULT_MODE;
		}
		mode = this.selectedMode.name();

		return mode;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public String getLabel() {
		return this.label;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public String getIncludes() {
		return this.includes;
	}

	public String getExcludes() {
		return this.excludes;
	}

	public Integer getTimeout() {
		return this.timeout;
	}

	public Boolean getIndicateLanguages() {
		return indicateLanguages;
	}
	
	public String getLanguages() {
		return this.languages;
	}

	public String getMeasure() {
		return this.measure;
	}

	public Double getUnstableThreshold() {
		return this.unstableThreshold;
	}

	public Double getFailureThreshold() {
		return this.failureThreshold;
	}

	public Mode getSelectedMode() {
		return selectedMode;
	}

	public String getApplicationName_dm() {
		return applicationName_dm;
	}

	public String getLabel_dm() {
		return label_dm;
	}

	public String getEncoding_dm() {
		return encoding_dm;
	}

	public String getIncludes_dm() {
		return includes_dm;
	}

	public String getExcludes_dm() {
		return excludes_dm;
	}

	public Integer getTimeout_dm() {
		return timeout_dm;
	}

	public Integer getTimeout_em() {
		return timeout_em;
	}

	public Boolean getIndicateLanguages_dm() {
		return indicateLanguages_dm;
	}
	
	public String getLanguages_dm() {
		return languages_dm;
	}

	public String getChangeRequest_dm() {
		return changeRequest_dm;
	}

	public String getAnalysisScope_dm() {
		return analysisScope_dm;
	}

	public Boolean getWaitForAuditResults_dm() {
		return waitForAuditResults_dm;
	}

	public String getBranch_dm() {
		return branch_dm;
	}

	public String getChangeRequestStatus_dm() {
		return changeRequestStatus_dm;
	}

	public String getCommandArgs_em() {
		return commandArgs_em;
	}

	public String getExtraParameters_em() {
		return extraParameters_em;
	}

	public String getMarkBuildWhenNoPass_dm() {
		return markBuildWhenNoPass_dm;
	}

	public String getSuccessResultCodes_em() {
		return successResultCodes_em;
	}

	public String getUnstableResultCodes_em() {
		return unstableResultCodes_em;
	}

	public String getFailureResultCodes_em() {
		return failureResultCodes_em;
	}

	public String getNotBuiltResultCodes_em() {
		return notBuiltResultCodes_em;
	}

	public String getAbortedResultCodes_em() {
		return abortedResultCodes_em;
	}

	public String getMarkAsInOtherCases_em() {
		return markAsInOtherCases_em;
	}

	/**
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild,
	 *      hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		long startTime = System.currentTimeMillis();
		long endTime = startTime + TimeUnit.MILLISECONDS.convert(this.timeout, TimeUnit.MINUTES);

		AtomicReference<Throwable> exceptionReference = new AtomicReference<Throwable>();
		AtomicReference<Result> resultReference = new AtomicReference<Result>();
		Thread thread = createExecutionThread(build, launcher, listener, resultReference, exceptionReference);
		thread.start();

		long currentTime = System.currentTimeMillis();
		try {
			while (thread.isAlive() && currentTime < endTime) {
				TimeUnit.MILLISECONDS.sleep(TIMEOUT_MARGIN);
				currentTime = System.currentTimeMillis();
			}
		} catch (InterruptedException interruptedException) {
			if (thread.isAlive()) {
				thread.interrupt();
			}
			build.setResult(Result.ABORTED);
			throw interruptedException;
		}

		if (thread.isAlive()) {
			listener.getLogger().println("Aborted by timeout.");
			build.setResult(Result.ABORTED);
		}

		Throwable throwable = exceptionReference.get();
		if (throwable != null) {
			if (throwable instanceof InterruptedException) {
				throw (InterruptedException) throwable;
			} else if (throwable instanceof IOException) {
				throw (IOException) throwable;
			} else {
				build.setResult(Result.FAILURE);
			}
		}

		Result result = resultReference.get();
		if (result != null) {
			build.setResult(result);
		}

		return true;
	}

	public boolean isInMode(String mode) {
		return (getMode().equals(mode)) ? true : false;
	}

	private Thread createExecutionThread(final AbstractBuild<?, ?> build, final Launcher launcher,
			final BuildListener listener, final AtomicReference<Result> resultReference,
			final AtomicReference<Throwable> exceptionReference) {
		Runnable runnable = new Runnable() {

			public void run() {
				try {
					DescriptorImpl descriptor = getDescriptor();
					FormValidation connectionTestResult = descriptor.doTestConnection(descriptor.getUsername(),
							descriptor.getPassword(), descriptor.isConfigureProxy(), descriptor.getProxyHost(),
							descriptor.getProxyPort(), descriptor.getProxyProtocol(),
							descriptor.getProxyAuthentication(), descriptor.getProxyUsername(),
							descriptor.getProxyPassword());
					if (Kind.OK.equals(connectionTestResult.kind)) {
						performScan(build, launcher, listener, resultReference);
					} else {
						listener.getLogger().print("Could not get authorization from Kiuwan. Verify your ");
						listener.hyperlink("/configure", "Kiuwan account settings");
						listener.getLogger().println(".");
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
		};

		Thread thread = new Thread(runnable);
		return thread;
	}

	private void performScan(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener,
			AtomicReference<Result> resultReference) throws KiuwanException, IOException, InterruptedException {
		String name = null;
		String analysisLabel = null;
		String analysisEncoding = null;
		
		if (Mode.DELIVERY_MODE.equals(this.selectedMode)) {
			name = this.applicationName_dm;
			analysisLabel = this.label_dm;
			analysisEncoding = this.encoding_dm;
		}
		else{
			name = this.applicationName;
			analysisLabel = this.label;
			analysisEncoding = this.encoding;
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
		
		FilePath jenkinsHome = new FilePath(new File(environment.get("JENKINS_HOME")));
		
		FilePath remoteDir = jenkinsHome.child(KiuwanComputerListener.INSTALL_DIR);
		FilePath agentHome = remoteDir.child(KiuwanComputerListener.AGENT_HOME);
		if (!agentHome.exists()) {
			installLocalAnalyzer(jenkinsHome, listener);
		}

		DescriptorImpl descriptor = getDescriptor();

		String command = launcher.isUnix() ? "agent.sh" : "agent.cmd";
		FilePath agentBinDir = agentHome.child("bin");
		FilePath script = agentBinDir.child(command);

		EnvVars env = agentBinDir.act(new KiuwanRemoteEnvironment());
		env.overrideAll(build.getBuildVariables());

		final PrintStream loggerStream = listener.getLogger();

		if (launcher.isUnix()) {
			loggerStream.println("Changing " + command + " permission");
			agentBinDir.child("agent.sh").chmod(0755);
		}

		printExecutionConfiguration(listener, script);

		String analysisCode = null;
		int result = -1;

		List<String> args = buildAgentCommand(launcher, name, analysisLabel, analysisEncoding, srcFolder, command, agentBinDir, listener, descriptor);

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

		procStarter = procStarter.envs(env).readStdout().pwd(script.getParent());
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
		} else if (Mode.STANDARD_MODE.equals(this.selectedMode)) {
			if(result != 0){
				listener.getLogger().println("Kiuwan Analyzer not finalized with success.");
				resultReference.set(Result.NOT_BUILT);	
			}
			else{
				double qualityIndicator = -1d;
				double effortToTarget = -1d;
				double riskIndex = -1d;
				boolean buildFailedInKiuwan = false;
				
				boolean end = false;
				KiuwanRestApiClient client = instantiateClient(descriptor);
				int retries = 3;
				String analysisUrl = null;
				do {
					try {
						loggerStream.println("Query for result: " + analysisCode);
						ApplicationResults results = client.getApplicationResultsByAnalysisCode(analysisCode);
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
					} catch (KiuwanClientException e) {
						if (retries > 0) {
							// Re-initializes the client.
							client = instantiateClient(descriptor);
							retries--;
						} else {
							loggerStream.println(e.getMessage());
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
					checkThresholds(build, listener, qualityIndicator, effortToTarget, riskIndex, resultReference);
					KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(analysisUrl);
					build.addAction(link);
				}
			}
		} else if (Mode.DELIVERY_MODE.equals(this.selectedMode)) {
			if(this.waitForAuditResults_dm){
				if(result == 10){//Audit not passed
					String markBuildWhenNoPass = this.markBuildWhenNoPass_dm;
					listener.getLogger().println("Audit not passed. Marking build as "+markBuildWhenNoPass);
					resultReference.set(Result.fromString(markBuildWhenNoPass));
				}
			}
			
			String auditResultURL = getAuditResultURL(descriptor, analysisCode);
			if (auditResultURL != null) {
				addLink(build, auditResultURL);
			}
		} else if (Mode.EXPERT_MODE.equals(this.selectedMode)) {
			Set<Integer> successCodes = parseErrorCodes(this.successResultCodes_em);
			Set<Integer> failureCodes = parseErrorCodes(this.failureResultCodes_em);
			Set<Integer> unstableCodes = parseErrorCodes(this.unstableResultCodes_em);
			Set<Integer> abortedCodes = parseErrorCodes(this.abortedResultCodes_em);
			Set<Integer> notBuiltCodes = parseErrorCodes(this.notBuiltResultCodes_em);
			
			if(successCodes.contains(result)){
				//continue
			}
			else if(failureCodes.contains(result)){
				resultReference.set(Result.FAILURE);
			}
			else if(unstableCodes.contains(result)){
				resultReference.set(Result.UNSTABLE);
			}
			else if(abortedCodes.contains(result)){
				resultReference.set(Result.ABORTED);
			}
			else if(notBuiltCodes.contains(result)){
				resultReference.set(Result.NOT_BUILT);
			}
			else{
				String markAsInOtherCases = this.markAsInOtherCases_em;
				loggerStream.println("Not configured result code received: "+result+". Marking build as "+markAsInOtherCases+".");
				resultReference.set(Result.fromString(markAsInOtherCases));
			}
			
			String auditResultURL = getAuditResultURL(descriptor, analysisCode);
			if (auditResultURL != null) {
				addLink(build, auditResultURL);
			}
			else{
				String analysisURL = getAnalysisURL(descriptor, analysisCode);	
				if(analysisURL != null){
					addLink(build, auditResultURL);
				}
				else{
					addDefaultAnalysisLink(build, name, analysisLabel);
				}
			}
		}
		else {
			addDefaultAnalysisLink(build, name, analysisLabel);
		}
	}

	private void addLink(AbstractBuild<?, ?> build, String url) {
		KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(url);
		build.addAction(link);
	}

	private void addDefaultAnalysisLink(AbstractBuild<?, ?> build, String name, String analysisLabel) {
		KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(buildKiuwanResultUrl(name, analysisLabel));
		build.addAction(link);
	}

	private String getAuditResultURL(DescriptorImpl descriptor, String analysisCode) {
		KiuwanRestApiClient client = instantiateClient(descriptor);

		int retries = 3;
		String auditResultURL = null;
		do {
			try {
				ApplicationResults results = client.getApplicationResultsByAnalysisCode(analysisCode);
				auditResultURL = results.getAuditResultURL();
				retries = 0;
			} catch (KiuwanClientException e) {
				retries--;
			}
		} while (retries > 0);
		return auditResultURL;
	}

	private String getAnalysisURL(DescriptorImpl descriptor, String analysisCode) {
		KiuwanRestApiClient client = instantiateClient(descriptor);

		int retries = 3;
		String analysisReslultURL = null;
		do {
			try {
				ApplicationResults results = client.getApplicationResultsByAnalysisCode(analysisCode);
				analysisReslultURL = results.getAnalysisURL();
				retries = 0;
			} catch (KiuwanClientException e) {
				if (retries > 0) {
					client = instantiateClient(descriptor);
				}
				retries--;
			}
		} while (retries > 0);
		return analysisReslultURL;
	}
	
	private String getRemoteFileAbsolutePath(FilePath filePath, TaskListener listener)
			throws IOException, InterruptedException {
		String path = filePath.act(new KiuwanRemoteFilePath());
		if (path == null) {
			listener.fatalError("File: " + filePath + " not found.");
		}
		return path;
	}

	private void printExecutionConfiguration(BuildListener listener, FilePath script) throws IOException, InterruptedException {
		listener.getLogger().println("Script: " + getRemoteFileAbsolutePath(script, listener));
		
		if (Mode.STANDARD_MODE.equals(this.selectedMode)) {
			listener.getLogger().println("Threshold measure: " + getMeasure());
			listener.getLogger().println("Unstable threshold: " + getUnstableThreshold());
			listener.getLogger().println("Failure threshold: " + getFailureThreshold());
		}
	}

	private void checkThresholds(AbstractBuild<?, ?> build, BuildListener listener, double qualityIndicator,
			double effortToTarget, double riskIndex, AtomicReference<Result> resultReference) {
		if (QUALITY_INDICATOR.equalsIgnoreCase(getMeasure())) {
			if (qualityIndicator < getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Quality indicator is lower than " + getFailureThreshold());
			} else if (qualityIndicator < getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Quality indicator is lower than " + getUnstableThreshold());
			}
		} else if (EFFORT_TO_TARGET.equalsIgnoreCase(getMeasure())) {
			if (effortToTarget > getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Effort to target is greater than " + getFailureThreshold());
			} else if (effortToTarget > getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Effort to target is greater than " + getUnstableThreshold());
			}
		} else if (RISK_INDEX.equalsIgnoreCase(getMeasure())) {
			if (riskIndex > getFailureThreshold()) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Risk index is greater than " + getFailureThreshold());
			} else if (riskIndex > getUnstableThreshold()) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Risk index is greater than " + getUnstableThreshold());
			}
		}
	}

	private void printAnalysisSummary(BuildListener listener, double qualityIndicator, double effortToTarget,
			double riskIndex) {
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println("                    Kiuwan Static Analysis Summary                        ");
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println(" - Quality indicator: " + qualityIndicator);
		listener.getLogger().println(" - Effort to target: " + effortToTarget);
		listener.getLogger().println(" - Risk index: " + riskIndex);
		listener.getLogger().println();
	}

	private List<String> buildAgentCommand(Launcher launcher, String name, String analysisLabel,
			String analysisEncoding, FilePath srcFolder, String command, FilePath agentBinDir, TaskListener listener,
			DescriptorImpl descriptor) throws IOException, InterruptedException {
		
		Integer timeout = null;
		String includes = null;
		String excludes = null;
		String languages = null;
		
		if (Mode.DELIVERY_MODE.equals(this.selectedMode)) {
			timeout = this.timeout_dm;
			includes = this.includes_dm;
			excludes = this.excludes_dm;
			languages = this.languages_dm;
		}
		else if (Mode.EXPERT_MODE.equals(this.selectedMode)) {
			timeout = this.timeout_em;
		}
		else{
			timeout = this.timeout;
			includes = this.includes;
			excludes = this.excludes;
			languages = this.languages;
		}
		
		String timeoutAsString = Long.toString(TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES) - TIMEOUT_MARGIN);

		List<String> args = new ArrayList<String>();

		String commandAbsolutePath = getRemoteFileAbsolutePath(agentBinDir.child(command), listener);

		args.add(buildArgument(launcher, commandAbsolutePath));
		args.add("-s");
		args.add(buildArgument(launcher, getRemoteFileAbsolutePath(srcFolder, listener)));

		args.add("--user");
		args.add(buildArgument(launcher, descriptor.getUsername()));
		args.add("--pass");
		args.add(buildArgument(launcher, descriptor.getPassword()));

		if (Mode.STANDARD_MODE.equals(this.selectedMode)) {
			args.add("-n");
			args.add(buildArgument(launcher, name));
			args.add("-l");
			args.add(buildArgument(launcher, analysisLabel));
			args.add("-c");
		} else if (Mode.DELIVERY_MODE.equals(this.selectedMode)) {
			args.add("-n");
			args.add(buildArgument(launcher, name));
			args.add("-l");
			args.add(buildArgument(launcher, analysisLabel));
			args.add("-cr");
			args.add(buildArgument(launcher, this.changeRequest_dm));
			args.add("-as");
			String deliveryType = this.analysisScope_dm;
			if (DeliveryType.COMPLETE_DELIVERY.name().equals(deliveryType)) {
				deliveryType = "completeDelivery";
			} else if (DeliveryType.PARTIAL_DELIVERY.name().equals(deliveryType)) {
				deliveryType = "partialDelivery";
			}
			args.add(buildArgument(launcher, deliveryType));
			if(this.waitForAuditResults_dm){
				args.add("-wr");	
			}
			
			String branch = this.branch_dm;
			if(StringUtils.isNotBlank(branch)){
				args.add("-bn");
				args.add(buildArgument(launcher, branch));
			}
			
			String changeRequestStatus = this.changeRequestStatus_dm;
			if(StringUtils.isNotBlank(changeRequestStatus)){
				args.add("-crs");
				args.add(buildArgument(launcher, changeRequestStatus));				
			}
		} else if (Mode.EXPERT_MODE.equals(this.selectedMode)) {
			parseOptions(args, launcher);
			parseParameters(args, launcher);
		}

		args.add(buildAdditionalParameterExpression(launcher, "timeout", timeoutAsString));

		if (!Mode.EXPERT_MODE.equals(this.selectedMode)) {
			if (StringUtils.isNotBlank(includes)) {
				args.add(buildAdditionalParameterExpression(launcher, "include.patterns", includes));
			}
			if (StringUtils.isNotBlank(excludes)) {
				args.add(buildAdditionalParameterExpression(launcher, "exclude.patterns", excludes));
			}
			
			args.add(buildAdditionalParameterExpression(launcher, "encoding", analysisEncoding));
			if(
				(Mode.STANDARD_MODE.equals(this.selectedMode) && (this.indicateLanguages != null && this.indicateLanguages == true))
				|| 
				(Mode.DELIVERY_MODE.equals(this.selectedMode) && (this.indicateLanguages_dm != null && this.indicateLanguages_dm == true))
			){
				args.add(buildAdditionalParameterExpression(launcher, "supported.technologies", languages));
			}
		}

		String proxyHost = descriptor.getProxyHost();
		String proxyPort = Integer.toString(descriptor.getProxyPort());
		String proxyProtocol = descriptor.getProxyProtocol();
		String proxyAuthentication = PROXY_AUTHENTICATION_BASIC;
		String proxyUsername = descriptor.getProxyUsername();
		String proxyPassword = descriptor.getProxyPassword();
				
		if(!descriptor.isConfigureProxy()){
			proxyHost="";
			proxyAuthentication = PROXY_AUTHENTICATION_NONE;
		}
		else if(!PROXY_AUTHENTICATION_BASIC.equals(descriptor.getProxyAuthentication())){
			proxyAuthentication = PROXY_AUTHENTICATION_NONE;
			proxyUsername = "";
			proxyPassword = "";
		}
		
		args.add(buildAdditionalParameterExpression(launcher, PROXY_HOST, proxyHost));
		args.add(buildAdditionalParameterExpression(launcher, PROXY_PORT, proxyPort));
		args.add(buildAdditionalParameterExpression(launcher, PROXY_PROTOCOL, proxyProtocol));
		args.add(buildAdditionalParameterExpression(launcher, PROXY_AUTHENTICATION, proxyAuthentication));
		args.add(buildAdditionalParameterExpression(launcher, PROXY_USERNAME, proxyUsername));
		args.add(buildAdditionalParameterExpression(launcher, PROXY_PASSWORD, proxyPassword));
			
		writeProxyConfigToProperties(agentBinDir, proxyHost, proxyPort, proxyProtocol, proxyAuthentication,	proxyUsername, proxyPassword);
		
		return args;
	}

	private void writeProxyConfigToProperties(FilePath agentBinDir, String proxyHost, String proxyPort,
			String proxyProtocol, String proxyAuthentication, String proxyUsername, String proxyPassword)
			throws IOException, InterruptedException {
		FilePath agentPropertiesPath = agentBinDir.getParent().child(AGENT_CONF_DIR_NAME).child(AGENT_PROPERTIES_FILE_NAME);
		
		StringBuilder newFileContent = new StringBuilder();
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new InputStreamReader(agentPropertiesPath.read()));
			String line = null;
			while((line = reader.readLine()) != null){
				if(!Pattern.matches(proxyHostRegExp, line) 
					&& !Pattern.matches(proxyPortRegExp, line) 
					&& !Pattern.matches(proxyProtocolRegExp, line)
					&& !Pattern.matches(proxyAuthenticationRegExp, line)
					&& !Pattern.matches(proxyUsernameRegExp, line)
					&& !Pattern.matches(proxyPasswordRegExp, line)
				){
					newFileContent.append(line+"\n");
				}
			}
		}
		finally{
			IOUtils.closeQuietly(reader);
		}
		
		newFileContent.append(PROXY_HOST+"="+proxyHost+"\n");
		newFileContent.append(PROXY_PORT+"="+proxyPort+"\n");
		newFileContent.append(PROXY_PROTOCOL+"="+proxyProtocol+"\n");
		newFileContent.append(PROXY_AUTHENTICATION+"="+proxyAuthentication+"\n");
		newFileContent.append(PROXY_USERNAME+"="+proxyUsername+"\n");
		newFileContent.append(PROXY_PASSWORD+"="+proxyPassword+"\n");
		
		agentPropertiesPath.write(newFileContent.toString(), "UTF-8");
	}

	private void parseOptions(List<String> args, Launcher launcher) {
		String commandArgs = this.commandArgs_em;
		String escapedBackslash = null;
		do{
			escapedBackslash = "--"+new Random().nextInt()+"--";
		}while(commandArgs.contains(escapedBackslash));

		commandArgs = commandArgs.replaceAll("\\\\\\\\", escapedBackslash);
		
		String escapedDQuotes = null;
		do{
			escapedDQuotes = "--"+new Random().nextInt()+"--";
		}while(commandArgs.contains(escapedDQuotes));

		commandArgs = commandArgs.replaceAll("\\\\\"", escapedDQuotes);
		
		String escapedSpaces = null;
		do{
			escapedSpaces = "--"+new Random().nextInt()+"--";
		}while(commandArgs.contains(escapedSpaces));
		
		String dquotes = null;
		do{
			dquotes = "--"+new Random().nextInt()+"--";
		}while(commandArgs.contains(dquotes));
		
		Pattern compile = Pattern.compile("^([^\"]*)(\"[^\"]*\")(.*)$");
		Matcher matcher = compile.matcher(commandArgs);
		while(matcher.find()){
			commandArgs = matcher.group(1)+matcher.group(2).replaceAll(" ", escapedSpaces).replaceAll("\"", dquotes)+matcher.group(3);
			matcher = compile.matcher(commandArgs);
		}
		
		String[] split = commandArgs.split("\\s+");
		for (String token : split) {
			token = token.replaceAll(dquotes, "\"").replaceAll(escapedSpaces, " ");
			token = token.replaceAll(escapedDQuotes, "\"");
			token = token.replaceAll(escapedBackslash, "\\\\");
			
			Matcher doubleQuotedTokenMatcher = Pattern.compile("^\"(.*)\"$").matcher(token);
			if(doubleQuotedTokenMatcher.find()){
				token =	doubleQuotedTokenMatcher.group(1);
			}
			
			args.add(buildArgument(launcher, token));
		}
	}

	private String buildKiuwanResultUrl(String applicationName, String analysisLabel) {
		return "https://www.kiuwan.com/saas/application?app=" + applicationName + "&label=" + analysisLabel;
	}

	private void parseParameters(List<String> args, Launcher launcher) {
		String extraParams = this.extraParameters_em;
		Pattern equalWithSpaces = Pattern.compile("^(.*\\s*[^=\\s]+)\\s+=\\s+(\".*\".*)$");
		Matcher equalsWithSpacesMatcher = equalWithSpaces.matcher(extraParams);
		while(equalsWithSpacesMatcher.find()){
			extraParams = equalsWithSpacesMatcher.group(1)+"="+equalsWithSpacesMatcher.group(2);

			equalsWithSpacesMatcher = equalWithSpaces.matcher(extraParams);
		}
		
		String escapedBackslash = null;
		do{
			escapedBackslash = "--"+new Random().nextInt()+"--";
		}while(extraParams.contains(escapedBackslash));

		extraParams = extraParams.replaceAll("\\\\\\\\", escapedBackslash);
		
		String escapedDQuotes = null;
		do{
			escapedDQuotes = "--"+new Random().nextInt()+"--";
		}while(extraParams.contains(escapedDQuotes));

		extraParams = extraParams.replaceAll("\\\\\"", escapedDQuotes);
		
		String escapedSpaces = null;
		do{
			escapedSpaces = "--"+new Random().nextInt()+"--";
		}while(extraParams.contains(escapedSpaces));
		
		String dquotes = null;
		do{
			dquotes = "--"+new Random().nextInt()+"--";
		}while(extraParams.contains(dquotes));
		
		Pattern compile = Pattern.compile("^([^\"]*)(\"[^\"]*\")(.*)$");
		Matcher matcher = compile.matcher(extraParams);
		while(matcher.find()){
			extraParams = matcher.group(1)+matcher.group(2).replaceAll(" ", escapedSpaces).replaceAll("\"", dquotes)+matcher.group(3);
			matcher = compile.matcher(extraParams);
		}
		
		String[] split = extraParams.split("\\s+");
		for (String token : split) {
			token = token.replaceAll(dquotes, "\"").replaceAll(escapedSpaces, " ");
			token = token.replaceAll(escapedDQuotes, "\"");
			token = token.replaceAll(escapedBackslash, "\\\\");
			
			String[] paramValue = token.split("=", 2);
			if(paramValue.length == 2){
				String key = paramValue[0];
				String value = paramValue[1];
				Matcher doubleQuotedTokenMatcher = Pattern.compile("^\"(.*)\"$").matcher(value);
				if(doubleQuotedTokenMatcher.find()){
					value =	doubleQuotedTokenMatcher.group(1);
				}
				
				args.add(buildAdditionalParameterExpression(launcher, key, value));
			}
		}
	}

	private String buildArgument(Launcher launcher, String argument) {
		if (launcher.isUnix()) {
			return argument;
		} else {
			String backslashWithDQuote = null;
			do {
				backslashWithDQuote = "-" + Integer.toHexString(new Random().nextInt()) + "-";
			} while (argument.contains(backslashWithDQuote));
			argument = argument.replaceAll("\\\\\"", backslashWithDQuote);
			argument = argument.replaceAll(Pattern.quote("^"), "^^^^^^^^^");
			argument = argument.replaceAll("\"", "\\\\\"");	
			argument = argument.replaceAll(Pattern.quote(backslashWithDQuote), "\\\\\\\\\\\\\"");
			
			Matcher matcher = Pattern.compile("^.*[^\\\\]+(\\\\+)$").matcher(argument);
			if(matcher.find()){
				String group = matcher.group(1);
				argument += group; 
			}
			return "\"" + argument + "\"";
		}
	}

	private String buildAdditionalParameterExpression(Launcher launcher, String parameterName, String parameterValue) {
		String parameterExpression = "";
		if (launcher.isUnix()) {
			parameterExpression = parameterName + "=" + parameterValue;
		} else {
			String backslashWithDQuote = null;
			do {
				backslashWithDQuote = "-" + Integer.toHexString(new Random().nextInt()) + "-";
			} while (parameterValue.contains(backslashWithDQuote));
			parameterValue = parameterValue.replaceAll("\\\\\"", backslashWithDQuote);
			parameterValue = parameterValue.replaceAll(Pattern.quote("^"), "^^^^^^^^^");
			parameterValue = parameterValue.replaceAll("\"", "\\\\\"");
			parameterValue = parameterValue.replaceAll(Pattern.quote(backslashWithDQuote), "\\\\\\\\\\\\\"");

			Matcher matcher = Pattern.compile("^.*[^\\\\]+(\\\\+)$").matcher(parameterValue);
			if(matcher.find()){
				String group = matcher.group(1);
				parameterValue += group;
			}

			parameterExpression = "\""+parameterName + "=" + parameterValue + "\"";
		}
		return parameterExpression;
	}

	private void installLocalAnalyzer(FilePath root, BuildListener listener) throws IOException, InterruptedException {
		KiuwanDownloadable kiuwanDownloadable = new KiuwanDownloadable();
		FilePath remoteDir = root.child(KiuwanComputerListener.INSTALL_DIR);
		listener.getLogger().println("Installing KiuwanLocalAnalyzer in " + remoteDir);
		File zip = kiuwanDownloadable.resolve(null, null, listener);
		remoteDir.mkdirs();
		new FilePath(zip).unzip(remoteDir);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	static KiuwanRestApiClient instantiateClient(DescriptorImpl descriptor) {
		String username = descriptor.getUsername();
		String password = descriptor.getPassword();
		boolean configureProxy = descriptor.isConfigureProxy();
		String proxyHost = descriptor.getProxyHost(); 
		int proxyPort = descriptor.getProxyPort(); 
		String proxyProtocol = descriptor.getProxyProtocol(); 
		String proxyAuthentication = descriptor.getProxyAuthentication(); 
		String proxyUsername = descriptor.getProxyUsername();
		String proxyPassword = descriptor.getProxyPassword();
		
		KiuwanRestApiClient client = null;
		if (configureProxy && StringUtils.isNotBlank(proxyHost)) {
			Type proxyType = null;
			if (PROXY_TYPE_HTTP.equalsIgnoreCase(proxyProtocol)) {
				proxyType = Type.HTTP;
			} else if (PROXY_TYPE_SOCKS.equalsIgnoreCase(proxyProtocol)) {
				proxyType = Type.SOCKS;
			} else {
				throw new IllegalArgumentException("Unsupported proxy protocol found: " + proxyProtocol);
			}

			if (PROXY_AUTHENTICATION_BASIC.equals(proxyAuthentication)) {
				client = new KiuwanRestApiClient(username, password, proxyHost, proxyPort, proxyType, proxyUsername,
						proxyPassword);
			} else {
				// None authentication
				client = new KiuwanRestApiClient(username, password, proxyHost, proxyPort, proxyType);
			}
		} else {
			client = new KiuwanRestApiClient(username, password);
		}
		return client;
	}

	static Set<Integer> parseErrorCodes(String resultCodes) {
		Set<Integer> errorCodes = new HashSet<Integer>();
		if(resultCodes != null){
			String[] errorCodesAsString = resultCodes.split(",");
			for (String errorCodeAsString : errorCodesAsString) {
				if (StringUtils.isNotBlank(errorCodeAsString)) {
					errorCodes.add(Integer.parseInt(errorCodeAsString.trim()));
				}
			}
		}
		return errorCodes;
	}
	
	public enum Mode {
		STANDARD_MODE,
		DELIVERY_MODE,
		EXPERT_MODE;
	}
	
	public enum DeliveryType {
		COMPLETE_DELIVERY,
		PARTIAL_DELIVERY;
	}
	
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private final static String[] comboValues = { QUALITY_INDICATOR, RISK_INDEX, EFFORT_TO_TARGET };

		private final static String[] measureComboNames = { "Quality indicator", "Risk index", "Effort to target" };

		private final static String[] proxyProtocolComboValues = { PROXY_TYPE_HTTP, PROXY_TYPE_SOCKS };

		private final static String[] proxyAuthenticationTypeComboValues = { PROXY_AUTHENTICATION_NONE,
				PROXY_AUTHENTICATION_BASIC };

		private final static String[] buildResultComboValues = { Result.FAILURE.toString(), Result.UNSTABLE.toString(),
				Result.ABORTED.toString(), Result.NOT_BUILT.toString() };

		private final static String[] deliveryTypeComboNames = { "Complete delivery", "Partial delivery" };

		private final static String[] deliveryTypeComboValues = { DeliveryType.COMPLETE_DELIVERY.name(), DeliveryType.PARTIAL_DELIVERY.name() };

		private String username;

		private String password;

		private boolean configureProxy;

		private String proxyHost;

		private int proxyPort;

		private String proxyProtocol;

		private String proxyAuthentication;

		private String proxyUsername;

		private String proxyPassword;

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			// to persist global configuration information,
			// set that to properties and call save().
			String username = (String) json.get("username");
			String password = (String) json.get("password");
			Boolean configureProxy = (Boolean) json.get("configureProxy");
			String proxyHost = (String) json.get("proxyHost");
			int proxyPort = Integer.parseInt((String) json.get("proxyPort"));
			String proxyProtocol = (String) json.get("proxyProtocol");
			String proxyAuthentication = (String) json.get("proxyAuthentication");
			String proxyUsername = (String) json.get("proxyUsername");
			String proxyPassword = (String) json.get("proxyPassword");

			this.username = username;
			Secret secret = Secret.fromString(password);
			this.password = secret.getEncryptedValue();
			this.configureProxy = configureProxy;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
			this.proxyProtocol = proxyProtocol;
			this.proxyAuthentication = proxyAuthentication;
			this.proxyUsername = proxyUsername;
			this.proxyPassword = (proxyPassword == null) ? null : Secret.fromString(proxyPassword).getEncryptedValue();

			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Analyze your source code with Kiuwan!";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> item) {
			return true;
		}

		/**
		 * @return the username
		 */
		public String getUsername() {
			return this.username;
		}

		/**
		 * @return the password
		 */
		public String getPassword() {
			return Secret.toString(Secret.decrypt(this.password));
		}

		/**
		 * @return the configureProxy
		 */
		public boolean isConfigureProxy() {
			return this.configureProxy;
		}

		/**
		 * @return the proxyHost
		 */
		public String getProxyHost() {
			return this.proxyHost;
		}

		/**
		 * @return the proxyPort
		 */
		public int getProxyPort() {
			return this.proxyPort;
		}

		/**
		 * @return the proxyProtocol
		 */
		public String getProxyProtocol() {
			return this.proxyProtocol;
		}

		/**
		 * @return the proxyAuthentication
		 */
		public String getProxyAuthentication() {
			return this.proxyAuthentication;
		}

		/**
		 * @return the proxyUsername
		 */
		public String getProxyUsername() {
			return this.proxyUsername;
		}

		/**
		 * @return the proxyPassword
		 */
		public String getProxyPassword() {
			return (this.proxyPassword == null) ? null : Secret.toString(Secret.decrypt(this.proxyPassword));
		}

		public FormValidation doTestConnection(@QueryParameter String username, @QueryParameter String password,
				@QueryParameter boolean configureProxy, @QueryParameter String proxyHost, @QueryParameter int proxyPort,
				@QueryParameter String proxyProtocol, @QueryParameter String proxyAuthentication,
				@QueryParameter String proxyUsername, @QueryParameter String proxyPassword) {
			DescriptorImpl descriptor = new DescriptorImpl();
			descriptor.username = username;
			descriptor.password = Secret.fromString(password).getEncryptedValue();
			descriptor.configureProxy = configureProxy;
			descriptor.proxyHost = proxyHost;
			descriptor.proxyPort = proxyPort;
			descriptor.proxyProtocol = proxyProtocol;
			descriptor.proxyAuthentication = proxyAuthentication;
			descriptor.proxyUsername = proxyUsername;
			descriptor.proxyPassword = (proxyPassword != null)? Secret.fromString(proxyPassword).getEncryptedValue() : null;
			
			KiuwanRestApiClient client = KiuwanRecorder.instantiateClient(descriptor);
			try {
				client.getApplications();
				return FormValidation.ok("Authentication completed successfully!");
			} catch (KiuwanClientException kiuwanClientException) {
				return FormValidation.error("Authentication failed.");
			} catch (Throwable throwable) {
				return FormValidation
						.warning("Could not initiate the authentication process. Reason: " + throwable.getMessage());
			}
		}

		public ListBoxModel doFillAnalysisScope_dmItems(@QueryParameter("analysisScope_dm") String deliveryType) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < deliveryTypeComboValues.length; i++) {
				if (deliveryTypeComboValues[i].equalsIgnoreCase(deliveryType)) {
					items.add(new ListBoxModel.Option(deliveryTypeComboNames[i], deliveryTypeComboValues[i], true));
				} else {
					items.add(deliveryTypeComboNames[i], deliveryTypeComboValues[i]);
				}
			}

			return items;
		}

		public ListBoxModel doFillMeasureItems(@QueryParameter("measure") String measure) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < measureComboNames.length; i++) {
				if (comboValues[i].equalsIgnoreCase(measure)) {
					items.add(new ListBoxModel.Option(measureComboNames[i], comboValues[i], true));
				} else {
					items.add(measureComboNames[i], comboValues[i]);
				}
			}

			return items;
		}

		public ListBoxModel doFillMarkBuildWhenNoPass_dmItems(
				@QueryParameter("markBuildWhenNoPass_dm") String markBuildWhenNoPass) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < buildResultComboValues.length; i++) {
				if (buildResultComboValues[i].equalsIgnoreCase(markBuildWhenNoPass)) {
					items.add(new ListBoxModel.Option(buildResultComboValues[i], buildResultComboValues[i], true));
				} else {
					items.add(buildResultComboValues[i], buildResultComboValues[i]);
				}
			}

			return items;
		}
		
		public ListBoxModel doFillMarkAsInOtherCases_emItems(
				@QueryParameter("markAsInOtherCases_em") String markAsInOtherCases) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < buildResultComboValues.length; i++) {
				if (buildResultComboValues[i].equalsIgnoreCase(markAsInOtherCases)) {
					items.add(new ListBoxModel.Option(buildResultComboValues[i], buildResultComboValues[i], true));
				} else {
					items.add(buildResultComboValues[i], buildResultComboValues[i]);
				}
			}

			return items;
		}

		public ListBoxModel doFillProxyProtocolItems(@QueryParameter("proxyProtocol") String proxyProtocol) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < proxyProtocolComboValues.length; i++) {
				if (proxyProtocolComboValues[i].equalsIgnoreCase(proxyProtocol)) {
					items.add(new ListBoxModel.Option(proxyProtocolComboValues[i], proxyProtocolComboValues[i], true));
				} else {
					items.add(proxyProtocolComboValues[i], proxyProtocolComboValues[i]);
				}
			}

			return items;
		}

		public ListBoxModel doFillProxyAuthenticationItems(
				@QueryParameter("proxyAuthentication") String proxyAuthentication) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < proxyAuthenticationTypeComboValues.length; i++) {
				if (proxyAuthenticationTypeComboValues[i].equalsIgnoreCase(proxyAuthentication)) {
					items.add(new ListBoxModel.Option(proxyAuthenticationTypeComboValues[i],
							proxyAuthenticationTypeComboValues[i], true));
				} else {
					items.add(proxyAuthenticationTypeComboValues[i], proxyAuthenticationTypeComboValues[i]);
				}
			}

			return items;
		}

		public FormValidation doCheckTimeout(@QueryParameter("timeout") int timeout) {
			if (timeout < 1) {
				return FormValidation.error("Timeout must be greater than 0.");
			} else {
				return FormValidation.ok();
			}
		}
		
		public FormValidation doCheckTimeout_dm(@QueryParameter("timeout_dm") int timeout) {
			return doCheckTimeout(timeout);
		}
		
		public FormValidation doCheckTimeout_em(@QueryParameter("timeout_em") int timeout) {
			return doCheckTimeout(timeout);
		}

		public FormValidation doCheckThresholds(@QueryParameter("unstableThreshold") String unstableThreshold,
				@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
			FormValidation unstableThresholdValidationResult = doCheckUnstableThreshold(unstableThreshold, failureThreshold, measure);
			if (Kind.OK.equals(unstableThresholdValidationResult.kind)) {
				return doCheckFailureThreshold(failureThreshold, unstableThreshold, measure);
			} else {
				return unstableThresholdValidationResult;
			}
		}

		public FormValidation doCheckUnstableThreshold(@QueryParameter("unstableThreshold") String unstableThreshold,
				@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
			double unstable = 0;
			try {
				unstable = Double.parseDouble(unstableThreshold);
				if (unstable < 0) {
					return FormValidation.error("Unstable threshold must be a positive number.");
				}
			} catch (Throwable throwable) {
				return FormValidation.error("Unstable threshold must be a non-negative numeric value.");
			}

			if (QUALITY_INDICATOR.equalsIgnoreCase(measure)) {
				if (unstable >= 100) {
					return FormValidation.error("Unstable threshold must be lower than 100.");
				} else {
					try {
						double failure = Double.parseDouble(failureThreshold);
						if (failure >= unstable) {
							return FormValidation
									.error("Unstable threshold can not be lower or equal than failure threshold.");
						}
					} catch (Throwable throwable) {
						// Ignore
					}
				}
			} else if (RISK_INDEX.equalsIgnoreCase(measure)) {
				if (unstable <= 0) {
					return FormValidation.error("Unstable threshold must be greater than 0.");
				} else {
					try {
						double failure = Double.parseDouble(failureThreshold);
						if (failure <= unstable) {
							return FormValidation
									.error("Unstable threshold can not be greater or equal than failure threshold.");
						}
					} catch (Throwable throwable) {
						// Ignore
					}
				}
			} else if (EFFORT_TO_TARGET.equalsIgnoreCase(measure)) {
				try {
					double failed = Double.parseDouble(failureThreshold);
					if (failed <= unstable) {
						return FormValidation
								.error("Unstable threshold can not be greater or equal than failure threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckFailureThreshold(@QueryParameter("failureThreshold") String failureThreshold,
				@QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("measure") String measure) {
			double failure = 0;
			try {
				failure = Double.parseDouble(failureThreshold);
				if (failure < 0) {
					return FormValidation.error("Failure threshold must be a positive number.");
				}
			} catch (Throwable throwable) {
				return FormValidation.error("Failure threshold must be a non-negative numeric value.");
			}

			if (QUALITY_INDICATOR.equalsIgnoreCase(measure)) {
				try {
					double unstable = Double.parseDouble(unstableThreshold);
					if (failure >= unstable) {
						return FormValidation
								.error("Failure threshold can not be greater or equal than unstable threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			} else if (RISK_INDEX.equalsIgnoreCase(measure)) {
				if (failure > 100) {
					return FormValidation.error("Failure threshold must be lower or equal than 100.");
				} else {
					try {
						double unstable = Double.parseDouble(unstableThreshold);
						if (failure <= unstable) {
							return FormValidation
									.error("Failure threshold can not be lower or equal than unstable threshold.");
						}
					} catch (Throwable throwable) {
						// Ignore
					}
				}
			} else if (EFFORT_TO_TARGET.equalsIgnoreCase(measure)) {
				try {
					double unstable = Double.parseDouble(unstableThreshold);
					if (failure <= unstable) {
						return FormValidation
								.error("Failure threshold can not be lower or equal than unstable threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckSuccessResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
			return validateResultCodes(successResultCodes, new String[]{unstableResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes});
		}

		public FormValidation doCheckUnstableResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
			return validateResultCodes(unstableResultCodes, new String[]{successResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes});
		}
		
		public FormValidation doCheckFailureResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
			return validateResultCodes(failureResultCodes, new String[]{successResultCodes, unstableResultCodes, notBuiltResultCodes, abortedResultCodes});
		}
		
		public FormValidation doCheckNotBuiltResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
			return validateResultCodes(notBuiltResultCodes, new String[]{successResultCodes, unstableResultCodes, failureResultCodes, abortedResultCodes});
		}
		
		public FormValidation doCheckAbortedResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
			return validateResultCodes(abortedResultCodes, new String[]{successResultCodes, unstableResultCodes, failureResultCodes, notBuiltResultCodes});
		}
		
		private FormValidation validateResultCodes(String resultCodes, String[] otherCodes) {
			if(resultCodes != null){
				Set<Integer> codes = null;
				try{
					codes = parseErrorCodes(resultCodes);
				}catch(Throwable throwable){
					return FormValidation.error("Invalid value detected in result codes.");
				}
				
				//check duplicated codes
				for (String currentCodes : otherCodes) {
					try{
						Set<Integer> currentCodesList = parseErrorCodes(currentCodes);
						if(currentCodesList.retainAll(codes) && !currentCodesList.isEmpty()){
							return FormValidation.error("One result code can only be assigned to one build status. These codes are binded with multiple build statuses: "+currentCodesList+". Please remove the duplicity.");
						}
					}
					catch(Throwable throwable){
						//Ignore
					}
				}
			}
			
			return FormValidation.ok();
		}
	
	}

}