package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci;

import java.io.Serializable;

/**
 * Bean for CI report file
 *
 * @author <a href="mailto:felix.carnicero@kiuwan.com">fcarnicero</a>
 *
 */
public class CiReportFile implements Serializable {

	private static final long serialVersionUID = -7886188925118596514L;
	
	private String file;
	private String modificationType;

	public String getFile() { return file; }
	public void setFile(String file) { this.file = file; }

	public String getModificationType() { return modificationType; }
	public void setModificationType(String modificationType) { this.modificationType = modificationType; }
}
