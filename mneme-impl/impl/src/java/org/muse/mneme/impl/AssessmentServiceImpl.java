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
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.PolicyException;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.tool.api.SessionManager;

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
	protected SubmissionService submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditAssessment(Assessment assessment, String userId)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEditAssessment: " + assessment.getId() + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, assessment.getContext());

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowListDeliveryAssessment: " + context + ": " + userId);

		// check permission - user must have SUBMIT_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, context);

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageAssessments(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowManageAssessments: " + context + ": " + userId);

		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context);

		return ok;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowRemoveAssessment(Assessment assessment, String userId)
	{
		// user must have manage permission
		if (!this.allowManageAssessments(assessment.getContext(), userId)) return Boolean.FALSE;

		// check policy
		return satisfyAssessmentRemovalPolicy(assessment);
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
	public Assessment getAssessment(String id)
	{
		if (id == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("idAssessment: " + id);

		// TODO: check to see if id is a valid existing assessment?
		// this.storage.assesmentExists(id);

		AssessmentImpl rv = this.storage.getAssessment(id);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context, AssessmentsSort sort)
	{
		if (context == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getContextAssessments: " + context + " " + sort);

		List<Assessment> rv = this.storage.getContextAssessments(context, sort);
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

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(Assessment assessment) throws AssessmentPermissionException, PolicyException
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("removeAssessment: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		// policy check
		if (!satisfyAssessmentRemovalPolicy(assessment)) throw new PolicyException();

		this.storage.removeAssessment((AssessmentImpl) assessment);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.ASSESSMENT_EDIT, getAssessmentReference(assessment.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(Assessment assessment) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("saveAssessment: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		// if the assessment is new (i.e. no id), set the createdBy information, if not already set
		if ((assessment.getId() == null) && (assessment.getCreatedBy().getUserId() == null))
		{
			assessment.getCreatedBy().setDate(new Date());
			assessment.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		}

		// update last modified information
		assessment.getModifiedBy().setDate(new Date());
		assessment.getModifiedBy().setUserId(sessionManager.getCurrentSessionUserId());

		this.storage.saveAssessment((AssessmentImpl) assessment);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.ASSESSMENT_EDIT, getAssessmentReference(assessment.getId()), true));
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
		submissionService = service;
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
	 * Check if this assessment meets the delete policy.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if the assessment may be deleted, FALSE if not.
	 */
	protected Boolean satisfyAssessmentRemovalPolicy(Assessment assessment)
	{
		// TODO: removal policy
		// assessment must have no completed submissions
		if (assessment.getSubmissionCounts().getCompleted() > 0) return Boolean.FALSE;

		return Boolean.TRUE;
	}
}
