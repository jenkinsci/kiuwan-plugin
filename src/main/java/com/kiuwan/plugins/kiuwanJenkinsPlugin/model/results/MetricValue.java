package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricValue {

	private String name;
	private Double value;
	private List<MetricValue> children;
	
	@Override
	public String toString() {
		return "MetricValue [name=" + name + ", value=" + value + "]";
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public Double getValue() { return value; }
	public void setValue(Double value) { this.value = value; }
	
	public List<MetricValue> getChildren() { return children; }
	public void setChildren(List<MetricValue> children) { this.children = children; }
	
}
