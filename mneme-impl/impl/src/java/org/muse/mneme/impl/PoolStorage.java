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

import java.util.List;

import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;

/**
 * PoolStorage defines the storage interface for Pools.
 */
public interface PoolStorage
{
	/**
	 * Draw a set of questions from the pool.
	 * 
	 * @param pool
	 *        The pool to draw from.
	 * @oaram seed A Random seed for the random draw.
	 * @param numQuestions
	 *        The number of questions to draw.
	 * @return a List of question ids drawn from the pool.
	 */
	List<String> drawQuestionIds(Pool pool, long seed, Integer numQuestions);

	/**
	 * Find all the pools this user has access to.
	 * 
	 * @param userId
	 *        The user id
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @return The list of pools the user has access to.
	 */
	List<Pool> findPools(String userId, PoolService.FindPoolsSort sort, String search);

	/**
	 * Access a pool by id.
	 * 
	 * @param poolId
	 *        the pool id.
	 * @return The pool with this id, or null if not found.
	 */
	PoolImpl getPool(String poolId);

	/**
	 * Count the questions in a pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @return The number of questions in the pool.
	 */
	Integer getPoolSize(PoolImpl pool);

	/**
	 * Construct a new pool object.
	 * 
	 * @return A pool object.
	 */
	PoolImpl newPool();

	/**
	 * Check if a pool by this id exists.
	 * 
	 * @param poolId
	 *        The pool id
	 * @return TRUE if the pool with this id exists, FALSE if not.
	 */
	Boolean poolExists(String poolId);

	/**
	 * Remove a pool from storage.
	 * 
	 * @param pool
	 *        The pool to remove.
	 */
	void removePool(PoolImpl pool);

	/**
	 * Save changes made to this pool.
	 * 
	 * @param pool
	 *        the pool to save.
	 */
	void savePool(PoolImpl pool);

	/**
	 * Set the PoolService
	 * 
	 * @param service
	 *        The PoolService.
	 */
	void setPoolService(PoolServiceImpl service);

	/**
	 * Set the QuestionService
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	void setQuestionService(QuestionServiceImpl service);
}
