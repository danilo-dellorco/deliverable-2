package data;

import java.util.Date;

import git.GitRelease;
import tools.PathHandler;

public class ProjectClass {
	
	private String path;
	private String name;
	private GitRelease release;
	private Date dateAdded;
	private Metrics metrics;
	
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

	// Attributi della classe
	private boolean buggyness;
	
	public ProjectClass (String path, String name, GitRelease release){
		this.path = path;
		this.name = name;
		this.release = release;
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
	
	public void rename(String newPath) {
		this.setPath(newPath);
		this.setName(PathHandler.getNameFromPath(newPath));
	}
	
	public void print() {
		System.out.println("Class Name: "+this.path);
		System.out.println("Class Rele: "+this.release.getName());
		if (dateAdded!=null) {
			System.out.println("Class Date: "+this.getDateAdded());
			System.out.println("Reles Date: "+this.release.getDate());
		}
	}
}
