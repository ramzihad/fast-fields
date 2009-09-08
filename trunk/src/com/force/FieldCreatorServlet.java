package com.force;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.force.metadata.utils.FieldCreator;

public class FieldCreatorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(FieldCreatorServlet.class.getName());
	
	private String sessionId;
	private String serverUrl;
	private String objectName;
	
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		
		log.setLevel(Level.ALL);
		
		log.info("Staring field creator...");
		
		sessionId = req.getParameter("sid");
		serverUrl = req.getParameter("srv");
		objectName = req.getParameter("objectName");	
		
		FieldCreator creator = new FieldCreator(sessionId, serverUrl, objectName);
		
		log.info("Field creator initialized");
		
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
			
			} catch(Exception e) {
				
				log.warning("Exception: " + e.getMessage());
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
			req.setAttribute("error",creator.getError());
		} else {
			req.setAttribute("error", "0");
		}
		
		String fieldCreatorUrl = FieldCreatorProperties.fieldCreatorURL + "?";
		String layoutBuilderUrl = FieldCreatorProperties.layoutBuilderServlet + "?";
		Set<String> params = req.getParameterMap().keySet();
		for(String param : params) {
			if(!param.startsWith("field")) {
				fieldCreatorUrl += param + "=" + req.getParameter(param) + "&";
				layoutBuilderUrl += param + "=" + req.getParameter(param) + "&";
			}
		}
		// add info for custom fields created
		layoutBuilderUrl += "fields=" + creator.fieldNameList();
		
		req.setAttribute("fieldCreatorUrl",fieldCreatorUrl);
		req.setAttribute("layoutBuilderUrl",layoutBuilderUrl);
			
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.fieldCreatorResult);
		rd.forward(req, resp);		
	}

	
}
