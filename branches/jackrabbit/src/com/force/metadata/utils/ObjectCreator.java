package com.force.metadata.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.DeploymentStatus;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.SharingModel;
import com.sforce.ws.ConnectionException;

public class ObjectCreator {
	private static final Logger log = Logger.getLogger(ObjectCreator.class.getName());
	
	private String sessionId;
	private String endpointUrl;
	private String objectName;
	
	private String status;
	private String error;
	
	private CustomObject customObject;
	
	private SalesforceConnection connection;
	
	public String getStatus() {
		return status;
	}
	
	public String getError() {
		return error;
	}
	
	public ObjectCreator(String sessionId, String endpointUrl, String objectName) {
		log.setLevel(Level.ALL);
		
		this.sessionId = sessionId;
		this.endpointUrl = endpointUrl;
		this.objectName = objectName;
		status = "Initialized";
		error = "";
		
		buildObject();
	}
	
	private void buildObject() {
		log.info("Building Custom Object");
		customObject = new CustomObject();
		String objectDevName = FieldCreatorHelperFunctions.getDevName(objectName);
		customObject.setFullName(objectDevName);
		customObject.setLabel(objectName);
		customObject.setPluralLabel(objectName + "s");
		customObject.setEnableReports(true);
		customObject.setEnableActivities(true);
		customObject.setEnableHistory(true);
		customObject.setDeploymentStatus(DeploymentStatus.Deployed);
		customObject.setSharingModel(SharingModel.ReadWrite);
		// TODO: figure out how to make it available for customer portal
		
		CustomField nameField = new CustomField();
		nameField.setType(FieldType.Text);
		String fieldName = objectName + " Name";
		nameField.setLabel(fieldName);
		String fieldDevName = objectDevName + "." + FieldCreatorHelperFunctions.getDevName(fieldName);
		nameField.setFullName(fieldDevName);
		customObject.setNameField(nameField);	
	}
	
	public void sendToSalesforce() {
		connection = new SalesforceConnection(sessionId, endpointUrl);
		try {
			connection.createConnection();
		} catch (ConnectionException e) {
			String errorMessage = "Connection Exception with salesforce: " + e.getMessage();
			handleError(errorMessage, e);
		}
		try {
			connection.sendToSalesforce(new CustomObject[] { customObject });
		} catch (Exception e) {
			String errorMessage = "Exception during object creation: " + e.getMessage();
			handleError(errorMessage, e);
		}
		status = "Success";
	}
	
	private void handleError(String errorMessage, Exception e) {
		log.warning(errorMessage);
		status = "Error";
		error = errorMessage;
		e.printStackTrace();
	}
}
