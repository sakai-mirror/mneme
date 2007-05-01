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
import org.muse.ambrosia.api.Text;

/**
 * UiText implements Text.
 */
public class UiText extends UiController implements Text
{
	/** The message that will provide text to display. */
	protected Message message = null;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		if (this.message != null)
		{
			String msg = this.message.getMessage(context, focus);
			if (msg != null)
			{
				// TODO: need class, was "instruction"
				response.println("<div class=\"ambrosiaText\">" + msg + "</div>");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Text setText(String selector, PropertyReference... references)
	{
		this.message = new UiMessage().setMessage(selector, references);
		return this;
	}
}
