/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import junit.framework.TestCase;

import org.apache.commons.dbcp.SakaiBasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.db.impl.BasicSqlService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.thread_local.impl.ThreadLocalComponent;

/**
 * Test base for db tests
 */
public abstract class TestX extends TestCase
{
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
	private static final Log log = LogFactory.getLog(TestX.class);

	protected final static String CONTEXT = "JUNIT_TEST_JUNIT";

	protected SqlServiceTest sqlService = null;

	protected ThreadLocalComponent thread_localManager = null;

	/**
	 * @param arg0
	 */
	public TestX(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		SakaiBasicDataSource ds = setupDataSource();
		if (ds != null)
		{
			// the thread local manager
			ThreadLocalComponent tl = new ThreadLocalComponent();
			tl.init();
			thread_localManager = tl;

			// the SqlService
			SqlServiceTest sql = new SqlServiceTest();
			sql.setVendor(vendor());
			sql.setDefaultDataSource(ds);
			sql.setThreadLocalManager(thread_localManager);
			sql.init();
			sqlService = sql;
		}
	}

	protected abstract SakaiBasicDataSource setupDataSource();

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		if (sqlService != null) sqlService.destroy();
		if (thread_localManager != null) thread_localManager.destroy();

		super.tearDown();
	}

	protected abstract String vendor();
}
