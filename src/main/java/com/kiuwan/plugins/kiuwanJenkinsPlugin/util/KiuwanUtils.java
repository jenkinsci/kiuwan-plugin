package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.KiuwanModelObject;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyAuthentication;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyConfig;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyProtocol;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;

import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

public class KiuwanUtils {
	
	private static final Logger LOGGER = Logger.getLogger("com.kiuwan.plugins.kiuwanJenkinsPlugin");
	
	private static final String KIUWAN_CACHE_RELATIVE_PATH = "cache/kiuwan";
	private static final String KIUWAN_TOOLS_RELATIVE_PATH = "tools/kiuwan";
	private static final String KIUWAN_TEMP_RELATIVE_PATH = "temp";
	
	public static Logger logger() {
		return LOGGER;
	}
	
	public static String getRemoteFileAbsolutePath(FilePath filePath, TaskListener listener) throws IOException, InterruptedException {
		String path = filePath.act(new KiuwanRemoteFilePath());
		if (path == null) {
			listener.fatalError("File: " + filePath + " not found.");
		}
		return path;
	}
	
	public static FilePath getNodeJenkinsDirectory(Node node, FilePath workspace) {
		FilePath rootPath = node.getRootPath();
		FilePath nodeJenkinsDir = null;
		if (workspace.isRemote()) {
			nodeJenkinsDir = new FilePath(workspace.getChannel(), rootPath.getRemote());
		} else {
			nodeJenkinsDir = new FilePath(new File(rootPath.getRemote()));
		}

		return nodeJenkinsDir;
	}
	
	public static String getCacheRelativePath(KiuwanConnectionProfile connectionProfile) {
		return getRelativePathForConnectionProfile(KIUWAN_CACHE_RELATIVE_PATH, connectionProfile);
	}
	
	public static String getToolsRelativePath(KiuwanConnectionProfile connectionProfile) {
		return getRelativePathForConnectionProfile(KIUWAN_TOOLS_RELATIVE_PATH, connectionProfile);
	}
	
	public static String getToolsTempRelativePath(KiuwanConnectionProfile connectionProfile) {
		String toolsRelativePath = getRelativePathForConnectionProfile(KIUWAN_TOOLS_RELATIVE_PATH, connectionProfile);
		return toolsRelativePath + "/" + KIUWAN_TEMP_RELATIVE_PATH;
	}
	
	/**
	 * Returns a relative path by concatenating the specified <code>prefix</code> to a safe and unique folder name. 
	 * @param prefix directory prefix
	 * @param connectionProfile the current connection profile
	 * @return the specified prefix followed by the character '_' and the connection profile uuid 
	 */
	private static String getRelativePathForConnectionProfile(String prefix, KiuwanConnectionProfile connectionProfile) {
		return prefix + "_" + connectionProfile.getUuid();
	}

	public static Double parseDouble(String value) {
		Double doubleValue = null;
		try {
			doubleValue = Double.parseDouble(value);
		} catch (Throwable e) {
			logger().log(Level.WARNING, e.getLocalizedMessage());
		}
		return doubleValue;
	}
	
	public static double roundDouble(Double value) {
		BigDecimal valueBigDecimal = new BigDecimal(value);
		valueBigDecimal = valueBigDecimal.setScale(2, RoundingMode.HALF_UP);
		return valueBigDecimal.doubleValue();
	}
	
	public static Set<Integer> parseErrorCodes(String resultCodes) {
		Set<Integer> errorCodes = new HashSet<Integer>();
		if (resultCodes != null) {
			String[] errorCodesAsString = resultCodes.split(",");
			for (String errorCodeAsString : errorCodesAsString) {
				if (StringUtils.isNotEmpty(errorCodeAsString)) {
					errorCodes.add(Integer.parseInt(errorCodeAsString.trim()));
				}
			}
		}
		return errorCodes;
	}

	public static ListBoxModel createListBoxModel(KiuwanModelObject[] data, String selectedValue) {
		ListBoxModel items = new ListBoxModel();
		KiuwanUtils.addAllOptionsToListBoxModel(items, data, selectedValue);
		return items;
	}
	
	public static boolean addAllOptionsToListBoxModel(ListBoxModel items, KiuwanModelObject[] data, String selectedValue) {
		boolean optionFound = false;
		for (KiuwanModelObject kmo : data) {
			String label = kmo.getDisplayName();
			String value = kmo.getValue();
			boolean selected = StringUtils.equalsIgnoreCase(value, selectedValue);
			items.add(new ListBoxModel.Option(label, value, selected));
			optionFound |= selected;
		}
		return optionFound;
	}
	
	public static String buildArgument(Launcher launcher, String argument) {
		return escapeArg(launcher.isUnix(), argument);
	}
	
	public static String buildAdditionalParameterExpression(Launcher launcher, String parameterName, String parameterValue) {
		return escapeArg(launcher.isUnix(), parameterName + "=" + parameterValue);
	}
	
	public static String escapeArg(boolean isUnix, String arg) {
		if (!isUnix) {
			if (arg.contains(" ") || 
				arg.contains("\"") || 
				arg.contains("^") || 
				arg.contains("&") || 
				arg.contains("|") ||
				arg.contains("<") ||
				arg.contains(">")) {
			
				// Replace all \ that precede a " with \\
				StringBuilder sb = new StringBuilder();
				boolean quoteFound = false;
				for (int i = arg.length() - 1; i >= 0; i--) {
					char c = arg.charAt(i);
					if (c != '\\') quoteFound = false;
					if (c == '"') quoteFound = true;
					sb.insert(0, quoteFound && c == '\\' ? "\\\\" : c);
				}
				
				arg = sb.toString();
				
				// Replace " with ""
				arg = arg.replace("\"", "\"\"");
				
				// Quote
				arg = "\"" + arg + "\"";
			}
		}
		
		if (StringUtils.isEmpty(arg)) {
			arg = "\"\"";
		}
		
		return arg;
	}
	
	public static String getCurrentTimestampString() {
		return Long.toHexString(System.currentTimeMillis());
	}
	
	public static AnalysisResult readAnalysisResult(InputStream is) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.IGNORE_UNDEFINED, true);
		mapper.configure(Feature.ALLOW_MISSING_VALUES, true);
		
		AnalysisResult AnalysisResult = mapper.readValue(is, AnalysisResult.class);
		return AnalysisResult;
	}

	public static ProxyConfig getJenkinsProxy(String targetHost) {
		ProxyConfiguration jenkinsProxyConfiguration = null;
		try {
			jenkinsProxyConfiguration = ProxyConfiguration.load();
		} catch (IOException e) {
			logger().severe("Could not retrieve Jenkins proxy configuration: " + e.getLocalizedMessage());
		}
		
		ProxyConfig proxy = null;
		if (jenkinsProxyConfiguration != null) {
			String proxyUsername = jenkinsProxyConfiguration.getUserName();
			String proxyPassword = jenkinsProxyConfiguration.getPassword();
			
			Proxy javaProxy = jenkinsProxyConfiguration.createProxy(targetHost);
			if (javaProxy.address() instanceof InetSocketAddress) {
				InetSocketAddress address = (InetSocketAddress) javaProxy.address();
				String proxyHost = address.getHostString();
				int proxyPort = address.getPort();
				
				if (StringUtils.isEmpty(proxyUsername)) {
					proxy = new ProxyConfig(proxyHost, proxyPort, 
						ProxyProtocol.HTTP, ProxyAuthentication.NONE, 
						null, null);

				} else {
					proxy = new ProxyConfig(proxyHost, proxyPort, 
						ProxyProtocol.HTTP, ProxyAuthentication.BASIC, 
						proxyUsername, proxyPassword);
				}
			}
		}
		
		return proxy;
	}

}
