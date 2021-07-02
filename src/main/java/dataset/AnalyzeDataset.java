package dataset;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.CSVHandler;
import utils.Parameters;
import weka.WekaAPI;
import weka.WekaMetrics;
import weka.core.Instances;


public class AnalyzeDataset {
	
	public void analyze(String projectName) {		
		Logger logger = Logger.getLogger(AnalyzeDataset.class.getName());
		logger.log(Level.INFO,"Load dataset...");
		
		WekaAPI weka = new WekaAPI(projectName);
		
		//conversione del file CSV in ARFF e caricamento del dataset
		CSVHandler.convertCSVtoARFF(Parameters.OUTPUT_PATH + projectName + Parameters.WEKA_CSV);
		Instances data = CSVHandler.loadFileARFF(Parameters.OUTPUT_PATH + projectName + Parameters.DATASET_ARFF);
		weka.setDataset(data);
		
		//cancellazione del file CSV temporaneo usato per creare l'ARFF
		File file = new File(Parameters.OUTPUT_PATH + projectName + Parameters.WEKA_CSV);
		if(file.exists()) {
			file.deleteOnExit();
		}
		
		//esecuzione di walk forward per tutti i tipi di classificatori, metodi di feature selection e metodi di balancing
		logger.log(Level.INFO,"Computing results...");
		List<WekaMetrics> resultList = weka.runWalkForward();
		
		//scrittura dei risultati su un file CSV		
		logger.log(Level.INFO,"Writing CSV...");
		CSVHandler.writeResultOnCSV(resultList, projectName, Parameters.RESULT_CSV);
		
		logger.log(Level.INFO,"CSV written successfully.\nEnd of the program.");
	}
	
	public static void main(String[] args) {
		AnalyzeDataset analyzer = new AnalyzeDataset();
		analyzer.analyze(Parameters.BOOKKEEPER);
	}
}
