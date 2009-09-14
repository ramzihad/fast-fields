<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.force.FieldCreatorProperties" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.util.List" %>
<%@ page import="java.lang.String" %>

<html>
<head>
<link type="text/css" rel="stylesheet" media="handheld,print,projection,screen,tty,tv" href="../css/fieldCreator.css"/>
<script src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.0.3/prototype.js"></script>
</head>
<body>
<center>
<div id="container">
	<div id="myForm" action="../fieldCreatorServlet">
		<div id="header">
			<a id="changeObjectLink" class="button" href="<%= request.getParameter("retUrl") %>" target="_top">Switch Object</a>
			<a id="runAgainLink" class="button" href="<%= request.getAttribute("fieldCreatorUrl") %>">Create More Fields</a>			
			<h3 style="text-align: left;">current object  <span id="currentObject"><%= URLDecoder.decode(request.getParameter("objectLabel")) %></span></h3>
		</div>

<% if ( request.getAttribute("status") == "Success" ) { %>
	<h1 style="color: green">Success!!</h1><br/>
	<a id="addToPageLayoutLink" class="button" href="<%= request.getAttribute("layoutBuilderUrl") %>">Add Fields to All Page Layouts</a>
<% } else if ( request.getAttribute("status") == "Error" ) { %>
	<h1 style="color: red">Uh-oh, there was an error!!</h1>
	<p><%= request.getAttribute("error") %></p>
<% } else { %>
	<h1 style="color: yellow">Hmm, I didn't get a status message.</h1>
	<p>Maybe I worked, try checking the object setup to see if the fields were created.  Otherwise try again.
	If that still doesn't work email rcallaway@salesforce.com and let him know about the error so he can fix it.</p>
<% } %>

<%
// TODO: need to move the decoding into a jsp helper class
String rawEncodedFields = (String) request.getAttribute("fields"); 

String[] encodedFields = FieldCreatorProperties.decodeFields(rawEncodedFields);

for(String field : encodedFields) {
%>			
	<p><ul>
<%
	String[] attributes = field.split(";");
	for(String attribute : attributes) {
		String[] nameValuePair = attribute.split("=");
		String name = nameValuePair[0];
		String value = nameValuePair[1];
%>
	<li><span class="name"><%= name %>: </span><%= value %></li>
<%
	} 
%>
	</ul></p>
<%
}
%>
	</div>
</div>
</center>
</body>
</html>