package com.kiuwan.plugins.kiuwanJenkinsPlugin.upgrade;

import static com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils.logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.util.KiuwanUtils;

import hudson.XmlFile;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.XStream2;
import jenkins.model.Jenkins;

public class UpgradeToConnectionProfiles {

	/**
	 * This method handles the configuration data upgrade from old versions.
	 * Older versions only allowed for a single connection configuration to Kiuwan.
	 * This method will read old configurations and create a new one that contains the existing
	 * configuration profile.
	 */
	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static boolean upgradeConfiguration() {
		
		// Check first if upgrade has been already executed
		KiuwanGlobalConfigDescriptor instance = KiuwanGlobalConfigDescriptor.get();
		if (instance != null && instance.isConfigUpgradedToConnectionProfiles()) {
			return true;
		}

		File configFile = KiuwanGlobalConfigDescriptor.getGlobalConfigFile();
		File oldConfigFile = KiuwanGlobalConfigDescriptor.getOldGlobalConfigFile();
		
		// Create the new config file from the old one
		boolean upgradeSuccessful = false;
		if (oldConfigFile.exists() && !configFile.exists()) {
			logger().log(Level.INFO, "Kiuwan CONFIG upgrade to connection profiles process started");
			
			boolean oldDataRead = false;
			KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile();
			try {
				// We don't want to use Items.XSTREAM2 here because we want 
				// the aliases only to be applied during the upgrade process
				XStream2 xstream2 = new XStream2();
				
				// 1.0.0 <= Kiuwan plugin version <= 1.4.6
				xstream2.addCompatibilityAlias("com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl", 
					com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.class);
				
				// 1.4.6 <= Kiuwan plugin version <= 1.5.2
				xstream2.addCompatibilityAlias("com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor", 
					com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanConnectionProfile.class);
				
				XmlFile oldXmlFile = new XmlFile(xstream2, oldConfigFile);
				oldXmlFile.unmarshal(connectionProfile);
				oldDataRead = true;
			} catch (IOException e) {
				logger().log(Level.SEVERE, "Could not read old data from " + oldConfigFile, e);
			}
			
			if (oldDataRead) {
				connectionProfile.setName("Kiuwan Connection Profile 1");
				
				List<KiuwanConnectionProfile> list = new ArrayList<>();
				list.add(connectionProfile);
				
				instance.setConnectionProfiles(list);
				instance.setUpgradeConfigToConnectionProfilesTimestamp(KiuwanUtils.getCurrentTimestampString());
				instance.save();
				
				upgradeSuccessful = true;
				oldConfigFile.delete();
			}

			logger().log(Level.INFO, "Kiuwan CONFIG upgrade to connection profiles " + 
				"process finished " + (upgradeSuccessful ? "successfully" : "with errors"));

		} else if (!oldConfigFile.exists() && !configFile.exists()) {
			instance.setUpgradeConfigToConnectionProfilesTimestamp(KiuwanUtils.getCurrentTimestampString());
			instance.save();
			upgradeSuccessful = true;
		}
		
		return upgradeSuccessful;
	}
	
	@SuppressWarnings("rawtypes")
	@Initializer(after = InitMilestone.JOB_LOADED)
	public static boolean upgradeJobs() {
		KiuwanGlobalConfigDescriptor instance = KiuwanGlobalConfigDescriptor.get();
		
		String defaultConnectionProfileUuid = null;
		if (instance != null) {
			
			// Check first if upgrade has been already executed
			if (instance.isJobsUpgradedToConnectionProfiles()) {
				return true;
			}
			
			List<KiuwanConnectionProfile> connectionProfiles = instance.getConnectionProfiles();
			if (connectionProfiles != null && connectionProfiles.size() == 1) {
				defaultConnectionProfileUuid = connectionProfiles.iterator().next().getUuid();
			}
		}
		
		// Retrieve jobs with a Kiuwan publisher
		Map<AbstractProject, KiuwanRecorder> jobsWithKiuwanPublisherMap = new LinkedHashMap<>();
		for (AbstractProject<?, ?> job : Jenkins.get().getAllItems(AbstractProject.class)) {
			DescribableList<Publisher, Descriptor<Publisher>> publishers = job.getPublishersList();
			for (Publisher publisher : publishers) {
				if (publisher instanceof KiuwanRecorder) {
					KiuwanRecorder kiuwanRecorder = (KiuwanRecorder) publisher;
					jobsWithKiuwanPublisherMap.put(job, kiuwanRecorder);
				}
			}
		}
		
		boolean upgradeSuccessful = false;
		logger().log(Level.INFO, "Kiuwan JOBS upgrade to connection profiles process started");
		
		// If there are no jobs with a Kiuwan recorder, set the upgrade process as done
		if (jobsWithKiuwanPublisherMap.isEmpty()) {
			upgradeSuccessful = true;
			
		// A default connection profile must exist in order to update the current publishers
		} else if (StringUtils.isNotEmpty(defaultConnectionProfileUuid)) {
			for (AbstractProject job : jobsWithKiuwanPublisherMap.keySet()) {
				KiuwanRecorder kiuwanRecorder = jobsWithKiuwanPublisherMap.get(job);
				String connectionProfileUuid = kiuwanRecorder.getConnectionProfileUuid();
				
				// This is how we discern an already upgraded job from a job that has not been checked before.
				// If the assigned connection profile is null, the upgrade has not been run on this job.
				// An empty string would mean that the user has manually set an empty profile afterwards.
				if (connectionProfileUuid == null) {
					kiuwanRecorder.setConnectionProfileUuid(defaultConnectionProfileUuid);
					try {
						job.save();
					} catch (IOException e) {
						logger().log(Level.SEVERE, "Could not save job " + 
							job.getFullDisplayName() + " while upgrading Kiuwan publisher to connection profiles!", e);
					}
				}
			}
			
			upgradeSuccessful = true;
		
		} else {
			logger().log(Level.WARNING, "Kiuwan JOBS upgrade cannot be run: no default connection profile found");
		}
		
		if (upgradeSuccessful) {
			instance.setUpgradeJobsToConnectionProfilesTimestamp(KiuwanUtils.getCurrentTimestampString());
			instance.save();
		}
		
		logger().log(Level.INFO, "Kiuwan JOBS upgrade to connection profiles " + 
			"process finished " + (upgradeSuccessful ? "successfully" : "with errors"));
		
		return upgradeSuccessful;
	}
	
}
