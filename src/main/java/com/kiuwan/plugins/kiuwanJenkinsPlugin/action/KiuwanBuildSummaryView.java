package com.kiuwan.plugins.kiuwanJenkinsPlugin.action;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData.OBSOLESCENCE_RISK;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData.VULNERABILITY_RISK;

import java.text.NumberFormat;
import java.util.Map;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.MetricValue;

/**
 * This class deals with the particularities of a kiuwan analysis result data structure
 * and eases using those results in a jelly page
 * @author gsimmross
 */
public class KiuwanBuildSummaryView {

	private AnalysisResult analysisResult;
	
	public KiuwanBuildSummaryView(AnalysisResult analysisResult) {
		super();
		this.analysisResult = analysisResult;
	}
	
	public boolean isAvailable() {
		return analysisResult != null;
	}
	
	public boolean isBaseline() {
		return AnalysisResult.ANALYSIS_SCOPE_BASELINE.equals(analysisResult.getAnalysisScope());
	}
	
	public boolean isDelivery() {
		return 
			AnalysisResult.ANALYSIS_SCOPE_COMPLETE_DELIVERY.equals(analysisResult.getAnalysisScope()) ||
			AnalysisResult.ANALYSIS_SCOPE_PARTIAL_DELIVERY.equals(analysisResult.getAnalysisScope());
	}
	
	public Integer getLinesOfCode() {
		Double loc = getMainMetricValue("Lines of code");
		return loc != null ? loc.intValue() : null;
	}
	
	public boolean hasSecurity() {
		return analysisResult.getSecurityMetrics() != null && !analysisResult.getSecurityMetrics().isEmpty();
	}
	
	public boolean hasQuality() {
		MetricValue riskIndex = analysisResult.getRiskIndex();
		MetricValue qualityIndicator = analysisResult.getQualityIndicator();
		MetricValue effortToTarget = analysisResult.getEffortToTarget();
		return riskIndex != null && qualityIndicator != null && effortToTarget != null;
	}
	
	public boolean hasInsights() {
		return analysisResult.getInsightsData() != null;
	}
	
	public int getSecurityRating() {
		Double securityRating = (Double) analysisResult.getSecurityMetrics().get("Rating");
		return securityRating != null ? securityRating.intValue() : 0;
	}
	
	public boolean hasSecurityRating(int rating) {
		return getSecurityRating() >= rating;
	}
	
	public Integer getSecurityVulnerabilities(String key) {
		Map<?, ?> vulnerabilities = (Map<?, ?>) analysisResult.getSecurityMetrics().get("Vulnerabilities");
		Object value = vulnerabilities.get(key);
		Integer valueInt = null;
		if (value instanceof Double) {
			Double valueDouble = (Double) value;
			valueInt = valueDouble != null ? valueDouble.intValue() : null;
		} else if (value instanceof Integer) {
			valueInt = (Integer) value;
		}
		
		return valueInt;
	}
	
	public Double getMainMetricValue(String name) {
		if (analysisResult.getMainMetrics() != null) {
			for (MetricValue metricValue : analysisResult.getMainMetrics()) {
				if (name.equals(metricValue.getName())) {
					return metricValue.getValue();
				}
			}
		}
		return null;
	}
	
	public Double getQualityIndicator(String name) {
		if (analysisResult.getQualityIndicator().getChildren() != null) {
			for (MetricValue metricValue : analysisResult.getQualityIndicator().getChildren()) {
				if (name.equals(metricValue.getName())) {
					return metricValue.getValue();
				}
			}
		}
		return null;
	}
	
	public Integer getInsightRiskMap(String key, String entryKey) {
		Map<String, Integer> riskMap = analysisResult.getInsightsData().getRiskMap(key);
		Integer value = riskMap.get(entryKey);
		return value != null ? value : 0;
	}
	
	public Integer getSecurityVulnerabilitiesTotal() {
		return getSecurityVulnerabilities("Total");
	}
	
	public Integer getSecurityVulnerabilitiesVeryHigh() {
		return getSecurityVulnerabilities("VeryHigh");
	}
	
	public Integer getSecurityVulnerabilitiesHigh() {
		return getSecurityVulnerabilities("High");
	}
	
	public Integer getSecurityVulnerabilitiesNormal() {
		return getSecurityVulnerabilities("Normal");
	}
	
	public Integer getSecurityVulnerabilitiesLow() {
		return getSecurityVulnerabilities("Low");
	}
	
	public Integer getTotalDefects() {
		Double totalDefects = (Double) getMainMetricValue("Total defects");
		return totalDefects != null ? totalDefects.intValue() : null;
	}
	
	public String getRiskIndex() {
		return formatDouble(analysisResult.getRiskIndex().getValue());
	}
	
	public String getQualityIndicator() {
		return formatDouble(analysisResult.getQualityIndicator().getValue());
	}
	
	public String getEffortToTarget() {
		return formatDouble(analysisResult.getEffortToTarget().getValue());
	}
	
	public String getEfficiency() {
		return formatDouble(getQualityIndicator("Efficiency"));
	}
	
	public String getMaintainability() {
		return formatDouble(getQualityIndicator("Maintainability"));
	}
	
	public String getPortability() {
		return formatDouble(getQualityIndicator("Portability"));
	}
	
	public String getReliability() {
		return formatDouble(getQualityIndicator("Reliability"));
	}
	
	public String getSecurity() {
		return formatDouble(getQualityIndicator("Security"));
	}
	
	public Integer getTotalComponents() {
		return analysisResult.getInsightsData().getComponents();
	}
	
	public Integer getDuplicatedComponents() {
		return analysisResult.getInsightsData().getDuplicates();
	}
	
	public Integer getInsightVulnerabilityRiskHigh() {
		return getInsightRiskMap(VULNERABILITY_RISK, "HIGH");
	}
	
	public Integer getInsightVulnerabilityRiskMedium() {
		return getInsightRiskMap(VULNERABILITY_RISK, "MEDIUM");
	}
	
	public Integer getInsightVulnerabilityRiskLow() {
		return getInsightRiskMap(VULNERABILITY_RISK, "LOW");
	}
	
	public Integer getInsightVulnerabilityRiskNone() {
		return getInsightRiskMap(VULNERABILITY_RISK, "NONE");
	}
	
	public Integer getInsightObsolescenceRiskHigh() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, "HIGH");
	}
	
	public Integer getInsightObsolescenceRiskMedium() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, "MEDIUM");
	}
	
	public Integer getInsightObsolescenceRiskLow() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, "LOW");
	}
	
	public Integer getInsightObsolescenceRiskNone() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, "NONE");
	}
	
	public Integer getInsightLicenseRiskHigh() {
		return getInsightRiskMap(InsightsData.LICENSE_RISK, "HIGH");
	}
	
	public Integer getInsightLicenseRiskNone() {
		return getInsightRiskMap(InsightsData.LICENSE_RISK, "NONE");
	}
	
	public Integer getInsightLicenseRiskUnknown() {
		return getInsightRiskMap(InsightsData.LICENSE_RISK, "UNKNOWN");
	}
	
	public String getUrl() {
		return isBaseline() ? analysisResult.getAnalysisURL() : analysisResult.getAuditResultURL();
	}
	
	private static String formatDouble(Double number) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		return nf.format(number);
	}
	
}
