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

/**
 * QuestionService provides services around question management for Mneme.
 */
public interface QuestionService
{
	/**
	 * Check if the current user is allowed to edit this question from this context.
	 * 
	 * @param question
	 *        The question.
	 * @param context
	 *        The context.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the user is allowed, FALSE if not.
	 */
	Boolean allowEditQuestion(Question question, String context, String userId);

	/**
	 * Locate a list of questions with this criteria.
	 * 
	 * @param userId
	 *        the user id.
	 * @return a list of questions that meet the criteria.
	 */
	List<Question> findQuestions(String userId);

	/**
	 * Access a question by id.
	 * 
	 * @param questionId
	 * @return The Question with this id, or null if not found.
	 */
	Question idQuestion(String questionId);

	/**
	 * Create a new question.
	 * 
	 * @return The new question.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new question.
	 */
	Question newQuestion() throws AssessmentPermissionException;

	/**
	 * Remove this question.
	 * 
	 * @param question
	 *        The question to remove.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to edit this question.
	 */
	void removeQuestion(Question question) throws AssessmentPermissionException;

	/**
	 * Save changes made to this question.
	 * 
	 * @param question
	 *        The question to save.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to edit this question.
	 */
	void saveQuestion(Question question) throws AssessmentPermissionException;
}
