package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.model.Action;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 2)
public class KiuwanBuildSummaryAction implements Action {

	private String url;
	private String icon;

	@DataBoundConstructor
	public KiuwanBuildSummaryAction(String url) {
		this.url = url;
		this.icon = "/plugin/kiuwanJenkinsPlugin/logo.png";
	}

	@Exported
	public String getIcon() {
		return icon;
	}

	@Exported
	public String getUrl() {
		return url;
	}

	public String getIconFileName() {
		return "";
	}

	public String getDisplayName() {
		return "";
	}

	public String getUrlName() {
		return "";
	}

}
