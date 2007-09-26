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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		Boolean responseTextual = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getResponseTextual());

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
				if (responseTextual == Boolean.TRUE)
				{
					if (isFillInAnswerCorrect(correctAnswersArray[i].trim(), answersArray[i].trim(), caseSensitive.booleanValue()) == Boolean.TRUE)
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
					if (isNumericAnswerCorrect(correctAnswersArray[i].trim(), answersArray[i].trim()) == Boolean.TRUE)
					{
						allCorrect = true;
					}
					else
					{
						allCorrect = false;
						break;
					}
				}
				/*
				 * if (caseSensitive == Boolean.TRUE) { if ((correctAnswersArray[i].trim()).equals(answersArray[i].trim())) { allCorrect = true; }
				 * else { allCorrect = false; break; } } else { if ((correctAnswersArray[i].trim()).equalsIgnoreCase(answersArray[i].trim())) {
				 * allCorrect = true; } else { allCorrect = false; break; } }
				 */

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

	/**
	 * Figure out if a fill-in answer is correct.
	 * 
	 * @param answer
	 *        The given answer.
	 * @param correct
	 *        The correct answer pattern (with option bars and wild cards).
	 * @param caseSensitive
	 *        if we should be case sensitive.
	 * @return true if the answer is correct, false if not
	 */
	private boolean isFillInAnswerCorrect(String answer, String correct, boolean caseSensitive)
	{
		// get the set of valid answers from the correct answer pattern (each one may have wild cards)
		String[] valid = correct.split("\\|");
		for (String test : valid)
		{
			// prepare the test as a regex, quoting all non-wildcards, changing the wildcard "*" into a regex ".+"
			StringBuffer regex = new StringBuffer();
			String[] parts = test.replaceAll("\\*", "|*|").split("\\|");
			for (String part : parts)
			{
				if ("*".equals(part))
				{
					regex.append(".+");
				}
				else
				{
					regex.append(Pattern.quote(part));
				}
			}
			Pattern p = Pattern.compile(regex.toString(), ((!caseSensitive) ? Pattern.CASE_INSENSITIVE : 0));

			// test
			Matcher m = p.matcher(answer);
			boolean result = m.matches();

			if (result) return true;
		}

		return false;
	}

	/**
	 * Figure out if a fill-in numeric answer is correct.
	 * 
	 * @param answer
	 *        The given answer.
	 * @param correct
	 *        The correct answer pattern (with option bars).
	 * @return true if the answer is correct, false if not
	 */
	private boolean isNumericAnswerCorrect(String answer, String correct)
	{
		try
		{
			// allow dot or comma for decimal point
			answer = answer.replace(',', '.');
			correct = correct.replace(',', '.');

			// answer needs to become a float (allow dot or comma for decimal point)
			float answerValue = Float.parseFloat(answer);

			// form the range of correct answers
			Float[] range = new Float[2];

			// if there's a bar in the correct pattern, split and use the first two as the range
			if (correct.indexOf("|") != -1)
			{
				String[] parts = correct.split("\\|");
				range[0] = Float.parseFloat(parts[0]);
				range[1] = Float.parseFloat(parts[1]);

				// make sure [0] <= [1]
				if (range[0].floatValue() > range[1].floatValue())
				{
					Float hold = range[0];
					range[0] = range[1];
					range[1] = hold;
				}
			}

			// otherwise use the single value for both sides of the range
			else
			{
				range[0] = range[1] = Float.parseFloat(correct);
			}

			// test
			if ((answerValue >= range[0].floatValue()) && (answerValue <= range[1].floatValue()))
			{
				return true;
			}
		}
		catch (NumberFormatException e)
		{
		}

		return false;
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
