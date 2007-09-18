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

import org.muse.mneme.api.QuestionService.FindQuestionsSort;

/**
 * Pool defines the question pools.
 */
public interface Pool
{
	/**
	 * Count the questions in this pool with this criteria.
	 * 
	 * @param userId
	 *        The user id (if null, the current user is used).
	 * @param search
	 *        The search criteria.
	 * @return The questions in this pool with this criteria.
	 */
	Integer countQuestions(String userId, String search);

	/**
	 * Draw questions based on this random seed.
	 * 
	 * @param seed
	 *        The random seed.
	 * @param numQuestions
	 *        The number of questions to draw.
	 * @return A List of question ids drawn from the pool.
	 */
	List<String> drawQuestionIds(long seed, Integer numQuestions);

	/**
	 * Locate a list of questions in this pool with this criteria.
	 * 
	 * @param userId
	 *        the user id (if null, the current user is used).
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
	List<Question> findQuestions(String userId, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize);

	/**
	 * Access all questions.
	 * 
	 * @return A List of question ids from the pool.
	 */
	List<String> getAllQuestionIds();

	/**
	 * Access the created-by (owner / date)
	 * 
	 * @return The pool's created-by.
	 */
	Attribution getCreatedBy();

	/**
	 * Access the description of the pool.
	 * 
	 * @return The description of the pool.
	 */
	String getDescription();

	/**
	 * Access the difficulty value for the questions in this pool.<br />
	 * Values range from 1 (hardest) to 5 (easiest).
	 * 
	 * @return The difficulty value for the questions in this pool.
	 */
	Integer getDifficulty();

	/**
	 * Access the id of this pool.
	 * 
	 * @return The pool's id.
	 */
	String getId();

	/**
	 * Access the modified-by (owner / date)
	 * 
	 * @return The pool's modified-bt.
	 */
	Attribution getModifiedBy();

	/**
	 * Access the number of questions currently defined in the pool.
	 * 
	 * @return The number of questions currently defined in the pool.
	 */
	Integer getNumQuestions();

	/**
	 * Access the number of points for each question in this pool.
	 * 
	 * @return The number of points for each question in this pool.
	 */
	Float getPoints();

	/**
	 * Access the subject of the pool.
	 * 
	 * @return The subject of the pool.
	 */
	String getSubject();

	/**
	 * Access the title of the pool.
	 * 
	 * @return The title of the pool.
	 */
	String getTitle();

	/**
	 * Access the version of this pool.
	 * 
	 * @return The pool's version.
	 */
	String getVersion();

	/**
	 * Set the description of the pool.
	 * 
	 * @param description
	 *        The description of the pool.
	 */
	void setDescription(String description);

	/**
	 * Set the difficulty value for the questions in this pool.
	 * 
	 * @param difficulty
	 *        The difficulty value for the questions in this pool.
	 */
	void setDifficulty(Integer difficulty);

	/**
	 * Set the number of points for each question in this pool.
	 * 
	 * @param points
	 *        The number of points for each question in this pool.
	 */
	void setPoints(Float points);

	/**
	 * Set the subject of the pool.
	 * 
	 * @param subject
	 *        The subject of the pool.
	 */
	void setSubject(String subject);

	/**
	 * Set the title of the pool.
	 * 
	 * @param title
	 *        The title of the pool.
	 */
	void setTitle(String title);
}
