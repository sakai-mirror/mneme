/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.SubmissionAnswerEvaluation;
import org.sakaiproject.assessment.api.SubmissionEvaluation;

/**
 * SubmissionAnswerEvaluationImpl is ...
 */
public class SubmissionEvaluationImpl implements SubmissionEvaluation
{
	protected List<SubmissionAnswerEvaluationImpl> answerEvaluations = new ArrayList<SubmissionAnswerEvaluationImpl>();

	protected String comments = null;

	protected String id = null;

	protected Float manualScore = new Float(0);

	protected String submissionId = null;

	/**
	 * Construct
	 */
	public SubmissionEvaluationImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public SubmissionEvaluationImpl(SubmissionEvaluationImpl other)
	{
		setAnswerEvaluations(other.answerEvaluations);
		setComments(other.getComments());
		initId(other.getId());
		setManualScore(other.getManualScore());
		setSubmissionId(other.getSubmissionId());
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof SubmissionEvaluation)) throw new ClassCastException();

		// TODO: no natural ordering?
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SubmissionEvaluation)) return false;
		if (this.getId() == null) return false;
		if (((SubmissionEvaluation) obj).getId() == null) return false;
		if (((SubmissionEvaluation) obj).getId() != this.getId()) return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionAnswerEvaluation> getAnswerEvaluations()
	{
		// deep copy
		List<SubmissionAnswerEvaluation> rv = new ArrayList<SubmissionAnswerEvaluation>(this.answerEvaluations.size());

		for (SubmissionAnswerEvaluationImpl answerEvaluation : this.answerEvaluations)
		{
			rv.add(new SubmissionAnswerEvaluationImpl(answerEvaluation));
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAnswersManualScore()
	{
		float score = 0;
		for (SubmissionAnswerEvaluationImpl answerEvaluation : this.answerEvaluations)
		{
			score += answerEvaluation.getManualScore();
		}

		return new Float(score);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getComments()
	{
		return this.comments;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getManualScore()
	{
		return this.manualScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getSubmission()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSubmissionId()
	{
		return this.submissionId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalManualScore()
	{
		float score = getAnswersManualScore().floatValue();
		score += getManualScore().floatValue();

		return new Float(score);
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.getId().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnswerEvaluations(List<? extends SubmissionAnswerEvaluation> answers)
	{
		this.answerEvaluations.clear();

		for (SubmissionAnswerEvaluation answerEvaluation : answers)
		{
			SubmissionAnswerEvaluationImpl copy = new SubmissionAnswerEvaluationImpl(
					(SubmissionAnswerEvaluationImpl) answerEvaluation);
			this.answerEvaluations.add(copy);
			copy.initSubmissionEvaluation(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setComments(String comments)
	{
		this.comments = comments;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setManualScore(Float adjustment)
	{
		this.manualScore = adjustment == null ? new Float(0) : adjustment;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionId(String submissionId)
	{
		this.submissionId = submissionId;
	}

	/**
	 * Establish the answer evaluations to this (no deep copy).
	 * 
	 * @param answers
	 *        The answer evaluations for this submission evaluation.
	 */
	protected void initAnswerEvaluations(List<SubmissionAnswerEvaluationImpl> answers)
	{
		this.answerEvaluations = answers;

		for (SubmissionAnswerEvaluationImpl answerEvaluation : answers)
		{
			answerEvaluation.initSubmissionEvaluation(this);
		}
	}

	/**
	 * Establish the submission evaluation id.
	 * 
	 * @param id
	 *        The submission evaluation id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}
}
