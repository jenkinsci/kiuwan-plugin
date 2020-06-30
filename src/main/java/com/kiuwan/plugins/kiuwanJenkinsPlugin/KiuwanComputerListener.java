package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.File;
import java.io.IOException;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable.KiuwanRunnable;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import jenkins.model.Jenkins;

@Extension
public class KiuwanComputerListener extends ComputerListener {

	@Override
	public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
		// work around the bug where master doesn't call preOnline method
		if (c.getNode() == Jenkins.getInstance()) {
			process(c, c.getNode().getRootPath(), listener);
		}
	}

	@Override
	public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener)
			throws IOException, InterruptedException {
		process(c, root, listener);
	}

	public void process(Computer c, FilePath root, TaskListener listener) throws IOException, InterruptedException {
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		if (descriptor.getConnectionProfiles() != null) {
			for (KiuwanConnectionProfile connectionProfile : descriptor.getConnectionProfiles()) {
				installKiuwanLocalAnalyzer(connectionProfile, root, listener);
			}
		}
	}

	private void installKiuwanLocalAnalyzer(KiuwanConnectionProfile connectionProfile, FilePath root,
			TaskListener listener) throws IOException, InterruptedException {

		try {
			String installDir = KiuwanUtils.getPathFromConfiguredKiuwanURL(KiuwanRunnable.LOCAL_ANALYZER_PARENT_DIRECTORY, connectionProfile);
			FilePath remoteDir = root.child(installDir);
			if (!remoteDir.child(KiuwanRunnable.LOCAL_ANALYZER_DIRECTORY).exists()) {
				listener.getLogger().println("Installing KiuwanLocalAnalyzer (connection profile = \"" + 
					connectionProfile.getName() + "\") to " + remoteDir);
				File zip = KiuwanUtils.downloadKiuwanLocalAnalyzer(listener, connectionProfile);
				remoteDir.mkdirs();
				new FilePath(zip).unzip(remoteDir);
			}

		} catch (IOException e) {
			listener.error("Failed to install KiuwanAnalyzer: " + e);
			// but continuing
		}
	}

}
