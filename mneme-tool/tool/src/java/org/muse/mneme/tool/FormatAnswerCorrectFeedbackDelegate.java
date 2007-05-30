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
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.sakaiproject.util.StringUtil;

/**
 * The "FormatAnswerCorrectFeedback" format delegate for the mneme tool.
 */
public class FormatAnswerCorrectFeedbackDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatAnswerCorrectFeedbackDelegate.class);

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
	public String format(Context context, Object focus)
	{
		if (focus == null) return null;
		if (!(focus instanceof AssessmentAnswer)) return null;
		AssessmentAnswer answer = (AssessmentAnswer) focus;

		// the question this is an answer to
		AssessmentQuestion question = answer.getPart().getQuestion();
		if (question == null) return null;

		Object o = context.get("submission");
		if (!(o instanceof Submission)) return null;
		Submission submission = (Submission) o;

		Assessment assessment = submission.getAssessment();
		if (assessment == null) return null;

		// if we are doing feedback just now
		if (assessment.getFeedbackNow())
		{
			// if we are doing currect answer feedback
			if (assessment.getFeedbackShowCorrectAnswer().booleanValue())
			{
				// search for our answer without creating it, and if found check if it is this QuestionAnswer
				for (SubmissionAnswer subAnswer : submission.getAnswers())
				{
					// is this submission answer the answer to our assessment question answer's question?
					if (subAnswer.getQuestion().equals(question))
					{
						// not for survey
						if (question.getType() != QuestionType.survey)
						{
							// is the submission answer this answer?
							if (StringUtil.contains(subAnswer.getEntryAnswerIds(), answer.getId()))
							{
								// correct
								if ((answer.getIsCorrect() != null) && answer.getIsCorrect().booleanValue())
								{
									return "<img src=\"" + context.get("sakai.return.url") + "/icons/correct.png\" alt=\""
											+ context.getMessages().getString("correct") + "\" />";
								}

								// incorrect
								else
								{
									return "<img src=\"" + context.get("sakai.return.url") + "/icons/wrong.png\" alt=\""
											+ context.getMessages().getString("incorrect") + "\" />";
								}
							}
						}
					}
				}
			}
		}

		return null;
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
