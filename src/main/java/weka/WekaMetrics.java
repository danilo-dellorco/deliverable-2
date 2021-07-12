package weka;

import utils.Parameters;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instance;
import weka.core.Instances;


public class WekaMetrics {
	double tp;
	double fp;
	double tn;
	double fn;
	double precision;
	double recall;
	double auc;
	double kappa;
	
	private boolean mean;	// Specifica se il risultato è relativo alla media.
	
	private String projName;
	private int numTrainingRelease;
	private double percentageTraining;
	private double percentageBuggyInTraining;
	private double percentageBuggyInTesting;
	
	private String classifierName;
	private String featureSelectionName;
	private String resamplingMethodName;
	private String costSensitivityName;
	
	public WekaMetrics(String classifierName, String featureSelectionName, String resamplingMethodName, String costSensitivityName) {
		super();
		this.setClassifierName(classifierName);
		this.setFeatureSelectionName(featureSelectionName);
		this.setResamplingMethodName(resamplingMethodName);
		this.setCostSensitivityName(costSensitivityName);
	}

	
	/**
	 * Salva le informazioni del dataset.
	 * Imposta il numero di release in training e la percentuale di buggyness nel training e nel test set
	 * */
	public void setDatasetValues(Instances training, Instances test, int testReleaseIndex) {
		this.numTrainingRelease = testReleaseIndex - 1;
		
		int numInstancesTraining = training.numInstances();
		int numInstancesTest = test.numInstances();
		this.percentageTraining = (double)numInstancesTraining/(double)(numInstancesTraining+numInstancesTest);
		
		int numBuggyTraining = 0;
		int numFeatures = training.numAttributes();
		for(Instance instance: training) {
			if( ((String)instance.stringValue(numFeatures-1)).equalsIgnoreCase("true")) {
				numBuggyTraining = numBuggyTraining + 1;
			}
		}
		
		int numBuggyTest = 0;
		for(Instance instance: test) {
			if( ((String)instance.stringValue(numFeatures-1)).equalsIgnoreCase("true")) {
				numBuggyTest = numBuggyTest + 1;
			}
		}
		this.percentageBuggyInTraining = (double)numBuggyTraining/(double)numInstancesTraining;
		this.percentageBuggyInTesting = (double)numBuggyTest/(double)numInstancesTest;
	}
	
	/**
	 * Imposta i valori delle varie metriche usate per il performance evaluation.
	 * Nei casi limiti si seguono le indicazioni qui ripostate https://github.com/dice-group/gerbil/wiki/Precision,-Recall-and-F1-measure
	 * */
	public void setValues(Evaluation eval, int positiveClassIndex) {
		tp = eval.numTruePositives(positiveClassIndex);
		fp = eval.numFalsePositives(positiveClassIndex);
		tn = eval.numTrueNegatives(positiveClassIndex);
		fn = eval.numFalseNegatives(positiveClassIndex);
		if(tp == 0 && fp == 0 && fn == 0) {
			this.precision = 1;
			this.recall = 1;
		} else if (fp == 0 && tn == 0) {
			this.auc = 0;
			this.precision = eval.precision(positiveClassIndex);
			this.recall = eval.recall(positiveClassIndex);
			this.kappa = eval.kappa();
		} else if (tp == 0 && (fp > 0 || fn > 0)) {
			this.precision = 0;
			this.recall = 0;
		} else {
			this.precision = eval.precision(positiveClassIndex);
			this.recall = eval.recall(positiveClassIndex);
			this.auc = eval.areaUnderROC(positiveClassIndex);
			this.kappa = eval.kappa();
		}
	}
	
	public String toString() {
		String str = "";
		str = str + "TP: " + tp + " - FP: "+ fp + "\nFN: "+ fn + " - TN: " + tn+ "\n"
				+ "precision: " + precision +"\nrecall: " + recall + "\nauc: "+ auc +"\nkappa: "+ kappa;
		return str;
	}
	
	public void setTotalValues(WekaMetrics result) {
		this.tp = this.tp + result.getTP();
		this.tn = this.tn + result.getTN();
		this.fp = this.fp + result.getFP();
		this.fn = this.fn + result.getFN();
		this.kappa = this.kappa + result.getKappa();
		this.recall = this.recall + result.getRecall();
		this.precision = this.precision + result.getPrecision();
		this.auc = this.auc + result.getAuc();
	}
	
	/**
	 * Calcola la media di tutte le metriche utilizzate
	 */
	public void calculateMean(double releases) {
		tp = tp/releases;
		fp = fp/releases;
		fn = fn/releases;
		tn = tn/releases;
		kappa = kappa/releases;
		recall = recall/releases;
		precision = precision/releases;
		auc = auc/releases;
		this.mean = true;
	}
	
	public static CostMatrix getCostMatrix() {
		
		CostMatrix costMatrix = new CostMatrix(2);
		costMatrix.setElement(0, 0, 0);
		costMatrix.setElement(0, 1, Parameters.FALSE_NEGATIVE_COST);
		costMatrix.setElement(1, 0, Parameters.FALSE_POSITIVE_COST);
		costMatrix.setElement(1, 1, 0);
				
		return costMatrix;
	}
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	
	public boolean isMean() {
		return this.mean;
	}
	
	public double getTP() {
		return tp;
	}

	public void setTP(double tP) {
		tp = tP;
	}

	public double getFP() {
		return fp;
	}

	public void setFP(double fP) {
		fp = fP;
	}

	public double getTN() {
		return tn;
	}

	public void setTN(double tN) {
		tn = tN;
	}

	public double getFN() {
		return fn;
	}

	public void setFN(double fN) {
		fn = fN;
	}

	public double getPrecision() {
		return precision;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public double getRecall() {
		return recall;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public double getAuc() {
		return auc;
	}

	public void setAuc(double auc) {
		this.auc = auc;
	}

	public double getKappa() {
		return kappa;
	}

	public void setKappa(double kappa) {
		this.kappa = kappa;
	}
	
	public String getProjName() {
		return projName;
	}

	public void setProjName(String projName) {
		this.projName = projName;
	}

	public int getNumTrainingRelease() {
		return numTrainingRelease;
	}

	public void setNumTrainingRelease(int numTrainingRelease) {
		this.numTrainingRelease = numTrainingRelease;
	}

	public double getPercentageTraining() {
		return percentageTraining;
	}

	public void setPercentageTraining(double percentageTraining) {
		this.percentageTraining = percentageTraining;
	}

	public double getPercentageBuggyInTraining() {
		return percentageBuggyInTraining;
	}

	public void setPercentageBuggyInTraining(double percentageBuggyInTraining) {
		this.percentageBuggyInTraining = percentageBuggyInTraining;
	}

	public double getPercentageBuggyInTesting() {
		return percentageBuggyInTesting;
	}

	public void setPercentageBuggyInTesting(double percentageBuggyInTesting) {
		this.percentageBuggyInTesting = percentageBuggyInTesting;
	}
	
	public String getFeatureSelectionName() {
		return featureSelectionName;
	}

	public void setFeatureSelectionName(String featureSelectionName) {
		this.featureSelectionName = featureSelectionName;
	}

	public String getResamplingMethodName() {
		return resamplingMethodName;
	}

	public void setResamplingMethodName(String resamplingMethodName) {
		this.resamplingMethodName = resamplingMethodName;
	}

	public String getClassifierName() {
		return classifierName;
	}

	public void setClassifierName(String classifierName) {
		this.classifierName = classifierName;
	}

	public String getCostSensitivityName() {
		return costSensitivityName;
	}

	public void setCostSensitivityName(String costSensitivity) {
		this.costSensitivityName = costSensitivity;
	}
	
	
}
