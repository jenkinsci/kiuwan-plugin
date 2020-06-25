package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InsightsData {
	
	public static final String HIGH = "HIGH";
	public static final String MEDIUM = "MEDIUM";
	public static final String LOW = "LOW";
	public static final String NONE = "NONE";
	public static final String UNKNOWN = "UNKNOWN";
	
	public static final String VULNERABILITY_RISK = "Vulnerability risk";
	public static final String OBSOLESCENCE_RISK = "Obsolescence risk";
	public static final String LICENSE_RISK = "License risk";
	
	private int components;
	private int duplicates;
	private int licenses;
	private int vulnerabilities;
	private List<InsightsRiskData> risks;
	
	public Map<String, Integer> getRiskMap(String name) {
		if (risks != null) {
			for (InsightsRiskData risk : risks) {
				if (name.equals(risk.getName())) {
					return risk.getRisk();
				}
			}
		}
		return null;
	}
	
	public int getComponents() { return components; }
	public void setComponents(int components) { this.components = components; }
	
	public int getDuplicates() { return duplicates; }
	public void setDuplicates(int duplicates) { this.duplicates = duplicates; }
	
	public int getLicenses() { return licenses; }
	public void setLicenses(int licenses) { this.licenses = licenses; }
	
	public int getVulnerabilities() { return vulnerabilities; }
	public void setVulnerabilities(int vulnerabilities) { this.vulnerabilities = vulnerabilities; }
	
	public List<InsightsRiskData> getRisks() { return risks; }
	public void setRisks(List<InsightsRiskData> risks) { this.risks = risks; }
	
}
