package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

import java.net.Proxy.Type;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

public class ProxyConfig {

	/** The default port */
	public static final int PORT_DEFAULT = 3128;
	
	/** An empty proxy config */
	public static final ProxyConfig EMPTY = new ProxyConfig(
		"", ProxyConfig.PORT_DEFAULT, 
		ProxyProtocol.HTTP, ProxyAuthentication.NONE, 
		"", "");
	
	private String host;
	private int port;
	private ProxyProtocol protocol;
	private ProxyAuthentication authentication;
	private String username;
	private String password;
	
	public ProxyConfig(String host, int port, String protocolStr, 
			String authenticationStr, String username, String password) {
		this(host, port, parseProtocol(protocolStr), parseAuthentication(authenticationStr), username, password);
	}
	
	public ProxyConfig(String host, int port, ProxyProtocol protocol, 
			ProxyAuthentication authentication, String username, String password) {
		super();
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.authentication = authentication;
		this.username = username;
		this.password = password;
	}

	public Type getJavaProxyType() {
		Type proxyType = null;
		
		// Type: HTTP
		if (ProxyProtocol.HTTP.equals(protocol)) {
			proxyType = Type.HTTP;
		
		// Type: Socks
		} else if (ProxyProtocol.SOCKS.equals(protocol)) {
			proxyType = Type.SOCKS;
		}
		
		return proxyType;
	}
	
	public String getHost() { return host; }
	public int getPort() { return port;	}
	public ProxyProtocol getProtocol() { return protocol; }
	public ProxyAuthentication getAuthentication() { return authentication; }
	public String getUsername() { return username; }
	public String getPassword() { return password; }
	
	/** One of 'http' or 'socks' */
	public String getLocalAnalyzerProtocolOption() {
		String localAnalyzerProtocol = null;
		
		if (ProxyProtocol.HTTP.equals(protocol)) {
			localAnalyzerProtocol = "http";
		} else if (ProxyProtocol.SOCKS.equals(protocol)) {
			localAnalyzerProtocol = "socks";
		} else {
			localAnalyzerProtocol = "";
		}
		
		return localAnalyzerProtocol;
	}
	
	/** One of 'None' or 'Basic' */
	public String getLocalAnalyzerAuthenticationOption() {
		String localAnalyzerAuthentication = null;
		
		if (ProxyAuthentication.NONE.equals(authentication)) {
			localAnalyzerAuthentication = "None";
		
		} else if (ProxyAuthentication.BASIC.equals(authentication)) {
			localAnalyzerAuthentication = "Basic";
		
		} else {
			localAnalyzerAuthentication = "";
		}
		
		return localAnalyzerAuthentication;
	}
	
	private static ProxyProtocol parseProtocol(String protocolStr) {
		ProxyProtocol protocol = null;
		if (StringUtils.isNotEmpty(protocolStr)) {
			protocol = ProxyProtocol.valueOf(protocolStr.toUpperCase(Locale.ENGLISH));
		}
		return protocol;
	}

	private static ProxyAuthentication parseAuthentication(String authenticationStr) {
		ProxyAuthentication authentication = null;
		if (StringUtils.isNotEmpty(authenticationStr)) {
			authentication = ProxyAuthentication.valueOf(authenticationStr.toUpperCase(Locale.ENGLISH));
		}
		return authentication;
	}
	
}
