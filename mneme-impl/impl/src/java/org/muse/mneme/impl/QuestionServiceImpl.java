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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;

/**
 * <p>
 * QuestionServiceImpl implements QuestionService
 * </p>
 */
public class QuestionServiceImpl implements QuestionService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionServiceImpl.class);

	/** Dependency: AssessmentService */
	protected AssessmentServiceImpl assessmentService = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	/** Dependency: PoolService */
	protected PoolServiceImpl poolService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Storage handler. */
	protected QuestionStorage storage = null;

	/** Storage option map key for the option to use. */
	protected String storageKey = null;

	/** Map of registered QuestionStorage options. */
	protected Map<String, QuestionStorage> storgeOptions;

	/** Dependency: SubmissionService */
	protected SubmissionServiceImpl submissionService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditQuestion: " + question.getId());

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getContext());

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageQuestions(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowManageQuestions: " + context + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		// TODO: other users allowed...
		// TODO: or is this user based...
		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintQuestions()
	{
		if (M_log.isDebugEnabled()) M_log.debug("clearStaleMintQuestions");

		// give it a day
		Date stale = new Date();
		stale.setTime(stale.getTime() - (1000l * 60l * 60l * 24l));

		this.storage.clearStaleMintQuestions(stale);
	}

	/**
	 * {@inheritDoc}
	 */
	public void copyPoolQuestions(Pool source, Pool destination) throws AssessmentPermissionException
	{
		if (source == null) throw new IllegalArgumentException();
		if (destination == null) throw new IllegalArgumentException();
		if (source.equals(destination)) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("copyPoolQuestions: source: " + source.getId() + " destination: " + destination.getId());

		String userId = sessionManager.getCurrentSessionUserId();

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, destination.getContext());

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(destination, false);

		this.storage.copyPoolQuestions(userId, source, destination);

		// TODO: event?
	}

	/**
	 * {@inheritDoc}
	 */
	public Question copyQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("copyQuestion: " + question.getId() + ((pool == null) ? "" : (" to pool: " + pool.getId())));

		String userId = sessionManager.getCurrentSessionUserId();
		Date now = new Date();

		// security check
		Pool destination = (pool != null) ? pool : question.getPool();
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, destination.getContext());

		// before anything changes, move the destination pool to history if needed
		this.poolService.createHistoryIfNeeded(destination, false);

		// create a copy of the question
		QuestionImpl rv = this.storage.newQuestion((QuestionImpl) question);

		// clear the id to make it new
		rv.id = null;

		// update created and last modified information
		rv.getCreatedBy().setDate(now);
		rv.getCreatedBy().setUserId(userId);
		rv.getModifiedBy().setDate(now);
		rv.getModifiedBy().setUserId(userId);

		// set the new pool, if needed
		if (pool != null)
		{
			rv.setPool(pool);
		}

		// save
		((QuestionImpl) rv).clearChanged();
		this.storage.saveQuestion((QuestionImpl) rv);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(rv.getId()), true));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(Pool pool, String search)
	{
		if (pool == null) throw new IllegalArgumentException();

		// check the thread-local cache
		String key = cacheKeyPoolCount(pool.getId());
		Integer rv = (Integer) this.threadLocalManager.get(key);
		if (rv != null) return rv;

		if (M_log.isDebugEnabled()) M_log.debug("countQuestions: pre-caching for context: " + pool.getContext() + " pool: " + pool.getId());

		// anticipate that we will want more than just this pool; read the counts of the pools of the context
		Map<String, Integer> counts = this.storage.countPoolQuestions(pool.getContext());

		// and cache them
		for (Map.Entry entry : counts.entrySet())
		{
			key = cacheKeyPoolCount((String) entry.getKey());
			this.threadLocalManager.set(key, entry.getValue());
		}

		// TODO: search

		// check the thread-local cache
		key = cacheKeyPoolCount(pool.getId());
		rv = (Integer) this.threadLocalManager.get(key);
		if (rv != null) return rv;

		if (M_log.isDebugEnabled()) M_log.debug("countQuestions: pool: " + pool.getId());
		rv = this.storage.countPoolQuestions(pool);

		// cache
		this.threadLocalManager.set(key, rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(String context, String search)
	{
		if (context == null) throw new IllegalArgumentException();

		// TODO: search

		// check the thread-local cache
		String key = cacheKeyContextCount(context);
		Integer rv = (Integer) this.threadLocalManager.get(key);
		if (rv != null) return rv;

		if (M_log.isDebugEnabled()) M_log.debug("countQuestions: context: " + context + " search: " + search);

		rv = this.storage.countContextQuestions(context);

		// cache
		this.threadLocalManager.set(key, rv);

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
	public Boolean existsQuestion(String questionId)
	{
		if (questionId == null) return null;

		// for thread-local caching
		String key = cacheKey(questionId);
		QuestionImpl rv = (QuestionImpl) this.threadLocalManager.get(key);
		if (rv != null)
		{
			return true;
		}

		if (M_log.isDebugEnabled()) M_log.debug("existsQuestion: " + questionId);

		// assume we are going to need the question
		// return this.storage.existsQuestion(questionId);
		boolean found = (getQuestion(questionId) != null);
		return found;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(Pool pool, FindQuestionsSort sort, String search, String questionType, Integer pageNum, Integer pageSize)
	{
		if (pool == null) throw new IllegalArgumentException();

		// TODO: search

		if (M_log.isDebugEnabled()) M_log.debug("findQuestions: pool: " + pool.getId());

		return new ArrayList<Question>(this.storage.findPoolQuestions(pool, sort, questionType, pageNum, pageSize));
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String context, FindQuestionsSort sort, String search, String questionType, Integer pageNum, Integer pageSize)
	{
		if (context == null) throw new IllegalArgumentException();

		// TODO: search

		if (M_log.isDebugEnabled()) M_log.debug("findQuestions: context: " + context);

		return new ArrayList<Question>(this.storage.findContextQuestions(context, sort, questionType, pageNum, pageSize));
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion(String questionId)
	{
		if (questionId == null) throw new IllegalArgumentException();

		// for thread-local caching
		String key = cacheKey(questionId);
		QuestionImpl rv = (QuestionImpl) this.threadLocalManager.get(key);
		if (rv != null)
		{
			// return a copy
			return this.storage.newQuestion(rv);
		}

		if (M_log.isDebugEnabled()) M_log.debug("getQuestion: " + questionId);

		rv = this.storage.getQuestion(questionId);

		// thread-local cache (a copy)
		if (rv != null) this.threadLocalManager.set(key, this.storage.newQuestion(rv));

		return rv;
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

			M_log.info("init() storage: " + this.storage);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void moveQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("moveQuestion: " + question.getId() + " to pool: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getContext());
		// TODO: also check the pool?

		// if to the same pool, do nothing
		if (question.getPool().equals(pool)) return;

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(question.getPool(), false);
		this.poolService.createHistoryIfNeeded(pool, false);

		// clear the cache
		String key = cacheKey(question.getId());
		this.threadLocalManager.set(key, null);

		// do the move
		this.storage.moveQuestion(question, pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question newQuestion(Pool pool, String type) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("newQuestion: pool: " + pool.getId() + " type: " + type);

		String userId = sessionManager.getCurrentSessionUserId();

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, pool.getContext());

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(pool, false);

		QuestionImpl question = this.storage.newQuestion();
		question.setPool(pool);

		// set the new created info
		question.getCreatedBy().setUserId(userId);
		question.getCreatedBy().setDate(new Date());

		// set the type, building a type-specific handler
		setType(type, question);

		doSave(question);

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removeQuestion: " + question.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getContext());

		removeQuestion(question, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (((QuestionImpl) question).getIsHistorical()) throw new IllegalArgumentException();

		// if any changes made, clear mint
		if (question.getIsChanged())
		{
			((QuestionImpl) question).clearMint();
		}

		// otherwise we don't save: but if mint, we delete
		else
		{
			// if mint, delete instead of save
			if (((QuestionImpl) question).getMint())
			{
				if (M_log.isDebugEnabled()) M_log.debug("saveQuestion: deleting mint: " + question.getId());

				// Note: mint questions cannot have already been dependened on, so we can just forget about it.
				this.storage.removeQuestion((QuestionImpl) question);
			}

			return;
		}

		if (M_log.isDebugEnabled()) M_log.debug("saveQuestion: " + question.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getContext());

		doSave(question);
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentServiceImpl service)
	{
		assessmentService = service;
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
	 * Dependency: MnemeService.
	 * 
	 * @param service
	 *        The MnemeService.
	 */
	public void setMnemeService(MnemeService service)
	{
		mnemeService = service;
	}

	/**
	 * Dependency: PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolServiceImpl service)
	{
		poolService = service;
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
	 * Dependency: SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionServiceImpl service)
	{
		submissionService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		threadLocalManager = service;
	}

	/**
	 * Form a key for caching a question.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return The cache key.
	 */
	protected String cacheKey(String questionId)
	{
		String key = "mneme:question:" + questionId;
		return key;
	}

	/**
	 * Form the cache key for caching questions-in-context count.
	 * 
	 * @param poolId
	 *        The pool id.
	 * @return The cache key.
	 */
	protected String cacheKeyContextCount(String context)
	{
		return "mneme:question:context:count:" + context;
	}

	/**
	 * Form the cache key for caching questions-in-pool count.
	 * 
	 * @param poolId
	 *        The pool id.
	 * @return The cache key.
	 */
	protected String cacheKeyPoolCount(String poolId)
	{
		return "mneme:question:pool:count:" + poolId;
	}

	/**
	 * Form the cache key for caching question ids in pool.
	 * 
	 * @param poolId
	 *        The pool id.
	 * @return The cache key.
	 */
	protected String cacheKeyPoolQuestions(String poolId)
	{
		return "mneme:question:pool:questions:" + poolId;
	}

	/**
	 * Save the question.
	 * 
	 * @param question
	 *        The question to save.
	 */
	protected void doSave(Question question)
	{
		String userId = sessionManager.getCurrentSessionUserId();
		Date now = new Date();

		// if the question is new (i.e. no id), set the createdBy information, if not already set
		if ((question.getId() == null) && (question.getCreatedBy().getUserId() == null))
		{
			question.getCreatedBy().setDate(now);
			question.getCreatedBy().setUserId(userId);
		}

		// update last modified information
		question.getModifiedBy().setDate(now);
		question.getModifiedBy().setUserId(userId);

		// see if the question has been moved from its current pool
		QuestionImpl current = (question.getId() == null) ? null : this.storage.getQuestion(question.getId());

		Pool currentPool = ((current == null) ? null : current.getPool());

		if ((current != null) && (!currentPool.equals(question.getPool())))
		{
			// TODO: I'm not completely sure of this - ggolden
			// before anything changes, move to history if needed by assessments drawing from the pool
			// or using this question
			// - Note: the 'false' widens this to any assessment drawing from the pool or using any pool question
			// but that's not a real problem -ggolden
			// - when done, any live test at all dependent on this pool (draw or manual) will
			// no longer be dependent on the pool, but will be dependent on the history made from the pool.
			this.poolService.createHistoryIfNeeded(current.getPool(), false);
			this.poolService.createHistoryIfNeeded(question.getPool(), false);

			// clear cache entries for the pool that the question is leaving
			this.threadLocalManager.set(this.cacheKeyPoolCount(current.getPool().getId()), null);
			this.threadLocalManager.set(this.cacheKeyContextCount(current.getContext()), null);
			this.threadLocalManager.set(this.cacheKeyPoolQuestions(current.getPool().getId()), null);
		}

		// deal with direct dependencies on this question from live assessments
		if ((current != null) && (this.assessmentService.liveDependencyExists(question)))
		{
			// get a new id on the old and save it as history
			current.initId(null);
			current.makeHistorical();
			((QuestionImpl) current).clearChanged();
			this.storage.saveQuestion(current);

			// switch the manual uses of this question in tests to the history
			this.assessmentService.switchLiveDependency(question, current);
		}

		// deal with (draw-only, direct) dependencies to this question's pool from live assessments
		if ((current != null) && (this.assessmentService.liveDependencyExists(currentPool, true)))
		{
			// generate history if needed
			if (!current.getIsHistorical())
			{
				current.initId(null);
				current.makeHistorical();
				((QuestionImpl) current).clearChanged();
				this.storage.saveQuestion(current);
			}

			// Note: if the question is moved to a new pool, the code above (createHistoryIfNeeded) will
			// catch it, and we won't be in here, since there will then be no live dependencies on the pool -ggolden
			this.poolService.createHistory(currentPool, true);
		}

		// make history if any submissions reference this question
		if ((current != null) && (!current.getIsHistorical()) && this.submissionService.submissionsDependsOn(current))
		{
			current.initId(null);
			current.makeHistorical();
			((QuestionImpl) current).clearChanged();
			this.storage.saveQuestion(current);
		}

		// if any frozen / historical pool manifests reference this, we need history (skip if we already are going history)
		if ((current != null) && (!current.getIsHistorical()) && this.poolService.manifestDependsOn(current))
		{
			current.initId(null);
			current.makeHistorical();
			((QuestionImpl) current).clearChanged();
			this.storage.saveQuestion(current);
		}

		// if we made history
		if ((current != null) && current.getIsHistorical())
		{
			// update any frozen manifest pools so they refer to the historical (i.e. unchanged) quesion
			// Note: this catches the cases where we just (above) possibly made the pool historical.
			this.poolService.switchManifests(question, current);

			// switch any use of this question in a submission to the history
			this.submissionService.switchLiveDependency(question, current);
		}

		// save
		((QuestionImpl) question).clearChanged();
		this.storage.saveQuestion((QuestionImpl) question);

		// clear the cache
		String key = cacheKey(question.getId());
		this.threadLocalManager.set(key, null);

		// clear thread-local counts for the pool and context and questions
		this.threadLocalManager.set(this.cacheKeyPoolCount(question.getPool().getId()), null);
		this.threadLocalManager.set(this.cacheKeyContextCount(question.getContext()), null);
		this.threadLocalManager.set(this.cacheKeyPoolQuestions(question.getPool().getId()), null);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
	}

	/**
	 * Find all the questions in the pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The List of question ids that are in the pool.
	 */
	protected List<String> getPoolQuestions(Pool pool)
	{
		// check the cache (return a copy)
		String key = cacheKeyPoolQuestions(pool.getId());
		List<String> rv = (List<String>) this.threadLocalManager.get(key);
		if (rv != null) return new ArrayList<String>(rv);

		rv = this.storage.getPoolQuestions(pool);

		// cache (a copy)
		this.threadLocalManager.set(key, new ArrayList<String>(rv));

		return rv;
	}

	/**
	 * Form an question reference for this question id.
	 * 
	 * @param questionId
	 *        the question id.
	 * @return the pool reference for this pool id.
	 */
	protected String getQuestionReference(String questionId)
	{
		String ref = MnemeService.REFERENCE_ROOT + "/" + MnemeService.QUESTION_TYPE + "/" + questionId;
		return ref;
	}

	/**
	 * Remove the question
	 * 
	 * @param question
	 *        The quesiton
	 * @param historyPool
	 *        if the pool's history pool already made, or null if there is none.
	 */
	protected void removeQuestion(Question question, Pool historyPool)
	{
		if (M_log.isDebugEnabled()) M_log.debug("removeQuestion: " + question.getId() + ", " + ((historyPool == null) ? "" : historyPool.getId()));

		// get the current from storage, we may need to make a copy for history
		QuestionImpl current = (question.getId() == null) ? null : this.storage.getQuestion(question.getId());

		// if we don't have one, or we are trying to delete history, that's bad!
		if (current == null) throw new IllegalArgumentException();
		if (current.getIsHistorical()) throw new IllegalArgumentException();
		Pool currentPool = current.getPool();

		// clear the cache
		String key = cacheKey(question.getId());
		this.threadLocalManager.set(key, null);

		// deal with direct dependencies on this question from live assessments
		if (this.assessmentService.liveDependencyExists(question))
		{
			// get a new id on the old and save it as history
			current.initId(null);
			current.makeHistorical();
			((QuestionImpl) current).clearChanged();
			this.storage.saveQuestion(current);

			// switch the manual uses of this question in tests to the history
			this.assessmentService.switchLiveDependency(question, current);
		}

		// any remaining assessment dependencies need to be removed
		this.assessmentService.removeDependency(question);

		// if any locked pool manifests reference this, we need history, too! (skip if we already are going history)
		if ((!current.getIsHistorical()) && this.poolService.manifestDependsOn(current))
		{
			// make sure we have history
			if (!current.getIsHistorical())
			{
				current.initId(null);
				current.makeHistorical();
				((QuestionImpl) current).clearChanged();
				this.storage.saveQuestion(current);
			}
		}

		// deal with (draw-only direct) dependencies to this question's pool from live assessments
		if (this.assessmentService.liveDependencyExists(currentPool, true))
		{
			// make sure we have history
			if (!current.getIsHistorical())
			{
				current.initId(null);
				current.makeHistorical();
				((QuestionImpl) current).clearChanged();
				this.storage.saveQuestion(current);
			}

			// lock down the pool's manifest
			if (historyPool == null)
			{
				historyPool = this.poolService.createHistory(currentPool, true);
			}
		}

		// if any submissions reference this, we need history (skip if we already are going history)
		if ((!current.getIsHistorical()) && this.submissionService.submissionsDependsOn(current))
		{
			// make sure we have history
			if (!current.getIsHistorical())
			{
				current.initId(null);
				current.makeHistorical();
				((QuestionImpl) current).clearChanged();
				this.storage.saveQuestion(current);
			}
		}

		// if we made history
		if (current.getIsHistorical())
		{
			// // set the history question's pool to historyPool, if we have one
			// if (historyPool != null)
			// {
			// // set it to null, so that history questions have no pool
			// // TODO: this might not be what we want, but lets give it a try -golden
			// this.storage.setPool(current, /* historyPool */null);
			// }

			// update any frozen manifest pools
			this.poolService.switchManifests(question, current);

			// switch any use of this question in a submission to the history
			this.submissionService.switchLiveDependency(question, current);
		}

		// delete
		this.storage.removeQuestion((QuestionImpl) question);

		// clear thread-local counts for the pool and context and questions
		this.threadLocalManager.set(this.cacheKeyPoolCount(question.getPool().getId()), null);
		this.threadLocalManager.set(this.cacheKeyContextCount(question.getContext()), null);
		this.threadLocalManager.set(this.cacheKeyPoolQuestions(question.getPool().getId()), null);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
	}

	/**
	 * Set the question type, and set it up with a type-specific handler.
	 * 
	 * @param type
	 *        The type.
	 * @param question
	 *        The question.
	 */
	protected void setType(String type, QuestionImpl question)
	{
		question.initType(type);

		// build a type-specific handler
		QuestionPlugin plugin = this.mnemeService.getQuestionPlugin(type);
		TypeSpecificQuestion handler = null;
		if (plugin != null)
		{
			handler = plugin.newQuestion(question);
		}
		if (handler != null)
		{
			question.initTypeSpecificQuestion(handler);
		}
		else
		{
			M_log.warn("setTypeHandler: no plugin for type: " + type);
		}
	}
}
