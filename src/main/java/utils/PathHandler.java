package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
/**
 *	Fornisce tutti i metodi per la gestione dei Path di sistema
 */
public class PathHandler {
	private PathHandler() {}
	
	/**
	 * Ottiene il Path della cartella Git locale tramite il file di configurazione "paths.config"
	 */
	public static String getGitPath() throws FileNotFoundException {
		File myObj = new File ("./paths.config");
		Scanner myReader = new Scanner(myObj);
		String gitPath = null;
		while (myReader.hasNextLine()) {
			String data = myReader.nextLine();
			gitPath = data.substring(data.lastIndexOf("=") + 1);
		}
		myReader.close();
		return gitPath;
    }
	
	
	/**
	 * Dato un full path in input ritorna soltanto il nome della classe Java
	 */
	public static String getNameFromPath(String path) {
		return path.substring(path.lastIndexOf("/")+1);
	}
}
