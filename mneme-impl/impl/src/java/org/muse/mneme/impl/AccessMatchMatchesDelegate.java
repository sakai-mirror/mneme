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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.FormatDelegateImpl;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.muse.mneme.impl.MatchQuestionImpl.MatchQuestionPair;

/**
 * The "AccessMatchMatches" format delegate for the mneme tool.
 */
public class AccessMatchMatchesDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AccessMatchMatchesDelegate.class);

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
		return value.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Object formatObject(Context context, Object value)
	{
		// value is the question
		if (value == null) return value;
		if (!(value instanceof Question)) return value;
		Question question = (Question) value;

		TypeSpecificQuestion tsq = question.getTypeSpecificQuestion();
		if (!(tsq instanceof MatchQuestionImpl)) return value;

		MatchQuestionImpl plugin = (MatchQuestionImpl) tsq;

		// pull out a list of the match ids
		List<MatchQuestionPair> pairs = plugin.getPairs();
		List<String> rv = new ArrayList<String>();
		for (MatchQuestionPair pair : pairs)
		{
			rv.add(pair.getId());
		}

		return rv;
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
