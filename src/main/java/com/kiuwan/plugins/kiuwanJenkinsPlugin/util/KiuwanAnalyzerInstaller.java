package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;

import hudson.FilePath;
import hudson.Platform;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class KiuwanAnalyzerInstaller {

	private static final String KIUWAN_CLOUD_DOWNLOAD_URL = "https://static.kiuwan.com/download";
	private static final String KIUWAN_LOCAL_ANALYZER_DOWNLOAD_PATH = "/analyzer/KiuwanLocalAnalyzer.zip";
	
	public static final String LOCAL_ANALYZER_PARENT_DIRECTORY = "tools/kiuwan";
	public static final String LOCAL_ANALYZER_DIRECTORY = "KiuwanLocalAnalyzer";
	
	public static FilePath installKiuwanLocalAnalyzer(FilePath rootDir, TaskListener listener, 
			KiuwanConnectionProfile connectionProfile) throws IOException, InterruptedException {
		
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
		
		return agentHome;
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
		String path = getRelativePathForConnectionProfile("cache/kiuwan", connectionProfile);
		File parentDir = new File(rootDir, path);
		return new File(parentDir, fileName);
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
