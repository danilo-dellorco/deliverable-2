package utils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;

import data.ProjectClass;
import git.GitCommit;
import git.GitRelease;
import git.GitRepo;
import jira.JiraRelease;

public class Debug {
	
	private Debug() {}
	
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
}
