package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum Measure {
	QUALITY_INDICATOR("Global indicator"), 
	RISK_INDEX("Risk index"), 
	EFFORT_TO_TARGET("Effort to target"),
	NONE("None - wait for results disabled");
	
	private String label;
	private Measure(String label) { this.label = label; }
	public String getLabel() { return label; }
	
}
