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
import java.util.Collection;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.FillIn;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.TextEdit;
import org.sakaiproject.util.Validator;

/**
 * UiFillIn presents a set of text inputs for the user to edit embedded in a surrounding string. The string is formatted with "{}" where the fill-ins
 * are expected.<br />
 * The values are taken from / returned to an array property by reference.
 */
public class UiFillIn extends UiController implements FillIn
{
	/** The decision to include the correct marking. */
	protected Decision[] correctDecision = null;

	/** The icon to use to mark correct parts. */
	protected String correctIcon = null;

	/** The message key for the alt-text for correct icon. */
	protected String correctMessage = null;

	/** The PropertyReference for getting a correctness flag for each part. */
	protected PropertyReference correctsReference = null;

	/** The decision that controls if the field should get on-load focus. */
	protected Decision focusDecision = null;

	/** The icon to use to mark incorrect parts. */
	protected String incorrectIcon = null;

	/** The message key for the alt-text for incorrect icon. */
	protected String incorrectMessage = null;

	/** The number of columns per row for each text input. */
	protected int numCols = 50;

	/** The decision to control the onEmptyAlert. */
	protected Decision onEmptyAlertDecision = null;

	/** The message for the onEmptyAlert. */
	protected Message onEmptyAlertMsg = null;

	/**
	 * The PropertyReference for encoding and decoding this selection - this is what will be updated with the end-user's text edit, and what value
	 * seeds the display.
	 */
	protected PropertyReference propertyReference = null;

	/** The property reference to provide the read only setting. */
	protected PropertyReference readOnlyReference = null;

	/** The message that will provide fill-in text. */
	protected Message textMessage = null;

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

		// get some ids
		int idRoot = context.getUniqueId();
		String id = this.getClass().getSimpleName() + "_" + idRoot;
		String decodeId = "decode_" + idRoot;

		PrintWriter response = context.getResponseWriter();

		if (onEmptyAlert)
		{
			// this will become visible if a submit happens and the validation fails
			response.println("<div class=\"ambrosiaAlert\" style=\"display:none\" id=\"alert_" + id + "\">"
					+ Validator.escapeHtml(this.onEmptyAlertMsg.getMessage(context, focus)) + "</div>");
		}

		// title
		if (this.titleMessage != null)
		{
			response.println(Validator.escapeHtml(this.titleMessage.getMessage(context, focus)) + "<br />");
		}

		// read the text
		String fillInText = null;
		String[] fillInParts = null;
		if (this.textMessage != null)
		{
			fillInText = this.textMessage.getMessage(context, focus);
			if (fillInText != null)
			{
				fillInParts = fillInText.split("\\{\\}");
			}
		}

		// there's a hole between each part... and at the ends if the text starts / ends with the "{}" pattern

		// read the current values - we want a String[]
		String[] values = null;
		if (this.propertyReference != null)
		{
			Object o = this.propertyReference.readObject(context, focus);
			if (o != null)
			{
				if (o.getClass().isArray())
				{
					values = (String[]) o;
				}

				else if (o instanceof Collection)
				{
					values = (String[]) ((Collection) o).toArray(new String[0]);
				}
			}
		}

		// the correct marking decision
		boolean correctMarkingIncluded = true;
		if (this.correctDecision != null)
		{
			for (Decision decision : this.correctDecision)
			{
				if (!decision.decide(context, focus))
				{
					correctMarkingIncluded = false;
					break;
				}
			}
		}

		// read the correct flags - we want a Boolean[]
		Boolean[] corrects = null;
		if (correctMarkingIncluded && (this.correctsReference != null))
		{
			Object o = this.correctsReference.readObject(context, focus);
			if (o != null)
			{
				if (o.getClass().isArray())
				{
					corrects = (Boolean[]) o;
				}

				else if (o instanceof Collection)
				{
					corrects = (Boolean[]) ((Collection) o).toArray(new Boolean[0]);
				}
			}
		}

		// count the boxes
		int boxCount = 0;

		// put out the text and inputs
		if ((fillInParts != null) && (fillInParts.length > 0))
		{
			for (int i = 0; i < fillInParts.length - 1; i++)
			{
				// text
				if (fillInParts[i].length() > 0)
				{
					response.print(fillInParts[i]);
				}

				// if marked correct, flag with an icon
				if (correctMarkingIncluded && (corrects != null) && (corrects.length > i))
				{
					if ((corrects[i] != null) && (corrects[i].booleanValue()))
					{
						if (this.correctIcon != null)
						{
							response.print("<img src=\"" + context.get("sakai.return.url") + this.correctIcon + "\" alt=\""
									+ ((this.correctMessage != null) ? context.getMessages().getString(this.correctMessage) : "") + "\" />");
						}
					}

					else
					{
						if (this.incorrectIcon != null)
						{
							response.print("<img src=\"" + context.get("sakai.return.url") + this.incorrectIcon + "\" alt=\""
									+ ((this.incorrectMessage != null) ? context.getMessages().getString(this.incorrectMessage) : "") + "\" />");
						}
					}
				}

				// input
				response.print("<input type=\"text\" name=\"" + id + "\" id=\"" + id + Integer.toString(i) + "\" size=\""
						+ Integer.toString(this.numCols) + "\" value=\"");
				boxCount++;
				if ((values != null) && (values.length > i))
				{
					response.print(Validator.escapeHtml(values[i]));
				}
				response.print("\"" + (readOnly ? " disabled=\"disabled\"" : "") + " />");
			}

			// the last text
			if (fillInParts[fillInParts.length - 1].length() > 0)
			{
				response.print(fillInParts[fillInParts.length - 1]);
			}

			// with an input if we have a trailing pattern
			if (fillInText.endsWith("{}"))
			{
				// if marked correct, flag with an icon
				if (correctMarkingIncluded && (corrects != null) && (corrects.length > (fillInParts.length - 1)) && (this.correctIcon != null))
				{
					if ((corrects[fillInParts.length - 1] != null) && (corrects[fillInParts.length - 1].booleanValue()))
					{
						response.print("<img src=\"" + context.get("sakai.return.url") + this.correctIcon + "\" alt=\""
								+ ((correctMessage != null) ? context.getMessages().getString(correctMessage) : "") + "\" />");
					}
				}

				response.print("<input type=\"text\" name=\"" + id + "\" id=\"" + id + Integer.toString(fillInParts.length - 1) + "\" size=\""
						+ Integer.toString(this.numCols) + "\" value=\"");
				boxCount++;
				if ((values != null) && (values.length > fillInParts.length - 1))
				{
					response.print(Validator.escapeHtml(values[fillInParts.length - 1]));
				}
				response.print("\"" + (readOnly ? " disabled=\"disabled\"" : "") + " />");
			}
		}

		// the decode directive
		if ((this.propertyReference != null) && (!readOnly))
		{
			response.println("<input type=\"hidden\" name=\"" + decodeId + "\" value =\"" + id + "\" />" + "<input type=\"hidden\" name=\"" + "prop_"
					+ decodeId + "\" value=\"" + this.propertyReference.getFullReference(context) + "\" />");
		}

		// for onEmptyAlert, add some client-side validation
		if ((onEmptyAlert) && (!readOnly) && (boxCount > 0))
		{
			// concat all the parts values together
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < boxCount; i++)
			{
				buf.append("trim(document.getElementById('" + id + Integer.toString(i) + "').value)+");
			}
			buf.setLength(buf.length() - 1);

			context.addValidation("	if (" + buf.toString() + " == \"\")\n" + "	{\n" + "		if (document.getElementById('alert_" + id
					+ "').style.display == \"none\")\n" + "		{\n" + "			document.getElementById('alert_" + id + "').style.display = \"\";\n"
					+ "			rv=false;\n" + "		}\n" + "	}\n");
		}

		response.println("<br />");

		// for on-load focus
		if ((!readOnly) && (this.focusDecision != null) && (this.focusDecision.decide(context, focus)))
		{
			// add the first field id to the focus path
			context.addFocusId(id + "0");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setCorrectMarker(PropertyReference propertyReference, String correctIcon, String correctMessage, String incorrectIcon,
			String incorrectMessage, Decision... decision)
	{
		this.correctsReference = propertyReference;
		this.correctIcon = correctIcon;
		this.correctMessage = correctMessage;
		this.incorrectIcon = incorrectIcon;
		this.incorrectMessage = incorrectMessage;
		this.correctDecision = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setFocus(Decision decision)
	{
		this.focusDecision = decision;

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setOnEmptyAlert(Decision decision, String selector, PropertyReference... references)
	{
		this.onEmptyAlertDecision = decision;
		this.onEmptyAlertMsg = new UiMessage().setMessage(selector, references);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setReadOnly(PropertyReference reference)
	{
		this.readOnlyReference = reference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setText(String selector, PropertyReference... references)
	{
		this.textMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setTitle(String selector, PropertyReference... references)
	{
		this.titleMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FillIn setWidth(int width)
	{
		numCols = width;
		return this;
	}
}
