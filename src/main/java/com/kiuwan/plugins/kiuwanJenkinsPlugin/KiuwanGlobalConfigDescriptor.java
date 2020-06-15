package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.XmlFile;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.XStream2;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

@Extension
public class KiuwanGlobalConfigDescriptor extends GlobalConfiguration implements Serializable {

	private static final long serialVersionUID = 3509491644782698195L;
	
	@CopyOnWrite
	private List<KiuwanConnectionProfile> connectionProfiles;
	private String configSaveTimestamp;
	
	public KiuwanGlobalConfigDescriptor() {
		super();
		load();
	}
	
	public List<KiuwanConnectionProfile> getConnectionProfiles() { return connectionProfiles; }
	public String getConfigSaveTimestamp() { return configSaveTimestamp; }

	public static KiuwanGlobalConfigDescriptor get() {
		return GlobalConfiguration.all().get(KiuwanGlobalConfigDescriptor.class);
	}
	
	public KiuwanConnectionProfile getConnectionProfile(String connectionProfileUuid) {
		KiuwanConnectionProfile connectionProfile = null;
		for (KiuwanConnectionProfile cp : connectionProfiles) {
			if (connectionProfileUuid != null && connectionProfileUuid.equals(cp.getUuid())) {
				connectionProfile = cp;
				break;
			}
		}
		
		return connectionProfile;
	}
	
	
	@Override
	public String getDisplayName() {
		return "Kiuwan Global Configuration";
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		List<KiuwanConnectionProfile> list = req.bindJSONToList(KiuwanConnectionProfile.class, json.get("connectionProfiles"));
		
		// Generate UUIDs for new connection profiles
		for (KiuwanConnectionProfile connectionProfile : list) {
			if (connectionProfile.getUuid() == null || connectionProfile.getUuid().isEmpty()) {
				connectionProfile.generateUuid();
			}
		}
		
		this.connectionProfiles = new ArrayList<>();
		this.connectionProfiles.addAll(list);
		this.configSaveTimestamp = Long.toHexString(System.currentTimeMillis());;
	
		save();
	    return true;
	}
	
	public boolean existDuplicateNames() {
		Set<String> profileNames = new HashSet<>();
		for (KiuwanConnectionProfile connectionProfile : connectionProfiles) {
			if (profileNames.contains(connectionProfile.getName())) {
				return true;
			}
			
			if (connectionProfile.getName() != null && !connectionProfile.getName().isEmpty()) {
				profileNames.add(connectionProfile.getName());
			}
		}
		
		return false;
	}
	
	public List<String> getDuplicateNames() {
		Set<String> profileNames = new HashSet<>();
		List<String> duplicates = new ArrayList<>();
		for (KiuwanConnectionProfile connectionProfile : connectionProfiles) {
			if (!profileNames.add(connectionProfile.getName())) {
				if (connectionProfile.getName() != null && !connectionProfile.getName().isEmpty()) {
					duplicates.add(connectionProfile.getName());
				}
			}
		}
		
		return duplicates;
	}
		
	public boolean existMisconfiguredJobs() {
		List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
		for (AbstractProject<?, ?> job : jobs) {
			DescribableList<Publisher, Descriptor<Publisher>> publishers = job.getPublishersList();
			for (Publisher publisher : publishers) {
				if (publisher instanceof KiuwanRecorder) {
					KiuwanRecorder kiuwanRecorder = (KiuwanRecorder) publisher;
					String connectionProfileUuid = kiuwanRecorder.getConnectionProfileUuid();
					if (connectionProfileUuid == null || connectionProfileUuid.isEmpty()) {
						return true;
					} else {
						KiuwanConnectionProfile connectionProfile = getConnectionProfile(connectionProfileUuid);
						if (connectionProfile == null) {
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	public List<String> getMisconfiguredJobs() {
		List<String> misconfiguredJobs = new ArrayList<>();
		List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
		for (AbstractProject<?, ?> job : jobs) {
			DescribableList<Publisher, Descriptor<Publisher>> publishers = job.getPublishersList();
			for (Publisher publisher : publishers) {
				if (publisher instanceof KiuwanRecorder) {
					KiuwanRecorder kiuwanRecorder = (KiuwanRecorder) publisher;
					String connectionProfileUuid = kiuwanRecorder.getConnectionProfileUuid();
					if (connectionProfileUuid == null || connectionProfileUuid.isEmpty()) {
						misconfiguredJobs.add(job.getFullDisplayName() + " (profile not set)");
					} else {
						KiuwanConnectionProfile connectionProfile = getConnectionProfile(connectionProfileUuid);
						if (connectionProfile == null) {
							misconfiguredJobs.add(job.getFullDisplayName() + " (configured profile not found)");
						}
					}
				}
			}
		}
		
		return misconfiguredJobs;
	}
	
	/* 
	 * ---------------------------
	 * OLD CONFIGURATION MIGRATION
	 * ---------------------------
	 */
	
	/**
	 * This {@link XStream2} instance will be used to support loading of the old plugin descriptor
	 * @see <a href="https://wiki.jenkins.io/display/JENKINS/Hint+on+retaining+backward+compatibility">
	 * Retaining backward compatibility</a>
	 * @see {@link #getConfigFile} method for loading the configuration file
	 */
	// private static final XStream2 XSTREAM2 = new XStream2();

	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void addAliases() {
	    
		// 1 - com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl
		// 2 - com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor
		// Now - "com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor"
		
		/*
		Items.XSTREAM2.addCompatibilityAlias(
	    	"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl", 
	    	com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor.class);
	    */
		
		Items.XSTREAM2.addCompatibilityAlias(
	    	"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl", 
	    	com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
		
		Items.XSTREAM2.addCompatibilityAlias(
	    	"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor", 
	    	com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
	}
	
	/*
	static {
		// XSTREAM2.addCompatibilityAlias(
		//	"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl",
		//	com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor.class);
		
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
		
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
	}
	*/

	@Override
	protected XmlFile getConfigFile() {
		
		/*
		XmlFile xmlFile = new XmlFile(new File(
			Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.xml"));
		
		return new XmlFile(XSTREAM2, xmlFile.getFile());
		*/
		
		XmlFile xmlFile = new XmlFile(new File(
			Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfig.xml"));
		
		return new XmlFile(Items.XSTREAM2, xmlFile.getFile());
	}
	
}
