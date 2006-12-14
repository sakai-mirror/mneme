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
 * AssessmentSection is one section of an assessment; the section contains multiple questions.
 * </p>
 */
public interface AssessmentSection
{
	/**
	 * Access the back pointer to the assessment.
	 * 
	 * @return The back pointer to the assessment.
	 */
	Assessment getAssessment();

	/**
	 * Access the description.
	 * 
	 * @return The description.
	 */
	String getDescription();

	/**
	 * Access the first question.  The order will be in a random order (if enabled) based on the current user.
	 * 
	 * @return The first question, or null if there are none.
	 */
	AssessmentQuestion getFirstQuestion();

	/**
	 * Access the id.
	 * 
	 * @return The id.
	 */
	String getId();

	/**
	 * Access the last question.  The order will be in a random order (if enabled) based on the current user.
	 * 
	 * @return The last question, or null if there are none.
	 */
	AssessmentQuestion getLastQuestion();

	/**
	 * Access the count of questions.
	 * 
	 * @return The count of questions.
	 */
	Integer getNumQuestions();

	/**
	 * Access the ordering information within the assessment.
	 * 
	 * @return The ordering information within the assessment.
	 */
	Ordering<AssessmentSection> getOrdering();

	/**
	 * Access one of the questions, by question id.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return the question, or null if not found.
	 */
	AssessmentQuestion getQuestion(String questionId);

	/**
	 * Access the questions. The order will be in a random order (if enabled) based on the current user.
	 * 
	 * @return The questions. The order will be in a random order (if enabled) based on the current user.
	 */
	List<? extends AssessmentQuestion> getQuestions();

	/**
	 * Access the questions. The order will be in authored order.
	 * 
	 * @return The questions. The order will be in authored order.
	 */
	List<? extends AssessmentQuestion> getQuestionsAsAuthored();

	/**
	 * Access the random question ordering setting; if true, questions are to be presented in random order withing the section.
	 * 
	 * @return The random question ordering setting.
	 */
	Boolean getRandomQuestionOrder();

	/**
	 * Access the title.
	 * 
	 * @return The title.
	 */
	String getTitle();

	/**
	 * Access the sum of all possible points for all questions in the section.
	 * 
	 * @return The sum of all possible points for all questions in the section.
	 */
	Float getTotalPoints();

	/**
	 * Set the description.
	 * 
	 * @param description
	 *        The description.
	 */
	void setDescription(String description);

	/**
	 * Set the questions to (a deep copy of) this list of questions.
	 * 
	 * @param questions
	 *        The questions.
	 */
	void setQuestions(List<? extends AssessmentQuestion> questions);

	/**
	 * Set the random ordering setting.
	 * 
	 * @param setting
	 *        The random ordering setting.
	 */
	void setRandomQuestionOrder(Boolean setting);

	/**
	 * Set the title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);
}
