package com.force.metadata.utils;

public class FieldCreatorHelperFunctions {

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
	
	// create metadata api url from pod
	// https://na1.api.salesforce.com/services/Soap/m/15.0/00D30000000YXDL
	@Deprecated
	public static String getMetadataEndpointUrl(String pod, String organizationId) {
		return "https://" + pod + ".api.salesforce.com/services/Soap/" + organizationId;
	}
	
}
