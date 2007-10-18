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

	/** String array of user answers */
	protected String[] answers;

	/** Set when the answer has been changed. */
	protected boolean changed = false;

	protected List<Boolean> entryCorrects = new ArrayList<Boolean>();

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

			((FillBlanksAnswerImpl) rv).answer = answer;

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
		// partial credit for each correct answer, 0 for each incorrect, floor at 0.

		Question question = answer.getQuestion();
		List<String> correctAnswers = ((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswers();
		String[] correctAnswersArray = new String[correctAnswers.size()];
		correctAnswersArray = (String[]) correctAnswers.toArray(correctAnswersArray);

		// Get all other question properties
		Boolean caseSensitive = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCaseSensitive());
		Boolean anyOrder = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getAnyOrder());
		Boolean responseTextual = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getResponseTextual());

		// each correct gets a part of the total points
		float partial = (correctAnswers.size() > 0) ? question.getPool().getPoints() / correctAnswers.size() : 0f;

		float total = 0f;
		if (this.answers != null)
		{
			String[] answersArray = (String[]) this.answers.clone();
			// Any order only matters when there is more than one blank
			if ((anyOrder == Boolean.TRUE) && (correctAnswers.size() > 1))
			{
				for (int j = 0; j < answersArray.length; j++)
				{
					boolean foundCorrect = false;
					if (answersArray[j] != null)
					{
						if (answersArray[j].trim().length() > 0)
						{
							for (int i = 0; i < correctAnswersArray.length; i++)
							{

								if (responseTextual == Boolean.TRUE)
								{
									if (isFillInAnswerCorrect(answersArray[j].trim(), correctAnswersArray[i].trim(), caseSensitive.booleanValue()))
									{
										total += partial;
										foundCorrect = true;
										break;
									}
									else
									{
										foundCorrect = false;
									}
								}
								else
								{
									if (isNumericAnswerCorrect(answersArray[j].trim(), correctAnswersArray[i].trim()))
									{
										total += partial;
										foundCorrect = true;
										break;
									}
									else
									{
										foundCorrect = false;
									}
								}
							}
						}
					}
				}
			}
			else
			{
				for (int i = 0; i < correctAnswersArray.length; i++)
				{
					if (answersArray[i] != null)
					{
						if (answersArray[i].trim().length() > 0)
						{
							if (responseTextual == Boolean.TRUE)
							{
								if (isFillInAnswerCorrect(answersArray[i].trim(), correctAnswersArray[i].trim(), caseSensitive.booleanValue()))
								{
									total += partial;
								}
							}
							else
							{
								if (isNumericAnswerCorrect(answersArray[i].trim(), correctAnswersArray[i].trim()))
								{
									total += partial;
								}
							}
						}
					}
				}
			}
		}

		// floor at 0
		if (total <= 0f) total = 0f;

		return total;
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
	public List<Boolean> getEntryCorrects()
	{
		// partial credit for each correct answer, 0 for each incorrect, floor at 0.

		Question question = answer.getQuestion();
		List<String> correctAnswers = ((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCorrectAnswers();
		String[] correctAnswersArray = new String[correctAnswers.size()];
		correctAnswersArray = (String[]) correctAnswers.toArray(correctAnswersArray);

		// Get all other question properties
		Boolean caseSensitive = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getCaseSensitive());
		Boolean anyOrder = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getAnyOrder());
		Boolean responseTextual = Boolean.valueOf(((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getResponseTextual());

		if (this.answers != null)
		{
			String[] answersArray = (String[]) this.answers.clone();
			// Any order only matters when there is more than one blank
			if ((anyOrder == Boolean.TRUE) && (correctAnswers.size() > 1))
			{
				for (int j = 0; j < answersArray.length; j++)
				{
					boolean foundCorrect = false;
					if (answersArray[j] != null)
					{
						if (answersArray[j].trim().length() > 0)
						{
							for (int i = 0; i < correctAnswersArray.length; i++)
							{

								if (responseTextual == Boolean.TRUE)
								{
									if (isFillInAnswerCorrect(answersArray[j].trim(), correctAnswersArray[i].trim(), caseSensitive.booleanValue()))
									{
										foundCorrect = true;
										break;
									}
									else
									{
										foundCorrect = false;
									}
								}
								else
								{
									if (isNumericAnswerCorrect(answersArray[j].trim(), correctAnswersArray[i].trim()))
									{
										foundCorrect = true;
										break;
									}
									else
									{
										foundCorrect = false;
									}
								}
							}
							if (foundCorrect)
							{
								this.entryCorrects.add(Boolean.TRUE);
							}
							else
							{
								this.entryCorrects.add(Boolean.FALSE);
							}
						}
					}
				}
			}
			else
			{
				for (int i = 0; i < correctAnswersArray.length; i++)
				{
					if (answersArray[i] != null)
					{
						if (answersArray[i].trim().length() > 0)
						{
							if (responseTextual == Boolean.TRUE)
							{
								if (isFillInAnswerCorrect(answersArray[i].trim(), correctAnswersArray[i].trim(), caseSensitive.booleanValue()))
								{
									this.entryCorrects.add(Boolean.TRUE);
								}
								else
								{
									this.entryCorrects.add(Boolean.FALSE);
								}
							}
							else
							{
								if (isNumericAnswerCorrect(answersArray[i].trim(), correctAnswersArray[i].trim()))
								{
									this.entryCorrects.add(Boolean.TRUE);
								}
								else
								{
									this.entryCorrects.add(Boolean.FALSE);
								}
							}
						}
					}
				}
			}
		}
		else
		{
			// If all answers are blank, they each need to be marked incorrect
			for (int i = 0; i < correctAnswersArray.length; i++)
			{
				this.entryCorrects.add(Boolean.FALSE);
			}
		}
		return this.entryCorrects;
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
	 * @return a string containing the user's answers in it, and blank if there was no answer
	 */
	public String getReviewText()
	{
		Question question = answer.getQuestion();
		String parsedText = ((FillBlanksQuestionImpl) question.getTypeSpecificQuestion()).getParsedText();
		if (this.answers != null)
		{
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
}
