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
	protected transient boolean genericChanged = false;

	protected String id = null;

	protected Boolean markedForReview = Boolean.FALSE;

	protected transient MnemeService mnemeService = null;

	protected String partId = null;

	protected String questionId = null;

	protected String reason = null;

	protected Submission submission = null;

	protected Date submittedDate = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AnswerImpl(AnswerImpl other, Submission owner)
	{
		set(other, owner);
	}

	/**
	 * Construct.
	 * 
	 * @param mnemeService
	 *        The MnemeService
	 */
	public AnswerImpl(MnemeService mnemeService)
	{
		this.mnemeService = mnemeService;
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
		Float score = this.answerHandler.getAutoScore();
		if (score == null) return null;

		// round
		float newScore = Math.round(score * 100.0f) / 100.0f;

		return Float.valueOf(newScore);
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
		Part p = getSubmission().getAssessment().getParts().getPart(this.partId);
		Question q = p.getQuestion(this.questionId);

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
		// if no auto nor evaluation, we have no score
		Float autoScore = getAutoScore();
		if ((autoScore == null) && (this.evaluation.getScore() == null)) return null;

		float rv = 0f;
		if (autoScore != null)
		{
			rv += autoScore.floatValue();
		}

		if (this.evaluation.getScore() != null)
		{
			rv += this.evaluation.getScore().floatValue();
		}

		// round away bogus decimals
		rv = Math.round(rv * 100.0f) / 100.0f;

		return Float.valueOf(rv);
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
		reason = StringUtil.trimToNull(reason);

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
		// take a null to mean clear the evaluation adjustment
		if (score == null)
		{
			this.evaluation.setScore(null);
		}

		else
		{
			float total = score.floatValue();

			// adjust to remove the current auto score
			if (getAutoScore() != null)
			{
				total -= getAutoScore().floatValue();
			}

			// round away bogus decimals
			total = Math.round(total * 100.0f) / 100.0f;

			this.evaluation.setScore(Float.valueOf(total));
		}
	}

	/**
	 * Clear the is-changed flag(s).
	 */
	protected void clearIsChanged()
	{
		this.genericChanged = false;
		this.answerHandler.clearIsChanged();
		this.evaluation.clearIsChanged();
	}

	/**
	 * Access the part id.
	 * 
	 * @return The part id.
	 */
	protected String getPartId()
	{
		return this.partId;
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
	 * Initialize the part id.
	 * 
	 * @param id
	 *        The part id.
	 */
	protected void initPartId(String id)
	{
		this.partId = id;
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
		initPartId(question.getPart().getId());

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
	 * Initialize the question id and type specific handler.
	 * 
	 * @param questionId
	 *        The question id.
	 * @param type
	 *        The question type.
	 */
	protected void initQuestion(String questionId, String type)
	{
		this.questionId = questionId;

		QuestionPlugin plugin = this.mnemeService.getQuestionPlugin(type);
		if (plugin != null)
		{
			this.answerHandler = plugin.newAnswer(this);
		}

		if (this.answerHandler == null)
		{
			M_log.warn("initQuestion: no plugin for type: " + type);
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
	protected void set(AnswerImpl other, Submission owner)
	{
		if (other.answerHandler != null) this.answerHandler = (TypeSpecificAnswer) (other.answerHandler.clone(this));
		this.evaluation = new AnswerEvaluationImpl(other.evaluation);
		this.id = other.id;
		this.markedForReview = other.markedForReview;
		this.mnemeService = other.mnemeService;
		this.partId = other.partId;
		this.questionId = other.questionId;
		this.reason = other.reason;
		this.submission = owner;
		this.submittedDate = other.submittedDate;
	}
}
