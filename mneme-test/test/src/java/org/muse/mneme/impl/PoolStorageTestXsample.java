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

import org.apache.commons.dbcp.SakaiBasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test PoolStorageSample
 */
public class PoolStorageTestXsample extends PoolStorageTestX
{
	public class MyPoolStorageSample extends PoolStorageSample
	{
		public PoolImpl newPool()
		{
			return new PoolImpl();
		}
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(PoolStorageTestXsample.class);

	/**
	 * @param arg0
	 */
	public PoolStorageTestXsample(String arg0)
	{
		super(arg0);
	}

	protected SakaiBasicDataSource setupDataSource()
	{
		return null;
	}

	protected PoolStorage setupPoolStorage()
	{
		PoolStorageSample s = new MyPoolStorageSample();
		s.init();

		return s;
	}

	protected void teardownPoolStorage()
	{
		((PoolStorageSample) storage).destroy();
	}

	protected String vendor()
	{
		return null;
	}
}
