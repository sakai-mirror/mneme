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

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

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

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question, String context, String userId)
	{
		if (question == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditQuestion: " + question.getId() + ": " + context + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		// TODO: other users allowed...
		// TODO: or is this user based...
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
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String userId)
	{
		// TODO: other (sort, select, paging, ...) parameters

		if (M_log.isDebugEnabled()) M_log.debug("findQuestions: " + userId);
		List<Question> rv = this.storage.findQuestions(userId);

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
	public Question newQuestion(String context, String userId, Pool pool, String type) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("newQuestion: " + context + ": " + userId);

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		QuestionImpl question = this.storage.newQuestion();
		question.getAttribution().setUserId(userId);
		question.setPool(pool);
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
			M_log.warn("newQuestion: no plugin for type: " + type);
		}

		// TODO: date

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question, String context) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removeQuestion: " + question.getId() + ": " + context);

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		this.storage.removeQuestion((QuestionImpl) question);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question, String context) throws AssessmentPermissionException
	{
		if (question == null) throw new IllegalArgumentException();
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("saveQuestion: " + question.getId() + ": " + context);

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		this.storage.saveQuestion((QuestionImpl) question);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.QUESTION_EDIT, getQuestionReference(question.getId()), true));
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
	public void setPoolService(PoolService service)
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
