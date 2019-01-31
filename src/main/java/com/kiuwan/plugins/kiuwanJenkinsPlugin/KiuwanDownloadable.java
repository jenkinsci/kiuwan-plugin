package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.model.DownloadService.Downloadable;
import jenkins.model.Jenkins;

@Extension
public class KiuwanDownloadable extends Downloadable {

	public KiuwanDownloadable() {
		super("com.kiuwan.KiuwanAnalyzer");
	}

	/**
	 * If the package isn't downloaded yet, download it and return its local cache.
	 * @param listener the task listener
	 * @param descriptor current kiuwan descriptor
	 * @return The cached file or the newly downloaded file
	 * @throws IOException if a problem occurs trying to resolve the package to download
	 */
	public File resolve(TaskListener listener, KiuwanDescriptor descriptor) throws IOException {
		URL url = KiuwanUtils.getKiuwanLocalAnalyzerDownloadURL(descriptor);
		File f = getLocalCacheFile(url, descriptor);
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

	private File getLocalCacheFile(URL src, KiuwanDescriptor descriptor) throws MalformedURLException {
		String s = src.toExternalForm();
		String fileName = s.substring(s.lastIndexOf('/') + 1);
		File rootDir = Jenkins.getInstance().getRootDir();
		String path = KiuwanUtils.getPathFromConfiguredKiuwanURL("cache/Kiuwan", descriptor);
		File parentDir = new File(rootDir, path);
		return new File(parentDir, fileName);
	}

}
