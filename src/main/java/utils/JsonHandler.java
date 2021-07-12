package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;
/**
 *	Fornisce i vari metodi per la gestione dei JSON
 */
public class JsonHandler {
	
	private JsonHandler() {}
	
	/**
	 * Legge un Json partendo da un URL
	 */
	public static JSONObject readJsonFromUrl(String projectURL) throws IOException, JSONException {
		InputStream is = new URL(projectURL).openStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String jsonText = readAll(rd);
		is.close();
		return new JSONObject(jsonText);
	}

	
	/**
	 * Legge tutti i dati
	 */
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

}
