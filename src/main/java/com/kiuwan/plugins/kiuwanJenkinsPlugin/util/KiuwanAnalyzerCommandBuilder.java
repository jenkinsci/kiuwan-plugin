package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.buildAdditionalParameterExpression;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.buildArgument;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.getRemoteFileAbsolutePath;

import java.io.BufferedReader;
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
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
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
	
	private KiuwanGlobalConfigDescriptor descriptor;
	private KiuwanRecorder recorder;
	private KiuwanConnectionProfile connectionProfile;
	
	public KiuwanAnalyzerCommandBuilder(KiuwanGlobalConfigDescriptor descriptor, KiuwanRecorder recorder) {
		super();
		this.descriptor = descriptor;
		this.recorder = recorder;
		
		String connectionProfileUuid = recorder.getConnectionProfileUuid();
		this.connectionProfile = descriptor.getConnectionProfile(connectionProfileUuid);
	}

	public List<String> buildAgentCommand(Launcher launcher, String name, 
			String analysisLabel, String analysisEncoding, FilePath srcFolder, 
			String command, FilePath agentBinDir, TaskListener listener,
			EnvVars envVars) throws IOException, InterruptedException {
		
		Integer timeout = null;
		String includes = null;
		String excludes = null;
		String languages = null;
		
		Mode selectedMode = recorder.getSelectedMode();
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
		
		String timeoutAsString = Long.toString(TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES) - KiuwanRecorder.TIMEOUT_MARGIN);

		List<String> args = new ArrayList<String>();

		// Always quote command absolute path under windows
		String commandAbsolutePath = getRemoteFileAbsolutePath(agentBinDir.child(command), listener);
		args.add(launcher.isUnix() ? commandAbsolutePath : "\"" + commandAbsolutePath + "\"");

		args.add("-s");
		args.add(buildArgument(launcher, getRemoteFileAbsolutePath(srcFolder, listener)));

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
			
		} else if (Mode.DELIVERY_MODE.equals(recorder.getSelectedMode())) {
			args.add("-n");
			args.add(buildArgument(launcher, name));
			args.add("-l");
			args.add(buildArgument(launcher, analysisLabel));
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

		args.add(buildAdditionalParameterExpression(launcher, "timeout", timeoutAsString));

		if (!Mode.EXPERT_MODE.equals(recorder.getSelectedMode())) {
			if (StringUtils.isNotBlank(includes)) {
				launcher.getListener().getLogger().println("Setting includes pattern -> "+includes);
				args.add(buildAdditionalParameterExpression(launcher, "include.patterns", includes));
			}
			if (StringUtils.isNotBlank(excludes)) {
				args.add(buildAdditionalParameterExpression(launcher, "exclude.patterns", excludes));
			}
			
			args.add(buildAdditionalParameterExpression(launcher, "encoding", analysisEncoding));
			if (
				(Mode.STANDARD_MODE.equals(recorder.getSelectedMode()) && (recorder.getIndicateLanguages() != null && recorder.getIndicateLanguages() == true))
				|| 
				(Mode.DELIVERY_MODE.equals(recorder.getSelectedMode()) && (recorder.getIndicateLanguages_dm() != null && recorder.getIndicateLanguages_dm() == true))
			) {
				args.add(buildAdditionalParameterExpression(launcher, "supported.technologies", languages));
			}
		}
		
		String proxyHost = connectionProfile.getProxyHost();
		String proxyPort = Integer.toString(connectionProfile.getProxyPort());
		String proxyProtocol = connectionProfile.getProxyProtocol();
		String proxyAuthentication = KiuwanConnectionProfile.PROXY_AUTHENTICATION_BASIC;
		String proxyUsername = connectionProfile.getProxyUsername();
		String proxyPassword = connectionProfile.getProxyPassword();
		String configSaveStamp = descriptor.getConfigSaveTimestamp();
				
		if (!connectionProfile.isConfigureProxy()) {
			proxyHost = "";
			proxyAuthentication = KiuwanConnectionProfile.PROXY_AUTHENTICATION_NONE;
		
		} else if (!KiuwanConnectionProfile.PROXY_AUTHENTICATION_BASIC.equals(connectionProfile.getProxyAuthentication())) {
			proxyAuthentication = KiuwanConnectionProfile.PROXY_AUTHENTICATION_NONE;
			proxyUsername = "";
			proxyPassword = "";
		}
			
		writeConfigToProperties(agentBinDir, 
			proxyHost, proxyPort, proxyProtocol, proxyAuthentication,
			proxyUsername, proxyPassword, configSaveStamp);
		
		return args;
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
	
	private void writeConfigToProperties(FilePath agentBinDir, 
			String proxyHost, String proxyPort, String proxyProtocol, String proxyAuthentication, 
			String proxyUsername, String proxyPassword, String configSaveStamp) throws IOException, InterruptedException {
		
		FilePath agentPropertiesPath = agentBinDir.getParent().child(AGENT_CONF_DIR_NAME).child(AGENT_PROPERTIES_FILE_NAME);
		StringBuilder newFileContent = new StringBuilder();
		boolean updateConfig = true;
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(agentPropertiesPath.read()))) {
			boolean firstLineProcessed = false;
			String line = null;
			
			while (updateConfig && (line = reader.readLine()) != null) {
				if (!firstLineProcessed) {
					Matcher matcher = Pattern.compile(kiuwanJenkinsPluginHeaderPattern).matcher(line);
					if (matcher.find()) {
						if (configSaveStamp != null) {
							String stamp = matcher.group(1);
							if(configSaveStamp.equals(stamp)){
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
			newFileContent.append(PROXY_HOST + "=" + proxyHost + "\n");
			newFileContent.append(PROXY_PORT + "=" + proxyPort + "\n");
			newFileContent.append(PROXY_PROTOCOL + "=" + proxyProtocol + "\n");
			newFileContent.append(PROXY_AUTHENTICATION + "=" + proxyAuthentication + "\n");
			newFileContent.append(PROXY_USERNAME + "=" + proxyUsername + "\n");
			newFileContent.append(PROXY_PASSWORD + "=" + proxyPassword + "\n");
			
			agentPropertiesPath.write(newFileContent.toString(), "UTF-8");
		}
	}
	
}
