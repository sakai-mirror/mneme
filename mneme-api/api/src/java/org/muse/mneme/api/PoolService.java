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
 * PoolService provides services around question pool management for Mneme.
 */
public interface PoolService
{
	/**
	 * Sort options for findPools()
	 */
	enum FindPoolsSort
	{
		points_a, points_d, subject_a, subject_d, title_a, title_d
	}

	/**
	 * Check if the current user is allowed to manage pools from this context.
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the user is allowed, FALSE if not.
	 */
	Boolean allowManagePools(String context, String userId);

	/**
	 * Copy an existing pool creating a new pool.
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        the pool owner (if null, the currrent user is used)
	 * @param pool
	 *        The pool to copy.
	 * @return The new pool as a copy of the old pool, complete with questions.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new pool.
	 */
	Pool copyPool(String context, String userId, Pool pool) throws AssessmentPermissionException;

	/**
	 * Count the pools available to this context with this criteria.
	 * 
	 * @param context
	 *        The context.
	 * @param search
	 *        The search criteria.
	 * @return a list of pools that meet the criteria.
	 */
	Integer countPools(String context, String search);

	/**
	 * Check if this pool exists.
	 * 
	 * @param poolId
	 *        The pool ID.
	 * @return TRUE if the pool exists, FALSE if not.
	 */
	Boolean existsPool(String poolId);

	/**
	 * Locate a list of pools accessible to this context with this criteria.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return a list of pools that meet the criteria.
	 */
	List<Pool> findPools(String context, FindPoolsSort sort, String search, Integer pageNum, Integer pageSize);

	/**
	 * Access a pool by id.
	 * 
	 * @param poolId
	 *        The pool ID.
	 * @return The Pool with this id, or null if not found.
	 */
	Pool getPool(String poolId);

	/**
	 * Get a list of all the subjects of all the pools accessible to this context.
	 * 
	 * @param context
	 *        The context.
	 * @return a list of all the subjects of all the pools accessible to the context.
	 */
	List<String> getSubjects(String context);

	/**
	 * Create a new pool.
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        the pool owner (if null, the currrent user is used)
	 * @return The new pool.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new pool.
	 */
	Pool newPool(String context, String userId) throws AssessmentPermissionException;

	/**
	 * Remove this pool.
	 * 
	 * @param pool
	 *        The pool to remove.
	 * @param context
	 *        The context.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to edit this pool.
	 */
	void removePool(Pool pool, String context) throws AssessmentPermissionException;

	/**
	 * Save changes made to this pool.
	 * 
	 * @param pool
	 *        The pool to save.
	 * @param context
	 *        The context.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to edit this pool.
	 */
	void savePool(Pool pool, String context) throws AssessmentPermissionException;
}
