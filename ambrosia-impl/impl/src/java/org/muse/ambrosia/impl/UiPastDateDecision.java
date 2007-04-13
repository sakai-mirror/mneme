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
import org.muse.ambrosia.api.PastDateDecision;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;

/**
 * UiPastDateDecision is a decision that is true if the value, a Time type, is in the past.
 */
public class UiPastDateDecision extends UiDecision implements PastDateDecision
{
	// TODO: add a date against which to check rather than now -ggolden

	/**
	 * {@inheritDoc}
	 */
	public boolean decide(Context context, Object focus)
	{
		if (this.propertyReference == null) return false;

		// get the value as an object, not formatted
		Object value = this.propertyReference.readObject(context, focus);
		if (value == null) return false;

		// we want a Time
		if (!(value instanceof Time)) return false;
		Time time = (Time) value;

		return time.before(TimeService.newTime());
	}
}
