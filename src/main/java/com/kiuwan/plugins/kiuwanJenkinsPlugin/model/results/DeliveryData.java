package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryData {

	private Long modelId;
	private Long baseAnalysisId;
	
	public Long getModelId() { return modelId; }
	public void setModelId(Long modelId) { this.modelId = modelId; }

	public Long getBaseAnalysisId() { return baseAnalysisId; }
	public void setBaseAnalysisId(Long baseAnalysisId) { this.baseAnalysisId = baseAnalysisId; }
	
}
