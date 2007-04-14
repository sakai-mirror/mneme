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
 * QuestionPresentation enumerates the question presentation options (single question per page, section per page, entire assessment
 * on one page)
 * </p>
 */
public class QuestionPresentation
{
	private final Integer id;

	private final String name;

	private QuestionPresentation(int id, String name)
	{
		this.id = new Integer(id);
		this.name = name;
	}

	public String toString()
	{
		return this.name;
	}

	public Integer dbEncoding()
	{
		return this.id;
	}

	static public QuestionPresentation parse(int id)
	{
		switch (id)
		{
			case 1:
				return BY_QUESTION;
			case 2:
				return BY_SECTION;
			case 3:
				return BY_ASSESSMENT;
			default:
				// TODO: what is the default?
				return BY_QUESTION;
		}
	}

	/** Single question per page. */
	public static final QuestionPresentation BY_QUESTION = new QuestionPresentation(1, "BY_QUESTION");

	/** All questions in a section on a single page. */
	public static final QuestionPresentation BY_SECTION = new QuestionPresentation(2, "BY_SECTION");

	/** All questions in the assessment on a single page. */
	public static final QuestionPresentation BY_ASSESSMENT = new QuestionPresentation(3, "BY_ASSESSMENT");
}
