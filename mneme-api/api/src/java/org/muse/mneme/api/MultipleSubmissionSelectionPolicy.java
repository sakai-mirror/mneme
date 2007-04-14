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

package org.muse.mneme.api;

/**
 * <p>
 * MultipleSubmissionSelectionPolicy enumerates different policies for how to pick from multiple submissions to an assessment from a single user.
 * </p>
 */
public class MultipleSubmissionSelectionPolicy
{
	private final String m_name;

	private final Integer m_id;

	private MultipleSubmissionSelectionPolicy(int id, String name)
	{
		m_id = new Integer(id);
		m_name = name;
	}

	static public MultipleSubmissionSelectionPolicy parse(int id)
	{
		if (USE_HIGHEST_GRADED.m_id.intValue() == id) return USE_HIGHEST_GRADED;
		return USE_LATEST;
	}

	public String toString()
	{
		return m_name;
	}

	public Integer dbEncoding()
	{
		return m_id;
	}

	/** Use the latest submission. */
	public static final MultipleSubmissionSelectionPolicy USE_LATEST = new MultipleSubmissionSelectionPolicy(2, "latest");

	/** Use the highest graded submission. */
	public static final MultipleSubmissionSelectionPolicy USE_HIGHEST_GRADED = new MultipleSubmissionSelectionPolicy(1, "highest");
}
