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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.QuestionService.FindQuestionsSort;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.tool.api.SessionManager;

/**
 * <p>
 * PoolServiceImpl implements PoolService
 * </p>
 */
public class PoolServiceImpl implements PoolService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(PoolServiceImpl.class);

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Storage handler. */
	protected PoolStorage storage = null;

	/** Storage option map key for the option to use. */
	protected String storageKey = null;

	/** Map of registered PoolStorage options. */
	protected Map<String, PoolStorage> storgeOptions;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditPool(Pool pool, String context, String userId)
	{
		if (pool == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditPool: " + pool.getId() + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		// TODO: other users allowed...
		// TODO: or is this user based...
		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManagePools(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowManagePools: " + context + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		// TODO: other users allowed...
		// TODO: or is this user based...
		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool copyPool(String context, String userId, Pool pool) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		if (pool == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("copyPool: " + context);

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		// make a copy of the pool
		PoolImpl rv = storage.newPool((PoolImpl) pool);
		rv.setOwnerId(userId);
		savePool(rv, context);

		// make a copy of the questions
		// TODO:

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPools(String context, String userId, String search)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("countPools: " + userId);

		Integer rv = storage.countPools(context, userId, search);
		return rv;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String context, String userId, FindPoolsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		if (sort == null) sort = PoolService.FindPoolsSort.title_a;

		if (M_log.isDebugEnabled()) M_log.debug("findPools: " + userId);

		List<Pool> rv = storage.findPools(context, userId, sort, search, pageNum, pageSize);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool(String poolId)
	{
		if (poolId == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getPool: " + poolId);

		// TODO: check to see if id is a valid existing pool?
		// storage.checkPool(poolId);

		PoolImpl pool = this.storage.getPool(poolId);

		return pool;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSubjects(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		return this.storage.getSubjects(context, userId);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// storage - as configured
			if (this.storageKey != null)
			{
				// if set to "SQL", replace with the current SQL vendor
				if ("SQL".equals(this.storageKey))
				{
					this.storageKey = sqlService.getVendor();
				}

				this.storage = this.storgeOptions.get(this.storageKey);
			}

			// use "default" if needed
			if (this.storage == null)
			{
				this.storage = this.storgeOptions.get("default");
			}

			if (storage == null) M_log.warn("no storage set: " + this.storageKey);

			M_log.info("init()");
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool newPool(String context, String userId) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("newPool: " + context);

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		PoolImpl pool = storage.newPool();
		pool.setOwnerId(userId);
		savePool(pool, context);

		return pool;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool, String context) throws AssessmentPermissionException
	{
		if (pool == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removePool: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		// remove the questions from the pool
		this.questionService.removePoolQuestions(pool, context);

		storage.removePool((PoolImpl) pool);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(pool.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(Pool pool, String context) throws AssessmentPermissionException
	{
		if (pool == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("savePool: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		// if the pool is new (i.e. no id), set the createdBy information, if not already set
		if ((pool.getId() == null) && (pool.getCreatedBy().getUserId() == null))
		{
			pool.getCreatedBy().setDate(new Date());
			pool.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		}

		// update last modified information
		pool.getModifiedBy().setDate(new Date());
		pool.getModifiedBy().setUserId(sessionManager.getCurrentSessionUserId());

		storage.savePool((PoolImpl) pool);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(pool.getId()), true));
	}

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		eventTrackingService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		sessionManager = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		sqlService = service;
	}

	/**
	 * Set the storage class options.
	 * 
	 * @param options
	 *        The PoolStorage options.
	 */
	public void setStorage(Map options)
	{
		this.storgeOptions = options;
	}

	/**
	 * Set the storage option key to use, selecting which PoolStorage to use.
	 * 
	 * @param key
	 *        The storage option key.
	 */
	public void setStorageKey(String key)
	{
		this.storageKey = key;
	}

	/**
	 * Count the questions in this pool with this criteria.
	 * 
	 * @param pool
	 *        The pool.
	 * @param userId
	 *        The user id (if null, the current user is used).
	 * @param search
	 *        The search criteria.
	 * @return The questions in this pool with this criteria.
	 */
	protected Integer countQuestions(Pool pool, String userId, String search)
	{
		Integer rv = this.questionService.countQuestions(userId, pool, search);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	protected List<String> drawQuestionIds(Pool pool, long seed, Integer numQuestions)
	{
		List<String> rv = storage.drawQuestionIds(pool, seed, numQuestions);
		return rv;
	}

	/**
	 * Form an pool reference for this pool id.
	 * 
	 * @param poolId
	 *        the pool id.
	 * @return the pool reference for this pool id.
	 */
	protected String getPoolReference(String poolId)
	{
		String ref = MnemeService.REFERENCE_ROOT + "/" + MnemeService.POOL_TYPE + "/" + poolId;
		return ref;
	}

	/**
	 * Count the questions in a pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @return The number of questions in the pool.
	 */
	protected Integer getPoolSize(Pool pool)
	{
		Integer rv = storage.getPoolSize((PoolImpl) pool);
		return rv;
	}

	/**
	 * Locate a list of questions in this pool with this criteria.
	 * 
	 * @param pool
	 *        The pool.
	 * @param userId
	 *        the user id (if null, the current user is used).
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return a list of questions that meet the criteria.
	 */
	protected List<Question> getQuestions(Pool pool, String userId, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		List<Question> rv = this.questionService.findQuestions(userId, pool, sort, search, pageNum, pageSize);
		return rv;
	}
}
