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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
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
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String userId)
	{
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("findPools: " + userId);

		List<Pool> rv = storage.findPools(userId);
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
			this.storage.setPoolService(this);
			this.storage.setQuestionService((QuestionServiceImpl) this.questionService);

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

	protected QuestionService questionService = null;

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
}
