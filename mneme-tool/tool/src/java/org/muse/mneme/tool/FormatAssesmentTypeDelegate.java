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
import org.muse.mneme.api.AssessmentType;

/**
 * The "FormatAssesmentActive" format delegate for the mneme tool.
 */
public class FormatAssesmentTypeDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatAssesmentTypeDelegate.class);

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
		if (!(value instanceof AssessmentType)) return value.toString();
		AssessmentType type = (AssessmentType) value;

		// if active, use this icon
		switch (type)
		{
			case test:
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/test_type.png\" alt=\""
						+ context.getMessages().getString("active-alt") + "\" />";
			case survey:
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/survey_type.png\" alt=\""
						+ context.getMessages().getString("active-alt") + "\" />";
			case assignment:
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/assignment_type.png\" alt=\""
						+ context.getMessages().getString("active-alt") + "\" />";
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
