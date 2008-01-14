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

package org.muse.mneme.test;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.dbcp.SakaiBasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.impl.PoolImpl;
import org.muse.mneme.impl.PoolStorageMysql;
import org.sakaiproject.db.impl.BasicSqlService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.thread_local.impl.ThreadLocalComponent;

/**
 * Test Pool
 */
public class PoolStorageTestX extends TestCase
{
	final static String CONTEXT = "JUNIT_TEST_JUNIT";

	public class SqlServiceTest extends BasicSqlService
	{
		ThreadLocalManager tlm = null;

		protected void setThreadLocalManager(ThreadLocalManager tlm)
		{
			this.tlm = tlm;
		}

		protected ThreadLocalManager threadLocalManager()
		{
			return tlm;
		}

		protected UsageSessionService usageSessionService()
		{
			// TODO: might have to mock
			return null;
		}
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(PoolStorageTestX.class);

	protected SqlServiceTest sqlService = null;

	protected PoolStorageMysql storage = null;

	protected ThreadLocalComponent thread_localManager = null;

	/**
	 * @param arg0
	 */
	public PoolStorageTestX(String arg0)
	{
		super(arg0);
	}

	/**
	 * Test clearStaleMintPools()
	 * 
	 * @throws Exception
	 */
	public void test001() throws Exception
	{
		// create a pool - leave it mint - make it old
		Date old = new Date(new Date().getTime()-(2 * 24 * 60 * 60 * 1000));
		PoolImpl pool = this.storage.newPool();
		pool.setContext(CONTEXT);
		pool.setTitle(CONTEXT);
		pool.getCreatedBy().setDate(old);
		pool.getCreatedBy().setUserId("admin");
		pool.getModifiedBy().setDate(old);
		pool.getModifiedBy().setUserId("admin");
		this.storage.savePool(pool);
		
		// it should now exist
		Boolean exists = this.storage.existsPool(pool.getId());
		assertTrue(exists == Boolean.TRUE);

		// this should leave the pool in place
		this.storage.clearStaleMintPools(old);
		
		// it should now exist
		exists = this.storage.existsPool(pool.getId());
		assertTrue(exists == Boolean.TRUE);

		// this should remove it
		this.storage.clearStaleMintPools(new Date());

		// it should not exist
		exists = this.storage.existsPool(pool.getId());
		assertTrue(exists == Boolean.FALSE);
	}

	/**
	 * Test existsPool()
	 * 
	 * @throws Exception
	 */
	public void test002() throws Exception
	{
		// create a pool
		PoolImpl pool = this.storage.newPool();
		pool.setContext(CONTEXT);
		pool.setTitle(CONTEXT);
		pool.getCreatedBy().setDate(new Date());
		pool.getCreatedBy().setUserId("admin");
		pool.getModifiedBy().setDate(new Date());
		pool.getModifiedBy().setUserId("admin");
		this.storage.savePool(pool);

		// it should now exist
		Boolean exists = this.storage.existsPool(pool.getId());
		assertTrue(exists == Boolean.TRUE);

		// remove it
		this.storage.removePool(pool);

		// it should not exist
		exists = this.storage.existsPool(pool.getId());
		assertTrue(exists == Boolean.FALSE);
	}

	/**
	 * Test findPools()
	 * 
	 * @throws Exception
	 */
	public void test003() throws Exception
	{
		// create some pools
		PoolImpl pool1 = this.storage.newPool();
		pool1.setContext(CONTEXT);
		pool1.setTitle("a");
		pool1.setPoints(Float.valueOf(5));
		pool1.getCreatedBy().setDate(new Date());
		pool1.getCreatedBy().setUserId("admin");
		pool1.getModifiedBy().setDate(new Date());
		pool1.getModifiedBy().setUserId("admin");
		pool1.clearMint();
		this.storage.savePool(pool1);

		PoolImpl pool2 = this.storage.newPool();
		pool2.setContext(CONTEXT);
		pool2.setTitle("b");
		pool2.setPoints(Float.valueOf(10));
		pool2.getCreatedBy().setDate(new Date());
		pool2.getCreatedBy().setUserId("admin");
		pool2.getModifiedBy().setDate(new Date());
		pool2.getModifiedBy().setUserId("admin");
		pool2.clearMint();
		this.storage.savePool(pool2);

		PoolImpl pool3 = this.storage.newPool();
		pool3.setContext(CONTEXT);
		pool3.setTitle("c");
		pool3.setPoints(Float.valueOf(1));
		pool3.getCreatedBy().setDate(new Date());
		pool3.getCreatedBy().setUserId("admin");
		pool3.getModifiedBy().setDate(new Date());
		pool3.getModifiedBy().setUserId("admin");
		pool3.clearMint();
		this.storage.savePool(pool3);

		PoolImpl pool4 = this.storage.newPool();
		pool4.setContext(CONTEXT);
		pool4.setTitle("a");
		pool4.setPoints(Float.valueOf(5));
		pool4.getCreatedBy().setDate(new Date());
		pool4.getCreatedBy().setUserId("admin");
		pool4.getModifiedBy().setDate(new Date());
		pool4.getModifiedBy().setUserId("admin");
		pool4.clearMint();
		this.storage.savePool(pool4);

		// mint still, so should not show up
		PoolImpl pool5 = this.storage.newPool();
		pool5.setContext(CONTEXT);
		pool5.setTitle("d");
		pool5.setPoints(Float.valueOf(7));
		pool5.getCreatedBy().setDate(new Date());
		pool5.getCreatedBy().setUserId("admin");
		pool5.getModifiedBy().setDate(new Date());
		pool5.getModifiedBy().setUserId("admin");
		this.storage.savePool(pool5);

		// title_a
		List<PoolImpl> pools = this.storage.findPools(CONTEXT, PoolService.FindPoolsSort.title_a);
		assertTrue(pools != null);
		assertTrue(pools.size() == 4);
		assertTrue(pools.get(0).equals(pool1));
		assertTrue(pools.get(1).equals(pool4));
		assertTrue(pools.get(2).equals(pool2));
		assertTrue(pools.get(3).equals(pool3));
		
		// title_d
		pools = this.storage.findPools(CONTEXT, PoolService.FindPoolsSort.title_d);
		assertTrue(pools != null);
		assertTrue(pools.size() == 4);
		assertTrue(pools.get(0).equals(pool3));
		assertTrue(pools.get(1).equals(pool2));
		assertTrue(pools.get(2).equals(pool4));
		assertTrue(pools.get(3).equals(pool1));

		// points_a
		pools = this.storage.findPools(CONTEXT, PoolService.FindPoolsSort.points_a);
		assertTrue(pools != null);
		assertTrue(pools.size() == 4);
		assertTrue(pools.get(0).equals(pool3));
		assertTrue(pools.get(1).equals(pool1));
		assertTrue(pools.get(2).equals(pool4));
		assertTrue(pools.get(3).equals(pool2));

		// points_d
		pools = this.storage.findPools(CONTEXT, PoolService.FindPoolsSort.points_d);
		assertTrue(pools != null);
		assertTrue(pools.size() == 4);
		assertTrue(pools.get(0).equals(pool2));
		assertTrue(pools.get(1).equals(pool4));
		assertTrue(pools.get(2).equals(pool1));
		assertTrue(pools.get(3).equals(pool3));
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		// a data source (see db/pack/components.xml javax.sql.BaseDataSource)
		SakaiBasicDataSource ds = new SakaiBasicDataSource();
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUrl("jdbc:mysql://127.0.0.1:3306/sakai?useUnicode=true&characterEncoding=UTF-8");
		ds.setUsername("sakaiuser");
		ds.setPassword("password");
		ds.setInitialSize(10);
		ds.setMaxActive(10);
		ds.setMaxIdle(10);
		ds.setMinIdle(10);
		ds.setMaxWait(300000);
		ds.setNumTestsPerEvictionRun(3);
		ds.setTestOnBorrow(true);
		ds.setTestOnReturn(false);
		ds.setTestWhileIdle(false);
		ds.setValidationQuery("select 1 from DUAL");
		ds.setDefaultAutoCommit(false);
		ds.setDefaultReadOnly(false);
		ds.setDefaultTransactionIsolationString("TRANSACTION_READ_COMMITTED");
		ds.setPoolPreparedStatements(false);
		ds.setMaxOpenPreparedStatements(0);
		ds.setMinEvictableIdleTimeMillis(1800000);
		ds.setTimeBetweenEvictionRunsMillis(900000);

		// the thread local manager
		ThreadLocalComponent tl = new ThreadLocalComponent();
		tl.init();
		thread_localManager = tl;

		// the SqlService
		SqlServiceTest sql = new SqlServiceTest();
		sql.setVendor("mysql");
		sql.setDefaultDataSource(ds);
		sql.setThreadLocalManager(thread_localManager);
		sql.init();
		sqlService = sql;

		// finally, our target...
		PoolStorageMysql s = new PoolStorageMysql();
		s.setAutoDdl("true");
		s.setPoolService(null);
		s.setQuestionService(null);
		s.setSqlService(sqlService);
		s.setThreadLocalManager(thread_localManager);
		s.init();
		storage = s;

		// clean up from any prior tests
		List<PoolImpl> pools = storage.findPools(CONTEXT, PoolService.FindPoolsSort.title_a);
		for (PoolImpl pool : pools)
		{
			storage.removePool(pool);
		}
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		// clean up from any prior tests
		List<PoolImpl> pools = storage.findPools(CONTEXT, PoolService.FindPoolsSort.title_a);
		for (PoolImpl pool : pools)
		{
			storage.removePool(pool);
		}

		storage.destroy();
		sqlService.destroy();
		thread_localManager.destroy();

		super.tearDown();
	}
}
