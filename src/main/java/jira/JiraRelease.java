package jira;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JiraRelease {
	Logger logger = Logger.getLogger(JiraRelease.class.getName());
	
	private String name;
	private LocalDate date;
	private int id;	//ID incrementale della release
	
	public JiraRelease(String name, LocalDate date) {
		this.name = name;
		this.date = date;
	}
	
	/**
	 * [DEBUG] Stampa a schermo tutte le informazioni su una JiraRelease
	 **/
	public void print() {
		String output = String.format("Name: %s%ndate: %s%nID: %d",this.name,this.date,this.id);
		logger.log(Level.INFO,output);
	}
	
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	public JiraRelease(String name) {
		this.name = name;
	}
	
	public JiraRelease() {
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public LocalDate getReleaseDate() {
		return date;
	}
	
	public void setReleaseDate(LocalDate releaseDate) {
		this.date = releaseDate;
	}
	
	public int getID() {
		return id;
	}
	
	public void setID(int id) {
		this.id = id;
	}
}
