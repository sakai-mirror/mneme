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

import java.util.HashSet;
import java.util.Set;

import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificAnswer;

/**
 * MatchAnswerImpl handles answers for the multiple choice question type.
 */
public class MatchAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The answers, as index references to the question's choices. */
	protected Set<Integer> answerData = new HashSet<Integer>();

	/** The auto score. */
	protected Float autoScore = null;

	/** Set when the answer has been changed. */
	protected boolean changed = false;

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public MatchAnswerImpl(Answer answer)
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
	public MatchAnswerImpl(Answer answer, MatchAnswerImpl other)
	{
		this.answer = answer;
		this.answerData = new HashSet<Integer>(other.answerData);
		this.autoScore = other.autoScore;
		this.changed = other.changed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void autoScore()
	{
		// partial credit for each correct answer, partial negative for each incorrect, floor at 0.

		// count the number of correct answers
		Question question = answer.getQuestion();
		Set correctAnswers = ((MatchQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswerSet();

		// each correct / incorrect gets a part of the total points
		float partial = (correctAnswers.size() > 0) ? question.getPool().getPoints() / correctAnswers.size() : 0f;

		float total = 0f;
		for (Integer answer : this.answerData)
		{
			// if this is one of the correct answers, give credit
			if (correctAnswers.contains(answer))
			{
				total += partial;
			}

			// otherwise remove credit
			else
			{
				total -= partial;
			}
		}

		// floor at 0
		if (total < 0f) total = 0f;

		this.autoScore = total;
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
	public Object clone()
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// deep copy
			((MatchAnswerImpl) rv).answerData = new HashSet<Integer>(this.answerData);

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	/**
	 * Access the currently selected answer as a string.
	 * 
	 * @return The answer.
	 */
	public String[] getAnswers()
	{
		String[] rv = new String[answerData.size()];
		int i = 0;
		for (Integer answer : this.answerData)
		{
			rv[i++] = answer.toString();
		}

		return rv;
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
		return !this.answerData.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		return this.changed;
	}

	/**
	 * Set the answers
	 * 
	 * @param answers
	 *        array of strings
	 */
	public void setAnswers(String[] answers)
	{
		Set<Integer> s = new HashSet<Integer>();
		if ((answers == null) || (answers.length == 0)) return;
		for (String answer : answers)
		{
			s.add(Integer.valueOf(answer));
		}

		// check if the answers to set exactly match the answers we already have. Don't set the changed flag if so.
		if (s.equals(this.answerData)) return;

		this.answerData = s;
		this.changed = true;
	}
}
