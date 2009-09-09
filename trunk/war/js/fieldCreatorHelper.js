// pull the values for the metadata server url, session id, and object name into the html form
// NO LONGER USED
var fillParams = function() {
	alert("fillParams is deprecated");
	var url = location.href;
	var params = url.slice(url.indexOf('?')+1).split('&');	
	for (i=0; i < params.length; i++) {
		var input = params[i].split('=');
		if($(input[0]) != null)
			$(input[0]).value = input[1];
	}
}

// display the label for the object being operated on
// NO LONGER USED
var setObjectName = function() {
	alert("set objectname is deprecated");
	$("currentObject").innerHTML = unescape($("objectLabel").value);
}

// set the change object url to return back to salesforce
// NO LONGER USED
var setChangeObjectUrl = function() {
	alert("setChangeObjectURL is deprecated");
	$("changeObjectLink").href=$("retUrl").value;
}

// set the focus on the first field type drop down
var startFocus = function() {
	$("field1Type").focus();
}

// disable input fields based on the field type
var fieldSelected = function(number) {
	var fieldType = $("field" + number + "Type").value;
	switch(fieldType) {
		case "text" :
		case "checkbox" :
		case "phone" :
		case "date" :
			$("field" + number + "Decimals").disabled = true;
			$("field" + number + "Decimals").style.background = "#D8D8D8";			
			$("field" + number + "Values").disabled = true;
			$("field" + number + "Values").style.background = "#D8D8D8";
			break;
		case "currency" :
		case "number" :
			$("field" + number + "Decimals").disabled = false;
			$("field" + number + "Decimals").style.background = "";
			$("field" + number + "Values").disabled = true;
			$("field" + number + "Values").style.background = "#D8D8D8";
			break;		
		case "picklist" :
			$("field" + number + "Decimals").disabled = true;
			$("field" + number + "Decimals").style.background = "#D8D8D8";
			$("field" + number + "Values").disabled = false;
			$("field" + number + "Values").style.background = "";
			break;
	}
}