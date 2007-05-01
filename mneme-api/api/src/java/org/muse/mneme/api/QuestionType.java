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

public enum QuestionType
{
	audioRecording(7), essay(5), fileUpload(6), fillIn(8), matching(9), multipleChoice(1), multipleCorrect(2), numeric(11), survey(
			3), trueFalse(4);

	/**
	 * Find the type that matches this db encoding.
	 * 
	 * @param dbEncoding
	 *        The db encoding.
	 * @return The type that matches the encoding, or null if not found.
	 */
	public static QuestionType valueOf(Integer dbEncoding)
	{
		for (QuestionType type : QuestionType.values())
		{
			if (type.dbEncoding.equals(dbEncoding)) return type;
		}

		return null;
	}

	/** The database encoding for this type. */
	protected Integer dbEncoding = null;

	QuestionType(int dbEncoding)
	{
		this.dbEncoding = new Integer(dbEncoding);
	}

	/**
	 * Access the database encoding for this type.
	 * 
	 * @return The database encoding for this type.
	 */
	public Integer getDbEncoding()
	{
		return this.dbEncoding;
	}
}
