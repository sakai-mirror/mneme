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
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Selection;
import org.sakaiproject.util.Validator;

/**
 * UiSelection presents a selection for the user to choose or not.<br />
 * The text can be either a property reference or a message.
 */
public class UiSelection extends UiController implements Selection
{
	/** The value we use if the user does not selecet the selection. */
	protected String notSelectedValue = "false";

	/**
	 * The PropertyReference for encoding and decoding this selection - this is what will be updated with the end-user's selection
	 * choice, and what value seeds the display.
	 */
	protected PropertyReference propertyReference = null;

	/** The property reference to provide the read only setting. */
	protected PropertyReference readOnlyReference = null;

	/** The value we find if the user selects the selection. */
	protected String selectedValue = "true";

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

		// generate some ids
		int idRoot = context.getUniqueId();
		String id = this.getClass().getSimpleName() + "_" + idRoot;
		String decodeId = "decode_" + idRoot;

		PrintWriter response = context.getResponseWriter();

		// read the current value
		String value = "false";
		if (this.propertyReference != null)
		{
			value = this.propertyReference.read(context, focus);
		}

		// convert to boolean
		boolean checked = Boolean.parseBoolean(value);

		// the check box
		response.println("<br /><input type=\"checkbox\" name=\"" + id + "\" id=\"" + id + "\" value=\"" + this.selectedValue
				+ "\" " + (checked ? "CHECKED" : "") + (readOnly ? " disabled=\"disabled\"" : "") + " />");

		// the decode directive
		if ((this.propertyReference != null) && (!readOnly))
		{
			response.println("<input type=\"hidden\" name=\"" + decodeId + "\" value =\"" + id + "\" />"
					+ "<input type=\"hidden\" name=\"" + "prop_" + decodeId + "\" value=\""
					+ this.propertyReference.getFullReference(context) + "\" />" + "<input type=\"hidden\" name=\"" + "null_"
					+ decodeId + "\" value=\"" + this.notSelectedValue + "\" />");
		}

		// title after
		if (this.titleMessage != null)
		{
			response.println("<label for=\"" + id + "\">");
			response.println(this.titleMessage.getMessage(context, focus));
			response.println("</label>");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection setReadOnly(PropertyReference reference)
	{
		this.readOnlyReference = reference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection setSelectedValue(String value)
	{
		this.selectedValue = value;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection setTitle(String selector, PropertyReference... references)
	{
		this.titleMessage = new UiMessage().setMessage(selector, references);
		return this;
	}
}
