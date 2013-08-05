package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Action;

public class KiuwanLinkAction implements Action {

	private String url, text, icon;

    @DataBoundConstructor
    public KiuwanLinkAction(String applicationName, String analysisLabel) {
    	setUrl(applicationName, analysisLabel);
    	this.text = "Kiuwan results";
    	this.icon = "/plugin/kiuwanJenkinsPlugin/logo.png";
    }

    public String getUrlName() { return url; }
    public String getDisplayName() { return text; }
    public String getIconFileName() { return icon; }
    
    public void setUrl(String applicationName, String analysisLabel) {
    	this.url = "https://kiuwan.com/saas/application?app=" + applicationName + "&label=" + analysisLabel;
    }
    
}
