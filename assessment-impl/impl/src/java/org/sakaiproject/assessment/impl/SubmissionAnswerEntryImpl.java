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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;

/**
 * SubmissionEntryImpl ...
 */
public class SubmissionAnswerEntryImpl implements SubmissionAnswerEntry
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionAnswerEntryImpl.class);

	/** Don't store the assessment answer here. */
	protected String answerId = null;

	protected String answerText = null;

	protected Float autoScore = null;

	/** To hold Samigo's db id. */
	protected String id = null;

	/** Back pointer to our submission answer. */
	protected transient SubmissionAnswerImpl submissionAnswer = null;

	/**
	 * Construct
	 */
	public SubmissionAnswerEntryImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public SubmissionAnswerEntryImpl(SubmissionAnswerEntryImpl other)
	{
		answerId = other.answerId;
		answerText = other.answerText;
		autoScore = other.autoScore;
		id = other.id;
		submissionAnswer = other.submissionAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEntry)) throw new ClassCastException();

		// no natrual comparison?
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEntry)) return false;
		if (this == obj) return true;
		if (((SubmissionAnswerEntryImpl) obj).submissionAnswer != this.submissionAnswer) return false;
		if (((SubmissionAnswerEntryImpl) obj).answerId != this.answerId) return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerText()
	{
		return this.answerText;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAnswer getAssessmentAnswer()
	{
		if (this.answerId == null) return null;

		return this.submissionAnswer.getQuestion().getAnswer(this.answerId);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAssessmentAnswerId()
	{
		return this.answerId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		if (this.autoScore == null) return new Float(0);

		return this.autoScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionAnswer getSubmissionAnswer()
	{
		return this.submissionAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return (this.submissionAnswer.questionId + this.answerId).hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnswerText(String text)
	{
		this.answerText = text;

		// clear the auto score
		this.autoScore = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAssessmentAnswer(AssessmentAnswer answer)
	{
		this.answerId = null;
		
		if (answer != null)
		{
			this.answerId = answer.getId();
		}

		// clear the auto score
		this.autoScore = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAssessmentAnswerId(String answerId)
	{
		this.answerId = answerId;

		// clear the auto score
		this.autoScore = null;
	}

	/**
	 * Access the db id for this answer entry.
	 * 
	 * @return The db id for this answer entry.
	 */
	protected String getId()
	{
		return this.id;
	}

	/**
	 * Establish the submission answer back reference.
	 * 
	 * @param answer
	 *        The submission answer this is part of.
	 */
	protected void initAnswer(SubmissionAnswerImpl answer)
	{
		this.submissionAnswer = answer;
	}

	/**
	 * Establish the auto score.
	 * 
	 * @param autoScore
	 *        The auto score.
	 */
	protected void initAutoScore(Float autoScore)
	{
		this.autoScore = autoScore;
	}

	/**
	 * Establish the db id for this answer entry.
	 * 
	 * @param id
	 *        The db id for this answer entry.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}
}
