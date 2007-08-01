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

import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;

/**
 * PoolImpl implements Pool
 */
public class PoolImpl implements Pool
{
	protected String description = null;

	protected Integer difficulty = null;

	protected String id = null;

	protected String ownerId = null;

	protected Float points = null;

	protected PoolServiceImpl poolService = null;

	protected String subject = null;

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
	public PoolImpl(PoolServiceImpl service)
	{
		this.poolService = service;
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
	public Integer getNumQuestions()
	{
		return this.poolService.getPoolSize(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getOwnerId()
	{
		return this.ownerId;
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
	public String getSubject()
	{
		return this.subject;
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
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDifficulty(Integer difficulty)
	{
		this.difficulty = difficulty;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOwnerId(String ownerId)
	{
		this.ownerId = ownerId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPoints(Float points)
	{
		this.points = points;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
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
		this.description = other.description;
		this.difficulty = other.difficulty;
		this.id = other.id;
		this.ownerId = other.ownerId;
		this.points = other.points;
		this.poolService = other.poolService;
		this.subject = other.subject;
		this.title = other.title;
		this.version = other.version;
	}
}
