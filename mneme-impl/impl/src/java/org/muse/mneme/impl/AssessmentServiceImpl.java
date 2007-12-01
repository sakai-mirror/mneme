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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

/**
 * AssessmentServiceImpl implements AssessmentService.
 */
public class AssessmentServiceImpl implements AssessmentService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentServiceImpl.class);

	/** A cache of assessments. */
	protected Cache assessmentCache = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Storage handler. */
	protected AssessmentStorage storage = null;

	/** Storage option map key for the option to use. */
	protected String storageKey = null;

	/** Map of registered PoolStorage options. */
	protected Map<String, AssessmentStorage> storgeOptions;

	/** Dependency: SubmissionService */
	protected SubmissionServiceImpl submissionService = null;

	protected UserDirectoryService userDirectoryService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditAssessment(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditAssessment: " + assessment.getId() + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, assessment.getContext());

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context)
	{
		if (context == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowListDeliveryAssessment: " + context + ": " + userId);

		// check permission - user must have SUBMIT_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, context);

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageAssessments(String context)
	{
		if (context == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowManageAssessments: " + context + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowRemoveAssessment(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		// user must have manage permission
		if (!this.allowManageAssessments(assessment.getContext())) return Boolean.FALSE;

		// check policy
		return satisfyAssessmentRemovalPolicy(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintAssessments()
	{
		// give it a day
		Date stale = new Date();
		stale.setTime(stale.getTime() - (1000l * 60l * 60l * 24l));

		this.storage.clearStaleMintAssessments(stale);
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment copyAssessment(String context, Assessment assessment) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("copyAssessment: context:" + context + " id: " + assessment.getId());

		String userId = sessionManager.getCurrentSessionUserId();
		Date now = new Date();

		// security check
		securityService.secure(userId, MnemeService.MANAGE_PERMISSION, context);

		AssessmentImpl rv = this.storage.newAssessment((AssessmentImpl) assessment);

		// clear the id to make it a new one
		rv.id = null;

		// set the context
		rv.setContext(context);

		// clear archived
		rv.setArchived(Boolean.FALSE);

		// clear out any special access
		rv.getSpecialAccess().clear();

		// start out unpublished
		rv.setPublished(Boolean.FALSE);

		// update created and last modified information
		rv.getCreatedBy().setDate(now);
		rv.getCreatedBy().setUserId(userId);
		rv.getModifiedBy().setDate(now);
		rv.getModifiedBy().setUserId(userId);

		// set the parts to their original question and pool values
		for (Part part : rv.getParts().getParts())
		{
			((PartImpl) part).setOrig();
		}

		// save
		this.storage.saveAssessment(rv);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.ASSESSMENT_EDIT, getAssessmentReference(rv.getId()), true));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessments(String context)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("countAssessments: " + context);

		return this.storage.countAssessments(context);
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
	public List<Assessment> getArchivedAssessments(String context)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getArchivedAssessments: " + context);

		List<Assessment> rv = new ArrayList<Assessment>(this.storage.getArchivedAssessments(context));
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getAssessment(String id)
	{
		if (id == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("idAssessment: " + id);

		AssessmentImpl rv = this.storage.getAssessment(id);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context, AssessmentsSort sort, Boolean publishedOnly)
	{
		if (context == null) throw new IllegalArgumentException();
		if (publishedOnly == null) throw new IllegalArgumentException();
		if (sort == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getContextAssessments: " + context + " " + sort);

		List<Assessment> rv = new ArrayList<Assessment>(this.storage.getContextAssessments(context, sort, publishedOnly));
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<User> getSubmitUsers(String context)
	{
		// ge the ids
		Set<String> ids = this.securityService.getUsersIsAllowed(MnemeService.SUBMIT_PERMISSION, context);

		// turn into users
		List<User> users = this.userDirectoryService.getUsers(ids);

		// TODO: sort!
		return users;
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
	public Assessment newAssessment(String context) throws AssessmentPermissionException
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("newAssessment: " + context);

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		AssessmentImpl rv = this.storage.newAssessment();
		rv.setContext(context);
		save(rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removeAssessment: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		// policy check
		if (!satisfyAssessmentRemovalPolicy(assessment)) throw new AssessmentPolicyException();

		// remove incomplete submissions
		// TODO: I'm not sure we can remove if we have submissions started... -ggolden
		//this.submissionService.removeIncompleteAssessmentSubmissions(assessment);

		this.storage.removeAssessment((AssessmentImpl) assessment);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.ASSESSMENT_EDIT, getAssessmentReference(assessment.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException
	{
		if (assessment == null) throw new IllegalArgumentException();

		// if any changes made, clear mint
		if (assessment.getIsChanged())
		{
			((AssessmentImpl) assessment).clearMint();
		}

		// otherwise we don't save: but if mint, we delete
		else
		{
			// if mint, delete instead of save
			if (((AssessmentImpl) assessment).getMint())
			{
				if (M_log.isDebugEnabled()) M_log.debug("saveAssessment: deleting mint: " + assessment.getId());

				// Note: mint questions cannot have already been dependened on, so we can just forget about it.
				this.storage.removeAssessment((AssessmentImpl) assessment);
			}

			return;
		}

		if (M_log.isDebugEnabled()) M_log.debug("saveAssessment: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		// check for changes not allowed if live
		if ((assessment.getIsLive()) && ((AssessmentImpl) assessment).getIsLiveChanged()) throw new AssessmentPolicyException();

		save(assessment);
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
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		questionService = service;
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
	public void setSubmissionService(SubmissionService service)
	{
		submissionService = (SubmissionServiceImpl) service;
	}

	/**
	 * Dependency: UserDirectoryService.
	 * 
	 * @param service
	 *        The UserDirectoryService.
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		userDirectoryService = service;
	}

	/**
	 * Form an assessment reference for this assessment id.
	 * 
	 * @param assessmentId
	 *        the assessment id.
	 * @return the assessment reference for this assessment id.
	 */
	protected String getAssessmentReference(String assessmentId)
	{
		String ref = MnemeService.REFERENCE_ROOT + "/" + MnemeService.ASSESSMENT_TYPE + "/" + assessmentId;
		return ref;
	}

	/**
	 * Check if any live assessments have any dependency on this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @param directOnly
	 *        if TRUE, check only direct use of the pool, if FALSE check indirect as well.
	 * @return TRUE if any live assessments have a dependency on this pool, FALSE if not.
	 */
	protected Boolean liveDependencyExists(Pool pool, Boolean directOnly)
	{
		return this.storage.liveDependencyExists(pool, directOnly);
	}

	/**
	 * Check if any live assessments have any direct dependency on this question.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if any live assessments have a direct dependency on this question, FALSE if not.
	 */
	protected Boolean liveDependencyExists(Question question)
	{
		return this.storage.liveDependencyExists(question);
	}

	/**
	 * Set this assessment to be live.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void makeLive(Assessment assessment)
	{
		this.storage.makeLive(assessment);
	}

	/**
	 * Remove any direct dependencies on this pool from all assessments.
	 * 
	 * @param question
	 *        The question.
	 */
	protected void removeDependency(Pool pool)
	{
		this.storage.removeDependency(pool);
	}

	/**
	 * Remove any direct dependencies on this question from all assessments.
	 * 
	 * @param question
	 *        The question.
	 */
	protected void removeDependency(Question question)
	{
		this.storage.removeDependency(question);
	}

	/**
	 * Check if this assessment meets the delete policy.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if the assessment may be deleted, FALSE if not.
	 */
	protected Boolean satisfyAssessmentRemovalPolicy(Assessment assessment)
	{
		// live tests may not be deleted
		if (assessment.getIsLive()) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void save(Assessment assessment)
	{
		if (M_log.isDebugEnabled()) M_log.debug("save: " + assessment.getId());

		// is there a change that needs to generate history?
		boolean historyNeeded = ((AssessmentImpl) assessment).getHistoryChanged();

		// get the current assessment for history
		AssessmentImpl current = null;
		if (historyNeeded) current = (assessment.getId() == null) ? null : this.storage.getAssessment(assessment.getId());

		Date now = new Date();

		// if the assessment is new (i.e. no id), set the createdBy information, if not already set
		if ((assessment.getId() == null) && (assessment.getCreatedBy().getUserId() == null))
		{
			assessment.getCreatedBy().setDate(now);
			assessment.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		}

		// update last modified information
		assessment.getModifiedBy().setDate(now);
		assessment.getModifiedBy().setUserId(sessionManager.getCurrentSessionUserId());

		// save
		this.storage.saveAssessment((AssessmentImpl) assessment);

		// if there are any history dependencies on this changed assessment, we need to store the history version
		if (historyNeeded && (current != null))
		{
			if (this.submissionService.historicalDependencyExists(assessment))
			{
				// get a new id on the old and save it
				current.initId(null);
				current.makeHistorical();
				this.storage.saveAssessment(current);

				// swap all historical dependencies to the new
				this.submissionService.switchHistoricalDependency(assessment, current);
			}
		}

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.ASSESSMENT_EDIT, getAssessmentReference(assessment.getId()), true));
	}

	/**
	 * Change any live assessments that are dependent on the from pool to become dependent instead on the to pool
	 * 
	 * @param from
	 *        The from pool.
	 * @param to
	 *        The to pool.
	 * @param directOnly
	 *        if true, switch only direct (draw) dependencies, else seitch those as well as (manual) question dependencies.
	 */
	protected void switchLiveDependency(Pool from, Pool to, boolean directOnly)
	{
		this.storage.switchLiveDependency(from, to, directOnly);
	}

	/**
	 * Change any live assessments that are directly dependent on the from question to become dependent instead on the to question
	 * 
	 * @param from
	 *        The from question.
	 * @param to
	 *        The to question.
	 */
	protected void switchLiveDependency(Question from, Question to)
	{
		this.storage.switchLiveDependency(from, to);
	}
}
