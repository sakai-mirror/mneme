/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.GradesService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.StringUtil;

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

	/** Dependency: GradesService */
	protected GradesService gradesService = null;

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** The submission id which is the last to get pre 1.0.6 shuffle behavior. If null, all get the new behavior. */
	protected String shuffle106CrossoverId = null;

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

	/** Dependency: UserDirectoryService. */
	protected UserDirectoryService userDirectoryService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();
		Assessment assessment = submission.getAssessment();
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("allowCompleteSubmission: " + submission.getId() + " user: " + userId);

		// user must be this submission's user
		if (!submission.getUserId().equals(userId)) return Boolean.FALSE;

		// submission must be incomplete
		if (submission.getIsComplete()) return Boolean.FALSE;

		// user must have submit permission in the context of the assessment for this submission
		// test drive submissions need manage instead
		if (!submission.getIsTestDrive())
		{
			if (!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext())) return Boolean.FALSE;
		}
		else
		{
			if (!securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, assessment.getContext())) return Boolean.FALSE;
		}

		// the assessment must be currently open for submission (with a grace period to allow completion near closing time)
		// test drive submissions can skip this
		if (!submission.getIsTestDrive())
		{
			if (!assessment.getDates().getIsOpen(Boolean.TRUE)) return Boolean.FALSE;
		}

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(String context)
	{
		if (context == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEvaluate: context: " + context + " user: " + userId);

		// user must have grade permission in the context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.GRADE_PERMISSION, context)) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowEvaluate: " + submission.getId() + " user: " + userId);

		// the submission must be complete
		if (!submission.getIsComplete()) return Boolean.FALSE;

		// user must have grade permission in the context of the assessment for this submission
		if (!securityService.checkSecurity(userId, MnemeService.GRADE_PERMISSION, submission.getAssessment().getContext())) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();
		Assessment assessment = submission.getAssessment();
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("allowReviewSubmission: " + submission.getId() + " user: " + userId);

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
	public Boolean allowSubmit(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("allowSubmit: " + submission.getAssessment().getId() + " user: " + userId);

		// user must have submit permission in the context of the assessment for this submission
		// test drive submissions need manage permission instead
		if (!submission.getIsTestDrive())
		{
			if (!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, submission.getAssessment().getContext()))
				return Boolean.FALSE;
		}
		else
		{
			if (!securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, submission.getAssessment().getContext()))
				return Boolean.FALSE;
		}

		// the assessment must be currently open for submission
		// test drive submissions can skip this
		if (!submission.getIsTestDrive())
		{
			if (!submission.getAssessment().getDates().getIsOpen(Boolean.FALSE)) return Boolean.FALSE;
		}

		// if the user has a submission in progress, this is good
		if (submission.getIsStarted() && (!submission.getIsComplete())) return Boolean.TRUE;

		// if the user can submit a new one, this is good
		Integer remaining = countRemainingSubmissions(submission);
		if ((remaining == null) || (remaining > 0)) return Boolean.TRUE;

		// at this point, let the test-drive start
		if (submission.getIsTestDrive()) return Boolean.TRUE;

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

		if (M_log.isDebugEnabled()) M_log.debug("completeSubmission: submission: " + submission.getId());

		// submission must be incomplete
		if (submission.getIsComplete()) throw new SubmissionCompletedException();

		// the current user must be the submission user
		if (!submission.getUserId().equals(sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) assessmentService).getAssessmentReference(assessment.getId()));
		}

		// user must have SUBMIT_PERMISSION in the context of the assessment
		// or for test-drive, manage permission
		if (!s.getIsTestDrive())
		{
			this.securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());
		}
		else
		{
			this.securityService.secure(submission.getUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());
		}

		// the assessment must be currently open for submission (with the grace period to support completion near closing time)
		// test drive can skip this
		if (!s.getIsTestDrive())
		{
			if (!assessment.getDates().getIsOpen(Boolean.TRUE)) throw new AssessmentClosedException();
		}

		// update the submission
		submission.setSubmittedDate(asOf);
		submission.setIsComplete(Boolean.TRUE);

		// if grade at submission
		if (assessment.getGrading().getAutoRelease())
		{
			submission.setIsReleased(Boolean.TRUE);
		}

		// clear the cache
		String key = cacheKey(submission.getId());
		this.threadLocalManager.set(key, null);

		// store the changes
		((SubmissionImpl) submission).clearIsChanged();
		this.storage.saveSubmission((SubmissionImpl) submission);

		// report the grade (not for test drive)
		if (!submission.getIsTestDrive())
		{
			this.gradesService.reportSubmissionGrade(submission);
		}

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()), true));
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessmentSubmissions(Assessment assessment, Boolean official, String allUid)
	{
		// TODO: review the efficiency of this method! -ggolden

		if (assessment == null) throw new IllegalArgumentException();
		if (official == null) throw new IllegalArgumentException();
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("countAssessmentSubmissions: assessment: " + assessment.getId() + " official: " + official + " allUid: " + allUid);

		// get the submissions to the assignment made by all possible submitters
		List<SubmissionImpl> all = getAssessmentSubmissions(assessment, FindAssessmentSubmissionsSort.status_a, null);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> rv = null;
		if (official)
		{
			rv = officializeByUser(all, allUid);
		}
		else
		{
			rv = new ArrayList<Submission>(all.size());
			rv.addAll(all);
		}

		return rv.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countSubmissionAnswers(Assessment assessment, Question question)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (question == null) throw new IllegalArgumentException();

		// TODO: review the efficiency of this method! -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("countSubmissionAnswers: assessment: " + assessment.getId() + " question: " + question.getId());

		List<Answer> answers = findSubmissionAnswers(assessment, question, FindAssessmentSubmissionsSort.status_a, null, null);

		return answers.size();
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
	public Submission enterSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		if (submission == null) throw new IllegalArgumentException();
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("enterSubmission: assessment: " + submission.getAssessment().getId() + " user: " + submission.getUserId());

		// user must have SUBMIT_PERMISSION in the context of the assessment
		// or if test drive, manage
		if (!submission.getIsTestDrive())
		{
			this.securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, submission.getAssessment().getContext());
		}
		else
		{
			this.securityService.secure(submission.getUserId(), MnemeService.MANAGE_PERMISSION, submission.getAssessment().getContext());
		}

		// the assessment must be currently open for submission
		// test drive can skip this
		if (!submission.getIsTestDrive())
		{
			if (!submission.getAssessment().getDates().getIsOpen(Boolean.FALSE)) throw new AssessmentClosedException();
		}

		// use the one in progress if
		if (submission.getIsStarted() && (!submission.getIsComplete()))
		{
			// event track it (not a modify event)
			this.eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_CONTINUE,
					getSubmissionReference(submission.getId()), false));

			return submission;
		}

		// the user must be able to create a new submission
		// test drive can skip this
		if (!submission.getIsTestDrive())
		{
			Integer remaining = countRemainingSubmissions(submission);
			if ((remaining != null) && (remaining == 0)) throw new AssessmentCompletedException();
		}

		// TODO: it is possible to make too many submissions for the assessment. If this method is entered concurrently for the same user and
		// assessment, the previous count check might fail.

		// go live - not for test drive
		if ((!submission.getIsTestDrive()) && (!submission.getAssessment().getIsLive()))
		{
			((AssessmentServiceImpl) this.assessmentService).makeLive(submission.getAssessment());
		}

		// make a new submission
		SubmissionImpl rv = this.storage.newSubmission();
		rv.initAssessmentId(submission.getAssessment().getId());
		rv.initUserId(submission.getUserId());
		rv.setIsComplete(Boolean.FALSE);
		rv.setStartDate(asOf);
		rv.setSubmittedDate(asOf);

		// if the user does not have submit, mark it as test drive
		if (!securityService.checkSecurity(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, submission.getAssessment().getContext()))
		{
			rv.initTestDrive(Boolean.TRUE);
		}

		// store the new submission, setting the id
		((SubmissionImpl) rv).clearIsChanged();
		this.storage.saveSubmission(rv);

		// populate the questions (need to first have the submission id set for the draws)
		for (Question question : rv.getAssessment().getParts().getQuestions())
		{
			AnswerImpl answer = this.storage.newAnswer();
			answer.initQuestion(question);
			((SubmissionImpl) rv).replaceAnswer(answer);
		}
		((SubmissionImpl) rv).clearIsChanged();
		this.storage.saveAnswers(rv.getAnswers());

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_ENTER, getSubmissionReference(rv.getId()), true));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateAnswers(Collection<Answer> answers) throws AssessmentPermissionException
	{
		if (answers == null) throw new IllegalArgumentException();
		if (answers.isEmpty()) return;

		if (M_log.isDebugEnabled()) M_log.debug("evaluateAnswers");

		Date now = new Date();
		String userId = sessionManager.getCurrentSessionUserId();

		// check that all answers are to the same context
		String context = null;
		for (Answer answer : answers)
		{
			if (context == null)
			{
				context = answer.getSubmission().getAssessment().getContext();
			}
			else if (!context.equals(answer.getSubmission().getAssessment().getContext()))
			{
				throw new IllegalArgumentException();
			}
		}

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, context);

		// set the attribution for each answer
		List<Answer> work = new ArrayList<Answer>(answers);
		Set<Submission> submissions = new HashSet<Submission>();
		for (Iterator i = work.iterator(); i.hasNext();)
		{
			Answer answer = (Answer) i.next();

			if (((EvaluationImpl) answer.getEvaluation()).getIsChanged())
			{
				// set attribution
				answer.getEvaluation().getAttribution().setDate(now);
				answer.getEvaluation().getAttribution().setUserId(userId);

				// clear changed flag
				((EvaluationImpl) answer.getEvaluation()).clearIsChanged();

				// clear the answer's submission from the thread-local cache
				String key = cacheKey(answer.getSubmission().getId());
				this.threadLocalManager.set(key, null);

				submissions.add(answer.getSubmission());
			}
			else
			{
				i.remove();
			}
		}

		// save the answers
		if (!work.isEmpty())
		{
			this.storage.saveAnswersEvaluation(work);

			// TODO: events? single event?
			// eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_GRADE, getSubmissionReference(submission.getId()),
			// true));
		}

		// push each submission modified to the gb
		for (Submission s : submissions)
		{
			if (!s.getIsTestDrive())
			{
				this.gradesService.reportSubmissionGrade(s);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateSubmission(Submission submission) throws AssessmentPermissionException
	{
		Date now = new Date();
		String userId = sessionManager.getCurrentSessionUserId();
		if (submission == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("evaluateSubmission: " + submission.getId());

		// consoliate the total score
		submission.consolidateTotalScore();

		// check for changes
		boolean changed = ((EvaluationImpl) submission.getEvaluation()).getIsChanged();
		if (!changed) changed = ((SubmissionImpl) submission).getIsChanged();
		if (!changed)
		{
			for (Answer answer : submission.getAnswers())
			{
				if (((EvaluationImpl) answer.getEvaluation()).getIsChanged())
				{
					changed = true;
					break;
				}
			}
		}
		if (!changed) return;

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, submission.getAssessment().getContext());

		// set the attribution
		if (((EvaluationImpl) submission.getEvaluation()).getIsChanged())
		{
			submission.getEvaluation().getAttribution().setDate(now);
			submission.getEvaluation().getAttribution().setUserId(userId);
		}

		// if this is phantom, make the submission real
		if (((SubmissionImpl) submission).getIsPhantom())
		{
			// make a new submission
			SubmissionImpl temp = this.storage.newSubmission();
			temp.initAssessmentId(submission.getAssessment().getId());
			temp.initUserId(submission.getUserId());
			temp.setIsComplete(Boolean.TRUE);
			temp.setStartDate(now);
			temp.setSubmittedDate(now);
			temp.evaluation = (SubmissionEvaluationImpl) submission.getEvaluation();

			if (submission.getAssessment().getGrading().getAutoRelease())
			{
				temp.setIsReleased(Boolean.TRUE);
			}

			// if the user does not have submit, but has manage, mark it as test drive
			if ((!securityService.checkSecurity(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, submission.getAssessment().getContext()))
					&& securityService.checkSecurity(submission.getUserId(), MnemeService.MANAGE_PERMISSION, submission.getAssessment().getContext()))
			{
				temp.initTestDrive(Boolean.TRUE);
			}

			// store the new submission, setting the id
			((SubmissionImpl) temp).clearIsChanged();
			this.storage.saveSubmission(temp);

			// preserve the evaluation score as the total score, even after adding in the questions
			Float total = temp.getTotalScore();

			// populate the questions (need to first have the submission id set for the draws)
			for (Question question : temp.getAssessment().getParts().getQuestions())
			{
				AnswerImpl answer = this.storage.newAnswer();
				answer.initQuestion(question);
				((SubmissionImpl) temp).replaceAnswer(answer);
			}
			((SubmissionImpl) temp).clearIsChanged();
			this.storage.saveAnswers(temp.getAnswers());

			temp.setTotalScore(total);
			temp.consolidateTotalScore();
			if (temp.getIsChanged())
			{
				((SubmissionImpl) temp).clearIsChanged();
				this.storage.saveSubmission(temp);
			}

			// push the grade - not for test drive
			if (!temp.getIsTestDrive())
			{
				this.gradesService.reportSubmissionGrade(temp);
			}

			// TODO: which event? event track it
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_ENTER, getSubmissionReference(temp.getId()), true));

			return;
		}

		// attribution for answers - remove any not changed so they are not saved
		List<Answer> work = new ArrayList<Answer>(submission.getAnswers());
		for (Iterator i = work.iterator(); i.hasNext();)
		{
			Answer answer = (Answer) i.next();

			if (((EvaluationImpl) answer.getEvaluation()).getIsChanged())
			{
				// set attribution
				answer.getEvaluation().getAttribution().setDate(now);
				answer.getEvaluation().getAttribution().setUserId(userId);

				// clear the changed flag
				((EvaluationImpl) answer.getEvaluation()).clearIsChanged();
			}

			else
			{
				i.remove();
			}
		}

		// save just evaluation stuff and answers and released
		if (((EvaluationImpl) submission.getEvaluation()).getIsChanged())
		{
			((EvaluationImpl) submission.getEvaluation()).clearIsChanged();
			this.storage.saveSubmissionEvaluation((SubmissionImpl) submission);
		}
		if (!work.isEmpty())
		{
			this.storage.saveAnswersEvaluation(work);
		}

		// save the released setting if changed
		if (((SubmissionImpl) submission).getIsReleasedChanged())
		{
			((SubmissionImpl) submission).clearReleasedIsChanged();
			this.storage.saveSubmissionReleased((SubmissionImpl) submission);
		}

		// clear the cache
		String key = cacheKey(submission.getId());
		this.threadLocalManager.set(key, null);

		// event
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_GRADE, getSubmissionReference(submission.getId()), true));

		// push the grade - not for test drive
		if (!submission.getIsTestDrive())
		{
			this.gradesService.reportSubmissionGrade(submission);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateSubmissions(Assessment assessment, String comment, Float score) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();
		if ((comment == null) && (score == null)) return;
		Date now = new Date();
		String userId = sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("evaluateSubmissions: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());

		// get the completed submissions to this assessment
		List<SubmissionImpl> submissions = this.storage.getAssessmentCompleteSubmissions(assessment);

		// TODO: only for the "official" one ? submissions = officialize(submissions);

		// process all submissions, official or not
		for (SubmissionImpl submission : submissions)
		{
			// if there's a comment to set, append it
			if (comment != null)
			{
				String newComment = submission.evaluation.getComment();
				if (newComment == null)
				{
					newComment = comment;
				}
				else
				{
					newComment += comment;
				}

				submission.evaluation.setComment(newComment);
			}

			// if there's a score to set, add it
			if (score != null)
			{
				float total = score;
				if (submission.evaluation.getScore() != null)
				{
					total += submission.evaluation.getScore();
				}
				submission.evaluation.setScore(total);
			}

			// save the submission evaluation (if changed)
			if (((EvaluationImpl) submission.getEvaluation()).getIsChanged())
			{
				// set the attribution
				submission.evaluation.getAttribution().setDate(now);
				submission.evaluation.getAttribution().setUserId(userId);

				// clear the changed flag
				((EvaluationImpl) submission.getEvaluation()).clearIsChanged();

				// clear the cache
				String key = cacheKey(submission.getId());
				this.threadLocalManager.set(key, null);

				// save
				this.storage.saveSubmissionEvaluation(submission);
			}

			// TODO: event? s?
		}

		// release the grades to the grading authority
		this.gradesService.reportAssessmentGrades(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort, Boolean official, String allUid,
			Integer pageNum, Integer pageSize)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (official == null) throw new IllegalArgumentException();
		if (sort == null) sort = FindAssessmentSubmissionsSort.userName_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("findAssessmentSubmissions: assessment: " + assessment.getId() + " sort: " + sort + " official: " + official + " allUid: "
					+ allUid);

		// get the submissions to the assignment made by all possible submitters
		List<SubmissionImpl> all = getAssessmentSubmissions(assessment, sort, null);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> rv = null;
		if (official)
		{
			rv = officializeByUser(all, allUid);
		}
		else
		{
			rv = new ArrayList<Submission>(all.size());
			rv.addAll(all);
		}

		// if sorting by status, do that sort
		if (sort == FindAssessmentSubmissionsSort.status_a || sort == FindAssessmentSubmissionsSort.status_d)
		{
			rv = sortByGradingSubmissionStatus((sort == FindAssessmentSubmissionsSort.status_d), rv);
		}

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
	public List<Question> findPartQuestions(Part part)
	{
		if (M_log.isDebugEnabled()) M_log.debug("findPartQuestions: " + part.getId());

		List<String> qids = this.storage.findPartQuestions(part);
		List<Question> rv = new ArrayList<Question>();

		for (String qid : qids)
		{
			QuestionImpl q = (QuestionImpl) this.questionService.getQuestion(qid);
			if (q != null)
			{
				q.initPartContext(part);
				rv.add(q);
			}
		}

		// sort by question text
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				String s0 = StringUtil.trimToZero(((QuestionImpl) arg0).getDescription());
				String s1 = StringUtil.trimToZero(((QuestionImpl) arg1).getDescription());
				int rv = s0.compareToIgnoreCase(s1);
				return rv;
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] findPrevNextSubmissionIds(Submission submission, FindAssessmentSubmissionsSort sort, Boolean official)
	{
		// TODO: can we do this cheaper?
		if (submission == null) throw new IllegalArgumentException();
		if (official == null) throw new IllegalArgumentException();
		if (sort == null) sort = FindAssessmentSubmissionsSort.userName_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("findNextPrevSubmissionIds: submission: " + submission.getId() + " sort: " + sort + " official: " + official);

		// get the submissions to the assignment made by all possible submitters
		// TODO: we don't really need them all... no phantoms!
		List<SubmissionImpl> all = getAssessmentSubmissions(submission.getAssessment(), sort, null);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> working = null;
		if (official)
		{
			working = officializeByUser(all, null);
		}
		else
		{
			working = new ArrayList<Submission>(all.size());
			working.addAll(all);
		}

		// if sorting by status, do that sort
		if (sort == FindAssessmentSubmissionsSort.status_a || sort == FindAssessmentSubmissionsSort.status_d)
		{
			working = sortByGradingSubmissionStatus((sort == FindAssessmentSubmissionsSort.status_d), working);
		}

		// find our submission by id
		Submission prev = null;
		Submission next = null;
		boolean done = false;
		for (Submission s : working)
		{
			// TODO: we should not have to filter these out...
			if (((SubmissionImpl) s).getIsPhantom()) continue;

			if (done)
			{
				next = s;
				break;
			}

			if (s.getId().equals(submission.getId()))
			{
				done = true;
			}
			else
			{
				prev = s;
			}
		}

		// if we didn't find it by id, find it by user id
		if (!done)
		{
			next = null;
			prev = null;
			for (Submission s : working)
			{
				// TODO: we should not have to filter these out...
				if (((SubmissionImpl) s).getIsPhantom()) continue;

				if (done)
				{
					next = s;
					break;
				}

				if (s.getUserId().equals(submission.getUserId()))
				{
					done = true;
				}
				else
				{
					prev = s;
				}
			}

		}

		String[] rv = new String[2];
		if (!done)
		{
			rv[0] = null;
			rv[1] = null;
		}
		else
		{
			rv[0] = ((prev == null) ? null : prev.getId());
			rv[1] = ((next == null) ? null : next.getId());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Answer> findSubmissionAnswers(Assessment assessment, Question question, FindAssessmentSubmissionsSort sort, Integer pageNum,
			Integer pageSize)
	{
		// TODO: review the efficiency of this method! -ggolden
		// TODO: consider removing the official (set to false, getting all) to improve efficiency -ggolden

		if (assessment == null) throw new IllegalArgumentException();
		if (question == null) throw new IllegalArgumentException();
		if (sort == null) sort = FindAssessmentSubmissionsSort.userName_a;
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("findAssessmentSubmissions: assessment: " + assessment.getId() + " question: " + question.getId() + " sort: " + sort);

		// read all the submissions for this assessment from all possible submitters
		List<SubmissionImpl> all = getAssessmentSubmissions(assessment, sort, question);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one (if official)
		List<Submission> rv = new ArrayList<Submission>(all.size());
		rv.addAll(all);

		// if sorting by status, do that sort
		if (sort == FindAssessmentSubmissionsSort.status_a || sort == FindAssessmentSubmissionsSort.status_d)
		{
			rv = sortByGradingSubmissionStatus((sort == FindAssessmentSubmissionsSort.status_d), rv);
		}

		// pull out the one answer we want from the completed submissions
		List<Answer> answers = new ArrayList<Answer>();
		for (Submission s : rv)
		{
			if (s.getIsComplete())
			{
				Answer a = s.getAnswer(question);
				if ((a != null) && (a.getIsAnswered()))
				{
					answers.add(a);
				}
			}
		}

		// page the results
		if ((pageNum != null) && (pageSize != null))
		{
			// start at ((pageNum-1)*pageSize)
			int start = ((pageNum - 1) * pageSize);
			if (start < 0) start = 0;
			if (start > answers.size()) start = answers.size() - 1;

			// end at ((pageNum)*pageSize)-1, or max-1, (note: subList is not inclusive for the end position)
			int end = ((pageNum) * pageSize);
			if (end < 0) end = 0;
			if (end > answers.size()) end = answers.size();

			answers = answers.subList(start, end);
		}

		return answers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Answer getAnswer(String answerId)
	{
		if (answerId == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getAnswer:" + answerId);

		return this.storage.getAnswer(answerId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentHasUnscoredSubmissions(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getAssessmentHasUnscoredSubmissions:" + assessment.getId());

		return this.storage.getAssessmentHasUnscoredSubmissions(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Float> getAssessmentHighestScores(Assessment assessment, Boolean releasedOnly)
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getAssessmentHighestScores:" + assessment.getId());

		// get all possible users who can submit
		Set<String> userIds = this.securityService.getUsersIsAllowed(MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		Map<String, Float> rv = this.storage.getAssessmentHighestScores(assessment, releasedOnly);

		// add anyone missing
		for (String userId : userIds)
		{
			if (rv.get(userId) == null)
			{
				rv.put(userId, null);
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled())
			M_log.debug("getAssessmentQuestionHasUnscoredSubmissions:" + assessment.getId() + " question: " + question.getId());

		return this.storage.getAssessmentQuestionHasUnscoredSubmissions(assessment, question);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getAssessmentScores:" + assessment.getId());

		List<Float> rv = this.storage.getAssessmentScores(assessment);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getNewUserAssessmentSubmission(Assessment assessment, String userId)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("getNewUserAssessmentSubmission: assessment: " + assessment.getId() + " userId: " + userId);

		// read all the submissions for this user to this assessment, with all the assessment and submission data we need
		// each assessment is covered with at least one - if there are no submission yet for an assessment, an empty submission is returned
		List<SubmissionImpl> all = getUserAssessmentSubmissions(assessment, userId);

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = officializeByAssessment(all);

		if (official.size() == 1)
		{
			// if we end up with an unstarted or in-progress one, return that
			Submission rv = official.get(0);
			if (!rv.getIsComplete()) return rv;

			// otherwise we need a new one
			int count = rv.getSiblingCount();
			rv = getPhantomSubmission(userId, assessment);
			((SubmissionImpl) rv).initSiblingCount(count);
			return rv;
		}

		// TODO: we don't really want to return null
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		if (question == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("getQuestionScores: " + question.getId());

		final List<Float> rv = this.storage.getQuestionScores(question);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(String id)
	{
		if (id == null) throw new IllegalArgumentException();

		// for thread-local caching
		String key = cacheKey(id);
		SubmissionImpl rv = (SubmissionImpl) this.threadLocalManager.get(key);
		if (rv != null)
		{
			// return a copy
			return this.storage.clone(rv);
		}

		// recognize phantom ids
		if (id.startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// split out the phantom id parts: [1] the aid, [2] the uid
			String[] idParts = StringUtil.split(id, "/");
			String aid = idParts[1];
			String userId = idParts[2];

			// create a phantom - note, we will not mark test drive (no context handy) -ggolden
			rv = this.storage.newSubmission();
			rv.initUserId(userId);
			rv.initAssessmentId(aid);
			rv.initId(id);
			// rv = this.getPhantomSubmission(userId, aid);

			return rv;
		}

		if (M_log.isDebugEnabled()) M_log.debug("getSubmission: " + id);

		rv = this.storage.getSubmission(id);

		// thread-local cache (a copy)
		if (rv != null) this.threadLocalManager.set(key, this.storage.clone(rv));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getSubmissionOfficialScore(Assessment assessment, String userId)
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (userId == null) throw new IllegalArgumentException();

		// highest is official
		return this.storage.getSubmissionHighestScore(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sortParam)
	{
		if (context == null) throw new IllegalArgumentException();
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();
		if (sortParam == null) sortParam = GetUserContextSubmissionsSort.title_a;
		final GetUserContextSubmissionsSort sort = sortParam;
		Date asOf = new Date();

		if (M_log.isDebugEnabled()) M_log.debug("getUserContextSubmissions: context: " + context + " userId: " + userId + ": " + sort);

		// if we are in a test drive situation, use unpublished as well
		Boolean publishedOnly = Boolean.TRUE;
		if (securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, context)
				&& (!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, context)))
		{
			publishedOnly = Boolean.FALSE;
		}

		// read all the submissions for this user in the context
		List<SubmissionImpl> all = this.storage.getUserContextSubmissions(context, userId, publishedOnly);

		// filter out invalid assessments
		for (Iterator i = all.iterator(); i.hasNext();)
		{
			Submission s = (Submission) i.next();
			if (!s.getAssessment().getIsValid())
			{
				i.remove();
			}
		}

		// get all the assessments for this context
		List<Assessment> assessments = this.assessmentService
				.getContextAssessments(context, AssessmentService.AssessmentsSort.title_a, publishedOnly);

		// if any valid assessment is not represented in the submissions we found, add an empty submission for it
		for (Assessment a : assessments)
		{
			if (!a.getIsValid()) continue;

			boolean found = false;
			for (Submission s : all)
			{
				if (s.getAssessment().equals(a))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				SubmissionImpl s = this.getPhantomSubmission(userId, a);
				all.add(s);
			}
		}

		// sort
		// status sorts first by due date descending, then status final sorting is done in the service
		Collections.sort(all, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				switch (sort)
				{
					case title_a:
					{
						String s0 = StringUtil.trimToZero(((Submission) arg0).getAssessment().getTitle());
						String s1 = StringUtil.trimToZero(((Submission) arg1).getAssessment().getTitle());
						rv = s0.compareToIgnoreCase(s1);
						break;
					}
					case title_d:
					{
						String s0 = StringUtil.trimToZero(((Submission) arg0).getAssessment().getTitle());
						String s1 = StringUtil.trimToZero(((Submission) arg1).getAssessment().getTitle());
						rv = -1 * s0.compareToIgnoreCase(s1);
						break;
					}
					case type_a:
					{
						rv = ((Submission) arg0).getAssessment().getType().getSortValue().compareTo(
								((Submission) arg1).getAssessment().getType().getSortValue());
						break;
					}
					case type_d:
					{
						rv = -1
								* ((Submission) arg0).getAssessment().getType().getSortValue().compareTo(
										((Submission) arg1).getAssessment().getType().getSortValue());
						break;
					}
					case dueDate_a:
					{
						// no due date sorts high
						if (((Submission) arg0).getAssessment().getDates().getDueDate() == null)
						{
							if (((Submission) arg1).getAssessment().getDates().getDueDate() == null)
							{
								rv = 0;
								break;
							}
							rv = 1;
							break;
						}
						if (((Submission) arg1).getAssessment().getDates().getDueDate() == null)
						{
							rv = -1;
							break;
						}
						rv = ((Submission) arg0).getAssessment().getDates().getDueDate().compareTo(
								((Submission) arg1).getAssessment().getDates().getDueDate());
						break;
					}
					case dueDate_d:
					case status_a:
					case status_d:
					{
						// no due date sorts high
						if (((Submission) arg0).getAssessment().getDates().getDueDate() == null)
						{
							if (((Submission) arg1).getAssessment().getDates().getDueDate() == null)
							{
								rv = 0;
								break;
							}
							rv = -1;
							break;
						}
						if (((Submission) arg1).getAssessment().getDates().getDueDate() == null)
						{
							rv = 1;
							break;
						}
						rv = -1
								* ((Submission) arg0).getAssessment().getDates().getDueDate().compareTo(
										((Submission) arg1).getAssessment().getDates().getDueDate());
						break;
					}
				}

				return rv;
			}
		});

		// see if any needs to be completed based on time limit or dates
		checkAutoComplete(all, asOf);

		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = officializeByAssessment(all);

		// // if sorting by due date, fix it so null due dates are LARGE not SMALL
		// if (sort == GetUserContextSubmissionsSort.dueDate_a || sort == GetUserContextSubmissionsSort.dueDate_d
		// || sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		// {
		// // pull out the null date entries
		// List<Submission> nulls = new ArrayList<Submission>();
		// for (Iterator i = official.iterator(); i.hasNext();)
		// {
		// Submission s = (Submission) i.next();
		// if (s.getAssessment().getDates().getDueDate() == null)
		// {
		// nulls.add(s);
		// i.remove();
		// }
		// }
		//
		// // for ascending, treat the null dates as LARGE so put them at the end
		// if ((sort == GetUserContextSubmissionsSort.dueDate_a) || (sort == GetUserContextSubmissionsSort.status_d))
		// {
		// official.addAll(nulls);
		// }
		//
		// // for descending, (all status is first sorted date descending) treat the null dates as LARGE so put them at the beginning
		// else
		// {
		// nulls.addAll(official);
		// official.clear();
		// official.addAll(nulls);
		// }
		// }

		// if sorting by status, do that sort
		if (sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		{
			official = sortByAssessmentSubmissionStatus((sort == GetUserContextSubmissionsSort.status_d), official);
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

			storage.init();

			// start the checking thread
			if (timeoutCheckMs > 0)
			{
				start();
			}

			M_log.info("init(): timout check seconds: " + timeoutCheckMs / 1000 + " storage: " + this.storage);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void releaseSubmissions(Assessment assessment, Boolean evaluatedOnly) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();
		if (evaluatedOnly == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("releaseSubmissions: assessment: " + assessment.getId() + " evaluatedOnly: " + evaluatedOnly);

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());

		// get the completed submissions to this assessment
		List<SubmissionImpl> submissions = this.storage.getAssessmentCompleteSubmissions(assessment);

		// TODO: only for the "official" one ? submissions = officialize(submissions);

		// release the all, official or not
		for (SubmissionImpl submission : submissions)
		{
			if ((evaluatedOnly) && !submission.evaluation.getEvaluated()) continue;

			if (submission.getIsReleased()) continue;

			// set as released
			submission.setIsReleased(Boolean.TRUE);

			// clear the changed flag
			((SubmissionImpl) submission).clearReleasedIsChanged();

			// clear the cache
			String key = cacheKey(submission.getId());
			this.threadLocalManager.set(key, null);

			// save release info
			this.storage.saveSubmissionReleased(submission);

			// push the grade
			if (!submission.getIsTestDrive())
			{
				this.gradesService.reportSubmissionGrade(submission);
			}

			// TODO: event? s?
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void retractSubmissions(Assessment assessment) throws AssessmentPermissionException
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("retractSubmissions: assessment: " + assessment.getId());

		// security check
		securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());

		// get the completed submissions to this assessment
		List<SubmissionImpl> submissions = this.storage.getAssessmentCompleteSubmissions(assessment);

		// TODO: only for the "official" one ? submissions = officialize(submissions);

		// retract them all, offical or not
		for (SubmissionImpl submission : submissions)
		{
			if (!submission.getIsReleased()) continue;

			// set as not released
			submission.setIsReleased(Boolean.FALSE);

			// clear the changed flag
			((SubmissionImpl) submission).clearReleasedIsChanged();

			// clear the cache
			String key = cacheKey(submission.getId());
			this.threadLocalManager.set(key, null);

			// save the released info
			this.storage.saveSubmissionReleased(submission);

			// pull the grade
			if (!submission.getIsTestDrive())
			{
				this.gradesService.retractSubmissionGrade(submission);
			}

			// TODO: event? s?
		}
	}

	/**
	 * Run the expiration checking thread.
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
				if (M_log.isDebugEnabled()) M_log.debug("run: running");

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
	 * Set the submission id which is the last to get pre 1.0.6 shuffle behavior. If missing, all get the new behavior.
	 * 
	 * @param id
	 *        The submission id.
	 */
	public void set106ShuffleCrossoverId(String id)
	{
		this.shuffle106CrossoverId = id;
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
	 * Dependency: GradesService.
	 * 
	 * @param service
	 *        The GradesService.
	 */
	public void setGradesService(GradesService service)
	{
		this.gradesService = service;
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
	public void setUserDirectoryService(UserDirectoryService service)
	{
		this.userDirectoryService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean submissionsExist(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		if (M_log.isDebugEnabled()) M_log.debug("submissionsExist: assessment: " + assessment.getId());

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

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswer: answer: " + answer.getId() + " completeAnswer: " + completeAnswer + " completeSubmission: "
					+ completeSubmission);

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

		// TODO: Assume these are all to the same submission... test this?
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

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswers: submission: " + submission.getId() + " completeAnswer: " + completeAnswers + " completeSubmission: "
					+ completeSubmission);

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		// or for test-drive, MANAGE
		if (!submission.getIsTestDrive())
		{
			this.securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());
		}
		else
		{
			this.securityService.secure(submission.getUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());
		}

		// the assessment must be currently open for submission (with the grace period to support completion near closing time)
		// test drive can skip this
		if (!submission.getIsTestDrive())
		{
			if (!assessment.getDates().getIsOpen(Boolean.TRUE)) throw new AssessmentClosedException();
		}

		Date asOf = new Date();

		// update the dates and answer scores
		submission.setSubmittedDate(asOf);
		List<Answer> work = new ArrayList<Answer>(answers);
		for (Iterator i = work.iterator(); i.hasNext();)
		{
			Answer answer = (Answer) i.next();

			// mark a submitted date only if the new answers are complete (and the answer has otherwise been changed OR the answer is incomplete)
			if (completeAnswers)
			{
				if (answer.getIsChanged() || (answer.getSubmittedDate() == null))
				{
					answer.setSubmittedDate(asOf);
					((AnswerImpl) answer).clearIsChanged();
				}
				else
				{
					// remove unchanged answers from further processing
					i.remove();
				}
			}
			else
			{
				if (!answer.getIsChanged())
				{
					i.remove();
				}
				else
				{
					((AnswerImpl) answer).clearIsChanged();
				}
			}
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

		// clear the cache
		String key = cacheKey(submission.getId());
		this.threadLocalManager.set(key, null);

		// save the answers, update the submission
		((SubmissionImpl) submission).clearIsChanged();
		this.storage.saveSubmission((SubmissionImpl) submission);
		this.storage.saveAnswers(work);

		// event track it (one for each answer)
		for (Answer answer : work)
		{
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_ANSWER, getSubmissionReference(submission.getId()) + ":"
					+ answer.getQuestion().getId(), true));
		}

		// if complete
		if (submission.getIsComplete())
		{
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()),
					true));

			// push the grade
			if (!submission.getIsTestDrive())
			{
				this.gradesService.reportSubmissionGrade(submission);
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

		// clear the cache
		String key = cacheKey(submission.getId());
		this.threadLocalManager.set(key, null);

		((SubmissionImpl) submission).clearIsChanged();
		this.storage.saveSubmission((SubmissionImpl) submission);

		// push the grade
		if (!submission.getIsTestDrive())
		{
			this.gradesService.reportSubmissionGrade(submission);
		}

		// event track it
		eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_AUTO_COMPLETE, getSubmissionReference(submission.getId()),
				true));

		return true;
	}

	/**
	 * Form a key for caching a submission.
	 * 
	 * @param submissionId
	 *        The submission id.
	 * @return The cache key.
	 */
	protected String cacheKey(String submissionId)
	{
		String key = "mneme:submission:" + submissionId;
		return key;
	}

	/**
	 * Check if the candidate has a better score than the best so far.
	 * 
	 * @param bestSubmission
	 *        The best so far.
	 * @param candidateSub
	 *        The candidate.
	 * @return true if the candidate is better, false if not.
	 */
	protected boolean candidateBetter(SubmissionImpl bestSubmission, SubmissionImpl candidateSub)
	{
		Float best = bestSubmission.getTotalScore();
		Float candidate = candidateSub.getTotalScore();
		if ((best == null) && (candidate == null)) return false;
		if (candidate == null) return false;
		if (best == null) return true;
		if (best.floatValue() < candidate.floatValue()) return true;
		return false;
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
	 * Check how many additional submissions are allowed to this assessment by this user.<br />
	 * If the user has no permission to submit, has submitted the maximum, or the assessment is closed for submissions as of this time, return 0.
	 * 
	 * @param submission
	 *        The submission.
	 * @return The count of remaining submissions allowed for this user to this assessment, or null if submissions are unlimited.
	 */
	protected Integer countRemainingSubmissions(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();
		Date asOf = new Date();

		if (M_log.isDebugEnabled())
			M_log.debug("countRemainingSubmissions: assessment: " + submission.getAssessment().getId() + " userId: " + submission.getUserId()
					+ " asOf: " + asOf);

		// check the assessment's max submissions
		Integer allowed = submission.getAssessment().getTries();

		// if unlimited, send back a null to indicate this.
		if (allowed == null) return null;

		// get a count of completed submissions by this user to the assessment
		int completed = submission.getSiblingCount();
		if (submission.getIsStarted() && (!submission.getIsComplete())) completed--;

		// how many tries left?
		int remaining = allowed - completed;
		if (remaining < 0) remaining = 0;

		return Integer.valueOf(remaining);
	}

	/**
	 * Access the submission id which is the last to get pre 1.0.6 shuffle behavior. If null, all get the new behavior.
	 * 
	 * @return id The submission id.
	 */
	protected String get106ShuffleCrossoverId()
	{
		return this.shuffle106CrossoverId;
	}

	/**
	 * Get the submissions to the assignment made by all users.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param sort
	 *        The sort.
	 * @param question
	 *        An optional question, to use for sort-by-score (the score would be for this question in the submission, not the overall).
	 * @return A List<Submission> of the submissions for the assessment.
	 */
	protected List<SubmissionImpl> getAssessmentSubmissions(Assessment assessment, final FindAssessmentSubmissionsSort sort, final Question question)
	{
		// collect the submissions to this assessment
		List<SubmissionImpl> rv = this.storage.getAssessmentSubmissions(assessment);

		// get all possible users who can submit
		Set<String> userIds = this.securityService.getUsersIsAllowed(MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// if any user is not represented in the submissions we found, add an empty submission
		for (String userId : userIds)
		{
			boolean found = false;
			for (Submission s : rv)
			{
				if (s.getUserId().equals(userId))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				SubmissionImpl s = this.getPhantomSubmission(userId, assessment);
				rv.add(s);
			}
		}

		// sort - secondary sort of user name, or if primary is title, on submit date
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				FindAssessmentSubmissionsSort secondary = null;
				switch (sort)
				{
					case userName_a:
					case userName_d:
					case status_a:
					case status_d:
					{
						String id0 = ((Submission) arg0).getUserId();
						try
						{
							User u = userDirectoryService.getUser(id0);
							id0 = u.getSortName();
						}
						catch (UserNotDefinedException e)
						{
						}

						String id1 = ((Submission) arg1).getUserId();
						try
						{
							User u = userDirectoryService.getUser(id1);
							id1 = u.getSortName();
						}
						catch (UserNotDefinedException e)
						{
						}

						rv = id0.compareToIgnoreCase(id1);
						secondary = FindAssessmentSubmissionsSort.sdate_a;
						break;
					}
					case final_a:
					case final_d:
					{
						Float final0 = null;
						Float final1 = null;
						if (question != null)
						{
							Answer a0 = ((Submission) arg0).getAnswer(question);
							Answer a1 = ((Submission) arg1).getAnswer(question);
							final0 = ((a0 == null) ? Float.valueOf(0f) : a0.getTotalScore());
							final1 = ((a1 == null) ? Float.valueOf(0f) : a1.getTotalScore());
						}
						else
						{
							final0 = ((Submission) arg0).getTotalScore();
							final1 = ((Submission) arg1).getTotalScore();
						}

						// null sorts small
						if ((final0 == null) && (final1 == null))
						{
							rv = 0;
						}
						else if (final0 == null)
						{
							rv = -1;
						}
						else if (final1 == null)
						{
							rv = 1;
						}
						else
						{
							rv = final0.compareTo(final1);
						}
						secondary = FindAssessmentSubmissionsSort.userName_a;
						break;
					}
					case sdate_a:
					case sdate_d:
					{
						Date date0 = ((Submission) arg0).getSubmittedDate();
						Date date1 = ((Submission) arg1).getSubmittedDate();
						if ((date0 == null) && (date1 == null))
						{
							rv = 0;
						}
						else if (date0 == null)
						{
							rv = -1;
						}
						else if (date1 == null)
						{
							rv = 1;
						}
						else
						{
							rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
						}

						secondary = null;
						break;
					}
				}

				// secondary sort
				FindAssessmentSubmissionsSort third = null;
				if ((rv == 0) && (secondary != null))
				{
					switch (secondary)
					{
						case userName_a:
						case userName_d:
						{
							String id0 = ((Submission) arg0).getUserId();
							try
							{
								User u = userDirectoryService.getUser(id0);
								id0 = u.getSortName();
							}
							catch (UserNotDefinedException e)
							{
							}

							String id1 = ((Submission) arg1).getUserId();
							try
							{
								User u = userDirectoryService.getUser(id1);
								id1 = u.getSortName();
							}
							catch (UserNotDefinedException e)
							{
							}

							rv = id0.compareToIgnoreCase(id1);
							third = FindAssessmentSubmissionsSort.sdate_a;
							break;
						}

						case sdate_a:
						case sdate_d:
						{
							Date date0 = ((Submission) arg0).getSubmittedDate();
							Date date1 = ((Submission) arg1).getSubmittedDate();
							if ((date0 == null) && (date1 == null))
							{
								rv = 0;
							}
							else if (date0 == null)
							{
								rv = -1;
							}
							else if (date1 == null)
							{
								rv = 1;
							}
							else
							{
								rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
							}
							break;
						}
					}
				}

				// third sort
				if ((rv == 0) && (third != null))
				{
					switch (third)
					{
						case sdate_a:
						case sdate_d:
						{
							Date date0 = ((Submission) arg0).getSubmittedDate();
							Date date1 = ((Submission) arg1).getSubmittedDate();
							if ((date0 == null) && (date1 == null))
							{
								rv = 0;
							}
							else if (date0 == null)
							{
								rv = -1;
							}
							else if (date1 == null)
							{
								rv = 1;
							}
							else
							{
								rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
							}
							break;
						}
					}
				}

				return rv;
			}
		});

		// reverse for descending (except for status)
		switch (sort)
		{
			case final_d:
			case userName_d:
			case sdate_d:
			{
				Collections.reverse(rv);
			}
		}

		return rv;
	}

	/**
	 * Create a phantom submission for this user and this assessment.
	 * 
	 * @param userId
	 *        The user id.
	 * @param assessment
	 *        The assessment.
	 * @return A phantom submission for this user and this assessment.
	 */
	protected SubmissionImpl getPhantomSubmission(String userId, Assessment assessment)
	{
		SubmissionImpl s = this.storage.newSubmission();
		s.initUserId(userId);
		s.initAssessmentId(assessment.getId());

		// if the user does not have submit, mark it as test drive
		if ((!securityService.checkSecurity(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext()))
				&& securityService.checkSecurity(userId, MnemeService.MANAGE_PERMISSION, assessment.getContext()))
		{
			s.initTestDrive(Boolean.TRUE);
		}

		// set the id so we know it is a phantom
		s.initId(SubmissionService.PHANTOM_PREFIX + assessment.getId() + "/" + userId);

		return s;
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
		// TODO: tune this so more is done in the db, fewer are read -ggolden
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
	 * Get the user's submissions to the assessment. If there are none, create a phantom.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return a List of the submissions.
	 */
	protected List<SubmissionImpl> getUserAssessmentSubmissions(Assessment assessment, String userId)
	{
		List<SubmissionImpl> rv = this.storage.getUserAssessmentSubmissions(assessment, userId);

		// if we didn't get one, invent one
		if (rv.isEmpty())
		{
			SubmissionImpl s = this.getPhantomSubmission(userId, assessment);
			rv.add(s);
		}

		return rv;
	}

	/**
	 * Clump a list of all submissions from a user in a context, which may include many to the same assessment, into a list of official ones, with
	 * siblings.<br />
	 * Clumping is by assessment.
	 * 
	 * @param all
	 *        The list of all submissions.
	 * @return The official submissions, with siblings for the others.
	 */
	protected List<Submission> officializeByAssessment(List<SubmissionImpl> all)
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
						M_log.warn("officializeByAssessment: another unstarted for aid: " + aid + " sid:" + candidateSub.getId());
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
						else if (candidateBetter(bestSubmission, candidateSub))
						// else if (bestSubmission.getTotalScore().floatValue() < candidateSub.getTotalScore().floatValue())
						{
							bestSubmission = candidateSub;
						}

						// if we match the best, pick the latest submit date
						else if (sameScores(bestSubmission, candidateSub))
						// else if (bestSubmission.getTotalScore().floatValue() == candidateSub.getTotalScore().floatValue())
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
	 * Clump a list of all submissions to an assessment, which may include many from the same user, into a list of official ones, with siblings.<br />
	 * Clumping is by user.
	 * 
	 * @param all
	 *        The list of all submissions.
	 * @param allUid
	 *        if set, leave this user's submissions all in there.
	 * @return The official submissions, with siblings for the others.
	 */
	protected List<Submission> officializeByUser(List<SubmissionImpl> all, String allUid)
	{
		// pick one for each user - the one in progress, or the official complete one
		// List<Submission> official = new ArrayList<Submission>();

		// in all's order
		List<Submission> allOrder = new ArrayList<Submission>(all);

		while (all.size() > 0)
		{
			// take the first one out
			SubmissionImpl submission = all.remove(0);

			// count the submissions actually present in the list for this user
			int count = 0;
			if (submission.getIsStarted())
			{
				count++;
			}

			String uid = submission.getUserId();
			SubmissionImpl bestSubmission = null;
			SubmissionImpl inProgressSubmission = null;

			// keep it if it belongs to allUid
			// if (uid.equals(allUid)) official.add(submission);

			// this one may be our best, or in progress, but only if it's started
			if (submission.getIsStarted())
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

			// remove all others with this one's user id - keeping track of the best score if complete
			List<Submission> loosers = new ArrayList<Submission>();
			for (Iterator i = all.iterator(); i.hasNext();)
			{
				SubmissionImpl candidateSub = (SubmissionImpl) i.next();
				if (candidateSub.getUserId().equals(uid))
				{
					// take this one out
					i.remove();

					// keep it if it belongs to allUid
					// if (candidateSub.getUserId().equals(allUid)) official.add(candidateSub);

					// we should not get a second one that is unstarted
					if (!candidateSub.getIsStarted())
					{
						M_log.warn("officializeByUser: another unstarted for uid: " + uid + " sid:" + candidateSub.getId());
						continue;
					}

					// count as a sibling
					count++;

					// track the in-progress one, if any
					if (!candidateSub.getIsComplete())
					{
						if (inProgressSubmission != null)
						{
							M_log.warn("officializeByUser: another inprogress for uid: " + uid + " sid:" + candidateSub.getId());
						}
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
						else if (candidateBetter(bestSubmission, candidateSub))
						// else if (bestSubmission.getTotalScore().floatValue() < candidateSub.getTotalScore().floatValue())
						{
							loosers.add(bestSubmission);
							bestSubmission = candidateSub;
						}

						// if we match the best, pick the latest submit date
						else if (sameScores(bestSubmission, candidateSub))
						// else if (bestSubmission.getTotalScore().floatValue() == candidateSub.getTotalScore().floatValue())
						{
							if ((bestSubmission.getSubmittedDate() != null) && (candidateSub.getSubmittedDate() != null)
									&& (bestSubmission.getSubmittedDate().before(candidateSub.getSubmittedDate())))
							{
								loosers.add(bestSubmission);
								bestSubmission = candidateSub;
							}
						}
					}

					if ((bestSubmission != candidateSub) && (inProgressSubmission != candidateSub))
					{
						loosers.add(candidateSub);
					}
				}
			}

			// pick the winner
			SubmissionImpl winner = inProgressSubmission;
			if (winner == null) winner = bestSubmission;
			if (winner == null) winner = submission;

			// did our best become a looser?
			if ((bestSubmission != null) && (winner != bestSubmission))
			{
				loosers.add(bestSubmission);
			}

			// set the winner's sibling count
			winner.initSiblingCount(new Integer(count));

			// set the winner's best
			if (bestSubmission != null)
			{
				winner.initBest(bestSubmission);
			}

			// // keep the winner - unless we already did
			// if (!winner.getUserId().equals(allUid))
			// {
			// official.add(winner);
			// }

			// mark the allUid's loosers
			if (uid.equals(allUid))
			{
				for (Submission looser : loosers)
				{
					if (bestSubmission != null)
					{
						((SubmissionImpl) looser).initBest(bestSubmission);
						((SubmissionImpl) looser).initSiblingCount(new Integer(count));
					}
				}
			}

			// remove the loosers from the allOrder (except allUid)
			for (Submission looser : loosers)
			{
				if (!looser.getUserId().equals(allUid))
				{
					allOrder.remove(looser);
				}
			}
		}

		// this returns the allUid entries grouped together, against any sort
		// return official;

		// this returns the proper set of entries, preserving the sort, but allUid is not grouped
		return allOrder;
	}

	/**
	 * Remove any test-drive submissions for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void removeTestDriveSubmissions(Assessment assessment)
	{
		this.storage.removeTestDriveSubmissions(assessment);
	}

	/**
	 * Remove any test-drive submissions for this context.
	 * 
	 * @param context
	 *        The context.
	 */
	protected void removeTestDriveSubmissions(String context)
	{
		this.storage.removeTestDriveSubmissions(context);
	}

	/**
	 * Check if the candidate has a better score than the best so far.
	 * 
	 * @param bestSubmission
	 *        The best so far.
	 * @param candidateSub
	 *        The candidate.
	 * @return true if the candidate is better, false if not.
	 */
	protected boolean sameScores(SubmissionImpl bestSubmission, SubmissionImpl candidateSub)
	{
		Float best = bestSubmission.getTotalScore();
		Float candidate = candidateSub.getTotalScore();
		if ((best == null) && (candidate == null)) return true;
		if ((candidate == null) || (best == null)) return false;
		if (best.floatValue() == candidate.floatValue()) return true;
		return false;
	}

	/**
	 * Sort a list of submissions by their (AssessmentSubmissionStatus) status.
	 * 
	 * @param descending
	 *        true if descending, false if ascending
	 * @param submissions
	 *        The submission list to sort.
	 * @return The sorted list of submissions.
	 */
	protected List<Submission> sortByAssessmentSubmissionStatus(boolean descending, List<Submission> submissions)
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
		List<Submission> overdueCompleteReady = new ArrayList<Submission>();
		List<Submission> ready = new ArrayList<Submission>();
		List<Submission> overdueReady = new ArrayList<Submission>();
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

				case overdueCompleteReady:
				{
					overdueCompleteReady.add(s);
					break;
				}

				case ready:
				{
					ready.add(s);
					break;
				}

				case overdueReady:
				{
					overdueReady.add(s);
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
		rv.addAll(overdueCompleteReady);
		rv.addAll(ready);
		rv.addAll(overdueReady);
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
	 * Sort a list of submissions by their (GradingSubmissionStatus) status.
	 * 
	 * @param descending
	 *        true if descending, false if ascending
	 * @param submissions
	 *        The submission list to sort.
	 * @return The sorted list of submissions.
	 */
	protected List<Submission> sortByGradingSubmissionStatus(boolean descending, List<Submission> submissions)
	{
		// the easy cases
		if ((submissions == null) || (submissions.size() < 2)) return submissions;

		List<Submission> rv = new ArrayList<Submission>();

		// sort order (a) future, notStarted, released, inProgress, submitted, evaluated
		List<Submission> future = new ArrayList<Submission>();
		List<Submission> notStarted = new ArrayList<Submission>();
		List<Submission> released = new ArrayList<Submission>();
		List<Submission> inProgress = new ArrayList<Submission>();
		List<Submission> submitted = new ArrayList<Submission>();
		List<Submission> evaluated = new ArrayList<Submission>();

		for (Submission s : submissions)
		{
			switch (s.getGradingStatus())
			{
				case future:
				{
					future.add(s);
					break;
				}

				case notStarted:
				{
					notStarted.add(s);
					break;
				}

				case inProgress:
				{
					inProgress.add(s);
					break;
				}

				case submitted:
				{
					submitted.add(s);
					break;
				}

				case evaluated:
				{
					evaluated.add(s);
					break;
				}

				case released:
				{
					released.add(s);
					break;
				}
			}
		}

		// order ascending
		rv.addAll(future);
		rv.addAll(notStarted);
		rv.addAll(released);
		rv.addAll(inProgress);
		rv.addAll(submitted);
		rv.addAll(evaluated);

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
