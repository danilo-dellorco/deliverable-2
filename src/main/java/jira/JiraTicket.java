package jira;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import api.JiraAPI;

public class JiraTicket {
	private String id;
	private String name;
	private LocalDate creationDate;
	private LocalDate resolutionDate;
	private JiraRelease iv;
	private JiraRelease ov;
	private JiraRelease fv;							//Singola Fixed Version dopo aver corretto i dati del Ticket
	private List<JiraRelease> fixedVersions;		//Lista delle Fixed Versions ricavate da Jira
	private List<JiraRelease> affectedVersions;
	
	
	/**
	 * Calcolo dell'OV di un ticket come prima release successiva alla data di creazione
	 **/
	public JiraRelease getObservedVersion(List<JiraRelease> releases) {
		LocalDate creationdate = this.getCreationDate();
		JiraRelease ovTemp = new JiraRelease();
		if(creationdate.compareTo(releases.get(0).getReleaseDate()) <=0) {
			ovTemp = releases.get(0);
		} 
		else {
			for (int i = 0; i<releases.size()-1; i++) {
				JiraRelease prevRelease = releases.get(i);
				JiraRelease nextRelease = releases.get(i+1);
				
				if (creationdate.isAfter(prevRelease.getReleaseDate()) && (creationdate.compareTo(nextRelease.getReleaseDate()) <=0)) {
					ovTemp = nextRelease;
				}
			}
		}
		return ovTemp;
	}
	
	
	/**
	 * Aggiunge una AV alla lista delle Affected Versions
	 **/
	public void addAffectedVersion(JiraRelease av) {
		if (!this.affectedVersions.contains(av)) {
			this.affectedVersions.add(av);
		}
	}
	
	
	/**
	 * Calcolo dell'IV come release più vecchia tra le AV, se presenti
	 **/
	public JiraRelease getIvFromAffectedVersions() {
		List<JiraRelease> av = this.getAffectedVersions();
		JiraRelease ivTemp = new JiraRelease();
		if (av.isEmpty()) {
			ivTemp.setName("");
			ivTemp.setID(0);
			return ivTemp;
		}
		else if (av.size()==1) {
			ivTemp=av.get(0);
		}
		else {
			ivTemp=JiraAPI.getOldestJiraRelease(av);
		}
		return ivTemp;
	}
	
	
	/**
	 * Correggo la lista delle affected versions impostando AV = [IV,FV).
	 * Questo perchè la lista di AV potrebbe presentare: 
	 * 		1) Un AV che coincide con FV
	 * 		2) Un AV che supera la FV
	 * 		3) Una lista di AV che copre parzialmente l'intervallo [IV,FV)
	 **/
	public void fixAvList(List<JiraRelease> releases) {
		int ivID = iv.getID();
		int fvID = fv.getID();
		affectedVersions = new ArrayList<>();
		
		if (ivID == 0) {
			return;
		}
		
		// Imposto la lista di AV come [IV,FV)
		for(int j=ivID; j<fvID; j++) {
			JiraRelease av = releases.get(j-1);
			this.addAffectedVersion(av);
		}
	}
	
	
	/**
	 * [DEBUG] Stampa a schermo tutte le informazioni sul JiraTicket
	 **/
	public void print() {
		List<JiraRelease> av = this.getAffectedVersions();
		System.out.println("ID: " + this.getId());
		System.out.println("Name: " + this.getName());
		
		System.out.print("AV: [");
		for(int i=0; i<av.size(); i++) {
			System.out.print(av.get(i).getName() + " ");
		}
		System.out.print("]\n");
		
		System.out.println("FV: " + this.getFv().getName());
		System.out.println("OV: " + this.getOv().getName());
		System.out.println("IV: " + this.getIv().getName());
		System.out.println("Creation Date: " + this.getCreationDate());
		System.out.println("Resolution Date: " + this.getResolutionDate());
		System.out.println("\n");
	}
	
	
	
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public LocalDate getResolutionDate() {
		return resolutionDate;
	}

	public void setResolutionDate(LocalDate resolutionDate) {
		this.resolutionDate = resolutionDate;
	}

	public List<JiraRelease> getAffectedVersions() {
		return affectedVersions;
	}

	public void setAffectedVersions(List<JiraRelease> affectedVersions) {
		this.affectedVersions = affectedVersions;
	}

	public List<JiraRelease> getFixedVersions() {
		return this.fixedVersions;
	}
	
	public void setFixedVersions(List<JiraRelease> fixedVersions) {
		this.fixedVersions = fixedVersions;
	}

	public JiraRelease getIv() {
		return iv;
	}

	public void setIv(JiraRelease iv) {
		this.iv = iv;
	}

	public JiraRelease getOv() {
		return ov;
	}

	public void setOv(JiraRelease ov) {
		this.ov = ov;
	}

	public JiraRelease getFv() {
		return fv;
	}

	public void setFv(JiraRelease fv) {
		this.fv = fv;
	}
}