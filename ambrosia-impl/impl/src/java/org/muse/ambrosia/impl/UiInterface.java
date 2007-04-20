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

package org.muse.ambrosia.impl;

import java.io.PrintWriter;
import java.util.List;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.api.Interface;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiInterface is the top most container for each user interface.<br />
 * The interface title is rendered as the main (think window or frame) title.<br />
 * The interface has a header that is rendered in the display at the top.
 */
public class UiInterface extends UiContainer implements Interface
{
	/** The message selector and properties for the header. */
	protected Message header = null;

	/** If we want to disable browser auto-complete. */
	protected boolean noAutoComplete = false;

	/** The message selector and properties for the title. */
	protected Message title = null;

	/**
	 * {@inheritDoc}
	 */
	public Interface add(Controller controller)
	{
		super.add(controller);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		PrintWriter response = context.getResponseWriter();

		boolean fragment = ((Boolean) context.get(Context.FRAGMENT)).booleanValue();

		// start
		if (!fragment)
		{
			response
					.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
			response.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
			response.println("<head>");
			response.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");

			// include the portal's stuff in head (css and js links)
			String headInclude = (String) context.get("sakai.html.head");
			if (headInclude != null)
			{
				response.println(headInclude);
			}

			// our js
			response.println("<script type=\"text/javascript\" language=\"JavaScript\" src=\"/ambrosia_library/js/ambrosia.js\"></script>\n");

			// our css
			response.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"/ambrosia_library/skin/ambrosia.css\" />");

			// use our title
			// TODO: we might want to send in the placement title and deal with that...
			response.print("<title>");
			if (this.title != null)
			{
				response.print(Validator.escapeHtml(this.title.getMessage(context, focus)));
			}
			response.println("</title>");

			response.print("</head><body");

			// do the body the portal wants to do
			String onload = (String) context.get("sakai.html.body.onload");

			// if we didn't get an onload, add one for stand-alone
			if (onload == null)
			{
				onload = "setMainFrameHeight('');";
			}

			response.print(" onload=\"" + onload + "\"");

			response.println(">");
		}

		// for Safari, add a frame so that we don't get stuck in their back/forward cache (see
		// http://developer.apple.com/internet/safari/faq.html#anchor5)
		response.println("<iframe style=\"height:0px;width:0px;visibility:hidden\" src=\"about:blank\">");
		response.println("this frame prevents back forward cache in Safari");
		response.println("</iframe>");

		// pick a name for the form, and store this name in the context for other components to reference.
		String name = "form" + context.getUniqueId();
		context.setFormName(name);

		// wrap up in a form - back to the current destination
		String href = (String) context.get("sakai.destination.url");
		response.println("<div class=\"ambrosiaInterface\">");
		response.println("<form name=\"" + name + "\" method=\"post\" action=\"" + href
				+ "\" enctype=\"multipart/form-data\" onsubmit=\"return validate();\" " + (this.noAutoComplete ? "autocomplete=\"off\" " : "") + ">");

		// put in a hidden field that can be set with a tool destination (for use when submitting the form without a normal submit
		// button
		response.println("<input type=\"hidden\" name =\"" + "destination_" + "\" value=\"\" />");

		// header, if defined
		if (this.header != null)
		{
			response.println("<div class=\"ambrosiaInterfaceHeader\">" + Validator.escapeHtml(this.header.getMessage(context, focus)) + "</div>");
		}

		// render the contained
		for (Controller c : this.contained)
		{
			c.render(context, focus);
		}

		// end
		response.println("</form></div>");

		// add validation method
		response.println("<script language=\"JavaScript\">");
		response.println("var enableValidate=false;");
		response.println("function validate()");
		response.println("{");
		response.println("  if (!enableValidate) return true;");
		response.println("  var rv=true;");

		String validation = context.getValidation();
		if (validation != null)
		{
			response.println(validation);
		}

		// reset the enableValidate
		response.println("  enableValidate=false;\n");

		response.println("  return rv;");
		response.println("}");

		// add a variable that controllers can use to set / test if we have submitted already
		response.println("var submitted=false;");

		// add any other script we have accumulated
		String script = context.getScript();
		if (script != null)
		{
			response.println(script);
		}

		// and any focus path ids
		List<String> focusIds = context.getFocusIds();
		if ((focusIds != null) && (!focusIds.isEmpty()))
		{
			StringBuffer buf = new StringBuffer();
			for (String id : focusIds)
			{
				buf.append("\"" + id + "\",");
			}
			buf.setLength(buf.length() - 1);

			response.println("focus_path = [" + buf.toString() + "];");
		}

		response.println("</script>");

		// the blocker for disabling the screen after a navigation
		// Note: set to 0, can set to like 0.1 for a visual effect, perhaps with a color of #BBBBBB for a very slight visual effect
		// -ggolden
		// response
		// .println("<div id=\"blocker\" style=\"width:100%; height:100%; position:absolute; left:0px; top:0px; background-color:#000000; opacity:0;
		// display:none;\"></div>");

		if (!fragment)
		{
			response.println("</body></html>");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Interface setHeader(String selector, PropertyReference... references)
	{
		this.header = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Interface setNoAutoComplete()
	{
		this.noAutoComplete = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Interface setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}
}
