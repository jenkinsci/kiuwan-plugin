package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.util.List;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

public class KiuwanLocalAnalyzerBuildListener implements BuildListener {

	private List<Throwable> exceptions;
	
	public KiuwanLocalAnalyzerBuildListener(List<Throwable> exceptions) {
		this.exceptions = exceptions;
	}

	public void taskStarted(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}

	public void taskFinished(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	public void targetStarted(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	public void targetFinished(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	public void messageLogged(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	public void buildStarted(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	public void buildFinished(BuildEvent event) {
		addExceptionIfExists(exceptions, event);
	}
	
	private void addExceptionIfExists(final List<Throwable> exceptions, BuildEvent event) {
		if(event.getException() != null){
			exceptions.add(event.getException());
		}
	}

}