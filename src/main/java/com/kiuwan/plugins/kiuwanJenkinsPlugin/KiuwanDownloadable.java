package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

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

	/** If the package isn't downloaded yet, download it and return its local cache. */
	public File resolve(String osName, String sunArchDataModel, TaskListener listener, KiuwanDescriptor descriptor) throws IOException {
		URL url = KiuwanUtils.getKiuwanLocalAnalyzerDownloadURL(descriptor);
		File f = getLocalCacheFile(url, descriptor);
		if (f.exists()) return f;

		// download to a temporary file and rename it in to handle concurrency
		// and failure correctly,
		listener.getLogger().println("Downloading analyzer... ");
		File tmp = new File(f.getPath() + ".tmp");
		tmp.getParentFile().mkdirs();
		try {
			FileOutputStream out = new FileOutputStream(tmp);
			try {
				IOUtils.copy(ProxyConfiguration.open(url).getInputStream(), out);
			} finally {
				IOUtils.closeQuietly(out);
			}

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
