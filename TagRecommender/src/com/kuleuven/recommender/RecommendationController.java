package com.kuleuven.recommender;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RecommendationController
 */
@WebServlet("/recommend")
public class RecommendationController extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public RecommendationController() {
        super();
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
    	//response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        // Output string die teruggestuurd wordt naar de client (JSON-formaat indien Sproutcore daar makkelijk mee omgaat?)
        StringBuilder output = new StringBuilder();
        
        String todo = request.getParameter("todo");
        //
        // Allerlei stuff om onze recommendation te doen en onze output string op te bouwen...
        //
        
        output.append("[ { name: \"" + todo + "\", prob: 0.5 }, { name: \"#shop\", prob: 0.2 } ]");
        //output.append("{ name: \"" + todo + "\", prob: 0.5 }");
        
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

}
