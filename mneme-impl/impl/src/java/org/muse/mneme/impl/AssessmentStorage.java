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

import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;

/**
 * AssessmentStorage defines the storage interface for Assessments.
 */
public interface AssessmentStorage
{
	/**
	 * Check if an assessment by this id exists.
	 * 
	 * @param id
	 *        The assessment id
	 * @return TRUE if the assessment with this id exists, FALSE if not.
	 */
	Boolean assessmentExists(String id);

	/**
	 * Clear out any mint objects that are old enough to be considered abandoned.
	 */
	void clearStaleMintAssessments();

	/**
	 * Count the assessments in a context that are not archived.
	 * 
	 * @param context
	 *        The context.
	 * @return The count of assessment defined in a context.
	 */
	Integer countAssessments(String context);

	/**
	 * Get all the archived assessments in the context.
	 * 
	 * @param context
	 *        The context.
	 * @return The List<Assesment> of all archived assesments in the context, or empty if there are none.
	 */
	List<Assessment> getArchivedAssessments(String context);

	/**
	 * Access a assessment by id.
	 * 
	 * @param id
	 *        the submiassessmentssion id.
	 * @return The assessment with this id, or null if not found.
	 */
	AssessmentImpl getAssessment(String id);

	/**
	 * Get all the assessments defined in this context, sorted. Does not include archived assessments.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort specification.
	 * @param publishedOnly
	 *        if TRUE, return only published assessments, else return unpublished as well.
	 * @return The list of Assessments defined in the context, sorted.
	 */
	List<Assessment> getContextAssessments(String context, AssessmentService.AssessmentsSort sort, Boolean publishedOnly);

	/**
	 * Check if any live tests have any dependency on this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @param directOnly
	 *        if true, check only for direct (draw) dependencies, else use those as well as (manual) question dependencies.
	 * @return TRUE if any live tests have a dependency on this pool, FALSE if not.
	 */
	Boolean liveDependencyExists(Pool pool, boolean directOnly);

	/**
	 * Check if any live assessments have any direct dependency on this question.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if any live assessments have a direct dependency on this question, FALSE if not.
	 */
	Boolean liveDependencyExists(Question question);

	/**
	 * Construct a new Assessment object.
	 * 
	 * @return A new Assessment object.
	 */
	AssessmentImpl newAssessment();

	/**
	 * Construct a new Assessment object that is a copy of another.
	 * 
	 * @param assessment
	 *        The assessment to copy.
	 * @return A new Assessment object.
	 */
	AssessmentImpl newAssessment(AssessmentImpl assessment);

	/**
	 * Remove a assessment from storage.
	 * 
	 * @param assessment
	 *        The assessment to remove.
	 */
	void removeAssessment(AssessmentImpl assessment);

	/**
	 * Remove any direct dependencies on this pool from all assessments.
	 * 
	 * @param pool
	 *        The pool.
	 */
	void removeDependency(Pool pool);

	/**
	 * Remove any direct dependencies on this question from all assessments.
	 * 
	 * @param question
	 *        The question.
	 */
	void removeDependency(Question question);

	/**
	 * Save changes made to this assessment.
	 * 
	 * @param assessment
	 *        the assessment to save.
	 */
	void saveAssessment(AssessmentImpl assessment);

	/**
	 * Change any live assessments that are dependent on the from pool to become dependent instead on the to pool
	 * 
	 * @param from
	 *        The from pool.
	 * @param to
	 *        The to pool.
	 * @param directOnly
	 *        if true, switch only direct (draw) dependencies, else seitch those as well as (manual) question dependencies.
	 */
	void switchLiveDependency(Pool from, Pool to, boolean directOnly);

	/**
	 * Change any live assessments that are directly dependent on the from question to become dependent instead on the to question
	 * 
	 * @param from
	 *        The from question.
	 * @param to
	 *        The to question.
	 */
	void switchLiveDependency(Question from, Question to);
}
