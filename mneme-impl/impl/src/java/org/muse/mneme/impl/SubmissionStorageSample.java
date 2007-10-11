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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService.FindAssessmentSubmissionsSort;
import org.muse.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.StringUtil;

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

	protected SecurityService securityService = null;

	protected SessionManager sessionManager = null;

	protected Map<String, SubmissionImpl> submissions = new HashMap<String, SubmissionImpl>();

	protected SubmissionServiceImpl submissionService = null;

	protected UserDirectoryService userDirectoryService = null;

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
	public List<SubmissionImpl> getAssessmentCompleteSubmissions(Assessment assessment)
	{
		// collect the submissions to this assessment
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getIsComplete() && submission.getAssessment().equals(assessment))
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentHasUnscoredSubmissions(Assessment assessment)
	{
		// check the submissions to this assessment
		for (SubmissionImpl submission : this.submissions.values())
		{
			// if any for this assessment are complete and not released, the assessment is not fully released
			if (submission.getAssessment().equals(assessment) && submission.getIsComplete())
			{
				for (Answer answer : submission.getAnswers())
				{
					if (answer.getTotalScore() == null)
					{
						return Boolean.TRUE;
					}
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentIsFullyReleased(Assessment assessment)
	{
		// check the submissions to this assessment
		for (SubmissionImpl submission : this.submissions.values())
		{
			// if any for this assessment are complete and not released, the assessment is not fully released
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
	public List<SubmissionImpl> getAssessmentSubmissions(Assessment assessment, final FindAssessmentSubmissionsSort sort, final Question question)
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
				SubmissionImpl s = newSubmission();
				s.initUserId(userId);
				s.initAssessmentIds(assessment.getId(), assessment.getId());
				rv.add(s);
			}
		}

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				switch (sort)
				{
					case userName_a:
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

						rv = id0.compareTo(id1);
						break;
					}
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

						rv = -1 * id0.compareTo(id1);
						break;
					}
					case final_a:
					{
						Float final0 = null;
						Float final1 = null;
						if (question != null)
						{
							final0 = ((Submission) arg0).getAnswer(question).getTotalScore();
							final1 = ((Submission) arg1).getAnswer(question).getTotalScore();
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
							break;
						}
						if (final0 == null)
						{
							rv = -1;
							break;
						}
						if (final1 == null)
						{
							rv = 1;
							break;
						}
						rv = final0.compareTo(final1);
						break;
					}
					case final_d:
					{
						Float final0 = null;
						Float final1 = null;
						if (question != null)
						{
							final0 = ((Submission) arg0).getAnswer(question).getTotalScore();
							final1 = ((Submission) arg1).getAnswer(question).getTotalScore();
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
							break;
						}
						if (final0 == null)
						{
							rv = 1;
							break;
						}
						if (final1 == null)
						{
							rv = +1;
							break;
						}
						rv = -1 * final0.compareTo(final1);
						break;
					}
				}

				return rv;
			}
		});
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
		if (rv != null)
		{
			rv = new SubmissionImpl(rv);
		}

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
	public List<SubmissionImpl> getUserAssessmentSubmissions(Assessment assessment, String userId)
	{
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			// find those to this assessment for this user, filter out archived and un-published assessments
			if (submission.getAssessment().equals(assessment) && submission.getUserId().equals(userId) && (!submission.getAssessment().getArchived())
					&& (submission.getAssessment().getPublished()))
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		// if we didn't get one, invent one
		if (rv.isEmpty())
		{
			SubmissionImpl s = newSubmission();
			s.initUserId(userId);
			s.initAssessmentIds(assessment.getId(), assessment.getId());
			rv.add(s);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getUserContextSubmissions(String context, String userId, final GetUserContextSubmissionsSort sort)
	{
		List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		for (SubmissionImpl submission : this.submissions.values())
		{
			// find those in the context for this user, filter out archived and un-published assessments
			if (submission.getAssessment().getContext().equals(context) && submission.getUserId().equals(userId)
					&& (!submission.getAssessment().getArchived()) && (submission.getAssessment().getPublished()))
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
				s.initAssessmentIds(a.getId(), a.getId());
				rv.add(s);
			}
		}

		// sort
		// status sorts first by due date descending, then status final sorting is done in the service
		Collections.sort(rv, new Comparator()
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
						rv = s0.compareTo(s1);
						break;
					}
					case title_d:
					{
						String s0 = StringUtil.trimToZero(((Submission) arg0).getAssessment().getTitle());
						String s1 = StringUtil.trimToZero(((Submission) arg1).getAssessment().getTitle());
						rv = -1 * s0.compareTo(s1);
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
	 * {@inheritDoc}
	 */
	public Boolean historicalDependencyExists(Assessment assessment)
	{
		for (SubmissionImpl submission : this.submissions.values())
		{
			SubmissionAssessmentImpl subAsmnt = (SubmissionAssessmentImpl) submission.getAssessment();
			if (subAsmnt.historicalAssessmentId.equals(assessment.getId()))
			{
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
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
		return new AnswerImpl(mnemeService);
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
	public void saveAnswersEvaluation(List<Answer> answers)
	{
		for (Answer a : answers)
		{
			// find the submission
			SubmissionImpl s = this.submissions.get(a.getSubmission().getId());
			if (s != null)
			{
				AnswerImpl oldAnswer = (AnswerImpl) s.getAnswer(a.getQuestion());
				if (oldAnswer != null)
				{
					oldAnswer.evaluation.set(((AnswerImpl) a).evaluation);
				}
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
	public void saveSubmissionEvaluation(SubmissionImpl submission)
	{
		// has to be an existing saved submission
		if (submission.getId() == null) throw new IllegalArgumentException();

		// we must already have the submission
		SubmissionImpl old = this.submissions.get(submission.getId());
		if (old == null) throw new IllegalArgumentException();

		// update the submission evaluation
		old.evaluation.set(submission.evaluation);

		// update the answer evaluations
		for (Answer answer : submission.getAnswers())
		{
			AnswerImpl oldAnswer = (AnswerImpl) old.getAnswer(answer.getQuestion());
			if (oldAnswer != null)
			{
				oldAnswer.evaluation.set(((AnswerImpl) answer).evaluation);
			}
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
	public void setUserDirectoryService(UserDirectoryService service)
	{
		this.userDirectoryService = service;
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

	/**
	 * {@inheritDoc}
	 */
	public void switchHistoricalDependency(Assessment assessment, Assessment newAssessment)
	{
		for (SubmissionImpl submission : this.submissions.values())
		{
			SubmissionAssessmentImpl subAsmnt = (SubmissionAssessmentImpl) submission.getAssessment();
			if (subAsmnt.historicalAssessmentId.equals(assessment.getId()))
			{
				subAsmnt.historicalAssessmentId = newAssessment.getId();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchLiveDependency(Question from, Question to)
	{
		// for all submissions in the context
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().getContext().equals(from.getPool().getContext()))
			{
				// check the answers
				for (Answer answer : submission.getAnswers())
				{
					if (((AnswerImpl) answer).questionId.equals(from.getId()))
					{
						((AnswerImpl) answer).questionId = to.getId();
					}
				}
			}
		}
	}
}
