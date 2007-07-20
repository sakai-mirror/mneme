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

package org.muse.mneme.impl;

import java.util.List;

import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * QuestionStorage defines the storage interface for Questions.
 */
public interface QuestionStorage
{
	/**
	 * Find all the questions this user has access to.
	 * 
	 * @param userId
	 *        The user id
	 * @return The list of pools the user has access to.
	 */
	List<Question> findQuestions(String userId);

	/**
	 * Find all the questions in the pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The List of question ids that are in the pool.
	 */
	List<String> getPoolQuestions(Pool pool);

	/**
	 * Access a question by id.
	 * 
	 * @param id
	 *        the question id.
	 * @return The question with this id, or null if not found.
	 */
	QuestionImpl getQuestion(String id);

	/**
	 * Construct a new question object.
	 * 
	 * @return A question object.
	 */
	QuestionImpl newQuestion();

	/**
	 * Check if a question by this id exists.
	 * 
	 * @param id
	 *        The question id
	 * @return TRUE if the question with this id exists, FALSE if not.
	 */
	Boolean questionExists(String id);

	/**
	 * Remove a question from storage.
	 * 
	 * @param question
	 *        The question to remove.
	 */
	void removeQuestion(QuestionImpl question);

	/**
	 * Save changes made to this question.
	 * 
	 * @param question
	 *        the question to save.
	 */
	void saveQuestion(QuestionImpl question);

	/**
	 * Set the PoolService
	 * 
	 * @param service
	 *        The PoolService.
	 */
	void setPoolService(PoolService service);

	/**
	 * Set the QuestionService
	 * 
	 * @param service
	 *        The QuestionsService.
	 */
	void setQuestionService(QuestionService service);
}
