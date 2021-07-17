package dataset;

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
		
		// Carico il dataset all'interno di Weka
		CSVHandler.convertCSVtoARFF(Parameters.OUTPUT_PATH + projectName + Parameters.WEKA_CSV);
		CSVHandler.deleteTempCSV(projectName);
		Instances dataset = CSVHandler.loadFileARFF(Parameters.OUTPUT_PATH + projectName + Parameters.DATASET_ARFF);
		weka.setDataset(dataset);
		
		// Eseguo Walk Forward per ogni metodo di Feature Selection, Balancing, Sensitivity
		logger.log(Level.INFO,"Computing results...");
		List<WekaMetrics> resultList = weka.runWalkForward();
		
		// Scrivo i risultati dell'analisi all'interno di un file CSV		
		logger.log(Level.INFO,"Writing analysis report on CSV...");
		CSVHandler.writeResultOnCSV(resultList, projectName, Parameters.RESULT_CSV);
		
		logger.log(Level.INFO,"Dataset analysis completed.\nEnd of the program.");
	}
	
	public static void main(String[] args) {
		AnalyzeDataset analyzer = new AnalyzeDataset();
		analyzer.analyze(Parameters.BOOKKEEPER);
		analyzer.analyze(Parameters.SYNCOPE);
	}
}
