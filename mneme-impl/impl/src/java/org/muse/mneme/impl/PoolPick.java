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

import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * PoolPick models a question and a pool.
 */
public class PoolPick
{
	protected String origQuestionId = null;

	protected String poolId = null;

	protected String questionId = null;

	protected transient QuestionService questionService = null;

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
	 * @param questionService
	 *        The QuestionService.
	 */
	public PoolPick(QuestionService questionService)
	{
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param questionService
	 *        The QuestionService.
	 * @param questionId
	 *        The question id.
	 */
	public PoolPick(QuestionService questionService, String questionId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.origQuestionId = questionId;
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param questionService
	 *        The QuestionService.
	 * @param questionId
	 *        The question id.
	 * @param poolId
	 *        The pool.
	 */
	public PoolPick(QuestionService questionService, String questionId, String poolId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		if (poolId == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.origQuestionId = questionId;
		this.poolId = poolId;
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param questionService
	 *        The QuestionService.
	 * @param questionId
	 *        The question id.
	 * @param origQid
	 *        The origQuestionId value.
	 * @param poolId
	 *        The pool.
	 */
	public PoolPick(QuestionService questionService, String questionId, String origQid, String poolId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		if (origQid == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.origQuestionId = origQid;
		this.poolId = poolId;
		this.questionService = questionService;
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
	 * Restore the pool and question ids to their original values.
	 * 
	 * @return true if successful, false if not.
	 */
	public boolean setOrig()
	{
		// if no change
		if (this.questionId.equals(this.origQuestionId) && this.poolId == null) return true;

		// the question must exist, and be non-historical, and its pool must exist and be non-historical
		Question q = this.questionService.getQuestion(this.origQuestionId);
		if ((q == null) || (q.getIsHistorical()) || q.getPool().getIsHistorical()) return false;

		// restore
		this.poolId = null;
		this.questionId = this.origQuestionId;
		return true;
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

		// set the orig only once
		if (this.origQuestionId == null)
		{
			this.origQuestionId = questionId;
		}
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PoolPick other)
	{
		this.origQuestionId = other.origQuestionId;
		this.poolId = other.poolId;
		this.questionId = other.questionId;
		this.questionService = other.questionService;
	}
}
