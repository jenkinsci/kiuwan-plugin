package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class LocalAnalyzerProperties implements FileCallable<Void> {

	private static final long serialVersionUID = 2265079817570278194L;

	private String username;

	public LocalAnalyzerProperties(String username) {
		this.username = username;
	}

	public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
		String propertiesFilePath = f.getAbsolutePath();

		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(propertiesFilePath))) {
			String line = null;
			String usernameValue = "";
			while ((line = bufferedReader.readLine()) != null) {
				String cleanLine = line.trim();
				if (cleanLine.startsWith("username=")) {
					usernameValue = cleanLine.replaceFirst("username=", "");
					line = "username=" + username;
				} else if (cleanLine.startsWith("password=")) {
					line = "password=" + usernameValue;
				}

				stringBuilder.append(line + "\n");
			}
		}

		try (FileWriter fw = new FileWriter(propertiesFilePath);
			 BufferedWriter bufferedWriter = new BufferedWriter(fw)) {
			
			bufferedWriter.write(stringBuilder.toString());
		}

		return null;
	}

}