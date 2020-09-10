package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

public enum DeliveryType implements KiuwanModelObject {
	
	COMPLETE_DELIVERY("Complete delivery"), 
	PARTIAL_DELIVERY("Partial delivery");
	
	private String displayName;
	
	private DeliveryType(String displayName) {
		this.displayName = displayName;
	}
	
	@Override public String getDisplayName() { return this.displayName; }
	@Override public String getValue() { return name(); }
	
}
