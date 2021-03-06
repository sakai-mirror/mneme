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
 * AssessmentType enumerates the different assessment types.
 */
public enum AssessmentType
{
	test(0), survey(2), assignment(1);

	private final int sortOrder;

	private AssessmentType(int sortOrder)
	{
		this.sortOrder = Integer.valueOf(sortOrder);
	}

	public Integer getSortValue()
	{
		return sortOrder;			
	}
}
