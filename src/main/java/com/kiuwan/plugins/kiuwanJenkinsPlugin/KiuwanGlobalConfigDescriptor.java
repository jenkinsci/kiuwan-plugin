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
		
		/*
		// Empty names
		for (KiuwanConnectionProfile connectionProfile : list) {
			if (connectionProfile.getName() == null || connectionProfile.getName().isEmpty()) {
				throw new FormException("Name is required", "name");
			}
		}
		
		// Duplicated profile names
		Set<String> profileNames = new HashSet<>();
		for (KiuwanConnectionProfile connectionProfile : list) {
			if (profileNames.contains(connectionProfile.getName())) {
				throw new FormException("Duplicate profile name!", "name");
			}
			profileNames.add(connectionProfile.getName());
		}
		*/
		
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
		
		
		/*
		this.connectionProfiles.clear();
		
		// Multiple profiles
		if (json.isArray()) {
			JSONArray connectionProfilesJSON = json.getJSONArray("connectionProfiles");
			for (int i = 0; i < connectionProfilesJSON.size(); i++) {
				JSONObject connectionProfileJSON = connectionProfilesJSON.getJSONObject(i);
				KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile(connectionProfileJSON);
				this.connectionProfiles.add(connectionProfile);
			}
		
		// Single profile
		} else {
			JSONObject connectionProfileJSON = json.getJSONObject("connectionProfiles");
			if (connectionProfileJSON != null && !connectionProfileJSON.isNullObject()) {
				KiuwanConnectionProfile connectionProfile = new KiuwanConnectionProfile(connectionProfileJSON);
				this.connectionProfiles.add(connectionProfile);
			}
		}
		*/
		
	    /*
		this.configSaveTimestamp = Long.toHexString(System.currentTimeMillis());
		super.configure(req, json);
		
		save();
		return true;
		*/
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
	 * @see #getConfigFile() method for loading the configuration file
	 */
	private static final XStream2 XSTREAM2 = new XStream2();

	static {
		/*
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor.class);
		*/
		
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder$DescriptorImpl",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
		
		XSTREAM2.addCompatibilityAlias(
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanDescriptor",
			com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanGlobalConfigDescriptor.class);
	}

	@Override
	protected XmlFile getConfigFile() {
		XmlFile xmlFile = new XmlFile(new File(
			Jenkins.getInstance().getRootDir(), 
			"com.kiuwan.plugins.kiuwanJenkinsPlugin.KiuwanRecorder.xml"));
		
		return new XmlFile(XSTREAM2, xmlFile.getFile());
	}
	
}
