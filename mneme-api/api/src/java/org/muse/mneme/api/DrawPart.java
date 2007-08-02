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

package org.muse.mneme.api;

import java.util.List;

/**
 * DrawPart holds a set of manually selected questions.
 */
public interface DrawPart extends Part
{
	/**
	 * Add a pool and count to draw from.
	 * 
	 * @param pool
	 *        The pool to draw from.
	 * @param numQuestions
	 *        The number of questions to draw.
	 */
	PoolDraw addPool(Pool pool, Integer numQuestions);

	/**
	 * Get all the pools and their counts.
	 * 
	 * @return The List of draws.
	 */
	List<PoolDraw> getDraws();

	/**
	 * Get a list of draws for each pool specified - for those that are in se in this part, have a non-null numQuestions.
	 * 
	 * @param userId
	 *        A user id - all the pools for this user will be included.
	 * @param sort
	 *        The sort criteria (from the PoolService).
	 * @param search
	 *        The search criteria.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return A list of draws for each pool.
	 */
	List<PoolDraw> getDrawsForPools(String userId, PoolService.FindPoolsSort sort, String search, Integer pageNum, Integer pageSize);

	/**
	 * Remove a pool's draw from the part.
	 * 
	 * @param pool
	 *        The pool to remove.
	 */
	void removePool(Pool pool);
}
