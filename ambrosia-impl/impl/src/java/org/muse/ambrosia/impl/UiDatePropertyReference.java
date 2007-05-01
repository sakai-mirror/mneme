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
import org.muse.ambrosia.api.DatePropertyReference;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.util.Validator;

/**
 * UiDatePropertyReference handles Time objects formatted in the standard way.
 */
public class UiDatePropertyReference extends UiPropertyReference implements DatePropertyReference
{
	/** If set, split the date to date on top, time below. */
	protected boolean multiLine = false;

	/**
	 * {@inheritDoc}
	 */
	protected String format(Context context, Object value)
	{
		if (value instanceof Time)
		{
			if (multiLine)
			{
				return "<span style=\"white-space: nowrap;\">" + Validator.escapeHtml(((Time) value).toStringLocalDate()) + "</span><br /><spanstyle=\"white-space: nowrap;\">" + Validator.escapeHtml(((Time) value).toStringLocalTime()) + "</span>";
			}
			else
			{
				return Validator.escapeHtml(((Time) value).toStringLocalFull());
			}
		}

		return super.format(context, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public DatePropertyReference setTwoLine()
	{
		this.multiLine = true;
		return this;
	}
}
