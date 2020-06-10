package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.parseErrorCodes;

import java.util.Set;

import org.kohsuke.stapler.QueryParameter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ChangeRequestStatusType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.DeliveryType;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Measure;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;

@Extension
public class KiuwanRecorderDescriptor extends BuildStepDescriptor<Publisher> {

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
		// return "Analyze your source code with Kiuwan!";
		return "DISPLAY NAME - KiuwanRecorderDescriptor";
	}
	
	public ListBoxModel doFillConnectionProfileItems(@QueryParameter("connectionProfileUuid") String connectionProfileUuid) {
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		Iterable<KiuwanConnectionProfile> connectionProfiles = descriptor.getConnectionProfiles();
		ListBoxModel items = new ListBoxModel();
		for (KiuwanConnectionProfile connectionProfile : connectionProfiles) {
			if (connectionProfile.getUuid().equals(connectionProfileUuid)) {
				items.add(new ListBoxModel.Option(connectionProfile.getName(), connectionProfile.getUuid(), true));
			} else {
				items.add(connectionProfile.getName(), connectionProfile.getUuid());
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

	public ListBoxModel doFillMarkBuildWhenNoPass_dmItems(@QueryParameter("markBuildWhenNoPass_dm") String markBuildWhenNoPass) {
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
	
	public ListBoxModel doFillMarkAsInOtherCases_emItems(@QueryParameter("markAsInOtherCases_em") String markAsInOtherCases) {
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
						return FormValidation.error("Unstable threshold can not be lower or equal than failure threshold.");
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
						return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
			
		} else if (Measure.EFFORT_TO_TARGET.name().equals(measure)) {
			try {
				double failed = Double.parseDouble(failureThreshold);
				if (failed <= unstable) {
					return FormValidation.error("Unstable threshold can not be greater or equal than failure threshold.");
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
	
	private FormValidation validateResultCodes(String resultCodes, String[] otherCodes) {
		if (resultCodes != null) {
			Set<Integer> codes = null;
			try {
				codes = parseErrorCodes(resultCodes);
			} catch (Throwable throwable) {
				return FormValidation.error("Invalid value detected in result codes.");
			}

			// check duplicated codes
			for (String currentCodes : otherCodes) {
				try {
					Set<Integer> currentCodesList = parseErrorCodes(currentCodes);
					if (currentCodesList.retainAll(codes) && !currentCodesList.isEmpty()) {
						return FormValidation.error("One result code can only be assigned to one build status. " +
							"These codes are binded with multiple build statuses: " + currentCodesList +
							". Please remove the duplicity.");
					}
				} catch (Throwable throwable) {
					// Ignore
				}
			}
		}
		
		return FormValidation.ok();
	}
	
}
