<!-- DEPRECATED, replaced by fieldCreatorSelector.jsp -->

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.force.FieldCreatorProperties" %>

<html>
<head>
<link type="text/css" rel="stylesheet" media="handheld,print,projection,screen,tty,tv" href="fieldCreator.css"/>
<script src="/fieldCreatorHelper.js"></script>
<script src="/gen_validatorv31.js" type="text/javascript"></script>
<script src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.0.3/prototype.js"></script>
</head>
<body onload="init();">
<script>
var init = function() {
	fillParams();
	setObjectName();
	setChangeObjectUrl();
	startFocus();
}
</script>
This page is deprecated.  Please email rcallaway@salesforce.com and tell him you reached test.jsp and saw this message.
<center>
<div id="container">
	
	<form id="myForm" action="fieldCreatorServlet">
		<div id="header">
			<a id="changeObjectLink" class="button" href="#" target="_top">Switch Object</a>
			<input type="submit" id="createFieldsButton" value="Create Fields"/>			
			<h3 style="text-align: left;">current object  <span id="currentObject"></span></h3>
		</div>
		<table style="text-align: center; margin: 0 auto;" cellpadding="5" rules="rows">
			<tbody id="insertTarget">
			<tr>
				<th>#</th>
				<th>Field Type</th>
				<th>Name</th>
				<th>Decimals<br/><small style="font-weight: normal; color: #666;">(number of zeros after decimal point)</small></th>
				<th>Values<br/><small style="font-weight: normal; color: #666;">(separate with ',')</small></th>
			</tr>
<% for (int i = 1; i <= FieldCreatorProperties.entryRows; i++ ) { %>
			<tr>
				<td><%= i %></td>
				<td>
					<select id="<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>" onchange="fieldSelected(<%= i %>);" name="<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>" size="1">
						<option value="text">Text</option>
						<option value="number">Number</option>
						<option value="currency">Currency</option>
						<option value="date">Date</option>
						<option value="checkbox">Checkbox</option>
						<option value="picklist">Picklist</option>
						<option value="phone">Phone</option>
					</select>			
				</td>
				<td><input type="text" class="textEntry" id="<%= FieldCreatorProperties.getNameIdForFieldNum(i) %>" maxlength="40" name="<%= FieldCreatorProperties.getNameIdForFieldNum(i) %>" /></td>
				<td><input type="text" disabled="true" style="background: #D8D8D8;" class="textEntry" id="<%= FieldCreatorProperties.getDecimalsIdForFieldNum(i) %>" name="<%= FieldCreatorProperties.getDecimalsIdForFieldNum(i) %>" /></td>
				<td><input type="text" disabled="true" style="background: #D8D8D8;" class="textEntry" id="<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>" name="<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>" /></td>
			</tr>
<% } %>
			</tbody>
		</table>
		<input type="hidden" id="srv" name="srv"/>
		<input type="hidden" id="sid" name="sid"/>
		<input type="hidden" id="objectName" name="objectName"/>
		<input type="hidden" id="objectLabel" name="objectLabel"/>
		<input type="hidden" id="retUrl" name="retUrl"/>		
	</form>
</div>
<script>
var frmvalidator = new Validator("myForm");

<% for (int i = 1; i <= FieldCreatorProperties.entryRows; i++ ) { %>

frmvalidator.addValidation("<%= FieldCreatorProperties.getDecimalsIdForFieldNum(i) %>","num");
frmvalidator.addValidation("<%= FieldCreatorProperties.getDecimalsIdForFieldNum(i) %>","lt=16");
frmvalidator.addValidation("<%= FieldCreatorProperties.getDecimalsIdForFieldNum(i) %>","gt=-1");
frmvalidator.addValidation("field1Name","maxlen=40");
/*function ValidatePicklistValues<%= i %>() {
	alert("Validating Function");	
	var frm = document.forms["myForm"];
	alert("frm.<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>.value");
	alert(frm.<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>.value);
	if (frm.<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>.value == "picklist")
	{
		alert("It's a picklist");
		
		if ( frm.<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>.value == "" ) 
		{
			alert("It doesn't have any values");
			sfm_show_error_msg("No values entered for picklist", frm.<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>);
			return false;
		}
	}
	
	return true;
}*/
//frmvalidator.setAddnlValidationFunction("ValidatePicklistValues<%= i %>");

<% } %>

function ValidatePicklistValues() {
	var frm = document.forms["myForm"];
	
	<% for (int i = 1; i <= FieldCreatorProperties.entryRows; i++ ) { %>
	
		if (frm.<%= FieldCreatorProperties.getTypeIdForFieldNum(i) %>.value == "picklist")
		{			
			if ( frm.<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>.value == "" ) 
			{		
				sfm_show_error_msg("No values entered for picklist <%= i %>", frm.<%= FieldCreatorProperties.getValuesIdForFieldNum(i) %>);
				return false;
			}
		}
	
	<% } %>
	
	else { return true; }
}
frmvalidator.setAddnlValidationFunction("ValidatePicklistValues");
</script>
</center>
</body>
</html>