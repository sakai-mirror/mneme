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

package org.muse.mneme.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.FormatDelegateImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.Answer;

/**
 * The "FormatQuestionDecoration" format delegate for the mneme tool.
 */
public class QuestionScoreDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionScoreDelegate.class);

	/**
	 * Format a score to 2 decimal places, trimming ".0" if present.
	 * 
	 * @param score
	 *        The score to format.
	 * @return The formatted score
	 */
	protected static String formatScore(float score)
	{
		// round to a single place
		String rv = Float.toString(Math.round(score * 100.0f) / 100.0f);

		// get rid of ".00"
		if (rv.endsWith(".00"))
		{
			rv = rv.substring(0, rv.length() - 3);
		}

		// get rid of ".0"
		if (rv.endsWith(".0"))
		{
			rv = rv.substring(0, rv.length() - 2);
		}

		return rv;
	}

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public String format(Context context, Object value)
	{
		if (value == null) return null;
		if (!(value instanceof Question)) return value.toString();
		Question question = (Question) value;

		Object o = context.get("submission");
		if (!(o instanceof Submission)) return value.toString();
		Submission submission = (Submission) o;

		Assessment assessment = submission.getAssessment();
		if (assessment == null) return value.toString();

		// use the {}/{} format if doing feedback, or just {} if not.
		StringBuffer rv = new StringBuffer();

		Boolean review = (Boolean) context.get("review");

		// if we are doing review just now, and if we are needing review and it's set, and if the submission has been graded
		if ((review != null) && review && submission.getIsGraded())
		{
			// if we are doing question score feedback
			if (assessment.getReview().getShowCorrectAnswer())
			{
				// the auto-scores for this answered question
				float score = 0;

				// find the section answer to this question (don't create it!)
				for (Answer answer : submission.getAnswers())
				{
					if (answer.getQuestion().equals(question))
					{
						score = answer.getTotalScore().floatValue();
						break;
					}
				}

				rv.append(context.getMessages().getString("score") + ": " + formatScore(score));
			}
		}

		// add the possible points for the question
		rv.append(" (<span style=\"font-size:80%\">" + context.getMessages().getString("max") + "</span> "
				+ formatScore(question.getPool().getPoints()) + ")");

		return rv.toString();
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}
}
