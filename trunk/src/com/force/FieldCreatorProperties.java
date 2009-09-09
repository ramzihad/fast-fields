package com.force;

import java.util.ArrayList;
import java.util.List;

import com.force.metadata.utils.FieldCreator;

public class FieldCreatorProperties {
	
	// NB: careful changing url prop names as jsp won't automatically refactor 
	public static final int entryRows = 10;
	public static final String fieldCreatorURL = "/jsp/fieldCreatorSelector.jsp";
	public static final String fieldCreatorResult = "/jsp/fieldCreatorResult.jsp";
	public static final String fieldCreatorServlet = "/fieldCreatorServlet";
	
	public static final String layoutBuilderServlet = "/layoutBuilderServlet";
	public static final String layoutBuilderResult = "/jsp/layoutBuilderResult.jsp";
	
	public static final String objectCreator = "/jsp/objectCreator.jsp";
	public static final String objectCreatorServlet = "/objectCreatorServlet";
	public static final String objectCreatorResult = "/jsp/objectCreatorResult.jsp";
	
	public static final String demoComponentUrl = "https://na1.salesforce.com/a2x3000000000RW";
	
	public static String getTypeIdForFieldNum(int i) {
		return "field" + i + "Type";
	}	
	public static String getNameIdForFieldNum(int i) {
		return "field" + i + "Name";
	}
	public static String getDecimalsIdForFieldNum(int i) {
		return "field" + i + "Decimals";
	}
	public static String getValuesIdForFieldNum(int i) {
		return "field" + i + "Values";
	}
	
	public static String[] getFieldTypes() {
		return FieldCreator.getFieldTypes();
	}
	
	// TODO: this should probably be in a different class
	// encoded fields look like [(name=field1__c;label=Field1;type=number,...),(...),...]
	// This returns "name=field1__c;label=Field;type=number;..."
	public static String[] decodeFields(String encodedFieldString) {
	try {
		
		List<String> output = new ArrayList<String>();
		encodedFieldString = encodedFieldString.trim();
		
		// strip brackets
		encodedFieldString = encodedFieldString.substring(1,encodedFieldString.length()-1);
		
		// parse into individual fields and strip parentheses
		if(encodedFieldString.contains("|")) {
			String[] temp = encodedFieldString.split("\\|");
			for(String s : temp) {
				output.add(s.substring(1,s.length()-1));
			}
		} else {
			output.add(encodedFieldString.substring(1,encodedFieldString.length()-1));
		}
		
		return output.toArray(new String[output.size()]);
	} catch (Exception e) {
		System.out.println("Exception: " + e.getMessage());
		System.out.println("Encoded Field String: " + encodedFieldString);
		e.printStackTrace();
		return null;
	}
	}
	
}
