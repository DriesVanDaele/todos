package com.kuleuven.recommender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kuleuven.tagger.MLTagger;
import com.kuleuven.tagger.Tag;

/**
 * Servlet implementation class RecommendationController
 */
@WebServlet("/recommend")
public class RecommendationController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private MLTagger tagger;

    /**
     * Default constructor. 
     */
    public RecommendationController() {
        super();
        tagger = new MLTagger("models/modelSMO_stem.bin", false, "data/todo_stem.arff", 15);
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String todo = request.getParameter("todo");
        if(todo == null)
        	return;
        
        StringBuilder output = new StringBuilder("[");

        todo = todo.toLowerCase();
		todo = todo.replaceAll(" #", " TAG");
		todo = todo.replaceAll("[\\W&&[^\\s]]", "");
		todo = todo.replaceAll("class", "class_");
		
		try {
			List<Tag> tags = predictTodo(todo);
			for (Tag tag : tags) {
				if(tag.getConfidence() <= 0)
					break;
				if(!todo.contains(tag.getTag()))
					output.append(tag.toJSON() + ",");
			}
			if(output.charAt(output.length()-1) == ',')
				output.setCharAt(output.length()-1, ' ');
				
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        output.append("]");
        
        out.println(output);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}
	
	/**
	 * Runs an instance of MLTagger using the model stored in location ./temp/modelSMO_stem2.bin 
	 * will apply stemming upon the input text and won't let the model do any learning.
	 * @param text : the input text for which you'd like to get some prediction
	 * @return A list of tags. Each tag contains its tag 'name' as well as the 'confidence' associated to it.
	 * The list that gets returned has been sorted by descending confidence.
	 */
	private List<Tag> predictTodo(String text) throws Exception {
		return tagger.quickRun("data/todo_stem.arff", 15, true, text);
	}

}
