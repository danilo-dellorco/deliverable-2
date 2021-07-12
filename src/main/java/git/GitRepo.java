package git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import api.GitHubAPI;
import data.ProjectClass;
import jira.JiraRelease;
import jira.JiraTicket;
import utils.DateHandler;
import utils.Debug;
import utils.Parameters;

public class GitRepo {
	static Logger logger = Logger.getLogger(GitRepo.class.getName());
	
	String projectName;
	String releaseFilter;
	String local;
	List<GitCommit> commitList;
	List<GitCommit> fixCommitList;
	List<GitRelease> releaseList;
	private Git git;

	public GitRepo(String projectName, String local) throws GitAPIException, IOException {
		this.projectName = projectName;
		this.local = local;
		this.commitList = new ArrayList<>();
		this.fixCommitList = new ArrayList<>();
		this.releaseList = new ArrayList<>();
		this.git = GitHubAPI.initializeRepository(projectName, local);
		this.releaseFilter = fetchReleaseFilter();
	}
	
	public String fetchReleaseFilter() {
		if (projectName.equals(Parameters.BOOKKEEPER)) {
			return Parameters.BOOKKEEPER_FILTER_REL;
		}
		else {
			return Parameters.SYNCOPE_FILTER_REL;
		}
	}

	
	/**
	 * Ritorna la lista di tutti i commit effettuati sulla branch di default. Instanziamo un oggetto
	 * GitCommit soltanto per i commit presenti anche su Jira, in modo da aumentare le prestazioni.
	 */
	public void fetchCommits() throws GitAPIException, MissingObjectException, IncorrectObjectTypeException {
		GitRelease latest = GitHubAPI.getLatestGitRelease(this.releaseList);
		GitRelease oldest = GitHubAPI.getOldestGitRelease(this.releaseList);
		
		// Prendiamo soltanto i commit delle Release Git che sono anche in Jira
		LogCommand logCommand = this.git.log();		
		logCommand = logCommand.addRange(oldest.getCommit().getId(),latest.getCommit().getId());
		Iterable<RevCommit> logCommits = logCommand.call();

		for (RevCommit c : logCommits) {
			Date date = DateHandler.getDateFromEpoch(c.getCommitTime() * 1000L);
			ObjectId parentID = null;
			
			GitCommit commit = new GitCommit(c.getId(), date, c.getFullMessage());
			if (c.getParentCount() != 0) {
				parentID = c.getParent(0);
			}

			commit.setParentID(parentID); 
			commit.setAuthor(c.getAuthorIdent());
			this.commitList.add(commit);
		}
		orderCommitList();
	}

	/**
	 * Ordina la lista dei commit in base alla data e la setta nel parametro di classe
	 */
	public void orderCommitList() {
		this.commitList.sort(Comparator.comparing(GitCommit::getDate));
	}

	
	/**
	 * Ottiene la lista di tutte le release della repository Git
	 */
	public List<GitRelease> fetchReleases() throws IOException {
		List<Ref> tagList = null;
		List<GitRelease> fetchedReleases = new ArrayList<>();
		try {
			tagList = git.tagList().call();

		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		RevWalk walk = new RevWalk(this.git.getRepository());
		
		for (Ref tag : tagList) {

			String tagName = tag.getName();
			String releaseName = tagName.substring((releaseFilter + Parameters.TAG_FORMAT).length());

			// Alcuni tag contengono una versione per docker e non una release
			if (releaseName.contains("docker")) {
				continue;
			}
			RevCommit c = walk.parseCommit(tag.getObjectId());
			Date releaseDate = DateHandler.getDateFromEpoch(c.getCommitTime() * 1000L);
			
			ObjectId parentID = null;
			if (c.getParentCount() != 0) {
				parentID = c.getParent(0);
			}


			GitCommit gitCommit = new GitCommit(c.getId(), releaseDate, c.getFullMessage());
			gitCommit.setParentID(parentID); 
			gitCommit.setAuthor(c.getAuthorIdent());
			
			GitRelease release = new GitRelease(this.git, gitCommit, releaseName, releaseDate);
			fetchedReleases.add(release);

		}
		walk.close();
		return fetchedReleases;
	}
	
	
	/**
	 * Per ogni classe di ogni release, imposto (di default) la data di aggiunta su Git come la data della
	 * prima release.
	 */
	public void setDefaultAdditionDates(List<GitRelease> releases) {
		Date oldest = GitHubAPI.getOldestGitRelease(releases).getDate();
		for (GitRelease r:releases) {
			for (ProjectClass p:r.getClassList()) {
				p.setDateAdded(oldest);
			}
		}
	}

	
	/**
	 * Ottiene la lista dei commit tra una release e quella successiva. Utilizzata
	 * per assegnare ad ogni revisione la relativa release
	 */
	public void getRevisionsBetweenTwoReleases(GitRelease startRelease, GitRelease endRelease) {
		Date startDate;
		Date endDate = endRelease.getDate();
		Date commitDate;

		if (startRelease == null) {
			startDate = DateHandler.getDateFromEpoch(0);
		} else {
			startDate = startRelease.getDate();
		}

		for (GitCommit c : this.commitList) {
			commitDate = c.getDate();
			if (commitDate.after(startDate) && (commitDate.before(endDate) || commitDate.equals(endDate))) {
				c.setRelease(endRelease);
			}
		}
	}

	
	/**
	 * Assegna a tutti i commit della repository la relativa release, iterando il metodo getRevisionsBetweenTwoReleases()
	 */
	public void bindRevisionsToReleases() {
		GitRelease start = null;
		GitRelease end = null;

		// Primo passo dell'algoritmo per la prima release
		end = this.releaseList.get(0);
		getRevisionsBetweenTwoReleases(null, end);

		// Passo iterativo per le restanti release
		for (int i = 0; i < releaseList.size() - 1; i++) {
			start = this.releaseList.get(i);
			end = this.releaseList.get(i + 1);
			this.getRevisionsBetweenTwoReleases(start, end);
		}
	}

	
	/**
	 * Vengono mantenuti soltanto i commit di Git che hanno nel messaggio l'id del
	 * Ticket di Jira. I JiraTicket che non hanno una corrispondenza vengono ora RIMOSSI dalla lista.
	 * Ad ogni commit di tipo FixBug viene settato il riferimento al relativo JiraTicket.
	 */
	public List<GitCommit> filterCommits(List<JiraTicket> tickets) {
		List<GitCommit> filtered = new ArrayList<>();
		boolean founded = false;

		Iterator<JiraTicket> iterator = tickets.iterator();
		while (iterator.hasNext()) {
			JiraTicket t = iterator.next();
			for (GitCommit c : this.commitList) {
				if (c.hasTicketName(t.getName())) {
					filtered.add(c);
					c.setFixCommit(true);
					c.setTicket(t);
					founded = true;
					break;
				}
			}
			if (!founded) {			// se il ticket non ha un relativo commit su Git, viene rimosso dalla lista di JiraTickets
				iterator.remove();
			}
			founded = false;
		}
		return filtered;
	}

	
	/**
	 * Effettua il mapping tra le release Jira e quelle Git. Vengono
	 * scartate le release Git che non sono state inserite su Jira.
	 * @throws IOException 
	 */
	public void setCommonReleases(List<JiraRelease> jiraReleases) throws IOException {
		List<GitRelease> commonReleases = new ArrayList<>();
		List<GitRelease> gitReleases = fetchReleases();

		for (JiraRelease jR : jiraReleases) {
			for (GitRelease gR : gitReleases) {
				if (gR.getName().equals(jR.getName())) {
					gR.fetchClassList();
					commonReleases.add(gR);
					jR.setReleaseDate(DateHandler.convertToLocalDate(gR.getDate()));
					this.commitList.add(gR.getCommit());
					break;
				}
			}
		}

		// Ordino le release di Jira e Git in base alla data ed assegno gli ID incrementali ad entrambe
		jiraReleases.sort(Comparator.comparing(JiraRelease::getReleaseDate));
		commonReleases.sort(Comparator.comparing(GitRelease::getDate));
		for (int i = 0; i < commonReleases.size(); i++) {
			commonReleases.get(i).setId(i + 1);
			jiraReleases.get(i).setID(i + 1);
		}
		
		setDefaultAdditionDates(commonReleases);
		this.releaseList = commonReleases;
	}

	
	/**
	 * Ritorna la release tramite il nome della versione (es. 6.3.18)
	 */
	public GitRelease getReleaseByName(String version) {
		for (GitRelease r : this.releaseList) {
			if (r.getName().equals(version)) {
				return r;
			}
		}
		return null;
	}

	
	/**
	 * Imposta tutte le metriche tramite i diff di ogni commit. In particolare
	 * esamina tutte le DiffEntry del commit in input ed in base al tipo di commit (Revision/FixBug)
	 * e al tipo di Diff (ADD/MODIFY/RENAME) calcola le metriche opportune
	 * @throws GitAPIException 
	 */
	public void calcMetricsFromDiff(GitCommit commit, GitCommit previous) throws IOException {
		GitDiff gitDiff;
		GitRelease releaseClass;
		String pathClass;
		
		DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
			diffFormatter.setRepository(git.getRepository());
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
		ObjectId origin = commit.getParentID();
		if (previous != null) {
			origin = previous.getId();
		}
		List<DiffEntry> diffEntries = diffFormatter.scan(origin, commit.getId());
		List<DiffEntry> javaDiffs = GitDiff.filterJavaDiff(diffEntries);
		diffFormatter.flush();
		diffFormatter.close();
		int chgSetSize = diffEntries.size();
		
		for (DiffEntry d : javaDiffs) {
			gitDiff = new GitDiff(d);						
			releaseClass = commit.getRelease();
			pathClass = gitDiff.getPath();

			
			ProjectClass projectClass = releaseClass.getProjectClass(pathClass);
			
			// Se la classe è stata cancellata, non esiste al momento della release quindi non và considerata
			if (projectClass == null) {
				continue;
			}
			projectClass.setAnalyzed(true);
			
			// Gestione del Rename
			if (gitDiff.isRename()) {
				String oldPath = d.getOldPath();
				ProjectClass oldProjectClass = commit.getRelease().getProjectClass(oldPath);
				projectClass.setMetrics(oldProjectClass.getMetrics());
				
				if (commit.isFixCommit()) {
					setBuggynessWithAV(commit, oldPath);
				}
			}
			
			// Prendo la data di aggiunta della classe e la imposto anche per quella classe nelle future release
			if (gitDiff.isAdd()) {
				setAdditionDateOverReleases(projectClass, commit);
			}
			
			// Mi calcolo la LOC_TOUCHED solo per le modifiche su una classe
			if (gitDiff.isModify()) {
				EditList editList = diffFormatter.toFileHeader(d).toEditList();
				projectClass.getMetrics().calculateLocTouched(editList);
			}
			
			// Se il commit è di tipo fixBug setto la buggyness e aumento il numero di commit FixBug
			if (commit.isFixCommit()) {
				setBuggynessWithAV(commit,pathClass);
				projectClass.getMetrics().increaseNumberBugFixed();
			}
			
			// Set del chgSetSize && numberRevisions a prescindere dal tipo di Diff
			projectClass.getMetrics().increaseChgSetSize(chgSetSize);
			projectClass.getMetrics().increaseNumberRevisions();
			projectClass.getMetrics().calculateNAuth(commit.getAuthor().getName());
		}
	}

	
	/**
	 * Imposta la buggyness di una classe in tutte le Affected Versions. Viene settata la buggyness partendo
	 * dall'ultima AV (versione precedente al Fix) fino alla prima AV (injected version)
	 */
	public void setBuggynessWithAV(GitCommit fixCommit,String pathClass) {
		JiraTicket fixTicket = fixCommit.getTicket();
		List<JiraRelease> affectedVersions = fixTicket.getAffectedVersions();
		
		for (JiraRelease av:affectedVersions) {
			GitRelease gitAv = getReleaseByName(av.getName());
			ProjectClass projClass = gitAv.getProjectClass(pathClass);
			if (projClass!=null) {
				projClass.setBuggy(true);
				projClass.setAnalyzed(true);
			}
		}
	}
	
	
	
	/**
	 * Imposta l'addition di una classe come la data del commit dove è stato effettuato
	 * l'ADD della classe. Si esegue un ciclo su tutte le versioni successive per impostare la stessa
	 * data di aggiunta per la stessa classe nelle altre releases.
	 */
	public void setAdditionDateOverReleases(ProjectClass projectClass,GitCommit commit) {
		projectClass.setDateAdded(commit.getDate());
		List<GitRelease> newest = getReleasesFrom(commit.getRelease());
		for (GitRelease r:newest) {
			ProjectClass p = r.getProjectClass(projectClass.getPath());
			if (p!=null) {
				p.setDateAdded(commit.getDate());
			}
		}
	}
	
	
	/**
	 * Ritorna tutte le classi del progetto nelle varie release. Inoltre calcola l'AGE di ognuna delle classi
	 * come la differenza tra la data di aggiunta e la data della release della classe.
	 */
	public List<ProjectClass> getAllProjectClasses() {
		List<ProjectClass> list = new ArrayList<>();
		for (GitRelease r:this.releaseList) {
			for (ProjectClass p:r.getClassList()) {
				Date additionDate = p.getDateAdded();
				Date releaseDate = p.getRelease().getDate();
				int age = DateHandler.getWeeksBetweenDates(additionDate, releaseDate);
				p.getMetrics().setAge(age);
				list.add(p);
			}
		}
		return list;
	}
	
	public List<ProjectClass> getNoSnoringClasses() {
		List<ProjectClass> classes = new ArrayList<>();
		List<GitRelease> releases = new ArrayList<>();
		for (GitRelease r:this.releaseList) {
			List<ProjectClass> releaseClassList = r.getClassList();
			if (!validClassList(releaseClassList)) {
				r.print();
				continue;
			}
			releases.add(r);
			
			for (ProjectClass p:releaseClassList) {
				Date additionDate = p.getDateAdded();
				Date releaseDate = p.getRelease().getDate();
				int age = DateHandler.getWeeksBetweenDates(additionDate, releaseDate);
				p.getMetrics().setAge(age);
				classes.add(p);
			}
		}
		normalizeReleaseID(classes);
		return getHalfClasses(releases);
	}
	
	public boolean validClassList(List<ProjectClass> list) {
		boolean firstValue = list.get(0).isBuggy();
		for (int i = 0; i<list.size(); i++) {
			ProjectClass p = list.get(i);
			boolean actualValue = p.isBuggy();
			if (actualValue!=firstValue) {
				if (!firstValue) {
					ProjectClass n = list.get(i+1);
					n.setBuggy(true);
				}
				return true;
			}
		}
		return false;
	}

	public List<ProjectClass> getHalfClasses(List<GitRelease> releases) {
		int total = releases.size();
		int discarded = this.releaseList.size() - releases.size() +1 ;
		GitRelease endSnoring = releases.get(total/2+discarded/2); 
		List<ProjectClass> list = new ArrayList<>();
		for (GitRelease r:releases) {
			if (r.equals(endSnoring)) {
				return list;
			}
			for (ProjectClass p:r.getClassList()) {
				Date additionDate = p.getDateAdded();
				Date releaseDate = p.getRelease().getDate();
				int age = DateHandler.getWeeksBetweenDates(additionDate, releaseDate);
				p.getMetrics().setAge(age);
				list.add(p);
			}
		}
		return list;
	}
	
	public void normalizeReleaseID(List<ProjectClass> list) { 
		int actualID = 1;
		int classID = 0;
		for (ProjectClass p : list) {
			classID = p.getRelease().getId();
			if (classID != actualID) {
				actualID++;
				p.getRelease().setId(actualID);
			}
		}
	}
	
	/**
	 * Ritorna tutte le release successive ad una release passata in input
	 */
	public List<GitRelease> getReleasesFrom(GitRelease start){
		List<GitRelease> releases = new ArrayList<>();
		for (GitRelease r:this.releaseList) {
			if (r.getDate().after(start.getDate())) {
				releases.add(r);
			}
		}
		return releases;
	}
	
	
	/**
	 * Calcola ed imposta tutte le metriche relative alle classi del progetto. Esegue il metodo
	 * calcMetricsFromDiff() passando in input tutti i commit del progetto.
	 **/
	public void setMetrics() throws IOException {
		double actual = 1;
		double total = this.commitList.size();
		GitCommit previous = null;
		for (GitCommit c : this.commitList) {
			Debug.printPercentage(actual,total);
			calcMetricsFromDiff(c,previous);
			previous = c;
			actual++;
		}
	}
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	public List<GitCommit> getCommitList() {
		return this.commitList;
	}

	public void setCommitList(List<GitCommit> commitList) {
		this.commitList = commitList;
	}
	
	public List<GitRelease> getReleaseList() {
		return releaseList;
	}

	public void setReleaseList(List<GitRelease> releaseList) {
		this.releaseList = releaseList;
	}
	
	public List<GitCommit> getFixCommitList() {
		return fixCommitList;
	}
	
	public void setFixCommitList(List<GitCommit> fixCommitList) {
		this.fixCommitList = fixCommitList;
	}
}