package com.kiuwan.plugins.kiuwanJenkinsPlugin.model;

import hudson.model.Result;

public enum SelectableResult implements KiuwanModelObject {

	FAILURE(Result.FAILURE),
	UNSTABLE(Result.UNSTABLE),
	ABORTED(Result.ABORTED), 
	NOT_BUILT(Result.NOT_BUILT); 
	
	private Result result;
	private SelectableResult(Result result) {
		this.result = result;
	}

	@Override public String getDisplayName() { return getValue(); }
	@Override public String getValue() { return result.toString(); }

}
