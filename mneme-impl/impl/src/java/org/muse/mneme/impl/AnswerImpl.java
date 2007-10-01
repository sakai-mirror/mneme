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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.AnswerEvaluation;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.TypeSpecificAnswer;
import org.sakaiproject.util.StringUtil;

/**
 * AnswerImpl implements Answer
 */
public class AnswerImpl implements Answer
{
	private static Log M_log = LogFactory.getLog(AnswerImpl.class);

	protected TypeSpecificAnswer answerHandler = null;

	protected AnswerEvaluationImpl evaluation = new AnswerEvaluationImpl(this);

	/** Set when there's a change in the generic part of the answer. */
	protected boolean genericChanged = false;

	protected String id = null;

	protected Boolean markedForReview = Boolean.FALSE;

	protected MnemeService mnemeService = null;

	protected String partId = null;

	protected String questionId = null;

	protected QuestionService questionService = null;

	protected String reason = null;

	protected Submission submission = null;

	protected Date submittedDate = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AnswerImpl(AnswerImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param mnemeService
	 * @param questionService
	 */
	public AnswerImpl(MnemeService mnemeService, QuestionService questionService)
	{
		this.mnemeService = mnemeService;
		this.questionService = questionService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void autoScore()
	{
		if (this.answerHandler != null)
		{
			this.answerHandler.autoScore();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two AnswerImpls are equals if they have the same id
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if ((this.id == null) || (((AnswerImpl) obj).id == null)) return false;
		return this.id.equals(((AnswerImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		return this.answerHandler.getAutoScore();
	}

	/**
	 * {@inheritDoc}
	 */
	public AnswerEvaluation getEvaluation()
	{
		return this.evaluation;
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
	public Boolean getIsAnswered()
	{
		if (this.answerHandler == null) return Boolean.FALSE;

		return this.answerHandler.getIsAnswered();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		if (this.genericChanged) return Boolean.TRUE;
		if (this.answerHandler == null) return Boolean.FALSE;
		return this.answerHandler.getIsChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsComplete()
	{
		// this is "complete" if we have a submission date
		return this.submittedDate != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMarkedForReview()
	{
		return this.markedForReview;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion()
	{
		QuestionImpl q = (QuestionImpl) this.questionService.getQuestion(this.questionId);
		Part p = getSubmission().getAssessment().getParts().getPart(this.partId);
		q.initPartContext(p);
		q.initSubmissionContext(this.submission);

		return q;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getReason()
	{
		return this.reason;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission()
	{
		return this.submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getSubmittedDate()
	{
		return this.submittedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalScore()
	{
		float rv = 0f;
		if (getAutoScore() != null)
		{
			rv += getAutoScore();
		}

		if (this.evaluation.getScore() != null)
		{
			rv += this.evaluation.getScore();
		}

		return new Float(rv);
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificAnswer getTypeSpecificAnswer()
	{
		return this.answerHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.id == null ? "null".hashCode() : this.id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMarkedForReview(Boolean forReview)
	{
		if (forReview == null) throw new IllegalArgumentException();
		if (this.markedForReview.equals(forReview)) return;

		this.markedForReview = forReview;
		this.genericChanged = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setReason(String reason)
	{
		if (!StringUtil.different(this.reason, reason)) return;

		this.reason = reason;
		this.genericChanged = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmittedDate(Date submitted)
	{
		this.submittedDate = submitted;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTotalScore(Float score)
	{
		float total = score.floatValue();

		// adjust to remove the current auto score
		if (getAutoScore() != null)
		{
			total -= getAutoScore().floatValue();
		}

		this.evaluation.setScore(total);
	}

	/**
	 * Clear the is-changed flag(s).
	 */
	protected void clearIsChanged()
	{
		this.genericChanged = false;
		this.answerHandler.clearIsChanged();
	}

	/**
	 * Initialize the id.
	 * 
	 * @param id
	 *        The id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Initialize the question to which this is an answer.
	 * 
	 * @param question
	 *        The question to which this is an answer.
	 */
	protected void initQuestion(Question question)
	{
		this.questionId = question.getId();
		this.partId = question.getPart().getId();

		QuestionPlugin plugin = this.mnemeService.getQuestionPlugin(question.getType());
		if (plugin != null)
		{
			this.answerHandler = plugin.newAnswer(this);
		}

		if (this.answerHandler == null)
		{
			M_log.warn("initQuestion: no plugin for type: " + question.getType());
		}
	}

	/**
	 * Initialize the submission this answer is part of.
	 * 
	 * @param submission
	 *        The submission this answer is part of.
	 */
	protected void initSubmission(Submission submission)
	{
		this.submission = submission;
	}

	/**
	 * Establish the type-specific answer handler.
	 * 
	 * @param answerHandler
	 *        The type-specific answer handler.
	 */
	protected void initTypeSpecificAnswer(TypeSpecificAnswer answerHandler)
	{
		this.answerHandler = answerHandler;
	}

	/**
	 * Set as a copy of another
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AnswerImpl other)
	{
		if (other.answerHandler != null) this.answerHandler = (TypeSpecificAnswer) (other.answerHandler.clone());
		this.evaluation = new AnswerEvaluationImpl(other.evaluation);
		this.id = other.id;
		this.markedForReview = other.markedForReview;
		this.mnemeService = other.mnemeService;
		this.partId = other.partId;
		this.questionId = other.questionId;
		this.questionService = other.questionService;
		this.reason = other.reason;
		this.submission = other.submission;
		this.submittedDate = other.submittedDate;
	}
}
