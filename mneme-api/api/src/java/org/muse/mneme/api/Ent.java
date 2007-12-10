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

import java.util.List;

import org.muse.mneme.api.QuestionService.FindQuestionsSort;

/**
 * Ent is even smaller than Entity - just something with an id and description.
 */
public interface Ent
{
	/**
	 * Access the description.
	 * 
	 * @return The description.
	 */
	String getDescription();

	/**
	 * Access the id.
	 * 
	 * @return The id;
	 */
	String getId();
}
