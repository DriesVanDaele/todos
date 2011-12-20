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
import mulan.data.InvalidDataFormatException;
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
	public MLTagger() { } // Default constructor
	
	public static void main(String[] args) throws Exception {		
		
		// collect the filenames from the arguments
		String arffFilename = Utils.getOption("arff", args);
		String xmlFilename = Utils.getOption("xml", args);
		String modelFilename = Utils.getOption("model", args); 
		String text = Utils.getOption("text", args); 
		String unlabeledFilename = Utils.getOption("unlabeled", args);

		// set booleans: 
		// - a model will be built if there was a 'b' flag
		// - a single line of text will be predicted if some text was followed by the 'text' flag
		// - stemming will be applied to the line of text if the program was called with an 's' flag
		boolean isBuildingModel = Utils.getFlag("b", args); 
		boolean isSingleLineInput = (text != ""); 
		boolean isUsingStemming = Utils.getFlag("s", args);
		
		MLTagger tagger = new MLTagger();
		List<Tag> suggestedTags = new ArrayList<Tag>();
		if (isSingleLineInput) {
			suggestedTags = tagger.run(modelFilename, isUsingStemming, isBuildingModel, text, xmlFilename, arffFilename);
			for (Tag tag : suggestedTags) {
				System.out.println(tag);
	    			}	
			} else {	
	    		suggestedTags = tagger.run(modelFilename, unlabeledFilename, isUsingStemming, isBuildingModel, xmlFilename, arffFilename);	
	    	}
	    }

    public List<Tag> run(String modelFilename, String unlabeledFilename, boolean isSingleLineInput, boolean isBuildingModel, boolean isUsingStemming, String text, String xmlFilename, String arffFilename) throws Exception {
    	RAkEL model = initializeRAkEL(isSingleLineInput, isBuildingModel, text, xmlFilename, arffFilename, modelFilename);
    	Instances unlabeledData = getInstances(text, arffFilename, xmlFilename, unlabeledFilename, isSingleLineInput, isUsingStemming);
    	return getSuggestedTagsWithInfo(unlabeledData, new MultiLabelInstances(arffFilename, 15), model);
    }
    
    public List<Tag> run(String modelFilename, String unlabeledFilename, boolean isUsingStemming, boolean isBuildingModel, String xmlFilename, String arffFilename) throws Exception {
    	return run(modelFilename, unlabeledFilename, false, isBuildingModel, isUsingStemming, "", xmlFilename, arffFilename);
    }
    
    public List<Tag> run(String modelFilename, boolean isUsingStemming, boolean isBuildingModel, String text, String xmlFilename, String arffFilename) throws Exception {
    	return run(modelFilename, "", true, isBuildingModel, isUsingStemming, text, xmlFilename, arffFilename);
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
	 
    private Instances getInstances(String text, String arffFilename, String xmlFilename, String unlabeledFilename, boolean isSingleLineInput, boolean isUsingStemming) throws InvalidDataException, ModelInitializationException, InvalidDataFormatException, Exception {
    	Instances unlabeledData = null;
    	if (isSingleLineInput) {
    	    unlabeledData = string2Instances(text, new MultiLabelInstances(arffFilename, 15), isUsingStemming);
    	} else {
    	    unlabeledData = getUnlabeledData(unlabeledFilename);
    	}		
    	return unlabeledData;
    }
    
     private List<Tag> getSuggestedTagsWithInfo(Instances unlabeledData, MultiLabelInstances multiUnlabeledData, RAkEL model) throws InvalidDataException, ModelInitializationException, Exception {
    	List<Tag> suggestedTags = new ArrayList<Tag>();
    	weka.core.Attribute [] attributes = {};
    	attributes = multiUnlabeledData.getLabelAttributes().toArray(attributes);
    	List<MultiLabelOutput> predictions = getPredictions(model, unlabeledData);
    	System.out.println("nbofpredictions = " + predictions.size());
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

    
    private RAkEL initializeRAkEL(boolean isSingleLineInput, boolean isBuildingModel, String text, String xmlFilename, String arffFilename, String modelFileName) throws Exception {
	MultiLabelInstances dataset;
	RAkEL model;
	if (isBuildingModel) {
	    model = new RAkEL(new LabelPowerset(new SMO()));
	    dataset = new MultiLabelInstances(arffFilename, xmlFilename);
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
     * Writes the given RAkEL model to the file with name 'name' under directory temp/
     * @param model : the instance of RAkEL that should get stored to memory
     * @param name : the name of the output file.
     */
    private void writeRAkELToFile(RAkEL model, String name) throws Exception {
	File f1 = new File("models/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
	objectOut.writeObject(model); // Write the object
	objectOut.close(); // Close the output stream
    }
   
    /**
     * Reads a RAkEL model from the file with name 'name' under directory temp/
     * @param name : the name of the input file.
     * @return the RAkEL object that was stored in the file under location temp/'name'.
     */
    private RAkEL readRAkELFromFile(String name) throws Exception {
    File f1 = new File("models/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
	RAkEL object = (RAkEL) objectIn.readObject(); // Read the object
	objectIn.close(); // Close the input stream
	return object;
    }
}