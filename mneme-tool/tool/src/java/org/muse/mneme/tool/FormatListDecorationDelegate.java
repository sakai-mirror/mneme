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
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.AssessmentSubmissionStatus;
import org.muse.mneme.api.Submission;

/**
 * The "FormatListDecoration" format delegate for the mneme tool.
 */
public class FormatListDecorationDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatListDecorationDelegate.class);

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public String format(Context context, Object value)
	{
		Object o = context.get("submission");
		if (!(o instanceof Submission)) return value.toString();
		Submission submission = (Submission) o;

		AssessmentSubmissionStatus status = submission.getAssessmentSubmissionStatus();

		switch (status)
		{
			case future:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/future.gif\" alt=\""
						+ context.getMessages().getString("list-key-future") + "\" /><br /><span style=\"font-size:smaller\">"
						+ context.getMessages().getString("list-key-future") + "</span>";
			}

			case ready:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/begin.gif\" alt=\""
						+ context.getMessages().getString("list-key-todo") + "\" /><br /><span style=\"font-size:smaller\">"
						+ context.getMessages().getString("list-key-todo") + "</span>";
			}

			case inProgressAlert:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/exit.gif\" alt=\""
						+ context.getMessages().getString("list-key-inprogress") + "\" />" + "<img src=\"" + context.get("sakai.return.url")
						+ "/icons/warning.gif\" alt=\"" + context.getMessages().getString("list-key-urgent") + "\" />"
						+ "<br /><span style=\"font-size:smaller\">" + context.getMessages().getString("list-key-inprogress-urgent") + "</span>";
			}

			case inProgress:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/exit.gif\" alt=\""
						+ context.getMessages().getString("list-key-inprogress") + "\" /><br /><span style=\"font-size:smaller\">"
						+ context.getMessages().getString("list-key-inprogress") + "</span>";
			}

			case completeReady:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/finish.gif\" alt=\""
						+ context.getMessages().getString("list-key-complete") + "\" />" + "<img src=\"" + context.get("sakai.return.url")
						+ "/icons/begin.gif\" alt=\"" + context.getMessages().getString("list-key-repeat") + "\" />"
						+ "<br /><span style=\"font-size:smaller\">" + context.getMessages().getString("list-key-complete-repeat") + "</span>";
			}

			case complete:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/finish.gif\" alt=\""
						+ context.getMessages().getString("list-key-complete") + "\" /><br /><span style=\"font-size:smaller\">"
						+ context.getMessages().getString("list-key-complete") + "</span>";
			}
			case over:
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/cancel.gif\" alt=\""
						+ context.getMessages().getString("list-key-overdue") + "\" /><br /><span style=\"font-size:smaller\">"
						+ context.getMessages().getString("list-key-overdue") + "</span>";
			}
		}

		return null;
	}
}
