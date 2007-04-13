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

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.TextPropertyReference;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.Validator;

/**
 * UiTextPropertyReference handles plain text String selector values.
 */
public class UiTextPropertyReference extends UiPropertyReference implements TextPropertyReference
{
	protected int maxChars = -1;

	protected boolean stripHtml = false;

	/**
	 * {@inheritDoc}
	 */
	public TextPropertyReference setMaxLength(int maxChars)
	{
		this.maxChars = maxChars;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public TextPropertyReference setStripHtml()
	{
		this.stripHtml = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public String read(Context context, Object focus)
	{
		String value = super.read(context, focus);
		if (value == null) return null;

		// strip the html if desired
		if (this.stripHtml)
		{
			value = FormattedText.convertFormattedTextToPlaintext(value);

			// also remove any remaining new lines
			value = value.replaceAll("\n", " ");
		}

		// truncate if desired and needed
		if (this.maxChars > -1)
		{
			if (value.length() > this.maxChars)
			{
				value = value.substring(0, this.maxChars) + "...";
			}
		}

		return Validator.escapeHtml(value);
	}
}
