package weka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;


public class WekaProject {
	private static final String VERSIONID = "VersionID";
	ArrayList<String> classifiers;
	ArrayList<String> resamplingMethods;
	ArrayList<String> featureSelectionMethods;
	private String name;
	private Instances dataset;
	
	public WekaProject(String projectName) {
		this.classifiers = new ArrayList<>(Arrays.asList("Random Forest", "Naive Bayes", "IBk"));
		this.resamplingMethods = new ArrayList<>(Arrays.asList("no resample", "Oversampling", "Undersampling", "Smote"));
		this.featureSelectionMethods = new ArrayList<>(Arrays.asList("no feature selection", "Best First"));
		this.setName(projectName);
		
	}
	
	
	/**
	 * Esegue tutte le iterazioni necessarie di Walk Forward in base al numero di release presenti nel dataset
	 */
	public List<WekaResult> runWalkForward() {
		int releasesNumber = getReleasesNumber(getDataset());
		List<WekaResult> resultList = new ArrayList<>();
		
		//per ogni classificatore, per ogni metodo di feature selection, per ogni metodo di balancing, per ogni iterazione di walk forward
		//viene salvato il risultato
		for(String classifierName : this.classifiers) {
			for(String featureSelectionName : this.featureSelectionMethods) {
				for(String resamplingMethodName : this.resamplingMethods) {	
					//con walk-forward partiamo dalla seconda release come test set perche non abbiamo un training set per la prima
					//terminiamo con l'ultima release come test set che avra tutte le precedenti come training set
					WekaResult mean = new WekaResult(classifierName, featureSelectionName, resamplingMethodName);
					for(int i = 2; i < releasesNumber+1; i++) {
						WekaResult result = new WekaResult(classifierName, featureSelectionName, resamplingMethodName);
						Instances[] trainTest = splitTrainingTestSet(getDataset(), i);
						runWalkForwardIteration(trainTest, result, i);
						resultList.add(result);
						
						mean.setTotalValues(result);
					}
					mean.calculateMean(releasesNumber-1);
					resultList.add(mean);
					
				}
			}
		}
		return resultList;
	}
	
	/**
	 * Ottiene il numero delle release, che corrisponde all'ID della release dell'ultima istanza del dataset
	 * */
	public int getReleasesNumber(Instances data) {
		Instance instance = data.lastInstance();
		int index = data.attribute(VERSIONID).index();
		return (int)instance.value(index);
	}
	
	/**
	 * Effettua lo split del dataset in training e test set in funzione della release che si usa come test set nel walk-forward
	 * */
	public Instances[] splitTrainingTestSet(Instances data, int testReleaseIndex) {
		Instances[] trainTest = new Instances[2];
		
		//creiamo due dataset vuoti con la stessa intestazione del dataset di partenza
		Instances trainingSet = new Instances(data,0);
		Instances testSet = new Instances(data,0);
		
		//per ogni istanza, se la release e' precedente a quella da usare come test set allora
		//si aggiunge quell'istanza al training set, altrimenti, se e' uguale, la aggiungo al test set
		int index = data.attribute(VERSIONID).index();
		for(Instance i : data) {
			if((int)i.value(index) < testReleaseIndex) {
				trainingSet.add(i);
			}else if((int)i.value(index) == testReleaseIndex) {
				testSet.add(i);
			}
		}
		trainTest[0] = trainingSet;
		trainTest[1] = testSet;
		return trainTest;
	}
	
	/**
	 * Esegue un'iterazione di Walk Forward
	 * */
	public void runWalkForwardIteration(Instances[] trainTest, WekaResult result, int iterationIndex) {
		Instances trainingSet = trainTest[0];
		Instances testSet = trainTest[1];
		
		//rimuove dal dataset la feature relativa all'id della release, necessaria solo per splittare il dataset
		int index = trainingSet.attribute(VERSIONID).index();
		trainingSet.deleteAttributeAt(index);
		testSet.deleteAttributeAt(index);
		
		//setta la feature da predirre, ovvero la buggyness
		trainingSet.setClassIndex(trainingSet.numAttributes()-1);
		testSet.setClassIndex(trainingSet.numAttributes()-1);
		
		//istanziamo gli oggetti da utilizzare in questa iterazione
		AbstractClassifier classifier = null;
		AttributeSelection featureSelection = null;
		Filter resamplingMethod = null;
		
		//otteniamo il classificatore
		switch(result.getClassifierName()) {
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
		
		//applichiamo il metodo di balancing
		try {
			switch(result.getResamplingMethodName()) {
				case "Undersampling":										
					resamplingMethod = new SpreadSubsample();
					resamplingMethod.setInputFormat(trainingSet);
					
					String[] opts = new String[]{ "-M", "1.0"};			
					resamplingMethod.setOptions(opts);
					
					trainingSet = Filter.useFilter(trainingSet, resamplingMethod);
					break;
					
				case "Oversampling":
					resamplingMethod = new Resample();
					resamplingMethod.setInputFormat(trainingSet);
					
					//Trovo qual è la classe maggioritaria per le opzioni del filtro 
					int trainingSetSize = trainingSet.size();
					int numInstancesTrue = getNumInstancesTrue(trainingSet);
					double percentageTrue = (double)(numInstancesTrue)/(double)(trainingSetSize)*100.0;
					double percentageMajorityClass = 0;
					if(percentageTrue > 50) {
						percentageMajorityClass = percentageTrue;
					} 
					else {
						percentageMajorityClass = 100 - percentageTrue;
					}
					
					String doublePercentageMajorityClassString = String.valueOf(percentageMajorityClass*2);
					// -Z = la dimensione finale del dataset /2*majorityClasses)
					opts = new String[]{ "-B", "1.0", "-Z", doublePercentageMajorityClassString};			
					resamplingMethod.setOptions(opts);
					
					trainingSet = Filter.useFilter(trainingSet, resamplingMethod);
					break;
					
				case "Smote":
					resamplingMethod = new SMOTE();
					double parameter = 0;
					numInstancesTrue = getNumInstancesTrue(trainingSet);
					int numInstancesFalse = trainingSet.numInstances()-numInstancesTrue;
					
					if(numInstancesTrue < numInstancesFalse && numInstancesTrue != 0) {
						parameter = ((double)numInstancesFalse-(double)numInstancesTrue)/(double)numInstancesTrue*100.0;
					} 
					else if (numInstancesTrue >= numInstancesFalse && numInstancesFalse != 0){
						parameter = ((double)numInstancesTrue-(double)numInstancesFalse)/(double)numInstancesFalse*100.0;
					}
					
					// -P = specifichiamo la percentuale delle istanze della classe minoritaria da creare per bilanciare il dataset
					// Default = 100%  [ raddoppia le istanze minoritarie ] 
					opts = new String[] {"-P", String.valueOf(parameter)};
					resamplingMethod.setOptions(opts);
					resamplingMethod.setInputFormat(trainingSet);
					
					trainingSet = Filter.useFilter(trainingSet, resamplingMethod);	
					break;
				
				case "No resampling":
					break;
				
				default:
					break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
		//otteniamo il metodo di feature selection
			if(result.getFeatureSelectionName().equalsIgnoreCase("Best First")) {
				//create AttributeSelection object
				featureSelection = new AttributeSelection();
				//create evaluator and search algorithm objects
				CfsSubsetEval eval = new CfsSubsetEval();
				GreedyStepwise search = new GreedyStepwise();
				//set the algorithm to search backward
				search.setSearchBackwards(true);
				//set the filter to use the evaluator and search algorithm
				featureSelection.setEvaluator(eval);
				featureSelection.setSearch(search);
				//specify the dataset
				featureSelection.setInputFormat(trainingSet);
				//apply
				trainingSet = Filter.useFilter(trainingSet, featureSelection);
				testSet = Filter.useFilter(testSet, featureSelection);
				int numAttrFiltered = trainingSet.numAttributes();
				trainingSet.setClassIndex(numAttrFiltered - 1);
				testSet.setClassIndex(numAttrFiltered - 1);
			}
		 
			//salvo le informazioni relative al numero di release in training e alla percentuale di bugginess nel training e nel test set
			result.setDatasetValues(trainingSet, testSet, iterationIndex);
		
			//addestro il classificatore con il training set
			if(classifier != null)
				classifier.buildClassifier(trainingSet);
			
			//effettuo la predizione sul test set
			Evaluation eval = new Evaluation(testSet);
			eval.evaluateModel(classifier, testSet);
			
			//salvo i risultati nell'oggetto result
			result.setValues(eval, getPositiveClassIndex());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Ottiene l'indice della classe da considerare come "positiva" nella stima
	 * */
	public int getPositiveClassIndex() {
		int index = 0;
		int positiveIndex = 0;
		//recupero l'indice della buggyness pari a true
		Enumeration<Object> values = this.dataset.attribute(this.dataset.numAttributes()-1).enumerateValues();
		while(values.hasMoreElements()) {
			Object v = values.nextElement();
			if (((String)v).equalsIgnoreCase("true")) {
				positiveIndex = index;
				break;
			} 
			index = index + 1;
		}
		return positiveIndex;
	}
	
	/**
	 * Ottiene il numero di istanze con buggyness pari a true
	 * */
	private int getNumInstancesTrue(Instances dataset) {
		int numInstancesTrue = 0;
		int buggyIndex = dataset.classIndex();
		for(Instance instance:dataset) {
			if(instance.stringValue(buggyIndex).equalsIgnoreCase("true")) {
				numInstancesTrue = numInstancesTrue + 1;
			}
		}
		return numInstancesTrue;
	}
	
	
	/*===============================================================================================
	 * Getters & Setters
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
