package data;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import git.GitRelease;
import utils.PathHandler;

/**
 * Rappresenta una classe relativa al progetto da analizzare
 */
public class ProjectClass {
	Logger logger = Logger.getLogger(ProjectClass.class.getName());
	
	// Attributi della classe
	private boolean buggyness = false;
	
	public ProjectClass (String path, String name, GitRelease release){
		this.path = path;
		this.name = name;
		this.release = release;
	}
	
	private String path;
	private String name;
	private GitRelease release;
	private Date dateAdded;
	private Metrics metrics;
	
	public void rename(String newPath) {
		this.setPath(newPath);
		this.setName(PathHandler.getNameFromPath(newPath));
	}
	
	public Metrics getMetrics() {
		return metrics;
	}

	public void setMetrics(Metrics metrics) {
		this.metrics = metrics;
	}
	
	public Date getDateAdded() {
		return dateAdded;
	}
	
	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GitRelease getRelease() {
		return release;
	}

	public void setRelease(GitRelease release) {
		this.release = release;
	}

	public boolean isBuggy() {
		return buggyness;
	}

	public void setBuggy(boolean buggyness) {
		this.buggyness = buggyness;
	}

	public void print() {
		String out1 = String.format("Class Name: %s%nnClass Release: %s", this.path, this.release.getName());
		logger.log(Level.INFO, out1);
		if (dateAdded!=null) {
			String out2 = String.format("Class Date: %s%nRelease Date: %s", this.getDateAdded(), this.release.getDate());
			logger.log(Level.INFO, out2);
		}
	}
}
