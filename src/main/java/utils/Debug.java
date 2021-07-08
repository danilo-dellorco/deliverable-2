package utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import data.ProjectClass;
import git.GitCommit;
import git.GitRelease;
import git.GitRepo;
import jira.JiraRelease;
import jira.JiraTicket;
import utils.VersionNumberComparator.AlphaDecimalComparator;

public class Debug {
	
	private Debug() {}
	
	public static void waitInput(){
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		scan.next(); //qui il programma attende l'immissione dei dati
	}
	
	public static void printJiraReleaseList(List<JiraRelease> list,boolean stop) {
		System.out.println("\n\n===============JIRA RELEASE LIST===============");
		for (JiraRelease r:list) {
			r.print();
		}
		if (stop) {
			waitInput();
		}
	}
	public static void printCommitList(List<GitCommit> list,boolean stop) {
		System.out.println("\n\n===============COMMIT LIST===============");
		for (GitCommit c:list) {
			c.printNoMsg();
		}
		if (stop) {
			waitInput();
		}
	}
	
	public static void printGitReleaseList(List<GitRelease> list,boolean stop) {
		System.out.println("\n\n===============GIT RELEASE LIST===============");
		if (list.isEmpty()) {
			System.out.println("NO GIT RELEASE FOUND");
		}
		for (GitRelease c:list) {
			c.print();
		}
		if (stop) {
			waitInput();
		}
	}
	
	public static void simplePrintGitReleaseList(List<GitRelease> list) {
		System.out.println("\n\n===============GIT RELEASE LIST===============");
		for (GitRelease c:list) {
			System.out.println(c.getName());
		}
	}
	
	public static void realOrderPrintReleaseList(GitRepo repository, List<GitRelease> list) {
		List<GitRelease> ordered = orderReleaseList(repository, list);
		for (GitRelease r:ordered) {
			System.out.println(r.getName());
		}
	}
	
	public static List<GitRelease> orderReleaseList(GitRepo repository, List<GitRelease> list) {
		List<String> nameList = new ArrayList<>();
		List<GitRelease> relsList = new ArrayList<>();
		for (GitRelease c:list) {
			nameList.add(c.getName());
		}
		nameList.sort(VersionNumberComparator.getInstance());
		for (String n:nameList) {
			GitRelease r = repository.getReleaseByName(n);
			relsList.add(r);
		}
		return relsList;
	}
	
	public static List<GitRelease> getRightReleases(List<GitRelease> list) {
		List<GitRelease> returnList = new ArrayList<>();
		for (int i = 0;i<list.size()-1;i++) {
			GitRelease r0 = list.get(i);
			GitRelease r1 = list.get(i+1);
			int compairison = VersionNumberComparator.getInstance().compare(r0.getName(),r1.getName());
			if (compairison<0) {
				returnList.add(r0);
			}
		}
		int x=1;
		for (GitRelease r:returnList) {
			r.setId(x);
			x++;
		}
		return returnList;
	}
	
	
    int extractInt(String s) {
        String num = s.replaceAll("\\D", "");
        // return 0 if no digits found
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
	
	public static void printAllProjectClasses(GitRepo repo) {
		System.out.println("\n\n===============PROJECT CLASS LIST===============");
		for (GitRelease r:repo.getReleaseList()) {
			for (ProjectClass p:r.getClassList()) {
				p.print();
			}
		}
	}
	
	public static void countBuggyClass(List<ProjectClass> list) {
		System.out.println("\n\n===============BUGGY CLASS===============");
		int bugCount = 0;
		int classCount = 0;
		for (ProjectClass p:list) {
			if(p.isBuggy()) {
				bugCount++;
			}
			classCount++;
		}
		System.out.println("Buggy Class: " + bugCount);
		System.out.println("Total Class: " + classCount);
	}
	
	public static void printAllJiraTickets(List<JiraTicket> tickets) {
		System.out.println("\n\n===============JIRA TICKET LIST===============");
		for (JiraTicket t:tickets) {
			t.print();
		}
	}
	
	/*
	 * [DEBUG] Stampa a schermo un Timestamp per valutare le prestazioni
	 */
	public static String timestamp() {
		return new Timestamp(System.currentTimeMillis()).toString();
	}
}
