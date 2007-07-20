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

package org.muse.mneme.impl;

import org.muse.mneme.api.Attribution;
import org.sakaiproject.time.api.Time;

/**
 * AttributionImpl implements Attribution
 */
public class AttributionImpl implements Attribution
{
	protected Time date = null;

	protected String userId = null;

	/**
	 * Construct.
	 */
	public AttributionImpl()
	{
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AttributionImpl(AttributionImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param date
	 *        The date.
	 * @param userId
	 *        The user id.
	 */
	public AttributionImpl(Time date, String userId)
	{
		this.date = date;
		this.userId = userId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getDate()
	{
		return this.date;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserId()
	{
		return this.userId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDate(Time date)
	{
		this.date = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AttributionImpl other)
	{
		this.date = other.date;
		this.userId = other.userId;
	}
}
