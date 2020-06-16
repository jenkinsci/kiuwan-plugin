package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.kohsuke.stapler.StaplerRequest;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

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
import hudson.util.FormValidation;
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
		fillData(list);
		save();
	    return true;
	}
	
	@Override
	protected XmlFile getConfigFile() {
		File file = getGlobalConfigFile();
		return new XmlFile(Items.XSTREAM2, file);
	}
	
	public boolean existOldConfigFile() {
		File oldConfigFile = getOldGlobalConfigFile();
		return oldConfigFile.exists();
	}

	public FormValidation doMigrateConfiguration() {
		boolean ok = migrateConfiguration();
		
		FormValidation ret = null;
		if (ok) {
			ret = FormValidation.okWithMarkup("Migration done! Please reload the <a href=\"" + 
				Jenkins.getInstance().getRootUrl() + "configure\">settings page</a>.");
		} else {
			ret = FormValidation.warning("Migration failed!");
		}
		
		return ret;
	}

	@SuppressWarnings("rawtypes")
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

	@SuppressWarnings("rawtypes")
	public List<String> getMisconfiguredJobs() {
		List<String> misconfiguredJobs = new ArrayList<>();
		List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
		for (AbstractProject<?, ?> job : jobs) {
			DescribableList<Publisher, Descriptor<Publisher>> publishers = job.getPublishersList();
			for (Publisher publisher : publishers) {
				if (publisher instanceof KiuwanRecorder) {
					KiuwanRecorder kiuwanRecorder = (KiuwanRecorder) publisher;
					String connectionProfileUuid = kiuwanRecorder.getConnectionProfileUuid();
					if (connectionProfileUuid == null) {
						misconfiguredJobs.add(job.getFullDisplayName() + " (old configuration)");
					} else if (connectionProfileUuid.isEmpty()) {
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

	public boolean existDuplicateNames() {
		if (connectionProfiles == null) return false;
		
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
	
	/**
	 * This method handles the configuration migration from old versions.
	 * Older versions only allowed for a single connection configuration to Kiuwan.
	 * This method will read old configurations and create a new one that contains the existing
	 * configuration profile.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static boolean migrateConfiguration() {
	    
		// We don't want to use Items.XSTREAM2 here because we want 
		// the aliases only to be applied during the migration process
		XStream2 xstream2 = new XStream2();
		
		// Kiuwan plugin version <= 1.4.6
		xstream2.addCompatibilityAlias("com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl", 
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.class);
		
		// Kiuwan plugin version <= 1.5.2
		xstream2.addCompatibilityAlias("com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor", 
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.class);
		
		File configFile = getGlobalConfigFile();
		File oldConfigFile = getOldGlobalConfigFile();
		
		// Create the new config file from the old one
		boolean migrationSuccessful = false;
		if (oldConfigFile.exists() && !configFile.exists()) {
	        
			boolean oldDataRead = false;
	        KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile();
	        try {
	        	XmlFile oldXmlFile = new XmlFile(xstream2, oldConfigFile);
				oldXmlFile.unmarshal(connectionProfile);
				oldDataRead = true;
			} catch (IOException e) {
				KiuwanUtils.logger().log(Level.SEVERE, "Could not read old data from " + oldConfigFile, e);
			}
			
	        if (oldDataRead) {
				connectionProfile.setName("Kiuwan Connection Profile 1");
				
				List<KiuwanConnectionProfile> list = new ArrayList<>();
				list.add(connectionProfile);
				
				KiuwanGlobalConfigDescriptor instance = KiuwanGlobalConfigDescriptor.get();
				instance.fillData(list);
				instance.save();
				
				oldConfigFile.delete();
				
				migrationSuccessful = true;
	        }
		}
		
		return migrationSuccessful;
	}
	
	@Initializer(after = InitMilestone.JOB_LOADED)
	public static void migratePublishers() {
		String defaultConnectionProfileUuid = null;
		KiuwanGlobalConfigDescriptor instance = KiuwanGlobalConfigDescriptor.get();
		List<KiuwanConnectionProfile> connectionProfiles = instance.getConnectionProfiles();
		if (connectionProfiles != null && connectionProfiles.size() == 1) {
			defaultConnectionProfileUuid = connectionProfiles.iterator().next().getUuid();
		}
		
		// A default connection profile must exist in order to update the current publishers
		if (defaultConnectionProfileUuid != null && !defaultConnectionProfileUuid.isEmpty()) {
			List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
			for (AbstractProject<?, ?> job : jobs) {
				DescribableList<Publisher, Descriptor<Publisher>> publishers = job.getPublishersList();
				for (Publisher publisher : publishers) {
					if (publisher instanceof KiuwanRecorder) {
						KiuwanRecorder kiuwanRecorder = (KiuwanRecorder) publisher;
						String connectionProfileUuid = kiuwanRecorder.getConnectionProfileUuid();
						
						// This is how we discern an already migrated job from a job that has not been checked before.
						// If the assigned connection profile is null, the migration has not been run on this job.
						// An empty string would mean that the user has manually set an empty profile afterwards.
						if (connectionProfileUuid == null) {
							kiuwanRecorder.setConnectionProfileUuid(defaultConnectionProfileUuid);
							try {
								job.save();
							} catch (IOException e) {
								KiuwanUtils.logger().log(Level.SEVERE, "Could not save job " + 
									job.getFullDisplayName() + " while migrating Kiuwan publisher!", e);
							}
						}
					}
				}
			}
		}

	}
	
	private void fillData(List<KiuwanConnectionProfile> list) {
		for (KiuwanConnectionProfile connectionProfile : list) {
			
			// Generate UUIDs for new connection profiles
			if (connectionProfile.getUuid() == null || connectionProfile.getUuid().isEmpty()) {
				connectionProfile.generateUuid();
			}
		}
		
		this.connectionProfiles = new ArrayList<>();
		this.connectionProfiles.addAll(list);
		this.configSaveTimestamp = Long.toHexString(System.currentTimeMillis());
	}

	private static File getGlobalConfigFile() {
		return new File(Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfig.xml");
	}

	private static File getOldGlobalConfigFile() {
		return new File(Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.xml");
	}

}
