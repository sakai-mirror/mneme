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

import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.Expiration;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;

/**
 * AssessmentDatesImpl implements AssessmentDates
 */
public class AssessmentDatesImpl implements AssessmentDates
{
	protected Time acceptUntil = null;

	protected Time due = null;

	protected Time open = null;

	protected TimeService timeService = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentDatesImpl(AssessmentDatesImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public AssessmentDatesImpl(TimeService service)
	{
		this.timeService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getAcceptUntilDate()
	{
		return this.acceptUntil;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getDueDate()
	{
		return this.due;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getDurationTillDue()
	{
		// if no due date
		if (this.due == null) return null;

		// if we have started, the clock is running - compute how long from NOW the end is
		long tillDue = this.due.getTime() - this.timeService.newTime().getTime();
		if (tillDue <= 0) return new Long(0);

		return new Long(tillDue);
	}

	/**
	 * {@inheritDoc}
	 */
	public Expiration getExpiration()
	{
		ExpirationImpl rv = new ExpirationImpl();

		// see if the assessment has a hard due date (w/ no late submissions accepted) or a retract date
		Time closedDate = getAcceptUntilDate();

		// compute an end time based on the assessment's closed date
		if (closedDate == null) return null;

		rv.time = closedDate;

		// the closeDate is the end time
		long endTime = closedDate.getTime();

		// if this closed date is more than 2 hours from now, ignore it and say we have no expiration
		if (endTime > this.timeService.newTime().getTime() + (2l * 60l * 60l * 1000l)) return null;

		// set the limit to 2 hours
		rv.limit = 2l * 60l * 60l * 1000l;

		rv.cause = Expiration.Cause.closedDate;

		// how long from now till endTime?
		long tillExpires = endTime - this.timeService.newTime().getTime();
		if (tillExpires <= 0) tillExpires = 0;

		rv.duration = new Long(tillExpires);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getOpenDate()
	{
		return this.open;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAcceptUntilDate(Time date)
	{
		this.acceptUntil = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Time date)
	{
		this.due = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOpenDate(Time date)
	{
		this.open = date;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentDatesImpl other)
	{
		this.acceptUntil = other.acceptUntil;
		this.timeService = other.timeService;
		this.open = other.open;
		this.timeService = other.timeService;
	}
}
