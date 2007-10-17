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

import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificAnswer;

/**
 * TrueFalseAnswerImpl handles answers for the true/false question type.
 */
public class TrueFalseAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The answer is stored as a Boolean: TRUE or FALSE; null if not answered. */
	protected Boolean answerData = null;

	/** Set when the answer has been changed. */
	protected transient boolean changed = false;

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public TrueFalseAnswerImpl(Answer answer)
	{
		this.answer = answer;
	}

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 * @param other
	 *        The other to copy.
	 */
	public TrueFalseAnswerImpl(Answer answer, TrueFalseAnswerImpl other)
	{
		this.answer = answer;
		this.answerData = other.answerData;
		this.changed = other.changed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearIsChanged()
	{
		this.changed = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone(Answer answer)
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// nothing to deep copy

			((TrueFalseAnswerImpl) rv).answer = answer;

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		// correct?
		boolean correct = false;
		if (this.answerData != null)
		{
			Question question = this.answer.getQuestion();
			Boolean correctAnswer = Boolean.valueOf(((TrueFalseQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswer());
			correct = this.answerData.equals(correctAnswer);
		}

		// full credit for correct answer, 0 for incorrect
		if (correct)
		{
			return answer.getQuestion().getPool().getPoints();
		}

		return 0f;
	}

	/**
	 * Access the currently selected answer as a string.
	 * 
	 * @return The answer.
	 */
	public String getAnswer()
	{
		if (this.answerData == null) return null;

		return this.answerData.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswered()
	{
		return this.answerData != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		return this.changed;
	}

	/**
	 * Set the answer - any boolean string value will work.
	 * 
	 * @param answer
	 *        The answer.
	 */
	public void setAnswer(String answer)
	{
		if ((answer == null) || (answer.trim().length() == 0)) return;

		// make a change only if the answer is different from what we are already set to
		Boolean b = Boolean.valueOf(answer.trim());
		if (b.equals(this.answerData)) return;

		this.answerData = b;
		this.changed = true;
	}
}
