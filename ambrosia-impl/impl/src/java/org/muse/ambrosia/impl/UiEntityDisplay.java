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
import java.util.List;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.PropertyRow;

/**
 * UiEntityDisplay presents a multi-row single-item display of properties of one entity from the model.<br />
 * setEntityReference() sets the reference to the entity to display.<br />
 * addRow() sets the rows, each a UiPropertyRow, that references some selector of the entity.<br />
 */
public class UiEntityDisplay extends UiController implements EntityDisplay
{
	/** The reference to the entity to display. */
	protected PropertyReference entityReference = null;

	/** Rows for this list. */
	protected List<PropertyRow> rows = new ArrayList<PropertyRow>();

	/** The message for the title. */
	protected Message title = null;

	/**
	 * {@inheritDoc}
	 */
	public EntityDisplay addRow(PropertyRow row)
	{
		this.rows.add(row);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		// title
		if (this.title != null)
		{
			response.println("<div class =\"ambrosiaInstructions\">" + this.title.getMessage(context, focus) + "</div>");
		}

		// the object
		Object entity = this.entityReference.readObject(context, focus);

		// start the table
		response.println("<table class=\"ambrosiaEntityDisplay\">");

		// rows
		for (PropertyRow r : this.rows)
		{
			if (!r.included(context, focus)) continue;

			// row header
			response.print("<tr><th>");
			if (r.getTitle() != null)
			{
				response.print(r.getTitle().getMessage(context, entity));
			}
			response.print("</th><td>");

			if (r.getProperty() != null)
			{
				String value = r.getProperty().read(context, entity);
				response.print(value);
			}
			response.println("</td></tr>");
		}

		response.println("</table>");
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityDisplay setEntityReference(PropertyReference entityReference)
	{
		this.entityReference = entityReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityDisplay setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}
}
