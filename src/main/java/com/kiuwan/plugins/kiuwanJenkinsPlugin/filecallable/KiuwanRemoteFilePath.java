package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

public class KiuwanRemoteFilePath implements FileCallable<String> {

	private static final long serialVersionUID = -3595932272327817085L;

	public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		String path = null;
		if (f.exists()) {
			path = f.getAbsolutePath();
		}

		return path;
	}

}