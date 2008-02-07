/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
 * QuestionPoolService provides parts of the full QuestionService related to a pool's live questions.<br />
 * Usually use the full QuestionService (which extends this).
 */
public interface QuestionPoolService
{
	/**
	 * Sort options for findQuestions()
	 */
	enum FindQuestionsSort
	{
		cdate_a, cdate_d, description_a, description_d, pool_difficulty_a, pool_difficulty_d, pool_points_a, pool_points_d, pool_title_a, pool_title_d, type_a, type_d;
	}

	/**
	 * Count the questions with this criteria.
	 * 
	 * @param pool
	 *        The pool criteria - count questions from this pool only.
	 * @param search
	 *        The search criteria.
	 * @param questionType
	 *        The (optional) question type; if specified, only questions of this type are included.
	 * @param survey
	 *        if TRUE, include only survey questions, if FALSE, include only assessment questions, if NULL, include both.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countQuestions(Pool pool, String search, String questionType, Boolean survey);

	/**
	 * Locate a list of questions with this criteria.
	 * 
	 * @param pool
	 *        The pool criteria - get questions from this pool only.
	 * @param sort
	 *        The sort criteria.
	 * @param search
	 *        The search criteria.
	 * @param questionType
	 *        The (optional) question type; if specified, only questions of this type are included.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @param survey
	 *        if TRUE, include only survey questions, if FALSE, include only assessment questions, if NULL, include both.
	 * @return a list of questions that meet the criteria.
	 */
	List<Question> findQuestions(Pool pool, FindQuestionsSort sort, String search, String questionType, Integer pageNum, Integer pageSize,
			Boolean survey);

	/**
	 * Find all the questions (ids) in the pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The List of question ids that are in the pool.
	 */
	List<String> getPoolQuestionIds(Pool pool);
}
