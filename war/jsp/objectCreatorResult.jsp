<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.force.FieldCreatorProperties" %>

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
			<a id="runAgainLink" class="button" href="..<%= FieldCreatorProperties.fieldCreatorURL %>">Add Fields</a>
			<h3 style="text-align: left;">current object  <span id="currentObject"><%= (String) session.getAttribute("objectLabel") %></span></h3>
		</div>

<% if ( request.getAttribute("status") == "Success" ) { %>
	<h1 style="color: green">Success!!</h1><br/>
<% } else if ( request.getAttribute("status") == "Error" ) { %>
	<h1 style="color: red">Uh-oh, there was an error!!</h1>
	<p><%= request.getAttribute("error") %></p>
<% } else { %>
	<h1 style="color: yellow">Hmm, I didn't get a status message.</h1>
	<p>Maybe I worked, try checking see if the action completed.  Otherwise try again.
	If that still doesn't work email rcallaway@salesforce.com and let him know about the error so he can fix it.</p>
<% } %>
		
	</div>
</div>
</center>
</body>
</html>
	