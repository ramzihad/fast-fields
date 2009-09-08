<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
<link type="text/css" rel="stylesheet" media="handheld,print,projection,screen,tty,tv" href="../css/fieldCreator.css"/>
<script src="../js/fieldCreatorHelper.js"></script>
<script src="../js/gen_validatorv31.js" type="text/javascript"></script>
<script src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.0.3/prototype.js"></script>
</head>
<body onload="init();">
<script>
var init = function() {
	fillParams();
}
</script>
<center>
<div id="container">
	<form id="myForm" action="../objectCreatorServlet">
		<div id="header">
			<input type="submit" id="createObjectButton" value="Create Object"/>			
		</div>
		<input type="text" id="objectName" name="objectName"/>
		<input type="hidden" id="srv" name="srv"/>
		<input type="hidden" id="sid" name="sid"/>		
		<input type="hidden" id="retUrl" name="retUrl"/>		
	</form>
</div>