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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.impl.PoolImpl;

/**
 * Test Pool
 */
public class PoolTest extends TestCase
{
	/** Logger. */
	private static final Log log = LogFactory.getLog(PoolTest.class);

	protected Pool pool = null;

	/**
	 * @param arg0
	 */
	public PoolTest(String arg0)
	{
		super(arg0);
	}

	/**
	 * Test the description: normal, untrimmed, all blanks, too long
	 * 
	 * @throws Exception
	 */
	public void testDescription() throws Exception
	{
		// normal
		final String TITLE_NORMAL = "title";
		pool.setDescription(TITLE_NORMAL);
		assertTrue(pool.getDescription().equals(TITLE_NORMAL));
		pool.setDescription(null);
		assertTrue(pool.getDescription() == null);

		// untrimmed
		final String TITLE_W_BLANKS = "  title   ";
		final String TITLE_WO_BLANKS = "title";
		pool.setDescription(TITLE_W_BLANKS);
		assertTrue(pool.getDescription().equals(TITLE_WO_BLANKS));

		// all blanks
		final String TITLE_BLANKS = "     ";
		pool.setDescription(TITLE_BLANKS);
		assertTrue(pool.getDescription() == null);

		// too long (length=260)
		final String TITLE_OVERLONG = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		final String TITLE_JUSTRIGHT = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
		pool.setDescription(TITLE_OVERLONG);
		assertTrue(pool.getDescription().equals(TITLE_JUSTRIGHT));
	}

	/**
	 * Test the difficulty
	 * 
	 * @throws Exception
	 */
	public void testDifficulty() throws Exception
	{
		// test default
		assertTrue(pool.getDifficulty() == 3);

		try
		{
			pool.setDifficulty(null);
			fail("expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
		}
		assertTrue(pool.getDifficulty() == 3);

		pool.setDifficulty(Integer.valueOf(0));
		assertTrue(pool.getDifficulty() == 1);

		pool.setDifficulty(Integer.valueOf(1));
		assertTrue(pool.getDifficulty() == 1);

		pool.setDifficulty(Integer.valueOf(2));
		assertTrue(pool.getDifficulty() == 2);

		pool.setDifficulty(Integer.valueOf(3));
		assertTrue(pool.getDifficulty() == 3);

		pool.setDifficulty(Integer.valueOf(4));
		assertTrue(pool.getDifficulty() == 4);

		pool.setDifficulty(Integer.valueOf(5));
		assertTrue(pool.getDifficulty() == 5);

		pool.setDifficulty(Integer.valueOf(6));
		assertTrue(pool.getDifficulty() == 5);
	}

	/**
	 * Test the points
	 * 
	 * @throws Exception
	 */
	public void testPoints() throws Exception
	{
		// unset, the points should be zero
		final Float ZERO = 0f;
		assertTrue(pool.getPoints().equals(ZERO));
		assertTrue(pool.getPointsEdit() == null);

		// 1.5
		final Float ONE_FIVE = 1.5f;
		pool.setPoints(ONE_FIVE);
		assertTrue(pool.getPoints().equals(ONE_FIVE));
		assertTrue(pool.getPointsEdit().equals(ONE_FIVE));

		// 0
		pool.setPoints(ZERO);
		assertTrue(pool.getPoints().equals(ZERO));
		assertTrue(pool.getPointsEdit().equals(ZERO));

		// big number
		final Float BIG = Float.MAX_VALUE;
		pool.setPoints(BIG);
		assertTrue(pool.getPoints().equals(BIG));
		assertTrue(pool.getPointsEdit().equals(BIG));

		// null
		try
		{
			pool.setPoints(null);
			fail("expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
		}
		assertTrue(pool.getPoints().equals(BIG));
		assertTrue(pool.getPointsEdit().equals(BIG));

		// null, but allowed
		pool.setPointsEdit(null);
		assertTrue(pool.getPointsEdit() == null);
		assertTrue(pool.getPoints().equals(ZERO));

		// value using set setPointsEdit
		pool.setPointsEdit(ONE_FIVE);
		assertTrue(pool.getPoints().equals(ONE_FIVE));
		assertTrue(pool.getPointsEdit().equals(ONE_FIVE));

		// negative (ignored)
		final Float NEGATIVE = -1.5f;
		pool.setPoints(NEGATIVE);
		assertTrue(pool.getPoints().equals(ONE_FIVE));
		assertTrue(pool.getPointsEdit().equals(ONE_FIVE));
	}

	/**
	 * Test the title: normal, untrimmed, all blanks, too long
	 * 
	 * @throws Exception
	 */
	public void testTitle() throws Exception
	{
		// normal title
		final String TITLE_NORMAL = "title";
		pool.setTitle(TITLE_NORMAL);
		assertTrue(pool.getTitle().equals(TITLE_NORMAL));
		pool.setTitle(null);
		assertTrue(pool.getTitle() == null);

		// untrimmed title
		final String TITLE_W_BLANKS = "  title   ";
		final String TITLE_WO_BLANKS = "title";
		pool.setTitle(TITLE_W_BLANKS);
		assertTrue(pool.getTitle().equals(TITLE_WO_BLANKS));

		// all blanks
		final String TITLE_BLANKS = "     ";
		pool.setTitle(TITLE_BLANKS);
		assertTrue(pool.getTitle() == null);

		// too long (length=260)
		final String TITLE_OVERLONG = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		final String TITLE_JUSTRIGHT = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
		pool.setTitle(TITLE_OVERLONG);
		assertTrue(pool.getTitle().equals(TITLE_JUSTRIGHT));
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		pool = new PoolImpl(null, null);
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
}
