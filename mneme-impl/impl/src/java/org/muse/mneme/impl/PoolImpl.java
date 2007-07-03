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

import org.muse.mneme.api.Pool;

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

	protected String subject = null;

	protected String title = null;

	protected String version = "only";

	public PoolImpl(String id)
	{
		this.id = id;
	}

	public PoolImpl(String id, String description, Integer difficulty, String ownerId, Float points, String subject, String title)
	{
		this.id = id;
		this.description = description;
		this.difficulty = difficulty;
		this.ownerId = ownerId;
		this.points = points;
		this.subject = subject;
		this.title = title;
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
		return new Integer(0);
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
}
