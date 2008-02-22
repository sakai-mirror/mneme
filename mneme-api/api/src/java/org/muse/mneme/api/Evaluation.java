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
 * Evaluation holds a comment and a score from an evaluator for a submission or an answer.
 */
public interface Evaluation
{
	/**
	 * Access the user / date attribution for who made this evaluation and when.
	 * 
	 * @return The attribution.
	 */
	Attribution getAttribution();

	/**
	 * Access the rich text (html) comment.
	 * 
	 * @return The rich text (html) comment.
	 */
	String getComment();

	/**
	 * Access the evaluated flag.
	 * 
	 * @return TRUE if marked evaluated, FALSE if not.
	 */
	Boolean getEvaluated();

	/**
	 * Access the score.
	 * 
	 * @return The score.
	 */
	Float getScore();

	/**
	 * Set the rich text (html) comment.
	 * 
	 * @param comment
	 *        The rich text (html) comment. Must be well formed HTML or plain text.
	 */
	void setComment(String comment);

	/**
	 * Set the evaluated flag.
	 * 
	 * @param Boolean
	 *        TRUE if marked evaluated, FALSE if not.
	 */
	void setEvaluated(Boolean evaluated);

	/**
	 * Set the score.
	 * 
	 * @param score
	 *        The score.
	 */
	void setScore(Float score);
}
