package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum ChangeRequestStatusType implements KiuwanModelObject {
	
	RESOLVED("Resolved"), 
	INPROGRESS("In progress");
	
	private String displayName;
	
	private ChangeRequestStatusType(String displayName) {
		this.displayName = displayName;
	}
	
	@Override public String getDisplayName() { return this.displayName; }
	@Override public String getValue() { return name(); }
	
}
