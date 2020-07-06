package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.logger;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.api.InformationApi;
import com.kiuwan.rest.client.model.UserInformationResponse;

import hudson.FilePath;
import hudson.Platform;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class KiuwanAnalyzerInstaller {

	private static final String KIUWAN_CLOUD_DOWNLOAD_URL = "https://static.kiuwan.com/download";
	
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_FILE = "/analyzer/KiuwanLocalAnalyzer.zip";
	private static final String KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE = "/analyzer/agent.version";
	
	private static final String KIUWAN_LOCAL_ANALYZER_ENGINE_DOWNLOAD_FILE = "/analyzer/engine_%s.zip";
	private static final String KIUWAN_LOCAL_ANALYZER_ENGINE_VERSION_DOWNLOAD_FILE = "/analyzer/engine.version";
	
	public static final String LOCAL_ANALYZER_PARENT_DIRECTORY = "tools/kiuwan";
	public static final String LOCAL_ANALYZER_DIRECTORY = "KiuwanLocalAnalyzer";
	
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
			logger().info("Updating cached Kiuwan Local Analyzer from" + klaURL);
			cachedKlaVersionFile.delete();
			cachedKlaFile.delete();
			downloadToFile(klaVersionURL, cachedKlaVersionFile);
			downloadToFile(klaURL, cachedKlaFile);
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
		String remoteEngineVersion = null;
		try {
			ApiClient client = KiuwanUtils.instantiateClient(
				connectionProfile.isConfigureKiuwanURL(), connectionProfile.getKiuwanURL(),
				connectionProfile.getUsername(), connectionProfile.getPassword(), connectionProfile.getDomain());
			InformationApi infoApi = new InformationApi(client);
			UserInformationResponse userInfo = infoApi.getInformation();
			remoteEngineVersion = userInfo.getEngineVersion();
		} catch (ApiException e) {
			logger().log(Level.SEVERE, e.getLocalizedMessage());
		}
		
		logger().info("Kiuwan Engine remote version = " + remoteEngineVersion);
		
		engineNeedsUpdate = cachedEngineVersion == null || !cachedEngineVersion.equals(remoteEngineVersion);
		logger().info("Kiuwan Engine needs update = " + engineNeedsUpdate);
		
		// 4 - Clean cache and store remote Kiuwan Engine files
		File engineCacheFile = null;
		if (engineNeedsUpdate && remoteEngineVersion != null) {
			String engineDownloadFileName = String.format(KIUWAN_LOCAL_ANALYZER_ENGINE_DOWNLOAD_FILE, remoteEngineVersion);
			URL engineURL = getDownloadURL(connectionProfile, engineDownloadFileName);
			engineCacheFile = getLocalCacheFile(engineURL, connectionProfile);
			if (engineNeedsUpdate) {
				logger().info("Updating cached Kiuwan Engine from " + engineURL);
				cachedEngineVersionFile.delete();
				engineCacheFile.delete();
				downloadToFile(engineVersionURL, cachedEngineVersionFile);
				downloadToFile(engineURL, engineCacheFile);
			}
		}
		
		// 5 - Install KLA if not already installed
		String profileRelativePath = getRelativePathForConnectionProfile(LOCAL_ANALYZER_PARENT_DIRECTORY, connectionProfile);
		FilePath nodeToolsDir = rootDir.child(profileRelativePath);
		FilePath klaHome = nodeToolsDir.child(LOCAL_ANALYZER_DIRECTORY);
		if (!klaHome.exists()) {
			listener.getLogger().println("Installing Kiuwan Local Analyzer for connection profile " + 
				connectionProfile.getDisplayName() + " to " + nodeToolsDir);
			
			klaHome.mkdirs();
			
			// Install Engine first (this eases the process, as the engine package contains a folder "engine" inside
			// and the FilePath implementation fails when moving all children to destinations that already exist)
			if (engineCacheFile != null && engineCacheFile.exists()) {
				FilePath engineCacheFilePath = new FilePath(engineCacheFile);
				engineCacheFilePath.unzip(nodeToolsDir);
				
				FilePath engineInstallDir = new FilePath(nodeToolsDir, "engine");
				engineInstallDir.moveAllChildrenTo(klaHome);
			}
			
			// Install KLA
			FilePath klaCacheFilePath = new FilePath(cachedKlaFile);
			klaCacheFilePath.unzip(nodeToolsDir);
			
			// Set permissions to all .sh files
			if (isUnix()) {
				for (FilePath shellScript : klaHome.list("*.sh")) {
					listener.getLogger().println("Changing " + shellScript + " permissions");
					shellScript.chmod(0755);
				}
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
		String path = getRelativePathForConnectionProfile("cache/kiuwan", connectionProfile);
		File parentDir = new File(rootDir, path);
		return new File(parentDir, fileName);
	}
	
	private static String downloadToString(URL url) throws IOException {
		String output = null;
		try (
			OutputStream baos = new ByteArrayOutputStream();
			InputStream in = ProxyConfiguration.open(url).getInputStream()
		) {
			output = IOUtils.toString(in);
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

	/**
	 * Returns a relative path by concatenating the specified <code>prefix</code> to a safe and unique folder name. 
	 * @param prefix directory prefix
	 * @param connectionProfile the current connection profile
	 * @return the specified prefix followed by the character '_' and the connection profile uuid 
	 */
	private static String getRelativePathForConnectionProfile(String prefix, KiuwanConnectionProfile connectionProfile) {
		return prefix + "_" + connectionProfile.getUuid();
	}
	
	private static boolean isUnix() {
		boolean isUnix = false;
		try {
			isUnix = (Platform.WINDOWS != Platform.current());
		} catch (Exception e) {
			KiuwanUtils.logger().warning("Could not get current platform: " + e.getLocalizedMessage());
		}
		return isUnix;
	}
	
}
