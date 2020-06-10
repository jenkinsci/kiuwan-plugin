package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.Serializable;
import java.net.Proxy.Type;
import java.util.UUID;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class KiuwanConnectionProfile implements Describable<KiuwanConnectionProfile>, Serializable {

	public static final String PROXY_AUTHENTICATION_BASIC = "Basic";
	public static final String PROXY_AUTHENTICATION_NONE = "None";
	
	public static final String PROXY_TYPE_HTTP = "http";
	public static final String PROXY_TYPE_SOCKS = "socks";
	public static final int DEFAULT_PROXY_PORT = 3128;
	
	private static final long serialVersionUID = 1768850962607295267L;
	
	private String uuid;
	private String name;
	
	private String username;
	private String password;
	private String domain;
	
	private boolean configureKiuwanURL;
	private String kiuwanURL;
	
	private boolean configureProxy;
	private String proxyHost;
	private int proxyPort;
	private String proxyProtocol;
	private String proxyAuthentication;
	private String proxyUsername;
	private String proxyPassword;
	
	@Override
	public Descriptor<KiuwanConnectionProfile> getDescriptor() {
		return Jenkins.getInstance().getDescriptorByType(KiuwanConnectionProfileDescriptor.class);
	}

	public KiuwanConnectionProfile() {
		super();
	}
	
	@DataBoundConstructor
	public KiuwanConnectionProfile(String uuid, String name, String username, String password, String domain,
			boolean configureKiuwanURL, String kiuwanURL, boolean configureProxy, String proxyHost, int proxyPort,
			String proxyProtocol, String proxyAuthentication, String proxyUsername, String proxyPassword) {
		
		super();
		
		this.uuid = uuid;
		this.name = name;
		this.username = username;
		this.password = Secret.fromString(password).getEncryptedValue();
		this.domain = domain;
		this.configureKiuwanURL = configureKiuwanURL;
		this.kiuwanURL = kiuwanURL;
		this.configureProxy = configureProxy;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyProtocol = proxyProtocol;
		this.proxyAuthentication = proxyAuthentication;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = Secret.fromString(proxyPassword).getEncryptedValue();
	}
	
	public Type getProxyType() {
		Type proxyType = null;
		
		// Type: HTTP
		if (PROXY_TYPE_HTTP.equalsIgnoreCase(proxyProtocol)) {
			proxyType = Type.HTTP;
		
		// Type: socks
		} else if (PROXY_TYPE_SOCKS.equalsIgnoreCase(proxyProtocol)) {
			proxyType = Type.SOCKS;
		}
		
		return proxyType;
	}
	
	public String generateUuid() {
		this.uuid = UUID.randomUUID().toString().substring(9, 18);
		return this.uuid;
	}

	public String getUuid() { return uuid; }
	public String getName() { return name; }
	public String getUsername() { return this.username; }
	public String getPassword() { return Secret.toString(Secret.decrypt(this.password)); }
	public String getDomain() { return this.domain; }
	public boolean isConfigureKiuwanURL() { return configureKiuwanURL; }
	public String getKiuwanURL() { return kiuwanURL; }
	public boolean isConfigureProxy() { return this.configureProxy; }
	public String getProxyHost() { return this.proxyHost; }
	public int getProxyPort() { return this.proxyPort; }
	public String getProxyProtocol() { return this.proxyProtocol; }
	public String getProxyAuthentication() { return this.proxyAuthentication; }
	public String getProxyUsername() { return this.proxyUsername; }
	public String getProxyPassword() { return (this.proxyPassword == null) ? null : Secret.toString(Secret.decrypt(this.proxyPassword)); }

}
