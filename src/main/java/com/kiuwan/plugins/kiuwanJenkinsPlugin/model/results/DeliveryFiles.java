package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryFiles {

	private Integer count;

	public Integer getCount() { return count; }
	public void setCount(Integer count) { this.count = count; }
	
}
