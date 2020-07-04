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
		String klaVersionCacheContents = null;
		
		URL klaVersionURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE);
		File klaVersionCacheFile = getLocalCacheFile(klaVersionURL, connectionProfile);
		try (InputStream is = new FileInputStream(klaVersionCacheFile)) {
			klaVersionCacheContents = klaVersionCacheFile != null ? IOUtils.toString(is, StandardCharsets.UTF_8) : null;
		}
		
		logger().info("Kiuwan Local Analyzer cached version = " + klaVersionCacheContents);
		
		boolean klaNeedsUpdate = true;
		if (klaVersionCacheContents != null) {
			String klaVersionContents = downloadToString(klaVersionURL);
			logger().info("Kiuwan Local Analyzer remote version = " + klaVersionContents);
			
			klaNeedsUpdate = klaVersionCacheContents.equals(klaVersionContents);
		}
		
		logger().info("Kiuwan Local Analyzer needs update = " + klaNeedsUpdate);
		
		// 2 - Clean cache and store remote Kiuwan Local Analyzer files
		URL klaURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_DOWNLOAD_FILE);
		File klaCacheFile = getLocalCacheFile(klaURL, connectionProfile);
		if (klaNeedsUpdate) {
			logger().info("Updating Kiuwan Local Analyzer from" + klaURL);
			klaVersionCacheFile.delete();
			klaCacheFile.delete();
			downloadToFile(klaVersionURL, klaVersionCacheFile);
			downloadToFile(klaURL, klaCacheFile);
		}
		
		// 3 - Check current engine.version
		String engineVersionCacheContents = null;
		
		URL engineVersionURL = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_ENGINE_VERSION_DOWNLOAD_FILE);
		File engineVersionCacheFile = getLocalCacheFile(engineVersionURL, connectionProfile);
		try (InputStream is = new FileInputStream(engineVersionCacheFile)) {
			engineVersionCacheContents = engineVersionCacheFile != null ? IOUtils.toString(is, StandardCharsets.UTF_8) : null;
		}
		
		logger().info("Kiuwan Engine cached version = " + engineVersionCacheContents);
		
		boolean engineNeedsUpdate = true;
		String engineVersionContents = null;
		if (engineVersionCacheContents != null) {
			try {
				ApiClient client = KiuwanUtils.instantiateClient(
					connectionProfile.isConfigureKiuwanURL(), connectionProfile.getKiuwanURL(),
					connectionProfile.getUsername(), connectionProfile.getPassword(), connectionProfile.getDomain());
				InformationApi infoApi = new InformationApi(client);
				UserInformationResponse userInfo = infoApi.getInformation();
				engineVersionContents = userInfo.getEngineVersion();
			} catch (ApiException e) {
				logger().log(Level.SEVERE, e.getLocalizedMessage());
			}
			logger().info("Kiuwan Engine remote version = " + engineVersionContents);
			
			engineNeedsUpdate = engineVersionCacheContents.equals(engineVersionContents);
		}
		
		logger().info("Kiuwan Engine needs update = " + engineNeedsUpdate);
		
		// 4 - Clean cache and store remote Kiuwan Engine files
		String engineDownloadFileName = String.format(KIUWAN_LOCAL_ANALYZER_ENGINE_DOWNLOAD_FILE, engineVersionContents);
		URL kiuwanEngineURL = getDownloadURL(connectionProfile, engineDownloadFileName);
		File kiuwanEngineCacheFile = getLocalCacheFile(kiuwanEngineURL, connectionProfile);
		if (engineNeedsUpdate) {
			logger().info("Updating Kiuwan Engine from" + kiuwanEngineURL);
			engineVersionCacheFile.delete();
			kiuwanEngineCacheFile.delete();
			downloadToFile(engineVersionURL, engineVersionCacheFile);
			downloadToFile(kiuwanEngineURL, kiuwanEngineCacheFile);
		}

		// 5 - Install KLA if not already installed
		String relativePath = getRelativePathForConnectionProfile(LOCAL_ANALYZER_PARENT_DIRECTORY, connectionProfile);
		FilePath installDir = rootDir.child(relativePath);
		FilePath klaHome = installDir.child(LOCAL_ANALYZER_DIRECTORY);
		if (!klaHome.exists()) {
			listener.getLogger().println("Installing Kiuwan Local Analyzer (connection profile = \"" + 
				connectionProfile.getName() + "\") to " + installDir);
			installDir.mkdirs();
			
			FilePath klaCacheFilePath = new FilePath(klaCacheFile);
			FilePath kiuwanEngineCacheFilePath = new FilePath(kiuwanEngineCacheFile);
			
			klaCacheFilePath.unzip(installDir);
			// TODO: ignore engine folder
			kiuwanEngineCacheFilePath.unzip(installDir);
			
			// TODO: set permissions to all .sh
			if (isUnix()) {
				FilePath klaBinDir = klaHome.child("bin");
				listener.getLogger().println("Changing agent.sh permission");
				klaBinDir.child("agent.sh").chmod(0755);
			}
		}
		
		/*
		String relativePath = getRelativePathForConnectionProfile(LOCAL_ANALYZER_PARENT_DIRECTORY, connectionProfile);
		FilePath installDir = rootDir.child(relativePath);
		FilePath agentHome = installDir.child(LOCAL_ANALYZER_DIRECTORY);
		if (!agentHome.exists()) {
			listener.getLogger().println("Installing KiuwanLocalAnalyzer (connection profile = \"" + 
				connectionProfile.getName() + "\") to " + installDir);
			File zip = downloadKiuwanLocalAnalyzer(listener, connectionProfile);
			installDir.mkdirs();
			new FilePath(zip).unzip(installDir);
			
			if (isUnix()) {
				FilePath agentBinDir = agentHome.child("bin");
				listener.getLogger().println("Changing agent.sh permission");
				agentBinDir.child("agent.sh").chmod(0755);
			}
		}
		*/
		
		return klaHome;
	}
	
	private static File downloadKiuwanLocalAnalyzerCurrentVersion(TaskListener listener, KiuwanConnectionProfile connectionProfile) 
			throws IOException {
		
		URL url = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_VERSION_DOWNLOAD_FILE);
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
	 * Downloads a Kiuwan Local Analyzer distribution for the specified connection profile. If the file
	 * was already downloaded, the cached file is returned. If not, the distribution is downloaded and cached,
	 * and the cached file is returned.
	 * @param listener the task listener
	 * @param connectionProfile the kiuwan connection profile that contains the data needed to build the download URL
	 * @return The cached file or the newly downloaded file
	 * @throws IOException if a problem occurs trying to resolve the package to download
	 */
	private static File downloadKiuwanLocalAnalyzer(TaskListener listener, KiuwanConnectionProfile connectionProfile) throws IOException {
		URL url = getDownloadURL(connectionProfile, KIUWAN_LOCAL_ANALYZER_DOWNLOAD_FILE);
		
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
			downloadBaseURL.getPort(), file);
		
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
	
	private static void downloadToFile(URL url, File file) throws IOException {
		Path tmpPath = Files.createTempFile("kiuwanJenkinsPlugin-", ".tmp");
		File tmpFile = tmpPath.toFile();
		tmpFile.getParentFile().mkdirs();
		
		try (InputStream in = ProxyConfiguration.open(url).getInputStream()) {
			Files.copy(in, tmpPath, REPLACE_EXISTING);
			tmpFile.renameTo(file);
			
		} finally {
			tmpFile.delete();
		}
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
