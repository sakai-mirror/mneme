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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCounts;
import org.muse.mneme.api.SubmissionService.FindAssessmentSubmissionsSort;
import org.muse.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.tool.api.SessionManager;

/**
 * SubmissionStorageSample defines sample storage for Submissions.
 */
public class SubmissionStorageSample implements SubmissionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionStorageSample.class);

	protected AssessmentService assessmentService = null;

	protected Object idGenerator = new Object();

	protected MnemeService mnemeService = null;

	protected long nextAnswerId = 100;

	protected long nextSubmissionId = 100;

	protected QuestionService questionService = null;

	protected SecurityService securityService = null;

	protected SessionManager sessionManager = null;

	protected Map<String, SubmissionImpl> submissions = new HashMap<String, SubmissionImpl>();

	protected SubmissionServiceImpl submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Integer countCompleteSubmissions(Assessment assessment, String userId)
	{
		int i = 0;
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment) && submission.getUserId().equals(userId) && submission.getIsComplete())
			{
				i++;
			}
		}

		return i;
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
	public List<SubmissionImpl> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort)
	{
		// collect the submissions to this assessment
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment))
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		// TODO: get all possible users who can submit
		List<String> userIds = new ArrayList<String>();

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
				SubmissionImpl s = newSubmission();
				s.initUserId(userId);
				s.initAssessmentId(assessment.getId());
				rv.add(s);
			}
		}

		// TODO: sort
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentIsFullyReleased(Assessment assessment)
	{
		// check the submissions to this assessment
		for (SubmissionImpl submission : this.submissions.values())
		{
			// if any for this assessment are complete and not released, the assessment is not fully graded
			if (submission.getAssessment().equals(assessment) && submission.getIsComplete() && (!submission.getIsReleased()))
			{
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		List<Float> rv = new ArrayList<Float>();

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getOpenSubmissions()
	{
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (!submission.getIsComplete())
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		List<Float> rv = new ArrayList<Float>();

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl getSubmission(String id)
	{
		SubmissionImpl rv = this.submissions.get(id);
		if (rv == null)
		{
			rv = newSubmission();
			rv.initId(id);
		}
		else
		{
			rv = new SubmissionImpl(rv);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionCounts getSubmissionCounts(Assessment assessment)
	{
		SubmissionCounts rv = new SubmissionCountsImpl();

		int completed = 0;
		int released = 0;
		int inProgress = 0;
		int unReleased = 0;

		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment))
			{
				if (submission.getIsComplete()) completed++;
				if (submission.getIsComplete() && submission.getIsReleased()) released++;
				if (submission.getIsComplete() && (!submission.getIsReleased())) unReleased++;
				if ((!submission.getIsComplete()) && submission.getIsStarted()) inProgress++;
			}
		}

		rv.setCompleted(completed);
		rv.setReleased(released);
		rv.setInProgress(inProgress);
		rv.setUnreleased(unReleased);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getSubmissionHighestScore(Assessment assessment, String userId)
	{
		float rv = 0f;
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment) && submission.getUserId().equals(userId) && submission.getIsComplete()
					&& submission.getTotalScore() > rv)
			{
				rv = submission.getTotalScore();
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl getSubmissionInProgress(Assessment assessment, String userId)
	{
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment) && submission.getUserId().equals(userId) && !submission.getIsComplete())
			{
				return new SubmissionImpl(submission);
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getSubmissionScore(Submission submission)
	{
		SubmissionImpl s = getSubmission(submission.getId());
		if (s != null)
		{
			return s.getTotalScore();
		}

		return 0f;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort)
	{
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().getContext().equals(context) && submission.getUserId().equals(userId))
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		// get all the assessments for this context
		List<Assessment> assessments = this.assessmentService.getContextAssessments(context, AssessmentService.AssessmentsSort.title_a, Boolean.TRUE);

		// if any assessment is not represented in the submissions we found, add an empty submission for it
		for (Assessment a : assessments)
		{
			boolean found = false;
			for (Submission s : rv)
			{
				if (s.getAssessment().equals(a))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				SubmissionImpl s = newSubmission();
				s.initUserId(userId);
				s.initAssessmentId(a.getId());
				rv.add(s);
			}
		}

		// TODO: sort
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getUsersSubmitted(Assessment assessment)
	{
		List<String> rv = new ArrayList<String>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment) && !rv.contains(submission.getUserId()))
			{
				rv.add(submission.getUserId());
			}
		}

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public AnswerImpl newAnswer()
	{
		return new AnswerImpl(mnemeService, questionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl newSubmission()
	{
		return new SubmissionImpl(assessmentService, securityService, submissionService, sessionManager);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeIncompleteAssessmentSubmissions(Assessment assessment)
	{
		for (Iterator i = this.submissions.values().iterator(); i.hasNext();)
		{
			SubmissionImpl submission = (SubmissionImpl) i.next();
			if (submission.getAssessment().equals(assessment) && (!submission.getIsComplete()))
			{
				i.remove();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeSubmission(SubmissionImpl submission)
	{
		this.submissions.remove(submission.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAnswers(List<Answer> answers)
	{
		// for each answer, place it into the submission replacing the answer we have or adding
		for (Answer a : answers)
		{
			// if there is no id, assign one
			if (a.getId() == null)
			{
				long id = 0;
				synchronized (this.idGenerator)
				{
					id = this.nextAnswerId;
					this.nextAnswerId++;
				}
				((AnswerImpl) a).initId("n" + Long.toString(id));
			}

			// find the submission
			SubmissionImpl s = this.submissions.get(a.getSubmission().getId());
			if (s != null)
			{
				// replace or add the answer
				s.replaceAnswer((AnswerImpl) a);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmission(SubmissionImpl submission)
	{
		// assign an id
		if (submission.getId() == null)
		{
			long id = 0;
			synchronized (this.idGenerator)
			{
				id = this.nextSubmissionId;
				this.nextSubmissionId++;
			}
			submission.initId("s" + Long.toString(id));
		}

		// if we have this already, update ONLY the main information, not the answers
		SubmissionImpl old = this.submissions.get(submission.getId());
		if (old != null)
		{
			old.setMain(submission);
		}

		// otherwise save it w/ no answers
		else
		{
			SubmissionImpl s = new SubmissionImpl(submission);
			s.clearAnswers();
			this.submissions.put(submission.getId(), s);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMnemeService(MnemeService service)
	{
		this.mnemeService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionService(SubmissionServiceImpl service)
	{
		this.submissionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean submissionExists(String id)
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean submissionsExist(Assessment assessment)
	{
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().equals(assessment))
			{
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}
}
