/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/FormatAssesmentActiveDelegate.java $
 * $Id: FormatAssesmentActiveDelegate.java 10759 2007-07-23 23:52:17Z ggolden@umich.edu $
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

/**
 * The "FormatAssesmentActive" format delegate for the mneme tool.
 */
public class FormatAssesmentGradedDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatAssesmentGradedDelegate.class);

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
		if (!(value instanceof Boolean)) return value.toString();
		Boolean active = (Boolean) value;
		if(active == null) active = false;
		// if graded, use this icon
		if (active)
		{
			return "<img src=\"" + context.get("sakai.return.url") + "/icons/checkon.gif\" alt=\"" + context.getMessages().getString("graded-alt")
					+ "\" />";
		}
		// else this one
		else
		{
			return context.getMessages().getString("ungraded-assessment");
		}
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
