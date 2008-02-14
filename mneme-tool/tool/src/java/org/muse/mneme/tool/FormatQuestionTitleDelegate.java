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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.FormatDelegateImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Submission;

/**
 * The "FormatScore" format delegate for the mneme tool.
 */
public class FormatQuestionTitleDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatQuestionTitleDelegate.class);

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
		if (!(value instanceof Question)) return null;

		Question question = (Question) value;
		Boolean continuous = question.getPart().getAssessment().getParts().getContinuousNumbering();

		// "submissions" is the List<Submission> of submissions (optional)
		Object o = context.get("submissions");
		List<Submission> submissions = null;
		if ((o != null) && (o instanceof List))
		{
			submissions = (List<Submission>) o;
		}

		Object[] args = new Object[4];
		if (continuous)
		{
			args[0] = question.getAssessmentOrdering().getPosition();
			args[1] = question.getPart().getAssessment().getParts().getNumQuestions();
		}
		else
		{
			args[0] = question.getPartOrdering().getPosition();
			args[1] = question.getPart().getNumQuestions();
		}

		// use the QuestionScore formatter to get the points with possible score
		if (question.getPart().getAssessment().getHasPoints() && question.getHasPoints())
		{
			QuestionScoreDelegate qs = new QuestionScoreDelegate();
			args[2] = qs.format(context, value);
		}

		String template = "format-question-title";
		if ((!question.getPart().getAssessment().getHasPoints()) || (!question.getHasPoints()))
		{
			template += "-no-points";
		}

		// if we have submissions in the context, count the answers given in them to this question
		if (submissions != null)
		{
			template += "-count";
			int total = 0;
			for (Submission s : submissions)
			{
				Answer a = s.getAnswer(question);
				if (a != null)
				{
					total++;
				}
			}

			if (total == 1)
			{
				template += "1";
			}
			args[3] = Integer.valueOf(total);
		}

		return context.getMessages().getFormattedMessage(template, args);
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
