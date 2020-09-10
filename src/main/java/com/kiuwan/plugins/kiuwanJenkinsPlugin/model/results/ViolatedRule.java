package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.List;

public class ViolatedRule {

	private String ruleCode;
	private Long modelId;
	private Long defectsCount;
	private Long suppressedDefectsCount;
	private Long filesCount;
	private String effort;
	private String characteristic;
	private String vulnerabilityType;
	private List<String> tags;
	private String priority;
	private String language;
	private String filesHref;
	
	public String getRuleCode() { return ruleCode; }
	public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
	
	public Long getModelId() { return modelId; }
	public void setModelId(Long modelId) { this.modelId = modelId; }
	
	public Long getDefectsCount() { return defectsCount; }
	public void setDefectsCount(Long defectsCount) { this.defectsCount = defectsCount; }
	
	public Long getSuppressedDefectsCount() { return suppressedDefectsCount; }
	public void setSuppressedDefectsCount(Long suppressedDefectsCount) { this.suppressedDefectsCount = suppressedDefectsCount; }
	
	public Long getFilesCount() { return filesCount; }
	public void setFilesCount(Long filesCount) { this.filesCount = filesCount; }
	
	public String getEffort() { return effort; }
	public void setEffort(String effort) { this.effort = effort; }
	
	public String getCharacteristic() { return characteristic; }
	public void setCharacteristic(String characteristic) { this.characteristic = characteristic; }
	
	public String getVulnerabilityType() { return vulnerabilityType; }
	public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }
	
	public List<String> getTags() { return tags; }
	public void setTags(List<String> tags) { this.tags = tags; }
	
	public String getPriority() { return priority; }
	public void setPriority(String priority) { this.priority = priority; }
	
	public String getLanguage() { return language; }
	public void setLanguage(String language) { this.language = language; }
	
	public String getFilesHref() { return filesHref; }
	public void setFilesHref(String filesHref) { this.filesHref = filesHref; }

}