package dataset;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.errors.GitAPIException;

import data.ProjectClass;
import data.Proportion;
import git.GitCommit;
import git.GitRepo;
import jira.JiraProject;
import jira.JiraTicket;
import utils.CSVHandler;
import utils.Debug;
import utils.Parameters;
import utils.PathHandler;

public class CreateDataset {
	Logger logger = Logger.getLogger(CreateDataset.class.getName());
//TODO vedere buggyness syncope
	//TODO portare classi avanti come fabiano ma è complicato
	// TODO tutte altre strade non percorribili
	//TODO vedere se tipo si possono prendere file da release veramente precedente o cose simili 
	// TODO vedere se fare funzione checkInconsistency per controllare se 1.2.9 sta dopo 2.0.0
	
	
	public void create(String projName) throws GitAPIException, IOException{
		
		String gitFolderPath = PathHandler.getGitPath() + projName.toLowerCase();
		GitRepo repository = new GitRepo(projName.toLowerCase(), gitFolderPath);
		JiraProject jiraClient = new JiraProject(projName);

		
		// Mantengo soltanto le GitRelease che hanno una corrispettiva release su Jira
		repository.setCommonReleases(jiraClient.getReleaseList());
		repository.fetchCommits();					// Chiamato dopo aver impostato le release comuni in GitRepo così da prendere solo i commit delle release Jira
		repository.bindRevisionsToReleases();		// Associa ad ogni commit/revisione la relativa release
		
		// Ottengo tutti i ticket di Jira. Per ogni ticket senza AV o IV andiamo ad effettuare una predizione con Proportion.
		List<JiraTicket> ticketList = jiraClient.getTickets();
		Proportion.predictIV(Parameters.INCREMENTAL, ticketList, jiraClient.getReleaseList());
		
		// Otteniamo la lista di commit di tipo BugFix e la impostiamo nella lista della GitRepo
		List<GitCommit> fixBugCommits = repository.filterCommits(ticketList);
		repository.setFixCommitList(fixBugCommits);
		
		// Calcolo ed imposto tutte le metriche delle classi
		repository.setMetrics();																	
		
		// Genero il dataset
		generateDataset(projName, repository);
	}
	
	
	/**
	 * Ottiene la lista di tutte le classi e genera il dataset
	 * scrivendo su un file CSV
	 */
	public void generateDataset(String projName, GitRepo repo) {
		switch (projName) {
		case (Parameters.BOOKKEEPER):
			List<ProjectClass> projectClassList = repo.getAllProjectClasses();
			Debug.countBuggyClass(projectClassList);
			// Genero il dataset
			logger.log(Level.INFO,"Writing data on CSV...");
			CSVHandler.writeClassOnCSV(projectClassList, projName, Parameters.DATASET_CSV);
			CSVHandler.writeCSVForWeka(projectClassList, projName, Parameters.WEKA_CSV);
			break;
			
		case (Parameters.SYNCOPE):
			// Genero il dataset senza snoring
			List<ProjectClass> noSnoringClassList = repo.getNoSnoringClasses();
			Debug.countBuggyClass(noSnoringClassList);
		
			// Genero il dataset senza snoring classes
			logger.log(Level.INFO,"Writing no-snoring data on CSV...");
			CSVHandler.writeClassOnCSV(noSnoringClassList, projName, Parameters.DATASET_CSV);
			CSVHandler.writeCSVForWeka(noSnoringClassList, projName, Parameters.WEKA_CSV);
			break;
			
		default:
		}
		logger.log(Level.INFO,"CSV write completed succesfullt.\nDataset Created.");
	}
	
	public static void main(String[] args) throws GitAPIException, IOException {
		CreateDataset datasetCreator = new CreateDataset();
		datasetCreator.create(Parameters.BOOKKEEPER);
		datasetCreator.create(Parameters.SYNCOPE);
	}
}