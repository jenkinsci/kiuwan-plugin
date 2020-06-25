package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents summary of an application analysis, with top-level metrics */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

	public static final String ANALYSIS_STATUS_EXECUTING = "EXECUTING";
	public static final String ANALYSIS_STATUS_FINISHED = "FINISHED";
	public static final String ANALYSIS_STATUS_FINISHED_WITH_ERROR = "FINISHED_WITH_ERROR";
	public static final String MAIN_METRICS_AUDIT_RESULT = "Audit result";
	
	public static final String ANALYSIS_SCOPE_BASELINE = "Baseline";
	public static final String ANALYSIS_SCOPE_COMPLETE_DELIVERY = "Complete delivery";
	public static final String ANALYSIS_SCOPE_PARTIAL_DELIVERY = "Partial delivery";
	
	private String name;
	private String description;
	private String auditName;
	private String label;
	private Date date;
	private Long modelId;
	private String encoding;
	private String analysisCode;
	private String analysisURL;
	private String auditResultURL;
	private String applicationBusinessValue;
	private Map<String, String> applicationPortfolios;
	private String analysisBusinessValue;
	private Map<String, String> analysisPortfolios;
	private String analysisStatus;
	private String analysisScope;
	private String qualityModel;
	private String orderedBy;
	
	private List<Language> languages;
	private List<MetricValue> mainMetrics;
	
	private MetricValue riskIndex;
	private MetricValue qualityIndicator;
	private MetricValue effortToTarget;
	private Map<String, Object> securityMetrics;
	private InsightsData insightsData;
	
	// Audit Result
	private String changeRequest;
	private String changeRequestStatus;
	private String branchName;	
	private String baselineAnalysisCode;	
	private DeliveryFiles deliveryFiles;
	private DeliveryDefects deliveryDefects;
	private AuditResult auditResult;

	@Override 
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public String getAuditName() { return auditName; }
	public void setAuditName(String auditName) { this.auditName = auditName; }
	
	public String getApplicationBusinessValue() { return applicationBusinessValue; }
	public void setApplicationBusinessValue(String applicationBusinessValue) { this.applicationBusinessValue = applicationBusinessValue; }
	
	public Map<String, String> getApplicationPortfolios() { return applicationPortfolios; }
	public void setApplicationPortfolios(Map<String, String> applicationPortfolios) { this.applicationPortfolios = applicationPortfolios; }

	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }

	public Date getDate() { return date; }
	public void setDate(Date date) { this.date = date; }
	
	public Long getModelId() { return modelId; }
	public void setModelId(Long modelId) { this.modelId = modelId; }

	public String getEncoding() { return encoding; }
	public void setEncoding(String encoding) { this.encoding = encoding; }

	public String getAnalysisCode() { return analysisCode; }
	public void setAnalysisCode(String analysisCode) { this.analysisCode = analysisCode; }
	
	public String getAnalysisURL() { return analysisURL; }
	public void setAnalysisURL(String analysisURL) { this.analysisURL = analysisURL; }
	
	public String getAuditResultURL() { return auditResultURL; }
	public void setAuditResultURL(String auditResultURL) { this.auditResultURL = auditResultURL; }

	public String getAnalysisBusinessValue() { return analysisBusinessValue; }
	public void setAnalysisBusinessValue(String analysisBusinessValue) { this.analysisBusinessValue = analysisBusinessValue; }

	public Map<String, String> getAnalysisPortfolios() { return analysisPortfolios; }
	public void setAnalysisPortfolios(Map<String, String> analysisPortfolios) { this.analysisPortfolios = analysisPortfolios; }

	public String getAnalysisStatus() { return analysisStatus; }
	public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
	
	public String getAnalysisScope() { return analysisScope; }
	public void setAnalysisScope(String analysisScope) { this.analysisScope = analysisScope; }
	
	@JsonProperty("quality_model") public String getQualityModel() { return qualityModel; }
	public void setQualityModel(String qualityModel) { this.qualityModel = qualityModel; }
	
	@JsonProperty("ordered_by") public String getOrderedBy() { return orderedBy; }
	public void setOrderedBy(String orderedBy) { this.orderedBy = orderedBy; }
	
	public List<Language> getLanguages() { return languages; }
	public void setLanguages(List<Language> languages) { this.languages = languages; }

	@JsonProperty("Main metrics") public List<MetricValue> getMainMetrics() { return mainMetrics; }
	public void setMainMetrics(List<MetricValue> mainMetrics) { this.mainMetrics = mainMetrics; }
	
	@JsonProperty("Risk index") public MetricValue getRiskIndex() { return riskIndex; }
	public void setRiskIndex(MetricValue riskIndex) { this.riskIndex = riskIndex; }

	@JsonProperty("Quality indicator") public MetricValue getQualityIndicator() { return qualityIndicator; }
	public void setQualityIndicator(MetricValue qualityIndicator) { this.qualityIndicator = qualityIndicator; }

	@JsonProperty("Effort to target") public MetricValue getEffortToTarget() { return effortToTarget; }
	public void setEffortToTarget(MetricValue effortToTarget) { this.effortToTarget = effortToTarget; }

	@JsonProperty("Security") public Map<String, Object> getSecurityMetrics() { return securityMetrics; }
	public void setSecurityMetrics(Map<String, Object> securityMetrics) { this.securityMetrics = securityMetrics; }
	
	public InsightsData getInsightsData() { return insightsData; }
	public void setInsightsData(InsightsData insightsData) { this.insightsData = insightsData; }
	
	public String getChangeRequest() { return changeRequest; }
	public void setChangeRequest(String changeRequest) { this.changeRequest = changeRequest; }

	public String getChangeRequestStatus() { return changeRequestStatus; }
	public void setChangeRequestStatus(String changeRequestStatus) { this.changeRequestStatus = changeRequestStatus; }

	public String getBranchName() { return branchName; }
	public void setBranchName(String branchName) { this.branchName = branchName; }

	public String getBaselineAnalysisCode() { return baselineAnalysisCode; }
	public void setBaselineAnalysisCode(String baselineAnalysisCode) { this.baselineAnalysisCode = baselineAnalysisCode; }

	public DeliveryFiles getDeliveryFiles() { return deliveryFiles; }
	public void setDeliveryFiles(DeliveryFiles deliveryFiles) { this.deliveryFiles = deliveryFiles; }

	public DeliveryDefects getDeliveryDefects() { return deliveryDefects; }
	public void setDeliveryDefects(DeliveryDefects deliveryDefects) { this.deliveryDefects = deliveryDefects; }

	public AuditResult getAuditResult() { return auditResult; }
	public void setAuditResult(AuditResult auditResult) { this.auditResult = auditResult; }

}
