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
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;

/**
 * PoolDrawImpl implements PoolDraw
 */
public class PoolDrawImpl implements PoolDraw
{
	protected Integer numQuestions = null;

	protected String origPoolId = null;

	protected String poolId = null;

	protected PoolService poolService = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PoolDrawImpl(PoolDrawImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param poolService
	 *        The PoolService.
	 */
	public PoolDrawImpl(PoolService poolService)
	{
		this.poolService = poolService;
	}

	/**
	 * Construct.
	 * 
	 * @param poolService
	 *        The PoolService.
	 * @param pool
	 *        The pool to draw from.
	 * @param numQuestions
	 *        The number of questions to draw.
	 */
	public PoolDrawImpl(PoolService poolService, Pool pool, Integer numQuestions)
	{
		this(poolService);
		if (pool == null) throw new IllegalArgumentException();
		this.poolId = pool.getId();
		this.origPoolId = pool.getId();
		this.numQuestions = numQuestions;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> drawQuestionIds(long seed)
	{
		Pool pool = getPool();
		if (pool == null) return new ArrayList<String>();

		return pool.drawQuestionIds(seed, this.numQuestions);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two PartImpls are equals if they have the same pool
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		return this.poolId.equals(((PoolDrawImpl) obj).poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getAllQuestionIds()
	{
		Pool pool = getPool();
		if (pool == null) return new ArrayList<String>();

		return pool.getAllQuestionIds();
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		return this.numQuestions;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool()
	{
		return poolService.getPool(this.poolId);
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
	public int hashCode()
	{
		return this.poolId.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNumQuestions(Integer numQuestions)
	{
		this.numQuestions = numQuestions;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPool(Pool pool)
	{
		if (pool == null) throw new IllegalArgumentException();
		this.poolId = pool.getId();

		// set the orig only once
		if (this.origPoolId == null)
		{
			this.origPoolId = pool.getId();
		}
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PoolDrawImpl other)
	{
		this.numQuestions = other.numQuestions;
		this.origPoolId = other.origPoolId;
		this.poolId = other.poolId;
		this.poolService = other.poolService;
	}

	/**
	 * Restore the pool id to the original value.
	 * 
	 * @return true if successful, false if the orig pool is not available.
	 */
	protected boolean setOrig()
	{
		// if there has been no change, we are done.
		if (this.poolId.equals(this.origPoolId)) return true;

		// check that the orig pool is available
		Pool pool = this.poolService.getPool(this.origPoolId);
		if ((pool == null) || (pool.getIsHistorical())) return false;

		// set it
		this.poolId = this.origPoolId;
		return true;
	}
}
