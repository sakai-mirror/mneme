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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;

/**
 * SubmissionEntryImpl ...
 */
public class SubmissionAnswerEntryImpl implements SubmissionAnswerEntry
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionAnswerEntryImpl.class);

	protected String answerId = null;

	protected String answerText = null;

	protected Float autoScore = null;

	/** To hold Samigo's db id. */
	protected String id = null;

	protected String questionPartId = null;

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
		questionPartId = other.questionPartId;
		submissionAnswer = other.submissionAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEntry)) throw new ClassCastException();

		// no natrual comparison?
		// TODO: something based on the question part and the part's ordering -ggolden
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SubmissionAnswerEntry)) return false;
		if (this == obj) return true;
		// TODO: exact same answer, or same by id? also, is answerId part of this? -ggolden
		if (!((SubmissionAnswerEntryImpl) obj).submissionAnswer.getSubmission().getId().equals(this.submissionAnswer.getSubmission().getId())) return false;
		if (!(((SubmissionAnswerEntryImpl) obj).questionPartId.equals(this.questionPartId))) return false;
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
	public Float getAutoScore()
	{
		if (this.autoScore == null) return new Float(0);

		return this.autoScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsCorrect()
	{
		AssessmentAnswer questionAnswer = getAssessmentAnswer();
		if (questionAnswer != null)
		{
			AssessmentQuestion question = questionAnswer.getPart().getQuestion();

			// fill-in special check
			if (question.getType() == QuestionType.fillIn)
			{
				if ((questionAnswer.getText() != null) && (getAnswerText() != null))
				{
					if (isFillInAnswerCorrect(getAnswerText(), questionAnswer.getText(), question.getCaseSensitive().booleanValue()))
					{
						return Boolean.TRUE;
					}
				}
			}

			// numeric special check
			else if (question.getType() == QuestionType.numeric)
			{
				if ((questionAnswer.getText() != null) && (getAnswerText() != null))
				{
					if (isNumericAnswerCorrect(getAnswerText(), questionAnswer.getText()))
					{
						return Boolean.TRUE;
					}
				}
			}

			else if ((questionAnswer.getIsCorrect() != null) && questionAnswer.getIsCorrect().booleanValue())
			{
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPart getQuestionPart()
	{
		if (this.questionPartId == null) return null;

		return this.submissionAnswer.getQuestion().getPart(this.questionPartId);
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
	public void setQuestionPart(QuestionPart part)
	{
		this.questionPartId = null;

		if (part != null)
		{
			this.questionPartId = part.getId();
		}
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
	 * Establish the answer from an id.
	 * 
	 * @param answerId
	 *        The id to the question answer selected for this entry.
	 */
	protected void initAssessmentAnswerId(String answerId)
	{
		this.answerId = answerId;

		// clear the auto score
		this.autoScore = null;
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

	/**
	 * Establish the question part by id.
	 * 
	 * @param partId
	 *        the quesiton part id.
	 */
	protected void initQuestionPartId(String partId)
	{
		this.questionPartId = partId;
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
	protected boolean isFillInAnswerCorrect(String answer, String correct, boolean caseSensitive)
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
	protected boolean isNumericAnswerCorrect(String answer, String correct)
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
