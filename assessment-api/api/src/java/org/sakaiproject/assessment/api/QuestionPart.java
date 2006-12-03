/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.api;

import java.util.List;

/**
 * <p>
 * QuestionPart defined one (or more) parts for a question.
 * </p>
 */
public interface QuestionPart
{
	/**
	 * Access an answer, by id.
	 * 
	 * @param answerId
	 *        The answer's id.
	 * @return The answer, or null if not found.
	 */
	AssessmentAnswer getAnswer(String answerId);

	/**
	 * Access answers.
	 * 
	 * @return The answers.
	 */
	List<? extends AssessmentAnswer> getAnswers();

	/**
	 * Access the correct answers.
	 * 
	 * @return The correct answers.
	 */
	List<AssessmentAnswer> getCorrectAnswers();

	/**
	 * Access the id of this part.
	 * 
	 * @return The part's id.
	 */
	String getId();

	/**
	 * Access the incorrect answers.
	 * 
	 * @return The incorrect answers.
	 */
	List<AssessmentAnswer> getIncorrectAnswers();

	/**
	 * Access the back pointer to the question.
	 * 
	 * @return The back pointer to the question.
	 */
	AssessmentQuestion getQuestion();

	/**
	 * Access the title.
	 * 
	 * @return The title.
	 */
	String getTitle();

	/**
	 * Set the answers to (a deep copy of) these answers.
	 * 
	 * @param answers
	 *        The answers.
	 */
	void setAnswers(List<? extends AssessmentAnswer> answers);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);
}
