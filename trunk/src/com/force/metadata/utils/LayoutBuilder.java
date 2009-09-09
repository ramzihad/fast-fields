package com.force.metadata.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.ws.ConnectionException;

public class LayoutBuilder {

	private static final Logger log = Logger.getLogger(LayoutBuilder.class.getName());
	private static final String SPLICE_SENTINEL = "XX__SPLICE__XX";

	private String objectName;
	private SalesforceConnection connection;
	private List<String> layouts;
	private String packageXml;
	private String packageXmlName;
	private List<String> customFieldsToAppend;
	private Map<String, String> layoutMap;
	
	private String status;
	private String error;
	
	public String getStatus() {
		return status;
	}
	
	public String getError() {
		return error;
	}
	
	private void handleError(String errorMessage, Exception e) {
		log.warning(errorMessage);
		status = "Error";
		error = errorMessage;
		e.printStackTrace();
	}
	
	private Boolean checkError() {
		return status.equals("Error");
	}
	
	public LayoutBuilder(String objectName, List<String> fields, SalesforceConnection connection) {
		this.objectName = objectName;
		this.customFieldsToAppend = fields;
		this.connection = connection;
		layoutMap = new HashMap<String, String>();		
		status = "Initialized";
	}
	
	public void buildLayouts() throws Exception {
		listLayouts();
		retrieveLayouts();
		if(checkError()) return;
		processLayouts();
		buildZipAndDeploy();
		if(checkError()) return;		
		status = "Success";
	}
	
	private void retrieveLayouts() throws IOException {
		
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(connection.retrieveLayouts(layouts));
		} catch (Exception e) {
			String errorMessage = "Exception (" + e.getClass().getCanonicalName() + ") during page layout deployment: " + e.getMessage();
			handleError(errorMessage, e);
			return;
		}

		ZipInputStream zis = new ZipInputStream(bais);
        ZipEntry entry;
        
        while((entry = zis.getNextEntry()) != null) {
        	if(!entry.getName().contains("package.xml")) {
        		layoutMap.put(entry.getName(),readData(zis).toString());
        	} else {
        		packageXml = readData(zis).toString();
        		packageXmlName = entry.getName();
        	}
        	zis.closeEntry();
        }
        
        bais.close();
        zis.close();
		
	}

	private void buildZipAndDeploy() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zout = new ZipOutputStream(baos);
		
		for(String pageLayout : layoutMap.keySet()) {
			byte[] input = layoutMap.get(pageLayout).getBytes();			
			ZipEntry entry = new ZipEntry(pageLayout);
			entry.setSize((long) input.length); 
			zout.putNextEntry(entry);
			zout.write(input);			
		}
		
		// add package.xml
		ZipEntry entry = new ZipEntry(packageXmlName);
		byte[] packageBytes = packageXml.getBytes();
		entry.setSize(packageBytes.length);
		zout.putNextEntry(entry);
		zout.write(packageXml.getBytes());
		zout.close();
		baos.flush();		
		
		try {
			connection.deployZipToSalesforce(baos.toByteArray());
		} catch (Exception e) {
			String errorMessage = "Exception (" + e.getClass().getCanonicalName() + ") during page layout deployment: " + e.getMessage();
			handleError(errorMessage, e);
		}		
	}
	
	private void processLayouts() throws IOException {
		for(String layoutMetadata : layoutMap.keySet()) {
			String readyForSplice = addSplicePoint(layoutMap.get(layoutMetadata));
			layoutMap.put(layoutMetadata, insertCustomFields(readyForSplice));
		}		
	}	
	
	// add xml entries for all custom fields just added
	private String insertCustomFields(String input) {
		String splice = new String();
		for(String s : customFieldsToAppend) {
			splice += "<layoutItems>\n" +
						"<behavior>Edit</behavior>\n" +
						"<field>" + s + "</field>\n" +
					  "</layoutItems>\n";
		}
		return input.replace(SPLICE_SENTINEL,splice);
	}
	
	// add a splice point to append the custom fields
	// place it in the information section if it exists
	// if not create a new information section	
	private String addSplicePoint(String input) {
		
		// try to find "<label>Information</label>", ideally we want to insert into a pre-existing section
		int informationSectionStart = input.indexOf("<label>Information</label>");
		
		// check for heading "ObjectName Information", some standard objects use this format
		if (informationSectionStart == -1)
			informationSectionStart = input.indexOf("<label>" + objectName + " Information</label>");
		
		if(informationSectionStart != -1) {
			// the information section exists find the end of the first column
			int endOfInformationColumn1 = input.indexOf("</layoutColumns>", informationSectionStart);
			
			// now find the index of the last layout item in that column
			int startOfLastColumn = input.substring(0,endOfInformationColumn1).lastIndexOf("</layoutItems>", endOfInformationColumn1);
			
			int splicePoint = startOfLastColumn + 14;
			
			String temp = input.substring(0,splicePoint);
			temp += "\n" + SPLICE_SENTINEL + "\n";
			temp += input.substring(splicePoint);
			System.out.println("Raw XML with splice point:");
			System.out.println(temp);
			return temp;
		}
		
		// no information section exists, find the start of the first section
		int startOfFirstSection = input.indexOf("<layoutSections>");
		
		String temp = input.substring(0, startOfFirstSection);
		temp += "\n<layoutSections>\n" +
					"<editHeading>true</editHeading>\n" +
					"<label>Information</label>\n" +
					"<layoutColumns>\n" +
						SPLICE_SENTINEL + "\n" +
					"</layoutColumns>\n" +
					"<layoutColumns/>\n" +
					"<style>TwoColumnsTopToBottom</style>\n" +
				"</layoutSections>\n";
		temp += input.substring(startOfFirstSection);
		
		return temp;
	}
	
	private void listLayouts() {
		layouts = new ArrayList<String>();

		log.info("Listing layouts for " + objectName);
		
		try {
			String component = "Layout";
	        String optionalFolder = null;
	        FileProperties[] layoutResult;
	        layoutResult = connection.listMetadata(component, optionalFolder);
	        
	        if (layoutResult != null) {
	            for (FileProperties n : layoutResult) {
	            	if(n.getFullName().startsWith(objectName + "-")) {
	            		layouts.add(n.getFullName());
	            	}
	            }
	        }            
		} catch (ConnectionException e) {			
			String errorMessage = "Connection Exception: " + e.getMessage();
			handleError(errorMessage, e);
		}

	}	
	
	private OutputStream readData(ZipInputStream zis) throws IOException {
		final int BUFFER_SIZE = 2;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = zis.read(buf, 0, BUFFER_SIZE)) != -1) {
            baos.write(buf, 0, bytesRead);            
        }

        return baos;
	}
	
}
