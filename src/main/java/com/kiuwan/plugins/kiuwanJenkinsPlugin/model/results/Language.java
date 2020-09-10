package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Language {

	private String name;
	private Double size;
	
	@Override
	public String toString() {
		return "Language [name=" + name + ", size=" + size + "]";
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Double getSize() { return size; }
	public void setSize(Double size) { this.size = size; }

}