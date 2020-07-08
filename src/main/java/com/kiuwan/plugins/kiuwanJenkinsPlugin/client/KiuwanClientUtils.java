package com.kiuwan.plugins.kiuwanJenkinsPlugin.client;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyAuthentication;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyConfig;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.Configuration;

public class KiuwanClientUtils {

	/**
	 * Creates a Kiuwan API client object using the current Jenkins proxy configuration and the specified connection settings
	 * @return a {@link ApiClient} object ready to use
	 */
	public static ApiClient instantiateClient(boolean isConfigureKiuwanURL, String kiuwanURL, 
			String username, String password, String domain) {
		
		if (isConfigureKiuwanURL)  {
			try {
				new URL(kiuwanURL);
			} catch (MalformedURLException e) {
				throw new RuntimeException("Cannot instantiate client - Kiuwan server URL is not valid (" + 
					e.getLocalizedMessage() + ")", e);
			}
		}
		
		ApiClient apiClient = Configuration
			.newClient(isConfigureKiuwanURL, kiuwanURL)
			.withBasicAuthentication(username, password)
			.withDomain(domain);
		
		URL basePathURL = null;
		try {
			basePathURL = new URL(apiClient.getBasePath());
		} catch (MalformedURLException e) {
			logger().log(Level.WARNING, "Could not create URL to setup proxy: " + e.getLocalizedMessage());
		}
		
		if (basePathURL != null) {
			ProxyConfig proxyConfig = KiuwanUtils.getJenkinsProxy(basePathURL.getHost());
			
			if (proxyConfig != null) {
				if (ProxyAuthentication.NONE.equals(proxyConfig.getAuthentication())) {
					apiClient = apiClient.withProxy(proxyConfig.getJavaProxyType().name(), 
						proxyConfig.getHost(), proxyConfig.getPort());
				
				} else {
					apiClient = apiClient.withProxy(proxyConfig.getJavaProxyType().name(), 
						proxyConfig.getHost(), proxyConfig.getPort(), 
						proxyConfig.getUsername(), proxyConfig.getPassword());
				}
			}
		}
		
		return apiClient;
	}
	
}
