package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.Configuration;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class KiuwanUtils {

	private static final String KIUWAN_ROOT_URL = "https://www.kiuwan.com/saas";
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH = "/pub/analyzer/KiuwanLocalAnalyzer.zip";
	
	private static final String KIUWAN_DOMAIN_HEADER = "X-KW-CORPORATE-DOMAIN-ID";
	
	private static final String SEPARATOR = "_";
	private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder();
	
	/**
	 * Returns a path by concatenating the specified <code>prefix</code> to a safe and 
	 * unique folder name for the kiuwan url in <code>descriptor</code>. When pointing to Kiuwan cloud, 
	 * <code>prefix</code> is returned. 
	 * @param prefix install directory prefix
	 * @param descriptor Kiuwan plugin global configuration
	 * @return This table summarizes different combinations and what will be returned:
	 * <br><br>
	 * <table border="1" summary="Example">
	 * <tr><th>prefix</th><th>kiuwanURL in descriptor</th><th>returned string</th></tr>
	 * <tr><td>tools/kiuwan</td><td></td><td>tools/kiuwan</td></tr>
	 * <tr><td>tools/kiuwan</td><td>http://mykiuwan.domain.com:9090/saas</td><td>tools/kiuwan_mykiuwan.domain.com_9090_L3NhYXM=</td></tr>
	 * <tr><td>cache/Kiuwan</td><td>http://kw.mydomain.com:7777/saas</td><td>cache/Kiuwan_kw.mydomain.com_7777_L3NhYXM=</td></tr>
	 * </table>
	 * @throws MalformedURLException if the configured kiuwanUrl in <code>descriptor</code> is not a valid {@link URL}
	 */
	public static String getPathFromConfiguredKiuwanURL(String prefix, KiuwanDescriptor descriptor) throws MalformedURLException {
		if (!descriptor.isConfigureKiuwanURL()) {
			return prefix;
		}
		
		URL url = new URL(descriptor.getKiuwanURL());
		
		String host = url.getHost();
		int port = url.getPort();
		String path = url.getPath();
		
		StringBuilder sb = new StringBuilder(host);
		
		if (port > 0) {
			sb.append(SEPARATOR + port);
		}
		
		if (StringUtils.isNotBlank(path) && !"/".equals(path)) {
			sb.append(SEPARATOR + BASE64_ENCODER.encodeToString(path.getBytes()));
		}
		
		String installDir = prefix + SEPARATOR + sb;
		
		return installDir;
    }
	
	public static URL getKiuwanLocalAnalyzerDownloadURL(KiuwanDescriptor descriptor) throws MalformedURLException {
		URL url = new URL(descriptor.isConfigureKiuwanURL() ? descriptor.getKiuwanURL() : KIUWAN_ROOT_URL);
		String urlPort = url.getPort() != -1 ? ":" + url.getPort() : "";
		String urlNoPath = url.getProtocol() + "://" + url.getHost() + urlPort;
		
		URL downloadURL = new URL(urlNoPath + KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH);
		return downloadURL;
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
	
	public static String getOutputRelativePath(AbstractBuild<?, ?> build) {
		File outputFile = getOutputFile(build);
		Path outputPath = outputFile.toPath();
		Path outputPathRelative = outputPath.relativize(Jenkins.getInstance().getRootDir().toPath());
		return outputPathRelative.toUri().getPath();
	}
	
	public static double roundDouble(Double value) {
		BigDecimal valueBigDecimnal = new BigDecimal(value);
		valueBigDecimnal = valueBigDecimnal.setScale(2, RoundingMode.HALF_UP);
		return valueBigDecimnal.doubleValue();
	}
	
	public static Set<Integer> parseErrorCodes(String resultCodes) {
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

	public static String buildArgument(Launcher launcher, String argument) {
		return escapeArg(launcher.isUnix(), argument);
	}
	
	public static String buildAdditionalParameterExpression(Launcher launcher, String parameterName, String parameterValue) {
		return escapeArg(launcher.isUnix(), parameterName + "=" + parameterValue);
	}
	
	public static String escapeArg(boolean isUnix, String arg) {
		if (isUnix) return arg;
		
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
		
		return arg;
	}
	
	public static ApiClient instantiateClient(KiuwanDescriptor descriptor) {
		com.kiuwan.rest.client.ApiClient apiClient = ApiClient(descriptor);
		
		String domain = descriptor.getDomain();
		if (StringUtils.isNotBlank(domain)) {
			apiClient.addDefaultHeader(KIUWAN_DOMAIN_HEADER, domain);
		}
		
		return apiClient;
	}
	
	public static AnalysisResult readAnalysisResult(InputStream is) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
	  	mapper.configure(Feature.IGNORE_UNDEFINED, true);
	  	mapper.configure(Feature.ALLOW_MISSING_VALUES, true);
	  	
	  	AnalysisResult AnalysisResult = mapper.readValue(is, AnalysisResult.class);
		return AnalysisResult;
	}

	/**
	 * @param descriptor
	 * @return
	 */
	private static ApiClient ApiClient(KiuwanDescriptor descriptor) {
		return Configuration.newClient(
			descriptor.isConfigureKiuwanURL(), 
			descriptor.getKiuwanURL(), 
			descriptor.getUsername(),
			descriptor.getPassword(),
			descriptor.isConfigureProxy(),
			descriptor.getProxyProtocol(), 
			descriptor.getProxyHost(), 
			descriptor.getProxyPort(), 
			descriptor.getProxyUsername(), 
			descriptor.getProxyPassword());
	}
	
}
