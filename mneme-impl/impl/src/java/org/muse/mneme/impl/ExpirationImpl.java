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

import org.muse.mneme.api.Expiration;
import org.sakaiproject.time.api.Time;

/**
 * <p>
 * ExpirationImpl implements Expiration
 * </p>
 */
public class ExpirationImpl implements Expiration
{
	protected Cause cause;

	protected Long duration;

	protected Long limit;

	protected Time time;

	/**
	 * {@inheritDoc}
	 */
	public Cause getCause()
	{
		return this.cause;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getDuration()
	{
		return this.duration;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getLimit()
	{
		return this.limit;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getTime()
	{
		return this.time;
	}
}
