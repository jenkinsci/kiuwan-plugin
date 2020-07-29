package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.CONFIGURE_PROXY_JENKINS;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.CONFIGURE_PROXY_NONE;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.TIMEOUT_MARGIN_MILLIS;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.TIMEOUT_MARGIN_SECONDS;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.buildAdditionalParameterExpression;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.buildArgument;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getNodeJenkinsDirectory;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getRemoteFileAbsolutePath;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getToolsTempRelativePath;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ChangeRequestStatusType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.DeliveryType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyConfig;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;

public class KiuwanAnalyzerCommandBuilder {

	private static final String AGENT_CONF_DIR_NAME = "conf";
	private static final String AGENT_PROPERTIES_FILE_NAME = "agent.properties";
	
	private static final String PROXY_PROTOCOL = "proxy.protocol";
	private static final String PROXY_HOST = "proxy.host";
	private static final String PROXY_PORT = "proxy.port";
	private static final String PROXY_AUTHENTICATION = "proxy.authentication";
	private static final String PROXY_USERNAME = "proxy.username";
	private static final String PROXY_PASSWORD = "proxy.password";
	
	private final static String proxyHostRegExp = "^\\s*proxy\\.host\\s*=.*$";
	private final static String proxyPortRegExp = "^\\s*proxy\\.port\\s*=.*$";
	private final static String proxyProtocolRegExp = "^\\s*proxy\\.protocol\\s*=.*$";
	private final static String proxyAuthenticationRegExp = "^\\s*proxy\\.authentication\\s*=.*$";
	private final static String proxyUsernameRegExp = "^\\s*proxy\\.username\\s*=.*$";
	private final static String proxyPasswordRegExp = "^\\s*proxy\\.password\\s*=.*$";
	private final static String kiuwanJenkinsPluginHeaderPrefix = "### KIUWAN JENKINS PLUGIN: ";
	private final static String kiuwanJenkinsPluginHeaderSuffix = " ###";
	private final static String kiuwanJenkinsPluginHeaderPattern = kiuwanJenkinsPluginHeaderPrefix + "(.*)" + kiuwanJenkinsPluginHeaderSuffix;
	
	private KiuwanRecorder recorder;
	private FilePath workspace;
	private KiuwanConnectionProfile connectionProfile;
	private KiuwanGlobalConfigDescriptor descriptor;
	private Node node;
	private Run<?, ?> run;
	private Launcher launcher;
	private TaskListener listener;
	
	private FilePath tempOutputFilePath;
	
	public KiuwanAnalyzerCommandBuilder(KiuwanRecorder recorder, FilePath workspace, KiuwanConnectionProfile connectionProfile, 
			KiuwanGlobalConfigDescriptor descriptor, Node node, Run<?, ?> run, Launcher launcher, TaskListener listener) {
		super();
		this.recorder = recorder;
		this.workspace = workspace;
		this.connectionProfile = connectionProfile;
		this.descriptor = descriptor;
		this.node = node;
		this.run = run;
		this.launcher = launcher;
		this.listener = listener;
	}

	public List<String> buildLocalAnalyzerCommand(FilePath agentHome, EnvVars envVars) throws IOException, InterruptedException {
		Mode selectedMode = recorder.getSelectedMode();
		
		String name = null;
		String analysisLabel = null;
		String analysisEncoding = null;
		{
			if (Mode.DELIVERY_MODE.equals(selectedMode)) {
				name = recorder.getApplicationName_dm();
				analysisLabel = recorder.getLabel_dm();
				analysisEncoding = recorder.getEncoding_dm();
			
			} else {
				name = recorder.getApplicationName();
				analysisLabel = recorder.getLabel();
				analysisEncoding = recorder.getEncoding();
			}
			
			if (StringUtils.isEmpty(name)) {
				name = run.getParent().getName();
			}
				
			if (StringUtils.isEmpty(analysisLabel)) {
				analysisLabel = "#" + run.getNumber();
			}
					
			if (StringUtils.isEmpty(analysisEncoding)) {
				analysisEncoding = "UTF-8";
			}
		}

		Integer timeout = null;
		String includes = null;
		String excludes = null;
		String languages = null;
		
		if (Mode.DELIVERY_MODE.equals(selectedMode)) {
			timeout = recorder.getTimeout_dm();
			includes = recorder.getIncludes_dm();
			excludes = recorder.getExcludes_dm();
			languages = recorder.getLanguages_dm();
			
		} else if (Mode.EXPERT_MODE.equals(selectedMode)) {
			timeout = recorder.getTimeout_em();
			
		} else {
			timeout = recorder.getTimeout();
			includes = recorder.getIncludes();
			excludes = recorder.getExcludes();
			languages = recorder.getLanguages();
		}
		
		long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES);
		long timeoutSeconds = TimeUnit.SECONDS.convert(timeout, TimeUnit.MINUTES);
		String timeoutAsStringMillis = Long.toString(timeoutMillis - TIMEOUT_MARGIN_MILLIS);
		String timeoutAsStringSeconds = Long.toString(timeoutSeconds - TIMEOUT_MARGIN_SECONDS);

		List<String> args = new ArrayList<String>();

		// Always quote command absolute path under windows
		FilePath command = getLocalAnalyzerCommandFilePath(launcher, agentHome);
		String commandAbsolutePath = getRemoteFileAbsolutePath(command, listener);
		args.add(launcher.isUnix() ? commandAbsolutePath : "\"" + commandAbsolutePath + "\"");

		FilePath srcFolder = resolveSrcFolder();
		args.add("-s");
		args.add(buildArgument(launcher, getRemoteFileAbsolutePath(srcFolder, listener)));
		
		// Get this node temporal directory under this profiles tools path
		FilePath nodeJenkinsDir = getNodeJenkinsDirectory(node, workspace);
		String toolsTempRelativePath = getToolsTempRelativePath(connectionProfile);
		FilePath toolsTempDir = nodeJenkinsDir.child(toolsTempRelativePath);
		toolsTempDir.mkdirs();
		tempOutputFilePath = toolsTempDir.createTempFile("kiuwanJenkinsPlugin-" + run.getDisplayName() + "_", ".json");
		
		args.add("-o");
		args.add(buildArgument(launcher, getRemoteFileAbsolutePath(tempOutputFilePath, listener)));

		args.add("--user");
		args.add(buildArgument(launcher, connectionProfile.getUsername()));
		args.add("--pass");
		args.add(buildArgument(launcher, connectionProfile.getPassword()));
		
		String domain = connectionProfile.getDomain();
		if(StringUtils.isNotBlank(domain)) {
			args.add("--domain-id");
			args.add(buildArgument(launcher, domain));
		}

		if (Mode.STANDARD_MODE.equals(recorder.getSelectedMode())) {
			args.add("-n");
			args.add(buildArgument(launcher, name));
			args.add("-l");
			args.add(buildArgument(launcher, analysisLabel));
			args.add("-c");
			
			// In standard mode, wait for results only if a measure to check thresholds is specified 
			if (!Measure.NONE.name().equalsIgnoreCase(recorder.getMeasure())) {
				args.add("-wr");
			}
			
		} else if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			args.add("-n");
			args.add(buildArgument(launcher, name));
			args.add("-l");
			args.add(buildArgument(launcher, analysisLabel));
			args.add("-c");
			args.add("-cr");
			args.add(buildArgument(launcher, recorder.getChangeRequest_dm()));
			args.add("-as");
			
			String deliveryType = recorder.getAnalysisScope_dm();
			if (DeliveryType.COMPLETE_DELIVERY.name().equals(deliveryType)) {
				deliveryType = "completeDelivery";
			} else if (DeliveryType.PARTIAL_DELIVERY.name().equals(deliveryType)) {
				deliveryType = "partialDelivery";
			}
			args.add(buildArgument(launcher, deliveryType));
			
			if (recorder.getWaitForAuditResults_dm()) {
				args.add("-wr");
			}
			
			String branch = recorder.getBranch_dm();
			if (StringUtils.isNotBlank(branch)) {
				args.add("-bn");
				args.add(buildArgument(launcher, branch));
			}
			
			String changeRequestStatus = recorder.getChangeRequestStatus_dm();
			if (StringUtils.isNotBlank(changeRequestStatus)) {
				args.add("-crs");
				if (ChangeRequestStatusType.INPROGRESS.name().equals(changeRequestStatus)) {
					changeRequestStatus = "inprogress";
				} else {
					changeRequestStatus = "resolved";
				}

				args.add(buildArgument(launcher, changeRequestStatus));
			}
			
		} else if (Mode.EXPERT_MODE.equals(recorder.getSelectedMode())) {
			parseOptions(args, launcher);
			parseParameters(args, launcher);
		}

		args.add(buildAdditionalParameterExpression(launcher, "timeout", timeoutAsStringMillis));
		args.add(buildAdditionalParameterExpression(launcher, "results.timeout", timeoutAsStringSeconds));

		if (!Mode.EXPERT_MODE.equals(recorder.getSelectedMode())) {
			if (StringUtils.isNotBlank(includes)) {
				launcher.getListener().getLogger().println("Setting includes pattern -> "+includes);
				args.add(buildAdditionalParameterExpression(launcher, "include.patterns", includes));
			}
			if (StringUtils.isNotBlank(excludes)) {
				args.add(buildAdditionalParameterExpression(launcher, "exclude.patterns", excludes));
			}
			
			args.add(buildAdditionalParameterExpression(launcher, "encoding", analysisEncoding));
			if ((Mode.STANDARD_MODE.equals(recorder.getSelectedMode()) && Boolean.TRUE.equals(recorder.getIndicateLanguages())) || 
				(Mode.DELIVERY_MODE.equals(recorder.getSelectedMode()) && Boolean.TRUE.equals(recorder.getIndicateLanguages_dm()))) {
				args.add(buildAdditionalParameterExpression(launcher, "supported.technologies", languages));
			}
		}
		
		ProxyConfig proxyConfig = null;
				
		// No proxy
		if (CONFIGURE_PROXY_NONE.equals(connectionProfile.getConfigureProxy())) {
			proxyConfig = ProxyConfig.EMPTY;
		
		// Use jenkins proxy
		} else if (CONFIGURE_PROXY_JENKINS.equals(connectionProfile.getConfigureProxy())) {
			proxyConfig = KiuwanUtils.getJenkinsProxy(null);
		
		// Use custom proxy
		} else if (KiuwanConnectionProfile.CONFIGURE_PROXY_CUSTOM.equals(connectionProfile.getConfigureProxy())) {
			proxyConfig = new ProxyConfig(connectionProfile.getProxyHost(), connectionProfile.getProxyPort(), 
				connectionProfile.getProxyProtocol(), connectionProfile.getProxyAuthentication(),
				connectionProfile.getProxyUsername(), connectionProfile.getProxyPassword());
		}
		
		FilePath agentBinDir = getAgentBinDir(agentHome);
		writeConfigToProperties(agentBinDir, proxyConfig);
		
		return args;
	}

	public FilePath getTempOutputFilePath() {
		return tempOutputFilePath;
	}
	
	public static FilePath getAgentBinDir(FilePath agentHome) {
		return agentHome.child("bin");
	}

	public static String getLocalAnalyzerCommandFileName(Launcher launcher) {
		return launcher.isUnix() ? "agent.sh" : "agent.cmd";
	}
	
	public static FilePath getLocalAnalyzerCommandFilePath(Launcher launcher, FilePath agentHome) {
		String command = getLocalAnalyzerCommandFileName(launcher);
		FilePath binDir = getAgentBinDir(agentHome);
		FilePath script = binDir.child(command);
		return script;
	}

	public static boolean[] getMasks(List<String> args) {
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
		return masks;
	}

	private void parseOptions(List<String> args, Launcher launcher) {
		String commandArgs = recorder.getCommandArgs_em();
		String escapedBackslash = null;
		do {
			escapedBackslash = "--" + new Random().nextInt() + "--";
		} while (commandArgs.contains(escapedBackslash));

		commandArgs = commandArgs.replaceAll("\\\\\\\\", escapedBackslash);
		
		String escapedDQuotes = null;
		do {
			escapedDQuotes = "--" + new Random().nextInt() + "--";
		} while (commandArgs.contains(escapedDQuotes));

		commandArgs = commandArgs.replaceAll("\\\\\"", escapedDQuotes);
		
		String escapedSpaces = null;
		do {
			escapedSpaces = "--" + new Random().nextInt() + "--";
		} while (commandArgs.contains(escapedSpaces));
		
		String dquotes = null;
		do {
			dquotes = "--" + new Random().nextInt() + "--";
		} while (commandArgs.contains(dquotes));
		
		Pattern compile = Pattern.compile("^([^\"]*)(\"[^\"]*\")(.*)$");
		Matcher matcher = compile.matcher(commandArgs);
		while (matcher.find()) {
			commandArgs = matcher.group(1) + 
				matcher.group(2).replaceAll(" ", escapedSpaces).replaceAll("\"", dquotes) + 
				matcher.group(3);
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
	
	private void parseParameters(List<String> args, Launcher launcher) {
		String extraParams = recorder.getExtraParameters_em();
		Pattern equalWithSpaces = Pattern.compile("^(.*\\s*[^=\\s]+)\\s+=\\s+(\".*\".*)$");
		Matcher equalsWithSpacesMatcher = equalWithSpaces.matcher(extraParams);
		while(equalsWithSpacesMatcher.find()){
			extraParams = equalsWithSpacesMatcher.group(1)+"="+equalsWithSpacesMatcher.group(2);

			equalsWithSpacesMatcher = equalWithSpaces.matcher(extraParams);
		}
		
		String escapedBackslash = null;
		do {
			escapedBackslash = "--" + new Random().nextInt() + "--";
		} while (extraParams.contains(escapedBackslash));

		extraParams = extraParams.replaceAll("\\\\\\\\", escapedBackslash);
		
		String escapedDQuotes = null;
		do {
			escapedDQuotes = "--" + new Random().nextInt() + "--";
		} while (extraParams.contains(escapedDQuotes));

		extraParams = extraParams.replaceAll("\\\\\"", escapedDQuotes);
		
		String escapedSpaces = null;
		do {
			escapedSpaces = "--" + new Random().nextInt() + "--";
		} while (extraParams.contains(escapedSpaces));
		
		String dquotes = null;
		do {
			dquotes = "--" + new Random().nextInt() + "--";
		} while (extraParams.contains(dquotes));
		
		Pattern compile = Pattern.compile("^([^\"]*)(\"[^\"]*\")(.*)$");
		Matcher matcher = compile.matcher(extraParams);
		while (matcher.find()) {
			extraParams = matcher.group(1) + 
				matcher.group(2).replaceAll(" ", escapedSpaces).replaceAll("\"", dquotes) + 
				matcher.group(3);
			matcher = compile.matcher(extraParams);
		}
		
		String[] split = extraParams.split("\\s+");
		for (String token : split) {
			token = token.replaceAll(dquotes, "\"").replaceAll(escapedSpaces, " ");
			token = token.replaceAll(escapedDQuotes, "\"");
			token = token.replaceAll(escapedBackslash, "\\\\");
			
			String[] paramValue = token.split("=", 2);
			if (paramValue.length == 2) {
				String key = paramValue[0];
				String value = paramValue[1];
				Matcher doubleQuotedTokenMatcher = Pattern.compile("^\"(.*)\"$").matcher(value);
				if (doubleQuotedTokenMatcher.find()) {
					value =	doubleQuotedTokenMatcher.group(1);
				}
				
				args.add(buildAdditionalParameterExpression(launcher, key, value));
			}
		}
	}
	
	private FilePath resolveSrcFolder() throws IOException {
		FilePath srcFolder = null;
		if (StringUtils.isNotEmpty(recorder.getSourcePath())) {
			String sourcePath = recorder.getSourcePath();
			File sourcePathFile = new File(sourcePath);
			if (sourcePathFile.isAbsolute()) {
				srcFolder = new FilePath(sourcePathFile);
			} else {
				srcFolder = new FilePath(workspace, sourcePath);
			}
		
		} else if (run instanceof AbstractBuild) {
			AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
			srcFolder = build.getModuleRoot();
		}
		
		if (srcFolder == null) {
			throw new IOException("Could not resolver source folder. Source path = \"" + recorder.getSourcePath() + "\"");
		}
		
		return srcFolder;
	}
	
	private void writeConfigToProperties(FilePath agentBinDir, ProxyConfig proxyConfig) 
			throws IOException, InterruptedException {
		
		FilePath agentPropertiesPath = agentBinDir.getParent().child(AGENT_CONF_DIR_NAME).child(AGENT_PROPERTIES_FILE_NAME);
		StringBuilder newFileContent = new StringBuilder();
		boolean updateConfig = true;
		String configSaveStamp = descriptor.getConfigSaveTimestamp();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(agentPropertiesPath.read()))) {
			boolean firstLineProcessed = false;
			String line = null;
			
			while (updateConfig && (line = reader.readLine()) != null) {
				if (!firstLineProcessed) {
					Matcher matcher = Pattern.compile(kiuwanJenkinsPluginHeaderPattern).matcher(line);
					if (matcher.find()) {
						if (configSaveStamp != null) {
							String stamp = matcher.group(1);
							if (configSaveStamp.equals(stamp)) {
								updateConfig = false;
							}
						
						} else {
							updateConfig = false;
						}
					
					} else {
						updateConfig = true;
						if (configSaveStamp == null) {
							configSaveStamp = Long.toHexString(System.currentTimeMillis());
						}
					}
					
					if (updateConfig) {
						newFileContent.append(kiuwanJenkinsPluginHeaderPrefix + configSaveStamp + kiuwanJenkinsPluginHeaderSuffix + "\n");
					}
					
					firstLineProcessed = true;
				}
				
				if (!Pattern.matches(kiuwanJenkinsPluginHeaderPattern, line)
					&& !Pattern.matches(proxyHostRegExp, line) 
					&& !Pattern.matches(proxyPortRegExp, line) 
					&& !Pattern.matches(proxyProtocolRegExp, line)
					&& !Pattern.matches(proxyAuthenticationRegExp, line)
					&& !Pattern.matches(proxyUsernameRegExp, line)
					&& !Pattern.matches(proxyPasswordRegExp, line)) {
					
					newFileContent.append(line + "\n");
				}
			}
		}
				
		if (updateConfig) {
			newFileContent.append(PROXY_HOST + "=" + proxyConfig.getHost() + "\n");
			newFileContent.append(PROXY_PORT + "=" + proxyConfig.getPort() + "\n");
			newFileContent.append(PROXY_PROTOCOL + "=" + proxyConfig.getLocalAnalyzerProtocolOption() + "\n");
			newFileContent.append(PROXY_AUTHENTICATION + "=" + proxyConfig.getLocalAnalyzerAuthenticationOption() + "\n");
			newFileContent.append(PROXY_USERNAME + "=" + proxyConfig.getUsername() + "\n");
			newFileContent.append(PROXY_PASSWORD + "=" + proxyConfig.getPassword() + "\n");
			
			agentPropertiesPath.write(newFileContent.toString(), "UTF-8");
		}
	}
	
}
