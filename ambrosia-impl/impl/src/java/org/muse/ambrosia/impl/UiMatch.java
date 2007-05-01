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
import java.util.List;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.FillIn;
import org.muse.ambrosia.api.Match;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiMatch is the assessment tool's matching interface. <br />
 */
public class UiMatch extends UiController implements Match
{
	public class Choice
	{
		public String id;

		public String label;

		public String text;
	}

	/** A reference to a property of the choice to get the id. */
	protected PropertyReference choiceIdReference = null;

	/** A reference to a property of the choice to get the label. */
	protected PropertyReference choiceLabelReference = null;

	/** A reference to a property of the choice to get the text. */
	protected PropertyReference choiceTextReference = null;

	/** The decision to include the correct marking. */
	protected Decision[] correctDecision = null;

	/** The icon to use to mark correct parts. */
	protected String correctIcon = null;

	/** The message key for the alt-text for correct icon. */
	protected String correctMessage = null;

	/** The PropertyReference for getting a correctness flag for each part. */
	protected PropertyReference correctsReference = null;

	/** The decision to include the feedback display. */
	protected Decision[] feedbackDecision = null;

	/** The message key for the feedback. */
	protected String feedbackMessage = null;

	/** The PropertyReference for getting feedback text for each part. */
	protected PropertyReference feedbacksReference = null;

	/** The icon to use to mark incorrect parts. */
	protected String incorrectIcon = null;

	/** The message key for the alt-text for incorrect icon. */
	protected String incorrectMessage = null;

	/** The decision to control the onEmptyAlert. */
	protected Decision onEmptyAlertDecision = null;

	/** The message for the onEmptyAlert. */
	protected Message onEmptyAlertMsg = null;

	/** A reference to a property of the parts to get the choice. */
	protected PropertyReference partsChoiceReference = null;

	/** A reference to a collection of objects that are the parts. */
	protected PropertyReference partsReference = null;

	/** A reference to a property of the parts to get the title. */
	protected PropertyReference partsTitleReference = null;

	/**
	 * The PropertyReference for encoding and decoding this selection - this is what will be updated with the end-user's selection choice, and what
	 * value seeds the display.
	 */
	protected PropertyReference propertyReference = null;

	/** The property reference to provide the read only setting. */
	protected PropertyReference readOnlyReference = null;

	/** The message that will head the choice dropdowns (i.e. 'select:'). */
	protected Message selectMessage = null;

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

		// the feedback decision
		boolean feedbackIncluded = true;
		if (this.feedbackDecision != null)
		{
			for (Decision decision : this.feedbackDecision)
			{
				if (!decision.decide(context, focus))
				{
					feedbackIncluded = false;
					break;
				}
			}
		}

		// read the feedback texts - we want a String[]
		String[] feedbacks = null;
		if (feedbackIncluded && (this.feedbacksReference != null))
		{
			Object o = this.feedbacksReference.readObject(context, focus);
			if (o != null)
			{
				if (o.getClass().isArray())
				{
					feedbacks = (String[]) o;
				}

				else if (o instanceof Collection)
				{
					feedbacks = (String[]) ((Collection) o).toArray(new String[0]);
				}
			}
		}

		if (onEmptyAlert)
		{
			// this will become visible if a submit happens and the validation fails
			response.println("<div class=\"ambrosiaAlert\" style=\"display:none\" id=\"alert_" + id + "\">"
					+ Validator.escapeHtml(this.onEmptyAlertMsg.getMessage(context, focus)) + "</div>");
		}

		// title
		if (this.titleMessage != null)
		{
			// TODO: proper HTML formatting for title? -ggolden
			response.print(Validator.escapeHtml(this.titleMessage.getMessage(context, focus)));
			response.println("<br />");
		}

		// read the parts
		List parts = null;
		if (this.partsReference != null)
		{
			parts = (List) this.partsReference.readObject(context, focus);
		}

		// use the first part for the choices listing
		if ((parts != null) && (parts.size() > 0))
		{
			// read the choice from the part
			Choice[] choices = null;
			if (this.partsChoiceReference != null)
			{
				context.put("part", parts.get(0));
				choices = readChoices(context);
				context.remove("part");

				if (choices != null)
				{
					response.println("<div class=\"ambrosiaMatchHighlight\">");
					for (Choice choice : choices)
					{
						response.println("<p>" + choice.label + ". " + choice.text + "</p>");
					}
					response.println("</div>");
				}
			}

			// for part
			int seq = 1;
			for (Object part : parts)
			{
				context.put("part", part);

				// this part's value - the values order lines up with the parts order
				String value = null;
				if ((values != null) && (values.length >= seq))
				{
					value = values[seq - 1];
				}

				response.println("<p>");

				// if marked correct, flag with an icon
				if (correctMarkingIncluded && (corrects != null) && (corrects.length > (seq - 1)))
				{
					if ((corrects[seq - 1] != null) && (corrects[seq - 1].booleanValue()))
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

				// this part's choices
				// read the choice from the part
				choices = null;
				if (this.partsChoiceReference != null)
				{
					choices = readChoices(context);
					if (choices != null)
					{
						response.println("<select name = \"" + id + "\" id = \"" + id + Integer.toString(seq) + "\" size=\"1\""
								+ (readOnly ? " disabled=\"disabled\"" : "") + ">");
						if (this.selectMessage != null)
						{
							response.println("<option value=\"\" >" + Validator.escapeHtml(this.selectMessage.getMessage(context, focus))
									+ "</option>");
						}
						for (Choice choice : choices)
						{
							response.println("<option value=\"" + choice.id + "\" " + (choice.id.equals(value) ? "SELECTED" : "") + ">"
									+ Validator.escapeHtml(choice.label) + "</option>");
						}
						response.println("</select>");
					}
				}

				// read the part title
				String title = null;
				if (this.partsTitleReference != null)
				{
					title = this.partsTitleReference.read(context, part);
					response.println("<label for=\"" + id + Integer.toString(seq) + "\">");
					response.println(Integer.toString(seq) + ". " + title);
					response.println("</label>");
				}

				// feedback?
				if (feedbackIncluded && (feedbacks != null) && (feedbacks.length > (seq - 1)))
				{
					if (feedbacks[seq - 1] != null)
					{
						response.print("&nbsp;&nbsp;");

						String text = null;
						if (this.feedbackMessage != null)
						{
							Object[] args = new Object[1];
							args[0] = feedbacks[seq - 1];
							text = context.getMessages().getFormattedMessage(feedbackMessage, args);
						}
						else
						{
							text = feedbacks[seq - 1];
						}
						response.print(text);
					}
				}

				seq++;
				response.println("</p>");

				context.remove("part");
			}

			// the decode directive
			if ((this.propertyReference != null) && (!readOnly))
			{
				response.println("<input type=\"hidden\" name=\"" + decodeId + "\" value =\"" + id + "\" />" + "<input type=\"hidden\" name=\""
						+ "prop_" + decodeId + "\" value=\"" + this.propertyReference.getFullReference(context) + "\" />");
			}

			// for onEmptyAlert, add some client-side validation
			if ((onEmptyAlert) && (!readOnly))
			{
				// concat all the parts values together
				StringBuffer buf = new StringBuffer();
				for (int i = 1; i < seq; i++)
				{
					buf.append("trim(document.getElementById('" + id + Integer.toString(i) + "').value)+");
				}
				buf.setLength(buf.length() - 1);

				context.addValidation("	if (" + buf.toString() + " == \"\")\n" + "	{\n" + "		if (document.getElementById('alert_" + id
						+ "').style.display == \"none\")\n" + "		{\n" + "			document.getElementById('alert_" + id + "').style.display = \"\";\n"
						+ "			rv=false;\n" + "		}\n" + "	}\n");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setChoiceId(PropertyReference propertyReference)
	{
		this.choiceIdReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setChoiceLabel(PropertyReference propertyReference)
	{
		this.choiceLabelReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setChoiceText(PropertyReference propertyReference)
	{
		this.choiceTextReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setCorrectMarker(PropertyReference propertyReference, String correctIcon, String correctMessage, String incorrectIcon,
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
	public Match setFeedback(PropertyReference propertyReference, String message, Decision... decision)
	{
		this.feedbacksReference = propertyReference;
		this.feedbackMessage = message;
		this.feedbackDecision = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setOnEmptyAlert(Decision decision, String selector, PropertyReference... references)
	{
		this.onEmptyAlertDecision = decision;
		this.onEmptyAlertMsg = new UiMessage().setMessage(selector, references);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setParts(PropertyReference propertyReference)
	{
		this.partsReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setPartsChoices(PropertyReference propertyReference)
	{
		this.partsChoiceReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setPartsTitle(PropertyReference propertyReference)
	{
		this.partsTitleReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setReadOnly(PropertyReference reference)
	{
		this.readOnlyReference = reference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setSelectText(String selector, PropertyReference... references)
	{
		this.selectMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Match setTitle(String selector, PropertyReference... references)
	{
		this.titleMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Choice[] readChoices(Context context)
	{
		Choice[] choices = null;

		if (this.partsChoiceReference != null)
		{
			List choicesList = (List) this.partsChoiceReference.readObject(context, null);
			if ((choicesList != null) && (choicesList.size() > 0))
			{
				choices = new Choice[choicesList.size()];
				int i = 0;
				for (Object choice : choicesList)
				{
					context.put("choice", choice);

					choices[i] = new Choice();
					choices[i].id = this.choiceIdReference.read(context, null);
					choices[i].text = this.choiceTextReference.read(context, null);
					choices[i].label = this.choiceLabelReference.read(context, null);
					i++;

					context.remove("choice");
				}
			}
		}

		return choices;
	}
}
