package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.EnvVars;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

public class KiuwanRemoteEnvironment implements FileCallable<EnvVars> {

	private static final long serialVersionUID = 4706152993217221631L;

	public EnvVars invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		return new EnvVars(EnvVars.masterEnvVars);
	}


}