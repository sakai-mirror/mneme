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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;

/**
 * PoolStorageSample defines a sample storage for PoolStorage.
 */
public class PoolStorageSample implements PoolStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(PoolStorageSample.class);

	protected Object idGenerator = new Object();

	protected long nextId = 100;

	protected Map<String, PoolImpl> pools = new HashMap<String, PoolImpl>();

	protected PoolServiceImpl poolService = null;

	protected QuestionServiceImpl questionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Integer countPools(String userId, String search)
	{
		fakeIt();

		int count = 0;

		for (PoolImpl pool : this.pools.values())
		{
			if (pool.getOwnerId().equals(userId))
			{
				count++;
			}
		}

		// TODO: search

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
	public List<String> drawQuestionIds(Pool pool, long seed, Integer numQuestions)
	{
		List<String> rv = this.questionService.getPoolQuestions(pool);

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed));

		// cut off the number of questions we want
		rv = rv.subList(0, numQuestions);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String userId, final PoolService.FindPoolsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		fakeIt();

		List<Pool> rv = new ArrayList<Pool>();

		for (PoolImpl pool : this.pools.values())
		{
			if (pool.getOwnerId().equals(userId))
			{
				rv.add(new PoolImpl(pool));
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
					case subject_a:
					{
						rv = ((Pool) arg0).getSubject().compareTo(((Pool) arg1).getSubject());
						break;
					}
					case subject_d:
					{
						rv = -1 * ((Pool) arg0).getSubject().compareTo(((Pool) arg1).getSubject());
						break;
					}
					case title_a:
					{
						rv = ((Pool) arg0).getTitle().compareTo(((Pool) arg1).getTitle());
						break;
					}
					case title_d:
					{
						rv = -1 * ((Pool) arg0).getTitle().compareTo(((Pool) arg1).getTitle());
						break;
					}
				}

				return rv;
			}
		});

		// TODO: search

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
	public PoolImpl getPool(String poolId)
	{
		fakeIt();

		PoolImpl rv = this.pools.get(poolId);
		if (rv == null)
		{
			rv = newPool();
			rv.initId(poolId);
		}
		else
		{
			rv = new PoolImpl(rv);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getPoolSize(PoolImpl pool)
	{
		List<String> rv = this.questionService.getPoolQuestions(pool);
		return rv.size();
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
	public PoolImpl newPool()
	{
		return new PoolImpl(this.poolService, this.questionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean poolExists(String poolId)
	{
		fakeIt();

		return (this.pools.get(poolId) != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(PoolImpl pool)
	{
		fakeIt();

		this.pools.remove(pool.getId());
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

		this.pools.put(pool.getId(), new PoolImpl(pool));
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

	protected void fakeIt()
	{
		if (pools.isEmpty())
		{
			PoolImpl pool = newPool();
			pool.initId("b1");
			pool.setDescription("description");
			pool.setDifficulty(5);
			pool.setOwnerId("admin");
			pool.setPoints(10f);
			pool.setSubject("subject");
			pool.setTitle("title");
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b2");
			pool.setDescription("description 2");
			pool.setDifficulty(5);
			pool.setOwnerId("admin");
			pool.setPoints(10f);
			pool.setSubject("subject 2");
			pool.setTitle("title 2");
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b3");
			pool.setDescription("description 3");
			pool.setDifficulty(3);
			pool.setOwnerId("admin");
			pool.setPoints(7f);
			pool.setSubject("subject 3");
			pool.setTitle("title 3");
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b4");
			pool.setDescription("description 4");
			pool.setDifficulty(3);
			pool.setOwnerId("admin");
			pool.setPoints(7f);
			pool.setSubject("subject 4");
			pool.setTitle("title 4");
			this.pools.put(pool.getId(), pool);

			pool = newPool();
			pool.initId("b5");
			pool.setDescription("description 5");
			pool.setDifficulty(1);
			pool.setOwnerId("admin");
			pool.setPoints(3f);
			pool.setSubject("subject 5");
			pool.setTitle("title 5");
			this.pools.put(pool.getId(), pool);
		}
	}
}
