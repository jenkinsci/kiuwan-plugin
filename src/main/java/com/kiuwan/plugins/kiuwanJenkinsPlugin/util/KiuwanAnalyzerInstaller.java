package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.logger;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientException;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientUtils;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.api.InformationApi;
import com.kiuwan.rest.client.model.UserInformationResponse;

import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class KiuwanAnalyzerInstaller {

	private static final String KIUWAN_CLOUD_DOWNLOAD_URL = "https://static.kiuwan.com/download";
	
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_FILE = "/analyzer/KiuwanLocalAnalyzer.zip";
	private static final String KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE = "/analyzer/agent.version";
	
	private static final String KIUWAN_LOCAL_ANALYZER_ENGINE_DOWNLOAD_FILE = "/analyzer/engine_%s.zip";
	private static final String KIUWAN_LOCAL_ANALYZER_ENGINE_VERSION_DOWNLOAD_FILE = "/analyzer/engine.version";
	
	private static final String LOCAL_ANALYZER_DIRECTORY = "KiuwanLocalAnalyzer";
	private static final String ENGINE_DIRECTORY = "engine";
	
	public static String getCurrentKlaVersion(boolean configureKiuwanURL, String kiuwanURL) throws IOException {
		KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile();
		connectionProfile.setConfigureKiuwanURL(configureKiuwanURL);
		connectionProfile.setKiuwanURL(kiuwanURL);
		
		URL klaVersionURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE);
		return downloadToString(klaVersionURL);
	}
	
	public static FilePath installKiuwanLocalAnalyzer(FilePath rootDir, TaskListener listener, 
			KiuwanConnectionProfile connectionProfile) throws IOException, InterruptedException {
		
		logger().info("Begin installation process of KLA and Kiuwan engine for " + connectionProfile);
		
		// 1 - Check current agent.version
		String cachedKlaVersion = null;
		
		URL klaVersionURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE);
		File cachedKlaVersionFile = getLocalCacheFile(klaVersionURL, connectionProfile);
		if (cachedKlaVersionFile != null && cachedKlaVersionFile.exists()) {
			try (InputStream is = new FileInputStream(cachedKlaVersionFile)) {
				cachedKlaVersion = IOUtils.toString(is, StandardCharsets.UTF_8);
				logger().info("Kiuwan Local Analyzer cached version = " + cachedKlaVersion);
			} catch (IOException e) {
				logger().log(Level.INFO, "agent.version not found in cache for profile " + connectionProfile);
			}
		}
		
		boolean klaNeedsUpdate = true;
		if (cachedKlaVersion != null) {
			String remoteKlaVersion = downloadToString(klaVersionURL);
			logger().info("Kiuwan Local Analyzer remote version = " + remoteKlaVersion);
			
			klaNeedsUpdate = !cachedKlaVersion.equals(remoteKlaVersion);
		}
		
		logger().info("Kiuwan Local Analyzer needs update = " + klaNeedsUpdate);
		
		// 2 - Clean cache and store remote Kiuwan Local Analyzer files
		URL klaURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_DOWNLOAD_FILE);
		File cachedKlaFile = getLocalCacheFile(klaURL, connectionProfile);
		if (klaNeedsUpdate) {
			logger().info("Updating cached Kiuwan Local Analyzer from " + klaURL);
			
			// Clean current cache
			cachedKlaVersionFile.delete();
			cachedKlaFile.delete();
			
			// Download version after zip to use it as signal of a successful cache update 
			downloadToFile(klaURL, cachedKlaFile);
			downloadToFile(klaVersionURL, cachedKlaVersionFile);
		}
		
		// 3 - Check current engine.version
		String cachedEngineVersion = null;
		
		URL engineVersionURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_ENGINE_VERSION_DOWNLOAD_FILE);
		File cachedEngineVersionFile = getLocalCacheFile(engineVersionURL, connectionProfile);
		if (cachedEngineVersionFile != null && cachedEngineVersionFile.exists()) { 
			try (InputStream is = new FileInputStream(cachedEngineVersionFile)) {
				cachedEngineVersion = IOUtils.toString(is, StandardCharsets.UTF_8);
				logger().info("Kiuwan Engine cached version = " + cachedEngineVersion);
			} catch (IOException e) {
				logger().log(Level.INFO, "engine.version not found in cache for profile " + connectionProfile);
			}
		}
		
		boolean engineNeedsUpdate = true;
		String remoteEngineVersion = getCustomerEngineVersion(connectionProfile);
		logger().info("Kiuwan Engine remote version = " + remoteEngineVersion);
		
		engineNeedsUpdate = cachedEngineVersion == null || !cachedEngineVersion.equals(remoteEngineVersion);
		logger().info("Kiuwan Engine needs update = " + engineNeedsUpdate);
		
		// 4 - Clean cache and store remote Kiuwan Engine files
		String engineDownloadFileName = String.format(KIUWAN_LOCAL_ANALYZER_ENGINE_DOWNLOAD_FILE, remoteEngineVersion);
		URL engineURL = getDownloadURL(connectionProfile, engineDownloadFileName);
		File engineCacheFile = getLocalCacheFile(engineURL, connectionProfile);
		if (engineNeedsUpdate && remoteEngineVersion != null) {
			logger().info("Updating cached Kiuwan Engine from " + engineURL);
			
			cachedEngineVersionFile.delete();
			engineCacheFile.delete();
			
			// Dump version after engine zip to use it as signal of a successful cache update
			downloadToFile(engineURL, engineCacheFile);
			
			try (OutputStream os = new FileOutputStream(cachedEngineVersionFile)) {
				IOUtils.write(remoteEngineVersion, os, StandardCharsets.UTF_8);
			} catch (IOException e) {
				logger().log(Level.SEVERE, e.getLocalizedMessage());	
			}
		}
		
		// 5 - Install KLA if not already installed
		String toolsRelativePath = KiuwanUtils.getToolsRelativePath(connectionProfile);
		FilePath nodeToolsDir = rootDir.child(toolsRelativePath);
		FilePath engineInstallDir = new FilePath(nodeToolsDir, ENGINE_DIRECTORY);
		FilePath klaHome = nodeToolsDir.child(LOCAL_ANALYZER_DIRECTORY);
		if (!klaHome.exists()) {
			listener.getLogger().println("Installing Kiuwan Local Analyzer for connection profile " + 
				connectionProfile.getDisplayName() + " into " + nodeToolsDir);
			
			try {
				klaHome.mkdirs();
				
				// Install Engine first (this eases the process, as the engine package contains a folder "engine" inside
				// and the FilePath implementation fails when moving all children to destinations that already exist)
				if (engineCacheFile != null && engineCacheFile.exists()) {
					FilePath engineCacheFilePath = new FilePath(engineCacheFile);
					engineCacheFilePath.unzip(nodeToolsDir);
					engineInstallDir.moveAllChildrenTo(klaHome);
				}
				
				// Install KLA
				FilePath klaCacheFilePath = new FilePath(cachedKlaFile);
				klaCacheFilePath.unzip(nodeToolsDir);
				
				// Set permissions to all .sh files in non windows systems
				if (klaHome.mode() != -1) {
					for (FilePath shellScript : klaHome.list("**/*.sh")) {
						listener.getLogger().println("Changing " + shellScript + " permissions");
						shellScript.chmod(0755);
					}
				}
				
			} catch (IOException e) {
				if (engineInstallDir.exists()) {
					try { engineInstallDir.delete(); } catch (IOException ioe) { }
				}
				if (klaHome.exists()) {
					try { klaHome.delete(); } catch (IOException ioe) { }
				}
				throw e;
			}
		}
		
		return klaHome;
	}
	
	private static URL getDownloadURL(KiuwanConnectionProfile connectionProfile, String file) throws MalformedURLException {
		
		// Default URL (Kiuwan cloud)
		String urlStr = null;
		if (!connectionProfile.isConfigureKiuwanURL()) {
			urlStr = KIUWAN_CLOUD_DOWNLOAD_URL;
			
		// Custom Kiuwan URL
		} else {
			URL url = new URL(connectionProfile.getKiuwanURL());
			String urlPort = url.getPort() != -1 ? ":" + url.getPort() : "";
			String baseURL = url.getProtocol() + "://" + url.getHost() + urlPort;
			urlStr = baseURL + "/pub";
		}	
		
		URL downloadBaseURL = new URL(urlStr);
		URL downloadURL = new URL(downloadBaseURL.getProtocol(), downloadBaseURL.getHost(), 
			downloadBaseURL.getPort(), downloadBaseURL.getFile() + file);
		
		return downloadURL;
	}

	private static File getLocalCacheFile(URL src, KiuwanConnectionProfile connectionProfile) throws MalformedURLException {
		String s = src.toExternalForm();
		String fileName = s.substring(s.lastIndexOf('/') + 1);
		File rootDir = Jenkins.getInstance().getRootDir();
		String cacheRelativePath = KiuwanUtils.getCacheRelativePath(connectionProfile);
		File parentDir = new File(rootDir, cacheRelativePath);
		return new File(parentDir, fileName);
	}
	
	private static String getCustomerEngineVersion(KiuwanConnectionProfile connectionProfile) {
		String remoteEngineVersion = null;
		try {
			ApiClient client = KiuwanClientUtils.instantiateClient(
				connectionProfile.isConfigureKiuwanURL(), connectionProfile.getKiuwanURL(),
				connectionProfile.getUsername(), connectionProfile.getPassword(), connectionProfile.getDomain());
			InformationApi infoApi = new InformationApi(client);
			UserInformationResponse userInfo = infoApi.getInformation();
			remoteEngineVersion = userInfo.getEngineVersion();
		
		} catch (ApiException e) {
			KiuwanClientException kce = KiuwanClientException.from(e);
			logger().log(Level.SEVERE, kce.getLocalizedMessage());
		}
		
		return remoteEngineVersion;
	}
	
	private static String downloadToString(URL url) throws IOException {
		String output = null;
		try (
			OutputStream baos = new ByteArrayOutputStream();
			InputStream in = ProxyConfiguration.open(url).getInputStream()
		) {
			output = IOUtils.toString(in, StandardCharsets.UTF_8);
		}
		return output;
	}

	private static void downloadToFile(URL url, File file) throws IOException {
		Path tmpPath = Files.createTempFile("kiuwanJenkinsPlugin-", ".tmp");
		File tmpFile = tmpPath.toFile();
		
		try (InputStream in = ProxyConfiguration.open(url).getInputStream()) {
			Files.copy(in, tmpPath, REPLACE_EXISTING);
			file.getParentFile().mkdirs();
			Files.move(tmpPath, file.toPath(), REPLACE_EXISTING);
			
		} finally {
			tmpFile.delete();
		}
	}

}
