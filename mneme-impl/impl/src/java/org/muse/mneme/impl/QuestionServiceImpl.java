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
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
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

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditQuestion: " + question.getId());

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getPool()
				.getContext());

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
	}

	/**
	 * {@inheritDoc}
	 */
	public Question copyQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("copyQuestion: " + question.getId());

		// security check
		String destinationContext = (pool != null) ? pool.getContext() : question.getPool().getContext();
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, destinationContext);

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(pool, false);

		QuestionImpl rv = this.storage.newQuestion((QuestionImpl) question);

		// clear the id to make it new
		rv.id = null;

		// TODO: set the new created info ??
		// rv.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		// rv.getCreatedBy().setDate(new Date());

		// set the new pool, if needed
		if (pool != null)
		{
			rv.setPool(pool);
		}

		// save
		saveQuestion(rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(Pool pool, String search)
	{
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("countQuestions");
		Integer rv = this.storage.countQuestions(null, pool, search);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(String context, String search)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("countQuestions");
		Integer rv = this.storage.countQuestions(context, null, search);

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
	public Boolean existsQuestion(String questionid)
	{
		return this.storage.existsQuestion(questionid);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(Pool pool, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("findQuestions");
		List<Question> rv = this.storage.findQuestions(null, pool, sort, search, pageNum, pageSize);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String context, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("findQuestions");
		List<Question> rv = this.storage.findQuestions(context, null, sort, search, pageNum, pageSize);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion(String questionId)
	{
		if (questionId == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getQuestion: " + questionId);

		// TODO: check to see if id is a valid existing question?
		// this.storage.questionExists(questionId);

		QuestionImpl question = this.storage.getQuestion(questionId);

		return question;
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
	public void moveQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("moveQuestion: " + question.getId() + " to pool: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getPool().getContext());
		// TODO: also check the pool?

		// if to the same pool, do nothing
		if (question.getPool().equals(pool)) return;

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(question.getPool(), false);
		this.poolService.createHistoryIfNeeded(pool, false);

		// do the move
		this.storage.moveQuestion(question, pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question newQuestion(Pool pool, String type) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("newQuestion: pool: " + pool.getId() + " tyrpe: " + type);

		String userId = sessionManager.getCurrentSessionUserId();

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, pool.getContext());

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(pool, false);

		QuestionImpl question = this.storage.newQuestion();
		question.setPool(pool);
		question.initType(type);

		// set the new created info
		question.getCreatedBy().setUserId(userId);
		question.getCreatedBy().setDate(new Date());

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
			M_log.warn("newQuestion: no plugin for type: " + type);
		}

		saveQuestion(question);

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePoolQuestions(Pool pool) throws AssessmentPermissionException
	{
		if (pool == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removePoolQuestions: " + pool.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, pool.getContext());

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(pool, false);

		this.storage.removePoolQuestions(pool);

		// TODO: events?
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removeQuestion: " + question.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getPool().getContext());

		// before anything changes, move to history if needed
		this.poolService.createHistoryIfNeeded(question.getPool(), false);

		this.storage.removeQuestion((QuestionImpl) question);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("saveQuestion: " + question.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, question.getPool().getContext());

		// if the question is new (i.e. no id), set the createdBy information, if not already set
		if ((question.getId() == null) && (question.getCreatedBy().getUserId() == null))
		{
			question.getCreatedBy().setDate(new Date());
			question.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		}

		// update last modified information
		question.getModifiedBy().setDate(new Date());
		question.getModifiedBy().setUserId(sessionManager.getCurrentSessionUserId());

		// see if the question has been moved from its current pool
		QuestionImpl current = this.storage.getQuestion(question.getId());
		if ((current != null) && (!current.getPool().equals(question.getPool())))
		{
			// before anything changes, move to history if needed
			this.poolService.createHistoryIfNeeded(current.getPool(), false);
			this.poolService.createHistoryIfNeeded(question.getPool(), false);
		}

		// deal with direct dependencies on this question from live assessments
		if (this.assessmentService.liveDependencyExists(question))
		{
			// get a new id on the old and save it as history
			current.initId(null);
			current.initHistorical();
			this.storage.saveQuestion(current);

			// switch the manual uses of this question in tests to the history
			this.assessmentService.switchLiveDependency(question, current);
		}

		// deal with (draw-only direct) dependencies to this question's pool from live assessments
		if (this.assessmentService.liveDependencyExists(question.getPool(), true))
		{
			// generate history if needed
			if (!current.isHistorical())
			{
				current.initId(null);
				current.initHistorical();
				this.storage.saveQuestion(current);
			}

			this.poolService.createHistory(current.getPool(), true);
		}

		// if we made history
		if (current.isHistorical())
		{
			// update any frozen manifest pools
			this.poolService.switchManifests(question, current);

			// switch any use of this question in a submission to the history
			this.submissionService.switchLiveDependency(question, current);
		}

		// save
		this.storage.saveQuestion((QuestionImpl) question);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
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
	 * Find all the questions in the pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The List of question ids that are in the pool.
	 */
	protected List<String> getPoolQuestions(Pool pool)
	{
		return this.storage.getPoolQuestions(pool);
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
}
