package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.model.DownloadService.Downloadable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;


@Extension
public class KiuwanDownloadable extends Downloadable {
    
	public KiuwanDownloadable() {
        super("com.kiuwan.KiuwanLocalAnalyzer");
    }

    /**
     * If the package isn't downloaded yet, download it and return its local cache.
     */
    public File resolve(String osName, String sunArchDataModel, TaskListener listener) throws IOException {
        URL url = new URL("https://www.kiuwan.com/pub/analyzer/KiuwanLocalAnalyzer.zip");
        File f = getLocalCacheFile(url);
        if (f.exists()) return f;
        
        // download to a temporary file and rename it in to handle concurrency and failure correctly,
        listener.getLogger().println("Downloading analyzer... ");
        File tmp = new File(f.getPath()+".tmp");
        tmp.getParentFile().mkdirs();
        try {
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                IOUtils.copy(ProxyConfiguration.open(url).getInputStream(), out);
            } finally {
                IOUtils.closeQuietly(out);
            }

            tmp.renameTo(f);
            return f;
        } finally {
            tmp.delete();
        }
    }
    
    private File getLocalCacheFile(URL src) {
        String s = src.toExternalForm();
        String fileName = s.substring(s.lastIndexOf('/')+1);
        return new File(Jenkins.getInstance().getRootDir(),"cache/Kiuwan/"+fileName);
    }
    
}
