package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.kiuwan.client.KiuwanClientException;
import com.kiuwan.client.KiuwanRestApiClient;
import com.kiuwan.client.model.ApplicationResults;

public class KiuwanRecorder extends Recorder {

	public static final String INSTALL_DIR = "tools/kiuwan";

	public static final String AGENT_HOME = "KiuwanLocalAnalyzer";

	public static final String QUALITY_INDICATOR = "QUALITY_INDICATOR";

	public static final String EFFORT_TO_TARGET = "EFFORT_TO_TARGET";

	public static final String RISK_INDEX = "RISK_INDEX";

	private String applicationName;
	private String label;
	private String encoding;
	private String includes;
	private String excludes;
	private int timeout;
	private double unstableThreshold;
	private double failureThreshold;
	private String measure;
	private final static Long TIMEOUT_MARGIN = 5000L;

	@DataBoundConstructor
	public KiuwanRecorder(String applicationName, String label, String encoding, String includes, String excludes, int timeout, String measure, double unstableThreshold, double failureThreshold) {
		this.applicationName = applicationName;
		this.label = label;
		this.encoding = encoding;
		this.timeout = timeout;
		this.includes = includes;
		this.excludes = excludes;
		this.measure = measure;
		this.unstableThreshold = unstableThreshold;
		this.failureThreshold = failureThreshold;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the encoding
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @return the includes
	 */
	public String getIncludes() {
		return includes;
	}

	/**
	 * @return the excludes
	 */
	public String getExcludes() {
		return excludes;
	}

	/**
	 * @return the measure
	 */
	public String getMeasure() {
		return measure;
	}

	/**
	 * @return the unstableThreshold
	 */
	public double getUnstableThreshold() {
		return unstableThreshold;
	}

	/**
	 * @return the failureThreshold
	 */
	public double getFailureThreshold() {
		return failureThreshold;
	}

	/**
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}
	
	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild,
	 *      hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		boolean performResult = true;
		long startTime = System.currentTimeMillis();
		long endTime = startTime+TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES);
		
		AtomicReference<Throwable> exceptionReference = new AtomicReference<Throwable>();
		AtomicReference<Result> resultReference = new AtomicReference<Result>();
		Thread thread = createExecutionThread(build, launcher, listener, resultReference, exceptionReference);
		thread.start();
		
		long currentTime = System.currentTimeMillis();
		try{
			while(thread.isAlive() && currentTime < endTime){
				TimeUnit.MILLISECONDS.sleep(TIMEOUT_MARGIN);
				currentTime = System.currentTimeMillis();
			}
		}
		catch(InterruptedException interruptedException){
			if(thread.isAlive()){
				thread.interrupt();
			}
			build.setResult(Result.ABORTED);
			performResult = false;
			throw interruptedException;
		}

		if(thread.isAlive()){
			listener.getLogger().println("Aborted by timeout.");
			build.setResult(Result.ABORTED);
			performResult = false;
		}

		Throwable throwable = exceptionReference.get();
		if(throwable != null){
			if(throwable instanceof InterruptedException){
				throw (InterruptedException) throwable;
			}
			else if(throwable instanceof IOException){
				throw (IOException) throwable;
			}
			else{
				build.setResult(Result.FAILURE);
				performResult = false;
			}
		}
		
		Result result = resultReference.get();
		if(result != null){
			if(result.isWorseThan(Result.UNSTABLE)){
				performResult = false;
			}
			build.setResult(result);
		}
		
//		return performResult;
		return true;
	}

	private Thread createExecutionThread(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener, final AtomicReference<Result> resultReference, final AtomicReference<Throwable> exceptionReference) {
		Runnable runnable = new Runnable() {
			
			public void run() {
				try {
					DescriptorImpl descriptor = getDescriptor();
					FormValidation connectionTestResult = descriptor.doTestConnection(descriptor.getUsername(), descriptor.getPassword());
					if(Kind.OK.equals(connectionTestResult.kind)){
						performScan(build, launcher, listener, resultReference);
					}
					else{
						listener.getLogger().print("Could not get authorization from Kiuwan. Verify your ");
						listener.hyperlink(Jenkins.getInstance().getRootUrl()+"/configure", "Kiuwan account settings");
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
				}
				catch(Throwable throwable){
					listener.getLogger().println(ExceptionUtils.getFullStackTrace(throwable));
					resultReference.set(Result.NOT_BUILT);
				}
			}
		};
		
		Thread thread = new Thread(runnable);
		return thread;
	}

	private void performScan(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, AtomicReference<Result> resultReference) throws KiuwanException, IOException, InterruptedException {
		String name = this.applicationName;
		if (StringUtils.isEmpty(name)) {
			name = build.getProject().getName();
		}

		String analysisLabel = this.label;
		if (StringUtils.isEmpty(analysisLabel)) {
			analysisLabel = "#" + build.getNumber();
		}

		String analysisEncoding = this.encoding;
		if (StringUtils.isEmpty(analysisEncoding)) {
			analysisEncoding = "UTF-8";
		}

		FilePath srcFolder = build.getModuleRoot();

		Computer currentComputer = Computer.currentComputer();
		FilePath rootPath = currentComputer.getNode().getRootPath();

		FilePath remoteDir = rootPath.child(KiuwanComputerListener.INSTALL_DIR);
		FilePath agentHome = remoteDir.child(KiuwanComputerListener.AGENT_HOME);
		if (!agentHome.exists()) {
			installLocalAnalyzer(rootPath, listener);
		}

		DescriptorImpl descriptor = getDescriptor();

		String command = launcher.isUnix() ? "agent.sh" : "agent.cmd";
		FilePath agentBinDir = agentHome.child("bin");
		String script = agentBinDir.child(command).getRemote();

		EnvVars env = build.getEnvironment(listener);
		env.overrideAll(build.getBuildVariables());

		final PrintStream loggerStream = listener.getLogger();
		
		if (launcher.isUnix()) {
			loggerStream.println("Changing "+command+" permission");
			launcher.launch().cmdAsSingleString("chmod u+x \"" + agentBinDir.child("agent.sh").getRemote()+"\"").envs(env).stdout(loggerStream).pwd(agentBinDir.getRemote()).join();
		}
		
		saveCredentials(launcher, descriptor, agentBinDir, script, env, loggerStream);
		
		printExecutionConfiguration(listener, name, analysisLabel, analysisEncoding, srcFolder, script);

		String analysisCode = null;
		int result = -1;
		
		BufferedReader bufferedReader = null;
		try{
			List<String> args = buildAgentCommand(launcher, name, analysisLabel, analysisEncoding, srcFolder, command, agentBinDir);
			
			StringBuilder stringBuilder = new StringBuilder();
			for (String arg : args) {
				stringBuilder.append(arg+" ");
			}
			
			ProcessBuilder processBuilder = null;
			
			if(launcher.isUnix()){
				processBuilder = new ProcessBuilder(args);
			}
			else{
				processBuilder = new ProcessBuilder(stringBuilder.toString());
			}
			processBuilder.directory(new File(agentBinDir.getRemote()));
			processBuilder.environment().putAll(env);
			processBuilder.redirectErrorStream(true);
			
			Process process = processBuilder.start();

			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			Pattern pattern = Pattern.compile(".*Analysis created in Kiuwan with code: (.*)$");
			String line = null;
			while((line = bufferedReader.readLine()) != null){
				Matcher matcher = pattern.matcher(line);
				boolean found = matcher.find();
				if(found){
					analysisCode = matcher.group(1);
				}
				listener.getLogger().println(line);
			}

			result = process.waitFor();
		}finally{
			IOUtils.closeQuietly(bufferedReader);
		}

		if (result != 0 || analysisCode == null) {
			resultReference.set(Result.NOT_BUILT);
		} else {
			double qualityIndicator = -1d;
			double effortToTarget = -1d;
			double riskIndex = -1d;
			boolean buildFailedInKiuwan = false;
			
			boolean end = false;
			KiuwanRestApiClient client = new KiuwanRestApiClient(descriptor.getUsername(), descriptor.getPassword());
			int retries = 3;
			do {
				try {
					loggerStream.println("Query for result: "+analysisCode);
					ApplicationResults results = client.getApplicationResultsByAnalysisCode(analysisCode);
					loggerStream.println("Analysis status in Kiuwan: "+results.getAnalysisStatus());
					if ("FINISHED".equalsIgnoreCase(results.getAnalysisStatus())) {
						qualityIndicator = results.getQualityIndicator().getValue();
						effortToTarget = results.getEffortToTarget().getValue();
						BigDecimal rawRiskIndex = new BigDecimal(results.getRiskIndex().getValue());
						rawRiskIndex = rawRiskIndex.setScale(2, RoundingMode.HALF_UP);
						riskIndex = rawRiskIndex.doubleValue();
						end = true;
					} else if ("FINISHED_WITH_ERROR".equalsIgnoreCase(results.getAnalysisStatus())) {
						buildFailedInKiuwan = true;
						end = true;
					}
				} catch (KiuwanClientException e) {
					if(retries > 0){
						// Re-initializes the client.
						client = new KiuwanRestApiClient(descriptor.getUsername(), descriptor.getPassword());
						retries--;
					}
					else{
						loggerStream.println(e.getMessage());
						buildFailedInKiuwan = true;
						end = true;
					}
				}
				
				if(!end){
					Thread.sleep(60000);					
				}
			}while (!end);

			if (buildFailedInKiuwan) {
				loggerStream.println("Build failed in Kiuwan");
				resultReference.set(Result.NOT_BUILT);
			} else {
				printAnalysisSummary(listener, qualityIndicator, effortToTarget, riskIndex);
				checkThresholds(build, listener, qualityIndicator, effortToTarget, riskIndex, resultReference);
				KiuwanBuildSummaryAction link = new KiuwanBuildSummaryAction(name, analysisLabel);
				build.addAction(link);
			}
		}
	}

	private void saveCredentials(Launcher launcher, DescriptorImpl descriptor, FilePath agentBinDir, String script, EnvVars env, final PrintStream loggerStream) throws IOException, InterruptedException {
		encryptSecret(launcher, descriptor, agentBinDir, script, env, loggerStream);
		saveCredentialsInProperties(descriptor, agentBinDir);
	}

	private void saveCredentialsInProperties(DescriptorImpl descriptor, FilePath agentBinDir) throws FileNotFoundException, IOException {
		FilePath agentProperties = agentBinDir.getParent().child("conf").child("agent.properties");
		String propertiesFilePath = agentProperties.getRemote();

		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		try{
			bufferedReader = new BufferedReader(new FileReader(propertiesFilePath));
		
			String line = null;
			String usernameValue = "";
			while((line = bufferedReader.readLine()) != null){
				String cleanLine = line.trim();
				if(cleanLine.startsWith("username=")){
					usernameValue = cleanLine.replaceFirst("username=", "");
					line = "username="+descriptor.getUsername();
				}
				else if(cleanLine.startsWith("password=")){
					line = "password="+usernameValue;
				}
				
				stringBuilder.append(line+"\n");
			}
		}
		finally{
			IOUtils.closeQuietly(bufferedReader);
		}
		
		BufferedWriter bufferedWriter = null;
		try{
			bufferedWriter= new BufferedWriter(new FileWriter(propertiesFilePath));
			bufferedWriter.write(stringBuilder.toString());
		}
		finally{
			IOUtils.closeQuietly(bufferedWriter);
		}
	}

	private void encryptSecret(Launcher launcher, DescriptorImpl descriptor, FilePath agentBinDir, String script, EnvVars env, final PrintStream loggerStream) throws IOException, InterruptedException {
		PipedInputStream userInput = new PipedInputStream();
		final PipedOutputStream userKeyboard = new PipedOutputStream(userInput);
		
		final PipedInputStream consoleReaderStream = new PipedInputStream();
		PipedOutputStream console = new PipedOutputStream(consoleReaderStream);

		try{
			final String password = descriptor.getPassword();
			Runnable consoleRunnable = new Runnable() {
				
				public void run() {
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(consoleReaderStream));
					OutputStreamWriter outputStreamWriter = new OutputStreamWriter(userKeyboard);
					try {
						bufferedReader.readLine();
						outputStreamWriter.write(password);
						outputStreamWriter.flush();
					} catch (IOException e) {
						loggerStream.println(ExceptionUtils.getFullStackTrace(e));
					}
					finally{
						IOUtils.closeQuietly(outputStreamWriter);
					}
				}
			};		
			
			Thread thread = new Thread(consoleRunnable);
			thread.start();
			
			launcher.launch().cmdAsSingleString("\""+script+"\" -e").envs(env).stdin(userInput).stdout(console).pwd(agentBinDir.getRemote()).join();

			thread.join(5000L);
		}
		finally{
			IOUtils.closeQuietly(console);
			IOUtils.closeQuietly(userInput);
		}
	}

	private void printExecutionConfiguration(BuildListener listener, String name, String analysisLabel, String analysisEncoding, FilePath srcFolder, String script) {
		listener.getLogger().println("Analyze folder: " + srcFolder.getRemote());
		listener.getLogger().println("Script: " + script);
		listener.getLogger().println("kiuwan app name: " + name);
		listener.getLogger().println("Analysis label: " + analysisLabel);
		listener.getLogger().println("Threshold measure: " + this.measure);
		listener.getLogger().println("Unstable threshold: " + this.unstableThreshold);
		listener.getLogger().println("Failure threshold: " + this.failureThreshold);
		listener.getLogger().println("encoding: " + analysisEncoding);
		listener.getLogger().println("includes pattern: " + includes);
		listener.getLogger().println("excludes pattern: " + excludes);
		listener.getLogger().println("timeout: " + timeout + " minutes");
	}

	private void checkThresholds(AbstractBuild<?, ?> build, BuildListener listener, double qualityIndicator, double effortToTarget, double riskIndex, AtomicReference<Result> resultReference) {
		if (QUALITY_INDICATOR.equalsIgnoreCase(this.measure)) {
			if (qualityIndicator < this.failureThreshold) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Quality indicator is lower than " + this.failureThreshold);
			} else if (qualityIndicator < this.unstableThreshold) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Quality indicator is lower than " + this.unstableThreshold);
			}
		} else if (EFFORT_TO_TARGET.equalsIgnoreCase(this.measure)) {
			if (effortToTarget > this.failureThreshold) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Effort to target is greater than " + this.failureThreshold);
			} else if (effortToTarget > this.unstableThreshold) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Effort to target is greater than " + this.unstableThreshold);
			}
		} else if (RISK_INDEX.equalsIgnoreCase(this.measure)) {
			if (riskIndex > this.failureThreshold) {
				resultReference.set(Result.FAILURE);
				listener.getLogger().println("Risk index is greater than " + this.failureThreshold);
			}
			else if (riskIndex > this.unstableThreshold) {
				resultReference.set(Result.UNSTABLE);
				listener.getLogger().println("Risk index is greater than " + this.unstableThreshold);
			}
		}
	}

	private void printAnalysisSummary(BuildListener listener, double qualityIndicator, double effortToTarget, double riskIndex) {
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println("                    Kiuwan Static Analysis Summary                        ");
		listener.getLogger().println("==========================================================================");
		listener.getLogger().println(" - Quality indicator: " + qualityIndicator);
		listener.getLogger().println(" - Effort to target: " + effortToTarget);
		listener.getLogger().println(" - Risk index: " + riskIndex);
		listener.getLogger().println();
	}

	private List<String> buildAgentCommand(Launcher launcher, String name, String analysisLabel, String analysisEncoding, FilePath srcFolder, String command, FilePath agentBinDir) {
		String timeoutAsString = Long.toString(TimeUnit.MILLISECONDS.convert(this.timeout, TimeUnit.MINUTES)-TIMEOUT_MARGIN);
		
		List<String> args = new ArrayList<String>();
		
		String commandAbsolutePath = agentBinDir.child(command).getRemote();
		args.add(buildArgument(launcher, commandAbsolutePath));
		args.add("-s");
		args.add(buildArgument(launcher, srcFolder.getRemote()));
		args.add("-n");
		args.add(buildArgument(launcher, name));
		args.add("-l");
		args.add(buildArgument(launcher, analysisLabel));
		args.add("-c");
		
		args.add(buildAdditionalParameterExpression(launcher, "timeout", timeoutAsString));
		args.add(buildAdditionalParameterExpression(launcher, "encoding", analysisEncoding));
		
		if (StringUtils.isNotBlank(includes)) {
			args.add(buildAdditionalParameterExpression(launcher, "include.patterns", includes));
		}
		if (StringUtils.isNotBlank(excludes)) {
			args.add(buildAdditionalParameterExpression(launcher, "exclude.patterns", excludes));
		}
		
		return args;
	}

	private String buildArgument(Launcher launcher, String argument) {
		if(launcher.isUnix()){
			return argument;
		}
		else{
			return "\""+argument+"\"";
		}
	}

	private String buildAdditionalParameterExpression(Launcher launcher, String parameterName, String parameterValue) {
		String parameterExpression = "";
		if(launcher.isUnix()){
			parameterExpression = parameterName+"="+parameterValue;
		}
		else{
			parameterExpression = parameterName+"=\""+parameterValue+"\"";
		}
		return parameterExpression;
	}

	private void installLocalAnalyzer(FilePath root, BuildListener listener) throws IOException, InterruptedException {
		KiuwanDownloadable kiuwanDownloadable = new KiuwanDownloadable();
		FilePath remoteDir = root.child(INSTALL_DIR);
		listener.getLogger().println("Installing KiuwanLocalAnalyzer in " + remoteDir);
		Map<Object, Object> props = Computer.currentComputer().getSystemProperties();
		File zip = kiuwanDownloadable.resolve((String) props.get("os.name"), (String) props.get("sun.arch.data.model"), listener);
		remoteDir.mkdirs();
		new FilePath(zip).unzip(remoteDir);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private final static String[] comboValues = { QUALITY_INDICATOR, RISK_INDEX, EFFORT_TO_TARGET };

		private final static String[] comboNames = { "Quality indicator", "Risk index", "Effort to target" };

		private String username;

		private String password;

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			// to persist global configuration information,
			// set that to properties and call save().
			String username = (String) json.get("username");
			String password = (String) json.get("password");

			this.username = username;
			Secret secret = Secret.fromString(password);
			this.password = secret.getEncryptedValue();
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

		public FormValidation doTestConnection(@QueryParameter String username, @QueryParameter String password) {
			KiuwanRestApiClient client = new KiuwanRestApiClient(username, password);
			try {
				client.getApplications();
				return FormValidation.ok("Authentication completed successfully!");
			} catch (KiuwanClientException kiuwanClientException) {
				return FormValidation.error("Authentication failed.");
			} catch (Throwable throwable) {
				return FormValidation.warning("Could not initiate the authentication process. Reason: " + throwable.getMessage());
			}
		}

		public ListBoxModel doFillMeasureItems(@QueryParameter("measure") String measure) {
			ListBoxModel items = new ListBoxModel();
			for (int i = 0; i < comboNames.length; i++) {
				if (comboValues[i].equalsIgnoreCase(measure)) {
					items.add(new ListBoxModel.Option(comboNames[i], comboValues[i], true));
				} else {
					items.add(comboNames[i], comboValues[i]);
				}
			}

			return items;
		}

		public FormValidation doCheckTimeout(@QueryParameter("timeout") int timeout) {
			if(timeout < 1){
				return FormValidation.error("Timeout must be greater than 0.");
			}
			else{
				return FormValidation.ok();
			}
		}
		
		public FormValidation doCheckThresholds(@QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
			FormValidation unstableThresholdValidationResult = doCheckUnstableThreshold(unstableThreshold, failureThreshold, measure);
			if(Kind.OK.equals(unstableThresholdValidationResult.kind)){
				return doCheckFailureThreshold(failureThreshold, unstableThreshold, measure);
			}
			else{
				return unstableThresholdValidationResult;
			}
		}
		
		public FormValidation doCheckUnstableThreshold(@QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
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
							return FormValidation.error("Unstable threshold can not be lower or equal than failure threshold.");
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
							return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
						}
					} catch (Throwable throwable) {
						// Ignore
					}
				}
			} else if (EFFORT_TO_TARGET.equalsIgnoreCase(measure)) {
				try {
					double failed = Double.parseDouble(failureThreshold);
					if (failed <= unstable) {
						return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckFailureThreshold(@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("measure") String measure) {
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
						return FormValidation.error("Failure threshold can not be greater or equal than unstable threshold.");
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
							return FormValidation.error("Failure threshold can not be lower or equal than unstable threshold.");
						}
					} catch (Throwable throwable) {
						// Ignore
					}
				}
			} else if (EFFORT_TO_TARGET.equalsIgnoreCase(measure)) {
				try {
					double unstable = Double.parseDouble(unstableThreshold);
					if (failure <= unstable) {
						return FormValidation.error("Failure threshold can not be lower or equal than unstable threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}

			return FormValidation.ok();
		}

	}
	
}