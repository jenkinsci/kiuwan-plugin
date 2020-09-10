package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryDefects {

	private Integer newDefects;
	private Integer removedDefects;
	private Integer defects;

	public Integer getNewDefects() { return newDefects; }
	public void setNewDefects(Integer newDefects) { this.newDefects = newDefects; }
	
	public Integer getRemovedDefects() { return removedDefects; }
	public void setRemovedDefects(Integer removedDefects) { this.removedDefects = removedDefects; }
	
	public Integer getDefects() { return defects; }
	public void setDefects(Integer defects) { this.defects = defects; }
	
}
