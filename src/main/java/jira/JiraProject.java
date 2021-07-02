package jira;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import api.JiraAPI;
import tools.DateHandler;
import tools.JsonHandler;
import tools.Parameters;

public class JiraProject {
	private String url;
	private String name;
	private List<JiraRelease> releaseList;

	public JiraProject(String projectName) {
		this.name = projectName;
		this.url = Parameters.REST_API + projectName;
		this.releaseList = fetchReleases();
	}

	/*
	 * Ottiene da Jira tutte le release, ignorando quelle senza informazioni
	 */
	public List<JiraRelease> fetchReleases() {
		JSONObject json;
		List<JiraRelease> allRelease = new ArrayList<>();
		try {
			json = JsonHandler.readJsonFromUrl(this.url);
			JSONArray releasesList = json.getJSONArray("versions");

			for (int i = 0; i < releasesList.length(); i++) {
				JSONObject tempRelease = releasesList.getJSONObject(i);

				if (tempRelease.has("releaseDate") && tempRelease.getBoolean(Parameters.RELEASED_JSON)) {
					LocalDate releaseDate = LocalDate.parse(tempRelease.getString("releaseDate"));
					JiraRelease release = new JiraRelease();
					release.setName(tempRelease.getString(Parameters.NAME_JSON));
					release.setReleaseDate(releaseDate);
					allRelease.add(release);
				}
			}
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		return allRelease;
	}

	
	/*
	 * Ottiene tutti i ticket riferiti a bug risolti tramite le Rest API di Jira.
	 * Dopo aver ottenuto la lista di tutti i ticket mantiene soltanto quelli con informazioni
	 * coerenti o su cui è possibile utilizzare proportion
	 */
	public List<JiraTicket> getTickets() throws JSONException, IOException {
		int i = 0;
		int j = 0;
		int nIssues = 1000;
		List<JiraTicket> tickets = new ArrayList<>();

		while (i < nIssues) {
			j = i + 1000;
			String query = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + this.name
					+ "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
					+ "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,fixVersions,resolutiondate,versions,created&startAt="
					+ i + "&maxResults=" + j;

			JSONObject json = JsonHandler.readJsonFromUrl(query);
			JSONArray issues = json.getJSONArray("issues");
			nIssues = json.getInt("total");
			i = j;

			for (int k = 0; k < issues.length(); k++) {
				JSONObject tempTicket = issues.getJSONObject(k);
				JSONObject fields = tempTicket.getJSONObject("fields");

				JSONArray fixedVersionsJson = fields.getJSONArray("fixVersions");
				JSONArray versionsJson = fields.getJSONArray("versions");

				List<JiraRelease> fixedVersions = cleanVersions(fixedVersionsJson);
				List<JiraRelease> versions = cleanVersions(versionsJson);
				JiraTicket t = new JiraTicket();
					t.setId(tempTicket.getString("id"));
					t.setName(tempTicket.getString("key"));
					t.setResolutionDate(DateHandler.stringToDate(fields.getString("resolutiondate")));
					t.setCreationDate(DateHandler.stringToDate(fields.getString("created")));
					t.setFixedVersions(fixedVersions);
					t.setAffectedVersions(versions);
				tickets.add(t);
			}
		}	
		// Ottenuti tutti i ticket di Jira ritorniamo soltanto quelli con informazioni coerenti.
		return filterTickets(tickets);
	}

	
	/*
	 * Pulizia dei tickets: si identifica la FV dalla lista di FV ritornate da Jira e
	 * si scartano eventuali ticket dove non posso identificare una FV
	 */
	public List<JiraTicket> cleanTickets(List<JiraTicket> tickets) throws JSONException {
		Iterator<JiraTicket> iterator = tickets.iterator();
		while (iterator.hasNext()) {
			JiraTicket t = iterator.next();
			t.setOv(t.getObservedVersion(releaseList));
			t.setIv(t.getIvFromAffectedVersions());

			if (t.getFixedVersions().isEmpty()) {
				fixEmptyFV(t);
				if (t.getFixedVersions().isEmpty()) {
					iterator.remove();
				}
			} else if (t.getFixedVersions().size() == 1) {
				t.setFv(t.getFixedVersions().get(0));
			} else {
				fixMultipleFV(t);
			}
		}
		return tickets;
	}

	
	/*
	 * Se la FV non è presente, si considera la prima versione successiva (in ordine
	 * cronologico) alla data di risoluzione del ticket
	 */
	private void fixEmptyFV(JiraTicket ticket) {
		JiraRelease newFV = new JiraRelease();
		LocalDate resolutiondate = ticket.getResolutionDate();
		if (resolutiondate.compareTo(this.releaseList.get(0).getReleaseDate()) <= 0) {
			newFV = this.releaseList.get(0);
			ticket.setFv(newFV);
		} else {
			for (int i = 0; i < this.releaseList.size() - 1; i++) {
				JiraRelease prevRelease = this.releaseList.get(i);
				JiraRelease nextRelease = this.releaseList.get(i + 1);

				if (resolutiondate.isAfter(prevRelease.getReleaseDate())
						&& (resolutiondate.compareTo(nextRelease.getReleaseDate()) <= 0)) {
					newFV = this.releaseList.get(0);
					ticket.setFv(newFV);
				}
			}
			
			// Caso in cui la data di risoluzione supera l'ultima release
			if (resolutiondate.isAfter(this.releaseList.get(this.releaseList.size() - 1).getReleaseDate())) {
				ticket.setFv(newFV);
			}
		}
	}

	
	/*
	 * Se ci sono piu FV, si considera la versione piu vecchia nella lista come FV
	 */
	private void fixMultipleFV(JiraTicket ticket) {
		List<JiraRelease> oldFV = ticket.getFixedVersions();
		JiraRelease newFV = JiraAPI.getOldestJiraRelease(oldFV);
		ticket.setFv(newFV);
	}

	
	//TODO queste informazioni vanno nella relazione, qui bisogna lasciare una spiegazione del metodo
	/*
	 * Assumiamo che OV=FV nel caso in cui si abbia OV>FV. 
	 * Eliminiamo i ticket nei seguenti casi:
	 * 		1) IV=OV=FV, il bug non esiste nella release
	 * 		perchè l'intervallo di AV è vuoto
	 * 
	 * 		2) OV=FV e IV non nota, dovremmo applicare
	 * 		proportion ma ricadiamo nel caso 1)
	 * 
	 * 		3) IV>OV=FV, l'IV non è coerente con il
	 * 		resto dei dati, perciò non la conosciamo e ricadiamo nel caso 2)
	 * 
	 * I ticket rimanenti sono quelli per cui IV<OV==FV e OV<FV.
	 * Nel primo caso, i dati sono coerenti e conosco l'intervallo [IV,FV) che sono le AV.
	 * Nel secondo caso andiamo a predirre l'IV applicando proportion
	 */
	public List<JiraTicket> selectTickets(List<JiraTicket> tickets) throws JSONException {
		JiraRelease ivRelease;
		JiraRelease ovRelease;
		JiraRelease fvRelease;
		LocalDate ovDate;
		LocalDate ivDate;
		LocalDate fvDate;

		Iterator<JiraTicket> iterator = tickets.iterator();
		while (iterator.hasNext()) {
			JiraTicket t = iterator.next();
			ivRelease = t.getIv();
			ovRelease = t.getOv();
			fvRelease = t.getFv();

			// Se OV>FV, impostiamo OV=FV
			if (ovRelease.getReleaseDate().isAfter(fvRelease.getReleaseDate())) {
				t.setOv(t.getFv());
				ovRelease = t.getOv();
			}

			// Il ticket Jira ha dati sulle AV
			if (!t.getAffectedVersions().isEmpty()) {
				ovDate = ovRelease.getReleaseDate();
				ivDate = ivRelease.getReleaseDate();
				
				// Se IV >= OV=FV, elimino il bug altrimenti lo mantengo perchè IV < OV=FV
				if (!ovDate.isAfter(ivDate)) {
					iterator.remove();
				}			
			}

			// Il ticket Jira non ha dati su AV
			else {
				ovDate = ovRelease.getReleaseDate();
				fvDate = fvRelease.getReleaseDate();
				
				// Se OV=FV, elimino il bug altrimenti lo mantengo perchè OV<FV
				if (ovDate.isEqual(fvDate)) {
					iterator.remove();
				}
			}
			t.fixAvList(this.releaseList);
		}
		return tickets;
	}
	
	
	/*
	 * Corregge i ticket con informazioni incomplete e filtra soltanto i ticket con
	 * informazioni coerenti o su cui è possibile applicare Proportion
	 */
	public List<JiraTicket> filterTickets(List<JiraTicket> tickets) {
		List<JiraTicket> cleanedTickets = cleanTickets(tickets);
		return selectTickets(cleanedTickets);
	}

	
	/*
	 * Mantiene soltanto le release/versioni che sono state effettivamente rilasciate su Jira/Git
	 * Questo perchè alcune versioni possono essere riportate in maniera erronea su Jira, ma sono
	 * in realtà versioni mai rilasciate.
	 */
	public List<JiraRelease> cleanVersions(JSONArray json) {
		List<JiraRelease> result = new ArrayList<>();
		JiraRelease ticketRelease = new JiraRelease();
		for (Object v : json) {
			JSONObject cleanV = (JSONObject) v;
			if (cleanV.getBoolean(Parameters.RELEASED_JSON)) {
				String release = cleanV.getString(Parameters.NAME_JSON);
				for(int i=0; i<releaseList.size(); i++) {
					if(release.equalsIgnoreCase(releaseList.get(i).getName())) {
						ticketRelease=releaseList.get(i);
					}
				}
				result.add(ticketRelease);
			}
		}
		return result;
	}
	
	
	/*
	 * Ritorna la release tramite il nome della versione (es. 4.4.0)
	 */
	public static JiraRelease getReleaseByName(List<JiraRelease> releases, String name) {
		for (JiraRelease r : releases) {
			if (r.getName().equalsIgnoreCase(name)) {
				return r;
			}
		}
		return null;
	}
	
	
	
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	
	public List<JiraRelease> getReleaseList() {
		return this.releaseList;
	}

	public void setReleaseList(List<JiraRelease> releaseList) {
		this.releaseList = releaseList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
