package data;

import java.util.List;

import jira.JiraRelease;
import jira.JiraTicket;

public class Proportion {
	private static int p = 0;
	
	private Proportion() {}

	
	/**
	 * Implementazione del metodo "Incremental" per il calcolo di Proportion
	 **/
	public static int incremental(List<JiraTicket> tickets) {
		double sum = 0;
		int k = 0;
		int iv;
		int ov;
		int fv;

		for (JiraTicket t : tickets) {
			iv = t.getIv().getID();
			if (iv != 0) { // if IV is known
				ov = t.getOv().getID();
				fv = t.getFv().getID();
				sum = sum + (double) (fv - iv) / (fv - ov + 1);
				k++;
			}
		}
		if (k != 0) {
			p = (int) Math.round(sum / k);
		}

		return p;
	}
	
	
	/**
	 * Predice l'Injected Version tramite Proportion ed imposta l'IV predetta per tutti
	 * i ticket Jira che non hanno una IV.
	 **/
	public static void predictIV(String mode, List<JiraTicket> tickets, List<JiraRelease> releases) {
		int ov;
		int fv;
		int predictedIV;
		switch (mode) {
		
		case "incremental":
			p = incremental(tickets);
			break;

		case "moving_window":
			// TODO implementare movingwindow
			break;

		default:
			// Default
		}

		for (JiraTicket t : tickets) {
			ov = t.getOv().getID();
			fv = t.getFv().getID();
			
			// IV non presente, applichiamo proportion, e a seconda della IV stimata
			// recuperiamo le informazioni dalla lista di releases
			predictedIV = fv - p * (fv - ov + 1);
				
			if (predictedIV == 0) {
				t.setIv(releases.get(0));
			}
			else {
				t.setIv(releases.get(predictedIV - 1));
			}
			t.fixAvList(releases); //Una volta predetta la IV imposto le AVs coerentemente con la FV
		}
	}
}
