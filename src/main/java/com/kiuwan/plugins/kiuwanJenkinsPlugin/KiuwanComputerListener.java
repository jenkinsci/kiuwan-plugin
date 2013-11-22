package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import jenkins.model.Jenkins;

@Extension
public class KiuwanComputerListener extends ComputerListener {

    public static final String INSTALL_DIR = "tools/kiuwan";
	
    public static final String AGENT_HOME = "KiuwanLocalAnalyzer";
    
    @Inject
    private KiuwanDownloadable kiuwanDownloadable;

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        if (c.getNode()== Jenkins.getInstance())    // work around the bug where master doesn't call preOnline method
            process(c, c.getNode().getRootPath(), listener);
    }

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        process(c, root,listener);
    }

    public void process(Computer c, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        try {
            FilePath remoteDir = root.child(INSTALL_DIR);
            if (!remoteDir.child(AGENT_HOME).exists()) {
                listener.getLogger().println("Installing KiuwanLocalAnalyzer to "+remoteDir);
                Map<Object,Object> props = c.getSystemProperties();
                File zip = kiuwanDownloadable.resolve((String) props.get("os.name"), (String) props.get("sun.arch.data.model"),listener);
                remoteDir.mkdirs();
                new FilePath(zip).unzip(remoteDir);
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to install kiuwanlocalanalyzer"));
            // but continuing
        }
    }

}
