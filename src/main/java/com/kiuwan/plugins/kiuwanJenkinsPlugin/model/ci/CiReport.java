package com.kiuwan.plugins.kiuwanJenkinsPlugin.model.ci;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean to parse CI report
 *
 * @author <a href="mailto:felix.carnicero@kiuwan.com">fcarnicero</a>
 *
 */
public class CiReport implements Serializable {

	private static final long serialVersionUID = 7623537960508584770L;

	// Build system data (i.e. Jenkins' build name, number, etc) 
	private CiReportBuildInfo buildInfo;

	// 'Main' commit data (AKA last commit, AKA most recent commit)
	private String branch;
	private String commitId;
	private List<String> parentCommitIds;
	private String commitMessage;
	private String author;
	private String commitDate;
	private List<CiReportFile> files = new ArrayList<>();

	// All other commits' data (newer first) 
	private List<CiReportCommit> commits = new ArrayList<>();

	public CiReportBuildInfo getBuildInfo() { return buildInfo; }
	public void setBuildInfo(CiReportBuildInfo buildInfo) { this.buildInfo = buildInfo; }

	public String getBranch() { return branch; }
	public void setBranch(String branch) { this.branch = branch; }

	public String getCommitId() { return commitId; }
	public void setCommitId(String commitId) { this.commitId = commitId; }

	public List<String> getParentCommitIds() { return parentCommitIds; }
	public void setParentCommitIds(List<String> parentCommitIds) { this.parentCommitIds = parentCommitIds; }

	public String getCommitMessage() { return commitMessage; }
	public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

	public String getAuthor() { return author; }
	public void setAuthor(String author) { this.author = author; }

	public String getCommitDate() { return commitDate; }
	public void setCommitDate(String commitDate) { this.commitDate = commitDate; }

	public List<CiReportFile> getFiles() { return files; }
	public void setFiles(List<CiReportFile> files) { this.files = files; }

	public List<CiReportCommit> getCommits() { return commits; }
	public void setCommits(List<CiReportCommit> commits) { this.commits = commits; }
}
