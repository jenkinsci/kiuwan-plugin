package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.DEFAULT_PROXY_PORT;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.createListBoxModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientException;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientUtils;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyAuthentication;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyProtocol;
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
		if (StringUtils.isEmpty(name)) {
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
		
		if (configureProxy && (StringUtils.isEmpty(proxyHost))) {
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
		return createListBoxModel(ProxyProtocol.values(), proxyProtocol);
	}

	public ListBoxModel doFillProxyAuthenticationItems(@QueryParameter("proxyAuthentication") String proxyAuthentication) {
		return createListBoxModel(ProxyAuthentication.values(), proxyAuthentication);
	}
	
	public FormValidation doCheckCredentials(@QueryParameter String username, 
			@QueryParameter String password, @QueryParameter String domain,
			@QueryParameter boolean configureKiuwanURL, @QueryParameter String kiuwanURL) {

		FormValidation formValidation = null;
		try {
			ApiClient client = KiuwanClientUtils.instantiateClient(configureKiuwanURL, kiuwanURL, username, password, domain);
			InformationApi api = new InformationApi(client);
			api.getInformation();
			formValidation = FormValidation.ok("Authentication completed successfully!");
		
		} catch (ApiException e) {
			KiuwanClientException krce = KiuwanClientException.from(e);
			KiuwanUtils.logger().log(Level.WARNING, krce.getLocalizedMessage());
			formValidation = FormValidation.error("Authentication failed: " + krce.getLocalizedMessage());
		
		} catch (Throwable t) {
			KiuwanUtils.logger().log(Level.SEVERE, t.getLocalizedMessage());
			formValidation = FormValidation.warning("Could not initiate the authentication process. Reason: " + t.getLocalizedMessage());
		}
		
		return formValidation;
	}
	
}
