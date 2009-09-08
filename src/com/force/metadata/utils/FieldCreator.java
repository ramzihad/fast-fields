package com.force.metadata.utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.Picklist;
import com.sforce.soap.metadata.PicklistValue;
import com.sforce.ws.ConnectionException;

public class FieldCreator {
	private static final Logger log = Logger.getLogger(FieldCreator.class.getName());
	private static final Map<String, FieldType> fieldTypeMapping;
	
	private String sessionId;
	private String endpointUrl;
	private String objectName;
	private List<CustomField> fields;
	
	private String status; // should move this into an enumeration
	private String error;
	
	private SalesforceConnection connection;
	
	public String getStatus() {
		return status;
	}
	
	public String getError() {
		return error;
	}
	
	public String fieldNameList() {
		String output = "";
		for(CustomField cf : fields) {
			output += cf.getFullName().split("\\.")[1] +",";
		}
		output = output.substring(0,output.length()-1);
		return output;
	}
	
	public String getEncodedCustomFields() {
		String output = "[";
		
		for(CustomField field : fields) {
			output += encodeCustomField(field) + "|";
		}
		
		output = output.substring(0,output.length()-1);
		
		output += "]";
		return output;		
	}
	
	private static String encodeCustomField(CustomField field) {
		String output = "(";
		output += "name=" + field.getFullName().split("\\.")[1];
		output += ";label=" + field.getLabel();
		output += ";type=" + field.getType();
		
		if(field.getType().equals(FieldType.Picklist)) {
			output += ";values=";
			for(PicklistValue p : field.getPicklist().getPicklistValues()) {
				output+= p.getFullName() + ",";
			}
			output = output.substring(0,output.length()-1);
		}
		
		if(field.getType().equals(FieldType.Currency) || field.getType().equals(FieldType.Number)) 
			output += ";decimals=" + field.getScale();
		
		output += ")";
		
		return output;
	}
	
	public static String[] getFieldTypes() {
		Set<String> fieldTypes = fieldTypeMapping.keySet();
		Object[] temp = fieldTypes.toArray();
		String[] returnVal = new String[fieldTypes.size()]; 
		for(int i = 0; i < fieldTypes.size(); i++) {
			returnVal[i] = (String) temp[i];
		}
		return returnVal;	
	}
	
	static {
		fieldTypeMapping = new HashMap<String, FieldType>();
		fieldTypeMapping.put("phone", FieldType.Phone);
		fieldTypeMapping.put("picklist", FieldType.Picklist);
		fieldTypeMapping.put("date", FieldType.Date);
		fieldTypeMapping.put("text",FieldType.Text);
		fieldTypeMapping.put("number",FieldType.Number);
		fieldTypeMapping.put("currency", FieldType.Currency);
		fieldTypeMapping.put("checkbox", FieldType.Checkbox);
		fieldTypeMapping.put("longTextArea", FieldType.LongTextArea);
		fieldTypeMapping.put("email", FieldType.Email);
	}

	public FieldCreator(String sessionId, String endpointUrl, String objectName) {
		log.setLevel(Level.ALL);
		
		this.sessionId = sessionId;
		this.endpointUrl = endpointUrl;
		this.objectName = objectName;
		fields = new ArrayList<CustomField>();
		status = "Initialized";
	}
	
	public void addField(String fieldName, String fieldType, Integer decimals, String[] values) throws InvalidParameterException {
		FieldType type = fieldTypeMapping.get(fieldType);
		if (null == type) {
			throw new InvalidParameterException("Invalid field Type");
		}
		
		CustomField cf = buildCustomField(fieldName, type);
		
		if (type == FieldType.Number || type == FieldType.Currency) {
			log.info("Field decimals: " + decimals);
			cf.setScale(decimals);
			cf.setPrecision(18);
		} else if (type == FieldType.Checkbox) {
			cf.setDefaultValue("true");
		} else if (type == FieldType.LongTextArea) {
			cf.setLength(32000);
			cf.setVisibleLines(3);
		} else if (type == FieldType.Picklist) {
			log.info("Field values: " + values);
			List<PicklistValue> pickListValues = new ArrayList<PicklistValue>();					
			for(String value : values) {
				PicklistValue val = new PicklistValue();
				val.setDefault(false);
				val.setFullName(value);
				pickListValues.add(val);			
			}
			
			Picklist list = new Picklist();
			list.setSorted(false);
			list.setPicklistValues( pickListValues.toArray(new PicklistValue[pickListValues.size()]) );			
			cf.setPicklist(list);		
		} else if (type == FieldType.Text) {
			cf.setLength(255);
		}
	
		fields.add(cf);				
	}
	
	private CustomField buildCustomField(String fieldName, FieldType fieldType) {
		String devName = FieldCreatorHelperFunctions.getDevName(fieldName);
		
		log.info("Creating custom field");
		log.info("Field type: " + fieldType);
		log.info("Field label: " + fieldName);
		log.info("Field name: " + devName);
		
		CustomField cf = new CustomField();
		cf.setLabel(fieldName);
		cf.setFullName(objectName + "." + devName);
		cf.setType(fieldType);
		
		return cf;
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
			CustomField[] fieldArr = fields.toArray(new CustomField[fields.size()]);
			connection.sendToSalesforce(fieldArr);
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
