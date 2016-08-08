package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.model.Action;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility=2)
public class KiuwanBuildSummaryAction implements Action {

	private String url;
	
	private String icon;
	
    @DataBoundConstructor
    public KiuwanBuildSummaryAction(String url) {
    	this.url = url;
    	this.icon = "/plugin/kiuwanJenkinsPlugin/logo.png";
    }
    
    /**
	 * @return the icon
	 */
    @Exported
    public String getIcon() {
		return icon;
	}
    
	/**
	 * @return the url
	 */
	@Exported
	public String getUrl() {
		return url;
	}
	
	/**
	 * @see hudson.model.Action#getIconFileName()
	 */
	public String getIconFileName() {
		return "";
	}

	/**
	 * @see hudson.model.Action#getDisplayName()
	 */
	public String getDisplayName() {
		return "";
	}

	/**
	 * @see hudson.model.Action#getUrlName()
	 */
	public String getUrlName() {
		return "";
	}
    
}
