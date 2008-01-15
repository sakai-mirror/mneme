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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;

/**
 * PoolStorageMysql implements PoolStorage in mysql.
 */
public class PoolStorageSql implements PoolStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(PoolStorageSql.class);

	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;

	/** Dependency: PoolService. */
	protected PoolServiceImpl poolService = null;

	/** Dependency: QuestionService. */
	protected QuestionServiceImpl questionService = null;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/**
	 * {@inheritDoc}
	 */
	public void clearContext(final String context)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				clearContextTx(context);
			}
		}, "clearContext: " + context.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintPools(final Date stale)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				clearStaleMintPoolsTx(stale);
			}
		}, "clearStaleMintPools: " + stale.toString());
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
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_POOL P");
		sql.append(" WHERE P.ID=?");
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(poolId);
		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			int size = Integer.parseInt((String) results.get(0));
			return Boolean.valueOf(size == 1);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolImpl> findPools(String context, PoolService.FindPoolsSort sort)
	{
		// if ((!pool.historical) && (!pool.getMint()) && pool.getContext().equals(context))

		// the where and order by
		StringBuilder whereOrder = new StringBuilder();
		whereOrder.append("WHERE P.CONTEXT=? AND P.MINT='0' AND P.HISTORICAL='0' ORDER BY ");
		switch (sort)
		{
			case title_a:
			{
				whereOrder.append("P.TITLE ASC, P.CREATED_BY_DATE ASC");
				break;
			}
			case title_d:
			{
				whereOrder.append("P.TITLE DESC, P.CREATED_BY_DATE DESC");
				break;
			}
			case points_a:
			{
				whereOrder.append("P.POINTS ASC, P.TITLE ASC, P.CREATED_BY_DATE ASC");
				break;
			}
			case points_d:
			{
				whereOrder.append("P.POINTS DESC, P.TITLE DESC, P.CREATED_BY_DATE DESC");
				break;
			}
		}

		Object[] fields = new Object[1];
		fields[0] = context;

		List<PoolImpl> rv = readPools(whereOrder.toString(), fields);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getManifest(String poolId)
	{
		// for thread-local caching
		String key = cacheKey(poolId);

		// check the thread-local cache
		List<String> rv = (List<String>) this.threadLocalManager.get(key);
		if (rv != null)
		{
			// return a copy
			return new ArrayList<String>(rv);
		}

		rv = readManifest(poolId);

		// thread-local cache (a copy)
		this.threadLocalManager.set(key, new ArrayList<String>(rv));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl getPool(String poolId)
	{
		return readPool(poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolImpl> getPools(String context)
	{
		// if ((!pool.historical) && (!pool.getMint()) && pool.getContext().equals(context))

		StringBuilder whereOrder = new StringBuilder();
		whereOrder.append("WHERE P.CONTEXT=? AND P.MINT='0' AND P.HISTORICAL='0' ORDER BY CREATED_BY_DATE ASC");

		Object[] fields = new Object[1];
		fields[0] = context;

		return readPools(whereOrder.toString(), fields);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		if (this.sqlService.getVendor().equals("mysql"))
		{
			// if we are auto-creating our schema, check and create
			if (autoDdl)
			{
				this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_pool");
			}

			M_log.info("init()");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl newPool()
	{
		return new PoolImplLazyManifest(this.poolService, this.questionService, this);
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolImpl newPool(PoolImpl pool)
	{
		return new PoolImplLazyManifest(pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(PoolImpl pool)
	{
		deletePool(pool);
		if (pool.getIsHistorical())
		{
			deletePoolManifest(pool);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(PoolImpl pool)
	{
		// for new pools
		if (pool.getId() == null)
		{
			insertPool(pool);

			// if newly made historical
			if (((PoolImplLazyManifest) pool).getNewlyHistorical())
			{
				// save the manifest
				insertManifest(pool);
				((PoolImplLazyManifest) pool).clearNewlyHistorical();
			}
		}

		// for existing pools
		else
		{
			updatePool(pool);

			// if newly made historical
			if (((PoolImplLazyManifest) pool).getNewlyHistorical())
			{
				// save the manifest
				insertManifest(pool);
				((PoolImplLazyManifest) pool).clearNewlyHistorical();
			}
		}
	}

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		autoDdl = new Boolean(value).booleanValue();
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
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		threadLocalManager = service;
	}

	/**
	 * Form a key for caching a pool.
	 * 
	 * @param poolId
	 *        The pool id.
	 * @return The cache key.
	 */
	protected String cacheKey(String poolId)
	{
		String key = "mneme:pool:manifest:" + poolId;
		return key;
	}

	/**
	 * Transaction code for clearContext()
	 */
	protected void clearContextTx(String context)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_POOL");
		sql.append(" WHERE CONTEXT=?");

		Object[] fields = new Object[1];
		fields[0] = context;

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("clearContextTx: dbWrite failed");
		}
	}

	/**
	 * Transaction code for clearStaleMintPools()
	 */
	protected void clearStaleMintPoolsTx(Date stale)
	{
		// if (pool.getMint() && pool.getCreatedBy().getDate().before(stale))

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_POOL");
		sql.append(" WHERE MINT='1' AND CREATED_BY_DATE < ?");

		Object[] fields = new Object[1];
		fields[0] = stale.getTime();

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("clearStaleMintPoolsTx: db write failed");
		}
	}

	/**
	 * Delete a pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void deletePool(final PoolImpl pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				deletePoolTx(pool);
			}
		}, "deletePool: " + pool.getId());
	}

	/**
	 * Delete a pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void deletePoolManifest(final PoolImpl pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				deletePoolManifestTx(pool);
			}
		}, "deletePoolManifest: " + pool.getId());
	}

	/**
	 * Delete a pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void deletePoolManifestTx(PoolImpl pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_POOL_MANIFEST");
		sql.append(" WHERE POOL_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(pool.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deletePoolManifestTx: db write failed");
		}
	}

	/**
	 * Delete a pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void deletePoolTx(PoolImpl pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_POOL");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(pool.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deletePoolTx: db write failed");
		}
	}

	/**
	 * Insert a new pool manifest.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertManifest(final PoolImpl pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertManifestTx(pool);
			}
		}, "insertManifest: " + pool.getId());
	}

	/**
	 * Insert a new pool manifest (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertManifestTx(PoolImpl pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_POOL_MANIFEST (");
		sql.append(" POOL_ID, QUESTION_ID )");
		sql.append(" VALUES(?,?)");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(pool.getId());

		for (String qid : pool.getFrozenManifest())
		{
			fields[1] = Long.valueOf(qid);

			if (!this.sqlService.dbWrite(sql.toString(), fields))
			{
				throw new RuntimeException("insertManifestTx: db write failed");
			}
		}
	}

	/**
	 * Insert a new pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertPool(final PoolImpl pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertPoolTx(pool);
			}
		}, "insertPool: " + pool.getId());
	}

	/**
	 * Insert a new pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertPoolTx(PoolImpl pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_POOL (");
		sql.append(" CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, DIFFICULTY, HISTORICAL,");
		sql.append(" MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POINTS, TITLE )");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[11];
		fields[0] = pool.getContext();
		fields[1] = pool.getCreatedBy().getDate().getTime();
		fields[2] = pool.getCreatedBy().getUserId();
		fields[3] = pool.getDescription();
		fields[4] = pool.getDifficulty().toString();
		fields[5] = pool.getIsHistorical() ? "1" : "0";
		fields[6] = pool.getMint() ? "1" : "0";
		fields[7] = pool.getModifiedBy().getDate().getTime();
		fields[8] = pool.getModifiedBy().getUserId();
		fields[9] = pool.getPointsEdit();
		fields[10] = pool.getTitle();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertPoolTx: dbInsert failed");
		}

		// set the pool's id
		pool.initId(id.toString());
	}

	/**
	 * Read a pool manifest
	 * 
	 * @param id
	 *        The pool id.
	 * @return The pool manifest.
	 */
	protected List<String> readManifest(String id)
	{
		final List<String> rv = new ArrayList<String>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT M.QUESTION_ID FROM MNEME_POOL_MANIFEST M WHERE M.POOL_ID = ? ORDER BY M.QUESTION_ID ASC");
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String qid = SqlHelper.readId(result, 1);
					if (qid != null) rv.add(qid);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readManifest: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Read a pool
	 * 
	 * @param id
	 *        The pool id.
	 * @return The pool.
	 */
	protected PoolImpl readPool(String id)
	{
		String whereOrder = "WHERE P.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List<PoolImpl> rv = readPools(whereOrder, fields);
		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Read a selection of pools
	 * 
	 * @param whereOrder
	 *        The WHERE and ORDER BY sql clauses
	 * @param fields
	 *        The bind variables.
	 * @return The pools.
	 */
	protected List<PoolImpl> readPools(String whereOrder, Object[] fields)
	{
		final List<PoolImpl> rv = new ArrayList<PoolImpl>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT P.CONTEXT, P.CREATED_BY_DATE, P.CREATED_BY_USER, P.DESCRIPTION, P.DIFFICULTY,");
		sql.append(" P.HISTORICAL, P.ID, P.MINT, P.MODIFIED_BY_DATE, P.MODIFIED_BY_USER, P.POINTS, P.TITLE");
		sql.append(" FROM MNEME_POOL P ");
		sql.append(whereOrder);

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					PoolImpl pool = newPool();
					pool.setContext(SqlHelper.readString(result, 1));
					pool.getCreatedBy().setDate(SqlHelper.readDate(result, 2));
					pool.getCreatedBy().setUserId(SqlHelper.readString(result, 3));
					pool.setDescription(SqlHelper.readString(result, 4));
					pool.setDifficulty(SqlHelper.readInteger(result, 5));
					pool.initHistorical(SqlHelper.readBoolean(result, 6));
					pool.initId(SqlHelper.readId(result, 7));
					pool.initMint(SqlHelper.readBoolean(result, 8));
					pool.getModifiedBy().setDate(SqlHelper.readDate(result, 9));
					pool.getModifiedBy().setUserId(SqlHelper.readString(result, 10));
					pool.setPointsEdit(SqlHelper.readFloat(result, 11));
					pool.setTitle(SqlHelper.readString(result, 12));

					pool.changed.clearChanged();
					rv.add(pool);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readPools: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Update an existing pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void updatePool(final PoolImpl pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updatePoolTx(pool);
			}
		}, "updatePool: " + pool.getId());
	}

	/**
	 * Update an existing pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void updatePoolTx(PoolImpl pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_POOL SET");
		sql.append(" CONTEXT=?, DESCRIPTION=?, DIFFICULTY=?, HISTORICAL=?,");
		sql.append(" MINT=?, MODIFIED_BY_DATE=?, MODIFIED_BY_USER=?, POINTS=?, TITLE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[10];
		fields[0] = pool.getContext();
		fields[1] = pool.getDescription();
		fields[2] = pool.getDifficulty().toString();
		fields[3] = pool.getIsHistorical() ? "1" : "0";
		fields[4] = pool.getMint() ? "1" : "0";
		fields[5] = pool.getModifiedBy().getDate().getTime();
		fields[6] = pool.getModifiedBy().getUserId();
		fields[7] = pool.getPointsEdit();
		fields[8] = pool.getTitle();
		fields[9] = Long.valueOf(pool.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updatePoolTx: db write failed");
		}
	}
}
