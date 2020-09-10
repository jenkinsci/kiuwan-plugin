package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum Measure implements KiuwanModelObject {

	QUALITY_INDICATOR("Global indicator"), 
	RISK_INDEX("Risk index"), 
	EFFORT_TO_TARGET("Effort to target"),
	NONE("None - wait for results disabled");
	
	private String displayName;
	
	private Measure(String displayName) {
		this.displayName = displayName;
	}
	
	@Override public String getDisplayName() { return this.displayName; }
	@Override public String getValue() { return name(); }
	
}
