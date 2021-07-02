package api;

import java.util.List;
import jira.JiraRelease;

public class JiraAPI {
	
	/**
	 * Inizializza il progetto Jira.
	 **/
	private JiraAPI() {}
	
	/**
	 * Ottiene la prima release Jira tra quelle disponibili
	 */
	public static JiraRelease getOldestJiraRelease(List<JiraRelease> versions) {
		JiraRelease oldest = versions.get(0);
		for (JiraRelease v:versions) {
			if (v.getReleaseDate().isBefore(oldest.getReleaseDate())) {
				oldest = v;
			}
		}
		return oldest;
	}
}
