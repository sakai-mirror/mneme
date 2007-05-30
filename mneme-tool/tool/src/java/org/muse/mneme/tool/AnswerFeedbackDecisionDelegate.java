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
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.util.DecisionDelegateImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.sakaiproject.util.StringUtil;

/**
 * The "AnswerFeedbackDecision" decision delegate for the mneme tool.
 */
public class AnswerFeedbackDecisionDelegate extends DecisionDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AnswerFeedbackDecisionDelegate.class);

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
	public boolean decide(Decision decision, Context context, Object focus)
	{
		// focus is the AssessmentAnswer
		if (focus == null) return false;
		if (!(focus instanceof AssessmentAnswer)) return false;

		AssessmentAnswer answer = (AssessmentAnswer) focus;
		AssessmentQuestion question = answer.getPart().getQuestion();
		Assessment assessment = question.getSection().getAssessment();

		if (!assessment.getFeedbackNow()) return false;
		if (!assessment.getFeedbackShowAnswerFeedback()) return false;

		if (!((question.getType() == QuestionType.multipleChoice) || (question.getType() == QuestionType.multipleCorrect))) return false;

		// for multipleChoice, this must be the answer selected by the entry of the submission answer
		if (question.getType() == QuestionType.multipleChoice)
		{
			SubmissionAnswer submissionAnswer = (SubmissionAnswer) context.get("answer");
			if (!StringUtil.contains(submissionAnswer.getEntryAnswerIds(), answer.getId())) return false;
		}

		return true;
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
