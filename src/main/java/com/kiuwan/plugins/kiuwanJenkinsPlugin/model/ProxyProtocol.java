package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum ProxyProtocol implements KiuwanModelObject {
	HTTP("http"),
	SOCKS("socks");
	
	private String value;
	private ProxyProtocol(String value) {
		this.value = value;
	}

	@Override public String getDisplayName() { return getValue(); }
	@Override public String getValue() { return value; }
}