package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.File;
import java.io.IOException;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

public class KiuwanRemoteFile extends MasterToSlaveFileCallable<File> {

	private static final long serialVersionUID = 1726113974129361646L;

	public File invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		return f;
	}

}