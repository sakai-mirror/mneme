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
import java.util.List;
import java.util.Arrays;

import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificAnswer;

/**
 * FillBlanksAnswerImpl handles answers for the true/false question type.
 */
public class FillBlanksAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** String array of answers */
	protected String[] answers;

	/** The auto score. */
	protected Float autoScore = null;

	/** Set when the answer has been changed. */
	// TODO: need to reset this at some point when stored... -ggolden
	protected boolean changed = false;

	protected String reviewText = null;

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public FillBlanksAnswerImpl(Answer answer)
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
	public FillBlanksAnswerImpl(Answer answer, FillBlanksAnswerImpl other)
	{
		this.answer = answer;
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
	 * Access the currently selected answer as a string.
	 * 
	 * @return The answer.
	 */
	public String[] getAnswers()
	{
		return this.answers;
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
		return this.answers != null;
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
	public void clearIsChanged()
	{
		this.changed = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsCorrect()
	{
		Question question = answer.getQuestion();
		List<String> correctAnswers = ((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswers();
		String[] correctAnswersArray = new String[correctAnswers.size()];
		correctAnswersArray = (String[]) correctAnswers.toArray(correctAnswersArray);
		Boolean caseSensitive = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCaseSensitive());
		Boolean anyOrder = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getAnyOrder());

		if (this.answers.length != correctAnswersArray.length)
		{
			return Boolean.FALSE;
		}
		else
		{
			Boolean emptiesExist = checkEmptyAnswers(answers);
			if (emptiesExist == Boolean.TRUE)
			{
				return Boolean.FALSE;
			}
			boolean allCorrect = true;
			// At this point, we can assume all answer entries are non-null and not empty
			String[] answersArray = (String[]) this.answers.clone();
			if (anyOrder == Boolean.TRUE)
			{
				Arrays.sort(correctAnswersArray);
				Arrays.sort(answersArray);
			}
			for (int i = 0; i < answersArray.length; i++)
			{
				if (caseSensitive == Boolean.TRUE)
				{
					if ((correctAnswersArray[i].trim()).equals(answersArray[i].trim()))
					{
						allCorrect = true;
					}
					else
					{
						allCorrect = false;
						break;
					}
				}
				else
				{
					if ((correctAnswersArray[i].trim()).equalsIgnoreCase(answersArray[i].trim()))
					{
						allCorrect = true;
					}
					else
					{
						allCorrect = false;
						break;
					}
				}

			}
			if (allCorrect == true)
			{
				return Boolean.TRUE;
			}
			else
			{
				return Boolean.FALSE;
			}
		}
	}

	private Boolean checkEmptyAnswers(String[] answers)
	{
		Boolean emptiesExist = Boolean.FALSE;
		for (int i = 0; i < this.answers.length; i++)
		{
			if (this.answers[i] == null)
			{
				emptiesExist = Boolean.TRUE;
				return emptiesExist;
			}
			else
			{
				if (this.answers[i].trim().length() == 0)
				{
					emptiesExist = Boolean.TRUE;
					return emptiesExist;
				}
			}
		}
		return emptiesExist;
	}

	public String getReviewText()
	{
		Question question = answer.getQuestion();
		String parsedText = ((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getParsedText();
		for (int i = 0; i < this.answers.length; i++)
		{
			if (this.answers[i] != null)
			{
				if (this.answers[i].trim().length() > 0)
				{
					parsedText = parsedText.replaceFirst("\\{\\}", "<U>" + this.answers[i] + "</U>");
				}
				else
				{
					parsedText = parsedText.replaceFirst("\\{\\}", "<U>" + "____" + "</U>");
				}
			}
			else
			{
				parsedText = parsedText.replaceFirst("\\{\\}", "<U>" + "____" + "</U>");
			}
		}
		this.reviewText = parsedText;
		return this.reviewText;

	}

	/**
	 * Set the answers
	 * 
	 * @param answers
	 *        array of strings
	 */
	public void setAnswers(String[] answers)
	{
		// TODO: check order of answers. Don't set the changed flag if so.
		if ((answers == null) || (answers.length == 0)) return;
		this.answers = answers;
		this.changed = true;
	}
}
