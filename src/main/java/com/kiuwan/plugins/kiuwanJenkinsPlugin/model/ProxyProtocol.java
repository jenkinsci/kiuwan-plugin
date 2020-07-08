package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum ProxyProtocol implements KiuwanModelObject {
	HTTP("HTTP", "http"),
	SOCKS("SOCKS", "socks");
	
	private String displayValue;
	private String value;
	private ProxyProtocol(String displayValue, String value) {
		this.displayValue = displayValue;
		this.value = value;
	}

	@Override public String getDisplayName() { return displayValue; }
	@Override public String getValue() { return value; }
}