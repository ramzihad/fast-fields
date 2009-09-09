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

import com.force.metadata.utils.SalesforceConnection;
import com.sforce.ws.ConnectionException;

public class LaunchServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(LaunchServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		
		log.setLevel(Level.ALL);
		
		// set url to return to start page
		HttpSession session = req.getSession();
		String returnUrl = req.getParameter("retUrl");
		session.setAttribute("returnUrl", returnUrl);
		
		// add object info if the user is creating fields
		String action = req.getParameter("action");
		if(action.equals("fields")) {
			String objectName = req.getParameter("objectName");
			String objectLabel = req.getParameter("objectLabel");
			session.setAttribute("objectName", objectName);
			session.setAttribute("objectLabel", objectLabel);			
		}
		
		
		// create salesforce connection for this session
		String sessionId = req.getParameter("sid");
		String endpointUrl = req.getParameter("srv");	
		SalesforceConnection connection = new SalesforceConnection(sessionId, endpointUrl);		
		
		try {
			// might as well catch connection errors on the launch page
			connection.createConnection();
			session.setAttribute("connection", connection);
			
			// send user to the appropriate initial action page
			RequestDispatcher rd = null;
			if(action.equals("fields")) {
				rd = req.getRequestDispatcher(FieldCreatorProperties.fieldCreatorURL);
			} else if(action.equals("object")) {
				rd = req.getRequestDispatcher(FieldCreatorProperties.objectCreator);
			} else {
				resp.getWriter().println("Uhoh.  I didn't get an action.  Email rcallaway@salesforce.com if you reached this page");
			}
			
			if(null != rd)
				rd.forward(req,resp);	
			
		} catch (ConnectionException e) {
			resp.getWriter().println("Connection Exception: " + e.getMessage());
			e.printStackTrace();
		}	
	}
}
