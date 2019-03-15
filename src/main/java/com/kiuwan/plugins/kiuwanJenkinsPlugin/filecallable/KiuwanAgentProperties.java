package com.kiuwan.plugins.kiuwanJenkinsPlugin.filecallable;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

@Deprecated
public class KiuwanAgentProperties implements FileCallable<Void> {

	private static final long serialVersionUID = 2265079817570278194L;

//	private String username;
//
//	public KiuwanAgentProperties(String username) {
//		this.username = username;
//	}

	public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
//		String propertiesFilePath = f.getAbsolutePath();
//
//		StringBuilder stringBuilder = new StringBuilder();
//		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(propertiesFilePath))) {
//			String line = null;
//			String usernameValue = "";
//			while ((line = bufferedReader.readLine()) != null) {
//				String cleanLine = line.trim();
//				if (cleanLine.startsWith("username=")) {
//					usernameValue = cleanLine.replaceFirst("username=", "");
//					line = "username=" + username;
//				} else if (cleanLine.startsWith("password=")) {
//					line = "password=" + usernameValue;
//				}
//
//				stringBuilder.append(line + "\n");
//			}
//		}
//
//		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(propertiesFilePath))) {
//			bufferedWriter.write(stringBuilder.toString());
//		}
//
		return null;
	}

	@Override
	public void checkRoles(RoleChecker checker) throws SecurityException {
		// FIXME esta todo comentado para ver si falla algo, por confirmar que es lo que parece: codigo muerto
	}

}