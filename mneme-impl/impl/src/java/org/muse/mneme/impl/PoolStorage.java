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

import java.util.Date;
import java.util.List;

import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;

/**
 * PoolStorage defines the storage interface for Pools.
 */
public interface PoolStorage
{
	/**
	 * Clear out any mint objects that are old enough to be considered abandoned.
	 * 
	 * @param stale
	 *        The time to compare to the create date; before this they are stale.
	 */
	void clearStaleMintPools(Date stale);

	/**
	 * Check if a pool by this id exists.
	 * 
	 * @param poolId
	 *        The pool id
	 * @return TRUE if the pool with this id exists, FALSE if not.
	 */
	Boolean existsPool(String poolId);

	/**
	 * Find all the pools in this context that meet the criteria.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort criteria.
	 * @return The list of pools that meet the criteria.
	 */
	List<PoolImpl> findPools(String context, PoolService.FindPoolsSort sort);

	/**
	 * Access a pool manifest.
	 * 
	 * @param poolId
	 *        The pool id.
	 * @return The pool manifest from the pool with this id, null if not found or no manifest.
	 */
	List<String> getManifest(String poolId);

	/**
	 * Access a pool by id.
	 * 
	 * @param poolId
	 *        the pool id.
	 * @return The pool with this id, or null if not found.
	 */
	PoolImpl getPool(String poolId);

	/**
	 * Access all pools in the context.
	 * 
	 * @param context
	 *        The context.
	 * @return The List of Pools in the context.
	 */
	List<PoolImpl> getPools(String context);

	/**
	 * Check if any frozen manifests reference this question.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if any frozen manifests reference the quesiton, FALSE if not.
	 */
	Boolean manifestDependsOn(Question question);

	/**
	 * Construct a new pool object.
	 * 
	 * @return A pool object.
	 */
	PoolImpl newPool();

	/**
	 * Construct a new pool object as a copy of another.
	 * 
	 * @param pool
	 *        The pool to copy.
	 * @return A pool object.
	 */
	PoolImpl newPool(PoolImpl pool);

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
	 * Switch any pool with a frozen manifest that references from to reference to.
	 * 
	 * @param from
	 *        The from question.
	 * @param to
	 *        The to question.
	 */
	void switchManifests(Question from, Question to);
}
