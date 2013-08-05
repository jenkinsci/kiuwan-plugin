package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.sf.json.JSONObject;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class KiuwanNotifier extends Notifier {
	
	private String applicationName;
	private String label;
	private String encoding;

	@DataBoundConstructor
	public KiuwanNotifier(String applicationName, String label, String encoding) {
		this.applicationName = applicationName;
		this.label = label;
		this.encoding = encoding;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		try {
			performScan(build, listener);
		} catch (KiuwanException e) {
			listener.getLogger().println(e.getMessage());
			listener.fatalError(e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			listener.getLogger().println(sw.toString());
		}
		return true;
	}

	
	public String getApplicationName() {
		return applicationName;
	}
	
	

	public String getLabel() {
		return label;
	}

	public String getEncoding() {
		return encoding;
	}

	

	private void performScan(AbstractBuild<?, ?> build, BuildListener listener) throws KiuwanException, IOException, InterruptedException {
		
		if (applicationName == null || applicationName.isEmpty()) {
			applicationName = build.getProject().getName();
		}
		
		if (label == null || label.isEmpty()) {
			label = "#" + build.getNumber();
		}
		
		KiuwanLinkAction link = null;
		for (Action action: build.getActions()) {
			if (action instanceof KiuwanLinkAction) {
				link = (KiuwanLinkAction) action;
			}
		}
		
		if (link == null) {
			link = new KiuwanLinkAction(applicationName, label);
			build.addAction(link);
		}
		else {
			link.setUrl(applicationName, label);
		}
		
		File srcFolder = build.getModuleRoot().act(new FileGetter());
		
		String localAnalyzerFolder = getDescriptor().getLocalAnalyzerFolder();
		String agentXml = localAnalyzerFolder + File.separator + "bin" + File.separator + "agent.xml";
		
		listener.getLogger().println("Analyze folder: " + srcFolder.getAbsolutePath());
		listener.getLogger().println("Local analyzer: " + agentXml);
		listener.getLogger().println("kiuwan app name: " + applicationName);
		listener.getLogger().println("analysis label: " + label);
		listener.getLogger().println("encoding: " + encoding);
		
		Project project = new Project();
		ProjectHelper.configureProject(project, new File(agentXml));

		project.setUserProperty(MagicNames.ANT_FILE, agentXml);
		project.setUserProperty("encoding", encoding);
		project.setUserProperty("sourcePaths", srcFolder.getAbsolutePath());
		project.setUserProperty("softwareName", applicationName);
		project.setUserProperty("label", label);
		project.setUserProperty("createIfNeeded", "true");

		project.executeTarget("run");
		

	}
	


	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private String localAnalyzerFolder;

		public DescriptorImpl() {
			super(KiuwanNotifier.class);
			load();
		}

		@Override
		public String getDisplayName() {
			return "Analyze your source code with Kiuwan!";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> item) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject o) throws FormException {

			// to persist global configuration information,
			// set that to properties and call save().
			localAnalyzerFolder = o.getString("localAnalyzerFolder");
			save();
			return super.configure(req, o);
		}

		public String getLocalAnalyzerFolder() {
			return localAnalyzerFolder;
		}

		public void setLocalAnalyzerFolder(String localAnalyzerFolder) {
			this.localAnalyzerFolder = localAnalyzerFolder;
		}
	}

	private static class FileGetter implements FilePath.FileCallable<File> {
		public File invoke(File f, VirtualChannel channel) {
			return f;
		}
	}

}
