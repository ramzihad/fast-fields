package com.force.metadata.utils;

import com.sforce.soap.metadata.Connector;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

@Deprecated
public class ConnectionManager {
	
	@Deprecated
	public static MetadataConnection getMetadataConnection(String sessionId, String endpointUrl) throws ConnectionException {
		// create connection
		MetadataConnection connection;
		ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(endpointUrl);
		config.setSessionId(sessionId);
		connection = Connector.newConnection(config);
		return connection;	
		
	}
	
}
