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

import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.Expiration;

/**
 * AssessmentDatesImpl implements AssessmentDates
 */
public class AssessmentDatesImpl implements AssessmentDates
{
	protected Date acceptUntil = null;

	protected Date archived = null;

	protected Date due = null;

	protected Date open = null;

	/**
	 * Construct.
	 */
	public AssessmentDatesImpl()
	{
	}

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
	 * {@inheritDoc}
	 */
	public Date getAcceptUntilDate()
	{
		return this.acceptUntil;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getArchivedDate()
	{
		return this.archived;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDueDate()
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
		Date now = new Date();
		long tillDue = this.due.getTime() - now.getTime();
		if (tillDue <= 0) return new Long(0);

		return new Long(tillDue);
	}

	/**
	 * {@inheritDoc}
	 */
	public Expiration getExpiration()
	{
		ExpirationImpl rv = new ExpirationImpl();

		// see if the assessment has a hard due date (no submissions allowed)
		Date closedDate = this.getSubmitUntilDate();
		if (closedDate == null) return null;

		// compute an end time based on the assessment's closed date
		Date now = new Date();

		// if we are past it already
		if (closedDate.before(now)) return null;

		rv.time = closedDate;

		// the closeDate is the end time
		long endTime = closedDate.getTime();

		// if this closed date is more than 2 hours from now, ignore it and say we have no expiration
		if (endTime > now.getTime() + (2l * 60l * 60l * 1000l)) return null;

		// set the limit to 2 hours
		rv.limit = 2l * 60l * 60l * 1000l;

		rv.cause = Expiration.Cause.closedDate;

		// how long from now till endTime?
		long tillExpires = endTime - now.getTime();
		if (tillExpires <= 0) tillExpires = 0;

		rv.duration = new Long(tillExpires);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// open, if defined, must be before acceptUntil and due, if defined
		if ((this.open != null) && (this.due != null) && (!this.open.before(this.due))) return Boolean.FALSE;
		if ((this.open != null) && (this.acceptUntil != null) && (!this.open.before(this.acceptUntil))) return Boolean.FALSE;

		// due, if defined, must be not after acceptUntil, if defined
		if ((this.due != null) && (this.acceptUntil != null) && (this.due.after(this.acceptUntil))) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getOpenDate()
	{
		return this.open;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getSubmitUntilDate()
	{
		// this is the acceptUntil date, if defined, or the due date.
		Date closedDate = this.acceptUntil;
		if (closedDate == null) closedDate = this.due;
		return closedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAcceptUntilDate(Date date)
	{
		this.acceptUntil = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Date date)
	{
		this.due = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOpenDate(Date date)
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
		this.due = other.due;
		this.open = other.open;
	}
}
