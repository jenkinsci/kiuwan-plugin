package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum ProxyAuthentication implements KiuwanModelObject {
	NONE("None"),
	BASIC("Basic");
	
	private String value;
	private ProxyAuthentication(String value) {
		this.value = value;
	}

	@Override public String getDisplayName() { return getValue(); }
	@Override public String getValue() { return value; }
}