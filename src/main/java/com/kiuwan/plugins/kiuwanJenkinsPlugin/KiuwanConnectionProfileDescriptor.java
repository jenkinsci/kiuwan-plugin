package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.DEFAULT_PROXY_PORT;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_AUTHENTICATION_BASIC;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_AUTHENTICATION_NONE;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_TYPE_HTTP;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.PROXY_TYPE_SOCKS;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.api.InformationApi;

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
		return "Kiuwan Connection Profile";
	}
	
	public int getDefaultPort() {
		return DEFAULT_PROXY_PORT;
	}
	
	public FormValidation doCheckName(@QueryParameter("name") String name) {
		if (name == null || name.isEmpty()) {
			return FormValidation.error("Field is required");
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

	public FormValidation doCheckProxyHost(
			@QueryParameter("configureProxy") boolean configureProxy, 
			@QueryParameter("proxyHost") String proxyHost) {
		
		if (configureProxy && (proxyHost == null || proxyHost.isEmpty())) {
			return FormValidation.error("Field is required");
		}
		return FormValidation.ok();
	}
	
	public FormValidation doCheckProxyPort(@QueryParameter("proxyPort") int proxyPort) {
		if (proxyPort <= 0) {
			return FormValidation.error("Proxy port must be a number greater than 0");
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
		
		KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile();
		connectionProfile.setUuid(uuid);
		connectionProfile.setName(name);
		connectionProfile.setUsername(username);
		connectionProfile.setPassword(password);
		connectionProfile.setDomain(domain);
		connectionProfile.setConfigureKiuwanURL(configureKiuwanURL);
		connectionProfile.setKiuwanURL(kiuwanURL);
		connectionProfile.setConfigureProxy(configureProxy);
		connectionProfile.setProxyHost(proxyHost);
		connectionProfile.setProxyPort(proxyPort);
		connectionProfile.setProxyProtocol(proxyProtocol);
		connectionProfile.setProxyAuthentication(proxyAuthentication);
		connectionProfile.setProxyUsername(proxyUsername);
		connectionProfile.setProxyPassword(proxyPassword);
		
		FormValidation formValidation = null;
		try {
			ApiClient client = KiuwanUtils.instantiateClient(connectionProfile);
			InformationApi api = new InformationApi(client);
			api.getInformation();
			formValidation = FormValidation.ok("Authentication completed successfully!");
		
		} catch (ApiException e) {
			KiuwanUtils.logger().log(Level.WARNING, e.getLocalizedMessage());
			formValidation = FormValidation.error("Authentication failed: " + e.getMessage());
		
		} catch (Throwable t) {
			KiuwanUtils.logger().log(Level.SEVERE, t.getLocalizedMessage());
			formValidation = FormValidation.warning("Could not initiate the authentication process. Reason: " + t.getLocalizedMessage());
		}
		
		return formValidation;
	}
	
}
