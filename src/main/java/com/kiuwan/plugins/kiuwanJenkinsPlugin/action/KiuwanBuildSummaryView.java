package com.kiuwan.plugins.kiuwanJenkinsPlugin.action;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData.LICENSE_RISK;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData.OBSOLESCENCE_RISK;
import static com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData.VULNERABILITY_RISK;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AuditResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.CheckpointResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.MetricValue;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.ViolatedRule;

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
		Double loc = getMainMetricValue(AnalysisResult.MAIN_METRICS_LOC);
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
	
	public boolean hasAuditResults() {
		return analysisResult.getAuditResult() != null;
	}
	
	public int getSecurityRating() {
		Double securityRating = (Double) analysisResult.getSecurityMetrics().get(AnalysisResult.SECURITY_METRICS_RATING);
		return securityRating != null ? securityRating.intValue() : 0;
	}
	
	public boolean hasSecurityRating(int rating) {
		return getSecurityRating() >= rating;
	}
	
	public Integer getSecurityVulnerabilities(String key) {
		Map<?, ?> vulnerabilities = (Map<?, ?>) analysisResult.getSecurityMetrics().get(AnalysisResult.SECURITY_METRICS_VULNERABILITIES);
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
		return getSecurityVulnerabilities(AnalysisResult.SECURITY_METRICS_VULNERABILITIES_TOTAL);
	}
	
	public Integer getSecurityVulnerabilitiesVeryHigh() {
		return getSecurityVulnerabilities(AnalysisResult.SECURITY_METRICS_VULNERABILITIES_VERY_HIGH);
	}
	
	public Integer getSecurityVulnerabilitiesHigh() {
		return getSecurityVulnerabilities(AnalysisResult.SECURITY_METRICS_VULNERABILITIES_HIGH);
	}
	
	public Integer getSecurityVulnerabilitiesNormal() {
		return getSecurityVulnerabilities(AnalysisResult.SECURITY_METRICS_VULNERABILITIES_NORMAL);
	}
	
	public Integer getSecurityVulnerabilitiesLow() {
		return getSecurityVulnerabilities(AnalysisResult.SECURITY_METRICS_VULNERABILITIES_LOW);
	}
	
	public Integer getTotalDefects() {
		Double totalDefects = (Double) getMainMetricValue(AnalysisResult.MAIN_METRICS_TOTAL_DEFECTS);
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
		return formatDouble(getQualityIndicator(AnalysisResult.QUALITY_INDICATOR_EFFICIENCY));
	}
	
	public String getMaintainability() {
		return formatDouble(getQualityIndicator(AnalysisResult.QUALITY_INDICATOR_MAINTAINABILITY));
	}
	
	public String getPortability() {
		return formatDouble(getQualityIndicator(AnalysisResult.QUALITY_INDICATOR_PORTABILITY));
	}
	
	public String getReliability() {
		return formatDouble(getQualityIndicator(AnalysisResult.QUALITY_INDICATOR_RELIABILITY));
	}
	
	public String getSecurity() {
		return formatDouble(getQualityIndicator(AnalysisResult.QUALITY_INDICATOR_SECURITY));
	}
	
	public Integer getTotalComponents() {
		return analysisResult.getInsightsData().getComponents();
	}
	
	public Integer getDuplicatedComponents() {
		return analysisResult.getInsightsData().getDuplicates();
	}
	
	public Integer getInsightVulnerabilityRiskHigh() {
		return getInsightRiskMap(VULNERABILITY_RISK, InsightsData.HIGH);
	}
	
	public Integer getInsightVulnerabilityRiskMedium() {
		return getInsightRiskMap(VULNERABILITY_RISK, InsightsData.MEDIUM);
	}
	
	public Integer getInsightVulnerabilityRiskLow() {
		return getInsightRiskMap(VULNERABILITY_RISK, InsightsData.LOW);
	}
	
	public Integer getInsightVulnerabilityRiskNone() {
		return getInsightRiskMap(VULNERABILITY_RISK, InsightsData.NONE);
	}
	
	public Integer getInsightObsolescenceRiskHigh() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, InsightsData.HIGH);
	}
	
	public Integer getInsightObsolescenceRiskMedium() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, InsightsData.MEDIUM);
	}
	
	public Integer getInsightObsolescenceRiskLow() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, InsightsData.LOW);
	}
	
	public Integer getInsightObsolescenceRiskNone() {
		return getInsightRiskMap(OBSOLESCENCE_RISK, InsightsData.NONE);
	}
	
	public Integer getInsightLicenseRiskHigh() {
		return getInsightRiskMap(LICENSE_RISK, InsightsData.HIGH);
	}
	
	public Integer getInsightLicenseRiskNone() {
		return getInsightRiskMap(LICENSE_RISK, InsightsData.NONE);
	}
	
	public Integer getInsightLicenseRiskUnknown() {
		return getInsightRiskMap(LICENSE_RISK, InsightsData.UNKNOWN);
	}
	
	public String getUrl() {
		return isBaseline() ? analysisResult.getAnalysisURL() : analysisResult.getAuditResultURL();
	}
	
	public boolean auditPassed() {
		return AuditResult.OVERALL_RESULT_OK.equals(analysisResult.getAuditResult().getOverallResult());
	}
	
	public String getAuditScore() {
		return formatDouble(analysisResult.getAuditResult().getScore());
	}
	
	public int getFailedCheckpointsCount() {
		return getAuditFixStatistics().getFailedCheckpointsCount();
	}
	
	public int getTotalCheckpointsCount() {
		return getAuditFixStatistics().getTotalCheckpointsCount();
	}
	
	public List<CheckpointResult> getCheckpointResults() {
		return analysisResult.getAuditResult().getCheckpointResults();
	}
	
	public boolean isCheckpointResultOK(CheckpointResult checkpointResult) {
		return checkpointResult != null && CheckpointResult.CHECKPOINT_RESULT_OK.equals(checkpointResult.getResult());
	}
	
	public int getAuditFixTotalDefectsCount() {
		return getAuditFixStatistics().getDefects();
	}
	
	public int getAuditFixTotalFilesCount() {
		return getAuditFixStatistics().getFiles();
	}
	
	public String getAuditEffort() {
		StringBuilder sb = new StringBuilder();
		
		int effortH = getAuditFixStatistics().getEffortHours();
		int effortM = getAuditFixStatistics().getEffortMinutes();
		
		sb.append(effortH + " hours ");
		sb.append(effortM + " minutes");
		
		return sb.toString();
	}
	
	private static String formatDouble(Double number) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);
		return nf.format(number);
	}
	
	private AuditFixStatistics auditFixStatistics;
	
	private AuditFixStatistics getAuditFixStatistics() {
		if (auditFixStatistics == null) {
			auditFixStatistics = new AuditFixStatistics(analysisResult);
		}
		return auditFixStatistics;
	}
	
	private static class AuditFixStatistics {
		
		private int totalCheckpointsCount = 0;
		private int failedCheckpointsCount = 0;
		private int defects = 0;
		private int files = 0;
		private int effortHours = 0;
		private int effortMinutes = 0;
		
		private AuditFixStatistics(AnalysisResult analysisResult) {
			if (analysisResult.getAuditResult() != null) {
				List<CheckpointResult> checkpointResults = analysisResult.getAuditResult().getCheckpointResults();
				if (checkpointResults != null) {
					totalCheckpointsCount = checkpointResults.size();
					for (CheckpointResult cr : checkpointResults) {
						if (!CheckpointResult.CHECKPOINT_RESULT_OK.equals(cr.getResult())) {
							failedCheckpointsCount++;
							if (cr.getViolatedRules() != null) {
								for (ViolatedRule vr : cr.getViolatedRules()) {
									defects += vr.getDefectsCount();
									files += vr.getFilesCount();
									String effortStr = vr.getEffort();
									int[] parsedEffort = parseEffort(effortStr);
									effortHours += parsedEffort[0];
									effortMinutes += parsedEffort[1];
								}
							}
						}
					}
				}
			}
		}

		public int getTotalCheckpointsCount() { return totalCheckpointsCount; }
		public int getFailedCheckpointsCount() { return failedCheckpointsCount; }
		public int getDefects() { return defects; }
		public int getFiles() { return files; }
		public int getEffortHours() { return effortHours; }
		public int getEffortMinutes() { return effortMinutes; }
		
		/** Kiuwan returns things like "19h 18" or "30m" or "06m" */
		private static int[] parseEffort(String effortStr) {
			int[] effortHoursMinutes = new int[] { 0, 0 };
			
			String hoursStr = null;
			String minsStr = null;
			
			int indexH = effortStr.lastIndexOf("h");
			int indexM = effortStr.lastIndexOf("m");
			
			if (indexH >= 0) {
				hoursStr = effortStr.substring(0, indexH);
				minsStr = effortStr.substring(indexH + 1, effortStr.length());
			
			} else {
				minsStr = effortStr.substring(0, indexM);
			}
			
			try { effortHoursMinutes[0] = Integer.parseInt(hoursStr); } catch (NumberFormatException e) { }
			try { effortHoursMinutes[0] = Integer.parseInt(minsStr); } catch (NumberFormatException e) { }
			
			return effortHoursMinutes;
		}
		
	}
	
}
