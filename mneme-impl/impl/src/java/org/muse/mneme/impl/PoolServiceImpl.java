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

	protected AssessmentServiceImpl assessmentService = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	protected QuestionServiceImpl questionService = null;

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
	public Boolean allowManagePools(String context)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("allowManagePools: " + context);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool copyPool(String context, Pool pool) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("copyPool: context: " + context + " id: " + pool.getId());

		String userId = sessionManager.getCurrentSessionUserId();
		Date now = new Date();

		// security check
		this.securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		// make a copy of the pool
		PoolImpl rv = storage.newPool((PoolImpl) pool);

		// clear the id to make it a new one
		rv.id = null;

		// set the context
		rv.setContext(context);

		// update created and last modified information
		rv.getCreatedBy().setDate(now);
		rv.getCreatedBy().setUserId(userId);
		rv.getModifiedBy().setDate(now);
		rv.getModifiedBy().setUserId(userId);

		// clear the changed settings
		((PoolImpl) rv).clearChanged();

		// save
		storage.savePool((PoolImpl) rv);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(rv.getId()), true));

		// make a copy of the questions
		this.questionService.copyPoolQuestions(pool, rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPools(String context, String search)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("countPools: context: " + context);

		Integer rv = storage.countPools(context, search);
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
	public Boolean existsPool(String poolId)
	{
		return this.storage.existsPool(poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String context, FindPoolsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		if (context == null) throw new IllegalArgumentException();
		if (sort == null) sort = PoolService.FindPoolsSort.title_a;

		if (M_log.isDebugEnabled()) M_log.debug("findPools: context: " + context);

		List<Pool> rv = storage.findPools(context, sort, search, pageNum, pageSize);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool(String poolId)
	{
		if (poolId == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getPool: " + poolId);

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
	public Pool newPool(String context) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("newPool: " + context);

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		PoolImpl pool = storage.newPool();

		// attributions will be set in save
		// set the context
		pool.setContext(context);

		// save
		doSave(pool);

		return pool;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool) throws AssessmentPermissionException
	{
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removePool: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, pool.getContext());

		// get the current pool for history
		PoolImpl current = this.storage.getPool(pool.getId());
		if (this.assessmentService.liveDependencyExists(pool, false))
		{
			// get a new id on the old and save it
			current.initId(null);
			current.initHistorical(pool);
			this.storage.savePool(current);

			// swap all historical dependencies to the new
			this.assessmentService.switchLiveDependency(pool, current, false);
		}

		// remove each of our questions
		List<String> qids = current.getAllQuestionIds();
		for (String qid : qids)
		{
			Question q = this.questionService.getQuestion(qid);
			if ((q != null) && (!q.getIsHistorical()))
			{
				// remove the questions
				// use current as the history pool, if needed
				// Note: will only be needed if there are live assessment pool dependencies,
				// - in which case we have already made current into a historical pool.
				this.questionService.removeQuestion(q, current);
			}
		}

		// remove any assessment dependencies on this pool
		// - any live dependencies have already been swapped tot the historical 'current',
		// so these are all non-live and may be removed.
		this.assessmentService.removeDependency(pool);

		// remove the pool
		storage.removePool((PoolImpl) pool);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(pool.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(Pool pool) throws AssessmentPermissionException
	{
		if (pool == null) throw new IllegalArgumentException();

		// if any changes made, clear mint
		if (((PoolImpl) pool).getChanged())
		{
			((PoolImpl) pool).clearMint();
		}

		// otherwise we don't save: but if mint, we delete
		else
		{
			// if mint, delete instead of save
			if (((PoolImpl) pool).getMint())
			{
				if (M_log.isDebugEnabled()) M_log.debug("savePool: deleting mint: " + pool.getId());

				// Note: mint questions cannot have already been dependened on, so we can just forget about it.
				this.storage.removePool((PoolImpl) pool);
			}

			return;
		}

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, pool.getContext());

		doSave(pool);
	}

	/**
	 * Set the AssessmentService.
	 * 
	 * @param service
	 *        the AssessmentService.
	 */
	public void setAssessmentService(AssessmentServiceImpl service)
	{
		this.assessmentService = service;
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
	public void setQuestionService(QuestionServiceImpl service)
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
	 * Create a historical record of this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @param directOnly
	 *        if TRUE, do it only if / for direct pool use (draw), else for all (manual and draw).
	 * @return the history pool made.
	 */
	protected Pool createHistory(Pool pool, boolean directOnly)
	{
		if (pool == null) throw new IllegalArgumentException();
		if (((PoolImpl) pool).getIsHistorical()) return pool;

		if (M_log.isDebugEnabled()) M_log.debug("createHistory: " + pool.getId());

		// get the current pool for history
		PoolImpl current = this.storage.getPool(pool.getId());

		// get a new id on the old and save it
		current.initId(null);
		current.initHistorical(pool);
		this.storage.savePool(current);

		// swap all historical dependencies to the new
		this.assessmentService.switchLiveDependency(pool, current, directOnly);

		// event
		// eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(pool.getId()), true));

		return current;
	}

	/**
	 * Create a historical record of this pool, if needed for live assessment dependencies.
	 * 
	 * @param pool
	 *        The pool.
	 * @param directOnly
	 *        if TRUE, do it only if / for direct pool use (draw), else for all (manual and draw).
	 * @return true if we made history, false if not.
	 */
	protected boolean createHistoryIfNeeded(Pool pool, boolean directOnly)
	{
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("createHistoryIfNeeded: " + pool.getId());

		// if there are any history dependencies on this changed pool, we need to store the history version
		if (this.assessmentService.liveDependencyExists(pool, directOnly))
		{
			createHistory(pool, directOnly);
			return true;
		}

		return false;
	}

	/**
	 * Save the pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void doSave(Pool pool)
	{
		if (M_log.isDebugEnabled()) M_log.debug("doSave: " + pool.getId());

		String userId = sessionManager.getCurrentSessionUserId();

		// get the current pool for history
		PoolImpl current = this.storage.getPool(pool.getId());

		Date now = new Date();

		// if the pool is new (i.e. no id), set the createdBy information, if not already set
		if ((pool.getId() == null) && (pool.getCreatedBy().getUserId() == null))
		{
			pool.getCreatedBy().setDate(now);
			pool.getCreatedBy().setUserId(userId);
		}

		// update last modified information
		pool.getModifiedBy().setDate(now);
		pool.getModifiedBy().setUserId(userId);

		// clear the changed settings
		((PoolImpl) pool).clearChanged();

		// save
		storage.savePool((PoolImpl) pool);

		if (current != null)
		{
			// if there are any history dependencies on this changed pool, we need to store the history version
			// - draws or manual question selection.
			if (this.assessmentService.liveDependencyExists(pool, false))
			{
				// get a new id on the old and save it
				current.initId(null);
				current.initHistorical(pool);
				this.storage.savePool(current);

				// swap all historical dependencies to the new
				this.assessmentService.switchLiveDependency(pool, current, false);
			}
		}

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.POOL_EDIT, getPoolReference(pool.getId()), true));
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
	 * {@inheritDoc}
	 */
	protected List<String> getAllQuestionIds(Pool pool)
	{
		List<String> rv = storage.getAllQuestionIds(pool);
		return rv;
	}

	/**
	 * Get all the pools available to the context.
	 * 
	 * @param context
	 *        The context.
	 * @return The pools available to the context.
	 */
	protected List<Pool> getContextPools(String context)
	{
		if (context == null) throw new IllegalArgumentException();

		// get all the pools for these users
		List<Pool> rv = this.storage.getPools(context);

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
	 * Check if any frozen manifests reference this question.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if any frozen manifests reference the quesiton, FALSE if not.
	 */
	protected Boolean manifestDependsOn(Question question)
	{
		return this.storage.manifestDependsOn(question);
	}

	/**
	 * Switch any pool with a frozen manifest that references from to reference to.
	 * 
	 * @param from
	 *        The from question.
	 * @param to
	 *        The to question.
	 */
	protected void switchManifests(Question from, Question to)
	{
		if (from == null) throw new IllegalArgumentException();
		if (to == null) throw new IllegalArgumentException();

		this.storage.switchManifests(from, to);
	}
}
