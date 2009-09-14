package com.force;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.force.metadata.utils.FieldCreatorHelperFunctions;
import com.force.metadata.utils.ObjectCreator;

public class ObjectCreatorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ObjectCreatorServlet.class.getName());
	
	private String sessionId;
	private String serverUrl;
	private String objectName;
	
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		log.setLevel(Level.ALL);
		log.info("Starting Object Creator");
		
		sessionId = req.getParameter("sid");
		serverUrl = req.getParameter("srv");
		objectName = req.getParameter("objectName");	
		
		log.info("Initializing object creator");
		ObjectCreator creator = new ObjectCreator(sessionId, serverUrl, objectName);
		
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
		
		// build navigation urls
		String fieldCreatorUrl = FieldCreatorProperties.fieldCreatorURL + "?";
		Set<String> params = req.getParameterMap().keySet();
		for(String param : params) {
			fieldCreatorUrl += param + "=" + req.getParameter(param) + "&";				
		}
		fieldCreatorUrl += "&objectLabel=" + objectName;
		fieldCreatorUrl += "&objectName=" + FieldCreatorHelperFunctions.getDevName(objectName);
		req.setAttribute("fieldCreatorUrl", fieldCreatorUrl);
		
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.objectCreatorResult);
		rd.forward(req,resp);
	}

}
