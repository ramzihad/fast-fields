package com.force;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.force.metadata.utils.FieldCreatorHelperFunctions;
import com.force.metadata.utils.ObjectCreator;
import com.force.metadata.utils.SalesforceConnection;

public class ObjectCreatorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ObjectCreatorServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		log.setLevel(Level.ALL);
		log.info("Starting Object Creator");
		
		HttpSession session = req.getSession();
		SalesforceConnection connection = (SalesforceConnection) session.getAttribute("connection");
		String objectName = req.getParameter("objectName");	
		
		log.info("Initializing object creator");
		ObjectCreator creator = new ObjectCreator(objectName, connection);
		
		log.info("Sending to salesforce");
		creator.sendToSalesforce();
		
		// gather status/error info and pass to result jsp page
		String status = creator.getStatus();
		req.setAttribute("status", status);
		
		if(status == "Error") {
			req.setAttribute("error",creator.getError());
		} else {
			req.setAttribute("error", "0");
		}
		
		// add object info to session if successful
		if(status.equals("Success")) {
			session.setAttribute("objectLabel", objectName);
			session.setAttribute("objectName", FieldCreatorHelperFunctions.getDevName(objectName));
		}
		
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.objectCreatorResult);
		rd.forward(req,resp);
	}

}
