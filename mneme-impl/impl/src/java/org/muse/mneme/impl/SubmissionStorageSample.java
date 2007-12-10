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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.muse.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.tool.api.SessionManager;
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

	protected Map<String, SubmissionImpl> submissions = new LinkedHashMap<String, SubmissionImpl>();

	protected SubmissionServiceImpl submissionService = null;

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
	public List<String> findPartQuestions(Part part)
	{
		List<String> rv = new ArrayList<String>();

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
						if (!rv.contains(answer.getQuestion().getId()))
						{
							rv.add(answer.getQuestion().getId());
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
	public List<SubmissionImpl> getAssessmentSubmissions(Assessment assessment)
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
			// find those to this assessment for this user
			if (submission.getAssessment().equals(assessment) && submission.getUserId().equals(userId))
			{
				rv.add(new SubmissionImpl(submission));
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getUserContextSubmissions(String context, String userId)
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
	public SubmissionImpl newSubmission(SubmissionImpl other)
	{
		return new SubmissionImpl(other);
	}

	// /**
	// * {@inheritDoc}
	// */
	// public void removeIncompleteAssessmentSubmissions(Assessment assessment)
	// {
	// for (Iterator i = this.submissions.values().iterator(); i.hasNext();)
	// {
	// SubmissionImpl submission = (SubmissionImpl) i.next();
	// if (submission.getAssessment().equals(assessment) && (!submission.getIsComplete()))
	// {
	// i.remove();
	// }
	// }
	// }

	// /**
	// * {@inheritDoc}
	// */
	// public void removeSubmission(SubmissionImpl submission)
	// {
	// this.submissions.remove(submission.getId());
	// }

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

			// clear the evaluation changed
			((EvaluationImpl) a.getEvaluation()).clearIsChanged();
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

}
