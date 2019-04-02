package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci;

import java.util.List;

public class CiReportBuildInfo {

	private List<String> causes;
	private String displayName;
	private Integer number;
	private String node;
	private String ciSystemName;
	private String ciSystemVersion;

	public List<String> getCauses() { return causes; }
	public void setCauses(List<String> causes) { this.causes = causes; }

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public Integer getNumber() { return number; }
	public void setNumber(Integer number) { this.number = number; }

	public String getNode() { return node; }
	public void setNode(String node) { this.node = node; }

	public String getCiSystemName() { return ciSystemName; }
	public void setCiSystemName(String ciSystemName) { this.ciSystemName = ciSystemName; }

	public String getCiSystemVersion() { return ciSystemVersion; }
	public void setCiSystemVersion(String ciSystemVersion) { this.ciSystemVersion = ciSystemVersion; }

}
