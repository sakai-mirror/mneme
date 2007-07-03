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
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.AssessmentSubmissionStatus;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;

/**
 * The "QuestionsAnswered" format delegate for the mneme tool.
 */
public class QuestionsAnsweredDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionsAnsweredDelegate.class);

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	public String format(Context context, Object value)
	{
		if (value == null) return null;

		Object o = context.get("submission");
		if (!(o instanceof Submission)) return value.toString();
		Submission submission = (Submission) o;

		// if focused on a section, we pick only that section's questions, else we use them all
		AssessmentSection section = null;
		if (value instanceof AssessmentSection)
		{
			section = (AssessmentSection) value;
		}

		// count the questions answered
		int count = 0;

		// find the section's answers to AssessmentQuestions that are in this section and are considered answered.
		for (SubmissionAnswer answer : submission.getAnswers())
		{
			if ((section == null || answer.getQuestion().getSection().equals(section)) && answer.getIsAnswered().booleanValue())
			{
				count++;
			}
		}

		return Integer.toString(count);
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
