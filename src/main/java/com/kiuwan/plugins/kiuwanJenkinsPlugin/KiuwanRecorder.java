package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable.KiuwanRunnable;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public class KiuwanRecorder extends Recorder {

	public final static Long TIMEOUT_MARGIN = 5000L;
	public final static Mode DEFAULT_MODE = Mode.STANDARD_MODE;

	private Mode selectedMode;

	/*
	 * Baseline Mode
	 */
	private String applicationName;
	private String label;
	private String encoding;
	private String includes;
	private String excludes;
	private Integer timeout;
	private Boolean indicateLanguages;
	private String languages;
	private String measure;
	private Double unstableThreshold;
	private Double failureThreshold;

	/*
	 * CI Mode
	 */
	private String applicationName_ci;
	private String label_ci;
	private String encoding_ci;
	private String includes_ci;
	private String excludes_ci;
	private Integer timeout_ci;
	private Boolean indicateLanguages_ci;
	private String languages_ci;
	private Boolean waitForAuditResults_ci;
	private String markBuildWhenNoPass_ci;

	/*
	 * Delivery Mode
	 */
	private String applicationName_dm;
	private String label_dm;
	private String changeRequest_dm;
	private String analysisScope_dm;
	private String branch_dm;
	private String changeRequestStatus_dm;
	private String encoding_dm;
	private String includes_dm;
	private String excludes_dm;
	private Integer timeout_dm;
	private Boolean indicateLanguages_dm;
	private String languages_dm;
	private Boolean waitForAuditResults_dm;
	private String markBuildWhenNoPass_dm;

	/*
	 * Expert Mode
	 */
	private String commandArgs_em;
	private String extraParameters_em;
	private Integer timeout_em;
	private String successResultCodes_em;
	private String unstableResultCodes_em;
	private String failureResultCodes_em;
	private String notBuiltResultCodes_em;
	private String abortedResultCodes_em;
	private String markAsInOtherCases_em;

	@DataBoundConstructor
	public KiuwanRecorder(String mode, String applicationName, String label, String encoding, String includes, String excludes, Integer timeout,
			Boolean indicateLanguages, String languages, String measure, Double unstableThreshold, Double failureThreshold, String applicationName_ci,
			String label_ci, String encoding_ci, String includes_ci, String excludes_ci, Integer timeout_ci, Boolean indicateLanguages_ci, String languages_ci,
			Boolean waitForAuditResults_ci, String markBuildWhenNoPass_ci, String applicationName_dm, String label_dm, String encoding_dm, String includes_dm,
			String excludes_dm, Integer timeout_dm, Boolean indicateLanguages_dm, String languages_dm, String changeRequest_dm, String changeRequestStatus_dm,
			String branch_dm, String analysisScope_dm, Boolean waitForAuditResults_dm, String markBuildWhenNoPass_dm, Integer timeout_em, String commandArgs_em,
			String extraParameters_em, String successResultCodes_em, String unstableResultCodes_em, String failureResultCodes_em, String notBuiltResultCodes_em,
			String abortedResultCodes_em, String markAsInOtherCases_em) {

		super();

		if (mode == null)
			mode = DEFAULT_MODE.name();

		this.selectedMode = Mode.valueOf(mode);

		this.applicationName = applicationName;
		this.label = label;
		this.encoding = encoding;
		this.includes = includes;
		this.excludes = excludes;
		this.timeout = timeout;
		this.indicateLanguages = indicateLanguages;
		this.languages = languages;
		this.measure = measure;
		this.unstableThreshold = unstableThreshold;
		this.failureThreshold = failureThreshold;

		this.applicationName_ci = applicationName_ci;
		this.label_ci = label_ci;
		this.encoding_ci = encoding_ci;
		this.includes_ci = includes_ci;
		this.excludes_ci = excludes_ci;
		this.timeout_ci = timeout_ci;
		this.indicateLanguages_ci = indicateLanguages_ci;
		this.languages_ci = languages_ci;
		this.waitForAuditResults_ci = waitForAuditResults_ci;
		this.markBuildWhenNoPass_ci = markBuildWhenNoPass_ci;

		this.applicationName_dm = applicationName_dm;
		this.label_dm = label_dm;
		this.encoding_dm = encoding_dm;
		this.includes_dm = includes_dm;
		this.excludes_dm = excludes_dm;
		this.timeout_dm = timeout_dm;
		this.indicateLanguages_dm = indicateLanguages_dm;
		this.languages_dm = languages_dm;
		this.changeRequest_dm = changeRequest_dm;
		this.analysisScope_dm = analysisScope_dm;
		this.waitForAuditResults_dm = waitForAuditResults_dm;
		this.changeRequestStatus_dm = changeRequestStatus_dm;
		this.branch_dm = branch_dm;
		this.markBuildWhenNoPass_dm = markBuildWhenNoPass_dm;

		this.timeout_em = timeout_em;
		this.commandArgs_em = commandArgs_em;
		this.extraParameters_em = extraParameters_em;
		this.successResultCodes_em = successResultCodes_em;
		this.unstableResultCodes_em = unstableResultCodes_em;
		this.failureResultCodes_em = failureResultCodes_em;
		this.notBuiltResultCodes_em = notBuiltResultCodes_em;
		this.abortedResultCodes_em = abortedResultCodes_em;
		this.markAsInOtherCases_em = markAsInOtherCases_em;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}

	@Override
	public KiuwanDescriptor getDescriptor() {
		return (KiuwanDescriptor) super.getDescriptor();
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

		long startTime = System.currentTimeMillis();

		Integer timeout = null;
		Mode selectedMode = getSelectedMode();
		if (Mode.DELIVERY_MODE.equals(selectedMode)) {
			timeout = getTimeout_dm();

		} else if (Mode.EXPERT_MODE.equals(selectedMode)) {
			timeout = getTimeout_em();

		} else if (Mode.CI_MODE.equals(selectedMode)) {
			timeout = getTimeout_ci();

		} else {
			timeout = getTimeout();
		}

		long endTime = startTime + TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES);

		AtomicReference<Throwable> exceptionReference = new AtomicReference<Throwable>();
		AtomicReference<Result> resultReference = new AtomicReference<Result>();
		Computer currentComputer = Computer.currentComputer();
		Node node = currentComputer.getNode();
		Thread thread = createExecutionThread(node, build, launcher, listener, resultReference, exceptionReference);
		thread.start();

		long currentTime = System.currentTimeMillis();
		try {
			while (thread.isAlive() && currentTime < endTime) {
				TimeUnit.MILLISECONDS.sleep(TIMEOUT_MARGIN);
				currentTime = System.currentTimeMillis();
			}
		} catch (InterruptedException interruptedException) {
			if (thread.isAlive()) {
				thread.interrupt();
			}
			build.setResult(Result.ABORTED);
			throw interruptedException;
		}

		if (thread.isAlive()) {
			listener.getLogger().println("Aborted by timeout.");
			build.setResult(Result.ABORTED);
		}

		Throwable throwable = exceptionReference.get();
		if (throwable != null) {
			if (throwable instanceof InterruptedException) {
				throw (InterruptedException) throwable;
			} else if (throwable instanceof IOException) {
				throw (IOException) throwable;
			} else {
				build.setResult(Result.FAILURE);
			}
		}

		Result result = resultReference.get();
		if (result != null) {
			build.setResult(result);
		}

		return true;
	}

	public boolean isInMode(String mode) {
		return getMode().equals(mode) ? true : false;
	}

	private Thread createExecutionThread(final Node node, final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener,
			final AtomicReference<Result> resultReference, final AtomicReference<Throwable> exceptionReference) {
		Runnable runnable = new KiuwanRunnable(getDescriptor(), this, node, build, launcher, listener, resultReference, exceptionReference);
		return new Thread(runnable);
	}

	/*
	 * Configuration getters
	 */

	public String getMode() {
		String mode = null;
		if (this.selectedMode == null) {
			this.selectedMode = DEFAULT_MODE;
		}
		mode = this.selectedMode.name();

		return mode;
	}

	public Double getUnstableThreshold() {
		Double ret = new Double(0);
		if (this.unstableThreshold != null) {
			ret = this.unstableThreshold;
		}
		return ret;
	}

	public Double getFailureThreshold() {
		Double ret = new Double(0);
		if (this.failureThreshold != null) {
			ret = this.failureThreshold;
		}
		return ret;
	}

	public Mode getSelectedMode() {
		return selectedMode;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public String getLabel() {
		return this.label;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public String getIncludes() {
		return this.includes;
	}

	public String getExcludes() {
		return this.excludes;
	}

	public Integer getTimeout() {
		return this.timeout;
	}

	public Boolean getIndicateLanguages() {
		return indicateLanguages;
	}

	public String getMeasure() {
		return measure;
	}

	public String getLanguages() {
		return this.languages;
	}

	public String getApplicationName_dm() {
		return applicationName_dm;
	}

	public String getLabel_dm() {
		return label_dm;
	}

	public String getEncoding_dm() {
		return encoding_dm;
	}

	public String getIncludes_dm() {
		return includes_dm;
	}

	public String getExcludes_dm() {
		return excludes_dm;
	}

	public Integer getTimeout_dm() {
		return timeout_dm;
	}

	public Integer getTimeout_em() {
		return timeout_em;
	}

	public Boolean getIndicateLanguages_dm() {
		return indicateLanguages_dm;
	}

	public String getLanguages_dm() {
		return languages_dm;
	}

	public String getChangeRequest_dm() {
		return changeRequest_dm;
	}

	public String getAnalysisScope_dm() {
		return analysisScope_dm;
	}

	public Boolean getWaitForAuditResults_dm() {
		return waitForAuditResults_dm;
	}

	public String getBranch_dm() {
		return branch_dm;
	}

	public String getChangeRequestStatus_dm() {
		return changeRequestStatus_dm;
	}

	public String getCommandArgs_em() {
		return commandArgs_em;
	}

	public String getExtraParameters_em() {
		return extraParameters_em;
	}

	public String getMarkBuildWhenNoPass_dm() {
		return markBuildWhenNoPass_dm;
	}

	public String getSuccessResultCodes_em() {
		return successResultCodes_em;
	}

	public String getUnstableResultCodes_em() {
		return unstableResultCodes_em;
	}

	public String getFailureResultCodes_em() {
		return failureResultCodes_em;
	}

	public String getNotBuiltResultCodes_em() {
		return notBuiltResultCodes_em;
	}

	public String getAbortedResultCodes_em() {
		return abortedResultCodes_em;
	}

	public String getMarkAsInOtherCases_em() {
		return markAsInOtherCases_em;
	}

	public String getApplicationName_ci() {
		return applicationName_ci;
	}

	public String getLabel_ci() {
		return label_ci;
	}

	public String getEncoding_ci() {
		return encoding_ci;
	}

	public String getIncludes_ci() {
		return includes_ci;
	}

	public String getExcludes_ci() {
		return excludes_ci;
	}

	public Integer getTimeout_ci() {
		return timeout_ci;
	}

	public Boolean getIndicateLanguages_ci() {
		return indicateLanguages_ci;
	}

	public String getLanguages_ci() {
		return languages_ci;
	}

	public Boolean getWaitForAuditResults_ci() {
		return waitForAuditResults_ci;
	}

	public String getMarkBuildWhenNoPass_ci() {
		return markBuildWhenNoPass_ci;
	}

}