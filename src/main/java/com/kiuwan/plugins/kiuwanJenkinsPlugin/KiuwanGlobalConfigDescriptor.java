package com.kiuwan.plugins.kiuwanJenkinsPlugin;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.upgrade.UpgradeToConnectionProfiles;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

@Extension
public class KiuwanGlobalConfigDescriptor extends GlobalConfiguration implements Serializable {

	private static final long serialVersionUID = 3509491644782698195L;
	
	@CopyOnWrite
	private List<KiuwanConnectionProfile> connectionProfiles;
	
	private String configSaveTimestamp;
	
	private String upgradeConfigToConnectionProfilesTimestamp;
	private String upgradeJobsToConnectionProfilesTimestamp;
	
	public KiuwanGlobalConfigDescriptor() {
		super();
		load();
	}
	
	public List<KiuwanConnectionProfile> getConnectionProfiles() { return connectionProfiles; }
	public String getConfigSaveTimestamp() { return configSaveTimestamp; }

	public void setUpgradeConfigToConnectionProfilesTimestamp(String upgradeConfigToConnectionProfilesTimestamp) { this.upgradeConfigToConnectionProfilesTimestamp = upgradeConfigToConnectionProfilesTimestamp; }
	public void setUpgradeJobsToConnectionProfilesTimestamp(String upgradeJobsToConnectionProfilesTimestamp) { this.upgradeJobsToConnectionProfilesTimestamp = upgradeJobsToConnectionProfilesTimestamp; }

	public boolean isConfigUpgradedToConnectionProfiles() {
		return upgradeConfigToConnectionProfilesTimestamp != null && !upgradeConfigToConnectionProfilesTimestamp.isEmpty();
	}
	
	public boolean isJobsUpgradedToConnectionProfiles() {
		return upgradeJobsToConnectionProfilesTimestamp != null && !upgradeJobsToConnectionProfilesTimestamp.isEmpty();
	}
	
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
		setConnectionProfiles(list);
		save();
		return true;
	}
	
	@Override
	public synchronized void save() {
		this.configSaveTimestamp = KiuwanUtils.getCurrentTimestampString();
		super.save();
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

	public FormValidation doUpgradeToConnectionProfiles() {
		KiuwanGlobalConfigDescriptor instance = KiuwanGlobalConfigDescriptor.get();

		boolean upgradeConfigOk = true;
		if (instance == null || !instance.isConfigUpgradedToConnectionProfiles()) {
			upgradeConfigOk = UpgradeToConnectionProfiles.upgradeConfiguration();
		}
		
		boolean upgradeJobsOk = true;
		if (instance == null || !instance.isJobsUpgradedToConnectionProfiles()) {
			upgradeJobsOk = UpgradeToConnectionProfiles.upgradeJobs();
		}
		
		FormValidation ret = null;
		if (upgradeConfigOk && upgradeJobsOk) {
			ret = FormValidation.okWithMarkup("Upgrade done! Please reload the <a href=\"" + 
				Jenkins.getInstance().getRootUrl() + "configure\">settings page</a>.");
		} else {
			ret = FormValidation.warning("Upgrade failed! Please check Jenkins logs to diagnose the problem.");
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
	
	public void setConnectionProfiles(List<KiuwanConnectionProfile> list) {
		for (KiuwanConnectionProfile connectionProfile : list) {
			
			// Generate UUIDs for new connection profiles
			if (connectionProfile.getUuid() == null || connectionProfile.getUuid().isEmpty()) {
				connectionProfile.generateUuid();
			}
		}
		
		this.connectionProfiles = new ArrayList<>();
		this.connectionProfiles.addAll(list);
	}

	public static File getGlobalConfigFile() {
		return new File(Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfig.xml");
	}

	public static File getOldGlobalConfigFile() {
		return new File(Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.xml");
	}

}
