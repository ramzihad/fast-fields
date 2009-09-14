package com.force.metadata.utils;

import java.util.logging.Logger;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.Connector;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class SalesforceConnection {

	private static final Logger log = Logger.getLogger(SalesforceConnection.class.getName());

	private MetadataConnection metadataConnection;	
	private String sessionId;
	private String endpointUrl;
	
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
}
