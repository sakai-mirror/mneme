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

import org.muse.ambrosia.api.Component;

/**
 * TypeSpecificAnswer defines the plug-in answer handles for a question type.
 */
public interface TypeSpecificQuestion extends Cloneable
{
	/**
	 * Clone a copy
	 * 
	 * @return A copy.
	 */
	Object clone();

	/**
	 * Access the rich text (html) answer key that shows the correct answer to the question.
	 * 
	 * @return The answer key, or null if there is none.
	 */
	String getAnswerKey();

	/**
	 * Access the user interface component for authoring of this question type.
	 * 
	 * @return The user interface component for authoring of this question type.
	 */
	Component getAuthoringUi();

	/**
	 * Access the user interface component for delivery of this question type.
	 * 
	 * @return The user interface component for delivery of this question type.
	 */
	Component getDeliveryUi();

	/**
	 * Access the user interface component for review of this question type.
	 * 
	 * @return The user interface component for review of this question type.
	 */
	Component getReviewUi();
}
