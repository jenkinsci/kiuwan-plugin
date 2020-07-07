package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;

public class ProxyConfig {

	public static final String PROTOCOL_HTTP = ProxyProtocol.HTTP.getValue();
	public static final String AUTHENTICATION_NONE = ProxyAuthentication.NONE.getValue();
	public static final String AUTHENTICATION_BASIC = ProxyAuthentication.BASIC.getValue();
	
	public static final int PORT_DEFAULT = KiuwanConnectionProfile.DEFAULT_PROXY_PORT;
	
	private String host;
	private int port;
	private String protocol;
	private String authentication;
	private String username;
	private String password;
	
	public ProxyConfig(String host, int port, String protocol, String authentication, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.authentication = authentication;
		this.username = username;
		this.password = password;
	}
	
	public String getHost() { return host; }
	public int getPort() { return port;	}
	public String getProtocol() { return protocol; }
	public String getAuthentication() { return authentication; }
	public String getUsername() { return username; }
	public String getPassword() { return password; }
	
}
