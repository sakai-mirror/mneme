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

import org.muse.mneme.api.SubmissionCount;

/**
 * SubmissionCountImpl implements SubmissionCountImpl
 */
public class SubmissionCountImpl implements SubmissionCount
{
	protected Integer completed = new Integer(0);

	protected Integer graded = new Integer(0);

	protected Integer inProgress = new Integer(0);

	protected Integer ungraded = new Integer(0);

	/**
	 * Construct.
	 */
	public SubmissionCountImpl()
	{
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public SubmissionCountImpl(SubmissionCountImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getCompleted()
	{
		return this.completed;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getGraded()
	{
		return this.graded;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getInProgress()
	{
		return this.inProgress;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getUngraded()
	{
		return this.ungraded;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCompleted(Integer count)
	{
		if (count == null) throw new IllegalArgumentException();
		this.completed = count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setGraded(Integer count)
	{
		if (count == null) throw new IllegalArgumentException();
		this.graded = count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInProgress(Integer count)
	{
		if (count == null) throw new IllegalArgumentException();
		this.inProgress = count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUngraded(Integer count)
	{
		if (count == null) throw new IllegalArgumentException();
		this.ungraded = count;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(SubmissionCountImpl other)
	{
		this.completed = other.completed;
		this.graded = other.graded;
		this.inProgress = other.inProgress;
		this.ungraded = other.ungraded;
	}
}