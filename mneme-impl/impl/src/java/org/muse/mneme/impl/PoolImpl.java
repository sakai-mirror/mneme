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

import java.util.ArrayList;
import java.util.List;

import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.QuestionService.FindQuestionsSort;

/**
 * PoolImpl implements Pool
 */
public class PoolImpl implements Pool
{
	/** Track any changes at all. */
	protected transient ChangeableImpl changed = new ChangeableImpl();

	protected String context = "";

	protected Attribution createdBy = null;

	/** Start of the versioning thing. */
	protected Boolean deleted = Boolean.FALSE;

	protected String description = null;

	protected Integer difficulty = Integer.valueOf(3);

	protected Boolean historical = Boolean.FALSE;

	protected String id = null;

	protected Attribution modifiedBy = null;

	protected Float points = Float.valueOf(0f);

	protected transient PoolServiceImpl poolService = null;

	/** optional list of questions - if null, use the live query from the services to find the quesitons. */
	protected List<String> questionIds = null;

	protected transient QuestionService questionService = null;

	protected String title = null;

	// TODO: version
	protected String version = "only";

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PoolImpl(PoolImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 */
	public PoolImpl(PoolServiceImpl service, QuestionService questionService)
	{
		this.poolService = service;
		this.questionService = questionService;

		this.createdBy = new AttributionImpl(this.changed);
		this.modifiedBy = new AttributionImpl(this.changed);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> drawQuestionIds(long seed, Integer numQuestions)
	{
		return this.poolService.drawQuestionIds(this, seed, numQuestions);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two PoolImpls are equals if they have the same id
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if ((this.id == null) || (((PoolImpl) obj).id == null)) return false;
		return this.id.equals(((PoolImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		return this.questionService.findQuestions(this, sort, search, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getAllQuestionIds()
	{
		return this.poolService.getAllQuestionIds(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return this.context;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getCreatedBy()
	{
		return this.createdBy;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.description;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getDifficulty()
	{
		return this.difficulty;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getModifiedBy()
	{
		return this.modifiedBy;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		return this.poolService.getPoolSize(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getPoints()
	{
		return this.points;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getVersion()
	{
		return this.version;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return getId() == null ? "null".hashCode() : this.getId().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		if (context == null) context = "";
		if (this.context.equals(context)) return;

		this.context = context;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDescription(String description)
	{
		if (!Different.different(this.description, description)) return;

		this.description = description;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDifficulty(Integer difficulty)
	{
		if (difficulty == null) throw new IllegalArgumentException();
		if (this.difficulty.equals(difficulty)) return;

		this.difficulty = difficulty;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPoints(Float points)
	{
		if (points == null) throw new IllegalArgumentException();
		if (this.points.equals(points)) return;

		this.points = points;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		if (!Different.different(this.title, title)) return;

		this.title = title;

		this.changed.setChanged();
	}

	/**
	 * Clear the changed flag(s).
	 */
	protected void clearChanged()
	{
		this.changed.clearChanged();
	}

	/**
	 * Check if the pool has been changed.
	 * 
	 * @return TRUE if the pool as been changed, FALSE if not.
	 */
	protected Boolean getChanged()
	{
		return this.changed.getChanged();
	}

	/**
	 * Access the optional question id list.
	 * 
	 * @return The question id list, or null if questions are taken live from the services.
	 */
	protected List<String> getQuestionIds()
	{
		return this.questionIds;
	}

	/**
	 * Set this assessment to be "historical" - used only for history by submissions.
	 */
	protected void initHistorical()
	{
		if (this.historical) return;

		this.historical = Boolean.TRUE;

		// suck in the current question manifest
		this.questionIds = this.getAllQuestionIds();
	}

	/**
	 * Establish the id.
	 * 
	 * @param id
	 *        The pool id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	protected void set(PoolImpl other)
	{
		this.changed = new ChangeableImpl(other.changed);
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy, this.changed);
		this.context = other.context;
		this.deleted = other.deleted;
		this.description = other.description;
		this.difficulty = other.difficulty;
		this.id = other.id;
		this.modifiedBy = new AttributionImpl((AttributionImpl) other.modifiedBy, this.changed);
		this.points = other.points;
		this.poolService = other.poolService;
		if (other.questionIds != null)
		{
			// TODO: this could be shallow copy, if we make the quesitions immutable
			this.questionIds = new ArrayList<String>(other.questionIds);
		}
		this.title = other.title;
		this.version = other.version;
	}
}
