package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InsightsRiskData {
	
	private String name;
	private Map<String, Integer> risk;
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public Map<String, Integer> getRisk() { return risk; }
	public void setRisk(Map<String, Integer> risk) { this.risk = risk; }

}
