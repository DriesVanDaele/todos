package tagger;
import java.util.List;
import tagger.MLTagger;

public class TempController {
	public static void main(String [] args) throws Exception {
		List<Tag> tags = predictTodo(args[0]);
		for (Tag tag : tags) {
			System.out.println(tag.toString());
		}
	}
	
	/**
	 * Runs an instance of MLTagger using the model stored in location ./temp/modelSMO_stem2.bin 
	 * will apply stemming upon the input text and won't let the model do any learning.
	 * @param text : the input text for which you'd like to get some prediction
	 * @return A list of tags. Each tag contains its tag 'name' as well as the 'confidence' associated to it.
	 * The list that gets returned has been sorted by descending confidence and when using the toString()
	 * method on its elements the 'TAG' prefix will get replaced by the hash symbol '#'
	 */
	public static List<Tag> predictTodo(String text) throws Exception {
		return new MLTagger().run("modelSMO_stem2.bin", true, false, text, "data/todo.xml", "data/todo_stem.arff");
	}
}


