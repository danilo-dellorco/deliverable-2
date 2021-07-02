package tools;

import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;

import data.ProjectClass;
import git.GitCommit;
import git.GitRelease;
import git.GitRepo;
import jira.JiraRelease;

public class DebugTools {
	
	private DebugTools() {}
	
	public static void waitInput(){
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		scan.next(); //qui il programma attende l'immissione dei dati
	}
	
	public static void printJiraReleaseList(List<JiraRelease> list,boolean stop) {
		for (JiraRelease r:list) {
			r.print();
		}
		if (stop) {
			waitInput();
		}
	}
	public static void printCommitList(List<GitCommit> list,boolean stop) {
		for (GitCommit c:list) {
			c.printNoMsg();
		}
		if (stop) {
			waitInput();
		}
	}
	
	public static void printGitReleaseList(List<GitRelease> list,boolean stop) {
		for (GitRelease c:list) {
			c.print();
		}
		if (stop) {
			waitInput();
		}
	}
	
	public static void printAllProjectClasses(GitRepo repo) {
		for (GitRelease r:repo.getReleaseList()) {
			for (ProjectClass p:r.getClassList()) {
				p.print();
			}
		}
	}
	
	/*
	 * [DEBUG] Stampa a schermo un Timestamp per valutare le prestazioni
	 */
	public static String timestamp() {
		return new Timestamp(System.currentTimeMillis()).toString();
	}
	
	/*
	public void consistencyCheck(List<GitCommit> fixBugCommits) {
		for (GitRelease r : this.getReleaseList()) {
			int count = 0;
			int buggy = 0;
			for (GitCommit c : fixBugCommits) {
				List<GitCommit> revisions = r.getRevisions();
				if (revisions.contains(c)) {
					count++;
				}
			}
			
			for (ProjectClass p : r.getClassList()) {
				if (p.isBuggy())
					buggy++;
			}
			System.out.println("=".repeat(30));
			System.out.println(
					String.format("Release: %s\nCommitDiTipoBug: %d\nClassi buggy: %d", r.getName(), count, buggy));
		}
	}
	*/
}
