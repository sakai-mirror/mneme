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
 * AssessmentStatus enumerates different lifecycle / type of assessments.
 * </p>
 */
public class AssessmentStatus
{
	private final Integer id;

	private final String name;

	private AssessmentStatus(int id, String name)
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

	static public AssessmentStatus parse(int id)
	{
		switch (id)
		{
			case 0:
				return INACTIVE;
			case 1:
				return ACTIVE;
			case 2:
				return DELETED;
			case 3:
				return PUBLISHED;
			default:
				return DELETED;
		}
	}

	/** Inactve. */
	public static final AssessmentStatus INACTIVE = new AssessmentStatus(0, "INACTIVE");

	/** Active. */
	public static final AssessmentStatus ACTIVE = new AssessmentStatus(1, "ACTIVE");

	/** Deleted. */
	public static final AssessmentStatus DELETED = new AssessmentStatus(2, "DELETED");

	/** Published. */
	public static final AssessmentStatus PUBLISHED = new AssessmentStatus(3, "PUBLISHED");
}
