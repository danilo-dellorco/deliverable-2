package dataset;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.errors.GitAPIException;

import data.ProjectClass;
import data.Proportion;
import git.GitCommit;
import git.GitRelease;
import git.GitRepo;
import jira.JiraProject;
import jira.JiraTicket;
import utils.CSVHandler;
import utils.Debug;
import utils.Parameters;
import utils.PathHandler;

public class CreateDataset {
//TODO vedere buggyness syncope
	
	
	public void create(String projName) throws GitAPIException, IOException{
		Logger logger = Logger.getLogger(CreateDataset.class.getName());
		String gitFolderPath = PathHandler.getGitPath() + projName.toLowerCase();
		GitRepo repository = new GitRepo(projName.toLowerCase(), gitFolderPath);
		JiraProject jiraClient = new JiraProject(projName);
		
		Debug.printJiraReleaseList(jiraClient.getReleaseList(), false);
		Debug.printGitReleaseList(repository.getReleaseList(), false);
		
		// Mantengo soltanto le GitRelease che hanno una corrispettiva release su Jira
		List<GitRelease> commonReleases = repository.filterReleases(jiraClient.getReleaseList(), repository.getReleaseList());	
		
		Debug.printGitReleaseList(commonReleases, false);
		repository.setReleaseList(commonReleases);	// Imposto come lista delle release solo quelle presenti anche su Jira
		repository.fetchCommits();					// Chiamato dopo aver impostato le release comuni in GitRepo così da prendere solo i commit delle release Jira
		repository.bindRevisionsToReleases();		// Associa ad ogni commit/revisione la relativa release
		
		// Ottengo tutti i ticket di Jira. Per ogni ticket senza AV o IV andiamo ad effettuare una predizione con Proportion.
		List<JiraTicket> ticketList = jiraClient.getTickets();
		Proportion.predictIV(Parameters.INCREMENTAL, ticketList, jiraClient.getReleaseList());
		
		// Otteniamo la lista di commit di tipo BugFix e la impostiamo nella lista della GitRepo
		List<GitCommit> fixBugCommits = repository.filterCommits(ticketList);
		repository.setFixCommitList(fixBugCommits);
		
		// Imposto tutte le metriche delle classi
		repository.setMetrics();																	

		// Ottengo tutta la lista delle classi e calcolo per ognuna anche l'Age
		List<ProjectClass> projectClassList = repository.getAllProjectClasses();
		
		// Genero il dataset
		logger.log(Level.INFO,"Writing CSV...");
		CSVHandler.writeClassOnCSV(projectClassList, projName, Parameters.DATASET_CSV);
		CSVHandler.writeCSVForWeka(projectClassList, projName, Parameters.WEKA_CSV);
		
		logger.log(Level.INFO,"CSV written successfully.\nEnd of the program.");
	}
	
	public static void main(String[] args) throws GitAPIException, IOException {
		CreateDataset builder = new CreateDataset();
//		builder.create(Parameters.BOOKKEEPER);
		builder.create(Parameters.SYNCOPE);
	}
}