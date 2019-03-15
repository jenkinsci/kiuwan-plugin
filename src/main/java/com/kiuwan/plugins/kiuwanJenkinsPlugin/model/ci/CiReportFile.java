package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci;

/**
 * Bean for CI report file
 *
 * @author <a href="mailto:felix.carnicero@kiuwan.com">fcarnicero</a>
 *
 */
public class CiReportFile {

	private String file;
	private String modificationType;

	public String getFile() { return file; }
	public void setFile(String file) { this.file = file; }

	public String getModificationType() { return modificationType; }
	public void setModificationType(String modificationType) { this.modificationType = modificationType; }
}
