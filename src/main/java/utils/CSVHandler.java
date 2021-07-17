package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.WekaMetrics;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import data.Metrics;
import data.ProjectClass;
import git.GitCommit;
import git.GitRelease;
import jira.JiraRelease;
import jira.JiraTicket;

public class CSVHandler {
	static Logger logger = Logger.getLogger(CSVHandler.class.getName());

	private CSVHandler() {
	}

	/**
	 * [DEBUG] Salva su un .csv i dati delle Release
	 **/
	public static void writeReleasesOnCSV(List<GitRelease> releases, String projectName, String fileName) {

		String outputName = Parameters.OUTPUT_PATH + projectName + fileName + ".csv";

		try (FileWriter fileWriter = new FileWriter(outputName)) {
			StringBuilder outputBuilder = new StringBuilder("ID;Version Name;Release Date\n");

			for (GitRelease r : releases) {
				outputBuilder.append(r.getId() + ";" + r.getName() + ";" + r.getDate() + "\n");
			}
			fileWriter.append(outputBuilder.toString());

		} catch (Exception e) {
			Logger logger = Logger.getLogger(CSVHandler.class.getName());
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * [DEBUG] Salva su un .csv i dati dei Commit
	 */
	public static void writeCommitsOnCSV(List<GitCommit> commits, String projectName, String fileName) {

		String outputName = Parameters.OUTPUT_PATH + projectName + fileName + ".csv";

		try (FileWriter fileWriter = new FileWriter(outputName)) {
			StringBuilder outputBuilder = new StringBuilder("Index;Date;Message\n");

			for (GitCommit c : commits) {
				outputBuilder.append(c.getId() + ";" + c.getDate() + ";" + c.getMessage() + "\n");
			}
			fileWriter.append(outputBuilder.toString());
		} catch (Exception e) {
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * [DEBUG] Salva su un .csv i dati dei JiraTicket
	 */
	public static void writeTicketOnCsv(List<JiraTicket> tickets, String projectName, String fileName) {

		String outputName = Parameters.OUTPUT_PATH + projectName + fileName + ".csv";

		try (FileWriter fileWriter = new FileWriter(outputName)) {
			StringBuilder outputBuilder = new StringBuilder(
					"Name;CreationDate;ResolutionDate;AffectedVersions;InjectedVersion;FixedVersion\n");

			for (JiraTicket t : tickets) {
				outputBuilder.append(t.getName() + ";" + t.getCreationDate() + ";" + t.getResolutionDate() + ";");
				for (JiraRelease j : t.getAffectedVersions()) {
					outputBuilder.append(j.getName() + " ");
				}
				outputBuilder.append(";");
				outputBuilder.append(t.getIv().getName() + ";");
				outputBuilder.append(t.getFv().getName() + " ");
				outputBuilder.append("\n");
			}
			fileWriter.append(outputBuilder.toString());

		} catch (Exception e) {
			Logger logger = Logger.getLogger(CSVHandler.class.getName());
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * Crea il Dataset scrivendo su un file .csv la lista delle classi con le
	 * relative metriche.
	 */
	public static void writeClassOnCSV(List<ProjectClass> classes, String projectName, String fileName) {
		Logger logger = Logger.getLogger(CSVHandler.class.getName());

		try (FileWriter fileWriter = new FileWriter(Parameters.OUTPUT_PATH + projectName + fileName)) {
			StringBuilder outputBuilder = new StringBuilder(Parameters.CSV_HEADER);

			for (ProjectClass c : classes) {
				Metrics metrics = c.getMetrics();
				outputBuilder.append(c.getRelease().getId() + ";" + c.getRelease().getName() + ";" + c.getPath() + ";"
						+ metrics.getSize() + ";" + metrics.getLocTouched() + ";" + metrics.getAvgLocAdded() + ";"
						+ metrics.getLocAdded() + ";" + metrics.getMaxLocAdded() + ";" + metrics.getChgSetSize() + ";" 
						+ metrics.getMaxChgSetSize() + ";" + metrics.getAvgChgSetSize() + ";" + metrics.getNumberRevisions() + ";"
						+ metrics.getNumberBugFixes() + ";" + metrics.getnAuth() + ";" + metrics.getAge() + ";"
						+ c.isBuggy());
				outputBuilder.append("\n");
			}
			fileWriter.append(outputBuilder.toString());

		} catch (Exception e) {
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * Crea un file .csv dove le colonne sono separate tramite "," così da
	 * evidenziare gli attributi per Weka e creare un ARFF corretto
	 */
	public static void writeCSVForWeka(List<ProjectClass> classes, String projectName, String fileName) {
		Logger logger = Logger.getLogger(CSVHandler.class.getName());

		try (FileWriter fileWriter = new FileWriter(Parameters.OUTPUT_PATH + projectName + fileName)) {
			StringBuilder outputBuilder = new StringBuilder(Parameters.CSV_HEADER_WEKA);

			for (ProjectClass c : classes) {
				Metrics metrics = c.getMetrics();
				outputBuilder.append(c.getRelease().getId() + "," + c.getRelease().getName() + "," + c.getPath() + ","
						+ metrics.getSize() + "," + metrics.getLocTouched() + "," + metrics.getAvgLocAdded() + ","
						+ metrics.getLocAdded() + "," + metrics.getMaxLocAdded() + "," + metrics.getChgSetSize() + "," 
						+ metrics.getMaxChgSetSize() + "," + metrics.getAvgChgSetSize() + "," + metrics.getNumberRevisions() + ","
						+ metrics.getNumberBugFixes() + "," + metrics.getnAuth() +  "," + metrics.getAge()+ "," + c.isBuggy());
				outputBuilder.append("\n");
			}
			fileWriter.append(outputBuilder.toString());

		} catch (Exception e) {
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * Scrive su un file .csv i risultati ottenuti tramite Weka.
	 */
	public static void writeResultOnCSV(List<WekaMetrics> results, String projectName, String fileName) {
		Logger logger = Logger.getLogger(CSVHandler.class.getName());

		try (FileWriter fileWriter = new FileWriter(Parameters.OUTPUT_PATH + projectName + fileName)) {
			StringBuilder outputBuilder = new StringBuilder(
					"Progetto;#Training Release;%Training;%Buggy in training;%Buggy in test;Classifier;Feature selection;"
							+ "Sensitivity;Balancing;TP;FP;TN;FN;Precision;Recall;Area Under ROC;Kappa\n");
			for (WekaMetrics r : results) {
				if (r.isMean()) {
					outputBuilder.append("MEAN;" + ";" + ";" + ";" + ";" + ";" + ";" + ";" + ";");
				} else {
					outputBuilder.append(projectName + ";" + r.getNumTrainingRelease() + ";"
							+ String.format(Locale.US, "%.2f", r.getPercentageTraining()) + ";"
							+ String.format(Locale.US, "%.2f", r.getPercentageBuggyInTraining()) + ";"
							+ String.format(Locale.US, "%.2f", r.getPercentageBuggyInTesting()) + ";"
							+ r.getClassifierName() + ";" + r.getFeatureSelectionName() + ";"
							+ r.getCostSensitivityName() + ";"
							+ r.getResamplingMethodName() + ";");
				}

				outputBuilder.append(
						String.format(Locale.US, "%.2f", r.getTP()) + ";" 
						+ String.format(Locale.US, "%.2f", r.getFP()) + ";" 
						+ String.format(Locale.US, "%.2f", r.getTN()) + ";" 
						+ String.format(Locale.US, "%.2f", r.getFN()) + ";" 
						+ String.format(Locale.US, "%.2f", r.getPrecision()) + ";"
						+ String.format(Locale.US, "%.2f", r.getRecall()) + ";"
						+ String.format(Locale.US, "%.2f", r.getAuc()) + ";"
						+ String.format(Locale.US, "%.2f", r.getKappa()));
				outputBuilder.append("\n");
				if (r.isMean()) {
					outputBuilder.append(";\n");
				}
			}
			fileWriter.append(outputBuilder.toString());

		} catch (Exception e) {
			logger.log(Level.SEVERE, Parameters.CSV_ERROR, e);
		}
	}

	/**
	 * Converte un file CSV in un file ARFF da fornire come dataset a Weka.
	 */
	public static void convertCSVtoARFF(String fileName) {
		Logger logger = Logger.getLogger(CSVHandler.class.getName());
		File file = new File(fileName.replace(Parameters.WEKA_CSV, Parameters.DATASET_ARFF));
		if(file.exists()) {
			logger.log(Level.INFO, "ARFF File already exists. Opening...");
			return;
		}
		try {
			logger.log(Level.INFO, "No ARFF File Found. Converting CSV Dataset...");
			Instances data = loadFileCSV(fileName);
			ArffSaver saver = new ArffSaver();
			saver.setInstances(data);
			saver.setFile(file);
			saver.writeBatch();
			logger.log(Level.INFO, "ARFF written succesfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Carica il file CSV contenente il dataset ed elimina le colonne inutili alla
	 * stima (feature selection preventiva) L'ID della release viene eliminato
	 * successivamente perchè necessario nell'iterazione di walk forward
	 */
	private static Instances loadFileCSV(String fileName) {
		CSVLoader loader = new CSVLoader();
		Instances data = null;
		int index = 0;
		try {
			loader.setSource(new File(fileName));
			data = loader.getDataSet();

			// Elimino le colonne relative al nome della versione e al nome del file
			index = data.attribute("VersionName").index();
			data.deleteAttributeAt(index);
			index = data.attribute("Path").index();
			data.deleteAttributeAt(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	
	/**
	 * Carica il file ARFF contenente il dataset
	 */
	public static Instances loadFileARFF(String fileName) {
		DataSource source;
		Instances data = null;
		try {
			source = new DataSource(fileName);
			data = source.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Cancella il file CSV temporaneo utilizzato per generare il dataset ARFF
	 */
	public static void deleteTempCSV(String projectName) {
		// Cancello il file csv temporaneo utilizzato per la conversione in ARFF
		File file = new File(Parameters.OUTPUT_PATH + projectName + Parameters.WEKA_CSV);
		if(file.exists()) {
			file.deleteOnExit();
		}
	}
}
