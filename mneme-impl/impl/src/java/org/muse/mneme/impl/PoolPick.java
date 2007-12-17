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
	/** Once a test is live, it stops updating with pool deletes. This is set if this draw would have been removed due. */
	protected Boolean modernDeleted = Boolean.FALSE;

	/** Once a test is live, it stops updating with pool / question changes. This keeps track of what would have been if not live. */
	protected String modernPoolId = null;

	/** Once a test is live, it stops updating with pool / question changes. This keeps track of what would have been if not live. */
	protected String modernQuestionId = null;

	/** The question's effective pool. */
	protected String poolId = null;

	/** The question. */
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
	public PoolPick(QuestionService questionService, Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		if (questionService == null) throw new IllegalArgumentException();
		this.questionId = question.getId();
		this.modernQuestionId = questionId;
		this.poolId = question.getPool().getId();
		this.modernPoolId = this.poolId;
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
		if (questionService == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.modernQuestionId = questionId;
		this.poolId = poolId;
		this.modernPoolId = poolId;
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param questionService
	 *        The QuestionService.
	 * @param questionId
	 *        The question id.
	 * @param modernQid
	 *        The modern question id.
	 * @param modernDeleted
	 *        The modern deleted value.
	 * @param poolId
	 *        The pool.
	 * @param modernPoolId
	 *        The modern pool id.
	 */
	public PoolPick(QuestionService questionService, String questionId, String modernQid, Boolean modernDeleted, String poolId, String modernPoolId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		if (modernQid == null) throw new IllegalArgumentException();
		if (modernDeleted == null) throw new IllegalArgumentException();
		if (poolId == null) throw new IllegalArgumentException();
		if (modernPoolId == null) throw new IllegalArgumentException();
		this.questionId = questionId;
		this.modernQuestionId = modernQid;
		this.modernDeleted = modernDeleted;
		this.poolId = poolId;
		this.modernPoolId = modernPoolId;
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
	public boolean setModern()
	{
		if (this.modernDeleted) return false;
		this.poolId = this.modernPoolId;
		this.questionId = this.modernQuestionId;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPool(String poolId)
	{
		this.poolId = poolId;
		this.modernPoolId = poolId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestion(String questionId)
	{
		this.questionId = questionId;
		this.modernQuestionId = questionId;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PoolPick other)
	{
		this.modernDeleted = other.modernDeleted;
		this.modernPoolId = other.modernPoolId;
		this.modernQuestionId = other.modernQuestionId;
		this.poolId = other.poolId;
		this.questionId = other.questionId;
		this.questionService = other.questionService;
	}
}
