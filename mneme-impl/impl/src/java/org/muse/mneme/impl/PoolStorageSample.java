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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.sakaiproject.util.StringUtil;

/**
 * PoolStorageSample defines a sample storage for PoolStorage.
 */
public class PoolStorageSample implements PoolStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(PoolStorageSample.class);

	protected boolean fakedAlready = false;

	protected Object idGenerator = new Object();

	protected long nextId = 100;

	protected Map<String, PoolImpl> pools = new LinkedHashMap<String, PoolImpl>();

	protected PoolServiceImpl poolService = null;

	protected QuestionServiceImpl questionService = null;

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintPools(Date stale)
	{
		// find them
		List<String> delete = new ArrayList<String>();
		for (PoolImpl pool : this.pools.values())
		{
			if (pool.getMint() && pool.getCreatedBy().getDate().before(stale))
			{
				delete.add(pool.getId());
			}
		}

		// remove them
		for (String id : delete)
		{
			this.pools.remove(id);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPools(String context)
	{
		fakeIt();

		int count = 0;

		for (PoolImpl pool : this.pools.values())
		{
			if ((!pool.historical) && (!pool.getMint()) && pool.getContext().equals(context))
			{
				count++;
			}
		}

		return count;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean existsPool(String poolId)
	{
		fakeIt();

		PoolImpl pool = this.pools.get(poolId);
		if (pool == null) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolImpl> findPools(String context, final PoolService.FindPoolsSort sort, Integer pageNum, Integer pageSize)
	{
		fakeIt();

		List<PoolImpl> rv = new ArrayList<PoolImpl>();

		for (PoolImpl pool : this.pools.values())
		{
			if ((!pool.historical) && (!pool.getMint()) && pool.getContext().equals(context))
			{
				rv.add(newPool(pool));
			}
		}

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				switch (sort)
				{
					case title_a:
					{
						String s0 = StringUtil.trimToZero(((Pool) arg0).getTitle());
						String s1 = StringUtil.trimToZero(((Pool) arg1).getTitle());
						rv = s0.compareToIgnoreCase(s1);
						break;
					}
					case title_d:
					{
						String s0 = StringUtil.trimToZero(((Pool) arg0).getTitle());
						String s1 = StringUtil.trimToZero(((Pool) arg1).getTitle());
						rv = -1 * s0.compareToIgnoreCase(s1);
						break;
					}
					case points_a:
					{
						Float f0 = ((Pool) arg0).getPoints();
						if (f0 == null) f0 = Float.valueOf(0f);
						Float f1 = ((Pool) arg1).getPoints();
						if (f1 == null) f1 = Float.valueOf(0f);
						rv = f0.compareTo(f1);
						break;
					}
					case points_d:
					{
						Float f0 = ((Pool) arg0).getPoints();
						if (f0 == null) f0 = Float.valueOf(0f);
						Float f1 = ((Pool) arg1).getPoints();
						if (f1 == null) f1 = Float.valueOf(0f);
						rv = -1 * f0.compareTo(f1);
						break;
					}
				}

				return rv;
			}
		});

		// page
		if ((pageNum != null) && (pageSize != null))
		{
			// start at ((pageNum-1)*pageSize)
			int start = ((pageNum - 1) * pageSize);
			if (start < 0) start = 0;
			if (start > rv.size()) start = rv.size() - 1;

			// end at ((pageNum)*pageSize)-1, or max-1, (note: subList is not inclusive for the end position)
			int end = ((pageNum) * pageSize);
			if (end < 0) end = 0;
			if (end > rv.size()) end = rv.size();

			rv = rv.subList(start, end);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getManifest(String poolId)
	{
		PoolImpl rv = this.pools.get(poolId);
		if (rv == null) return null;

		return rv.getFrozenManifest();
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl getPool(String poolId)
	{
		fakeIt();

		PoolImpl rv = this.pools.get(poolId);
		if (rv == null) return null;

		// return a copy
		rv = newPool(rv);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolImpl> getPools(String context)
	{
		fakeIt();

		List<PoolImpl> rv = new ArrayList<PoolImpl>();

		for (PoolImpl pool : this.pools.values())
		{
			if ((!pool.historical) && (!pool.getMint()) && pool.getId().equals(context))
			{
				rv.add(newPool(pool));
			}
		}

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean manifestDependsOn(Question question)
	{
		for (PoolImpl pool : this.pools.values())
		{
			if (pool.getContext().equals(question.getContext()) && (pool.getIsHistorical()))
			{
				List<String> manifest = pool.getFrozenManifest();
				int index = manifest.indexOf(question.getId());
				if (index != -1)
				{
					return Boolean.TRUE;
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl newPool()
	{
		return new PoolImpl(this.poolService, this.questionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl newPool(PoolImpl pool)
	{
		return new PoolImpl(pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(PoolImpl pool)
	{
		PoolImpl p = this.pools.remove(pool.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(PoolImpl pool)
	{
		fakeIt();

		// assign an id
		if (pool.getId() == null)
		{
			long id = 0;
			synchronized (this.idGenerator)
			{
				id = this.nextId;
				this.nextId++;
			}
			pool.initId("b" + Long.toString(id));
		}

		this.pools.put(pool.getId(), newPool(pool));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPoolService(PoolServiceImpl service)
	{
		this.poolService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionService(QuestionServiceImpl service)
	{
		this.questionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchManifests(Question from, Question to)
	{
		for (PoolImpl pool : this.pools.values())
		{
			if (pool.getContext().equals(from.getContext()) && (pool.getIsHistorical()))
			{
				List<String> manifest = pool.getFrozenManifest();
				int index = manifest.indexOf(from.getId());
				if (index != -1)
				{
					manifest.set(index, to.getId());
				}
			}
		}
	}

	protected void fakeIt()
	{
		if (!fakedAlready)
		{
			fakedAlready = true;

			Date now = new Date();

			PoolImpl pool = newPool();
			pool.initId("b1");
			pool.setDescription("description");
			pool.setDifficulty(5);
			pool.createdBy.setUserId("admin");
			pool.createdBy.setDate(now);
			pool.modifiedBy.setUserId("admin");
			pool.modifiedBy.setDate(now);
			pool.setPoints(10f);
			pool.setTitle("title");
			pool.getCreatedBy().setUserId("admin");
			pool.getCreatedBy().setDate(now);
			pool.getModifiedBy().setUserId("admin");
			pool.getModifiedBy().setDate(now);
			pool.setContext("mercury");
			pool.changed.clearChanged();
			pool.clearMint();
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b2");
			pool.setDescription("description 2");
			pool.setDifficulty(5);
			pool.createdBy.setUserId("admin");
			pool.createdBy.setDate(now);
			pool.modifiedBy.setUserId("admin");
			pool.modifiedBy.setDate(now);
			pool.setPoints(10f);
			pool.setTitle("title 2");
			pool.getCreatedBy().setUserId("admin");
			pool.getCreatedBy().setDate(now);
			pool.getModifiedBy().setUserId("admin");
			pool.getModifiedBy().setDate(now);
			pool.setContext("mercury");
			pool.changed.clearChanged();
			pool.clearMint();
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b3");
			pool.setDescription("description 3");
			pool.setDifficulty(3);
			pool.createdBy.setUserId("admin");
			pool.createdBy.setDate(now);
			pool.modifiedBy.setUserId("admin");
			pool.modifiedBy.setDate(now);
			pool.setPoints(7f);
			pool.setTitle("title 3");
			pool.getCreatedBy().setUserId("admin");
			pool.getCreatedBy().setDate(now);
			pool.getModifiedBy().setUserId("admin");
			pool.getModifiedBy().setDate(now);
			pool.setContext("mercury");
			pool.changed.clearChanged();
			pool.clearMint();
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b4");
			pool.setDescription("description 4");
			pool.setDifficulty(3);
			pool.createdBy.setUserId("admin");
			pool.createdBy.setDate(now);
			pool.modifiedBy.setUserId("admin");
			pool.modifiedBy.setDate(now);
			pool.setPoints(7f);
			pool.setTitle("title 4");
			pool.getCreatedBy().setUserId("admin");
			pool.getCreatedBy().setDate(now);
			pool.getModifiedBy().setUserId("admin");
			pool.getModifiedBy().setDate(now);
			pool.setContext("mercury");
			pool.changed.clearChanged();
			pool.clearMint();
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b5");
			pool.setDescription("description 5");
			pool.setDifficulty(1);
			pool.createdBy.setUserId("admin");
			pool.createdBy.setDate(now);
			pool.modifiedBy.setUserId("admin");
			pool.modifiedBy.setDate(now);
			pool.setPoints(3f);
			pool.setTitle("title 5");
			pool.getCreatedBy().setUserId("admin");
			pool.getCreatedBy().setDate(now);
			pool.getModifiedBy().setUserId("admin");
			pool.getModifiedBy().setDate(now);
			pool.setContext("mercury");
			pool.changed.clearChanged();
			pool.clearMint();
			this.pools.put(pool.getId(), pool);
		}
	}
}
