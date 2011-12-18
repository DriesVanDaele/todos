import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.transformation.LabelPowerset;
import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
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
    public static void main(String[] args) throws Exception {		
    	boolean isSingleLineInput = false;  
    	boolean isBuildingModel = false; 
    	boolean isUsingStemming = false;
    	// check some flash for the state of the booleans
    	if (Utils.getFlag("i", args)) { isSingleLineInput = true; }
    	if (Utils.getFlag("b", args)) { isBuildingModel = true; }
    	if (Utils.getFlag("s", args)) { isUsingStemming = true; }
    	
    	// collect the filenames from the arguments
    	String modelFileName = Utils.getOption("model", args);
    	String text = Utils.getOption("text", args);
    	String arffFilename = Utils.getOption("arff", args);
    	String xmlFilename = Utils.getOption("xml", args);
    	String unlabeledFilename = Utils.getOption("unlabeled", args);
	
    	MLTagger tagger = new MLTagger();
    	RAkEL model = tagger.initializeRAkEL(isSingleLineInput, isBuildingModel, text, xmlFilename, arffFilename, modelFileName);
    	Instances unlabeledData = tagger.getInstances(text, arffFilename, xmlFilename, unlabeledFilename, isSingleLineInput, isUsingStemming);
    	List<String> suggestedTags = tagger.getSuggestedTagsWithInfo(unlabeledData, new MultiLabelInstances(arffFilename, xmlFilename), model);
    	
//    	List<MultiLabelOutput> multiLabelOutputs = tagger.getPredictions(model, unlabeledData);
//    	
//    	//UNCOMMENT THIS IF YOU WANT SOME MORE SPECIFIC INFO (there still seems to be something wrong...)
//    	for (MultiLabelOutput output : multiLabelOutputs) {
//    		  if (output.hasBipartition()) {
//                  String bipartion = Arrays.toString(output.getBipartition());
//                  System.out.println("Predicted bipartion: " + bipartion);
//              }
//    		  // SOMETHING SEEMS WRONG WITH RANKING...
//              if (output.hasRanking()) {
//                  String ranking = Arrays.toString(output.getRanking());
//                  System.out.println("Predicted ranking: " + ranking);
//              }
//              if (output.hasConfidences()) {
//                  String confidences = Arrays.toString(output.getConfidences());
//                  System.out.println("Predicted confidences: " + confidences);
//              }
//    	}   
   	
    	for (String tag : suggestedTags) {
    		System.out.println(tag);
    	}	
    }
	
    public MLTagger() { } // Default constructor
    
    public Instances string2Instances(String text, MultiLabelInstances multilabelInstances, boolean isUsingStemming) throws InvalidDataException, ModelInitializationException, Exception {
    	 String [] splitText = text.split(" ");
    	 if (isUsingStemming) {
    		// currently using porter, since I haven't atm got .arff files for
    		// other types, even though 'english' is said to be slightly, though strictly better
     		SnowballStemmer stemmer = new SnowballStemmer();
     		for (int i = 0; i < splitText.length; i++) {
     			splitText[i] = stemmer.stem(splitText[i]);
     		}
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
    	    unlabeledData = string2Instances(text, new MultiLabelInstances(arffFilename, xmlFilename), isUsingStemming);
    	} else {
    	    unlabeledData = getUnlabeledData(unlabeledFilename);
    	}		
    	return unlabeledData;
    }
    
     private List<String> getSuggestedTagsWithInfo(Instances unlabeledData, MultiLabelInstances multiUnlabeledData, RAkEL model) throws InvalidDataException, ModelInitializationException, Exception {
    	List<String> suggestedTags = new ArrayList<String>();
    	weka.core.Attribute [] attributes = {};
    	attributes = multiUnlabeledData.getLabelAttributes().toArray(attributes);
    	List<MultiLabelOutput> predictions = getPredictions(model, unlabeledData);
    	for (MultiLabelOutput prediction : predictions) { 
    	    for (int i = 0; i < prediction.getBipartition().length; i++) {
    	    	if (prediction.getBipartition()[i]) {	
    	    		// THIS CODE REQUIRES THE TAGS TO BE ADDED LAST IN THE ARFF, can't find a cleaner solution :s
    	       		suggestedTags.add(multiUnlabeledData.getDataSet().attribute(multiUnlabeledData.getDataSet().numAttributes()-multiUnlabeledData.getNumLabels()+i).name() + " " + prediction.getConfidences()[i]);	
    	    	}
    	    }
    	}
    	return suggestedTags;
    }
    
    private RAkEL initializeRAkEL(boolean isSingleLineInput, boolean isBuildingModel, String text, String xmlFilename, String arffFilename, String modelFileName) throws Exception {
	MultiLabelInstances dataset;
	RAkEL model;
	if (isBuildingModel) {
	    //model = new RAkEL(new LabelPowerset(new J48()));
	    model = new RAkEL(new LabelPowerset(new SMO()));
	    //model = new RAkEL(new LabelPowerset(new NaiveBayes()));
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
    
    // slash will probably have to be replaced by a backslash under windows 
    private void writeRAkELToFile(RAkEL model, String name) throws Exception {
	File f1 = new File("./temp/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
	objectOut.writeObject(model); // Write the object
	objectOut.close(); // Close the output stream
    }
   
    // slash will probably have to be replaced by a backslash under windows 
    private RAkEL readRAkELFromFile(String name) throws Exception {
	File f1 = new File("./temp/" + name);  
	String path = f1.getAbsolutePath(); 
	
	ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
	RAkEL object = (RAkEL) objectIn.readObject(); // Read the object
	objectIn.close(); // Close the input stream
	return object;
    }
}