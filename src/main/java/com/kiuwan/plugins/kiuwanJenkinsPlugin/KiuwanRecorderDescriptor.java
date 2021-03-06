package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.createListBoxModel;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ChangeRequestStatusType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.DeliveryType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.SelectableResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

@Symbol("kiuwan")
@Extension
public class KiuwanRecorderDescriptor extends BuildStepDescriptor<Publisher> {

	public static final String DEFAULT_SOURCE_PATH = "";
	public static final String DEFAULT_OUTPUT_FILENAME = "kiuwan/output.json";
	public static final String DEFAULT_MODE = Mode.STANDARD_MODE.getValue();

	public static final String DEFAULT_APPLICATION_NAME = "${JOB_NAME}";
	public static final String DEFAULT_LABEL = "#${BUILD_NUMBER}";
	public static final String DEFAULT_ANALYSIS_SCOPE = DeliveryType.COMPLETE_DELIVERY.getValue();
	public static final String DEFAULT_CHANGE_REQUEST = "";
	public static final String DEFAULT_BRANCH = "";
	public static final String DEFAULT_CHANGE_REQUEST_STATUS = ChangeRequestStatusType.RESOLVED.getValue();
	public static final String DEFAULT_MEASURE = Measure.QUALITY_INDICATOR.getValue();
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final String DEFAULT_INCLUDES = "";
	public static final String DEFAULT_EXCLUDES = "**/src/test/**,**/__MACOSX/**,**/*.min.js,**/*.Designer.vb," +
		"**/*.designer.vb,**/*Reference.vb,**/*Service.vb,**/*Silverlight.vb,**/*.Designer.cs,**/*.designer.cs," +
		"**/*Reference.cs,**/*Service.cs,**/*Silverlight.cs,**/.*,**/Pods/BuildHeaders/**/*.h,**/Pods/Headers/**/*.h," +
		"**/node_modules/**,**/bower_components/**,**/target/**,**/bin/**,**/obj/**,**/dist/**,**/lib/**";
	public static final Integer DEFAULT_TIMEOUT = 60;
	
	public static final Boolean DEFAULT_INDICATE_LANGUAGES = false;
	public static final Boolean DEFAULT_WAIT_FOR_AUDIT_RESULTS = false;
	
	public static final String DEFAULT_LANGUAGES = "abap,actionscript,aspnet,c,cobol,cpp,csharp,go,groovy,html,informix," + 
		"java,javascript,jcl,jsp,kotlin,natural,objectivec,oracleforms,other,perl,php,plsql,powerscript,python,rpg4," +
		"ruby,scala,sqlscript,swift,transactsql,vb6,vbnet,xml";
	
	public static final String DEFAULT_MARK_BUILD_WHEN_NO_PASS = "UNSTABLE";
	public static final String DEFAULT_COMMAND_ARGS = "-c -n \"" + DEFAULT_APPLICATION_NAME + "\" -l \"" + DEFAULT_LABEL + "\"";
	public static final String DEFAULT_EXTRA_PARAMETERS = "encoding=\"" + DEFAULT_ENCODING + "\" " +
		"supported.technologies=\"" + DEFAULT_LANGUAGES + "\" " +
		"exclude.patterns=\"" + DEFAULT_EXCLUDES + "\"";
	
	public static final String DEFAULT_SUCCESS_RESULT_CODES = "0";
	public static final String DEFAULT_UNSTABLE_RESULT_CODES = "10, 13";
	public static final String DEFAULT_FAILURE_RESULT_CODES = "1, 11, 12, 14";
	public static final String DEFAULT_NOT_BUILT_RESULT_CODES = "";
	public static final String DEFAULT_ABORTED_RESULT_CODES = "";
	public static final String DEFAULT_MARK_AS_IN_OTHER_CASES_RESULT_CODES = "FAILURE";

	public KiuwanRecorderDescriptor() {
		super(KiuwanRecorder.class);
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public boolean isApplicable(Class<? extends AbstractProject> item) {
		return true;
	}
	
	@Override
	public String getDisplayName() {
		return "Analyze your source code with Kiuwan!";
	}
	
	public boolean isConnectionProfilesConfigured() {
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		List<KiuwanConnectionProfile> connectionProfiles = descriptor.getConnectionProfiles();
		return connectionProfiles != null && !connectionProfiles.isEmpty();
	}
	
	public Mode getBaselineMode() { return Mode.STANDARD_MODE; }
	public Mode getDeliveryMode() { return Mode.DELIVERY_MODE; }
	public Mode getExpertMode() { return Mode.EXPERT_MODE; }
	
	public ListBoxModel doFillConnectionProfileUuidItems(@QueryParameter("connectionProfileUuid") String connectionProfileUuid) {
		ListBoxModel items = new ListBoxModel();
		items.add("", "");
		
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		if (descriptor.getConnectionProfiles() != null) {
			List<KiuwanConnectionProfile> connectionProfiles = descriptor.getConnectionProfiles();
			KiuwanConnectionProfile[] data = connectionProfiles.toArray(new KiuwanConnectionProfile[connectionProfiles.size()]);
			boolean found = KiuwanUtils.addAllOptionsToListBoxModel(items, data, connectionProfileUuid);
			
			if (!found && StringUtils.isNotEmpty(connectionProfileUuid)) {
				String displayName = "[UNKNOWN PROFILE] (" + connectionProfileUuid + ")";
				items.add(new ListBoxModel.Option(displayName, connectionProfileUuid, true));
			}
		}
		
		return items;
	}

	public ListBoxModel doFillMeasureItems(@QueryParameter("measure") String measure) {
		return createListBoxModel(Measure.values(), measure);
	}
	
	public ListBoxModel doFillAnalysisScope_dmItems(@QueryParameter("analysisScope_dm") String deliveryType) {
		return createListBoxModel(DeliveryType.values(), deliveryType);
	}
	
	public ListBoxModel doFillChangeRequestStatus_dmItems(@QueryParameter("changeRequestStatus_dm") String changeRequestStatus) {
		return createListBoxModel(ChangeRequestStatusType.values(), changeRequestStatus);
	}

	public ListBoxModel doFillMarkBuildWhenNoPass_dmItems(@QueryParameter("markBuildWhenNoPass_dm") String markBuildWhenNoPass) {
		return createListBoxModel(SelectableResult.values(), markBuildWhenNoPass);
	}
	
	public ListBoxModel doFillMarkAsInOtherCases_emItems(@QueryParameter("markAsInOtherCases_em") String markAsInOtherCases) {
		return createListBoxModel(SelectableResult.values(), markAsInOtherCases);
	}

	public FormValidation doCheckConnectionProfileUuid(@QueryParameter("connectionProfileUuid") String connectionProfileUuid) {
		if (StringUtils.isEmpty(connectionProfileUuid)) {
			return FormValidation.error("This field is required.");
		} else {
			KiuwanConnectionProfile connectionProfile = KiuwanGlobalConfigDescriptor.get().getConnectionProfile(connectionProfileUuid);
			if (connectionProfile == null) {
				return FormValidation.errorWithMarkup("Selected connection profile does not exist anymore or is not correctly set up. " + 
					"Please, check your connection profiles in <a href=\"" + Jenkins.getInstance().getRootUrl() + 
					"configure\" target=\"_blank\">Kiuwan Global Settings</a> or select a substitute from the list.");
			}
		}
		
		return FormValidation.ok();
	}
	
	public FormValidation doCheckTimeout(@QueryParameter("timeout") int timeout) {
		if (timeout < 1) {
			return FormValidation.error("Timeout must be a number greater than 0.");
		}
		
		return FormValidation.ok();
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
		}
		
		return unstableThresholdValidationResult;
	}

	public FormValidation doCheckUnstableThreshold(@QueryParameter("unstableThreshold") String unstableThreshold,
			@QueryParameter("failureThreshold") String failureThreshold, @QueryParameter("measure") String measure) {
		
		Double unstable = KiuwanUtils.parseDouble(unstableThreshold);
		Double failure = KiuwanUtils.parseDouble(failureThreshold);
		
		if (unstable == null) return FormValidation.error("Unstable threshold must be a non-negative numeric value.");
		if (unstable < 0) return FormValidation.error("Unstable threshold must be a positive number.");
		
		if (Measure.QUALITY_INDICATOR.getValue().equals(measure)) {
			if (unstable >= 100) {
				return FormValidation.error("Unstable threshold must be lower than 100.");
			} else if (failure != null && failure >= unstable) {
				return FormValidation.error("Unstable threshold can not be lower or equal than failure threshold.");
			}
			
		} else if (Measure.RISK_INDEX.getValue().equals(measure)) {
			if (unstable <= 0) {
				return FormValidation.error("Unstable threshold must be greater than 0.");
			} else if (failure != null && failure <= unstable) {
				return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
			}
			
		} else if (Measure.EFFORT_TO_TARGET.getValue().equals(measure)) {
			if (failure != null && failure <= unstable) {
				return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
			}
		}

		return FormValidation.ok();
	}

	public FormValidation doCheckFailureThreshold(@QueryParameter("failureThreshold") String failureThreshold,
			@QueryParameter("unstableThreshold") String unstableThreshold, @QueryParameter("measure") String measure) {
		
		Double unstable = KiuwanUtils.parseDouble(unstableThreshold);
		Double failure = KiuwanUtils.parseDouble(failureThreshold);
		
		if (failure == null) return FormValidation.error("Failure threshold must be a non-negative numeric value.");
		if (failure < 0) return FormValidation.error("Failure threshold must be a positive number.");
		
		if (Measure.QUALITY_INDICATOR.getValue().equals(measure)) {
			if (unstable != null && failure >= unstable) {
				return FormValidation.error("Failure threshold can not be greater or equal than unstable threshold.");
			}
			
		} else if (Measure.RISK_INDEX.getValue().equals(measure)) {
			if (failure > 100) {
				return FormValidation.error("Failure threshold must be lower or equal than 100.");
			} else if (unstable != null && failure <= unstable) {
				return FormValidation.error("Failure threshold can not be lower or equal than unstable threshold.");
			}
			
		} else if (Measure.EFFORT_TO_TARGET.getValue().equals(measure)) {
			if (unstable != null && failure <= unstable) {
				return FormValidation.error("Failure threshold can not be lower or equal than unstable threshold.");
			}
		}
		
		return FormValidation.ok();
	}

	public FormValidation doCheckSuccessResultCodes_em(@QueryParameter("successResultCodes_em") String successResultCodes, 
			@QueryParameter("unstableResultCodes_em") String unstableResultCodes, 
			@QueryParameter("failureResultCodes_em") String failureResultCodes, 
			@QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes, 
			@QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(successResultCodes, 
			new String[] { unstableResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes});
	}

	public FormValidation doCheckUnstableResultCodes_em(
			@QueryParameter("successResultCodes_em") String successResultCodes,
			@QueryParameter("unstableResultCodes_em") String unstableResultCodes,
			@QueryParameter("failureResultCodes_em") String failureResultCodes,
			@QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes,
			@QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(unstableResultCodes,
			new String[] { successResultCodes, failureResultCodes, notBuiltResultCodes, abortedResultCodes });
	}
	
	public FormValidation doCheckFailureResultCodes_em(
			@QueryParameter("successResultCodes_em") String successResultCodes,
			@QueryParameter("unstableResultCodes_em") String unstableResultCodes,
			@QueryParameter("failureResultCodes_em") String failureResultCodes,
			@QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes,
			@QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(failureResultCodes,
			new String[] { successResultCodes, unstableResultCodes, notBuiltResultCodes, abortedResultCodes });
	}
	
	public FormValidation doCheckNotBuiltResultCodes_em(
			@QueryParameter("successResultCodes_em") String successResultCodes,
			@QueryParameter("unstableResultCodes_em") String unstableResultCodes,
			@QueryParameter("failureResultCodes_em") String failureResultCodes,
			@QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes,
			@QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(notBuiltResultCodes,
			new String[] { successResultCodes, unstableResultCodes, failureResultCodes, abortedResultCodes });
	}

	public FormValidation doCheckAbortedResultCodes_em(
			@QueryParameter("successResultCodes_em") String successResultCodes,
			@QueryParameter("unstableResultCodes_em") String unstableResultCodes,
			@QueryParameter("failureResultCodes_em") String failureResultCodes,
			@QueryParameter("notBuiltResultCodes_em") String notBuiltResultCodes,
			@QueryParameter("abortedResultCodes_em") String abortedResultCodes) {
		return validateResultCodes(abortedResultCodes,
			new String[] { successResultCodes, unstableResultCodes, failureResultCodes, notBuiltResultCodes });
	}
	
	private FormValidation validateResultCodes(String codesStr, String[] otherCodesStrArray) {
		if (codesStr != null) {
			Set<Integer> codes = null;
			try {
				codes = parseErrorCodes(codesStr);
			} catch (Throwable throwable) {
				return FormValidation.error("Invalid value. Result codes must be specified as a comma-separated list of numbers.");
			}

			// Check duplicated codes, accumulate repeated values to show correct error message
			Set<Integer> repeatedValues = new HashSet<>();
			for (String otherCodesStr : otherCodesStrArray) {
				try {
					Set<Integer> otherCodes = parseErrorCodes(otherCodesStr);
					if (!Collections.disjoint(codes, otherCodes) && !otherCodes.isEmpty()) {
						otherCodes.retainAll(codes);
						repeatedValues.addAll(otherCodes);
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
			
			if (!repeatedValues.isEmpty()) {
				String codeMessage = null;
				if (repeatedValues.size() == 1) {
					codeMessage = "This code is bound to multiple build statuses: ";
				} else {
					codeMessage = "These codes are bound to multiple build statuses: ";
				}
				
				return FormValidation.error("One result code can only be assigned to one build status. " +
					codeMessage + repeatedValues + ". Please remove the duplicity.");
			}
		}
		
		return FormValidation.ok();
	}
	
}
