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

/**
 * PoolPick models a question and a pool.
 */
public class PoolPick
{
	protected String poolId = null;

	protected String questionId = null;

	/**
	 * Construct.
	 */
	public PoolPick()
	{
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PoolPick(PoolPick other)
	{
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param questionId
	 *        The question id.
	 */
	public PoolPick(String questionId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		this.questionId = questionId;
	}

	/**
	 * Construct.
	 * 
	 * @param questionId
	 *        The question id.
	 */
	public PoolPick(String questionId, String poolId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		if (poolId == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.poolId = poolId;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two PartImpls are equals if they have the same question
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		return this.questionId.equals(((PoolPick) obj).questionId);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPoolId()
	{
		return this.poolId;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getQuestionId()
	{
		return this.questionId;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.questionId.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPool(String poolId)
	{
		this.poolId = poolId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestion(String questionId)
	{
		this.questionId = questionId;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PoolPick other)
	{
		this.poolId = other.poolId;
		this.questionId = other.questionId;
	}
}
