package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.instantiateClient;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;

import java.net.MalformedURLException;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Set;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ChangeRequestStatusType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.DeliveryType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.rest.client.ApiClient;
import com.kiuwan.rest.client.ApiException;
import com.kiuwan.rest.client.ApplicationApi;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.XStream2;
import net.sf.json.JSONObject;

@Extension
public class KiuwanDescriptor extends BuildStepDescriptor<Publisher> {

	public static final String PROXY_AUTHENTICATION_BASIC = "Basic";
	public static final String PROXY_AUTHENTICATION_NONE = "None";
	
	private static final String PROXY_TYPE_HTTP = "http";
	private static final String PROXY_TYPE_SOCKS = "socks";

	private final static String[] measureComboNames = {
		"Global indicator", 
		"Risk index", 
		"Effort to target"
	};
	
	private final static String[] measureComboValues = { 
		Measure.QUALITY_INDICATOR.name(), 
		Measure.RISK_INDEX.name(), 
		Measure.EFFORT_TO_TARGET.name() 
	};

	private final static String[] proxyProtocolComboValues = { 
		PROXY_TYPE_HTTP, 
		PROXY_TYPE_SOCKS
	};

	private final static String[] proxyAuthenticationTypeComboValues = { 
		PROXY_AUTHENTICATION_NONE, 
		PROXY_AUTHENTICATION_BASIC
	};

	private final static String[] buildResultComboValues = { 
		Result.FAILURE.toString(), 
		Result.UNSTABLE.toString(),
		Result.ABORTED.toString(), 
		Result.NOT_BUILT.toString() 
	};

	private final static String[] deliveryTypeComboNames = { 
		"Complete delivery", 
		"Partial delivery"
	};

	private final static String[] deliveryTypeComboValues = { 
		DeliveryType.COMPLETE_DELIVERY.name(), 
		DeliveryType.PARTIAL_DELIVERY.name() 
	};
	
	private final static String[] changeRequestStatusComboNames = { 
		"Resolved", 
		"In progress"
	};
	
	private final static String[] changeRequestStatusComboValues = { 
		ChangeRequestStatusType.RESOLVED.name(), 
		ChangeRequestStatusType.INPROGRESS.name()
	};

	private String username;
	private String password;
	
	private boolean configureKiuwanURL;
	private String kiuwanURL;
	
	private boolean configureProxy;
	private String proxyHost;
	private int proxyPort = 3128;
	private String proxyProtocol;
	private String proxyAuthentication;
	private String proxyUsername;
	private String proxyPassword;
	
	private String configSaveStamp;
	
	/**
	 * This {@link XStream2} instance will be used to support loading of the old plugin descriptor
	 * @see <a href="https://wiki.jenkins.io/display/JENKINS/Hint+on+retaining+backward+compatibility">
	 * Retaining backward compatibility</a>
	 * @see #getConfigFile() method for loading the configuration file
	 */
	private static final XStream2 XSTREAM2 = new XStream2();
	
	static {
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor.class);
	}

	public KiuwanDescriptor() {
		super(KiuwanRecorder.class);
		load();
	}
	
	@Override
	protected XmlFile getConfigFile() {
		XmlFile xmlFile = super.getConfigFile();
		return new XmlFile(XSTREAM2, xmlFile.getFile());
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		// to persist global configuration information,
		// set that to properties and call save().
		String username = (String) json.get("username");
		String password = (String) json.get("password");
		
		Boolean configureKiuwanURL = (Boolean) json.get("configureKiuwanURL");
		String kiuwanURL = (String) json.get("kiuwanURL");
		
		Boolean configureProxy = (Boolean) json.get("configureProxy");
		String proxyHost = (String) json.get("proxyHost");
		int proxyPort = Integer.parseInt((String) json.get("proxyPort"));
		String proxyProtocol = (String) json.get("proxyProtocol");
		String proxyAuthentication = (String) json.get("proxyAuthentication");
		String proxyUsername = (String) json.get("proxyUsername");
		String proxyPassword = (String) json.get("proxyPassword");

		this.username = username;
		Secret secret = Secret.fromString(password);
		this.password = secret.getEncryptedValue();
		this.configureKiuwanURL = configureKiuwanURL;
		this.kiuwanURL = kiuwanURL;
		this.configureProxy = configureProxy;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyProtocol = proxyProtocol;
		this.proxyAuthentication = proxyAuthentication;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = (proxyPassword == null) ? null : Secret.fromString(proxyPassword).getEncryptedValue();

		this.configSaveStamp = Long.toHexString(System.currentTimeMillis());
		
		save();
		return true;
	}

	@Override
	public String getDisplayName() {
		return "Analyze your source code with Kiuwan!";
	}

	@Override
	public boolean isApplicable(Class<? extends AbstractProject> item) {
		return true;
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

	public String getUsername() { return this.username; }
	public String getPassword() { return Secret.toString(Secret.decrypt(this.password)); }
	
	public boolean isConfigureKiuwanURL() { return configureKiuwanURL; }
	public String getKiuwanURL() { return kiuwanURL; }

	public boolean isConfigureProxy() { return this.configureProxy; }
	public String getProxyHost() { return this.proxyHost; }
	public int getProxyPort() { return this.proxyPort; }
	public String getProxyProtocol() { return this.proxyProtocol; }
	public String getProxyAuthentication() { return this.proxyAuthentication; }
	public String getProxyUsername() { return this.proxyUsername; }
	public String getProxyPassword() { return (this.proxyPassword == null) ? null : Secret.toString(Secret.decrypt(this.proxyPassword)); }

	public String getConfigSaveStamp() { return configSaveStamp; }
	
	public FormValidation doTestConnection(@QueryParameter String username, @QueryParameter String password,
			@QueryParameter boolean configureKiuwanURL, @QueryParameter String kiuwanURL,  
			@QueryParameter boolean configureProxy, @QueryParameter String proxyHost, @QueryParameter int proxyPort,
			@QueryParameter String proxyProtocol, @QueryParameter String proxyAuthentication,
			@QueryParameter String proxyUsername, @QueryParameter String proxyPassword) {
		
		KiuwanDescriptor descriptor = new KiuwanDescriptor();
		
		descriptor.username = username;
		descriptor.password = Secret.fromString(password).getEncryptedValue();
		
		descriptor.configureKiuwanURL = configureKiuwanURL;
		descriptor.kiuwanURL = kiuwanURL;
		
		descriptor.configureProxy = configureProxy;
		descriptor.proxyHost = proxyHost;
		descriptor.proxyPort = proxyPort;
		descriptor.proxyProtocol = proxyProtocol;
		descriptor.proxyAuthentication = proxyAuthentication;
		descriptor.proxyUsername = proxyUsername;
		descriptor.proxyPassword = (proxyPassword != null)? Secret.fromString(proxyPassword).getEncryptedValue() : null;
		
		ApiClient client = instantiateClient(descriptor);
		ApplicationApi api = new ApplicationApi(client);
		try {
			api.getApplications();
			return FormValidation.ok("Authentication completed successfully!");
		} catch (ApiException kiuwanClientException) {
			return FormValidation.error(kiuwanClientException, "Authentication failed.");
		} catch (Throwable throwable) {
			return FormValidation.warning("Could not initiate the authentication process. Reason: " + throwable);
		}
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

	public ListBoxModel doFillAnalysisScope_dmItems(@QueryParameter("analysisScope_dm") String deliveryType) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < deliveryTypeComboValues.length; i++) {
			if (deliveryTypeComboValues[i].equalsIgnoreCase(deliveryType)) {
				items.add(new ListBoxModel.Option(deliveryTypeComboNames[i], deliveryTypeComboValues[i], true));
			} else {
				items.add(deliveryTypeComboNames[i], deliveryTypeComboValues[i]);
			}
		}

		return items;
	}
	
	public ListBoxModel doFillChangeRequestStatus_dmItems(@QueryParameter("changeRequestStatus_dm") String changeRequestStatus) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < changeRequestStatusComboValues.length; i++) {
			if (changeRequestStatusComboValues[i].equalsIgnoreCase(changeRequestStatus)) {
				items.add(new ListBoxModel.Option(changeRequestStatusComboNames[i], changeRequestStatusComboValues[i], true));
			} else {
				items.add(changeRequestStatusComboNames[i], changeRequestStatusComboValues[i]);
			}
		}

		return items;
	}

	public ListBoxModel doFillMeasureItems(@QueryParameter("measure") String measure) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < measureComboNames.length; i++) {
			if (measureComboValues[i].equalsIgnoreCase(measure)) {
				items.add(new ListBoxModel.Option(measureComboNames[i], measureComboValues[i], true));
			} else {
				items.add(measureComboNames[i], measureComboValues[i]);
			}
		}

		return items;
	}

	public ListBoxModel doFillMarkBuildWhenNoPass_dmItems(
			@QueryParameter("markBuildWhenNoPass_dm") String markBuildWhenNoPass) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < buildResultComboValues.length; i++) {
			if (buildResultComboValues[i].equalsIgnoreCase(markBuildWhenNoPass)) {
				items.add(new ListBoxModel.Option(buildResultComboValues[i], buildResultComboValues[i], true));
			} else {
				items.add(buildResultComboValues[i], buildResultComboValues[i]);
			}
		}

		return items;
	}
	
	public ListBoxModel doFillMarkAsInOtherCases_emItems(
			@QueryParameter("markAsInOtherCases_em") String markAsInOtherCases) {
		ListBoxModel items = new ListBoxModel();
		for (int i = 0; i < buildResultComboValues.length; i++) {
			if (buildResultComboValues[i].equalsIgnoreCase(markAsInOtherCases)) {
				items.add(new ListBoxModel.Option(buildResultComboValues[i], buildResultComboValues[i], true));
			} else {
				items.add(buildResultComboValues[i], buildResultComboValues[i]);
			}
		}

		return items;
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

	public ListBoxModel doFillProxyAuthenticationItems(
			@QueryParameter("proxyAuthentication") String proxyAuthentication) {
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

	public FormValidation doCheckTimeout(@QueryParameter("timeout") int timeout) {
		if (timeout < 1) {
			return FormValidation.error("Timeout must be greater than 0.");
		} else {
			return FormValidation.ok();
		}
	}
	
	public FormValidation doCheckTimeout_dm(@QueryParameter("timeout_dm") int timeout) {
		return doCheckTimeout(timeout);
	}
	
	public FormValidation doCheckTimeout_em(@QueryParameter("timeout_em") int timeout) {
		return doCheckTimeout(timeout);
	}

	public FormValidation doCheckThresholds(@QueryParameter("unstableThreshold") String unstableThreshold,
			@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
		FormValidation unstableThresholdValidationResult = doCheckUnstableThreshold(unstableThreshold, failureThreshold, measure);
		if (Kind.OK.equals(unstableThresholdValidationResult.kind)) {
			return doCheckFailureThreshold(failureThreshold, unstableThreshold, measure);
		} else {
			return unstableThresholdValidationResult;
		}
	}

	public FormValidation doCheckUnstableThreshold(@QueryParameter("unstableThreshold") String unstableThreshold,
			@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
		double unstable = 0;
		try {
			unstable = Double.parseDouble(unstableThreshold);
			if (unstable < 0) {
				return FormValidation.error("Unstable threshold must be a positive number.");
			}
		} catch (Throwable throwable) {
			return FormValidation.error("Unstable threshold must be a non-negative numeric value.");
		}

		if (Measure.QUALITY_INDICATOR.name().equals(measure)) {
			if (unstable >= 100) {
				return FormValidation.error("Unstable threshold must be lower than 100.");
			} else {
				try {
					double failure = Double.parseDouble(failureThreshold);
					if (failure >= unstable) {
						return FormValidation
								.error("Unstable threshold can not be lower or equal than failure threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
		} else if (Measure.RISK_INDEX.name().equals(measure)) {
			if (unstable <= 0) {
				return FormValidation.error("Unstable threshold must be greater than 0.");
			} else {
				try {
					double failure = Double.parseDouble(failureThreshold);
					if (failure <= unstable) {
						return FormValidation
								.error("Unstable threshold can not be greater or equal than failure threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
		} else if (Measure.EFFORT_TO_TARGET.name().equals(measure)) {
			try {
				double failed = Double.parseDouble(failureThreshold);
				if (failed <= unstable) {
					return FormValidation
							.error("Unstable threshold can not be greater or equal than failure threshold.");
				}
			} catch (Throwable throwable) {
				// Ignore
			}
		}

		return FormValidation.ok();
	}

	public FormValidation doCheckFailureThreshold(@QueryParameter("failureThreshold") String failureThreshold,
			@QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("measure") String measure) {
		double failure = 0;
		try {
			failure = Double.parseDouble(failureThreshold);
			if (failure < 0) {
				return FormValidation.error("Failure threshold must be a positive number.");
			}
		} catch (Throwable throwable) {
			return FormValidation.error("Failure threshold must be a non-negative numeric value.");
		}

		if (Measure.QUALITY_INDICATOR.name().equals(measure)) {
			try {
				double unstable = Double.parseDouble(unstableThreshold);
				if (failure >= unstable) {
					return FormValidation
							.error("Failure threshold can not be greater or equal than unstable threshold.");
				}
			} catch (Throwable throwable) {
				// Ignore
			}
		} else if (Measure.RISK_INDEX.name().equals(measure)) {
			if (failure > 100) {
				return FormValidation.error("Failure threshold must be lower or equal than 100.");
			} else {
				try {
					double unstable = Double.parseDouble(unstableThreshold);
					if (failure <= unstable) {
						return FormValidation
								.error("Failure threshold can not be lower or equal than unstable threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
		} else if (Measure.EFFORT_TO_TARGET.name().equals(measure)) {
			try {
				double unstable = Double.parseDouble(unstableThreshold);
				if (failure <= unstable) {
					return FormValidation
							.error("Failure threshold can not be lower or equal than unstable threshold.");
				}
			} catch (Throwable throwable) {
				// Ignore
			}
		}

		return FormValidation.ok();
	}

	public FormValidation doCheckSuccessResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(successResultCodes, new String[]{unstableResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes});
	}

	public FormValidation doCheckUnstableResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(unstableResultCodes, new String[]{successResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes});
	}
	
	public FormValidation doCheckFailureResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(failureResultCodes, new String[]{successResultCodes, unstableResultCodes, notBuiltResultCodes, abortedResultCodes});
	}
	
	public FormValidation doCheckNotBuiltResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(notBuiltResultCodes, new String[]{successResultCodes, unstableResultCodes, failureResultCodes, abortedResultCodes});
	}
	
	public FormValidation doCheckAbortedResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, @QueryParameter("unstableResultCodes_em") String unstableResultCodes, @QueryParameter("failureResultCodes_em") String failureResultCodes, @QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, @QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(abortedResultCodes, new String[]{successResultCodes, unstableResultCodes, failureResultCodes, notBuiltResultCodes});
	}
	
	private FormValidation validateResultCodes(String resultCodes, String[] otherCodes) {
		if(resultCodes != null){
			Set<Integer> codes = null;
			try{
				codes = parseErrorCodes(resultCodes);
			}catch(Throwable throwable){
				return FormValidation.error("Invalid value detected in result codes.");
			}
			
			//check duplicated codes
			for (String currentCodes : otherCodes) {
				try{
					Set<Integer> currentCodesList = parseErrorCodes(currentCodes);
					if(currentCodesList.retainAll(codes) && !currentCodesList.isEmpty()){
						return FormValidation.error("One result code can only be assigned to one build status. These codes are binded with multiple build statuses: "+currentCodesList+". Please remove the duplicity.");
					}
				}
				catch(Throwable throwable){
					//Ignore
				}
			}
		}
		
		return FormValidation.ok();
	}

}
