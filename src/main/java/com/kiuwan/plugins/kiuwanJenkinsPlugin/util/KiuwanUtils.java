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

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.KiuwanModelObject;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.Configuration;

import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

public class KiuwanUtils {
	
	private static final Logger LOGGER = Logger.getLogger("com.kiuwan.plugins.kiuwanJenkinsPlugin");

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

	/** 
	 * Returns the location at the master node for the KLA output file:
	 * $JENKINS_HOME/jobs/$JOBNAME/builds/#BUILD/kiuwan/output.json
	 * @param build the current build object
	 * @return the file location
	 */
	public static File getOutputFile(AbstractBuild<?, ?> build) {
		return new File(build.getRootDir(), "kiuwan/output.json");
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
				if (errorCodeAsString != null && !errorCodeAsString.isEmpty()) {
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
			boolean selected = Objects.equal(value, selectedValue);
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
		
		if (arg == null || arg.isEmpty()) {
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

	/**
	 * Creates a Kiuwan API client object using the current Jenkins proxy configuration and the specified connection settings
	 * @return a {@link ApiClient} object ready to use
	 */
	public static ApiClient instantiateClient(boolean isConfigureKiuwanURL, String kiuwanURL, 
			String username, String password, String domain) {
		
		ApiClient apiClient = Configuration
			.newClient(isConfigureKiuwanURL, kiuwanURL)
			.withBasicAuthentication(username, password)
			.withDomain(domain);
		
		ProxyConfiguration jenkinsProxy = null;
		try {
			jenkinsProxy = ProxyConfiguration.load();
		} catch (IOException e) {
			logger().severe("Could not retrieve Jenkins proxy configuration: " + e.getLocalizedMessage());
		}
		
		if (jenkinsProxy != null && !Proxy.NO_PROXY.equals(jenkinsProxy)) {
			String proxyUsername = jenkinsProxy.getUserName();
			String proxyPassword = jenkinsProxy.getPassword();
			
			String basePath = apiClient.getBasePath();
			Proxy javaProxy = jenkinsProxy.createProxy(basePath);
			if (javaProxy.address() instanceof InetSocketAddress) {
				InetSocketAddress address = (InetSocketAddress) javaProxy.address();
				String proxyHost = address.getHostName();
				int proxyPort = address.getPort();

				if (jenkinsProxy.getUserName() == null || jenkinsProxy.getUserName().isEmpty()) {
					apiClient = apiClient.withProxy(Proxy.Type.HTTP.name(), proxyHost, proxyPort);
				} else {
					apiClient = apiClient.withProxy(Proxy.Type.HTTP.name(), proxyHost, proxyPort, proxyUsername, proxyPassword);
				}
			}
			
		}
		
		return apiClient;
	}

}
