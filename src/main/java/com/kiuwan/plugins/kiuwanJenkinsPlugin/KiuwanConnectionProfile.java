package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.Proxy.Type;
import java.net.URL;

import org.apache.commons.lang.RandomStringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.KiuwanModelObject;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class KiuwanConnectionProfile implements Describable<KiuwanConnectionProfile>, KiuwanModelObject, Serializable {

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
	
	@DataBoundConstructor
	public KiuwanConnectionProfile() {
		super();
	}
	
	@Override
	public Descriptor<KiuwanConnectionProfile> getDescriptor() {
		return Jenkins.getInstance().getDescriptorByType(KiuwanConnectionProfileDescriptor.class);
	}

	@Override
	public String toString() {
		return getDisplayName();
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
	
	@Override
	public String getDisplayName() {
		String profileName = name != null && !name.isEmpty() ? name : "?";
		String profileUsername = username != null && !username.isEmpty() ? username : "?";
		String profileHost = null;
		
		if (configureKiuwanURL) {
			try {
				URL url = new URL(kiuwanURL);
				profileHost = url.getHost() + (url.getPort() >= 0 ? ":" + url.getPort() : "");
			} catch (MalformedURLException e) {
				profileHost = "?";
			}
		} else {
			profileHost = "www.kiuwan.com";
		}
		
		return profileName + " - " + profileUsername + "@" + profileHost + " (" + this.uuid + ")";
	}
	
	@Override
	public String getValue() {
		return uuid;
	}
	
	public String generateUuid() {
		int length = 8;
		String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(length);
		String head = randomAlphanumeric.substring(0, length / 2);
		String tail = randomAlphanumeric.substring(length / 2);
		this.uuid = head + "-" + tail;
		return this.uuid;
	}

	public String getUuid() { return uuid; }
	public String getName() { return name; }
	public String getUsername() { return this.username; }
	public String getPassword() { return decrypt(this.password); }
	public String getDomain() { return this.domain; }
	public boolean isConfigureKiuwanURL() { return configureKiuwanURL; }
	public String getKiuwanURL() { return kiuwanURL; }
	public boolean isConfigureProxy() { return this.configureProxy; }
	public String getProxyHost() { return this.proxyHost; }
	public int getProxyPort() { return this.proxyPort; }
	public String getProxyProtocol() { return this.proxyProtocol; }
	public String getProxyAuthentication() { return this.proxyAuthentication; }
	public String getProxyUsername() { return this.proxyUsername; }
	public String getProxyPassword() { return decrypt(this.proxyPassword); }

	@DataBoundSetter public void setUuid(String uuid) { this.uuid = uuid; }
	@DataBoundSetter public void setName(String name) { this.name = name; }
	@DataBoundSetter public void setUsername(String username) { this.username = username; }
	@DataBoundSetter public void setPassword(String password) { this.password = encrypt(password); }
	@DataBoundSetter public void setDomain(String domain) { this.domain = domain; }
	@DataBoundSetter public void setConfigureKiuwanURL(boolean configureKiuwanURL) { this.configureKiuwanURL = configureKiuwanURL; }
	@DataBoundSetter public void setKiuwanURL(String kiuwanURL) { this.kiuwanURL = kiuwanURL; }
	@DataBoundSetter public void setConfigureProxy(boolean configureProxy) { this.configureProxy = configureProxy; }
	@DataBoundSetter public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }
	@DataBoundSetter public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }
	@DataBoundSetter public void setProxyProtocol(String proxyProtocol) { this.proxyProtocol = proxyProtocol; }
	@DataBoundSetter public void setProxyAuthentication(String proxyAuthentication) { this.proxyAuthentication = proxyAuthentication; }
	@DataBoundSetter public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }
	@DataBoundSetter public void setProxyPassword(String proxyPassword) { this.proxyPassword = encrypt(proxyPassword); }
	
	private static String encrypt(String value) {
		return Secret.fromString(value).getEncryptedValue();
	}
	
	private static String decrypt(String value) {
		return Secret.toString(Secret.decrypt(value));
	}
	
}
