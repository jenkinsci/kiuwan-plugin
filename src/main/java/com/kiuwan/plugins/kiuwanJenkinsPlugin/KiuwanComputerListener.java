package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerInstaller;
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

	public void process(Computer computer, FilePath root, TaskListener listener) throws IOException, InterruptedException {
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		if (descriptor.getConnectionProfiles() != null) {
			String message = "Checking Kiuwan Local Analyzer installations on node " + computer.getDisplayName();
			KiuwanUtils.logger().log(Level.INFO, message);
			listener.getLogger().println(message);

			List<String> nodeLabels = Arrays.asList(computer.getNode().getLabelString().toLowerCase().split(" "));
			for (KiuwanConnectionProfile connectionProfile : descriptor.getConnectionProfiles()) {
				try {
					if(connectionProfile.isConfigureFilterByLabel()) {
						List<String> profileFilterLabels = Arrays.asList(connectionProfile.getFilterByLabel().toLowerCase().split(","));
						boolean installOnNode = nodeLabels.stream().anyMatch(profileFilterLabels::contains);
						if( installOnNode ) {
							listener.getLogger().println(" *** The agent has a label for KLA installation: '"+connectionProfile.getFilterByLabel()+"' -> installing ...");
							KiuwanAnalyzerInstaller.installKiuwanLocalAnalyzer(root, listener, connectionProfile);
						}
						else {
							listener.getLogger().println(" *** Agent filtered: KLA will not installed on this node");
						}
					}
					else {
						listener.getLogger().println(" *** Filter is disabled for conection profile: "+connectionProfile.getUuid() + " please go to global configuration and set the filter by labels.");
						//KiuwanAnalyzerInstaller.installKiuwanLocalAnalyzer(root, listener, connectionProfile);
					}
					
				} catch (IOException e) {
					String error = "Failed to install Kiuwan Local Analyzer: " + e;
					KiuwanUtils.logger().log(Level.SEVERE, error);
					listener.error(error);
				}
			}
		}
	}

}
