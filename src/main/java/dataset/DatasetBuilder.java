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
import tools.CSVWriter;
import tools.Parameters;
import tools.PathHandler;

public class DatasetBuilder {

	public void create(String projName) throws GitAPIException, IOException{
		Logger logger = Logger.getLogger(DatasetBuilder.class.getName());
		String repoURL = String.format("https://github.com/%s/%s", Parameters.GIT_PROJ_ORG, Parameters.GIT_PROJ_NAME);
		String gitFolderPath = PathHandler.getGitPath() + Parameters.GIT_PROJ_NAME;
		GitRepo repository = new GitRepo(repoURL, gitFolderPath);
		JiraProject jiraClient = new JiraProject(Parameters.JIRA_PROJ_NAME);
		
		// Mantengo soltanto le GitRelease che hanno una corrispettiva release su Jira
		List<GitRelease> commonReleases = repository.filterReleases(jiraClient.getReleaseList(), repository.getReleaseList());	
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
		CSVWriter.writeClassOnCSV(projectClassList, Parameters.BOOKKEEPER, Parameters.DATASET_CSV);
		CSVWriter.writeCSVForWeka(projectClassList, Parameters.BOOKKEEPER, Parameters.WEKA_CSV);
		
		logger.log(Level.INFO,"CSV written successfully.\nEnd of the program.");
	}
	
	public static void main(String[] args) throws GitAPIException, IOException {
		DatasetBuilder builder = new DatasetBuilder();
		builder.create(Parameters.BOOKKEEPER);
	}
}