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

import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentDates;

/**
 * AssessmentDatesOverrideImpl implements AssessmentDates, merging a main dates impl with an AssesmentAccess impl.
 */
public class AssessmentDatesOverrideImpl extends AssessmentDatesBaseImpl implements AssessmentDates
{
	protected AssessmentAccess access = null;

	protected AssessmentDates main = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentDatesOverrideImpl(AssessmentDates main, AssessmentAccess access)
	{
		this.main = main;
		this.access = access;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getAcceptUntilDate()
	{
		if ((this.access != null) && (this.access.getOverrideAcceptUntilDate()))
		{
			return this.access.getAcceptUntilDate();
		}

		return this.main.getAcceptUntilDate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getArchivedDate()
	{
		return this.main.getArchivedDate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDueDate()
	{
		if ((this.access != null) && (this.access.getOverrideDueDate()))
		{
			return this.access.getDueDate();
		}

		return this.main.getDueDate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getOpenDate()
	{
		if ((this.access != null) && (this.access.getOverrideOpenDate()))
		{
			return this.access.getOpenDate();
		}

		return this.main.getOpenDate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAcceptUntilDate(Date date)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Date date)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOpenDate(Date date)
	{
		throw new IllegalArgumentException();
	}
}
