package com.force;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.force.metadata.utils.LayoutBuilder;
import com.force.metadata.utils.SalesforceConnection;

public class LayoutBuilderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(FieldCreatorServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		
		HttpSession session = req.getSession();
		String objectName = (String) session.getAttribute("objectName");
		SalesforceConnection connection = (SalesforceConnection) session.getAttribute("connection");
		String temp[] = req.getParameter("fields").split(",");
		List<String >fieldList = Arrays.asList(temp);
		
		LayoutBuilder builder = null;
		try {			
			builder = new LayoutBuilder(objectName, fieldList,connection);
			builder.buildLayouts();
		} catch (Exception e) {
			log.warning("Exception while building layouts: " + e.getMessage());
			e.printStackTrace();
		}
				
		String status = builder.getStatus();
		req.setAttribute("status", status);
		
		if(status == "Error") {
			req.setAttribute("error",builder.getError());
		} else {
			req.setAttribute("error", "0");
		}
		
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.layoutBuilderResult);
		rd.forward(req, resp);
	}
}
