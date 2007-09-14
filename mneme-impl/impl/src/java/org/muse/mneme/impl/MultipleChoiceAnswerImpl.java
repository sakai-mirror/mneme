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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * MultipleChoiceAnswerImpl handles answers for the multiple choice question type.
 */
public class MultipleChoiceAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The is the Integer HashSet that the user's answers are translated into. */
	protected Set<Integer> answerData = new HashSet<Integer>();

	/** The answers stored as an array */
	protected String[] answers;

	/** The auto score. */
	protected Float autoScore = null;

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public MultipleChoiceAnswerImpl(Answer answer)
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
	public MultipleChoiceAnswerImpl(Answer answer, MultipleChoiceAnswerImpl other)
	{
		this.answer = answer;
		this.answerData = other.answerData;
		this.autoScore = other.autoScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public void autoScore()
	{
		// full credit for correct answer, 0 for incorrect
		if (getIsCorrect())
		{
			this.autoScore = answer.getQuestion().getPool().getPoints();
		}
		else
		{
			this.autoScore = 0f;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone()
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// nothing to deep copy

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
		return this.autoScore;
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
		// TODO ???
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsCorrect()
	{
		// Check with Glenn on how this would work with shuffle choices
		if (this.answerData == null) return Boolean.FALSE;

		Question question = answer.getQuestion();
		return this.answerData.equals(((MultipleChoiceQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswers());
	}

	/**
	 * Access the currently selected answer as a string.
	 * 
	 * @return The answer.
	 */
	public String[] getAnswers()
	{
		if (this.answerData == null) return null;
		return (String[]) answerData.toArray(new String[answerData.size()]);
	}

	/**
	 * Set the answers
	 * 
	 * @param an
	 *        array of strings
	 */

	public void setAnswers(String[] answers)
	{
		if ((answers == null) || (answers.length == 0)) return;
		this.answerData = new HashSet(Arrays.asList(answers));

	}
}
