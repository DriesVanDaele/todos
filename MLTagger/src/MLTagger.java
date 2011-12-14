import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.transformation.LabelPowerset;
import mulan.data.MultiLabelInstances;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;


public class MLTagger {
	
	public static void main(String[] args) throws Exception {
		// collect the filenames from the arguments	
		String arffFilename = Utils.getOption("arff", args);
		String xmlFilename = Utils.getOption("xml", args);
		String unlabeledFilename = Utils.getOption("unlabeled", args);
		
		MLTagger tagger = new MLTagger(arffFilename, xmlFilename, unlabeledFilename);
		List<MultiLabelOutput> predictions = tagger.tag(arffFilename, xmlFilename, unlabeledFilename);
		
		// Temporary: print the output
		for (MultiLabelOutput prediction : predictions) {   
			System.out.println(prediction);
		}
	}
	
	public MLTagger(String arffFilename, String xmlFilename, String unlabeledFilename) {
		// Default constructor
	}
	
	public List<MultiLabelOutput> tag(String arffFilename, String xmlFilename, String unlabeledFilename) throws InvalidDataException, ModelInitializationException, Exception {
		// Loading the data 
		MultiLabelInstances dataset = new MultiLabelInstances(arffFilename, xmlFilename);
		
		// Create an instance of the learner of choice (here the RAkEL algorithm) 
		RAkEL model = new RAkEL(new LabelPowerset(new J48()));
		
		// Train the classifier using the loaded data
		model.build(dataset);
		
		// Loading the unlabeledData
		Instances unlabeledData = getUnlabeledData(unlabeledFilename);
		
		// Perform a prediction for each instance in the dataset
		return getPredictions(model, unlabeledData);
	}
	
	private Instances getUnlabeledData(String unlabeledFilename) throws IOException {
		 // Load the unlabeled data instances (.arff) (content must conform data on which the training dataset was built)
	     FileReader reader = new FileReader(unlabeledFilename);
	     Instances unlabeledData = new Instances(reader);
	     return unlabeledData;
	}
	
	private List<MultiLabelOutput> getPredictions(RAkEL model, Instances unlabeledData) throws InvalidDataException, ModelInitializationException, Exception {
		 List<MultiLabelOutput> predictions = new ArrayList<MultiLabelOutput>();
	     int numInstances = unlabeledData.numInstances();
	     for (int instanceIndex = 0; instanceIndex < numInstances; instanceIndex++) {	     		
	           Instance instance = unlabeledData.instance(instanceIndex);
	           predictions.add(model.makePrediction(instance));	           
	     }
	     return predictions;
	}
}
