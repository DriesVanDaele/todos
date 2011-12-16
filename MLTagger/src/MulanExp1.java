import mulan.classifier.lazy.MLkNN;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.LabelPowerset;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Utils;

public class MulanExp1 {

    public static void main(String[] args) throws Exception {
        String arffFilename = Utils.getOption("arff", args); // e.g. -arff emotions.arff
        String xmlFilename = Utils.getOption("xml", args); // e.g. -xml emotions.xml

        MultiLabelInstances dataset = new MultiLabelInstances(arffFilename, xmlFilename);
		
        MLkNN learner1 = new MLkNN();
		
        RAkEL learner2 = new RAkEL(new LabelPowerset(new J48()));
        RAkEL learner3 = new RAkEL(new LabelPowerset(new NaiveBayes()));
        RAkEL learner4 = new RAkEL(new LabelPowerset(new SMO()));
		
		BinaryRelevance learner5 = new BinaryRelevance(new J48());
		BinaryRelevance learner6 = new BinaryRelevance(new NaiveBayes());
		BinaryRelevance learner7 = new BinaryRelevance(new SMO());

        Evaluator eval = new Evaluator();
        MultipleEvaluation results;

        int numFolds = 10;
		long currentTime = System.currentTimeMillis();
		
		System.out.println("-- ML-kNN --");
        results = eval.crossValidate(learner1, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- RAkEL LabelPowerset J48 --");
        results = eval.crossValidate(learner2, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- RAkEL LabelPowerset NaiveBayes --");
        results = eval.crossValidate(learner3, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- RAkEL LabelPowerset SMO --");
        results = eval.crossValidate(learner4, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- BinaryRelevance J48 --");
        results = eval.crossValidate(learner5, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- BinaryRelevance NaiveBayes --");
        results = eval.crossValidate(learner6, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
		
		currentTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("-- BinaryRelevance SMO --");
        results = eval.crossValidate(learner7, dataset, numFolds);
        System.out.println(results);
		System.out.println("Time taken: " + (System.currentTimeMillis() - currentTime));
    }
}