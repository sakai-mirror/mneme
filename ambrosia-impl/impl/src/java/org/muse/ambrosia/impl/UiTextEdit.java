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

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.TextEdit;
import org.sakaiproject.util.Validator;

/**
 * UiTextEdit presents a text input for the user to edit.
 */
public class UiTextEdit extends UiController implements TextEdit
{
	/** The number of columns per row for the box. */
	protected int numCols = 50;

	/** The number of rows for the text box. */
	protected int numRows = 4;

	/** The decision to control the onEmptyAlert. */
	protected Decision onEmptyAlertDecision = null;

	/** The message for the onEmptyAlert. */
	protected Message onEmptyAlertMsg = null;

	/**
	 * The PropertyReference for encoding and decoding this selection - this is what will be updated with the end-user's text edit,
	 * and what value seeds the display.
	 */
	protected PropertyReference propertyReference = null;

	/** The property reference to provide the read only setting. */
	protected PropertyReference readOnlyReference = null;

	/** The message that will provide title text. */
	protected Message titleMessage = null;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		// read only?
		boolean readOnly = false;
		if (this.readOnlyReference != null)
		{
			String value = this.readOnlyReference.read(context, focus);
			if (value != null)
			{
				readOnly = Boolean.parseBoolean(value);
			}
		}

		// alert if empty at submit?
		boolean onEmptyAlert = false;
		if (this.onEmptyAlertMsg != null)
		{
			onEmptyAlert = true;
			if (this.onEmptyAlertDecision != null)
			{
				onEmptyAlert = this.onEmptyAlertDecision.decide(context, focus);
			}
		}

		PrintWriter response = context.getResponseWriter();

		// set some ids
		int idRoot = context.getUniqueId();
		String id = this.getClass().getSimpleName() + "_" + idRoot;
		String decodeId = "decode_" + idRoot;

		// read the current value
		String value = "";
		if (this.propertyReference != null)
		{
			value = this.propertyReference.read(context, focus);
		}

		// single line
		if (numRows == 1)
		{
			if (onEmptyAlert)
			{
				// this will become visible if a submit happens and the validation fails
				response.println("<div class=\"ambrosiaAlert\" style=\"display:none\" id=\"alert_" + id + "\">"
						+ Validator.escapeHtml(this.onEmptyAlertMsg.getMessage(context, focus)) + "</div>");

				// this marks the field as required
				// response.println("<span class=\"reqStarInline\">*</span>");
			}

			// title
			if (this.titleMessage != null)
			{
				response.println("<p class=\"ambrosiaTextEdit sludgeTextEditSingle\">");
				response.println("<label for=\"" + id + "\">");
				response.println(Validator.escapeHtml(this.titleMessage.getMessage(context, focus)));
				response.println("</label>");
			}

			response.println("<input type=\"text\" id=\"" + id + "\" name=\"" + id + "\" size=\"" + Integer.toString(numCols) + "\" value=\""
					+ Validator.escapeHtml(value) + "\"" + (readOnly ? " disabled=\"disabled\"" : "") + " />");
			response.println("</p>");
		}

		// or multi line
		else
		{
			if (onEmptyAlert)
			{
				// this will become visible if a submit happens and the validation fails
				response.println("<div class=\"ambrosiaAlert\" style=\"display:none\" id=\"alert_" + id + "\">"
						+ Validator.escapeHtml(this.onEmptyAlertMsg.getMessage(context, focus)) + "</div>");

				// this marks the field as required
				// response.println("<span class=\"reqStarInline\">*</span>");
			}

			if (this.titleMessage != null)
			{
				response.println("<div class=\"ambrosiaTextEdit sludgeTextEditMultiple\">");
				response.println("<label class=\"block\" for=\"" + id + "\">");
				response.println(Validator.escapeHtml(this.titleMessage.getMessage(context, focus)));
				response.println("</label>");
			}

			response.println("<textarea id=\"" + id + "\" name=\"" + id + "\" cols=" + Integer.toString(numCols) + " rows="
					+ Integer.toString(numRows) + (readOnly ? " disabled=\"disabled\"" : "") + ">");
			response.print(Validator.escapeHtmlTextarea(value));
			response.println("</textarea>");
			response.println("</div>");

		}

		// the decode directive
		if ((this.propertyReference != null) && (!readOnly))
		{
			response.println("<input type=\"hidden\" name=\"" + decodeId + "\" value =\"" + id + "\" />"
					+ "<input type=\"hidden\" name=\"" + "prop_" + decodeId + "\" value=\""
					+ this.propertyReference.getFullReference(context) + "\" />");
		}

		// for onEmptyAlert, add some client-side validation
		if ((onEmptyAlert) && (!readOnly))
		{
			context.addValidation("	if (trim(document.getElementById('" + id + "').value) == \"\")\n" + "	{\n"
					+ "		if (document.getElementById('alert_" + id + "').style.display == \"none\")\n" + "		{\n"
					+ "			document.getElementById('alert_" + id + "').style.display = \"\";\n" + "			rv=false;\n" + "		}\n"
					+ "	}\n");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TextEdit setOnEmptyAlert(Decision decision, String selector, PropertyReference... references)
	{
		this.onEmptyAlertDecision = decision;
		this.onEmptyAlertMsg = new UiMessage().setMessage(selector, references);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public TextEdit setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public TextEdit setReadOnly(PropertyReference reference)
	{
		this.readOnlyReference = reference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public TextEdit setSize(int rows, int cols)
	{
		this.numRows = rows;
		this.numCols = cols;

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public TextEdit setTitle(String selector, PropertyReference... references)
	{
		this.titleMessage = new UiMessage().setMessage(selector, references);
		return this;
	}
}
