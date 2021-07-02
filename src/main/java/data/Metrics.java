package data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;

public class Metrics {

	private int size;				//  1) Numero di righe di codice, escluse linee di commento e linee vuote
	private int locTouched; 		//  2) Numero di righe di codice modificate nel commit
	private int numberRevisions; 	//  3) Numero di revisioni in cui è stato modificata la classe (nella release)
	private int numberBugFixes; 	//  4) Numero di commit di tipo FixBug che hanno toccato la classe (nella release)
	private int locAdded; 			//  5) Numero di righe di codice aggiunte nel commit
	private int maxLocAdded; 		//  6) Massimo numero di LocAdded tra tutte le revisioni che hanno toccato la classe (nella release)
	private int avgLocAdded; 		//  7) Media tra tutti i LocAdded delle revisioni che hanno toccato la classe (nella release)
	private int age; 				//  8) Differenza in settimane tra la data di creazione della classe e la data della release corrente
	private int chgSetSize;			//  9) Numero di file committed insieme alla classe
	private int maxChgSetSize;		//  10) Numero massimo file committed insieme alla classe
	private int avgChgSetSize;		//  11) Numero medio file committed insieme alla classe
	private int nAuth;				//  12) Numero di autori che hanno toccato la classe (nella release)
	
	private int counterLocAdded;
	private int counterChurn;
	private int counterChgSet;
//	private List<PersonIdent> listOfAuthors = new ArrayList<>();
	private List<String> listOfAuthors = new ArrayList<>();
	
	// Metriche non utilizzate
	private int churn; 				//  8) Differenza tra LocAdded e LocDeleted
	private int maxChurn; 			//  9) Massimo tra tutti i churn delle revisioni (nella release)
	private int avgChurn; 			// 10) Media tra tutti i churn delle revisioni (nella release)

	public Metrics() {
		//Costruttore
	}
	

	/**
	 * Calcola le linee di codice di una classe, escludendo Commenti e linee vuote
	 * 
	 */
	public void calculateSize(ObjectId objectId, ObjectReader reader)
			throws LargeObjectException,IOException {
		byte[] data = reader.open(objectId).getBytes();
		String content = new String(data, StandardCharsets.UTF_8);

		int calcSize = 0;
		Scanner scanner = new Scanner(content);

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.replace("\s", "");
			if (!(line.startsWith("/") || line.startsWith("*") || line.startsWith("//") || line.startsWith("*/")
					|| line.equalsIgnoreCase(""))) {
				calcSize = calcSize + 1;
			}
		}
		scanner.close();
		this.setSize(calcSize);
	}
	
	
	/**
	 * Calcola le linee di codice modificate di una classe in un commit
	 */
	public void calculateLocTouched(EditList editList) {
		int linesAdded = 0;
		int linesDeleted = 0;
		for (Edit edit : editList) {
			linesDeleted += edit.getEndA() - edit.getBeginA();
			linesAdded += edit.getEndB() - edit.getBeginB();
		}
		this.locTouched += (linesAdded + linesDeleted);
		this.locAdded += linesAdded;
		calculateChurn(linesAdded, linesDeleted);
		calculateMaxLocAdded(linesAdded);
		calculateAVGLocAdded(linesAdded);
	}
	
	
	/**
	 * Ogni volta viene calcolata la media delle linee aggiunte.
	 * Viene anche calcolata la metrica locAdded (totale di locAdded in una release)
	 */
	public void calculateAVGLocAdded(int linesAdded) {
		if (linesAdded!=0) {
			counterLocAdded++;
			avgLocAdded = locAdded/counterLocAdded;
		}
	}

	
	/**
	 * Calcola il churn come (linesAdded-linesDeleted) tu tutte le versioni.
	 */
	private void calculateChurn(int linesAdded, int linesDeleted) {
		this.churn += linesAdded - linesDeleted;
		this.counterChurn ++;
		calculateMaxChurn(this.churn);
		calculateAVGChurn();
	}

	
	/**
	 * Calcola la metrica Max_Churn
	 */
	private void calculateMaxChurn(int churn) {
		if (churn > this.maxChurn) {
			this.maxChurn = churn;
		}
	}
	
	
	/**
	 * Calcola la metrica AVGChurn
	 */
	private void calculateAVGChurn() {
		this.avgChurn = this.churn/counterChurn;
	}
	

	/**
	 * Calcola la metrica MaxLocADDED
	 */
	private void calculateMaxLocAdded(int locAdded) {
		if (locAdded > this.maxLocAdded) {
			this.maxLocAdded = locAdded;
		}
	}
	
	
	/*
	 * Incrementa di 1 il numero di BugFixed in cui è coinvolto
	 */
	public void increaseNumberBugFixed() {
		this.numberBugFixes ++;
	}
	
	
	/*
	 * Incrementa di 1 il numero di NumberRevisions in cui è coinvolto
	 */
	public void increaseNumberRevisions() {
		this.numberRevisions ++;
	}
	
	
	/*
	 * Incrementa il numero di file insieme ai quali è stata committata la classe
	 */
	public void increaseChgSetSize(int chg) {
		this.counterChgSet++;
		this.chgSetSize += chg;
		calculateMaxChgSetSize(chg);
		calculateAVGChgSetSize();
	}
	
	
	/*
	 * Calcola il numero massimo di ChgSet
	 */
	public void calculateMaxChgSetSize(int chg) {
		if (chg > maxChgSetSize) {
			this.maxChgSetSize = chg;
		}
	}
	
	
	/*
	 * Calcola il numero medio di ChgSet
	 */
	public void calculateAVGChgSetSize() {
		this.avgChgSetSize = this.chgSetSize/counterChgSet;
	}
	
	/*
	 * Calcola il numero di autori che hanno interaggito su una classe C (nella release)
	 */
	public void calculateNAuth(String authorName) {
		if (!listOfAuthors.contains(authorName)) {
			nAuth++;
			listOfAuthors.add(authorName);
		}
	}
	
	/*
	 * [DEBUG] Stampa i dati sulle metriche
	 */
	public void print() {
		System.out.println(String.format("Size: %d%nLocTouched: %d%nMaxLocAdded: %d%nChurn: %d%nMaxChurn: %d", size, locTouched,
				maxLocAdded, churn, maxChurn));
	}
	
	
	
	/*===============================================================================================
	 * Getters & Setters
	 */

	public double getAvgLocAdded() {
		return this.avgLocAdded;
	}

	public int getLocAdded() {
		return this.locAdded;
	}
	
	public int getChurn() {
		return this.churn;
	}
	
	public int getMaxChurn() {
		return this.maxChurn;
	}
	
	public int getMaxLocAdded() {
		return this.maxLocAdded;
	}
	
	public int getChgSetSize() {
		return chgSetSize;
	}
	
	public void setChgSetSize(int chgSetSize) {
		this.chgSetSize = chgSetSize;
	}

	public int getNumberBugFixes() {
		return numberBugFixes;
	}

	public void setNumberBugFixes(int numberBugFixes) {
		this.numberBugFixes = numberBugFixes;
	}

	public int getNumberRevisions() {
		return numberRevisions;
	}

	public void setNumberRevisions(int numberRevisions) {
		this.numberRevisions = numberRevisions;
	}

	public int getAvgChurn() {
		return avgChurn;
	}

	public void setAvgChurn(int avgChurn) {
		this.avgChurn = avgChurn;
	}

	public int getMaxChgSetSize() {
		return maxChgSetSize;
	}

	public void setMaxChgSetSize(int maxChgSetSize) {
		this.maxChgSetSize = maxChgSetSize;
	}

	public int getAvgChgSetSize() {
		return avgChgSetSize;
	}

	public void setAvgChgSetSize(int avgChgSetSize) {
		this.avgChgSetSize = avgChgSetSize;
	}
	
	public void setAge(int age) {
		this.age = age;
	}
	
	public int getAge() {
		return this.age;
	}
	
	public void setLocTouched(int locTouched) {
		this.locTouched = locTouched;
	}
	
	public int getLocTouched() {
		return this.locTouched;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	public int getnAuth() {
		return nAuth;
	}

	public void setnAuth(int nAuth) {
		this.nAuth = nAuth;
	}
	
}
