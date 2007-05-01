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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.IconKey;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiIconKey presents a key to icon use for some other part of the interface using icons. Each icon is shown with a description.
 */
public class UiIconKey extends UiController implements IconKey
{
	/** The reference to the description property in each entity. */
	protected PropertyReference descriptionReference = null;

	/** The list of icon descriptions pre-set into the tool (used instead of the reference if defined. */
	protected List<Message> iconDescriptions = new ArrayList<Message>();

	/** The reference to the icon URL property in each entity. */
	protected PropertyReference iconReference = null;

	/** The list of icon URLs pre-set into the tool (used instead of the reference if defined. */
	protected List<String> iconUrls = new ArrayList<String>();

	/** The reference to the Collection of entities to list. */
	protected PropertyReference keysReference = null;

	/** The message for the title. */
	protected Message title = null;

	/** The column width. */
	protected String width = "16px";

	/**
	 * {@inheritDoc}
	 */
	public IconKey addIcon(String url, Message description)
	{
		this.iconUrls.add(url);
		this.iconDescriptions.add(description);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		if (this.iconUrls.size() > 0)
		{
			renderInternal(context, focus);
		}
		else
		{
			renderReferences(context, focus);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public IconKey setDescriptionReference(PropertyReference descriptionReference)
	{
		this.descriptionReference = descriptionReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public IconKey setIconReference(PropertyReference iconReference)
	{
		this.iconReference = iconReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public IconKey setKeysReference(PropertyReference keysReference)
	{
		this.keysReference = keysReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public IconKey setTitle(String selector, PropertyReference... properties)
	{
		this.title = new UiMessage().setMessage(selector, properties);
		return this;
	}

	/**
	 * Render from the internally set data
	 */
	protected void renderInternal(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		// the set of objects describing the icons are in this.iconUrls and this.iconDescriptions
		boolean empty = this.iconUrls.isEmpty();

		// title, if there is one and there is data
		if ((this.title != null) && (!empty))
		{
			response.println("<div class =\"ambrosiaInstructions\">" + Validator.escapeHtml(this.title.getMessage(context, focus)) + "</div>");
		}

		// start the table
		response.println("<table class=\"ambrosiaIconKey\" cellpadding=\"0\" cellspacing=\"0\" >");

		// data
		if (!empty)
		{
			for (int i = 0; i < this.iconUrls.size(); i++)
			{
				response.print("<tr><td style=\"width:" + this.width + "; text-align:center; vertical-align:middle;\">");

				// the icon URL
				String icon = this.iconUrls.get(i);

				// the description
				Message description = this.iconDescriptions.get(i);
				String descriptionText = "";
				if (description != null) descriptionText = description.getMessage(context, focus);

				if (icon != null)
				{
					// TODO: what escape for icon?
					response.print("<img src=\"" + context.get("sakai.return.url") + icon + "\" alt=\"" + Validator.escapeHtml(descriptionText)
							+ "\" />");
				}

				response.print("</td><td>");

				// the description
				response.print(Validator.escapeHtml(descriptionText));

				response.println("</td></tr>");
			}
		}

		response.println("</table>");
	}

	/**
	 * Render from data in the references.
	 */
	protected void renderReferences(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		// the set of objects describing the icons
		Collection data = (Collection) this.keysReference.readObject(context, focus);
		boolean empty = ((data == null) || (data.isEmpty()));

		// title, if there is one and there is data
		if ((this.title != null) && (!empty))
		{
			response.println("<div class =\"ambrosiaInstructions\">" + Validator.escapeHtml(this.title.getMessage(context, focus)) + "</div>");
		}

		// start the table
		response.println("<table class=\"ambrosiaIconKey\" cellpadding=\"0\" cellspacing=\"0\" >");

		// data
		if (!empty)
		{
			for (Object entity : data)
			{
				response.print("<tr><td>");

				// the icon
				String icon = null;
				if (this.iconReference != null) icon = this.iconReference.read(context, entity);

				// the description
				String description = "";
				if (this.descriptionReference != null) description = this.descriptionReference.read(context, entity);

				if (icon != null)
				{
					response
							.print("<img src=\"" + context.get("sakai.return.url") + icon + "\" alt=\"" + Validator.escapeHtml(description) + "\" />");
				}

				response.print("</td><td>");

				// the description
				response.print(Validator.escapeHtml(description));

				response.println("</td></tr>");
			}
		}

		response.println("</table>");
	}
}
