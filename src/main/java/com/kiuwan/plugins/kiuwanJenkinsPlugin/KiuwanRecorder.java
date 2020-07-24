package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.Mode;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.runnable.KiuwanRunnable;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;

public class KiuwanRecorder extends Recorder implements SimpleBuildStep {

	public final static Long TIMEOUT_MARGIN_MILLIS = 5000L;
	public final static Long TIMEOUT_MARGIN_SECONDS = SECONDS.convert(TIMEOUT_MARGIN_MILLIS, MILLISECONDS);
	public final static Mode DEFAULT_MODE = Mode.STANDARD_MODE;
	
	private String connectionProfileUuid;
	private String sourcePath;
	
	private Mode selectedMode;

	private String applicationName;
	private String applicationName_dm;
	private String label;
	private String label_dm;
	private String encoding;
	private String encoding_dm;
	private String includes;
	private String includes_dm;
	private String excludes;
	private String excludes_dm;
	private Integer timeout;
	private Integer timeout_dm;
	private Integer timeout_em;
	private Boolean indicateLanguages;
	private String measure;
	private String languages;
	private Boolean indicateLanguages_dm;
	private String languages_dm;
	private Double unstableThreshold;
	private Double failureThreshold;
	private String changeRequest_dm;
	private String analysisScope_dm;
	private Boolean waitForAuditResults_dm;
	private String branch_dm;
	private String changeRequestStatus_dm;
	private String commandArgs_em;
	private String extraParameters_em;
	private String markBuildWhenNoPass_dm;
	private String successResultCodes_em;
	private String unstableResultCodes_em;
	private String failureResultCodes_em;
	private String notBuiltResultCodes_em;
	private String abortedResultCodes_em;
	private String markAsInOtherCases_em;
	
	@DataBoundConstructor
	public KiuwanRecorder() {
		super();
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}
	
	@Override
	public KiuwanRecorderDescriptor getDescriptor() {
		return (KiuwanRecorderDescriptor) super.getDescriptor();
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		
		long startTime = System.currentTimeMillis();

		Integer timeout = null;
		Mode selectedMode = getSelectedMode();
		if (Mode.DELIVERY_MODE.equals(selectedMode)) {
			timeout = getTimeout_dm();
			
		} else if (Mode.EXPERT_MODE.equals(selectedMode)) {
			timeout = getTimeout_em();
			
		} else {
			timeout = getTimeout();
		}
		
		long endTime = startTime + TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MINUTES);
		
		Node node = Computer.currentComputer().getNode();
		AtomicReference<Throwable> exceptionReference = new AtomicReference<Throwable>();
		AtomicReference<Result> resultReference = new AtomicReference<Result>();
		
		KiuwanGlobalConfigDescriptor descriptor = KiuwanGlobalConfigDescriptor.get();
		KiuwanConnectionProfile connectionProfile = descriptor.getConnectionProfile(connectionProfileUuid);
		
		Runnable runnable = new KiuwanRunnable(this, workspace, connectionProfile, descriptor, node, run, 
			launcher, listener, exceptionReference, resultReference);
		
		Thread thread = new Thread(runnable);
		thread.start();

		long currentTime = System.currentTimeMillis();
		try {
			while (thread.isAlive() && currentTime < endTime) {
				TimeUnit.MILLISECONDS.sleep(TIMEOUT_MARGIN_MILLIS);
				currentTime = System.currentTimeMillis();
			}
		} catch (InterruptedException interruptedException) {
			if (thread.isAlive()) {
				thread.interrupt();
			}
			run.setResult(Result.ABORTED);
			throw interruptedException;
		}

		if (thread.isAlive()) {
			listener.getLogger().println("Aborted by timeout.");
			run.setResult(Result.ABORTED);
		}

		Throwable throwable = exceptionReference.get();
		if (throwable != null) {
			if (throwable instanceof InterruptedException) {
				throw (InterruptedException) throwable;
			} else if (throwable instanceof IOException) {
				throw (IOException) throwable;
			} else {
				run.setResult(Result.FAILURE);
			}
		}

		Result result = resultReference.get();
		if (result != null) {
			run.setResult(result);
		}

	}

	public boolean isInMode(String mode) {
		return getMode().equals(mode) ? true : false;
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

	public String getConnectionProfileUuid() { return connectionProfileUuid; }
	public String getSourcePath() { return sourcePath; }
	public Mode getSelectedMode() { return selectedMode; }
	public String getApplicationName() { return this.applicationName; }
	public String getLabel() { return this.label; }
	public String getEncoding() { return this.encoding; }
	public String getIncludes() { return this.includes; }
	public String getExcludes() { return this.excludes; }
	public Integer getTimeout() { return this.timeout; }
	public Boolean getIndicateLanguages() { return indicateLanguages; }
	public String getMeasure() { return measure; }
	public String getLanguages() { return this.languages; }
	public String getApplicationName_dm() { return applicationName_dm; }
	public String getLabel_dm() { return label_dm; }
	public String getEncoding_dm() { return encoding_dm; }
	public String getIncludes_dm() { return includes_dm; }
	public String getExcludes_dm() { return excludes_dm; }
	public Integer getTimeout_dm() { return timeout_dm; }
	public Integer getTimeout_em() { return timeout_em; }
	public Boolean getIndicateLanguages_dm() { return indicateLanguages_dm; }
	public String getLanguages_dm() { return languages_dm; }
	public String getChangeRequest_dm() { return changeRequest_dm; }
	public String getAnalysisScope_dm() { return analysisScope_dm; }
	public Boolean getWaitForAuditResults_dm() { return waitForAuditResults_dm; }
	public String getBranch_dm() { return branch_dm; }
	public String getChangeRequestStatus_dm() { return changeRequestStatus_dm; }
	public String getCommandArgs_em() { return commandArgs_em; }
	public String getExtraParameters_em() { return extraParameters_em; }
	public String getMarkBuildWhenNoPass_dm() { return markBuildWhenNoPass_dm; }
	public String getSuccessResultCodes_em() { return successResultCodes_em; }
	public String getUnstableResultCodes_em() { return unstableResultCodes_em; }
	public String getFailureResultCodes_em() { return failureResultCodes_em; }
	public String getNotBuiltResultCodes_em() { return notBuiltResultCodes_em; }
	public String getAbortedResultCodes_em() { return abortedResultCodes_em; }
	public String getMarkAsInOtherCases_em() { return markAsInOtherCases_em; }

	@DataBoundSetter public void setConnectionProfileUuid(String connectionProfileUuid) { this.connectionProfileUuid = connectionProfileUuid; }
	@DataBoundSetter public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
	@DataBoundSetter public void setMode(String mode) { this.selectedMode = (mode == null ? DEFAULT_MODE : Mode.valueOf(mode)); }
	@DataBoundSetter public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
	@DataBoundSetter public void setApplicationName_dm(String applicationName_dm) { this.applicationName_dm = applicationName_dm; }
	@DataBoundSetter public void setLabel(String label) { this.label = label; }
	@DataBoundSetter public void setLabel_dm(String label_dm) { this.label_dm = label_dm; }
	@DataBoundSetter public void setEncoding(String encoding) { this.encoding = encoding; }
	@DataBoundSetter public void setEncoding_dm(String encoding_dm) { this.encoding_dm = encoding_dm; }
	@DataBoundSetter public void setIncludes(String includes) { this.includes = includes; }
	@DataBoundSetter public void setIncludes_dm(String includes_dm) { this.includes_dm = includes_dm; }
	@DataBoundSetter public void setExcludes(String excludes) { this.excludes = excludes; }
	@DataBoundSetter public void setExcludes_dm(String excludes_dm) { this.excludes_dm = excludes_dm; }
	@DataBoundSetter public void setTimeout(Integer timeout) { this.timeout = timeout; }
	@DataBoundSetter public void setTimeout_dm(Integer timeout_dm) { this.timeout_dm = timeout_dm; }
	@DataBoundSetter public void setTimeout_em(Integer timeout_em) { this.timeout_em = timeout_em; }
	@DataBoundSetter public void setIndicateLanguages(Boolean indicateLanguages) { this.indicateLanguages = indicateLanguages; }
	@DataBoundSetter public void setMeasure(String measure) { this.measure = measure; }
	@DataBoundSetter public void setLanguages(String languages) { this.languages = languages; }
	@DataBoundSetter public void setIndicateLanguages_dm(Boolean indicateLanguages_dm) { this.indicateLanguages_dm = indicateLanguages_dm; }
	@DataBoundSetter public void setLanguages_dm(String languages_dm) { this.languages_dm = languages_dm; }
	@DataBoundSetter public void setUnstableThreshold(Double unstableThreshold) { this.unstableThreshold = unstableThreshold; }
	@DataBoundSetter public void setFailureThreshold(Double failureThreshold) { this.failureThreshold = failureThreshold; }
	@DataBoundSetter public void setChangeRequest_dm(String changeRequest_dm) { this.changeRequest_dm = changeRequest_dm; }
	@DataBoundSetter public void setAnalysisScope_dm(String analysisScope_dm) { this.analysisScope_dm = analysisScope_dm; }
	@DataBoundSetter public void setWaitForAuditResults_dm(Boolean waitForAuditResults_dm) { this.waitForAuditResults_dm = waitForAuditResults_dm; }
	@DataBoundSetter public void setBranch_dm(String branch_dm) { this.branch_dm = branch_dm; }
	@DataBoundSetter public void setChangeRequestStatus_dm(String changeRequestStatus_dm) { this.changeRequestStatus_dm = changeRequestStatus_dm; }
	@DataBoundSetter public void setCommandArgs_em(String commandArgs_em) { this.commandArgs_em = commandArgs_em; }
	@DataBoundSetter public void setExtraParameters_em(String extraParameters_em) { this.extraParameters_em = extraParameters_em; }
	@DataBoundSetter public void setMarkBuildWhenNoPass_dm(String markBuildWhenNoPass_dm) { this.markBuildWhenNoPass_dm = markBuildWhenNoPass_dm; }
	@DataBoundSetter public void setSuccessResultCodes_em(String successResultCodes_em) { this.successResultCodes_em = successResultCodes_em; }
	@DataBoundSetter public void setUnstableResultCodes_em(String unstableResultCodes_em) { this.unstableResultCodes_em = unstableResultCodes_em; }
	@DataBoundSetter public void setFailureResultCodes_em(String failureResultCodes_em) { this.failureResultCodes_em = failureResultCodes_em; }
	@DataBoundSetter public void setNotBuiltResultCodes_em(String notBuiltResultCodes_em) { this.notBuiltResultCodes_em = notBuiltResultCodes_em; }
	@DataBoundSetter public void setAbortedResultCodes_em(String abortedResultCodes_em) { this.abortedResultCodes_em = abortedResultCodes_em; }
	@DataBoundSetter public void setMarkAsInOtherCases_em(String markAsInOtherCases_em) { this.markAsInOtherCases_em = markAsInOtherCases_em; }

}