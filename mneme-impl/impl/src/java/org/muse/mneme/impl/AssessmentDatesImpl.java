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

import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.Changeable;

/**
 * AssessmentDatesImpl implements AssessmentDates
 */
public class AssessmentDatesImpl extends AssessmentDatesBaseImpl implements AssessmentDates
{
	protected Date acceptUntil = null;

	protected Date archived = null;

	protected Date due = null;

	protected Date open = null;

	protected transient Changeable owner = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentDatesImpl(Assessment assessment, AssessmentDatesImpl other, Changeable owner)
	{
		this(assessment, owner);
		set(other);
	}

	/**
	 * Construct.
	 */
	public AssessmentDatesImpl(Assessment assessment, Changeable owner)
	{
		super(assessment);
		this.owner = owner;
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
	public Date getOpenDate()
	{
		return this.open;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAcceptUntilDate(Date date)
	{
		if (!Different.different(this.acceptUntil, date)) return;

		this.acceptUntil = date;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Date date)
	{
		if (!Different.different(this.due, date)) return;

		this.due = date;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOpenDate(Date date)
	{
		if (!Different.different(this.open, date)) return;

		this.open = date;

		this.owner.setChanged();
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
		this.archived = other.archived;
		this.due = other.due;
		this.open = other.open;
	}
}
