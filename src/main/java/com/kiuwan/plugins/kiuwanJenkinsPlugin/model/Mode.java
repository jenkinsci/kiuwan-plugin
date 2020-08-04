package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum Mode implements KiuwanModelObject {

	STANDARD_MODE("Baseline Mode"), 
	DELIVERY_MODE("Delivery Mode"), 
	EXPERT_MODE("Expert Mode");
	
	private String displayName;
	
	private Mode(String displayName) {
		this.displayName = displayName;
	}
	
	@Override public String getDisplayName() { return this.displayName; }
	@Override public String getValue() { return name(); }
	
}