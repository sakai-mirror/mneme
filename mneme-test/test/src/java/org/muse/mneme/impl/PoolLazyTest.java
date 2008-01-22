/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolManifestService;
import org.muse.mneme.api.QuestionPoolService;
import org.muse.mneme.impl.PoolImpl;
import org.muse.mneme.impl.PoolImplLazyManifest;

/**
 * Test Pool.
 */
public class PoolLazyTest extends PoolTest
{
	public class MyPoolManifestService implements PoolManifestService
	{
		/**
		 * {@inheritDoc}
		 */
		public List<String> getManifest(Pool pool)
		{
			List<String> rv = new ArrayList<String>();
			rv.add("1");
			rv.add("2");
			rv.add("3");
			rv.add("4");
			rv.add("5");

			return rv;
		}
	}

	public void testFlags() throws Exception
	{
		assertTrue(pool.getMint().equals(Boolean.TRUE));
		assertTrue(pool.getIsHistorical().equals(Boolean.TRUE));
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(PoolTest.class);

	protected PoolManifestService poolManifestService = null;

	/**
	 * @param arg0
	 */
	public PoolLazyTest(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		poolManifestService = new MyPoolManifestService();

		PoolImplLazyManifest p = new PoolImplLazyManifest();
		p.setQuestionService(questionPoolService);
		p.setPoolService(poolManifestService);
		p.initHistorical(Boolean.TRUE);
		pool = p;
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
}
