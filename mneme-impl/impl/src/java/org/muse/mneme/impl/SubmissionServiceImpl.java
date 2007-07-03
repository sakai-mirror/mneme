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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;

/**
 * SubmissionServiceImpl implements SubmissionService
 */
public class SubmissionServiceImpl implements SubmissionService, Runnable
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionServiceImpl.class);

	/** The number of ms we allow answers and completions of submissions after hard deadlines. */
	protected final long GRACE = 2 * 60 * 1000;

	/** Dependency: AssessmentService */
	protected AssessmentService m_assessmentService = null;

	/** Dependency: AttachmentService */
	protected AttachmentService m_attachmentService = null;

	/** The # seconds between cache cleaning runs. */
	protected int m_cacheCleanerSeconds = 0;

	/** The # seconds to cache assessment reads. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

	/** Dependency: GradebookExternalAssessmentService */
	// for 2.4 only: protected GradebookExternalAssessmentService m_gradebookService = null;
	protected GradebookService m_gradebookService = null;

	/** Dependency: MemoryService */
	protected MemoryService m_memoryService = null;

	/** Dependency: SecurityService */
	protected SecurityService m_securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/** A cache of submissions. */
	protected Cache m_submissionCache = null;

	/** The checker thread. */
	protected Thread m_thread = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager m_threadLocalManager = null;

	/** The thread quit flag. */
	protected boolean m_threadStop = false;

	/** How long to wait (ms) between checks for timed-out submission in the db. 0 disables. */
	protected long m_timeoutCheckMs = 1000L * 300L;

	/** Dependency: TimeService */
	protected TimeService m_timeService = null;

	/**
	 * {@inheritDoc}
	 */
	public void addSubmission(final Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		// TODO: update the date to now? That would block past / future dating for special purposes... -ggolden

		// TODO: check for important values, such as assessment, user, dates... -ggolden

		Assessment assessment = submission.getAssessment();

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(submission.getUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) m_assessmentService).getAssessmentReference(assessment.getId()));
		}

		// check permission - submission user must have SUBMIT_PERMISSION in the context of the assessment
		m_securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// check that the assessment is currently open for submission
		if (!((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, submission.getSubmittedDate(), 0))
			throw new AssessmentClosedException();

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment, submission.getUserId());
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		if (M_log.isDebugEnabled()) M_log.debug("addSubmission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				addSubmissionTx(submission);
			}
		}, "addSubmission:" + submission.getId());

		// cache a copy
		cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));

		// event track it
		if (m_threadLocalManager.get("sakai.event.suppress") == null)
		{
			m_eventTrackingService.post(m_eventTrackingService
					.newEvent(MnemeService.SUBMISSION_ADD, getSubmissionReference(submission.getId()), true));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission, String userId)
	{
		Boolean rv = Boolean.FALSE;

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (submission != null)
		{
			// make sure the user is this submission's user
			if (submission.getUserId().equals(userId))
			{
				// make sure the submission is incomplete
				if ((submission.getIsComplete() == null) || (!submission.getIsComplete().booleanValue()))
				{
					Assessment assessment = submission.getAssessment();
					if (assessment != null)
					{
						// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
						if (m_securityService.checkSecurity(m_sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION, assessment
								.getContext()))
						{
							// check that the assessment is currently open for submission
							if (((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, m_timeService.newTime(), GRACE))
							{
								rv = Boolean.TRUE;
							}
						}
					}
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission, String userId)
	{
		Boolean rv = Boolean.FALSE;

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (submission != null)
		{
			// make sure the user is this submission's user
			if (submission.getUserId().equals(userId))
			{
				// make sure the submission is complete
				if ((submission.getIsComplete() != null) && (submission.getIsComplete().booleanValue()))
				{
					rv = Boolean.TRUE;
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Assessment assessment, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled())
			M_log.debug("allowSubmit: assessment: " + ((assessment == null) ? "null" : assessment.getId()) + " user: " + userId);

		Boolean rv = Boolean.FALSE;
		if (assessment != null)
		{
			// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
			if (m_securityService.checkSecurity(m_sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext()))
			{
				// check that the assessment is currently open for submission
				// if there is an in-progress submission, but it's too late now... this would catch it
				if (((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, m_timeService.newTime(), 0))
				{
					// see if the user has a submission in progress
					Submission submission = getSubmissionInProgress(assessment, userId);
					if (submission != null)
					{
						rv = Boolean.TRUE;
					}

					// if not, can we make one? Check if there are remaining submissions for this user
					// (also checks that the assessment is open)
					else
					{
						Integer count = countRemainingSubmissions(assessment, userId);
						if ((count != null) && (count.intValue() != 0))
						{
							rv = Boolean.TRUE;
						}
					}
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(final Submission s) throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException
	{
		// trust only the submission id passed in - get fresh and trusted additional information
		Submission submission = idSubmission(s.getId());
		Assessment assessment = submission.getAssessment();

		// the current time
		final Time asOf = m_timeService.newTime();

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) m_assessmentService).getAssessmentReference(assessment.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not
		// submission)
		m_securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// check that the assessment is currently open for submission
		if (!((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, asOf, GRACE)) throw new AssessmentClosedException();

		if (M_log.isDebugEnabled()) M_log.debug("completeSubmission: submission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				completeSubmissionTx(asOf, s.getId());
			}
		}, "completeSubmission:" + s.getId());

		// record in the gradebook if so configured
		recordInGradebook(submission, true);

		// update the submission parameter for the caller
		s.setSubmittedDate(asOf);
		s.setStatus(new Integer(1));
		s.setIsComplete(Boolean.TRUE);

		// collect the cached submission, before the event clears it
		SubmissionImpl recache = getCachedSubmission(s.getId());

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()),
				true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(submission.getId());

		// recache (this object used to be in the cache, but has been cleared, so we are the only owner)
		if (recache != null)
		{
			recache.initSubmittedDate(asOf);
			recache.initStatus(new Integer(1));
			recache.initIsComplete(Boolean.TRUE);

			// cache the object
			cacheSubmission(recache);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(Assessment assessment, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// if we have done this already this thread, use it
		String key = "coundRemainingSubmissions_" + assessment.getId() + "_" + userId;
		Integer count = (Integer) m_threadLocalManager.get(key);
		if (count != null) return count;

		final Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("countRemainingSubmissions: assessment: " + assessment.getId() + " userId: " + userId + " asOf: " + asOf);

		// we need the assessment's dates, and late handling policy, and the # submissions allowed
		// we need the user's count of completed submissions to this assessment
		// TODO: we need to know if the user has submit permissions at all

		String statement = "SELECT PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING, COUNT(AG.PUBLISHEDASSESSMENTID)"
				+ " FROM SAM_PUBLISHEDACCESSCONTROL_T PAC"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true)
				+ " WHERE PAC.ASSESSMENTID = ?"
				+ " GROUP BY AG.PUBLISHEDASSESSMENTID, PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING";

		Object[] fields = new Object[3];
		fields[0] = Integer.valueOf(assessment.getId());
		fields[1] = userId;
		fields[2] = Integer.valueOf(assessment.getId());

		List rv = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					boolean unlimitedSubmissions = result.getBoolean(1);

					int submissionsAllowed = result.getInt(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time startDate = null;
					if (ts != null)
					{
						startDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(4, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(5, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					int lateHandling = result.getInt(6);

					int submissionsMade = result.getInt(7);

					// if before start date, or after retract date, we are done
					if ((startDate != null) && asOf.before(startDate)) return new Integer(0);
					if ((retractDate != null) && asOf.after(retractDate)) return new Integer(0);

					// if after due date and we are not taking late submissions, we are done
					if ((dueDate != null) && (lateHandling != 1) && asOf.after(dueDate)) return new Integer(0);

					// if unlimited, return a -1
					if (unlimitedSubmissions) return new Integer(-1);

					// return the difference between the number taken already and the max
					int left = submissionsAllowed - submissionsMade;

					if (left <= 0) return new Integer(0);

					return new Integer(left);
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentDueDate: " + e);
					return null;
				}
			}
		});

		count = (rv.size() == 0) ? null : (Integer) rv.get(0);

		if (count != null)
		{
			m_threadLocalManager.set(key, count);
		}

		return count;
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
		if (a == null) return null;

		// trust only the id of the assessment passed in - get fresh and trusted additional information
		Assessment assessment = ((AssessmentServiceImpl) m_assessmentService).idAssessment(a.getId());

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled()) M_log.debug("enterSubmission: assessment: " + assessment.getId() + " user: " + userId + " asOf: " + asOf);

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		m_securityService.secure(userId, MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// check that the assessment is currently open for submission
		if (!((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, asOf, 0)) throw new AssessmentClosedException();

		// see if we have one already
		Submission submissionInProgress = getSubmissionInProgress(assessment, userId);
		if (submissionInProgress != null)
		{
			// event track it (not a modify event)
			m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_CONTINUE, getSubmissionReference(submissionInProgress
					.getId()), false));

			return submissionInProgress;
		}

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment, userId);
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		// TODO: it is possible to make too many submissions for the assessment.
		// If this method is entered concurrently for the same user and assessment, the previous count check might fail.
		final Submission submission = newSubmission(assessment);
		submission.setUserId(userId);
		submission.setStatus(new Integer(0));
		submission.setIsComplete(Boolean.FALSE);
		submission.setStartDate(asOf);
		submission.setSubmittedDate(asOf);

		if (M_log.isDebugEnabled()) M_log.debug("addSubmission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				addSubmissionTx(submission);
			}
		}, "addSubmission:" + submission.getId());

		// cache a copy
		cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_ENTER, getSubmissionReference(submission.getId()), true));

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		// TODO: Warning - this query is showing serious performance problems (Oracle)

		final List<Float> rv = new ArrayList<Float>();

		String statement = "SELECT AG.FINALSCORE" + " FROM SAM_ASSESSMENTGRADING_T AG" + " WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " ORDER BY AG.FINALSCORE";

		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		final SubmissionServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					float score = result.getFloat(1);
					rv.add(new Float(score));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentScores: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(String questionId)
	{
		// TODO: Warning - this query is showing serious performance problems (Oracle)
		final List<Float> rv = new ArrayList<Float>();

		String statement = "SELECT SUM(IG.AUTOSCORE+IG.OVERRIDESCORE) AS SCORE" + " FROM SAM_ITEMGRADING_T IG"
				+ " INNER JOIN SAM_ASSESSMENTGRADING_T AG ON IG.ASSESSMENTGRADINGID = AG.ASSESSMENTGRADINGID"
				+ " WHERE IG.PUBLISHEDITEMID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(true) + " GROUP BY IG.ASSESSMENTGRADINGID"
				+ " ORDER BY SCORE";

		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(questionId);

		final SubmissionServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					float score = result.getFloat(1);
					rv.add(new Float(score));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getQuestionScores: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(final String id)
	{
		// TODO: should the main and answers reads be done in a single transaction? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("getSubmission: " + id);

		if (id == null) return null;

		// cached?
		SubmissionImpl cached = getCachedSubmission(id);

		// if not cached, cache a placeholder
		if (cached == null)
		{
			cached = new SubmissionImpl(this);
			cached.initId(id);
			cacheSubmission(cached);
		}

		synchronized (cached)
		{
			// if we need to, read the main info
			if (!cached.isMainInited())
			{
				boolean found = readSubmissionMain(cached);
				if (!found) return null;
			}

			// if we need to, read the answers
			if (!cached.isAnswersInited())
			{
				readSubmissionAnswers(cached);
			}

			// return a copy so we don't return the cache
			return new SubmissionImpl(cached);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(final String context, String userId, GetUserContextSubmissionsSort sort)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();
		final String fUserId = userId;

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled()) M_log.debug("getUserContextSubmissions: context: " + context + " userId: " + userId);

		// figure sort
		String sortSql = null;
		if (sort == null)
		{
			sortSql = "P.TITLE ASC";
		}
		else
		{
			switch (sort)
			{
				case title_a:
				{
					sortSql = "P.TITLE ASC";
					break;
				}

				case title_d:
				{
					sortSql = "P.TITLE DESC";
					break;
				}

				case dueDate_a:
				{
					sortSql = "PAC.DUEDATE ASC, P.TITLE ASC";
					break;
				}

				case dueDate_d:
				{
					sortSql = "PAC.DUEDATE DESC, P.TITLE DESC";
					break;
				}

					// status ascending, i.e. boring to important, is backed by date boring (later) to important (sooner), i.e. date desc
					// status is always read ascending, reversed when we sort it later
				case status_a:
				case status_d:
				{
					sortSql = "PAC.DUEDATE DESC, P.TITLE DESC";
					break;
				}
			}
		}

		String statement = "SELECT AG.ASSESSMENTGRADINGID, P.ID, P.TITLE, AG.FINALSCORE, AG.ATTEMPTDATE,"
				+ " PAC.FEEDBACKDATE, AG.SUBMITTEDDATE, PE.SCORINGTYPE, PF.FEEDBACKDELIVERY, AG.FORGRADE,"
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.TIMELIMIT, PAC.DUEDATE, PAC.LATEHANDLING, PAC.RETRACTDATE, PE.TOGRADEBOOK"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON P.ID = PAC.ASSESSMENTID AND (PAC.RETRACTDATE IS NULL OR ? < PAC.RETRACTDATE)"
				+ " INNER JOIN SAM_PUBLISHEDFEEDBACK_T PF ON P.ID = PF.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON P.ID = PE.ASSESSMENTID"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON P.ID = AG.PUBLISHEDASSESSMENTID AND AG.AGENTID = ?" + " ORDER BY " + sortSql;

		Object[] fields = new Object[4];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;
		fields[2] = asOf;
		fields[3] = userId;

		final SubmissionServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String submissionId = result.getString(1);
					String publishedAssessmentId = result.getString(2);
					String title = result.getString(3);
					float score = result.getFloat(4);

					java.sql.Timestamp ts = result.getTimestamp(5, m_sqlService.getCal());
					Time attemptDate = null;
					if (ts != null)
					{
						attemptDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(6, m_sqlService.getCal());
					Time feedbackDate = null;
					if (ts != null)
					{
						feedbackDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(7, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					int mssPolicy = result.getInt(8);
					FeedbackDelivery feedbackDelivery = FeedbackDelivery.parse(result.getInt(9));
					boolean complete = result.getBoolean(10);
					boolean unlimitedSubmissions = result.getBoolean(11);
					int submissionsAllowed = result.getInt(12);

					ts = result.getTimestamp(13, m_sqlService.getCal());
					Time releaseDate = null;
					if (ts != null)
					{
						releaseDate = m_timeService.newTime(ts.getTime());
					}

					long timeLimit = result.getLong(14);

					ts = result.getTimestamp(15, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					int allowLateSubmit = result.getInt(16);

					ts = result.getTimestamp(17, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					int toGradebook = result.getInt(18);

					// for the non-submissions, create an non-null id
					if (submissionId == null)
					{
						submissionId = publishedAssessmentId + fUserId;
					}

					// create or update these properties in the submission cache
					SubmissionImpl cachedSubmission = getCachedSubmission(submissionId);
					if (cachedSubmission == null)
					{
						// cache an empty, but complete, one
						cachedSubmission = new SubmissionImpl(service);
						cachedSubmission.initId(submissionId);
						cacheSubmission(cachedSubmission);
					}
					synchronized (cachedSubmission)
					{
						cachedSubmission.initAssessmentId(publishedAssessmentId);
						cachedSubmission.initTotalScore(score);
						cachedSubmission.initStartDate(attemptDate);
						cachedSubmission.initSubmittedDate(submittedDate);
						cachedSubmission.initIsComplete(Boolean.valueOf(complete));
						cachedSubmission.initUserId(fUserId);
					}

					// create or update these properties in the assessment cache
					AssessmentImpl cachedAssessment = ((AssessmentServiceImpl) m_assessmentService).getCachedAssessment(publishedAssessmentId);
					if (cachedAssessment == null)
					{
						// cache an empty one
						cachedAssessment = new AssessmentImpl(((AssessmentServiceImpl) m_assessmentService));
						cachedAssessment.initId(publishedAssessmentId);
						((AssessmentServiceImpl) m_assessmentService).cacheAssessment(cachedAssessment);
					}
					synchronized (cachedAssessment)
					{
						cachedAssessment.initContext(context);
						cachedAssessment.initTitle(title);
						cachedAssessment.initFeedbackDate(feedbackDate);
						// cachedAssessment.initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.parse(mssPolicy));
						cachedAssessment.initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED);
						cachedAssessment.initFeedbackDelivery(feedbackDelivery);
						cachedAssessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));
						cachedAssessment.initReleaseDate(releaseDate);
						cachedAssessment.initTimeLimit(timeLimit == 0 ? null : new Long(timeLimit * 1000));
						cachedAssessment.initDueDate(dueDate);
						cachedAssessment.initAllowLateSubmit((allowLateSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);
						cachedAssessment.initRetractDate(retractDate);
						cachedAssessment.initGradebookIntegration(Boolean.valueOf(toGradebook == 1));
					}

					// return a copy of the cached submission
					return new SubmissionImpl(cachedSubmission);
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentDueDate: " + e);
					return null;
				}
			}
		});

		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = new ArrayList<Submission>();

		while (all.size() > 0)
		{
			// take the first one out
			SubmissionImpl submission = (SubmissionImpl) all.remove(0);

			// check if this is over time limit / deadline
			if (submission.getIsOver(asOf, 0))
			{
				// complete this one, using the exact 'over' date for the final date
				Time over = submission.getWhenOver();
				completeTheSubmission(over, submission);

				// update what we read (completeTheSubmission uncaches, so we own this submission object now)
				submission.initStatus(new Integer(1));
				submission.initIsComplete(Boolean.TRUE);
				submission.initSubmittedDate(over);

				// recache a copy
				cacheSubmission(new SubmissionImpl(submission));
			}

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
				if ((submission.getIsComplete() == null) || (!submission.getIsComplete()))
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

					// check if this is over time limit / deadline
					if (candidateSub.getIsOver(asOf, 0))
					{
						// complete this one, using the exact 'over' date for the final date
						Time over = candidateSub.getWhenOver();
						completeTheSubmission(over, candidateSub);

						// update what we read
						candidateSub.initStatus(new Integer(1));
						candidateSub.initIsComplete(Boolean.TRUE);
						candidateSub.initSubmittedDate(over);

						// recache a copy
						cacheSubmission(new SubmissionImpl(candidateSub));
					}

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

		// if sorting by due date, fix it so null due dates are LARGE not SMALL
		if (sort == GetUserContextSubmissionsSort.dueDate_a || sort == GetUserContextSubmissionsSort.dueDate_d
				|| sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		{
			// pull out the null date entries
			List<Submission> nulls = new ArrayList<Submission>();
			for (Iterator i = official.iterator(); i.hasNext();)
			{
				Submission s = (Submission) i.next();
				if (s.getAssessment().getDueDate() == null)
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
			official = sortByStatus(sort, official);
		}

		return official;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission idSubmission(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("idSubmission: " + id);

		if (id == null) return null;

		// cached?
		SubmissionImpl submission = getCachedSubmission(id);
		if (submission != null)
		{
			// return a copy
			synchronized (submission)
			{
				return new SubmissionImpl(submission);
			}
		}

		// TODO: perhaps don't check... (see idAssessment) -ggolden
		// check that it exists
		if (!checkSubmission(id)) return null;

		if (M_log.isDebugEnabled()) M_log.debug("idSubmission: creating: " + id);

		// setup a new assessment with only the id
		submission = new SubmissionImpl(this);
		submission.initId(id);

		// cache a copy
		cacheSubmission(new SubmissionImpl(submission));

		return submission;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// <= 0 indicates no caching desired
			if ((m_cacheSeconds > 0) && (m_cacheCleanerSeconds > 0))
			{
				// submissions cache, automatiaclly checking for expiration as configured mins, expire on events...
				m_submissionCache = new SubmissionCacheImpl(m_memoryService, m_eventTrackingService, m_cacheCleanerSeconds,
						getSubmissionReference(""), ":");
			}

			// start the checking thread
			if (m_timeoutCheckMs > 0)
			{
				start();
			}

			M_log.info("init(): caching minutes: " + m_cacheSeconds / 60 + " cache cleaner minutes: " + m_cacheCleanerSeconds / 60
					+ " timout check seconds: " + m_timeoutCheckMs / 1000);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission newSubmission(Assessment assessment)
	{
		SubmissionImpl submission = new SubmissionImpl(this);
		submission.setInited();

		submission.setAssessment(assessment);

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionAnswer newSubmissionAnswer(Submission submission, AssessmentQuestion question)
	{
		SubmissionAnswerImpl answer = new SubmissionAnswerImpl();
		answer.initSubmission((SubmissionImpl) submission);
		((SubmissionImpl) submission).answers.add(answer);

		answer.initQuestion(question);

		return answer;
	}

	/**
	 * Run the event checking thread.
	 */
	public void run()
	{
		// since we might be running while the component manager is still being created and populated, such as at server startup,
		// wait here for a complete component manager
		ComponentManager.waitTillConfigured();

		// loop till told to stop
		while ((!m_threadStop) && (!Thread.currentThread().isInterrupted()))
		{
			try
			{
				// get a list of submission ids that are open, timed, and well expired considering double our grace period, or open and past a retract
				// date or hard deadline
				List<Submission> submissions = getTimedOutSubmissions(2 * GRACE);

				// for each one, close it if it is still open
				for (Submission submission : submissions)
				{
					// we need to establish the "current" user to be the submission user
					// so that various attributions of the complete process have the proper user
					String user = submission.getUserId();
					Session s = m_sessionManager.getCurrentSession();
					if (s != null)
					{
						s.setUserId(user);
					}
					else
					{
						M_log.warn("run - no SessionManager.getCurrentSession, cannot set to user");
					}

					// complete this submission, using the exact 'over' date for the final date
					Time over = submission.getWhenOver();
					completeTheSubmission(over, submission);
				}
			}
			catch (Throwable e)
			{
				M_log.warn("run: will continue: ", e);
			}
			finally
			{
				// clear out any current current bindings
				m_threadLocalManager.clear();
			}

			// take a small nap
			try
			{
				Thread.sleep(m_timeoutCheckMs);
			}
			catch (Exception ignore)
			{
			}
		}
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		m_assessmentService = service;
	}

	/**
	 * Dependency: AttachmentService.
	 * 
	 * @param service
	 *        The AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		m_attachmentService = service;
	}

	/**
	 * Set the # minutes between cache cleanings.
	 * 
	 * @param time
	 *        The # minutes between cache cleanings. (as an integer string).
	 */
	public void setCacheCleanerMinutes(String time)
	{
		m_cacheCleanerSeconds = Integer.parseInt(time) * 60;
	}

	/**
	 * Set the # minutes to cache.
	 * 
	 * @param time
	 *        The # minutes to cache a get (as an integer string).
	 */
	public void setCacheMinutes(String time)
	{
		m_cacheSeconds = Integer.parseInt(time) * 60;
	}

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		m_eventTrackingService = service;
	}

	/**
	 * Dependency: GradebookService.
	 * 
	 * @param service
	 *        The GradebookService.
	 */
	public void setGradebookService(/* for 2.4 only: GradebookExternalAssessmentService */GradebookService service)
	{
		m_gradebookService = service;
	}

	/**
	 * Dependency: MemoryService.
	 * 
	 * @param service
	 *        The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		m_securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		m_sessionManager = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		m_threadLocalManager = service;
	}

	/**
	 * Set the # seconds to wait between db checks for timed-out submissions.
	 * 
	 * @param time
	 *        The # seconds to wait between db checks for timed-out submissions.
	 */
	public void setTimeoutCheckSeconds(String time)
	{
		m_timeoutCheckMs = Integer.parseInt(time) * 1000L;
	}

	/**
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(SubmissionAnswer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>(1);
		answers.add(answer);
		submitAnswers(answers, completeAnswer, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswers(final List<SubmissionAnswer> answers, Boolean completeAnswers, Boolean completeSubmission)
			throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException
	{
		// treat null booleans as false
		if (completeAnswers == null) completeAnswers = Boolean.FALSE;
		if (completeSubmission == null) completeSubmission = Boolean.FALSE;

		if ((answers == null) || (answers.size() == 0)) return;

		// unless we are going to complete the submission, if there has been no change in the answers, do nothing...
		// unless the answers are to be marked complete and they don't all have a submitted date
		if (!completeSubmission.booleanValue())
		{
			boolean anyChange = false;
			for (SubmissionAnswer answer : answers)
			{
				if (answer.getIsChanged().booleanValue())
				{
					anyChange = true;
					break;
				}

				if (completeAnswers.booleanValue() && (answer.getSubmittedDate() == null))
				{
					anyChange = true;
					break;
				}
			}

			if (!anyChange) return;
		}

		// trust only the answer information passed in, and the submission id it points to - get fresh and trusted additional information
		final Submission submission = idSubmission(answers.get(0).getSubmission().getId());
		Assessment assessment = submission.getAssessment();

		// a submission from the cache to update and re-cache
		SubmissionImpl recache = null;

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswer: submission: " + submission.getId() + " complete?: " + Boolean.toString(completeSubmission) + " asOf: " + asOf);

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION,
					((AssessmentServiceImpl) m_assessmentService).getAssessmentReference(assessment.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not
		// submission)
		m_securityService.secure(submission.getUserId(), MnemeService.SUBMIT_PERMISSION, assessment.getContext());

		// check that the assessment is currently open for submission
		// Note: we accept answers up to GRACE ms after any hard deadlilne
		if (!((AssessmentServiceImpl) m_assessmentService).isAssessmentOpen(assessment, asOf, GRACE)) throw new AssessmentClosedException();

		// update the dates and answer scores
		submission.setSubmittedDate(asOf);
		for (SubmissionAnswer answer : answers)
		{
			// mark a submitted date only if the new answers are complete (and the answer has otherwise been changed OR the answer is incomplete)
			if (completeAnswers.booleanValue())
			{
				if (answer.getIsChanged().booleanValue() || (answer.getSubmittedDate() == null))
				{
					answer.setSubmittedDate(asOf);
				}
			}

			// auto-score
			answer.autoScore();
		}

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		final Boolean completeSubmissionFinal = completeSubmission;
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				submitAnswersTx(answers, submission, completeSubmissionFinal);
			}
		}, "submitAnswers:" + submission.getId());

		// if complete and the assessment is integrated into the Gradebook, record the grade
		if (completeSubmission.booleanValue())
		{
			recordInGradebook(submission, true);
		}

		// collect the cached submission, before the event clears it
		recache = getCachedSubmission(submission.getId());

		// event track it (one for each answer)
		for (SubmissionAnswer answer : answers)
		{
			if (answer.getIsChanged())
			{
				m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_ANSWER,
						getSubmissionReference(submission.getId()) + ":" + answer.getQuestion().getId(), true));
			}
		}

		// track if we are complete
		if (completeSubmission.booleanValue())
		{
			m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()),
					true));
		}

		// the submission is altered by this - clear the cache
		unCacheSubmission(submission.getId());

		// recache (this object that used to be in the cache is no longer in the cache, so we are the only owner)
		if (recache != null)
		{
			// if the cached submission has had its answers read, we will update them
			if (recache.isAnswersInited())
			{
				for (SubmissionAnswer answer : answers)
				{
					if (answer.getIsChanged().booleanValue())
					{
						// This new answer for it's question id should replace an existing on in the submission, or, be added to the answers
						SubmissionAnswerImpl old = recache.findAnswer(answer.getQuestion().getId());
						if (old != null)
						{
							recache.answers.remove(old);
						}

						// update the cache with a copy of the answer, with the change flag cleared
						SubmissionAnswerImpl newAnswer = new SubmissionAnswerImpl((SubmissionAnswerImpl) answer);
						newAnswer.changed = Boolean.FALSE;

						recache.answers.add(newAnswer);
					}
				}
			}

			recache.initSubmittedDate(asOf);

			if (completeSubmission.booleanValue())
			{
				recache.initStatus(new Integer(1));
				recache.initIsComplete(Boolean.TRUE);
			}

			// cache the object
			cacheSubmission(recache);
		}

		// return properly updated answers - clear the chanegd flags
		for (SubmissionAnswer answer : answers)
		{
			((SubmissionAnswerImpl) answer).changed = Boolean.FALSE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateGradebook(Assessment assessment) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("updateGradebook: " + assessment.getId());

		// check permission
		m_securityService.secure(m_sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());

		// skip if there is no gradebook integration
		if ((assessment.getGradebookIntegration() == null) || !assessment.getGradebookIntegration().booleanValue()) return;

		// try each user with a submission
		List<String> userIds = getAssessmentSubmittedUsers(assessment);
		for (String uid : userIds)
		{
			// find the user's highest score among the completed submissions
			Float highestScore = getSubmissionHighestScore(assessment.getId(), uid);

			// push this into the GB
			try
			{
				m_gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), uid, ((highestScore == null) ? null
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
	 * The transaction for addSubmission
	 */
	protected void addSubmissionTx(Submission submission)
	{
		// TODO: eval? skip it?

		// we only work with our impl
		SubmissionImpl s = (SubmissionImpl) submission;

		// ID column? For non sequence db vendors, it is defaulted
		Long id = m_sqlService.getNextSequence("SAM_ASSESSMENTGRADING_ID_S", null);

		// Note: ASSESSMENTGRADINGID column is set to autoincrement... by using the special JDBC feature in dbInsert, we get the
		// value just allocated
		String statement = "INSERT INTO SAM_ASSESSMENTGRADING_T"
				+ " (PUBLISHEDASSESSMENTID, AGENTID, SUBMITTEDDATE, ISLATE, FORGRADE, TOTALAUTOSCORE,"
				+ " TOTALOVERRIDESCORE, FINALSCORE, STATUS, ATTEMPTDATE, TIMEELAPSED" + ((id == null) ? "" : ", ASSESSMENTGRADINGID") + ")"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?" + ((id == null) ? "" : ",?") + ")";
		Object fields[] = new Object[(id == null) ? 11 : 12];
		fields[0] = Integer.valueOf(s.getAssessmentId());
		fields[1] = s.getUserId();
		fields[2] = s.getSubmittedDate();
		fields[3] = new Integer(0); // TODO: islate
		fields[4] = s.getIsComplete().booleanValue() ? new Integer(1) : new Integer(0);
		fields[5] = s.getAnswersAutoScore();
		fields[6] = new Float(0); // TODO: from evaluation, the total of all manual scores for the submission
		fields[7] = fields[5]; // TODO: from evaluation, the total score, auto plus manual...
		fields[8] = s.getStatus();
		fields[9] = s.getStartDate();
		fields[10] = (s.getElapsedTime() == null) ? null : (s.getElapsedTime() / 1000);

		if (id != null)
		{
			fields[11] = id;
			m_sqlService.dbWrite(statement, fields);
		}
		else
		{
			id = m_sqlService.dbInsert(null, statement, fields, "ASSESSMENTGRADINGID");
		}

		// we really need that id
		if (id == null) throw new RuntimeException("failed to insert submission");

		// update the id
		s.initId(id.toString());

		// answers
		for (SubmissionAnswerImpl answer : s.answers)
		{
			// each answer has one or more entries
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

				statement = "INSERT INTO SAM_ITEMGRADING_T"
						+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
						+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
						+ " VALUES (?,?,?,?,?,?,?,?,?,?" + ((answerId == null) ? "" : ",?") + ")";
				fields = new Object[(answerId == null) ? 10 : 11];
				fields[0] = Integer.valueOf(answer.getSubmission().getId());
				fields[1] = Integer.valueOf(answer.getQuestionId());
				// if the entry's assessment answer is null, use the single part id
				fields[2] = Integer.valueOf((entry.getAssessmentAnswer() != null) ? entry.getAssessmentAnswer().getPart().getId() : answer
						.getQuestion().getPart().getId());
				fields[3] = s.getUserId();
				fields[4] = answer.getSubmittedDate();
				fields[5] = (entry.getAssessmentAnswer() == null) ? null : entry.getAssessmentAnswer().getId();
				fields[6] = answer.getRationale();
				fields[7] = entry.getAnswerText();
				fields[8] = entry.getAutoScore();
				fields[9] = new Float(0); // TODO: manual score from evaluation for this answer (divided up over the entries)

				if (answerId != null)
				{
					fields[10] = answerId;
					m_sqlService.dbWrite(statement, fields);
				}
				else
				{
					answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
				}

				// we really need that id
				if (answerId == null) throw new RuntimeException("failed to insert submission answer");
				entry.initId(answerId.toString());
			}
		}
	}

	/**
	 * Cache this submission. Use the short-term cache if enable, else use the thread-local cache.
	 * 
	 * @param submission
	 *        The submission to cache.
	 */
	protected void cacheSubmission(SubmissionImpl submission)
	{
		String ref = getSubmissionReference(submission.getId());

		// Note: we thread-local cache always, even if we are otherwise caching
		m_threadLocalManager.set(ref, submission);

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			m_submissionCache.put(ref, submission, m_cacheSeconds);
		}
	}

	/**
	 * Check a submission answer for complete correctness
	 * 
	 * @param answer
	 *        The answer to score.
	 * @return true if the answer is completely correct, false if not.
	 */
	protected boolean checkAnswer(SubmissionAnswerImpl answer)
	{
		AssessmentQuestion question = answer.getQuestion();

		// trueFalse / multipleChoice - one entry to check
		if ((question.getType() == QuestionType.trueFalse) || (question.getType() == QuestionType.multipleChoice))
		{
			if (answer.entries.get(0).getIsCorrect().booleanValue())
			{
				return true;
			}

			return false;
		}

		// multipleAnswer - only the correct choices should be selected
		else if (question.getType() == QuestionType.multipleCorrect)
		{
			// these correct answers must be selected
			List<AssessmentAnswer> correct = question.getPart().getCorrectAnswers();

			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				if (correct.contains(entry.getAssessmentAnswer()))
				{
					correct.remove(entry.getAssessmentAnswer());
				}

				// otherwise we found an incorrect entry
				else
				{
					return false;
				}
			}

			// if we have an entry for each correct, this is correct
			if (correct.isEmpty()) return true;

			return false;
		}

		// fillIn / numeric / matching - all entries must be correct
		else if ((question.getType() == QuestionType.fillIn) || (question.getType() == QuestionType.numeric)
				|| (question.getType() == QuestionType.matching))
		{
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				if (!entry.getIsCorrect().booleanValue())
				{
					return false;
				}
			}

			// mutually exclusive check
			if ((question.getType() == QuestionType.fillIn) && (question.getMutuallyExclusive().booleanValue()))
			{
				// check all but the last entry, looking down the list for a match
				// if this answer matches any following answer, and their question text also matches, this answer gets zero'ed out
				for (int i = 0; i < answer.entries.size() - 1; i++)
				{
					SubmissionAnswerEntryImpl entry = answer.entries.get(i);

					// this is the question text that must match some entry-down-below's question text
					String entryQuestionText = entry.getAssessmentAnswer().getText();

					// look down the list
					for (int j = i + 1; j < answer.entries.size(); j++)
					{
						// compare to this entry
						SubmissionAnswerEntryImpl compareEntry = answer.entries.get(j);

						// they need to be the same (i.e. !different) based on our case sensitive (the method takes ignore case, so
						// we reverse)
						if (!StringUtil.different(entry.getAnswerText(), compareEntry.getAnswerText(), !question.getCaseSensitive().booleanValue()))
						{
							// we will check against this other question's text, exactly
							String compareEntryQuestionText = compareEntry.getAssessmentAnswer().getText();
							if (entryQuestionText.equals(compareEntryQuestionText))
							{
								// we have a later match, so this is not correct
								return false;
							}
						}
					}
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Check if a submission is defined
	 * 
	 * @param id
	 *        The submission id to check.
	 */
	protected boolean checkSubmission(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("checkSubmission: " + id);

		String statement = "SELECT AG.ASSESSMENTGRADINGID FROM SAM_ASSESSMENTGRADING_T AG WHERE AG.ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(id);

		List results = m_sqlService.dbRead(statement, fields, null);
		return !results.isEmpty();
	}

	/**
	 * Transaction code for completeSubmission.
	 */
	protected void completeSubmissionTx(Time asOf, String submissionId)
	{
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T" + " SET SUBMITTEDDATE = ?, STATUS = 1, FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " WHERE ASSESSMENTGRADINGID = ?";
		Object fields[] = new Object[2];
		fields[0] = asOf;
		fields[1] = Integer.valueOf(submissionId);
		m_sqlService.dbWrite(statement, fields);
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
	protected boolean completeTheSubmission(Time asOf, final Submission submission)
	{
		// the current time if not set
		if (asOf == null) asOf = m_timeService.newTime();
		final Time fAsOf = asOf;

		if (M_log.isDebugEnabled()) M_log.debug("completeTheSubmission: submission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				completeTheSubmissionTx(fAsOf, submission.getId());
			}
		}, "completeSubmission:" + submission.getId());

		// record in the gradebook if so configured, using data only from the submission, no db refresh
		recordInGradebook(submission, false);

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_AUTO_COMPLETE,
				getSubmissionReference(submission.getId()), true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(submission.getId());

		return true;
	}

	/**
	 * The transaction code for completeTheSubmission.
	 */
	protected void completeTheSubmissionTx(Time asOf, String submissionId)
	{
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T" + " SET SUBMITTEDDATE = ?, STATUS = 1, FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " WHERE ASSESSMENTGRADINGID = ? AND FORGRADE = " + m_sqlService.getBooleanConstant(false);
		Object fields[] = new Object[2];
		fields[0] = asOf;
		fields[1] = Integer.valueOf(submissionId);
		m_sqlService.dbWrite(statement, fields);
	}

	/**
	 * Get a list of the users who have submitted to the assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return A List <String> of user ids of the users who have any form of submission to the assessment.
	 */
	protected List<String> getAssessmentSubmittedUsers(Assessment assessment)
	{
		String statement = "SELECT DISTINCT AG.AGENTID FROM SAM_ASSESSMENTGRADING_T AG WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true);

		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		List all = m_sqlService.dbRead(statement, fields, null);
		List<String> rv = new ArrayList<String>(all.size());
		for (Iterator i = all.iterator(); i.hasNext();)
		{
			rv.add((String) i.next());
		}

		return rv;
	}

	/**
	 * Check the cache for the submission. Use the short-term cache if enabled, else use the thread-local cache.
	 * 
	 * @param id
	 *        The submission id.
	 * @return The actual submission object cached, or null if not.
	 */
	protected SubmissionImpl getCachedSubmission(String id)
	{
		// Note: untill Samigo is no longer messing with the db behind our back, so that we can't keep our cache valid,
		// don't use anything from the cache that might have been modified by Samigo; such as completed submissions.
		// The thread-local cache is ok to use whatever we got.

		String ref = getSubmissionReference(id);

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			// if it is in there
			if (m_submissionCache.containsKey(ref))
			{
				SubmissionImpl s = (SubmissionImpl) m_submissionCache.get(ref);
				if (s != null)
				{
					// only incomplete, please
					if ((s.isComplete == null) || (!s.isComplete.booleanValue()))
					{
						return s;
					}
				}
			}
		}

		// if not found, check the thread-local cache
		return (SubmissionImpl) m_threadLocalManager.get(ref);
	}

	/**
	 * Find the highest score for this submission's user and assessment.
	 * 
	 * @param submission
	 *        The submission.
	 * @return The highest final score for the submissions's user and assessment.
	 */
	protected Float getSubmissionHighestScore(String assessmentId, String userId)
	{
		String statement = "SELECT MAX(FINALSCORE) FROM SAM_ASSESSMENTGRADING_T AG WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true);

		Object[] fields = new Object[2];
		fields[0] = Integer.valueOf(assessmentId);
		fields[1] = userId;

		final SubmissionServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					float score = result.getFloat(1);
					return new Float(score);
				}
				catch (SQLException e)
				{
					M_log.warn("getSubmissionHighestScore: " + e);
					return null;
				}
			}
		});

		return all.isEmpty() ? null : (Float) all.get(0);
	}

	/**
	 * Check if the user has an open submission to this assessment, and return it if found.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return The open submission for this user to this assessment, if found, or null if not.
	 */
	protected Submission getSubmissionInProgress(Assessment assessment, String userId)
	{
		Submission rv = null;

		// see if we have one already
		String statement = "SELECT AG.ASSESSMENTGRADINGID" + " FROM SAM_ASSESSMENTGRADING_T AG"
				+ " WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(false);
		// TODO: order by id asc so we always use the lowest (oldest) if there are ever two open?
		Object[] fields = new Object[2];
		fields[0] = Integer.valueOf(assessment.getId());
		fields[1] = userId;
		List results = m_sqlService.dbRead(statement, fields, null);
		if (results.size() > 0)
		{
			// we have one
			if (results.size() > 1)
				M_log.warn("getSubmissionInProgress: multiple incomplete submissions: " + results.size() + " aid: " + assessment.getId()
						+ " userId: " + userId);
			rv = idSubmission((String) results.get(0));
		}

		return rv;
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
		String ref = MnemeService.REFERENCE_ROOT + "/" + MnemeService.SUBMISSION_TYPE + "/" + submissionId;
		return ref;
	}

	/**
	 * Find the submissions that are open, timed, and well expired, or open and past a retract date or hard deadline
	 * 
	 * @param grace
	 *        The number of ms past the time limit that the submission's elapsed time must be to qualify
	 * @return A List of the submissions that are open, timed, and well expired.
	 */
	protected List<Submission> getTimedOutSubmissions(final long grace)
	{
		if (M_log.isDebugEnabled()) M_log.debug("getTimedOutSubmissions");

		final Time asOf = m_timeService.newTime();

		// select all the open submissions (or, just the TIMELIMIT > 0 or DUEDATE not null or RETRACTDATE not null?)
		String statement = "SELECT AG.ASSESSMENTGRADINGID, AG.ATTEMPTDATE, AG.AGENTID, AG.FINALSCORE, AG.PUBLISHEDASSESSMENTID,"
				+ " PAC.TIMELIMIT, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING, PE.SCORINGTYPE, PE.TOGRADEBOOK"
				+ " FROM SAM_ASSESSMENTGRADING_T AG" + " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON AG.PUBLISHEDASSESSMENTID = PAC.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON AG.PUBLISHEDASSESSMENTID = PE.ASSESSMENTID" + " WHERE AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(false);

		Object[] fields = new Object[0];

		final SubmissionServiceImpl service = this;
		final List<Submission> rv = new ArrayList<Submission>();
		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String submissionId = result.getString(1);

					java.sql.Timestamp ts = result.getTimestamp(2, m_sqlService.getCal());
					Time attemptDate = null;
					if (ts != null)
					{
						attemptDate = m_timeService.newTime(ts.getTime());
					}

					String userId = result.getString(3);
					float score = result.getFloat(4);
					String publishedAssessmentId = result.getString(5);

					// convert to ms from seconds
					long timeLimit = result.getLong(6) * 1000;

					ts = result.getTimestamp(7, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(8, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					boolean allowLate = (result.getInt(9) == 1);
					// MultipleSubmissionSelectionPolicy mssPolicy = MultipleSubmissionSelectionPolicy.parse(result.getInt(10));
					MultipleSubmissionSelectionPolicy mssPolicy = MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED;
					int toGradebook = result.getInt(11);

					// see if we want this one
					boolean selected = false;

					// for timed, if the elapsed time since their start is well past the time limit
					if ((timeLimit > 0) && (attemptDate != null) && ((asOf.getTime() - attemptDate.getTime()) > (timeLimit + grace)))
					{
						selected = true;
					}

					// for past retract date
					if ((retractDate != null) && (asOf.getTime() > (retractDate.getTime() + grace)))
					{
						selected = true;
					}

					// for past hard due date
					if ((dueDate != null) && (!allowLate) && (asOf.getTime() > (dueDate.getTime() + grace)))
					{
						selected = true;
					}

					if (selected)
					{
						// create or update these properties in the submission cache
						SubmissionImpl cachedSubmission = getCachedSubmission(submissionId);
						if (cachedSubmission == null)
						{
							// cache an empty one
							cachedSubmission = new SubmissionImpl(service);
							cachedSubmission.initId(submissionId);
							cacheSubmission(cachedSubmission);
						}
						synchronized (cachedSubmission)
						{
							cachedSubmission.initAssessmentId(publishedAssessmentId);
							cachedSubmission.initTotalScore(score);
							cachedSubmission.initStartDate(attemptDate);
							cachedSubmission.initUserId(userId);
							cachedSubmission.initIsComplete(Boolean.FALSE);
						}

						rv.add(new SubmissionImpl(cachedSubmission));

						// create or update these properties in the assessment cache
						AssessmentImpl cachedAssessment = ((AssessmentServiceImpl) m_assessmentService).getCachedAssessment(publishedAssessmentId);
						if (cachedAssessment == null)
						{
							// cache an empty one
							cachedAssessment = new AssessmentImpl(((AssessmentServiceImpl) m_assessmentService));
							cachedAssessment.initId(publishedAssessmentId);
							((AssessmentServiceImpl) m_assessmentService).cacheAssessment(cachedAssessment);
						}
						synchronized (cachedAssessment)
						{
							cachedAssessment.initTimeLimit(timeLimit == 0 ? null : new Long(timeLimit));
							cachedAssessment.initDueDate(dueDate);
							cachedAssessment.initAllowLateSubmit(Boolean.valueOf(allowLate));
							cachedAssessment.initRetractDate(retractDate);
							cachedAssessment.initMultipleSubmissionSelectionPolicy(mssPolicy);
							cachedAssessment.initGradebookIntegration(Boolean.valueOf(toGradebook == 1));
						}
					}
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getTimedOutSubmissions: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Id each of the submission in the id list
	 * 
	 * @param ids
	 *        The collection of submission ids.
	 * @return A collection if id'ed submission, one for each id.
	 */
	protected List<Submission> idSubmissions(List<String> ids)
	{
		List<Submission> rv = new ArrayList<Submission>(ids.size());
		for (String id : ids)
		{
			rv.add(idSubmission(id));
		}

		return rv;
	}

	/**
	 * Read the answers of the submission (not the main)
	 * 
	 * @param submission
	 *        The submission impl with the id set to fill in.
	 */
	protected void readSubmissionAnswers(final SubmissionImpl submission)
	{
		// TODO: Transaction to assure a consistent read? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("readSubmissionAnswers: " + submission.getId());

		if (submission.getId() == null)
		{
			M_log.warn("readSubmissionAnswers: attempt to read with no id set");
			return;
		}

		// mark the submission as inited for the answers, so the methods we are about to call don't try to re-read the answers
		submission.answersStatus = SubmissionImpl.PropertyStatus.inited;

		// read the answers.
		// The PUBLISHEDITEMTEXTID points to a question part, and we want to read the entiries in question part sequence order,
		// so we join to the SAM_PUBLISHEDITEMTEXT_T (i.e. question parts) table and order by the sequence there
		String statement = "SELECT I.ITEMGRADINGID, I.SUBMITTEDDATE, I.PUBLISHEDANSWERID, I.RATIONALE, I.ANSWERTEXT, I.REVIEW, I.PUBLISHEDITEMID, I.AUTOSCORE, I.PUBLISHEDITEMTEXTID, I.COMMENTS"
				+ " FROM SAM_ITEMGRADING_T I"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMTEXT_T PIT ON I.PUBLISHEDITEMTEXTID = PIT.ITEMTEXTID"
				+ " WHERE I.ASSESSMENTGRADINGID = ?" + " ORDER BY PIT.SEQUENCE ASC";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(submission.getId());

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String id = result.getString(1);

					java.sql.Timestamp ts = result.getTimestamp(2, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					String answerId = StringUtil.trimToNull(result.getString(3));
					String rationale = result.getString(4);
					String answerText = result.getString(5);
					boolean markedForReview = result.getBoolean(6);
					String questionId = result.getString(7);
					float autoScore = result.getFloat(8);
					String questionPartId = result.getString(9);
					String comments = result.getString(10);

					// do we have the answer to this question yet?
					SubmissionAnswerImpl answer = submission.findAnswer(questionId);

					// if not, make one and save it in the submission
					if (answer == null)
					{
						answer = new SubmissionAnswerImpl();
						answer.initQuestionId(questionId);
						answer.initRationale(rationale);
						answer.initMarkedForReview(Boolean.valueOf(markedForReview));
						answer.initSubmittedDate(submittedDate);
						answer.initEvalComments(StringUtil.trimToNull(comments));
						answer.id = id;

						answer.initSubmission(submission);
						submission.answers.add(answer);
					}

					// add an entry to the answer
					SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
					entry.initQuestionPartId(questionPartId);
					entry.initAssessmentAnswerId(answerId);
					entry.setAnswerText(answerText);
					entry.initId(id);
					entry.initAutoScore(new Float(autoScore));

					entry.initAnswer(answer);
					answer.entries.add(entry);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissionAnswers: " + e);
					return null;
				}
			}
		});

		// TODO: deduce the answer eval scores - the difference between the set auto score and the computed auto score
		for (SubmissionAnswerImpl answer : submission.answers)
		{
			// see what we read in from the db
			float dbScore = answer.countAutoScore();

			// do the auto-score, setting the proper auto score
			scoreAnswer(answer);

			// see what we have different, that's the eval score
			float newScore = answer.countAutoScore();
			if (newScore != dbScore)
			{
				answer.initEvalScore(new Float(dbScore - newScore));
			}
		}

		// read the uploaded attachments for the answers, and fill out the entries to hold their refs
		statement = "SELECT M.MEDIAID, M.FILENAME, I.PUBLISHEDITEMID" + " FROM SAM_MEDIA_T M"
				+ " INNER JOIN SAM_ITEMGRADING_T I ON M.ITEMGRADINGID = I.ITEMGRADINGID" + " WHERE I.ASSESSMENTGRADINGID = ?"
				+ " ORDER BY M.CREATEDDATE ASC";
		fields = new Object[1];
		fields[0] = Integer.valueOf(submission.getId());
		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String mediaId = result.getString(1);
					String name = result.getString(2);
					String questionId = result.getString(3);

					// we should already have an answer
					SubmissionAnswerImpl answer = submission.findAnswer(questionId);
					if (answer != null)
					{
						// we should have at least one entry
						SubmissionAnswerEntryImpl entry = answer.entries.get(0);
						if (entry != null)
						{
							// use this if the answer text is not a reference
							SubmissionAnswerEntryImpl newEntry = null;
							if ((entry.getAnswerText() == null) || (!entry.getAnswerText().startsWith("/")))
							{
								newEntry = entry;
							}

							// otherwise make a new entry
							else
							{
								newEntry = new SubmissionAnswerEntryImpl(entry);
								newEntry.setAssessmentAnswer(null);
								newEntry.setAnswerText(null);
								newEntry.initId(null);
								newEntry.initAnswer(answer);
								newEntry.initAutoScore(new Float(0));
								answer.entries.add(newEntry);
							}

							// set the reference to the attachment into the answer text
							String refStr = m_attachmentService.getAttachmentReference(submission.getId(), mediaId, name);
							newEntry.setAnswerText(refStr);
						}

						else
						{
							M_log.warn("readSubmissionAnswers: missing entry for answer to question for attachment: questionId: " + questionId
									+ " mediaId: " + mediaId);
						}
					}
					else
					{
						M_log.warn("readSubmissionAnswers: missing answer to question for attachment: questionId: " + questionId + " mediaId: "
								+ mediaId);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissionAnswers: " + e);
					return null;
				}
			}
		});

		// verify the answers
		for (SubmissionAnswerImpl answer : submission.answers)
		{
			answer.verifyEntries();
		}

		// update the cache if cached
		SubmissionImpl cached = getCachedSubmission(submission.getId());
		if (cached != null)
		{
			synchronized (cached)
			{
				cached.setAnswers(submission);
			}
		}
	}

	/**
	 * Read the main parts of the submission (not the answers)
	 * 
	 * @param submission
	 *        The submission impl with the id set to fill in.
	 * @return true if we read, false if we could not find the submission.
	 */
	protected boolean readSubmissionMain(final SubmissionImpl submission)
	{
		if (M_log.isDebugEnabled()) M_log.debug("readSubmissionMain: " + submission.getId());

		if (submission.getId() == null)
		{
			M_log.warn("readSubmissionMain: attempt to read with no id set");
			return false;
		}

		String statement = "SELECT AG.PUBLISHEDASSESSMENTID, AG.TOTALOVERRIDESCORE, AG.SUBMITTEDDATE, AG.AGENTID, AG.FORGRADE, AG.ATTEMPTDATE, AG.STATUS, AG.COMMENTS"
				+ " FROM SAM_ASSESSMENTGRADING_T AG" + " WHERE AG.ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(submission.getId());

		List results = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String aid = result.getString(1);
					float manualScore = result.getFloat(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					String userId = result.getString(4);
					boolean complete = result.getBoolean(5);

					ts = result.getTimestamp(6, m_sqlService.getCal());
					Time startDate = null;
					if (ts != null)
					{
						startDate = m_timeService.newTime(ts.getTime());
					}

					int status = result.getInt(7);
					String comments = result.getString(8);

					// pack it into a submission
					submission.initAssessmentId(aid);
					submission.initIsComplete(Boolean.valueOf(complete));
					submission.initStartDate(startDate);
					submission.initStatus(new Integer(status));
					submission.initSubmittedDate(submittedDate);
					submission.initUserId(userId);
					submission.initEvalScore(new Float(manualScore));
					submission.initEvalComment(StringUtil.trimToNull(comments));

					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("getSubmission: " + e);
					return null;
				}
			}
		});

		if (!results.isEmpty())
		{
			// update the cache if cached
			SubmissionImpl cached = getCachedSubmission(submission.getId());
			if (cached != null)
			{
				synchronized (cached)
				{
					cached.setMain(submission);
				}
			}

			return true;
		}

		// we didn't find it
		return false;
	}

	/**
	 * Record this submission in the gradebook. Assume it has complete and ready to go.
	 * 
	 * @param submission
	 *        The submission to record in the gradebook.
	 * @param refresh
	 *        if true, get the latest score from the db, if false, use the final score from the submission.
	 */
	protected void recordInGradebook(Submission submission, boolean refresh)
	{
		Assessment assessment = submission.getAssessment();

		// if the assessment is integrated into the Gradebook, record the grade
		if ((assessment.getGradebookIntegration() != null) && assessment.getGradebookIntegration().booleanValue())
		{
			// is there a gradebook? - we could just not care here, save all those GB db calls, and let it throw later
			// but that forces us to do our single read for score -ggolden
			if (true /* m_gradebookService.isGradebookDefined(assessment.getContext()) */)
			{
				Double points = null;

				if (refresh)
				{
					// read the final score
					String statement = "SELECT FINALSCORE FROM SAM_ASSESSMENTGRADING_T WHERE ASSESSMENTGRADINGID = ?";
					Object[] fields = new Object[1];
					fields[0] = Integer.valueOf(submission.getId());
					final List<String> scores = new ArrayList<String>(1);
					m_sqlService.dbRead(statement, fields, new SqlReader()
					{
						public Object readSqlResultRecord(ResultSet result)
						{
							try
							{
								String score = result.getString(1);
								scores.add(score);
								return null;
							}
							catch (SQLException e)
							{
								M_log.warn("submitAnswers: " + e);
								return null;
							}
						}
					});

					if (scores.size() == 1)
					{
						points = Double.valueOf(scores.get(0));
					}
				}

				else
				{
					points = submission.getTotalScore().doubleValue();
				}

				// if the mss policy is to use highest score
				if (assessment.getMultipleSubmissionSelectionPolicy() == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED)
				{
					// find the highest score recorded for this user and this assessment
					Float highestScore = getSubmissionHighestScore(submission.getAssessment().getId(), submission.getUserId());
					if ((highestScore != null) && (points.doubleValue() < highestScore.doubleValue()))
					{
						// if this submission's points is not highest, don't record this in GB
						return;
					}
				}

				// post it
				try
				{
					m_gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), submission.getUserId(), points);
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
		}
	}

	/**
	 * Add a single entry to the answer to reserve an id for this answer.
	 * 
	 * @param answer
	 *        The submission answer.
	 */
	protected void reserveAnswer(final SubmissionAnswerImpl answer)
	{
		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				reserveAnswerTx(answer);
			}
		}, "reserveAnswer: " + answer.getSubmission().getId());

		// collect the cached submission, before the event clears it
		SubmissionImpl recache = getCachedSubmission(answer.getSubmission().getId());

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.SUBMISSION_ANSWER, getSubmissionReference(answer.getSubmission()
				.getId())
				+ ":" + answer.getQuestion().getId(), true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(answer.getSubmission().getId());

		// recache (this object used to be in the cache but is no longer, so we are the only owner)
		if (recache != null)
		{
			// if the cached submission has had its answers read, we will update it and re-cache
			if (recache.isAnswersInited())
			{
				// This new answer for it's question id should replace an existing on in the submission, or, be added to the answers
				SubmissionAnswerImpl old = recache.findAnswer(answer.getQuestion().getId());
				if (old != null)
				{
					recache.answers.remove(old);
				}
				recache.answers.add(answer);
			}

			// cache the object
			cacheSubmission(recache);
		}
	}

	/**
	 * Transaction code for reserveAnswer.
	 * 
	 * @param answer
	 *        The submission answer.
	 */
	protected void reserveAnswerTx(SubmissionAnswerImpl answer)
	{
		Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

		// this will score the answer based on values in the database
		String statement = "INSERT INTO SAM_ITEMGRADING_T"
				+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
				+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE, REVIEW" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?," + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
				// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
				+ ((answerId == null) ? "" : ",?") + ")";
		Object[] fields = new Object[(answerId == null) ? 10 : 11];
		fields[0] = Integer.valueOf(answer.getSubmission().getId());
		fields[1] = Integer.valueOf(answer.getQuestion().getId());
		fields[2] = Integer.valueOf(answer.getQuestion().getPart().getId());
		fields[3] = answer.getSubmission().getUserId();
		fields[4] = answer.getSubmittedDate();
		fields[5] = null;
		fields[6] = answer.getRationale();
		fields[7] = null;
		fields[8] = new Float(0);
		fields[9] = new Float(0);

		if (answerId != null)
		{
			fields[10] = answerId;
			if (!m_sqlService.dbWrite(statement, fields))
			{
				// TODO: better exception
				throw new RuntimeException("reserveAnswer: dbWrite Failed");
			}
		}
		else
		{
			answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
			if (answerId == null)
			{
				// TODO: better exception
				throw new RuntimeException("reserveAnswer: dbInsert Failed");
			}
		}

		// set the id into the answer
		if (answerId == null) throw new RuntimeException("failed to insert submission answer");
		answer.id = answerId.toString();
	}

	/**
	 * Score a submission answer.
	 * 
	 * @param answer
	 *        The answer to score.
	 */
	protected void scoreAnswer(SubmissionAnswerImpl answer)
	{
		AssessmentQuestion question = answer.getQuestion();
		// types that score: fillIn, matching, multipleChoice, multipleCorrect, numeric, trueFalse

		// first, score each entry
		int pos = 1;
		for (SubmissionAnswerEntryImpl entry : answer.entries)
		{
			// score the entry.
			float score = 0;
			if (question.getPoints() > 0)
			{
				// trueFalse scoring
				if (question.getType() == QuestionType.trueFalse)
				{
					// question score if correct, 0 if not
					if (entry.getIsCorrect().booleanValue())
					{
						score = question.getPoints();
					}
				}

				// multipleChoice scoring
				else if (question.getType() == QuestionType.multipleChoice)
				{
					// question score if correct, 0 if not
					if (entry.getIsCorrect().booleanValue())
					{
						score = question.getPoints();
					}
				}

				// multipleAnswer scoring
				else if (question.getType() == QuestionType.multipleCorrect)
				{
					// give a % (score / # answers marked correct) for a correct answer, or negative that for incorrect
					float correctScore = 0;
					float incorrectScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
						incorrectScore = -1 * correctScore;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
					else
					{
						score = incorrectScore;
					}
				}

				// fillIn scoring
				else if (question.getType() == QuestionType.fillIn)
				{
					// give a % (score / # answers marked correct) for each correct entry, 0 for incorrect
					float correctScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}

				// numeric scoring
				else if (question.getType() == QuestionType.numeric)
				{
					// give a % (score / # answers marked correct) for each correct entry, 0 for incorrect
					float correctScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}

				// matching scoring
				else if (question.getType() == QuestionType.matching)
				{
					// give a % (score / # parts) for each correct entry, 0 for incorrect
					float correctScore = 0;
					int numParts = question.getParts().size();

					if (numParts > 0)
					{
						correctScore = question.getPoints() / numParts;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}
			}

			// set it
			entry.initAutoScore(score);

			pos++;
		}

		// if the answer now has a negative auto-score, adjust to 0
		while (answer.countAutoScore() < 0)
		{
			boolean found = false;
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				// remove a negative scoring
				if (entry.getAutoScore() < 0)
				{
					entry.initAutoScore(new Float(0));
					found = true;
					break;
				}
			}

			if (!found)
			{
				// trouble: no more entries to clear, still negative
				M_log.warn("scoreAnswers: ran out of entries to clear to get to 0 score: submissionId: " + answer.getSubmission().getId()
						+ " questionId: " + question.getId());
				break;
			}
		}

		// mutually exclusive check
		if ((question.getType() == QuestionType.fillIn) && (question.getMutuallyExclusive().booleanValue()))
		{
			// check all but the last entry, looking down the list for a match
			// if this answer matches any following answer, and their question text also matches, this answer gets zero'ed out
			for (int i = 0; i < answer.entries.size() - 1; i++)
			{
				SubmissionAnswerEntryImpl entry = answer.entries.get(i);

				// if this entry is already 0, skip it
				if (entry.getAutoScore() > 0)
				{
					// this is the question text that must match some entry-down-below's question text
					String entryQuestionText = entry.getAssessmentAnswer().getText();

					// look down the list
					for (int j = i + 1; j < answer.entries.size(); j++)
					{
						// compare to this entry
						SubmissionAnswerEntryImpl compareEntry = answer.entries.get(j);

						// they need to be the same (i.e. !different) based on our case sensitive (the method takes ignore case, so
						// we reverse)
						if (!StringUtil.different(entry.getAnswerText(), compareEntry.getAnswerText(), !question.getCaseSensitive().booleanValue()))
						{
							// we will check against this other question's text, exactly
							String compareEntryQuestionText = compareEntry.getAssessmentAnswer().getText();
							if (entryQuestionText.equals(compareEntryQuestionText))
							{
								// we have a later match, so we zero score the entry
								entry.initAutoScore(new Float(0));

								// stop checking this entry
								break;
							}
						}
					}
				}
			}
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
	protected List<Submission> sortByStatus(GetUserContextSubmissionsSort sort, List<Submission> submissions)
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
		if (sort == GetUserContextSubmissionsSort.status_d)
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
		m_threadStop = false;

		m_thread = new Thread(this, getClass().getName());
		m_thread.start();
	}

	/**
	 * Stop the clean and report thread.
	 */
	protected void stop()
	{
		if (m_thread == null) return;

		// signal the thread to stop
		m_threadStop = true;

		// wake up the thread
		m_thread.interrupt();

		m_thread = null;
	}

	/**
	 * Transaction code for submitAnswers.
	 */
	protected void submitAnswersTx(List<SubmissionAnswer> answers, Submission submission, Boolean completSubmission)
	{
		for (SubmissionAnswer answer : answers)
		{
			// if not changed, skip it
			if (!answer.getIsChanged().booleanValue()) continue;

			// unwrap file upload attachments from multiple entries to just one
			// TODO: do this when save submission also
			List<SubmissionAnswerEntryImpl> entries = ((SubmissionAnswerImpl) answer).entries;
			if (answer.getQuestion().getType() == QuestionType.fileUpload)
			{
				SubmissionAnswerEntryImpl sample = entries.get(0);
				entries = new ArrayList<SubmissionAnswerEntryImpl>(1);
				SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl(sample);
				entry.setAnswerText(null);
				entry.setAssessmentAnswer(null);

				// set the entry id to the id we saved in the answer
				entry.id = ((SubmissionAnswerImpl) answer).id;
				entries.add(entry);
			}

			// create submission answer record(s) if needed
			for (SubmissionAnswerEntryImpl entry : entries)
			{
				if (entry.getId() == null)
				{
					Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

					// this will score the answer based on values in the database
					String statement = "INSERT INTO SAM_ITEMGRADING_T"
							+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
							+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE, REVIEW" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?," + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
							// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ ((answerId == null) ? "" : ",?") + ")";
					Object[] fields = new Object[(answerId == null) ? 10 : 11];
					fields[0] = Integer.valueOf(submission.getId());
					fields[1] = Integer.valueOf(answer.getQuestion().getId());
					fields[2] = Integer.valueOf(entry.getQuestionPart().getId());
					fields[3] = submission.getUserId();
					fields[4] = answer.getSubmittedDate();
					fields[5] = (entry.getAssessmentAnswer() == null) ? null : Integer.valueOf(entry.getAssessmentAnswer().getId());
					fields[6] = answer.getRationale();
					fields[7] = entry.getAnswerText();
					fields[8] = entry.getAutoScore();
					fields[9] = new Float(0);

					if (answerId != null)
					{
						fields[10] = answerId;
						if (!m_sqlService.dbWrite(statement, fields))
						{
							// TODO: better exception
							throw new RuntimeException("submitAnswer: dbWrite Failed");
						}
					}
					else
					{
						answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
						if (answerId == null)
						{
							// TODO: better exception
							throw new RuntimeException("submitAnswers: dbInsert Failed");
						}
					}

					// set the id into the answer
					if (answerId == null) throw new RuntimeException("failed to insert submission answer");
					entry.initId(answerId.toString());
					if (((SubmissionAnswerImpl) answer).id == null) ((SubmissionAnswerImpl) answer).id = answerId.toString();
				}

				// otherwise update the submission answer record
				else
				{
					String statement = "UPDATE SAM_ITEMGRADING_T"
							+ " SET SUBMITTEDDATE = ?, PUBLISHEDANSWERID = ?, PUBLISHEDITEMTEXTID = ?, RATIONALE = ?, ANSWERTEXT = ?, AUTOSCORE = ?,"
							+ " REVIEW = " + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
							// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ " WHERE ITEMGRADINGID = ?";
					// TODO: for added security, add to WHERE: AND ASSESSMENTGRADINGID = ?answer.getSubmissionId() AND
					// PUBLISHEDITEMID = ?answer.getQuestionId() -ggolden
					Object[] fields = new Object[7];
					fields[0] = answer.getSubmittedDate();
					fields[1] = (entry.getAssessmentAnswer() == null) ? null : Integer.valueOf(entry.getAssessmentAnswer().getId());
					fields[2] = Integer.valueOf(entry.getQuestionPart().getId());
					fields[3] = answer.getRationale();
					fields[4] = entry.getAnswerText();
					fields[5] = entry.getAutoScore();
					fields[6] = Integer.valueOf(entry.getId());

					if (!m_sqlService.dbWrite(statement, fields))
					{
						// TODO: better exception
						throw new RuntimeException("submitAnswers: dbWrite Failed");
					}
				}
			}

			// for any entries unused that have an id, delete them
			for (SubmissionAnswerEntryImpl entry : ((SubmissionAnswerImpl) answer).recycle)
			{
				if (entry.getId() != null)
				{
					String statement = "DELETE FROM SAM_ITEMGRADING_T WHERE ITEMGRADINGID = ?";
					Object[] fields = new Object[1];
					fields[0] = Integer.valueOf(entry.getId());
					if (!m_sqlService.dbWrite(statement, fields))
					{
						// TODO: better exception
						throw new RuntimeException("submitAnswers: dbWrite Failed");
					}
				}
			}

			// clear the unused now we have deleted what we must
			((SubmissionAnswerImpl) answer).recycle.clear();
		}

		// if complete, update the STATUS to 1 and the FORGRADE to TRUE... always update the date
		// Note: for Samigo compat., we need to update the scores in the SAM_ASSESSMENTGRADING_T based on the sums of the item
		// scores
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T"
				+ " SET SUBMITTEDDATE = ?,"
				+ " TOTALAUTOSCORE = (SELECT SUM(AUTOSCORE)+SUM(OVERRIDESCORE) FROM SAM_ITEMGRADING_T WHERE ASSESSMENTGRADINGID = ?),"
				+ " FINALSCORE = TOTALAUTOSCORE+TOTALOVERRIDESCORE"
				+ (((completSubmission != null) && completSubmission.booleanValue()) ? (" ,STATUS = 1, FORGRADE = " + m_sqlService
						.getBooleanConstant(true)) : "") + " WHERE ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[3];
		fields[0] = submission.getSubmittedDate();
		fields[1] = Integer.valueOf(submission.getId());
		fields[2] = Integer.valueOf(submission.getId());
		if (!m_sqlService.dbWrite(statement, fields))
		{
			// TODO: better exception
			throw new RuntimeException("submitAnswers: dbWrite Failed");
		}
	}

	/**
	 * Clear this submission from the cache.
	 * 
	 * @param id
	 *        The submission id.
	 */
	protected void unCacheSubmission(String id)
	{
		String ref = getSubmissionReference(id);

		// Note: the cache will clear when the event is processed...
		// if (m_submissionCache != null) m_submissionCache.remove(ref);

		// clear the thread-local cache
		m_threadLocalManager.set(ref, null);
	}
}
