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
	 * Sort options for findQuestions()
	 */
	enum FindQuestionsSort
	{
		description_a, description_d, pool_difficulty_a, pool_difficulty_d, pool_points_a, pool_points_d, pool_subject_a, pool_subject_d, pool_title_a, pool_title_d, type_a, type_d;
	}

	/**
	 * Check if the current user is allowed to edit this question.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if the user is allowed, FALSE if not.
	 */
	Boolean allowEditQuestion(Question question);

	/**
	 * Create a new question that is a copy of each question in the pool.
	 * 
	 * @param source
	 *        The pool of questions to copy.
	 * @param destination
	 *        the pool where the new questions will live.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new question.
	 */
	void copyPoolQuestions(Pool source, Pool destination) throws AssessmentPermissionException;

	/**
	 * Create a new question as a copy of another.
	 * 
	 * @param question
	 *        The question to copy.
	 * @param pool
	 *        The pool where the question will live (or null to keep it in the same pool).
	 * @return The new question.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new question.
	 */
	Question copyQuestion(Question question, Pool pool) throws AssessmentPermissionException;

	/**
	 * Count the questions with this criteria.
	 * 
	 * @param pool
	 *        The pool criteria - count questions from this pool only.
	 * @param search
	 *        The search criteria.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countQuestions(Pool pool, String search);

	/**
	 * Count the questions with this criteria.
	 * 
	 * @param context
	 *        The context - count questions from all pools in this context.
	 * @param search
	 *        The search criteria.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countQuestions(String context, String search);

	/**
	 * Check if a question exists.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return TRUE if the question exists, FALSE if not.
	 */
	Boolean existsQuestion(String questionid);

	/**
	 * Locate a list of questions with this criteria.
	 * 
	 * @param pool
	 *        The pool criteria - get questions from this pool only.
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return a list of questions that meet the criteria.
	 */
	List<Question> findQuestions(Pool pool, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize);

	/**
	 * Locate a list of questions with this criteria.
	 * 
	 * @param context
	 *        The context - get questions from all pools in the context.
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return a list of questions that meet the criteria.
	 */
	List<Question> findQuestions(String context, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize);

	/**
	 * Access a question by id.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return The Question with this id, or null if not found.
	 */
	Question getQuestion(String questionId);

	/**
	 * Move a question from one pool to another.
	 * 
	 * @param question
	 *        The question to move.
	 * @param pool
	 *        The pool to hold the question.
	 * @throws AssessmentPermissionException
	 *         If the current user is not allowed to make changes to this question or pool.
	 */
	void moveQuestion(Question question, Pool pool) throws AssessmentPermissionException;

	/**
	 * Create a new question.
	 * 
	 * @param pool
	 *        The pool where the question will live.
	 * @param type
	 *        The question type.
	 * @return The new question.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create a new question.
	 */
	Question newQuestion(Pool pool, String type) throws AssessmentPermissionException;

	/**
	 * Remove all the questions that are in this pool
	 * 
	 * @param pool
	 *        The pool.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to remove these questions.
	 */
	void removePoolQuestions(Pool pool) throws AssessmentPermissionException;

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
