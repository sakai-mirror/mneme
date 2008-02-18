/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.FormatDelegateImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.TypeSpecificQuestion;

/**
 * The "FormatFillinPositionPercents" format delegate for the mneme tool.
 */
public class FormatFillinPositionPercentsDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatFillinPositionPercentsDelegate.class);

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
		// value is the answer text
		if (value == null) return null;
		if (!(value instanceof String)) return value.toString();
		String target = (String) value;

		// "question" is the Question
		Object o = context.get("question");
		if (o == null) return value.toString();
		if (!(o instanceof Question)) return value.toString();
		Question question = (Question) o;

		TypeSpecificQuestion tsq = question.getTypeSpecificQuestion();
		if (!(tsq instanceof FillBlanksQuestionImpl)) return value.toString();
		FillBlanksQuestionImpl plugin = (FillBlanksQuestionImpl) tsq;
		boolean caseSensitive = Boolean.valueOf(plugin.getCaseSensitive());

		// "submissions" is the Submissions list
		o = context.get("submissions");
		if (o == null) return value.toString();
		if (!(o instanceof List)) return value.toString();
		List<Submission> submissions = (List<Submission>) o;

		// "position" is the 1 based fill-in position
		o = context.get("position");
		if (o == null) return value.toString();
		if (!(o instanceof Integer)) return value.toString();
		Integer position = (Integer) o;
		int pos = position - 1;

		int count = 0;
		int total = 0;
		for (Submission s : submissions)
		{
			if (s.getIsPhantom()) continue;
			if (!s.getIsComplete()) continue;

			Answer a = s.getAnswer(question);
			if (a != null)
			{
				total++;

				if (a.getIsAnswered())
				{
					// does the answer's value match our target answer?
					// Note: assume that the answer for this position is the nth data element
					String[] answers = a.getTypeSpecificAnswer().getData();
					if ((answers != null) && (answers.length > pos) && (answers[pos] != null))
					{
						if (caseSensitive)
						{
							if (answers[pos].equals(target))
							{
								count++;
							}
						}
						else
						{
							if (answers[pos].equalsIgnoreCase(target))
							{
								count++;
							}
						}
					}
				}
			}
		}

		if (total > 0)
		{
			// percent
			int pct = (count * 100) / total;

			Object[] args = new Object[2];
			args[0] = Integer.valueOf(pct);
			args[1] = Integer.valueOf(count);

			String template = "format-percent";
			return context.getMessages().getFormattedMessage(template, args);
		}

		String template = "format-percent-none";
		return context.getMessages().getString(template);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object formatObject(Context context, Object value)
	{
		return value.toString();
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
