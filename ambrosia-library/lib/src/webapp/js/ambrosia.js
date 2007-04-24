/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Regents of the University of Michigan & Foothill College, ETUDES Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

// functions for Ambrosia applications

function trim(s)
{
	return s.replace(/^\s+/g, "").replace(/\s+$/g, "");
}

function showConfirm(name)
{
	el = document.getElementById(name);
	if (el.style.display == "none")
	{
		el.style.left = ((document.body.scrollWidth / 2) - (parseInt(el.style.width) / 2)) + "px";
		el.style.top = (-1 * (parseInt(el.style.height) + 10)) + "px";
	}
	el.style.display = "";

	if (parseInt(el.style.top) < -10)
	{
		el.style.top = (parseInt(el.style.top) + 10) + "px";
		setTimeout("showConfirm('" + name + "')",10);
	}
	else
	{
		el.style.top = "0px";
	}
}

var confirmedAction="";

function hideConfirm(name, action)
{
	if (action != "") confirmedAction = action;
	el = document.getElementById(name);

	if (parseInt(el.style.top) > (-1 * (parseInt(el.style.height))))
	{
		el.style.top = (parseInt(el.style.top) - 10) + "px";
		setTimeout("hideConfirm('" + name + "','')",10);
	}
	else
	{
		el.style.top = (-1 * (parseInt(el.style.height) + 10)) + "px"
		el.style.display = "none";
		eval(confirmedAction);
		confirmedAction="";
	}
}
