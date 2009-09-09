package com.force.metadata.utils;

import java.util.List;
import java.util.logging.Logger;

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.Connector;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class SalesforceConnection {

	private static final Logger log = Logger.getLogger(SalesforceConnection.class.getName());
	private static final double API_VERSION = 15.0;
	private static final int ONE_SECOND = 1000;

	private MetadataConnection metadataConnection;	
	private String sessionId;
	private String endpointUrl;

	@Deprecated
	// need this during transition to salesforceConnection object
	public MetadataConnection getCon() {
		return metadataConnection;
	}
	
	public SalesforceConnection(String sessionId, String endpointUrl) {
		this.sessionId = sessionId;
		this.endpointUrl = endpointUrl;
	}
	
	public void createConnection() throws ConnectionException {
		ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(endpointUrl);
		config.setSessionId(sessionId);
		metadataConnection = Connector.newConnection(config);
	}
	
	public void sendToSalesforce(Metadata[] components) throws Exception {
	
		checkConnection();
		
		AsyncResult[] ars;
			
		ars = metadataConnection.create(components);
	
		log.info("Metadata sent to salesforce.  Checking status...");
					
        String[] ids = new String[ars.length];
        for(int i = 0; i < ars.length; i++) {
        	ids[i] = ars[i].getId();
        }
        boolean done = false;
        long waitTimeMilliSecs = 1000;
        AsyncResult[] arsStatus = null;
        
        while (!done) {
        	
            arsStatus = metadataConnection.checkStatus(ids);
            
            // check that all components have been created
            done = true;
            String[] statusArr = new String[arsStatus.length];
            for(int i = 0; i < arsStatus.length; i++) {
            	if(done && !arsStatus[i].isDone()) {
            		done = false;
            	}
            	
            	statusArr[i] = arsStatus[i].getState().toString();                	
            	if (arsStatus[i].getStatusCode() != null )  {
                    Exception e = new Exception("Metadata creation failed.  Reason: " + arsStatus[i].getMessage());
            		throw e;
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
	}
	
	public void deployZipToSalesforce(byte[] zipBytes) throws Exception {
		
		checkConnection();
		
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
		
        AsyncResult asyncResult = metadataConnection.deploy(zipBytes, deployOptions);
        
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            asyncResult = metadataConnection.checkStatus(
                    new String[] {asyncResult.getId()})[0];
            log.info("Status is: " + asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
        	throw new Exception(asyncResult.getStatusCode() + " msg: " +
                    asyncResult.getMessage());
        }

        DeployResult result = metadataConnection.checkDeployStatus(asyncResult.getId());
        if (!result.isSuccess()) {
            throw new Exception("The files were not successfully deployed." + getDeployErrorString(result));
        }
	}
	
	/**
     * Print out any errors, if any, related to the deploy.
     * @param result - DeployResult
     */
    private String getDeployErrorString(DeployResult result)
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
        return buf.toString();
    }
	
	private void checkConnection() throws ConnectionException {
		if(null == metadataConnection) {
			createConnection();
		}
	}
	
	public FileProperties[] listMetadata(String component, String folder) throws ConnectionException {
		ListMetadataQuery query = new ListMetadataQuery();
        query.setFolder(folder);
        query.setType(component);
        
        FileProperties[] lmr;
		
		lmr = metadataConnection.listMetadata(
		    	new ListMetadataQuery[] {query});
		
		return lmr;
	}
	
	public byte[] retrieveLayouts(List<String> layouts) throws Exception {
		
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
		
		helloRequest = metadataConnection.retrieve(retrieveRequest);
		
        long waitTimeMilliSecs = ONE_SECOND;
        while (!helloRequest.isDone()) {
            try {
				Thread.sleep(waitTimeMilliSecs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
            helloRequest = metadataConnection.checkStatus(
            		new String[] {helloRequest.getId()})[0];
            log.info("Status is: " + helloRequest.getState());
        }

        if (helloRequest.getState() != AsyncRequestState.Completed) {
            throw new Exception(helloRequest.getStatusCode() + " msg: " +
                    helloRequest.getMessage());
        }

        RetrieveResult result = metadataConnection.checkRetrieveStatus(helloRequest.getId());
        
        // Capture any warning messages
        StringBuilder buf = new StringBuilder();
        if (result.getMessages() != null) {
            for (RetrieveMessage rm : result.getMessages()) {
                buf.append(rm.getFileName() + " - " + rm.getProblem());
            }
        }
        if (buf.length() > 0) {
        	throw new Exception("Retrive warnings: " + buf);            
        }
        
        return result.getZipFile();
	}
}
