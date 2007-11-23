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
 * TypeSpecificAnswer defines the plug-in answer handler for a question type.
 */
public interface TypeSpecificAnswer extends Cloneable
{
	/**
	 * Clear the is changed flag.s
	 */
	void clearIsChanged();

	/**
	 * Clone a copy
	 * 
	 * @param the
	 *        answer that the copy is for.
	 * @return A copy.
	 */
	Object clone(Answer answer);

	/**
	 * Consolidate the answer, such as after entering and before save.
	 * 
	 * @param destination
	 *        The destination from the post.
	 */
	void consolidate(String destination);

	/**
	 * Access the answer's automatic score.
	 * 
	 * @return The answer's auto-score.
	 */
	Float getAutoScore();

	/**
	 * Check if the question is answered.
	 * 
	 * @return TRUE if the question is considered to be answered, FALSE if not.
	 */
	Boolean getIsAnswered();

	/**
	 * Check if this answer has been changed by a setter.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	Boolean getIsChanged();
}
