package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.File;
import java.io.IOException;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci.CiReport;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Callable to dump CI Report data into Jenkins slave file.
 */
public class KiuwanReportCI extends MasterToSlaveFileCallable<Void> {

	private static final long serialVersionUID = -254185915300608702L;

	private CiReport ciReport;

	public KiuwanReportCI(CiReport ciReport) {
		super();
		this.ciReport = ciReport;
	}

	public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		KiuwanUtils.dumpCiReport(this.ciReport, f);
		return null;
	}

}