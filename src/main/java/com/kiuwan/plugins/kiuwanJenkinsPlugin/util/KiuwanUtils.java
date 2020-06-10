package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.Configuration;
import com.kiuwan.rest.client.api.InformationApi;

import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public class KiuwanUtils {

	private static final String KIUWAN_CLOUD_DOWNLOAD_URL = "https://static.kiuwan.com/download";
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH = "/analyzer/KiuwanLocalAnalyzer.zip";
	
	private static final String KIUWAN_DOMAIN_HEADER = "X-KW-CORPORATE-DOMAIN-ID";
	
	/**
	 * If the package isn't downloaded yet, download it and return its local cache.
	 * @param listener the task listener
	 * @param descriptor current kiuwan descriptor
	 * @return The cached file or the newly downloaded file
	 * @throws IOException if a problem occurs trying to resolve the package to download
	 */
	public static File resolve(TaskListener listener, KiuwanConnectionProfile connectionProfile) throws IOException {
		URL url = KiuwanUtils.getKiuwanLocalAnalyzerDownloadURL(connectionProfile);
		File f = getLocalCacheFile(url, connectionProfile);
		if (f.exists()) return f;

		// download to a temporary file and rename it in to handle concurrency
		// and failure correctly,
		listener.getLogger().println("Downloading analyzer... ");
		File tmp = new File(f.getPath() + ".tmp");
		tmp.getParentFile().mkdirs();
		try (InputStream in = ProxyConfiguration.open(url).getInputStream()) {
			Files.copy(in, tmp.toPath(), REPLACE_EXISTING);
			tmp.renameTo(f);
		} finally {
			tmp.delete();
		}
		
		return f;
	}

	private static File getLocalCacheFile(URL src, KiuwanConnectionProfile connectionProfile) throws MalformedURLException {
		String s = src.toExternalForm();
		String fileName = s.substring(s.lastIndexOf('/') + 1);
		File rootDir = Jenkins.getInstance().getRootDir();
		String path = KiuwanUtils.getPathFromConfiguredKiuwanURL("cache/kiuwan", connectionProfile);
		File parentDir = new File(rootDir, path);
		return new File(parentDir, fileName);
	}
	
	/**
	 * Returns a path by concatenating the specified <code>prefix</code> to a safe and unique folder name. 
	 * @param prefix install directory prefix
	 * @param connectionProfile the current connection profile
	 * @return the specified prefix followed by the character '_' and the connection profile uuid 
	 */
	public static String getPathFromConfiguredKiuwanURL(String prefix, KiuwanConnectionProfile connectionProfile) {
		return prefix + "_" + connectionProfile.getUuid();
    }
	
	public static URL getKiuwanLocalAnalyzerDownloadURL(KiuwanConnectionProfile connectionProfile) throws MalformedURLException {
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
	
	public static String getRemoteFileAbsolutePath(FilePath filePath, TaskListener listener) throws IOException, InterruptedException {
		String path = filePath.act(new KiuwanRemoteFilePath());
		if (path == null) {
			listener.fatalError("File: " + filePath + " not found.");
		}
		return path;
	}
	
	public static Set<Integer> parseErrorCodes(String resultCodes) {
		Set<Integer> errorCodes = new HashSet<Integer>();
		if(resultCodes != null){
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
			return FormValidation.error("Authentication failed.");
		} catch (Throwable throwable) {
			return FormValidation.warning("Could not initiate the authentication process. Reason: " + throwable);
		}
	}

	private static ApiClient newApiClient(KiuwanConnectionProfile connectionProfile) {
		return Configuration.newClient(
			connectionProfile.isConfigureKiuwanURL(),
			connectionProfile.getKiuwanURL(),
			connectionProfile.getUsername(),
			connectionProfile.getPassword(),
			connectionProfile.isConfigureProxy(),
			connectionProfile.getProxyProtocol(),
			connectionProfile.getProxyHost(),
			connectionProfile.getProxyPort(),
			connectionProfile.getProxyUsername(),
			connectionProfile.getProxyPassword());
	}
	
}
