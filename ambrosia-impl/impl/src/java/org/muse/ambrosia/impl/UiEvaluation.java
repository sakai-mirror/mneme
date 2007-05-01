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
import org.muse.ambrosia.api.Evaluation;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiEvaluation implements Evaluation.
 */
public class UiEvaluation extends UiText implements Evaluation
{
	/** an icon for the display. */
	protected String icon = null;

	/** The alt text for the icon. */
	protected Message iconAlt = null;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		String alt = "";
		if (this.iconAlt != null)
		{
			alt = this.iconAlt.getMessage(context, focus);
		}

		if (this.message != null)
		{
			response.println("<div class =\"instruction\" style=\"color:#990033\">"
					+ ((this.icon != null) ? "<img src=\"" + context.get("sakai.return.url") + this.icon + "\" alt=\"" + alt + "\" title=\"" + alt
							+ "\" />" : "") + Validator.escapeHtml(this.message.getMessage(context, focus)) + "</div>");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Evaluation setIcon(String icon, String selector, PropertyReference... references)
	{
		this.icon = icon;
		this.iconAlt = new UiMessage().setMessage(selector, references);

		return this;
	}
}
