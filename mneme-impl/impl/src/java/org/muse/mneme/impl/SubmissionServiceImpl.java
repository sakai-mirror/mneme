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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionCounts;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

/**
 * SubmissionServiceImpl implements SubmissionService
 */
public class SubmissionServiceImpl implements SubmissionService, Runnable
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionServiceImpl.class);

	/** Dependency: AssessmentService */
	protected AssessmentService assessmentService = null;

	/** The checker thread. */
	protected Thread checkerThread = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	/** Dependency: GradebookExternalAssessmentService */
	// for 2.4 only: protected GradebookExternalAssessmentService m_gradebookService = null;
	protected GradebookService gradebookService = null;

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Storage handler. */
	protected SubmissionStorage storage = null;

	/** Storage option map key for the option to use. */
	protected String storageKey = null;

	/** Map of registered SubmissionStorage options. */
	protected Map<String, SubmissionStorage> storgeOptions;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager threadLocalManager = null;

	/** The thread quit flag. */
	protected boolean threadStop = false;

	/** How long to wait (ms) between checks for timed-out submission in the db. 0 disables. */
	protected long timeoutCheckMs = 1000L * 300L;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission, String userId)
	{
		if (submission == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		Assessment assessment = submission.getAssessment();
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("allowCompleteSubmission: " + submission.getId() + ": " + userId);

		// user must be this submission's user
		if (!submission.getUserId().equals(userId)) return Boolean.FALSE;

		// submission must be incomplete
		if (submission.getIsComplete()) return Boolean.FALSE;

		// user must have submit permission inthe context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext())) return Boolean.FALSE;

		// the assessment must be currently open for submission (with a grace period to allow completion near closing time)
		if (!assessment.getIsOpen(Boolean.TRUE)) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(String context, String userId)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEvaluate_context: " + context + ": " + userId);

		// user must have grade permission in the context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.GRADE_PERMISSION, context)) return Boolean.FALSE;

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(Submission submission, String userId)
	{
		if (submission == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEvaluate: " + submission.getId() + ": " + userId);

		// the submission must be complete
		if (!submission.getIsComplete()) return Boolean.TRUE;

		// user must have grade permission in the context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.GRADE_PERMISSION, submission.getAssessment().getContext())) return Boolean.FALSE;

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission, String userId)
	{
		if (submission == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		Assessment assessment = submission.getAssessment();
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("allowReviewSubmission: " + submission.getId() + ": " + userId);

		// user must be this submission's user
		if (!submission.getUserId().equals(userId)) return Boolean.FALSE;

		// submission must be complete
		if (!submission.getIsComplete()) return Boolean.FALSE;

		// TODO: check on review now?

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Assessment assessment, String userId)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowSubmit: " + assessment.getId() + ": " + userId);

		// user must have submit permission inthe context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext())) return Boolean.FALSE;

		// the assessment must be currently open for submission
		if (!assessment.getIsOpen(Boolean.FALSE)) return Boolean.FALSE;

		// if the user has a submission in progress, this is good
		if (this.storage.getSubmissionInProgress(assessment, userId) != null) return Boolean.TRUE;

		// if the user can submit a new one, this is good
		Integer remaining = countRemainingSubmissions(assessment, userId);
		if ((remaining == null) || (remaining > 0)) return Boolean.TRUE;

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(Submission s) throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException
	{
		if (s == null) throw new IllegalArgumentException();
		Submission submission = getSubmission(s.getId());
		Assessment assessment = submission.getAssessment();
		if (assessment == null) throw new IllegalArgumentException();
		Date asOf = new Date();

		// submission must be incomplete
		if (submission.getIsComplete()) throw new SubmissionCompletedException();

		// the current user must be the submission user
		if (!submission.getUserId().equals(sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) assessmentService).getAssessmentReference(assessment.getId()));
		}

		// user must have SUBMIT_PERMISSION in the context of the assessment
		securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// the assessment must be currently open for submission (with the grace period to support completion near closing time)
		if (!assessment.getIsOpen(Boolean.TRUE)) throw new AssessmentClosedException();

		if (M_log.isDebugEnabled()) M_log.debug("completeSubmission: submission: " + submission.getId());

		// update the submission
		submission.setSubmittedDate(asOf);
		submission.setIsComplete(Boolean.TRUE);

		// if grade at submission
		if (assessment.getGrading().getAutoRelease())
		{
			submission.setIsReleased(Boolean.TRUE);
		}

		// store the changes
		this.storage.saveSubmission((SubmissionImpl) submission);

		// record in the gradebook if so configured
		if (assessment.getGrading().getGradebookIntegration() && submission.getIsReleased())
		{
			recordInGradebook(submission, true);
		}

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(Assessment assessment, String userId)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("countRemainingSubmissions: assessment: " + assessment.getId() + " userId: " + userId + " asOf: " + asOf);

		// check the assessment's max submissions
		Integer allowed = assessment.getNumSubmissionsAllowed();

		// if unlimited, send back a null to indicate this.
		if (allowed == null) return null;

		// get a count of completed submissions by this user to the assessment
		Integer completed = this.storage.countCompleteSubmissions(assessment, userId);

		// how many tries left?
		Integer remaining = allowed - completed;
		if (remaining < 0) remaining = 0;

		return remaining;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		// stop the checking thread
		stop();

		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission enterSubmission(Assessment a, String userId) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		if (a == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		Assessment assessment = assessmentService.getAssessment(a.getId());
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("enterSubmission: assessment: " + assessment.getId() + " user: " + userId + " asOf: " + asOf);

		// user must have SUBMIT_PERMISSION in the context of the assessment
		securityService.secure(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// the assessment must be currently open for submission
		if (!assessment.getIsOpen(Boolean.FALSE)) throw new AssessmentClosedException();

		// use the one in progress if
		Submission submissionInProgress = this.storage.getSubmissionInProgress(assessment, userId);
		if (submissionInProgress != null)
		{
			// event track it (not a modify event)
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_CONTINUE, getSubmissionReference(submissionInProgress
					.getId()), false));

			return submissionInProgress;
		}

		// the user must be able to create a new submission
		Integer remaining = countRemainingSubmissions(assessment, userId);
		if ((remaining != null) && (remaining == 0)) throw new AssessmentCompletedException();

		// TODO: it is possible to make too many submissions for the assessment. If this method is entered concurrently for the same user and
		// assessment, the previous count check might fail.

		// make a new submission
		SubmissionImpl submission = this.storage.newSubmission();
		submission.initAssessmentId(assessment.getId());
		submission.initUserId(userId);
		submission.setIsComplete(Boolean.FALSE);
		submission.setStartDate(asOf);
		submission.setSubmittedDate(asOf);

		// store the new submission, setting the id
		this.storage.saveSubmission(submission);

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_ENTER, getSubmissionReference(submission.getId()), true));

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateSubmissions(Assessment assessment, String comment, Float score, Boolean markGraded) throws AssessmentPermissionException
	{
		// TODO:

		// for each submission to this assessment

		// only for completed ones

		// only for the "official" one

		// get the evaluation

		// set it

		// save the submission
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort, Boolean official, Integer pageNum,
			Integer pageSize)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (official == null) throw new IllegalArgumentException();
		if (sort == null) sort = FindAssessmentSubmissionsSort.userName_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("findAssessmentSubmissions: assessment: " + assessment.getId());

		// read all the submissions for this user in the context, with all the assessment and submission data we need
		// each assessment is covered with at least one - if there are no submission yet for a user, an empty submission is included
		List<SubmissionImpl> all = this.storage.findAssessmentSubmissions(assessment, sort);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> rv = null;
		if (official)
		{
			rv = officialize(all);
		}
		else
		{
			rv.addAll(all);
		}

		// if sorting by status, do that sort
		if (sort == FindAssessmentSubmissionsSort.status_a || sort == FindAssessmentSubmissionsSort.status_d)
		{
			rv = sortByStatus((sort == FindAssessmentSubmissionsSort.status_d), rv);
		}

		// TODO: similar sort for "graded_a/_d status"

		// page the results
		if ((pageNum != null) && (pageSize != null))
		{
			// start at ((pageNum-1)*pageSize)
			int start = ((pageNum - 1) * pageSize);
			if (start < 0) start = 0;
			if (start > rv.size()) start = rv.size() - 1;

			// end at ((pageNum)*pageSize)-1, or max-1, (note: subList is not inclusive for the end position)
			int end = ((pageNum) * pageSize);
			if (end < 0) end = 0;
			if (end > rv.size()) end = rv.size();

			rv = rv.subList(start, end);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Answer> findSubmissionAnswers(Assessment assessment, Question question, FindAssessmentSubmissionsSort sort, Boolean official,
			Integer pageNum, Integer pageSize)
	{
		// TODO: review the efficiency of this method! -ggolden

		if (assessment == null) throw new IllegalArgumentException();
		if (question == null) throw new IllegalArgumentException();
		if (official == null) throw new IllegalArgumentException();
		if (sort == null) sort = FindAssessmentSubmissionsSort.userName_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("findAssessmentSubmissions: assessment: " + assessment.getId());

		// read all the submissions for this user in the context, with all the assessment and submission data we need
		// each assessment is covered with at least one - if there are no submission yet for a user, an empty submission is included
		List<SubmissionImpl> all = this.storage.findAssessmentSubmissions(assessment, sort);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> rv = null;
		if (official)
		{
			rv = officialize(all);
		}
		else
		{
			rv.addAll(all);
		}

		// if sorting by status, do that sort
		if (sort == FindAssessmentSubmissionsSort.status_a || sort == FindAssessmentSubmissionsSort.status_d)
		{
			rv = sortByStatus((sort == FindAssessmentSubmissionsSort.status_d), rv);
		}

		// TODO: similar sort for "graded_a/_d status"

		// page the results
		if ((pageNum != null) && (pageSize != null))
		{
			// start at ((pageNum-1)*pageSize)
			int start = ((pageNum - 1) * pageSize);
			if (start < 0) start = 0;
			if (start > rv.size()) start = rv.size() - 1;

			// end at ((pageNum)*pageSize)-1, or max-1, (note: subList is not inclusive for the end position)
			int end = ((pageNum) * pageSize);
			if (end < 0) end = 0;
			if (end > rv.size()) end = rv.size();

			rv = rv.subList(start, end);
		}

		// pull out the one answer we want
		List<Answer> answers = new ArrayList<Answer>();
		for (Submission s : rv)
		{
			answers.add(s.getAnswer(question));
		}

		return answers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentIsFullyReleased(Assessment assessment)
	{
		return this.storage.getAssessmentIsFullyReleased(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		List<Float> rv = this.storage.getAssessmentScores(assessment);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		final List<Float> rv = this.storage.getQuestionScores(question);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(String id)
	{
		if (id == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getSubmission: " + id);

		// TODO: check storage to see that it really exists?
		// this.storage.submissionExists(id);

		SubmissionImpl submission = this.storage.getSubmission(id);

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionCounts getSubmissionCounts(Assessment assessment)
	{
		return this.storage.getSubmissionCounts(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		if (sort == null) sort = GetUserContextSubmissionsSort.title_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("getUserContextSubmissions: context: " + context + " userId: " + userId + ": " + sort);

		// read all the submissions for this user in the context, with all the assessment and submission data we need
		// each assessment is covered with at least one - if there are no submission yet for an assessment, an empty submission is returned
		List<SubmissionImpl> all = this.storage.getUserContextSubmissions(context, userId, sort);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = officialize(all);

		// if sorting by due date, fix it so null due dates are LARGE not SMALL
		if (sort == GetUserContextSubmissionsSort.dueDate_a || sort == GetUserContextSubmissionsSort.dueDate_d
				|| sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		{
			// pull out the null date entries
			List<Submission> nulls = new ArrayList<Submission>();
			for (Iterator i = official.iterator(); i.hasNext();)
			{
				Submission s = (Submission) i.next();
				if (s.getAssessment().getDates().getDueDate() == null)
				{
					nulls.add(s);
					i.remove();
				}
			}

			// for ascending, treat the null dates as LARGE so put them at the end
			if (sort == GetUserContextSubmissionsSort.dueDate_a)
			{
				official.addAll(nulls);
			}

			// for descending, (all status is first sorted date descending) treat the null dates as LARGE so put them at the beginning
			else
			{
				nulls.addAll(official);
				official.clear();
				official.addAll(nulls);
			}
		}

		// if sorting by status, do that sort
		if (sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		{
			official = sortByStatus((sort == GetUserContextSubmissionsSort.status_d), official);
		}

		return official;
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

			// start the checking thread
			if (timeoutCheckMs > 0)
			{
				start();
			}

			M_log.info("init(): timout check seconds: " + timeoutCheckMs / 1000);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeIncompleteAssessmentSubmissions(Assessment assessment) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();

		// permission
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		this.storage.removeIncompleteAssessmentSubmissions(assessment);

		// TODO: events?
	}

	/**
	 * Run the event checking thread.
	 */
	public void run()
	{
		// since we might be running while the component manager is still being created and populated,
		// such as at server startup, wait here for a complete component manager
		ComponentManager.waitTillConfigured();

		// loop till told to stop
		while ((!threadStop) && (!Thread.currentThread().isInterrupted()))
		{
			try
			{
				// get a list of submission ids that are open, timed, and well expired (considering double our grace period),
				// or open and past an accept-until date
				List<Submission> submissions = getTimedOutSubmissions(2 * MnemeService.GRACE);

				// for each one, close it if it is still open
				for (Submission submission : submissions)
				{
					// we need to establish the "current" user to be the submission user
					// so that various attributions of the complete process have the proper user
					String user = submission.getUserId();
					Session s = sessionManager.getCurrentSession();
					if (s != null)
					{
						s.setUserId(user);
					}
					else
					{
						M_log.warn("run - no SessionManager.getCurrentSession, cannot set to user");
					}

					// complete this submission, using the exact 'over' date for the final date
					Date over = submission.getWhenOver();
					autoCompleteSubmission(over, submission);
				}
			}
			catch (Throwable e)
			{
				M_log.warn("run: will continue: ", e);
			}
			finally
			{
				// clear out any current current bindings
				this.threadLocalManager.clear();
			}

			// take a small nap
			try
			{
				Thread.sleep(timeoutCheckMs);
			}
			catch (Exception ignore)
			{
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmission(Submission submission) throws AssessmentPermissionException
	{
		if (submission == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("saveSubmission: " + submission.getId());

		// security check
		// TODO: grade_permission?
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, submission.getAssessment().getContext());

		// if the assessment is new (i.e. no id), set the createdBy information, if not already set
		// TODO:???
		// if ((submission.getId() == null) && (submission.getCreatedBy().getUserId() == null))
		// {
		// submission.getCreatedBy().setDate(new Date());
		// submission.getCreatedBy().setUserId(sessionManager.getCurrentSessionUserId());
		// }
		//
		// // update last modified information
		// submission.getModifiedBy().setDate(new Date());
		// submission.getModifiedBy().setUserId(sessionManager.getCurrentSessionUserId());

		this.storage.saveSubmission((SubmissionImpl) submission);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_GRADE, getSubmissionReference(submission.getId()), true));
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
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
		this.eventTrackingService = service;
	}

	/**
	 * Dependency: GradebookService.
	 * 
	 * @param service
	 *        The GradebookService.
	 */
	public void setGradebookService(/* for 2.4 only: GradebookExternalAssessmentService */GradebookService service)
	{
		this.gradebookService = service;
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
		this.securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
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
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * Set the # seconds to wait between db checks for timed-out submissions.
	 * 
	 * @param time
	 *        The # seconds to wait between db checks for timed-out submissions.
	 */
	public void setTimeoutCheckSeconds(String time)
	{
		this.timeoutCheckMs = Integer.parseInt(time) * 1000L;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean submissionsExist(Assessment assessment)
	{
		return this.storage.submissionsExist(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(Answer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		if (answer == null) throw new IllegalArgumentException();
		if (completeAnswer == null) throw new IllegalArgumentException();
		if (completeSubmission == null) throw new IllegalArgumentException();

		List<Answer> answers = new ArrayList<Answer>(1);
		answers.add(answer);
		submitAnswers(answers, completeAnswer, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswers(final List<Answer> answers, Boolean completeAnswers, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		if (answers == null) throw new IllegalArgumentException();
		if (completeAnswers == null) throw new IllegalArgumentException();
		if (completeSubmission == null) throw new IllegalArgumentException();

		if (answers.size() == 0) return;

		// unless we are going to complete the submission, if there has been no change in the answers, do nothing...
		// unless the answers are to be marked complete and they don't all have a submitted date
		// TODO:
		if (!completeSubmission)
		{
			boolean anyChange = false;
			for (Answer answer : answers)
			{
				if (answer.getIsChanged())
				{
					anyChange = true;
					break;
				}

				if (completeAnswers && (answer.getSubmittedDate() == null))
				{
					anyChange = true;
					break;
				}
			}

			if (!anyChange) return;
		}

		Submission submission = getSubmission(answers.get(0).getSubmission().getId());
		Assessment assessment = submission.getAssessment();

		// make sure this is an incomplete submission must be incomplete
		if (submission.getIsComplete()) throw new SubmissionCompletedException();

		// check that the current user is the submission user
		if (!submission.getUserId().equals(sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) assessmentService).getAssessmentReference(assessment.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// the assessment must be currently open for submission (with the grace period to support completion near closing time)
		if (!assessment.getIsOpen(Boolean.TRUE)) throw new AssessmentClosedException();

		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswer: submission: " + submission.getId() + " complete?: " + Boolean.toString(completeSubmission) + " asOf: " + asOf);

		// update the dates and answer scores
		submission.setSubmittedDate(asOf);
		for (Answer answer : answers)
		{
			// mark a submitted date only if the new answers are complete (and the answer has otherwise been changed OR the answer is incomplete)
			if (completeAnswers)
			{
				if (answer.getIsChanged() || (answer.getSubmittedDate() == null))
				{
					answer.setSubmittedDate(asOf);
				}
			}

			// auto-score
			answer.autoScore();
			
			// clear the changed
			((AnswerImpl) answer).clearIsChanged();
		}

		// complete the submission is requested to
		if (completeSubmission)
		{
			submission.setIsComplete(Boolean.TRUE);

			// check if we should also mark it graded
			if (assessment.getGrading().getAutoRelease())
			{
				submission.setIsReleased(Boolean.TRUE);
			}
		}

		// TODO: save the answers, update the submission
		this.storage.saveSubmission((SubmissionImpl) submission);
		this.storage.saveAnswers(answers);

		// record in gradebook if needed
		if (submission.getIsReleased() && assessment.getGrading().getGradebookIntegration())
		{
			recordInGradebook(submission, true);
		}

		// event track it (one for each answer)
		for (Answer answer : answers)
		{
			if (answer.getIsChanged())
			{
				eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_ANSWER, getSubmissionReference(submission.getId())
						+ ":" + answer.getQuestion().getId(), true));
			}
		}

		// track if we are complete
		if (submission.getIsComplete())
		{
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()),
					true));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateGradebook(Assessment assessment) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("updateGradebook: " + assessment.getId());

		// check permission
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());

		// skip if there is no gradebook integration
		if (!assessment.getGrading().getGradebookIntegration()) return;

		// try each user with a submission
		List<String> userIds = this.storage.getUsersSubmitted(assessment);

		for (String uid : userIds)
		{
			// find the user's highest score among the completed submissions
			Float highestScore = this.storage.getSubmissionHighestScore(assessment, uid);

			// push this into the GB
			try
			{
				gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), uid, ((highestScore == null) ? null
						: new Double(highestScore.doubleValue())));
			}
			catch (GradebookNotFoundException e)
			{
				// if there's no gradebook for this context, oh well...
				M_log.warn("updateGradebook: (no gradebook for context): " + e);
			}
			catch (AssessmentNotFoundException e)
			{
				// if the assessment has not been registered in gb, this is a problem
				M_log.warn("updateGradebook: (assessment has not been registered in context's gb): " + e);
			}
		}
	}

	/**
	 * Mark the submission as auto-complete as of now.
	 * 
	 * @param asOf
	 *        The effective time of the completion.
	 * @param submission
	 *        The submission.
	 * @return true if it was successful, false if not.
	 */
	protected boolean autoCompleteSubmission(Date asOf, Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		if (asOf == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("autoCompleteSubmission: submission: " + submission.getId());

		// update the submission
		submission.setIsComplete(Boolean.TRUE);
		submission.setSubmittedDate(asOf);

		if (submission.getAssessment().getGrading().getAutoRelease())
		{
			submission.setIsReleased(Boolean.TRUE);
		}

		this.storage.saveSubmission((SubmissionImpl) submission);

		// record in the gradebook if so configured, using data only from the submission, no db refresh
		if (submission.getIsReleased() && submission.getAssessment().getGrading().getGradebookIntegration())
		{
			recordInGradebook(submission, false);
		}

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_AUTO_COMPLETE, getSubmissionReference(submission.getId()),
				true));

		return true;
	}

	/**
	 * Check a list of submissions to see if they need to be auto-completed.
	 * 
	 * @param submissions
	 *        The submissions.
	 * @param asOf
	 *        The effective date.
	 */
	protected void checkAutoComplete(List<SubmissionImpl> submissions, Date asOf)
	{
		for (Submission submission : submissions)
		{
			// check if this is over time limit / deadline
			if (submission.getIsOver(asOf, 0))
			{
				// complete this one, using the exact 'over' date for the final date
				Date over = submission.getWhenOver();
				autoCompleteSubmission(over, submission);
			}
		}
	}

	/**
	 * Form a submission reference for this submission id.
	 * 
	 * @param submissionId
	 *        the submission id.
	 * @return the submission reference for this submission id.
	 */
	protected String getSubmissionReference(String submissionId)
	{
		return MnemeService.REFERENCE_ROOT + "/" + MnemeService.SUBMISSION_TYPE + "/" + submissionId;
	}

	/**
	 * Find the submissions that are open, timed, and well expired, or open and past a submit-until date
	 * 
	 * @param grace
	 *        The number of ms past the time limit that the submission's elapsed time must be to qualify
	 * @return A List of the submissions that are open, timed, and well expired.
	 */
	protected List<Submission> getTimedOutSubmissions(final long grace)
	{
		if (M_log.isDebugEnabled()) M_log.debug("getTimedOutSubmissions");

		final Date asOf = new Date();

		// select all open submission for every user assessment context
		List<SubmissionImpl> all = this.storage.getOpenSubmissions();

		// filter out the ones we really want
		List<Submission> rv = new ArrayList<Submission>();
		for (Submission submission : all)
		{
			// see if we want this one
			boolean selected = false;

			// for timed, if the elapsed time since their start is well past the time limit
			if (submission.getAssessment().getTimeLimit() != null)
			{
				if ((submission.getAssessment().getTimeLimit() > 0) && (submission.getSubmittedDate() != null)
						&& ((asOf.getTime() - submission.getSubmittedDate().getTime()) > (submission.getAssessment().getTimeLimit() + grace)))
				{
					selected = true;
				}
			}

			// for past submit-until date
			if (submission.getAssessment().getDates().getSubmitUntilDate() != null)
			{
				if (asOf.getTime() > (submission.getAssessment().getDates().getSubmitUntilDate().getTime() + grace))
				{
					selected = true;
				}
			}

			// TODO: what about unpublished? archived?

			if (selected) rv.add(submission);
		}

		return rv;
	}

	/**
	 * Clump a list of all submissions, which may include many from the same user, into a list of official ones with siblings.
	 * 
	 * @param all
	 *        The list of all submissions.
	 * @return
	 */
	protected List<Submission> officialize(List<SubmissionImpl> all)
	{
		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = new ArrayList<Submission>();

		while (all.size() > 0)
		{
			// take the first one out
			SubmissionImpl submission = all.remove(0);

			// count the submissions actually present in the list for this assessment
			int count = 0;
			if (submission.getStartDate() != null)
			{
				count++;
			}

			String aid = submission.getAssessmentId();
			SubmissionImpl bestSubmission = null;
			SubmissionImpl inProgressSubmission = null;

			// this one may be our best, or in progress, but only if it's started
			if (submission.getStartDate() != null)
			{
				// if incomplete, record this as in progress
				if (!submission.getIsComplete())
				{
					inProgressSubmission = submission;
				}

				// else, if complete, make it the best so far
				else
				{
					bestSubmission = submission;
				}
			}

			// remove all others with this one's assessment id - keeping track of the best score if complete
			for (Iterator i = all.iterator(); i.hasNext();)
			{
				SubmissionImpl candidateSub = (SubmissionImpl) i.next();
				if (candidateSub.getAssessmentId().equals(aid))
				{
					// take this one out
					i.remove();

					// we should not get a second one that is unstarted
					if (candidateSub.getStartDate() == null)
					{
						M_log.warn("getUserContextSubmissions: another unstarted for aid: " + aid + " sid:" + candidateSub.getId());
						continue;
					}

					// count as a sibling
					count++;

					// track the in-progress one, if any
					if ((candidateSub.getIsComplete() == null) || (!candidateSub.getIsComplete()))
					{
						inProgressSubmission = candidateSub;
					}

					// if not in progress, then see if it has the best score so far
					else
					{
						if (bestSubmission == null)
						{
							bestSubmission = candidateSub;
						}

						// take the new one if it exceeds the best so far
						else if (bestSubmission.getTotalScore().floatValue() < candidateSub.getTotalScore().floatValue())
						{
							bestSubmission = candidateSub;
						}

						// if we match the best, pick the latest submit date
						else if (bestSubmission.getTotalScore().floatValue() == candidateSub.getTotalScore().floatValue())
						{
							if ((bestSubmission.getSubmittedDate() != null) && (candidateSub.getSubmittedDate() != null)
									&& (bestSubmission.getSubmittedDate().before(candidateSub.getSubmittedDate())))
							{
								bestSubmission = candidateSub;
							}
						}
					}
				}
			}

			// pick the winner
			SubmissionImpl winner = inProgressSubmission;
			if (winner == null) winner = bestSubmission;
			if (winner == null) winner = submission;

			// set the winner's sibling count
			winner.initSiblingCount(new Integer(count));

			// set the winner's best
			if (bestSubmission != null)
			{
				winner.initBest(bestSubmission);
			}

			// keep the winner
			official.add(winner);
		}

		return official;
	}

	/**
	 * Record this submission in the gradebook.
	 * 
	 * @param submission
	 *        The submission to record in the gradebook.
	 * @param refresh
	 *        if true, get the latest score from the db, if false, use the final score from the submission.
	 */
	protected void recordInGradebook(Submission submission, boolean refresh)
	{
		if (submission == null) throw new IllegalArgumentException();

		Assessment assessment = submission.getAssessment();

		Double points = null;

		// refresh from the database if requested
		if (refresh)
		{
			// read the final score for this submission from the db
			points = this.storage.getSubmissionScore(submission).doubleValue();
		}
		else
		{
			// use the score from the submission record
			points = submission.getTotalScore().doubleValue();
		}

		// find the highest score recorded for this user and this assessment
		Float highestScore = this.storage.getSubmissionHighestScore(assessment, submission.getUserId());
		if ((highestScore != null) && (points.doubleValue() < highestScore.doubleValue()))
		{
			// if this submission's points is not highest, don't record this in GB
			return;
		}

		// post it
		try
		{
			gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), submission.getUserId(), points);
		}
		catch (GradebookNotFoundException e)
		{
			// if there's no gradebook for this context, oh well...
			M_log.warn("recordInGradebook: (no gradebook for context): " + e);
		}
		catch (AssessmentNotFoundException e)
		{
			// if the assessment has not been registered in gb, this is a problem
			M_log.warn("recordInGradebook: (assessment has not been registered in context's gb): " + e);
		}
	}

	/**
	 * Sort a list of submissions by their status.
	 * 
	 * @param sort
	 *        The sort (status_a or status_d)
	 * @param submissions
	 *        The submission list to sort.
	 * @return The sorted list of submissions.
	 */
	protected List<Submission> sortByStatus(boolean descending, List<Submission> submissions)
	{
		// the easy cases
		if ((submissions == null) || (submissions.size() < 2)) return submissions;

		List<Submission> rv = new ArrayList<Submission>();

		// sort order (a) other, future, over, complete, completeReady, ready, inProgress, inProgressAlert
		List<Submission> other = new ArrayList<Submission>();
		List<Submission> future = new ArrayList<Submission>();
		List<Submission> over = new ArrayList<Submission>();
		List<Submission> complete = new ArrayList<Submission>();
		List<Submission> completeReady = new ArrayList<Submission>();
		List<Submission> ready = new ArrayList<Submission>();
		List<Submission> inProgress = new ArrayList<Submission>();
		List<Submission> inProgressAlert = new ArrayList<Submission>();

		for (Submission s : submissions)
		{
			switch (s.getAssessmentSubmissionStatus())
			{
				case other:
				{
					other.add(s);
					break;
				}

				case future:
				{
					future.add(s);
					break;
				}

				case over:
				{
					over.add(s);
					break;
				}

				case complete:
				{
					complete.add(s);
					break;
				}

				case completeReady:
				{
					completeReady.add(s);
					break;
				}

				case ready:
				{
					ready.add(s);
					break;
				}

				case inProgress:
				{
					inProgress.add(s);
					break;
				}

				case inProgressAlert:
				{
					inProgressAlert.add(s);
					break;
				}
			}
		}

		// order ascending
		rv.addAll(other);
		rv.addAll(future);
		rv.addAll(over);
		rv.addAll(complete);
		rv.addAll(completeReady);
		rv.addAll(ready);
		rv.addAll(inProgress);
		rv.addAll(inProgressAlert);

		// reverse if descending
		if (descending)
		{
			Collections.reverse(rv);
		}

		return rv;
	}

	/**
	 * Start the clean and report thread.
	 */
	protected void start()
	{
		threadStop = false;

		checkerThread = new Thread(this, getClass().getName());
		checkerThread.start();
	}

	/**
	 * Stop the clean and report thread.
	 */
	protected void stop()
	{
		if (checkerThread == null) return;

		// signal the thread to stop
		threadStop = true;

		// wake up the thread
		checkerThread.interrupt();

		checkerThread = null;
	}
}
