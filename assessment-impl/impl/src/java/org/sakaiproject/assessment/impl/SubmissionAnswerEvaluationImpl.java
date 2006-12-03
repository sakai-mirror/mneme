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

import org.sakaiproject.assessment.api.SubmissionAnswerEvaluation;
import org.sakaiproject.assessment.api.SubmissionEvaluation;

/**
 * SubmissionAnswerEvaluationImpl is ...
 */
public class SubmissionAnswerEvaluationImpl implements SubmissionAnswerEvaluation
{
	protected String comments = null;

	protected Float manualScore = new Float(0);

	protected String submissionAnswerId = null;

	/** Back pointer to the evaluation this is part of. */
	protected transient SubmissionEvaluationImpl submissionEvaluation = null;

	/**
	 * Establish the back pointer to the submission evaluation of which this is a part.
	 * 
	 * @param submissionEvaluation
	 *        The back pointer to the submission evaluation of which this is a part.
	 */
	protected void initSubmissionEvaluation(SubmissionEvaluationImpl submissionEvaluation)
	{
		this.submissionEvaluation = submissionEvaluation;
	}

	/**
	 * Construct
	 */
	public SubmissionAnswerEvaluationImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public SubmissionAnswerEvaluationImpl(SubmissionAnswerEvaluationImpl other)
	{
		setComments(other.getComments());
		setManualScore(other.getManualScore());
		setSubmissionAnswerId(other.getSubmissionAnswerId());
		this.submissionEvaluation = other.submissionEvaluation;
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEvaluation)) throw new ClassCastException();

		// TODO: no natural ordering?
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEvaluation)) return false;
		if (((SubmissionAnswerEvaluationImpl) obj).submissionEvaluation != this.submissionEvaluation) return false;
		if (((SubmissionAnswerEvaluationImpl) obj).submissionAnswerId != this.submissionAnswerId) return false;
		return true;
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
	public Float getManualScore()
	{
		return this.manualScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSubmissionAnswerId()
	{
		return this.submissionAnswerId;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionEvaluation getSubmissionEvaluation()
	{
		return this.submissionEvaluation;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.submissionAnswerId.hashCode();
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
	public void setSubmissionAnswerId(String submissionAnswerId)
	{
		this.submissionAnswerId = submissionAnswerId;
	}
}
