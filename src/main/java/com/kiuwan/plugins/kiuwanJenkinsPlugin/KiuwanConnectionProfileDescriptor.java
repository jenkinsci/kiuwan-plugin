package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.DEFAULT_PROXY_PORT;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_AUTHENTICATION_BASIC;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_AUTHENTICATION_NONE;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_TYPE_HTTP;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_TYPE_SOCKS;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

@Extension
public class KiuwanConnectionProfileDescriptor extends Descriptor<KiuwanConnectionProfile> {

	private final static String[] proxyProtocolComboValues = { PROXY_TYPE_HTTP, PROXY_TYPE_SOCKS };
	private final static String[] proxyAuthenticationTypeComboValues = { PROXY_AUTHENTICATION_NONE, PROXY_AUTHENTICATION_BASIC };
	
	public KiuwanConnectionProfileDescriptor() {
		super(KiuwanConnectionProfile.class);
	}
	
	@Override
	public String getDisplayName() {
		return "DISPLAY NAME - KiuwanConnectionProfileDescriptor";
	}
	
	public int getDefaultPort() {
		return DEFAULT_PROXY_PORT;
	}
	
	public FormValidation doCheckProxyPort(@QueryParameter("proxyPort") int proxyPort) {
		if (proxyPort <= 0) {
			FormValidation.error("Proxy port must be greater than 0");
		}
		return FormValidation.ok();
	}
	
	public FormValidation doCheckKiuwanURL(
			@QueryParameter("configureKiuwanURL") boolean configureKiuwanURL, 
			@QueryParameter("kiuwanURL") String kiuwanURL) {
		
		if (configureKiuwanURL) {
			try {
				new URL(kiuwanURL + "/rest");
			} catch (MalformedURLException e) {
				return FormValidation.error("URL is not a valid.");
			}
		}
		return FormValidation.ok();
	}
	
	public ListBoxModel doFillProxyProtocolItems(@QueryParameter("proxyProtocol") String proxyProtocol) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < proxyProtocolComboValues.length; i++) {
			if (proxyProtocolComboValues[i].equalsIgnoreCase(proxyProtocol)) {
				items.add(new ListBoxModel.Option(proxyProtocolComboValues[i], proxyProtocolComboValues[i], true));
			} else {
				items.add(proxyProtocolComboValues[i], proxyProtocolComboValues[i]);
			}
		}

		return items;
	}

	public ListBoxModel doFillProxyAuthenticationItems(@QueryParameter("proxyAuthentication") String proxyAuthentication) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < proxyAuthenticationTypeComboValues.length; i++) {
			if (proxyAuthenticationTypeComboValues[i].equalsIgnoreCase(proxyAuthentication)) {
				items.add(new ListBoxModel.Option(proxyAuthenticationTypeComboValues[i],
						proxyAuthenticationTypeComboValues[i], true));
			} else {
				items.add(proxyAuthenticationTypeComboValues[i], proxyAuthenticationTypeComboValues[i]);
			}
		}

		return items;
	}
	
	public FormValidation doTestConnection(@QueryParameter String uuid, 
			@QueryParameter String name, @QueryParameter String username, 
			@QueryParameter String password, @QueryParameter String domain,
			@QueryParameter boolean configureKiuwanURL, @QueryParameter String kiuwanURL,  
			@QueryParameter boolean configureProxy, @QueryParameter String proxyHost, @QueryParameter int proxyPort,
			@QueryParameter String proxyProtocol, @QueryParameter String proxyAuthentication,
			@QueryParameter String proxyUsername, @QueryParameter String proxyPassword) {
		
		KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile(
			uuid, name, username, password, domain, 
			configureKiuwanURL, kiuwanURL, 
			configureProxy, proxyHost, proxyPort, proxyProtocol, 
			proxyAuthentication, proxyUsername, proxyPassword);
		
		FormValidation formValidation = KiuwanUtils.testConnection(connectionProfile);
		return formValidation;
	}
	
}
