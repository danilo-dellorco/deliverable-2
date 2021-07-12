package utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Fornisce metodi di supporto alla gestione delle date.
 */
public class DateHandler {
	
	private DateHandler() {}
	
	/**
	 * Converte una data Date in un LocalDate
	 */
	public static LocalDate convertToLocalDate(Date dateToConvert) {
	    return Instant.ofEpochMilli(dateToConvert.getTime())
	      .atZone(ZoneId.systemDefault())
	      .toLocalDate();
	}
	
	
	/**
	 * Converte una data come stringa in un oggetto LocalDate
	 */
	public static LocalDate stringToDate(String s) {
		String dateString = s.substring(0, Parameters.DATE_FORMAT.length());
		return LocalDate.parse(dateString);
	}
	
	
	/**
	 * Ricava un oggetto Date dai millisecondi
	 */
	public static Date getDateFromEpoch(long millisecondsFromEpoch) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millisecondsFromEpoch);	
		return calendar.getTime();
	}
	
	
	/**
	 * Calcola la differenza in settimane tra due date
	 */
	public static int getWeeksBetweenDates(Date date1, Date date2) {
		long epoc1 = date1.getTime();
		long epoc2 = date2.getTime();
		
		long seconds = (epoc2 - epoc1)/1000;
		long secondsInAWeek = (7*24*60*60);
		return (int) (seconds/secondsInAWeek);
	}
}
