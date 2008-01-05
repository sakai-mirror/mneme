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
 * MultipleChoiceAnswerImpl handles answers for the multiple choice question type.
 */
public class MultipleChoiceAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The answers, as index references to the question's choices. */
	protected Set<Integer> answerData = new HashSet<Integer>();

	/** Set when the answer has been changed. */
	protected boolean changed = false;

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
		this.answerData = new HashSet<Integer>(other.answerData);
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

			// deep copy
			((MultipleChoiceAnswerImpl) rv).answerData = new HashSet<Integer>(this.answerData);

			((MultipleChoiceAnswerImpl) rv).answer = answer;

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
	public void consolidate(String destination)
	{
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
		// partial credit for each correct answer, partial negative for each incorrect, floor at 0.

		// count the number of correct answers
		Question question = answer.getQuestion();
		Set<Integer> correctAnswers = ((MultipleChoiceQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswerSet();

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

		// round away bogus decimals
		total = Math.round(total * 100.0f) / 100.0f;

		return Float.valueOf(total);
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getData()
	{
		String[] rv = new String[this.answerData.size()];
		int i = 0;
		for (Integer a : this.answerData)
		{
			rv[i++] = a.toString();
		}

		return rv;
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
		if ((answers == null) || (answers.length == 0)) return;

		Set<Integer> s = new HashSet<Integer>();
		for (String answer : answers)
		{
			s.add(Integer.valueOf(answer));
		}

		// check if the new answers exactly match the answers we already have.
		if (s.equals(this.answerData)) return;

		this.answerData = s;
		this.changed = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setData(String[] data)
	{
		this.answerData = new HashSet<Integer>();
		if ((data != null) && (data.length > 0))
		{
			for (int i = 0; i < data.length; i++)
			{
				this.answerData.add(Integer.valueOf(data[i]));
			}
		}
	}
}
