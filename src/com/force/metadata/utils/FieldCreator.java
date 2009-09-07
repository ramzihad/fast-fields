package com.force.metadata.utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.MetadataConnection;
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
		String devName = getDevName(fieldName);
		
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
	
	// remove all non alpha numberic characters
	// replace spaces with underscores
	// add an X if it starts with a number
	public static String getDevName(String input) {
		input = input.trim();
		input = input.replaceAll("[^a-zA-Z0-9\\s]", "");
		input = input.replaceAll(" ", "_");
		input = input.replaceAll("^[0-9]","X" + input.substring(0,1));
		
		// check for double underscores
		input = input.replaceAll("[_]+", "_");
		
		// remove trailing underscores
		input = input.replaceAll("[_]+$", "");
		
		input += "__c";
		
		return input;
	}
	
	public void sendToSalesforce() {

		MetadataConnection connection;
		// create connection
		log.info("Creating Salesforce Metadata Connection...");
		try {
			connection = ConnectionManager.getMetadataConnection(sessionId, endpointUrl);			
		} catch (ConnectionException e) {
			log.warning("Connection Exception with salesforce: " + e.getMessage());
			e.printStackTrace();
			return;
		}
			
		log.info("Connected to Salesforce");
		
		// go through the fields array and do the metatdata api call
		AsyncResult[] ars;
		log.info("Creating new custom fields...");
		
		try {
			CustomField[] fieldPackage = fields.toArray(new CustomField[fields.size()]);
			ars = connection.create(fieldPackage);
		
			log.info("Metadata sent to salesforce.  Checking status...");
						
            String[] ids = new String[ars.length];
            for(int i = 0; i < ars.length; i++) {
            	ids[i] = ars[i].getId();
            }
            boolean done = false;
            long waitTimeMilliSecs = 1000;
            AsyncResult[] arsStatus = null;
            
            while (!done) {
            	
                arsStatus = connection.checkStatus(ids);
                
                // check that all fields have been created and log the current status
                done = true;
                String[] statusArr = new String[arsStatus.length];
                for(int i = 0; i < arsStatus.length; i++) {
                	if(done && !arsStatus[i].isDone()) {
                		done = false;
                	}
                	
                	statusArr[i] = arsStatus[i].getState().toString();                	
                	if (arsStatus[i].getStatusCode() != null )  {
                        log.warning("Field creation failed. Reason: " + arsStatus[i].getMessage());
                        status="Error";
                        error="Field creation failed.  Reason: " + arsStatus[i].getMessage();                   
                        return;
                    }
                }                
                
                Thread.sleep(waitTimeMilliSecs);
                
                // increase sleep time to max of 5 seconds
                waitTimeMilliSecs = (long) (waitTimeMilliSecs * 1.5 > 5000 ? 5000 : waitTimeMilliSecs * 1.5);
                String stateMessage = "Current Transaction States: ";
                for(int i = 0; i < statusArr.length; i++) {
                	stateMessage += statusArr[i] + ", ";
                }
				log.info(stateMessage);
            }
            status="Success";
                  
		} catch (ConnectionException e) {
			status="Error";
			error="Connection Exception: " + e.getMessage();
			log.warning("There was a connection exception: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			status="Error";
			error="Interrupted Exception: " + e.getMessage();
			log.warning("There was a interrupted exception: " + e.getMessage());
			e.printStackTrace();
		}		
	}
}
