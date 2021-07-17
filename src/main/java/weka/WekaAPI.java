package weka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import utils.Debug;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class WekaAPI {
	private static final String VERSIONID = "VersionID";
	ArrayList<String> classifiers;
	ArrayList<String> resamplingMethods;
	ArrayList<String> featureSelectionMethods;
	ArrayList<String> costSensitiveMethods;
	private String name;
	private Instances dataset;
	
	// Parametri di configurazione dell'iterazione
	AbstractClassifier classifier = null;
	AttributeSelection featureSelection = null;
	Filter resamplingMethod = null;
	CostSensitiveClassifier costSensitiveClassifier = null;

	public WekaAPI(String projectName) {
		this.classifiers = new ArrayList<>(Arrays.asList("Random Forest", "Naive Bayes", "IBk"));
		this.resamplingMethods = new ArrayList<>(
				Arrays.asList("no resample", "Oversampling", "Undersampling", "Smote"));
		this.featureSelectionMethods = new ArrayList<>(Arrays.asList("no feature selection", "Best First"));
		this.costSensitiveMethods = new ArrayList<>(Arrays.asList("no sensitive", "threshold", "sensitive learning"));
		this.setName(projectName);

	}

	/**
	 * Esegue tutte le iterazioni necessarie di Walk Forward in base al numero di
	 * release presenti nel dataset
	 */
	public List<WekaMetrics> runWalkForward() {
		int releasesNumber = getReleasesNumber(getDataset());
		List<WekaMetrics> resultList = new ArrayList<>();

		for (String classifierName : this.classifiers) {
			for (String featureSelectionName : this.featureSelectionMethods) {
				for (String resamplingMethodName : this.resamplingMethods) {
					for (String costSensitiveMethod : this.costSensitiveMethods) {
						Debug.printWekaRunConfiguration(classifierName, featureSelectionName, resamplingMethodName,
								costSensitiveMethod);
						WekaMetrics mean = new WekaMetrics(classifierName, featureSelectionName, resamplingMethodName,
								costSensitiveMethod);
						
						// Eseguo walk forward a partire dalla seconda release, in quanto la prima viene utilizzata
						// soltanto come Training Set
						for (int i = 2; i < releasesNumber; i++) {
							WekaMetrics result = new WekaMetrics(classifierName, featureSelectionName, resamplingMethodName, costSensitiveMethod);
							Instances[] trainTest = splitTrainingTestSet(getDataset(), i);
							runWalkForwardConfiguration(trainTest, result, i);
							resultList.add(result);
							mean.setTotalValues(result);
						}
						mean.calculateMean((double) releasesNumber - 2);
						resultList.add(mean);
					}
				}
			}
		}
		return resultList;
	}

	/**
	 * Ottiene il numero delle release, che corrisponde all'ID della release
	 * dell'ultima istanza del dataset
	 */
	public int getReleasesNumber(Instances data) {
		Instance instance = data.lastInstance();
		int index = data.attribute(VERSIONID).index();
		return (int) instance.value(index);
	}

	/**
	 * Effettua lo split del dataset in training e test set in funzione della
	 * release che si usa come test set nel walk-forward
	 */
	public Instances[] splitTrainingTestSet(Instances data, int testReleaseIndex) {
		Instances[] trainTest = new Instances[2];

		// Si inizializzano due dataset vuoti
		Instances trainingSet = new Instances(data, 0);
		Instances testSet = new Instances(data, 0);

		// Per ogni istanza si aggiunge la release al training set se è precedente all'iterazione corrente
		// Per ogni istanza si aggiunge la release al testing set se è uguale all'iterazione corrente
		int index = data.attribute(VERSIONID).index();
		for (Instance i : data) {
			if ((int) i.value(index) < testReleaseIndex) {
				trainingSet.add(i);
			} else if ((int) i.value(index) == testReleaseIndex) {
				testSet.add(i);
			}
		}
		trainTest[0] = trainingSet;
		trainTest[1] = testSet;
		return trainTest;
	}

	/**
	 * Esegue un'iterazione di Walk Forward
	 */
	public void runWalkForwardConfiguration(Instances[] trainTest, WekaMetrics metrics, int iterationIndex) {
		Instances trainingSet = trainTest[0];
		Instances testSet = trainTest[1];

		// Rimuove dal dataset la feature relativa all'ID della versione, utile soltanto per splittare il dataset
		int index = trainingSet.attribute(VERSIONID).index();
		trainingSet.deleteAttributeAt(index);
		testSet.deleteAttributeAt(index);

		// Setup della feature da predirre (buggyness)
		trainingSet.setClassIndex(trainingSet.numAttributes() - 1);
		testSet.setClassIndex(trainingSet.numAttributes() - 1);
		setupClassifier(metrics.getClassifierName());

		// Setup del metodo di balancing
		try {
			switch (metrics.getResamplingMethodName()) {
			case "Undersampling":
				resamplingMethod = new SpreadSubsample();
				resamplingMethod.setInputFormat(trainingSet);

				String[] opts = new String[] { "-M", "1.0" };
				resamplingMethod.setOptions(opts);

				trainingSet = Filter.useFilter(trainingSet, resamplingMethod);
				break;

			case "Oversampling":
				resamplingMethod = new Resample();
				resamplingMethod.setInputFormat(trainingSet);

				// Trovo qual è la classe maggioritaria per le opzioni del filtro
				int trainingSetSize = trainingSet.size();
				int numInstancesTrue = getNumInstancesTrue(trainingSet);
				double percentageTrue = (double) (numInstancesTrue) / (double) (trainingSetSize) * 100.0;
				double percentageMajorityClass = 0;
				if (percentageTrue > 50) {
					percentageMajorityClass = percentageTrue;
				} else {
					percentageMajorityClass = 100 - percentageTrue;
				}

				String doublePercentageMajorityClassString = String.valueOf(percentageMajorityClass * 2);
				// -Z = la dimensione finale del dataset /2*majorityClasses)
				opts = new String[] { "-B", "1.0", "-Z", doublePercentageMajorityClassString };
				resamplingMethod.setOptions(opts);

				trainingSet = Filter.useFilter(trainingSet, resamplingMethod);
				break;

			case "Smote":
				resamplingMethod = new SMOTE();
				double parameter = 0;
				numInstancesTrue = getNumInstancesTrue(trainingSet);
				int numInstancesFalse = trainingSet.numInstances() - numInstancesTrue;

				if (numInstancesTrue < numInstancesFalse && numInstancesTrue != 0) {
					parameter = ((double) numInstancesFalse - numInstancesTrue) / numInstancesTrue * 100.0;
				} else if (numInstancesTrue >= numInstancesFalse && numInstancesFalse != 0) {
					parameter = ((double) numInstancesTrue - numInstancesFalse) / numInstancesFalse * 100.0;
				}
				
				// Setup di smote, raddoppio le istanze minoritarie con -P 100
				opts = new String[] { "-P 100", String.valueOf(parameter) };
				resamplingMethod.setOptions(opts);
				resamplingMethod.setInputFormat(trainingSet);

				trainingSet = Filter.useFilter(trainingSet, resamplingMethod);
				break;

			case "No resampling":
				break;

			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Setup della Sensitivity
		switch (metrics.getCostSensitivityName()) {
		case "no sensitive":
			break;

		case "threshold":
			costSensitiveClassifier = new CostSensitiveClassifier();
			costSensitiveClassifier.setCostMatrix(WekaMetrics.getCostMatrix());
			costSensitiveClassifier.setClassifier(classifier);
			costSensitiveClassifier.setMinimizeExpectedCost(true);
			break;

		case "sensitive learning":
			costSensitiveClassifier = new CostSensitiveClassifier();
			costSensitiveClassifier.setCostMatrix(WekaMetrics.getCostMatrix());
			costSensitiveClassifier.setClassifier(classifier);
			costSensitiveClassifier.setMinimizeExpectedCost(false);
			break;
			
		default:
			// Default
		}

		
		// Setup del metodo di Feature Selection
		try {
			if (metrics.getFeatureSelectionName().equalsIgnoreCase("Best First")) {
				featureSelection = new AttributeSelection();
				CfsSubsetEval eval = new CfsSubsetEval();
				GreedyStepwise search = new GreedyStepwise();
				search.setSearchBackwards(true);
				featureSelection.setEvaluator(eval);
				featureSelection.setSearch(search);
				featureSelection.setInputFormat(trainingSet);
				trainingSet = Filter.useFilter(trainingSet, featureSelection);
				testSet = Filter.useFilter(testSet, featureSelection);
				int numAttrFiltered = trainingSet.numAttributes();
				trainingSet.setClassIndex(numAttrFiltered - 1);
				testSet.setClassIndex(numAttrFiltered - 1);
			}

			// Salvo le informazioni sul numero di release nel training set
			// e sulla percentuale di bugginess nel training e nel test set
			metrics.setDatasetValues(trainingSet, testSet, iterationIndex);

			Evaluation eval = new Evaluation(testSet);
			
			// Addestro il classificatore utilizzando il training set, e ne valuto il comportamento
			// tramite il testing set
			if (costSensitiveClassifier != null) {
				costSensitiveClassifier.buildClassifier(trainingSet);
				eval.evaluateModel(costSensitiveClassifier, testSet);
			} else if (classifier!=null) {
				classifier.buildClassifier(trainingSet);
				eval.evaluateModel(classifier, testSet);
			}

			// Salvo tutti i risultati ottenuti all'interno dell'oggetto metrics.
			metrics.setValues(eval, getPositiveClassIndex());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Ottiene l'indice della classe da considerare come "positiva" nella stima
	 */
	public int getPositiveClassIndex() {
		int index = 0;
		int positiveIndex = 0;
		// recupero l'indice della buggyness pari a true
		Enumeration<Object> values = this.dataset.attribute(this.dataset.numAttributes() - 1).enumerateValues();
		while (values.hasMoreElements()) {
			Object v = values.nextElement();
			if (((String) v).equalsIgnoreCase("true")) {
				positiveIndex = index;
				break;
			}
			index = index + 1;
		}
		return positiveIndex;
	}

	/**
	 * Ottiene il numero di istanze con buggyness pari a true
	 */
	private int getNumInstancesTrue(Instances dataset) {
		int numInstancesTrue = 0;
		int buggyIndex = dataset.classIndex();
		for (Instance instance : dataset) {
			if (instance.stringValue(buggyIndex).equalsIgnoreCase("true")) {
				numInstancesTrue = numInstancesTrue + 1;
			}
		}
		return numInstancesTrue;
	}

	
	/**
	 * Effettua il setup del classificatore in base all'iterazione corrente.
	 */
	public void setupClassifier(String classifierName) {
		switch (classifierName) {
		case "Random Forest":
			classifier = new RandomForest();
			break;

		case "Naive Bayes":
			classifier = new NaiveBayes();
			break;

		case "IBk":
			classifier = new IBk();
			break;

		default:
			break;
		}
	}
	
	
	/*
	 * =============================================================================
	 * ================== Getters & Setters
	 */

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instances getDataset() {
		return dataset;
	}

	public void setDataset(Instances dataset) {
		this.dataset = dataset;
	}
}
