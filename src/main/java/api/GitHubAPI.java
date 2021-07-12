package api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import git.GitRelease;
import utils.Parameters;
/**
 * API per interagire con la repositori GitHub
 */
public class GitHubAPI {
	
	private GitHubAPI() {}
	/**
	 * Inizializza la repository Git. Se non è presente in locale effettua il clone
	 * dall'URL GitHub, altrimenti apre la copia della repository già presente in
	 * locale
	 **/
	public static Git initializeRepository(String projectName, String local) throws GitAPIException, IOException {
		Git git;
		String repoURL = String.format("https://github.com/%s/%s", Parameters.PROJ_ORG, projectName.toLowerCase());
		Logger logger = Logger.getLogger("GitHubAPI");
		if (!Files.exists(Paths.get(local))) {
			logger.log(Level.INFO, "Starting Cloning Repository");
			git = Git.cloneRepository().setURI(repoURL).setDirectory(new File(local)).call();
			logger.log(Level.INFO, "Repository cloned Succesfully");
		} else {
			logger.log(Level.INFO, "Local Git Repository Found. Opening.");
			git = Git.open(new File(local));
			git.checkout().setName(getDefaultBranchName(git)).call();
			git.pull().call();
		}
		logger.log(Level.INFO, "Repository opened Succesfully");
		return git;
	}
	
	/**
	 * Ritorna la branch di default della repository Git
	 **/
	private static String getDefaultBranchName(Git git) {	
		try {
			List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();
			for (Ref branch : branches) {
				String branchName = branch.getName();
				if (branchName.startsWith("refs/heads/")) {
					int startIndex = "refs/heads/".length();
					return branchName.substring(startIndex);
				}
			}
		} catch (GitAPIException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return "";
	}
	
	
	/**
	 * Ritorna la GitRelease più vecchia tra quelle passate in input
	 **/
	public static GitRelease getOldestGitRelease(List<GitRelease> versions) {
		GitRelease oldest = versions.get(0);
		for (GitRelease v:versions) {
			if (v.getDate().before(oldest.getDate())) {
				oldest = v;
			}
		}
		return oldest;
	}

	
	/**
	 * Ritorna la GitRelease più recente tra quelle passate in input
	 **/
	public static GitRelease getLatestGitRelease(List<GitRelease> versions) {
		GitRelease latest = versions.get(0);
		for (GitRelease v:versions) {
			if (v.getDate().after(latest.getDate())) {
				latest = v;
			}
		}
		return latest;
	}
	
	/**
	 * Ritorna tutte le release prima di una release specificata
	 */
	public static List<GitRelease> getReleaseBefore(List<GitRelease> versions, GitRelease lastRelease) {
		List<GitRelease> beforeList = new ArrayList<>();
		for (GitRelease r:versions) {
			if (r.equals(lastRelease)) {
				return beforeList;
			}
			beforeList.add(r);
		}
		return beforeList;
	}
}
