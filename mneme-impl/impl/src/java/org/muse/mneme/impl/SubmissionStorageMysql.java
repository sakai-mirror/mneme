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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.ReviewTiming;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.muse.mneme.api.SubmissionService.FindAssessmentSubmissionsSort;
import org.muse.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.StringUtil;

/**
 * SubmissionStorageMysql handles submission storage for mysql.
 */
public class SubmissionStorageMysql implements SubmissionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionStorageMysql.class);

	protected AssessmentService assessmentService = null;

	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;

	protected Object idGenerator = new Object();

	protected MnemeService mnemeService = null;

	protected long nextAnswerId = 100;

	protected long nextSubmissionId = 100;

	protected SecurityService securityService = null;

	protected SessionManager sessionManager = null;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	protected Map<String, SubmissionImpl> submissions = new LinkedHashMap<String, SubmissionImpl>();

	protected SubmissionServiceImpl submissionService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

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
	public List<Question> findPartQuestions(Part part)
	{
		List<Question> rv = new ArrayList<Question>();

		// check the submissions to this assessment
		for (SubmissionImpl submission : this.submissions.values())
		{
			// TODO: only for complete? && submission.getIsComplete()
			if (submission.getAssessment().equals(part.getAssessment()))
			{
				for (Answer answer : submission.getAnswers())
				{
					// find the answers based on the part from their original, main, non-historical assessment part.
					if (((AnswerImpl) answer).getOrigPartId().equals(part.getId()))
					{
						if (!rv.contains((QuestionImpl) answer.getQuestion()))
						{
							// copy and set the part context to the main part (might have been historical)
							QuestionImpl q = new QuestionImpl((QuestionImpl) answer.getQuestion());
							q.initPartContext(part);

							rv.add(q);
						}
					}
				}
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
	public Answer getAnswer(String answerId)
	{
		for (SubmissionImpl submission : this.submissions.values())
		{
			for (Answer answer : submission.getAnswers())
			{
				if (answer.getId().equals(answerId))
				{
					return answer;
				}
			}
		}

		return null;
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
					if ((answer.getIsAnswered()) && (answer.getTotalScore() == null))
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
	public Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question)
	{
		// check the submissions to this assessment
		for (SubmissionImpl submission : this.submissions.values())
		{
			// if any submissions that are for this assessment are complete and not released, the assessment is not fully released
			if (submission.getAssessment().equals(assessment) && submission.getIsComplete())
			{
				for (Answer answer : submission.getAnswers())
				{
					if ((answer.getQuestion().equals(question)) && (answer.getIsAnswered()) && (answer.getTotalScore() == null))
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

				// set the id so we know it is a phantom
				s.initId(SubmissionService.PHANTOM_PREFIX + assessment.getId() + "/" + userId);

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
						rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
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
							rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
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
							rv = ((Submission) arg0).getSubmittedDate().compareTo(((Submission) arg1).getSubmittedDate());
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
		// recognize phantom ids
		if (id.startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// split out the phantom id parts: [1] the aid, [2] the uid
			String[] idParts = StringUtil.split(id, "/");
			String aid = idParts[1];
			String userId = idParts[2];

			// create a phantom
			SubmissionImpl s = newSubmission();
			s.initUserId(userId);
			s.initAssessmentIds(aid, aid);
			s.initId(id);

			return s;
		}

		// for real submissions
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

			// set the id so we know it is a phantom
			s.initId(SubmissionService.PHANTOM_PREFIX + assessment.getId() + "/" + userId);

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

				// set the id so we know it is a phantom
				s.initId(SubmissionService.PHANTOM_PREFIX + a.getId() + "/" + userId);

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
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_submission");
		}

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

			// clear the evaluation changed
			((EvaluationImpl) a.getEvaluation()).clearIsChanged();

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

		else if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// clear the submission evaluation changed
		((EvaluationImpl) submission.getEvaluation()).clearIsChanged();

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
		if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// has to be an existing saved submission
		if (submission.getId() == null) throw new IllegalArgumentException();

		// we must already have the submission
		SubmissionImpl old = this.submissions.get(submission.getId());
		if (old == null) throw new IllegalArgumentException();

		// update the submission evaluation
		old.evaluation.set(submission.evaluation);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmissionReleased(SubmissionImpl submission)
	{
		if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// has to be an existing saved submission
		if (submission.getId() == null) throw new IllegalArgumentException();

		// we must already have the submission
		SubmissionImpl old = this.submissions.get(submission.getId());
		if (old == null) throw new IllegalArgumentException();

		// update the submission evaluation
		old.released = submission.released;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		autoDdl = new Boolean(value).booleanValue();
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
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionService(SubmissionServiceImpl service)
	{
		this.submissionService = service;
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
	public Boolean submissionsDependsOn(Question question)
	{
		// for all submissions in the context
		for (SubmissionImpl submission : this.submissions.values())
		{
			if (submission.getAssessment().getContext().equals(question.getContext()))
			{
				// check the answers
				for (Answer answer : submission.getAnswers())
				{
					if (((AnswerImpl) answer).questionId.equals(question.getId()))
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
		// map the old part ids to the new
		Map<String, String> partIdMap = new HashMap<String, String>();
		for (int i = 0; i < assessment.getParts().getParts().size(); i++)
		{
			partIdMap.put(assessment.getParts().getParts().get(i).getId(), newAssessment.getParts().getParts().get(i).getId());
		}

		for (SubmissionImpl submission : this.submissions.values())
		{
			SubmissionAssessmentImpl subAsmnt = (SubmissionAssessmentImpl) submission.getAssessment();
			if (subAsmnt.historicalAssessmentId.equals(assessment.getId()))
			{
				subAsmnt.historicalAssessmentId = newAssessment.getId();

				// switch all answer part ids in submission to newAssessment's new part ids
				for (Answer answer : submission.getAnswers())
				{
					((AnswerImpl) answer).initPartId(partIdMap.get(((AnswerImpl) answer).getPartId()));
				}
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
			if (submission.getAssessment().getContext().equals(from.getContext()))
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

	/**
	 * Insert a new submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void insertSubmission(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertSubmissionTx(submission);
			}
		}, "insertSubmission: " + submission.getId());
	}

	/**
	 * Insert a new submission (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void insertSubmissionTx(SubmissionImpl submission)
	{
		// new submissions have no answers yet

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_SUBMISSION (");
		sql.append(" ASSESSMENT_ID, ASSESSMENT_HID, COMPLETE, CONTEXT, EVAL_ATRIB_DATE, EVAL_ATRIB_USER,");
		sql.append(" EVAL_COMMENT, EVAL_EVALUATED, EVAL_SCORE, RELEASED, START_DATE, SUBMITTED_DATE, USER )");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[13];
		fields[0] = submission.getAssessment().getId();
		fields[1] = submission.getAssessment().getId();
		fields[2] = submission.getIsComplete() ? "1" : "0";
		fields[3] = submission.getAssessment().getContext();
		fields[4] = (submission.getEvaluation().getAttribution().getDate() == null) ? null : submission.getEvaluation().getAttribution().getDate()
				.getTime();
		fields[5] = submission.getEvaluation().getAttribution().getUserId();
		fields[6] = submission.getEvaluation().getComment();
		fields[7] = submission.getEvaluation().getEvaluated() ? "1" : "0";
		fields[8] = submission.getEvaluation().getScore() == null ? null : Float.valueOf(submission.getEvaluation().getScore());
		fields[9] = submission.getIsReleased() ? "1" : "0";
		fields[10] = (submission.getStartDate() == null) ? null : submission.getStartDate().getTime();
		fields[11] = (submission.getSubmittedDate() == null) ? null : submission.getSubmittedDate().getTime();
		fields[12] = submission.getUserId();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertSubmissionTx: dbInsert failed");
		}

		// set the submission's id
		submission.initId(id.toString());
	}

	/**
	 * Read an submission
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission.
	 */
	protected SubmissionImpl readSubmission(String id)
	{
		String where = "WHERE S.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List<SubmissionImpl> rv = readSubmissions(where, null, fields);
		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Read a selection of submissions.
	 * 
	 * @param where
	 *        The where clause
	 * @param order
	 *        The order clause
	 * @param fields
	 *        The bind variables.
	 * @return The submissions.
	 */
	protected List<SubmissionImpl> readSubmissions(String where, String order, Object[] fields)
	{
		final List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		final Map<String, SubmissionImpl> submissions = new HashMap<String, SubmissionImpl>();

		// submissions
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.ASSESSMENT_ID, S.ASSESSMENT_HID, S.COMPLETE, S.CONTEXT, S.EVAL_ATRIB_DATE,");
		sql.append(" S.EVAL_ATRIB_USER, S.EVAL_COMMENT, S.EVAL_EVALUATED, S.EVAL_SCORE,");
		sql.append(" S.ID, S.RELEASED, S.START_DATE, S.SUBMITTED_DATE, S.USER");
		sql.append(" FROM MNEME_SUBMISSION S ");
		sql.append(where);
		if (order != null) sql.append(order);

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					int i = 1;
					SubmissionImpl submission = newSubmission();
					submission.initAssessmentIds(SqlHelper.readString(result, i++), SqlHelper.readString(result, i++));
					submission.setIsComplete(SqlHelper.readBoolean(result, i++));
					submission.getEvaluation().getAttribution().setDate(SqlHelper.readDate(result, i++));
					submission.getEvaluation().getAttribution().setUserId(SqlHelper.readString(result, i++));
					submission.getEvaluation().setComment(SqlHelper.readString(result, i++));
					submission.getEvaluation().setEvaluated(SqlHelper.readBoolean(result, i++));
					submission.getEvaluation().setScore(SqlHelper.readFloat(result, i++));
					submission.initId(SqlHelper.readId(result, i++));
					submission.setIsReleased(SqlHelper.readBoolean(result, i++));
					submission.setStartDate(SqlHelper.readDate(result, i++));
					submission.setSubmittedDate(SqlHelper.readDate(result, i++));
					submission.initUserId(SqlHelper.readString(result, i++));

					rv.add(submission);
					submissions.put(submission.getId(), submission);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissions(submission): " + e);
					return null;
				}
			}
		});

		// read all the answers for these submissions
		sql = new StringBuilder();
		sql.append("SELECT A.GUEST, A.EVAL_ATRIB_DATE, A.EVAL_ATRIB_USER, A.EVAL_COMMENT, A.EVAL_EVALUATED,");
		sql.append(" A.EVAL_SCORE, A.ID, A.ORIG_PID, A.PART_ID, A.QUESTION_ID, A.QUESTION_TYPE, A.REASON, A.REVIEW,");
		sql.append(" A.SUBMISSION_ID, A.SUBMITTED_DATE");
		sql.append(" FROM MNEME_ANSWER A");
		sql.append(" JOIN MNEME_SUBMISSION S ON A.SUBMISSION_ID=S.ID ");
		sql.append(where);
		sql.append(" ORDER BY A.SUBMISSION_ID ASC");

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String sid = SqlHelper.readId(result, 14);
					SubmissionImpl s = submissions.get(sid);
					AnswerImpl a = newAnswer();

					a.getEvaluation().getAttribution().setDate(SqlHelper.readDate(result, 2));
					a.getEvaluation().getAttribution().setUserId(SqlHelper.readString(result, 3));
					a.getEvaluation().setComment(SqlHelper.readString(result, 4));
					a.getEvaluation().setEvaluated(SqlHelper.readBoolean(result, 5));
					a.getEvaluation().setScore(SqlHelper.readFloat(result, 6));
					a.initId(SqlHelper.readId(result, 7));
					a.initPartIds(SqlHelper.readId(result, 9), SqlHelper.readId(result, 8));
					a.initQuestion(SqlHelper.readId(result, 10), SqlHelper.readString(result, 11));
					// TODO: guest SqlHelper.readString(result, 1)
					a.setReason(SqlHelper.readString(result, 12));
					a.setMarkedForReview(SqlHelper.readBoolean(result, 13));
					a.setSubmittedDate(SqlHelper.readDate(result, 14));

					a.clearIsChanged();
					s.initAnswer(a);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissions(answers): " + e);
					return null;
				}
			}
		});

		// clear changed for the submissions
		for (SubmissionImpl s : rv)
		{
			s.clearIsChanged();
		}

		return rv;
	}

	/**
	 * Update an existing submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmission(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateSubmissionTx(submission);
			}
		}, "updateSubmission: " + submission.getId());
	}

	/**
	 * Update an existing submission (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionTx(SubmissionImpl submission)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_SUBMISSION SET");
		sql.append(" COMPLETE=?, EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?,");
		sql.append(" EVAL_SCORE=?, RELEASED=?, SUBMITTED_DATE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[9];
		fields[0] = submission.getIsComplete() ? "1" : "0";
		fields[1] = (submission.getEvaluation().getAttribution().getDate() == null) ? null : submission.getEvaluation().getAttribution().getDate()
				.getTime();
		fields[2] = submission.getEvaluation().getAttribution().getUserId();
		fields[3] = submission.getEvaluation().getComment();
		fields[4] = submission.getEvaluation().getEvaluated() ? "1" : "0";
		fields[5] = submission.getEvaluation().getScore() == null ? null : Float.valueOf(submission.getEvaluation().getScore());
		fields[6] = submission.getIsReleased() ? "1" : "0";
		fields[7] = (submission.getSubmittedDate() == null) ? null : submission.getSubmittedDate().getTime();
		fields[8] = Long.valueOf(submission.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateSubmissionTx: db write failed");
		}
	}

	/**
	 * Update an existing submission answer.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswer(final AnswerImpl answer)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateAnswerTx(answer);
			}
		}, "updateAnswer: " + answer.getId());
	}

	/**
	 * Update an existing submission answer (transaction code).
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswerTx(AnswerImpl answer)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ANSWER SET");
		sql.append(" GUEST=?, EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?,");
		sql.append(" EVAL_SCORE=?, REASON=?, REVIEW=?, SUBMITTED_DATE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[10];
		fields[0] = null;// TODO: guest
		fields[1] = (answer.getEvaluation().getAttribution().getDate() == null) ? null : answer.getEvaluation().getAttribution().getDate().getTime();
		fields[2] = answer.getEvaluation().getAttribution().getUserId();
		fields[3] = answer.getEvaluation().getComment();
		fields[4] = answer.getEvaluation().getEvaluated() ? "1" : "0";
		fields[5] = answer.getEvaluation().getScore() == null ? null : Float.valueOf(answer.getEvaluation().getScore());
		fields[6] = answer.getReason();
		fields[7] = answer.getMarkedForReview() ? "1" : "0";
		fields[8] = (answer.getSubmittedDate() == null) ? null : answer.getSubmittedDate().getTime();
		fields[9] = Long.valueOf(answer.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateAnswerTx: db write failed");
		}
	}

	/**
	 * Insert a new answer.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void inserAnswer(final AnswerImpl answer)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertAnswerTx(answer);
			}
		}, "inserAnswer: " + answer.getId());
	}

	/**
	 * Insert a new pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertAnswerTx(AnswerImpl answer)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ANSWER (");
		sql.append(" GUEST, EVAL_ATRIB_DATE, EVAL_ATRIB_USER, EVAL_COMMENT, EVAL_EVALUATED, EVAL_SCORE,");
		sql.append(" ORIG_PID, PART_ID, QUESTION_ID, QUESTION_TYPE, REASON, REVIEW, SUBMISSION_ID, SUBMITTED_DATE)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[14];
		fields[0] = null;// TODO: guest
		fields[1] = (answer.getEvaluation().getAttribution().getDate() == null) ? null : answer.getEvaluation().getAttribution().getDate().getTime();
		fields[2] = answer.getEvaluation().getAttribution().getUserId();
		fields[3] = answer.getEvaluation().getComment();
		fields[4] = answer.getEvaluation().getEvaluated() ? "1" : "0";
		fields[5] = answer.getEvaluation().getScore() == null ? null : Float.valueOf(answer.getEvaluation().getScore());
		fields[6] = Long.valueOf(answer.getOrigPartId());
		fields[7] = Long.valueOf(answer.getPartId());
		Question q = answer.getQuestion();
		fields[8] = Long.valueOf(q.getId());
		fields[9] = q.getType();
		fields[10] = answer.getReason();
		fields[11] = answer.getMarkedForReview() ? "1" : "0";
		fields[12] = Long.valueOf(answer.getSubmission().getId());
		fields[13] = (answer.getSubmittedDate() == null) ? null : answer.getSubmittedDate().getTime();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertPoolTx: dbInsert failed");
		}

		// set the answer's id
		answer.initId(id.toString());
	}

}
