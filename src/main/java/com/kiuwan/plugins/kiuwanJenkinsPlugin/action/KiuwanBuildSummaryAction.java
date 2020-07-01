package com.kiuwan.plugins.kiuwanJenkinsPlugin.action;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Action;

@ExportedBean(defaultVisibility = 2)
public class KiuwanBuildSummaryAction implements Action {

	private KiuwanBuildSummaryView summaryView;
	private String icon;

	@DataBoundConstructor
	public KiuwanBuildSummaryAction(KiuwanBuildSummaryView summaryView) {
		super();
		this.summaryView = summaryView;
		this.icon = "/plugin/kiuwanJenkinsPlugin/images/kiuwan-logo.png";
	}

	@Exported public KiuwanBuildSummaryView getSummaryView() { return summaryView; }
	@Exported public String getIcon() { return icon; }

	public String getIconFileName() {
		return summaryView.isAvailable() ? "/plugin/kiuwanJenkinsPlugin/images/kiuwan-sign.png" : null;
	}

	public String getDisplayName() {
		return summaryView.isAvailable() ? "Kiuwan Analysis Results" : "";
	}

	public String getUrlName() {
		return summaryView.isAvailable() ? "kiuwan" : null;
	}

}
