package git;

import java.util.Date;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;

import jira.JiraTicket;

public class GitCommit {
	ObjectId id;
	Date date;
	String message;
	ObjectId parentID;
	GitRelease release;
	JiraTicket ticket;
	PersonIdent author;
	
	
	public GitCommit(ObjectId id, Date date, String message) {
		this.id = id;
		this.date = date;
		this.message = message;
	}

	/**
	 * Ritorna true se il commit ha nel messaggio il ticketID specificato in input
	 **/
	public boolean hasTicketName(String ticketName) {
		int index = this.message.indexOf(ticketName);
		return (index!=-1);
	}
	
	/**
	 * [DEBUG] Stampa a schermo tutte le informazioni del GitCommit
	 **/
	public void print() {
		String output = String.format("ID: %s%ndate: %s%nmessage: %s", this.id, this.date, this.message);

		System.out.println(output);
		System.out.println("=".repeat(200));
	}
	
	/**
	 * [DEBUG] Stampa a schermo tutte le informazioni del GitCommit senza messaggio
	 **/
	public void printNoMsg() {
		String output = String.format("ID: %s%ndate: %s", this.id, this.date);

		System.out.println(output);
		System.out.println("=".repeat(200));
	}

	
	/*===============================================================================================
	 * Getters & Setters
	 */
	public Date getDate() {
		return this.date;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public void setParentID(ObjectId id) {
		this.parentID = id;
	}

	public ObjectId getParentID() {
		return this.parentID;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public GitRelease getRelease() {
		return release;
	}

	public void setRelease(GitRelease release) {
		this.release = release;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public JiraTicket getTicket() {
		return ticket;
	}

	public void setTicket(JiraTicket ticket) {
		this.ticket = ticket;
	}

	public PersonIdent getAuthor() {
		return author;
	}

	public void setAuthor(PersonIdent author) {
		this.author = author;
	}
	
}
