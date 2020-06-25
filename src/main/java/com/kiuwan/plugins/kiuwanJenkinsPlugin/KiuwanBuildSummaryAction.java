package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;

import hudson.model.Action;

@ExportedBean(defaultVisibility = 2)
public class KiuwanBuildSummaryAction implements Action {

	private AnalysisResult analysisResult;
	private String outputPath;
	private String icon;

	@DataBoundConstructor
	public KiuwanBuildSummaryAction(AnalysisResult analysisResult, String outputPath) {
		super();
		this.analysisResult = analysisResult;
		this.outputPath = outputPath;
		this.icon = "/plugin/kiuwanJenkinsPlugin/logo.png";
	}

	@Exported public AnalysisResult getAnalysisResult() { return analysisResult; }
	@Exported public String getIcon() { return icon; }

	public String getIconFileName() {
		return "";
	}

	public String getDisplayName() {
		return "Kiuwan Analysis Results";
	}

	public String getUrlName() {
		return "/" + outputPath;
	}

}
