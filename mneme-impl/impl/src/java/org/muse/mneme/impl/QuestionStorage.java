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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * QuestionStorage defines the storage interface for Questions.
 */
public interface QuestionStorage
{
	/**
	 * Clear out any mint objects that are old enough to be considered abandoned.
	 * 
	 * @param stale
	 *        The time to compare to the create date; before this they are stale.
	 */
	void clearStaleMintQuestions(Date stale);

	/**
	 * Create a new question that is a copy of each question in the pool.
	 * 
	 * @param userId
	 *        The user to own the questions.
	 * @param source
	 *        The pool of questions to copy.
	 * @param destination
	 *        the pool where the question will live.
	 */
	void copyPoolQuestions(String userId, Pool source, Pool destination);

	/**
	 * Count the questions in this context.
	 * 
	 * @param context
	 *        The context.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countContextQuestions(String context);

	/**
	 * Count the questions in this pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countPoolQuestions(Pool pool);

	/**
	 * Count the questions in the pools of this context
	 * 
	 * @param context
	 *        The context.
	 * @return A Map of the pool id -> count.
	 */
	Map<String, Integer> countPoolQuestions(String context);

	/**
	 * Check if a question by this id exists.
	 * 
	 * @param id
	 *        The question id
	 * @return TRUE if the question with this id exists, FALSE if not.
	 */
	Boolean existsQuestion(String id);

	/**
	 * Find all the questions in the context, sorted and paged.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort criteria.
	 * @param questionType
	 *        The (optional) quesiton type; if specified, only questions of this type are included.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return The list of questions.
	 */
	List<QuestionImpl> findContextQuestions(String context, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum,
			Integer pageSize);

	/**
	 * Find all the questions in the Pool, sorted and paged.
	 * 
	 * @param pool
	 *        The Pool.
	 * @param sort
	 *        The sort criteria.
	 * @param questionType
	 *        The (optional) quesiton type; if specified, only questions of this type are included.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return The list of questions.
	 */
	List<QuestionImpl> findPoolQuestions(Pool pool, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum, Integer pageSize);

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
	 * Move a question from one pool to another.
	 * 
	 * @param question
	 *        The question to move.
	 * @param pool
	 *        The pool to hold the question.
	 */
	void moveQuestion(Question question, Pool pool);

	/**
	 * Switch all non-historical questions that are in the from pool to be in the to pool
	 * 
	 * @param from
	 *        The from pool.
	 * @param to
	 *        The to pool.
	 */
	void switchQuestionPools(Pool from, Pool to);

	/**
	 * Construct a new question object.
	 * 
	 * @return A question object.
	 */
	QuestionImpl newQuestion();

	/**
	 * Construct a new question object that is a copy of another.
	 * 
	 * @param question
	 *        The question to copy.
	 * @return A question object.
	 */
	QuestionImpl newQuestion(QuestionImpl question);

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

}
