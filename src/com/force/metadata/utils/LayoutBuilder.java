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

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;
import com.sforce.ws.ConnectionException;

public class LayoutBuilder {

	private static final Logger log = Logger.getLogger(LayoutBuilder.class.getName());
	private static final double API_VERSION = 15.0; 
	private static final long ONE_SECOND = 1000;
	private static final int MAX_NUM_POLL_REQUESTS = 50;
	private static final String SPLICE_SENTINEL = "XX__SPLICE__XX";

	private String objectName;
	private MetadataConnection connection = null;
	private List<String> layouts;
	//private List<OutputStream> rawLayoutMetadatum;
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
	
	private void setError(String message) {
		status = "Error";
		error = message;
	}
	
	public LayoutBuilder(String sessionId, String endpointUrl, String objectName, List<String> fields) throws Exception {
		status = "Initialized";
		error = "";
		this.objectName = objectName;
		this.customFieldsToAppend = fields;
		layoutMap = new HashMap<String, String>();
		
		try {
			connection = ConnectionManager.getMetadataConnection(sessionId, endpointUrl);
		} catch (ConnectionException e) {
			setError("Connection Exception: " + e.getMessage());
			log.warning("Connection Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void buildLayouts() throws Exception {
		listLayouts();
		retrieveLayouts();
		processLayouts();
		buildZipAndDeploy();
		status = "Success";
	}
	
	private void buildZipAndDeploy() throws Exception {
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
		//testZipCreation(baos);
		deployZipToSalesforce(baos.toByteArray());
		
	}
	
	private void deployZipToSalesforce(byte[] zipBytes) throws Exception {
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
		
        AsyncResult asyncResult = connection.deploy(zipBytes, deployOptions);
        
        // Wait for the deploy to complete
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
            	setError("Request timed out.  Give it another try.");
                throw new Exception("Request timed out. If this is a large set " +
                        "of metadata components, check that the time allowed by " +
                        "MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = connection.checkStatus(
                    new String[] {asyncResult.getId()})[0];
            System.out.println("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
        	setError(asyncResult.getStatusCode() + " msg: " + asyncResult.getMessage());
        	throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        DeployResult result = connection.checkDeployStatus(asyncResult.getId());
        if (!result.isSuccess()) {
            setError(printErrors(result));            
            throw new Exception("The files were not successfully deployed");
        }
	}
	
    /**
     * Print out any errors, if any, related to the deploy.
     * @param result - DeployResult
     */
    private String printErrors(DeployResult result)
    {
        DeployMessage messages[] = result.getMessages();
        StringBuilder buf = new StringBuilder("Failures:\n");
        for (DeployMessage message : messages) {
            if (!message.isSuccess()) {
                String loc = (message.getLineNumber() == 0 ? "" :
                    ("(" + message.getLineNumber() + "," +
                            message.getColumnNumber() + ")"));
                if (loc.length() == 0
                        && !message.getFileName().equals(message.getFullName())) {
                    loc = "(" + message.getFullName() + ")";
                }
                buf.append(message.getFileName() + loc + ":" +
                        message.getProblem()).append('\n');
            }
        }
        RunTestsResult rtr = result.getRunTestResult();
        if (rtr.getFailures() != null) {
            for (RunTestFailure failure : rtr.getFailures()) {
                String n = (failure.getNamespace() == null ? "" :
                    (failure.getNamespace() + ".")) + failure.getName();
                buf.append("Test failure, method: " + n + "." +
                        failure.getMethodName() + " -- " +
                        failure.getMessage() + " stack " +
                        failure.getStackTrace() + "\n\n");
            }
        }
        if (rtr.getCodeCoverageWarnings() != null) {
            for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
                buf.append("Code coverage issue");
                if (ccw.getName() != null) {
                    String n = (ccw.getNamespace() == null ? "" :
                        (ccw.getNamespace() + ".")) + ccw.getName();
                    buf.append(", class: " + n);
                }
                buf.append(" -- " + ccw.getMessage() + "\n");
            }
        }
        
        System.out.println(buf.toString());
        return buf.toString();
    }

	
//	private void testZipCreation(ByteArrayOutputStream zipOutput) throws IOException {
//        
//		// Parse zip file
//        System.out.println("Testing output zip file");
//        ByteArrayInputStream bais = new ByteArrayInputStream(zipOutput.toByteArray());
//        ZipInputStream zis = new ZipInputStream(bais);
//
//        ZipEntry entry;
//        while((entry = zis.getNextEntry()) != null) {
//        	System.out.println("Entry Name: " + entry.getName());
//        	//System.out.println("Zip Entry:");
//        	System.out.println(readData(zis).toString());
//        	zis.closeEntry();
//        }
//	}
	
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
	        ListMetadataQuery query = new ListMetadataQuery();
	        query.setFolder(optionalFolder);
	        query.setType(component);
	        
	        FileProperties[] lmr;
			
			lmr = connection.listMetadata(
			    	new ListMetadataQuery[] {query});
			
	        if (lmr != null) {
	            for (FileProperties n : lmr) {
	            	if(n.getFullName().startsWith(objectName + "-")) {
	            		layouts.add(n.getFullName());
	            	}
	            }
	        }            
		} catch (ConnectionException e) {
			setError("Connection Exception: " + e.getMessage());
			log.warning("Connection Exception: " + e.getMessage());
			e.printStackTrace();
		}

	}
	
	private void retrieveLayouts() throws Exception {
		
		PackageTypeMembers[] packageMembers = new PackageTypeMembers[1];
		packageMembers[0] = new PackageTypeMembers();
		packageMembers[0].setName("Layout");
		packageMembers[0].setMembers( layouts.toArray(new String[layouts.size()]));
		
		Package layoutManifest = new Package();
		layoutManifest.setVersion(String.valueOf(API_VERSION));
		layoutManifest.setTypes(packageMembers);
		
		RetrieveRequest retrieveRequest = new RetrieveRequest();
		retrieveRequest.setApiVersion(API_VERSION);
		retrieveRequest.setUnpackaged(layoutManifest);
		
		AsyncResult helloRequest = null;
		
		try {
			helloRequest = connection.retrieve(retrieveRequest);
		} catch (ConnectionException e) {
			setError("Connection Exception: " + e.getMessage());
			log.warning("Connection Exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Wait for the retrieve to complete
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!helloRequest.isDone()) {
            try {
				Thread.sleep(waitTimeMilliSecs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
            	setError("Request timed out.  Give it another try.");
                throw new Exception("Request timed out.  If this is a large set " +
                		"of metadata components, check that the time allowed " +
                		"by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            helloRequest = connection.checkStatus(
            		new String[] {helloRequest.getId()})[0];
            System.out.println("Status is: " + helloRequest.getState());
        }

        if (helloRequest.getState() != AsyncRequestState.Completed) {
            throw new Exception(helloRequest.getStatusCode() + " msg: " +
                    helloRequest.getMessage());
        }

        RetrieveResult result = connection.checkRetrieveStatus(helloRequest.getId());
        
        // Print out any warning messages
        StringBuilder buf = new StringBuilder();
        if (result.getMessages() != null) {
            for (RetrieveMessage rm : result.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem());
            }
        }
        if (buf.length() > 0) {
        	setError("Retrive warnings: " + buf);
            System.out.println("Retrieve warnings:\n" + buf);
        }
        
        // Parse zip file
        System.out.println("Parsing zip files");
        ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());
        ZipInputStream zis = new ZipInputStream(bais);

        //rawLayoutMetadatum = new ArrayList<OutputStream>();
        ZipEntry entry;
        while((entry = zis.getNextEntry()) != null) {
        	if(!entry.getName().contains("package.xml")) {
        		//rawLayoutMetadatum.add(readData(zis));
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
