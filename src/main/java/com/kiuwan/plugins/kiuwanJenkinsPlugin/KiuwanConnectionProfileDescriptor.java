package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.DEFAULT_PROXY_PORT;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.createListBoxModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientException;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.client.KiuwanClientUtils;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyAuthentication;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ProxyProtocol;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanAnalyzerInstaller;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.api.InformationApi;
import com.kiuwan.rest.client.model.UserInformationResponse;

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

		// 1 - Check credentials using Kiuwan rest client
		String customerEngineVersion = null;
		String credentialsErrorMessage = null;
		try {
			ApiClient client = KiuwanClientUtils.instantiateClient(configureKiuwanURL, kiuwanURL, username, password, domain);
			InformationApi api = new InformationApi(client);
			UserInformationResponse information = api.getInformation();
			customerEngineVersion = information.getEngineVersion() + (information.isEngineFrozen() ? " [FROZEN]" : "");
			
		} catch (ApiException e) {
			KiuwanClientException krce = KiuwanClientException.from(e);
			KiuwanUtils.logger().log(Level.WARNING, krce.toString());
			Throwable rootCause = ExceptionUtils.getRootCause(krce);
			credentialsErrorMessage = "Authentication failed. Reason: " + krce.getLocalizedMessage() + 
				(rootCause != null ? " (" + rootCause + ")" : "");
		
		} catch (Throwable t) {
			KiuwanUtils.logger().log(Level.SEVERE, t.toString());
			Throwable rootCause = ExceptionUtils.getRootCause(t);
			credentialsErrorMessage = "Could not initiate the authentication process. Reason: "  + t.getLocalizedMessage() + 
				(rootCause != null ? " (" + rootCause + ")" : "");
		}
		
		// 2 - Check connection from Jenkins (when connecting through an authenticating proxy, 
		// Basic authentication must be enabled in the Jenkins JVM, this also grants that no certificate
		// problems arise when connecting to a kiuwan on premises instance)
		String currentKlaVersion = null;
		String connectionErrorMessage = null;
		try {
			currentKlaVersion = KiuwanAnalyzerInstaller.getCurrentKlaVersion(configureKiuwanURL, kiuwanURL);
		
		} catch (Throwable t) {
			KiuwanUtils.logger().log(Level.SEVERE, t.toString());
			Throwable rootCause = ExceptionUtils.getRootCause(t);
			connectionErrorMessage = "Cannot reach Kiuwan using Jenkins connection API. Reason: " + t + 
				(rootCause != null ? " (" + rootCause + ")" : "");
		}
		
		// Success
		FormValidation formValidation = null;
		if (customerEngineVersion != null && currentKlaVersion != null) {
			formValidation = FormValidation.okWithMarkup("Authentication completed successfully.<ul>" + 
				"<li>Current Kiuwan Local Analyzer version: <b>" + currentKlaVersion + "</b>.</li>" + 
				"<li>Current Kiuwan Engine version: <b>" + customerEngineVersion + "</b>.</li></ul>");
		
		// Credentials failed
		} else if (customerEngineVersion == null && currentKlaVersion != null) {
			formValidation = FormValidation.error(credentialsErrorMessage);
					
		// Connection failed
		} else if (customerEngineVersion != null && currentKlaVersion == null) {
			formValidation = FormValidation.error(connectionErrorMessage);
			
		// All failed
		} else {
			formValidation = FormValidation.errorWithMarkup(credentialsErrorMessage + "<br><br>" + connectionErrorMessage);
		}
		
		return formValidation;
	}
	
}
