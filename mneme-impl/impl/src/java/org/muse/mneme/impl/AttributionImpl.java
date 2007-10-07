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

import java.util.Date;

import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.Changeable;

/**
 * AttributionImpl implements Attribution
 */
public class AttributionImpl implements Attribution
{
	protected Date date = null;

	protected transient Changeable owner = null;

	protected String userId = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AttributionImpl(AttributionImpl other, Changeable owner)
	{
		this(owner);
		set(other);
	}

	/**
	 * Construct.
	 */
	public AttributionImpl(Changeable owner)
	{
		this.owner = owner;
	}

	/**
	 * Construct.
	 * 
	 * @param date
	 *        The date.
	 * @param userId
	 *        The user id.
	 */
	public AttributionImpl(Date date, String userId, Changeable owner)
	{
		this(owner);
		this.date = date;
		this.userId = userId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDate()
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
	public void setDate(Date date)
	{
		if (!Different.different(this.date, date)) return;

		this.date = date;

		if (this.owner != null) this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUserId(String userId)
	{
		if (!Different.different(this.userId, userId)) return;
		this.userId = userId;

		if (this.owner != null) this.owner.setChanged();
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
