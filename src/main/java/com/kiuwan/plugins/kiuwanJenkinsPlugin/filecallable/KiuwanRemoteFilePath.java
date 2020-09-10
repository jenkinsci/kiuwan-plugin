package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.File;
import java.io.IOException;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/** Callable to install KLA in a Jenkins slave agent if and only if it's not installed yet. */
public class KiuwanRemoteFilePath extends MasterToSlaveFileCallable<String> {

	private static final long serialVersionUID = -3595932272327817085L;

	public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		String path = null;
		if (f.exists()) {
			path = f.getAbsolutePath();
		}

		return path;
	}

}