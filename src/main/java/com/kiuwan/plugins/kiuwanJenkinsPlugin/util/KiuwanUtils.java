package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable.KiuwanRemoteFilePath;
import com.kiuwan.rest.client.ApiClient;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import hudson.FilePath;
import hudson.model.TaskListener;

public class KiuwanUtils {

	private static final String KIUWAN_ROOT_URL = "https://www.kiuwan.com/saas";
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH = "/pub/analyzer/KiuwanLocalAnalyzer.zip";
	
	private static final String SEPARATOR = "_";
	private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder();
	
	/**
	 * Returns a path by concatenating the specified <code>prefix</code> to a safe and 
	 * unique folder name for the kiuwan url in <code>descriptor</code>. When pointing to Kiuwan cloud, 
	 * <code>prefix</code> is returned. 
	 * @param prefix install directory prefix
	 * @param descriptor Kiuwan plugin global configuration
	 * @return This table summarizes different combinations and what will be returned:
	 * <p><table border="1">
	 * <tr><th>prefix</th><th>kiuwanURL in descriptor</th><th>returned string</th></tr>
	 * <tr><td>tools/kiuwan</td><td></td><td>tools/kiuwan</td></tr>
	 * <tr><td>tools/kiuwan</td><td>http://mykiuwan.domain.com:9090/saas</td><td>tools/kiuwan_mykiuwan.domain.com_9090_L3NhYXM=</td></tr>
	 * <tr><td>cache/Kiuwan</td><td>http://kw.mydomain.com:7777/saas</td><td>cache/Kiuwan_kw.mydomain.com_7777_L3NhYXM=</td></tr>
	 * </table>
	 * @throws MalformedURLException
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
	
	public static ApiClient instantiateClient(KiuwanDescriptor descriptor) {
		String username = descriptor.getUsername();
		String password = descriptor.getPassword();
		
		
		boolean configureProxy = descriptor.isConfigureProxy();
		String proxyHost = descriptor.getProxyHost(); 
		int proxyPort = descriptor.getProxyPort(); 
		String proxyAuthentication = descriptor.getProxyAuthentication(); 
		String proxyUsername = descriptor.getProxyUsername();
		String proxyPassword = descriptor.getProxyPassword();
		
		String restURL = getKiuwanURLRest(descriptor);
		ApiClient client = new ApiClient();
		client.setBasePath(restURL);
		
		// Set user / password
		client.setUsername(username);
		client.setPassword(password);
		
		// Proxy configuration (optional)
		if (configureProxy && StringUtils.isNotBlank(proxyHost)) {
			Type proxyType = descriptor.getProxyType();
			if (proxyType == null) {
				throw new IllegalArgumentException("Unsupported proxy protocol found: " + descriptor.getProxyProtocol());
			}
			Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort));
			client.getHttpClient().setProxy(proxy);
			
			// Set new authenticator if needed for proxy
			if (KiuwanDescriptor.PROXY_AUTHENTICATION_BASIC.equals(proxyAuthentication)) {
				Authenticator authenticator = getClientAuthenticator(username, password, proxyUsername, proxyPassword);
				client.getHttpClient().setAuthenticator(authenticator);
			}
		}
		
		return client;
	}
	
	private static String getKiuwanURLRest(KiuwanDescriptor descriptor) {
		String baseURL = null;
		if (!descriptor.isConfigureKiuwanURL()) {
			baseURL = KIUWAN_ROOT_URL;
		} else {
			baseURL = descriptor.getKiuwanURL();
			if (baseURL.endsWith("/")) {
				baseURL = baseURL.substring(0, baseURL.lastIndexOf("/"));
			}
		}
		
		return baseURL + "/rest";
	}

	private static Authenticator getClientAuthenticator(String username, String password, String proxyUsername, String proxyPassword) {
		return new Authenticator() {
			
			@Override
			public Request authenticate(Proxy proxy, Response response) throws IOException {
				if (response.request().header("Authorization") != null) {
					return null; // Give up, we've already failed to authenticate.
				}
				String credential = Credentials.basic(username, password);
				return response.request().newBuilder().header("Authorization", credential).build();
			}

			@Override
			public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
				if (response.request().header("Proxy-Authorization") != null) {
					return null; // Give up, we've already failed to authenticate.
				}
				String credential = Credentials.basic(proxyUsername, proxyPassword);
				return response.request().newBuilder().header("Proxy-Authorization", credential).build();
			}
			
		};
	}
	
}
