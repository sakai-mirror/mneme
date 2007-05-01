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
import org.muse.ambrosia.api.DurationPropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiDurationPropertyReference implements DurationPropertyReference.
 */
public class UiDurationPropertyReference extends UiPropertyReference implements DurationPropertyReference
{
	// TODO: support various display options

	protected boolean concise = false;

	/**
	 * {@inheritDoc}
	 */
	public DurationPropertyReference setConcise()
	{
		this.concise = true;
		return this;
	}

	/**
	 * Format the long to at least two digits.
	 * 
	 * @param value
	 *        The long value.
	 * @return The long value formatted as a string of at least two digits.
	 */
	protected String fmtTwoDigit(Long value)
	{
		if (value.longValue() < 10) return "0" + value.toString();
		return value.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String format(Context context, Object value)
	{
		long time = 0;
		if (value instanceof Integer)
		{
			time = ((Integer) value).longValue();
		}
		else if (value instanceof Long)
		{
			time = ((Long) value).longValue();
		}
		else if (value instanceof String)
		{
			time = Long.parseLong((String) value);
		}

		// convert to seconds
		time = time / 1000;

		// format the hours and minutes
		long hours = time / (60 * 60);
		long minutes = (time - (hours * (60 * 60))) / 60;
		long seconds = (time - (hours * (60 * 60)) - (minutes * 60));

		// TODO: take message id for pattern... {0} hours, {1} minutes
		// Object[] args = new Object[2];
		// args[0] = Long.toString(hours);
		// args[1] = Long.toString(minutes);
		// return (String) messages.getFormattedMessage("time_taken", args);

		// TODO: handle days?

		if (this.concise)
		{
			return Validator.escapeHtml(fmtTwoDigit(hours) + ":" + fmtTwoDigit(minutes) + ":" + fmtTwoDigit(seconds));
		}

		return Validator.escapeHtml(Long.toString(hours) + " hours, " + Long.toString(minutes) + " minutes, "
				+ Long.toString(seconds) + " seconds");
	}
}
