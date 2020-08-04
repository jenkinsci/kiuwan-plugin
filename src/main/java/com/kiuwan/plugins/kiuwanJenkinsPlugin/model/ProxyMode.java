package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum ProxyMode implements KiuwanModelObject {

	// "false" and "true" values for NONE and CUSTOM options 
	// must be kept to keep backwards compatibility
	
	NONE("No proxy", "false"), 
	JENKINS("Use Jenkins proxy", "jenkins"), 
	CUSTOM("Use custom proxy", "true");
	
	private String displayName;
	private String value;
	
	private ProxyMode(String displayName, String value) {
		this.displayName = displayName;
		this.value = value;
	}
	
	@Override public String getDisplayName() { return this.displayName; }
	@Override public String getValue() { return value; }
	
}