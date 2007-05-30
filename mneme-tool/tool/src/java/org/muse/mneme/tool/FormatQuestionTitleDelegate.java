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
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.tool.DeliveryControllers.QuestionScore;

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
		if (!(value instanceof AssessmentQuestion)) return null;

		AssessmentQuestion question = (AssessmentQuestion) value;
		Boolean continuous = question.getSection().getAssessment().getContinuousNumbering();

		Object[] args = new Object[3];
		if ((continuous != null) && (continuous.booleanValue()))
		{
			args[0] = question.getAssessmentOrdering().getPosition();
			args[1] = question.getSection().getAssessment().getNumQuestions();
		}
		else
		{
			args[0] = question.getSectionOrdering().getPosition();
			args[1] = question.getSection().getNumQuestions();
		}

		// use the QuestionScore formater to get the points with possible score
		QuestionScore qs = new QuestionScore();
		args[2] = qs.format(context, value);

		return context.getMessages().getFormattedMessage("question-question-title", args);
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
