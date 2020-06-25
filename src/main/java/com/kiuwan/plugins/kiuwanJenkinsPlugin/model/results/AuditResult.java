package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditResult {

	private String auditName;
	private String description;
	private Double approvalThreshold;
	private String overallResult;
	private Double score;
	
	private List<CheckpointResult> checkpointResults;

	public String getAuditName() { return auditName; }
	public void setAuditName(String auditName) { this.auditName = auditName; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public Double getApprovalThreshold() { return approvalThreshold; }
	public void setApprovalThreshold(Double approvalThreshold) { this.approvalThreshold = approvalThreshold; }

	public String getOverallResult() { return overallResult; }
	public void setOverallResult(String overallResult) { this.overallResult = overallResult; }

	public Double getScore() { return score; }
	public void setScore(Double score) { this.score = score; }

	public List<CheckpointResult> getCheckpointResults() { return checkpointResults; }
	public void setCheckpointResults(List<CheckpointResult> checkpointResults) { this.checkpointResults = checkpointResults; }

}
