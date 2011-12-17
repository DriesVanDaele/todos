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
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
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
		boolean isSingleLineInput = false;  
		boolean isBuildingModel = false; 
			
		// collect the filenames from the arguments

		if (Utils.getFlag("i", args)) { isSingleLineInput = true; }
		if (Utils.getFlag("b", args)) { isBuildingModel = true; }
		
	    String text = Utils.getOption("text", args);
		String arffFilename = Utils.getOption("arff", args);
		String xmlFilename = Utils.getOption("xml", args);
		String unlabeledFilename = Utils.getOption("unlabeled", args);
		
		
		// testing, clean up later
		List<MultiLabelOutput> predictions = new MLTagger().tag(text, arffFilename, xmlFilename, unlabeledFilename, isSingleLineInput, isBuildingModel);
		
		// Temporary: print the output
		for (MultiLabelOutput prediction : predictions) { 
			System.out.println(prediction);
		}
	}
	
    public MLTagger() {
		// Default constructor
    }
    
    public Instances string2Instances(String text, MultiLabelInstances multilabelInstances) throws InvalidDataException, ModelInitializationException, Exception {
    	 String [] splitText = text.split(" ");
         Instances ins = multilabelInstances.getDataSet();
         Instance unknown = new DenseInstance(ins.numAttributes());        
         unknown.setDataset(multilabelInstances.getDataSet());
         for (int i = 0; i < splitText.length; i++) {
        	 for (int j = 0; j < ins.numAttributes(); j++) {
        		 if (ins.attribute(j).name().equals(splitText[i])) {
        			 unknown.setValue(ins.attribute(j),1); 
        		 }
        	 }
        }	
        Instances instances = multilabelInstances.getDataSet();
        instances.clear();
        instances.add(unknown);
    	return instances;
    }
	 
    //Note to self: parameters have to be cleaned up get rid of bools etc. just do something clever.
	public List<MultiLabelOutput> tag(String text, String arffFilename, String xmlFilename, String unlabeledFilename, boolean isSingleLineInput, boolean isBuildingModel) throws InvalidDataException, ModelInitializationException, Exception {
		// Loading the data       temporarily laced with booleans 
		RAkEL model = initializeRAkEL(false, true, text, xmlFilename, arffFilename);
		
		// Loading the unlabeledData 
		Instances unlabeledData = null;
		if (isSingleLineInput) {
			unlabeledData = string2Instances(text, new MultiLabelInstances(arffFilename, xmlFilename));
		} else {
			unlabeledData = getUnlabeledData(unlabeledFilename);
		}		
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
	
	private RAkEL initializeRAkEL(boolean isBuildingModel, boolean isSingleLineInput, String text, String xmlFilename, String arffFilename) throws Exception {
		MultiLabelInstances dataset;
		RAkEL model;
		if (isBuildingModel) {
			//model = new RAkEL(new LabelPowerset(new J48()));
			model = new RAkEL(new LabelPowerset(new SMO()));
			//model = new RAkEL(new LabelPowerset(new NaiveBayes()));
			dataset = new MultiLabelInstances(arffFilename, xmlFilename);
			model.build(dataset);	
			// Save the learner to a file
			writeRAkELToFile(model, "model.bin");
			return model;
		} else {
			model = readRAkELFromFile("model.bin");
			return model;
		}
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
	
	private void writeRAkELToFile(RAkEL model, String name) throws Exception {
		File f1 = new File("./temp/" + name);  
		String path = f1.getAbsolutePath(); 
		
		ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
		objectOut.writeObject(model); // Write object
		objectOut.close(); // Close the output stream
	}
			  
	private RAkEL readRAkELFromFile(String name) throws Exception {
		File f1 = new File("./temp/" + name);  
		String path = f1.getAbsolutePath(); 
		
		ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
		RAkEL object = (RAkEL) objectIn.readObject();
		objectIn.close();
		return object;
	}
}