package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.Configuration;
import com.kiuwan.rest.client.api.InformationApi;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public class KiuwanUtils {

	private static final Logger LOGGER = Logger.getLogger("com.kiuwan.plugins.kiuwanJenkinsPlugin");
	
	private static final String KIUWAN_DOMAIN_HEADER = "X-KW-CORPORATE-DOMAIN-ID";
	
	private static final String KIUWAN_CLOUD_DOWNLOAD_URL = "https://static.kiuwan.com/download";
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH = "/analyzer/KiuwanLocalAnalyzer.zip";
	
	public static Logger logger() {
		return LOGGER;
	}
	
	/**
	 * Downloads a Kiuwan Local Analyzer distribution for the specified connection profile. If the file
	 * was already downloaded, the cached file is returned. If not, the distribution is downloaded and cached,
	 * and the cached file is returned.
	 * @param listener the task listener
	 * @param connectionProfile the kiuwan connection profile that contains the data needed to build the download URL
	 * @return The cached file or the newly downloaded file
	 * @throws IOException if a problem occurs trying to resolve the package to download
	 */
	public static File downloadKiuwanLocalAnalyzer(TaskListener listener, KiuwanConnectionProfile connectionProfile) throws IOException {
		URL url = getKiuwanLocalAnalyzerDownloadURL(connectionProfile);
		File cacheFile = getLocalCacheFile(url, connectionProfile);
		if (cacheFile.exists()) return cacheFile;

		// download to a temporary file and rename it in to handle concurrency and failure correctly
		listener.getLogger().println("Downloading Kiuwan Local Analyzer from " + url);
		File tmp = new File(cacheFile.getPath() + ".tmp");
		tmp.getParentFile().mkdirs();
		
		try (InputStream in = ProxyConfiguration.open(url).getInputStream()) {
			Files.copy(in, tmp.toPath(), REPLACE_EXISTING);
			tmp.renameTo(cacheFile);
			
		} finally {
			tmp.delete();
		}
		
		return cacheFile;
	}

	/**
	 * Returns a relative path by concatenating the specified <code>prefix</code> to a safe and unique folder name. 
	 * @param prefix directory prefix
	 * @param connectionProfile the current connection profile
	 * @return the specified prefix followed by the character '_' and the connection profile uuid 
	 */
	public static String getPathFromConfiguredKiuwanURL(String prefix, KiuwanConnectionProfile connectionProfile) {
		return prefix + "_" + connectionProfile.getUuid();
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
	
	public static double roundDouble(Double value) {
		BigDecimal valueBigDecimnal = new BigDecimal(value);
		valueBigDecimnal = valueBigDecimnal.setScale(2, RoundingMode.HALF_UP);
		return valueBigDecimnal.doubleValue();
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
	
	public static ApiClient instantiateClient(KiuwanConnectionProfile connectionProfile) {
		ApiClient apiClient = newApiClient(connectionProfile);
		String domain = connectionProfile.getDomain();
		if (domain != null && !domain.isEmpty()) {
			apiClient.addDefaultHeader(KIUWAN_DOMAIN_HEADER, domain);
		}
		
		return apiClient;
	}
	
	public static FormValidation testConnection(KiuwanConnectionProfile connectionProfile) {
		ApiClient client = instantiateClient(connectionProfile);
		InformationApi api = new InformationApi(client);
		try {
			api.getInformation();
			return FormValidation.ok("Authentication completed successfully!");
		} catch (ApiException kiuwanClientException) {
			return FormValidation.error("Authentication failed: " + kiuwanClientException.getMessage());
		} catch (Throwable throwable) {
			return FormValidation.warning("Could not initiate the authentication process. Reason: " + throwable.getMessage());
		}
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

	private static ApiClient newApiClient(KiuwanConnectionProfile connectionProfile) {
		return Configuration.newClient(
			connectionProfile.isConfigureKiuwanURL(),
			connectionProfile.getKiuwanURL(),
			connectionProfile.getUsername(),
			connectionProfile.getPassword(),
			connectionProfile.isConfigureProxy(),
			connectionProfile.getProxyType().name(),
			connectionProfile.getProxyHost(),
			connectionProfile.getProxyPort(),
			connectionProfile.getProxyUsername(),
			connectionProfile.getProxyPassword());
	}

	private static URL getKiuwanLocalAnalyzerDownloadURL(KiuwanConnectionProfile connectionProfile) throws MalformedURLException {
		URL downloadURL = null;
		
		// Custom Kiuwan URL
		if (connectionProfile.isConfigureKiuwanURL()) {
			URL url = new URL(connectionProfile.getKiuwanURL());
			String urlPort = url.getPort() != -1 ? ":" + url.getPort() : "";
			String baseURL = url.getProtocol() + "://" + url.getHost() + urlPort;
			downloadURL = new URL(baseURL + "/pub" + KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH);
		
		// Default URL (Kiuwan cloud)
		} else {
			downloadURL = new URL(KIUWAN_CLOUD_DOWNLOAD_URL + KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH);
		}
				
		return downloadURL;
	}

	private static File getLocalCacheFile(URL src, KiuwanConnectionProfile connectionProfile) throws MalformedURLException {
		String s = src.toExternalForm();
		String fileName = s.substring(s.lastIndexOf('/') + 1);
		File rootDir = Jenkins.getInstance().getRootDir();
		String path = getPathFromConfiguredKiuwanURL("cache/kiuwan", connectionProfile);
		File parentDir = new File(rootDir, path);
		return new File(parentDir, fileName);
	}
	
}
