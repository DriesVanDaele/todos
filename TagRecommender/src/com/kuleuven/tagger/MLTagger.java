package com.kuleuven.tagger;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.transformation.LabelPowerset;
import mulan.data.MultiLabelInstances;
import weka.classifiers.functions.SMO;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stemmers.SnowballStemmer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Prints suggested tags
// Each outputline consists of a suggested tag followed by its confidence
public class MLTagger {
	
	// This variable is only required for the web application.
	// this way the model can be read in once and only once at the constructor. 
	// (reading in the file time and time again serves as a bottleneck - redundant IO)
	// then use quickRun() (instead of run()) to perform predictions, 
	private RAkEL model;
	
	public MLTagger(String modelFilename, boolean isBuildingModel, String arffFilename, int nbOfLabels) { 
		try {
			model = initializeRAkEL(modelFilename, isBuildingModel, arffFilename, nbOfLabels);
		} catch (Exception e) {
			e.printStackTrace();
			model = null;
		}
	} 
	
	public MLTagger() {
		// Default constructor
	}
	
	public static void main(String[] args) throws Exception {		
		
		// collect the filenames from the arguments
		String arffFilename = Utils.getOption("arff", args);
		String modelFilename = Utils.getOption("model", args); 
		String text = Utils.getOption("text", args); 
		String unlabeledFilename = Utils.getOption("unlabeled", args);
		
		// set the number of labels (15 for the todo application)
		int nbOfLabels = new Integer(Utils.getOption("labels", args)).intValue();

		// set booleans: 
		// - a model will be built if there was a 'b' flag
		// - a single line of text will be predicted if some text was followed by the 'text' flag
		// - stemming will be applied to the line of text if the program was called with an 's' flag
		boolean isBuildingModel = Utils.getFlag("b", args); 
		boolean isSingleLineInput = (text != ""); 
		boolean isUsingStemming = Utils.getFlag("s", args);

		MLTagger tagger = new MLTagger(modelFilename, isBuildingModel, arffFilename, nbOfLabels);
		List<Tag> suggestedTags = new ArrayList<Tag>();
		
		
		if (isSingleLineInput) {
			suggestedTags = tagger.quickRun(arffFilename, nbOfLabels, isUsingStemming, text);
			for (Tag tag : suggestedTags) {
				System.out.println(tag);
			}		
		} else {	
			suggestedTags = tagger.run(modelFilename, isBuildingModel, arffFilename, nbOfLabels, unlabeledFilename, isUsingStemming);	
	    }
	}
	public List<Tag> quickRun(String arffFilename, int nbOfLabels, boolean isUsingStemming, String text) throws Exception {
		Instances unlabeledData = getInstances(arffFilename, nbOfLabels, isUsingStemming, "", true, text);
    	return getSuggestedTagsWithInfo(unlabeledData, new MultiLabelInstances(arffFilename, nbOfLabels), model);
    }
	
    public List<Tag> run(String modelFilename, boolean isBuildingModel, String arffFilename, int nbOfLabels, boolean isUsingStemming, String unlabeledFilename, boolean isSingleLineInput, String text) throws Exception {
    	RAkEL model = initializeRAkEL(modelFilename, isBuildingModel, arffFilename, nbOfLabels);
    	Instances unlabeledData = getInstances(arffFilename, nbOfLabels, isUsingStemming, unlabeledFilename, isSingleLineInput, text);
    	return getSuggestedTagsWithInfo(unlabeledData, new MultiLabelInstances(arffFilename, nbOfLabels), model);
    }
    
    public List<Tag> run(String modelFilename, boolean isBuildingModel, String arffFilename, int nbOfLabels, String unlabeledFilename, boolean isUsingStemming) throws Exception {
    	return run(modelFilename, isBuildingModel, arffFilename, nbOfLabels, isUsingStemming, unlabeledFilename, false, "");
    }
    
    public List<Tag> run(String modelFilename, boolean isBuildingModel, String arffFilename, int nbOfLabels, boolean isUsingStemming, String text) throws Exception {
    	return run(modelFilename, isBuildingModel, arffFilename, nbOfLabels, isUsingStemming, "", true, text);
    }

    /**
     * Returns a stemmed array of strings
     * @param splitText : An array of Strings, each String specifying a single word
     * @return The input after applying stemming on it.
     */
    private String [] stemStrings(String [] splitText) {
    	// currently using porter, since I haven't currently got .arff files for
		// other types, even though 'english' is said to be slightly, though strictly, better
 		SnowballStemmer stemmer = new SnowballStemmer();
 		String [] stemmedSplitText = new String[splitText.length];
 		for (int i = 0; i < splitText.length; i++) {
 			stemmedSplitText[i] = stemmer.stem(splitText[i]);
 		}
 		return stemmedSplitText;
    }
    
    /**
     * Takes a given String of text and uses it together with information stored in multilabelInstances 
     * to turn create an instance of class Instances. If the boolean isUsingStemming evaluates to true, 
     * then stemming will be applied on the input string.
     * @param text : An input String. Stemming will get applied to it if isUsingStemming evaluates to true 
     * @param multilabelInstances : an instance of MultiLabelInstances
     * @param isUsingStemming : determines whether or not the given String will get stemmed
     * @return an instance of Class Instances
     */
    private Instances string2Instances(String text, MultiLabelInstances multilabelInstances, boolean isUsingStemming) throws InvalidDataException, ModelInitializationException, Exception {
    	 String [] splitText = text.split(" ");
    	 if (isUsingStemming) {
    		splitText = stemStrings(splitText);
     	 }
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
	 
    private Instances getInstances(String arffFilename, int nbOfLabels, boolean isUsingStemming, String unlabeledFilename, boolean isSingleLineInput, String text) throws Exception {
    	Instances unlabeledData = null;
    	if (isSingleLineInput) {
    	    unlabeledData = string2Instances(text, new MultiLabelInstances(arffFilename, nbOfLabels), isUsingStemming);
    	} else {
    	    unlabeledData = getUnlabeledData(unlabeledFilename);
    	}		
    	return unlabeledData;
    }
    
     private List<Tag> getSuggestedTagsWithInfo(Instances unlabeledData, MultiLabelInstances multiUnlabeledData, RAkEL model) throws Exception {
    	List<Tag> suggestedTags = new ArrayList<Tag>();
    	weka.core.Attribute [] attributes = {};
    	attributes = multiUnlabeledData.getLabelAttributes().toArray(attributes);
    	List<MultiLabelOutput> predictions = getPredictions(model, unlabeledData);
    	for (MultiLabelOutput prediction : predictions) { 
    	    for (int i = 0; i < prediction.getBipartition().length; i++) {
    	    	//if (prediction.getBipartition()[i]) {	
    	    		// THIS CODE REQUIRES THE TAGS TO BE ADDED LAST IN THE ARFF, can't find a cleaner solution :s
    	       		suggestedTags.add(new Tag(multiUnlabeledData.getDataSet().attribute(multiUnlabeledData.getDataSet().numAttributes()-multiUnlabeledData.getNumLabels()+i).name(), prediction.getConfidences()[i]));	
    	    	//}
    	    }
    	}    	
    	Collections.sort(suggestedTags, Collections.reverseOrder());
    	return suggestedTags;
    }
//    	for (MultiLabelOutput output : predictions) {
//		  if (output.hasBipartition()) {
//            String bipartion = Arrays.toString(output.getBipartition());
//            System.out.println("Predicted bipartion: " + bipartion);
//		  }
//		  if (output.hasRanking()) {
//            String ranking = Arrays.toString(output.getRanking());
//            System.out.println("Predicted ranking: " + ranking);
//		  }
//		  if (output.hasConfidences()) {
//            String confidences = Arrays.toString(output.getConfidences());
//            System.out.println("Predicted confidences: " + confidences);
//		  }
//        }   

    
    private RAkEL initializeRAkEL(String modelFileName, boolean isBuildingModel, String arffFilename, int nbOfLabels) throws Exception {
	MultiLabelInstances dataset;
	RAkEL model;
	if (isBuildingModel) {
	    model = new RAkEL(new LabelPowerset(new SMO()));
	    dataset = new MultiLabelInstances(arffFilename, nbOfLabels);
	    model.build(dataset);	
	    // Save the learner to a fileSMO
	    writeRAkELToFile(model, modelFileName);
	    return model;
	} else {
	    model = readRAkELFromFile(modelFileName);
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
    
    /**
     * Writes the given RAkEL model to the file with name 'name'
     * @param model : the instance of RAkEL that should get stored to memory
     * @param name : the name of the output file.
     */
    private void writeRAkELToFile(RAkEL model, String name) throws Exception {
	File f1 = new File(name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
	objectOut.writeObject(model); // Write the object
	objectOut.close(); // Close the output stream
    }
   
    /**
     * Reads a RAkEL model from the file with name 'name'
     * @param name : the name of the input file.
     * @return the RAkEL object that was stored in the file under location 'name'.
     */
    private RAkEL readRAkELFromFile(String name) throws Exception {
    File f1 = new File(name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
	RAkEL object = (RAkEL) objectIn.readObject(); // Read the object
	objectIn.close(); // Close the input stream
	return object;
    }
}