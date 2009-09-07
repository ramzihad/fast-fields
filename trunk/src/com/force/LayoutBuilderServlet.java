package com.force;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.force.metadata.utils.LayoutBuilder;

public class LayoutBuilderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
private static final Logger log = Logger.getLogger(FieldCreatorServlet.class.getName());
	
	private String sessionId;
	private String serverUrl;
	private String objectName;
	private List<String> fieldList;
	
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		
		sessionId = req.getParameter("sid");
		serverUrl = req.getParameter("srv");
		objectName = req.getParameter("objectName");
		String temp[] = req.getParameter("fields").split(",");
		fieldList = Arrays.asList(temp);
		
		LayoutBuilder builder = null;
		try {
			builder = new LayoutBuilder(sessionId,serverUrl,objectName,fieldList);
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
		
		String fieldCreatorUrl = FieldCreatorProperties.fieldCreatorURL + "?";		
		Set<String> params = req.getParameterMap().keySet();
		for(String param : params) {
			if(!param.startsWith("field")) {
				fieldCreatorUrl += param + "=" + req.getParameter(param) + "&";				
			} else if (param.equals("fields")) {
				fieldCreatorUrl += "oldFields=" + req.getParameter(param) + "&";
			}
		}
		
		req.setAttribute("fieldCreatorUrl",fieldCreatorUrl);
		
		RequestDispatcher rd = req.getRequestDispatcher(FieldCreatorProperties.layoutBuilderResult);
		rd.forward(req, resp);
	}
}
