import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MLTagger {
	
    public static void main(String[] args) throws Exception {
	// collect the filenames from the arguments	
	String arffFilename = Utils.getOption("arff", args);
	String xmlFilename = Utils.getOption("xml", args);
	String unlabeledFilename = Utils.getOption("unlabeled", args);
	
	MLTagger tagger = new MLTagger();
	
	List<MultiLabelOutput> predictions = tagger.tag(arffFilename, xmlFilename, unlabeledFilename, false);
	
	// Temporary: print the output
	for (MultiLabelOutput prediction : predictions) {   
	    System.out.println(prediction);
	}
    }
	
    public MLTagger() {
		// Default constructor
    }
	
    public List<MultiLabelOutput> tag(String arffFilename, String xmlFilename, String unlabeledFilename, boolean isBuildingModel) throws InvalidDataException, ModelInitializationException, Exception {
	// Loading the data 
	MultiLabelInstances dataset = new MultiLabelInstances(arffFilename, xmlFilename);
	
	RAkEL model;
	
	if (isBuildingModel) {
	    // Create an instance of the learner of choice (here the RAkEL algorithm with J48) 
	    model = new RAkEL(new LabelPowerset(new J48()));
	    
	    // Train the classifier using the loaded data
	    model.build(dataset);
	    
	    // Save the learner to a file
	    writeRAkELToFile(model, "model.bin");
	    //writeLearnerToFile(model.makeCopy(), "/home/dries/lalalalalaaltemp.bin");	
	    
	} else {
	    model = readRAkELFromFile("model.bin");
	    //model = new RAkEL(readLearnerFromFile("/home/dries/lalalalalaaltemp.bin"));
	}
	
	// Loading the unlabeledData
	Instances unlabeledData = getUnlabeledData(unlabeledFilename);
	MultiLabelInstances multiUnlabeledData = new MultiLabelInstances(unlabeledData, xmlFilename);
	
	// Print predictions (model.makePrediction results)
	int numInstances = multiUnlabeledData.getNumInstances();
	for (int instanceIndex = 0; instanceIndex < numInstances; instanceIndex++) {
            Instance instance = multiUnlabeledData.getDataSet().instance(instanceIndex);
            MultiLabelOutput output = model.makePrediction(instance);
	    
            if (output.hasBipartition()) {
                String bipartion = Arrays.toString(output.getBipartition());
                System.out.println("Predicted bipartion: " + bipartion);
            }
            if (output.hasRanking()) {
                String ranking = Arrays.toString(output.getRanking());
                System.out.println("Predicted ranking: " + ranking);
            }
            if (output.hasConfidences()) {
                String confidences = Arrays.toString(output.getConfidences());
                System.out.println("Predicted confidences: " + confidences);
            }
	}
        
	// Perform a prediction for each instance in the dataset and return
	return getPredictions(model, unlabeledData);
    }
    
    private Instances getUnlabeledData(String unlabeledFilename) throws IOException {
	// Load the unlabeled data instances (.arff) (content must conform data on which the training dataset was built)
	FileReader reader = new FileReader(unlabeledFilename);
	return new Instances(reader);
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
	
//	private void writeLearnerToFile(MultiLabelLearner learner, String location) throws Exception {
//		ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(location)));
//		objectOut.writeObject(learner); // Write object
//		objectOut.close(); // Close the output stream
//    }
//		  
//	private MultiLabelLearner readLearnerFromFile(String location) throws Exception {
//		ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(location)));
//		MultiLabelLearner object = (MultiLabelLearner) objectIn.readObject();
//		objectIn.close();
//		return object;
//	}
	
    // Windows moet wss de slash in new File(...) omzetten naar backslash of iets gelijkaardigs
    private void writeRAkELToFile(RAkEL model, String name) throws Exception {
	File f1 = new File("./temp/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
	objectOut.writeObject(model); // Write object
	objectOut.close(); // Close the output stream
    }
			  
    // Windows moet wss de slash in new File(...) omzetten naar backslash of iets gelijkaardigs
    private RAkEL readRAkELFromFile(String name) throws Exception {
	File f1 = new File("./temp/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
	RAkEL object = (RAkEL) objectIn.readObject();
	objectIn.close();
	return object;
    }
}		
