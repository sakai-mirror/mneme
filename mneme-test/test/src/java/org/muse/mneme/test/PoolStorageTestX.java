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

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.dbcp.SakaiBasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.impl.PoolImpl;
import org.muse.mneme.impl.PoolStorage;
import org.muse.mneme.impl.PoolStorageMysql;
import org.muse.mneme.impl.PoolStorageSample;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.db.impl.BasicSqlService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.thread_local.impl.ThreadLocalComponent;

/**
 * Test Pool
 */
public class PoolStorageTestX extends TestCase
{
	public class SqlServiceTest extends BasicSqlService
	{
		protected ThreadLocalManager threadLocalManager()
		{
			// TODO: might have to mock
			return null;
		}

		protected UsageSessionService usageSessionService()
		{
			// TODO: might have to mock
			return null;
		}
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(PoolStorageTestX.class);

	protected SqlService sqlService = null;

	protected PoolStorage storage = null;

	protected ThreadLocalManager thread_localManager = null;

	/**
	 * @param arg0
	 */
	public PoolStorageTestX(String arg0)
	{
		super(arg0);
	}

	/**
	 * Test the description: normal, untrimmed, all blanks, too long
	 * 
	 * @throws Exception
	 */
	public void testX() throws Exception
	{
		log.info(storage.toString());
		List<PoolImpl> pools = storage.getPools("mercury");
		assertTrue(pools != null);
		assertTrue(pools.size() > 0);
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

		// the SqlService
		BasicSqlService sql = new SqlServiceTest();
		sql.setVendor("mysql");
		sql.setDefaultDataSource(ds);
		sql.init();
		sqlService = sql;

		// the thread local manager
		ThreadLocalComponent tl = new ThreadLocalComponent();
		tl.init();
		thread_localManager = tl;

		// finally, our target...
		PoolStorageMysql s = new PoolStorageMysql();
		s.setAutoDdl("true");
		s.setPoolService(null);
		s.setQuestionService(null);
		s.setSqlService(sqlService);
		s.setThreadLocalManager(thread_localManager);
		s.init();
		storage = s;
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		((PoolStorageSample) storage).destroy();
		((SqlServiceTest) sqlService).destroy();
		((ThreadLocalComponent) thread_localManager).destroy();

		super.tearDown();
	}
}
