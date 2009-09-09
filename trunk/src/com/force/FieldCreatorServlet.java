package com.force;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.force.metadata.utils.FieldCreator;
import com.force.metadata.utils.SalesforceConnection;

public class FieldCreatorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(FieldCreatorServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		
		log.setLevel(Level.ALL);
		
		log.info("Starting Field Creator...");
		
		HttpSession session = req.getSession();		
		SalesforceConnection connection = (SalesforceConnection) session.getAttribute("connection");
		String objectName = (String) session.getAttribute("objectName");
		FieldCreator creator = new FieldCreator(objectName,connection);
		
		log.info("Field creator initialized");
		
		// process fields, we don't 
		for(int i = 1; i <= FieldCreatorProperties.entryRows; i++) {			
			
			log.info("Processing Field " + i);
			
			String fieldType = req.getParameter(FieldCreatorProperties.getTypeIdForFieldNum(i));
			String fieldName = req.getParameter(FieldCreatorProperties.getNameIdForFieldNum(i));
			
			Integer fieldDecimals = null;
			String tempDecimalsVal = req.getParameter(FieldCreatorProperties.getDecimalsIdForFieldNum(i));
			if(null != tempDecimalsVal && "" != tempDecimalsVal)
				fieldDecimals = Integer.parseInt(req.getParameter(FieldCreatorProperties.getDecimalsIdForFieldNum(i)));
			else
				fieldDecimals = 0;
			
			String[] fieldValues = new String[0];
			if(null != req.getParameter(FieldCreatorProperties.getValuesIdForFieldNum(i)))
				fieldValues = req.getParameter(FieldCreatorProperties.getValuesIdForFieldNum(i)).split(","); 
			
			if(fieldName == null || fieldName.isEmpty()) {
				log.info("No info for field " + i);
				continue;
			}
				
			try {
				creator.addField(fieldName, fieldType, fieldDecimals, fieldValues);
			} catch(InvalidParameterException e) {
				log.warning("Invalid param exception: " + e.getMessage());
				log.warning("Field Number: " + i);
				log.warning("Field Name:" + fieldName);
				log.warning("Field Type:" + fieldType);
				log.warning("Field Decimals: " + fieldDecimals);
				log.warning("Field Values: " + fieldValues);			
			}
		}	
	
		creator.sendToSalesforce();
		
		String fieldList = creator.getEncodedCustomFields();
		req.setAttribute("fields", fieldList);
		
		String status = creator.getStatus();
		req.setAttribute("status", status);
		
		if(status == "Error") {
			String errorMessage = creator.getError();			
			// check for error specific to a bug in the integration package
			// and if it's found notify the user to upgrade to latest package
			if(errorMessage.contains("Failed to get next element")) {
				errorMessage += ".  This error is caused by a bug in your version of Fields Fast!!!  This can be fixed by uninstalling and reinstalling the current package.  " +
						"Demo Component Url (62-org): " + FieldCreatorProperties.demoComponentUrl;
			}
			req.setAttribute("error",errorMessage);
		} else {
			req.setAttribute("error", "0");
		}
		
		// build url with field params for layout builder
		String layoutBuilderUrl = FieldCreatorProperties.layoutBuilderServlet + "?";
		layoutBuilderUrl += "fields=" + creator.fieldNameList();
		req.setAttribute("layoutBuilderUrl",layoutBuilderUrl);
		
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.fieldCreatorResult);
		rd.forward(req, resp);		
	}	
}
