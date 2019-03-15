package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.File;
import java.io.IOException;

import hudson.EnvVars;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Callable to provide Jenkins slave agents with Jenkins master environment variables.
 */
public class KiuwanRemoteEnvironment extends MasterToSlaveFileCallable<EnvVars> {

	private static final long serialVersionUID = 4706152993217221631L;

	public EnvVars invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		return new EnvVars(EnvVars.masterEnvVars);
	}

}