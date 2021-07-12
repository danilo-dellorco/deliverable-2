package utils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import data.ProjectClass;
import git.GitCommit;
import git.GitRelease;
import git.GitRepo;
import jira.JiraRelease;

public class Debug {
	static Logger logger = Logger.getLogger("log");
	
	private Debug() {}
	
	/**
	 * Permette di interrompere l'esecuzione, in modo da poter verificare un qualsiasi output
	 */
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
		if (list.isEmpty()) {
			logger.log(Level.INFO,"NO GIT RELEASE FOUND");
		}
		for (GitRelease c:list) {
			c.print();
		}
		if (stop) {
			waitInput();
		}
	}
	
	public static void simplePrintGitReleaseList(List<GitRelease> list) {
		for (GitRelease c:list) {
			logger.log(Level.INFO,c.getName());
		}
	}
	
	/**
	 * Stampa a schermo il progresso corrente riguardo l'analisi di tutti i commit considerati
	 */
	public static void printPercentage(double actual, double total) {
		double percentage = (actual/total*100.00);
		String perc = String.format("Commit Analysis: %.2f%%",percentage);
		logger.log(Level.INFO,perc);
	}
	
    int extractInt(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
	
	public static void printAllProjectClasses(GitRepo repo) {
		for (GitRelease r:repo.getReleaseList()) {
			for (ProjectClass p:r.getClassList()) {
				p.print();
			}
		}
	}
	
	/**
	 * Stampa a schermo un report con il numero di classi trovate ed il numero di classi buggy
	 */
	public static void countBuggyClass(List<ProjectClass> list) {
		int bugCount = 0;
		int classCount = 0;
		for (ProjectClass p:list) {
			if(p.isBuggy()) {
				bugCount++;
			}
			classCount++;
		}
		logger.log(Level.INFO,"Buggy Class: {0}", bugCount);
		logger.log(Level.INFO,"Total Class: {0}", classCount);
	}
	
	/**
	 * Stampa i parametri relativi alla corrente iterazione di Walk Forward
	 */
	public static void printWekaRunConfiguration(String classifierName,String featureSelectionName,String resamplingMethodName,String costSensitiveMethod) {
		String out = String.format("Configuration%nClassifier: %s%nFeature Selection: %s%nResampling: %s%nSensitivity: %s%n", classifierName,featureSelectionName,resamplingMethodName,costSensitiveMethod);
		logger.log(Level.INFO,out);
	}
	
	/**
	 * [DEBUG] Stampa a schermo un Timestamp per valutare le prestazioni
	 */
	public static String timestamp() {
		return new Timestamp(System.currentTimeMillis()).toString();
	}
}
