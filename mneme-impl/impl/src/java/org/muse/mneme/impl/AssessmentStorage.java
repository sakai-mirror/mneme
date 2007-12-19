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

import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;

/**
 * AssessmentStorage defines the storage interface for Assessments.
 */
public interface AssessmentStorage
{
	/**
	 * Clear out any mint objects that are old enough to be considered abandoned.
	 * 
	 * @param stale
	 *        The time to compare to the create date; before this they are stale.
	 */
	void clearStaleMintAssessments(Date stale);

	/**
	 * Count the assessments in a context that are not archived.
	 * 
	 * @param context
	 *        The context.
	 * @return The count of assessment defined in a context.
	 */
	Integer countAssessments(String context);

	/**
	 * Check if an assessment by this id exists.
	 * 
	 * @param id
	 *        The assessment id
	 * @return TRUE if the assessment with this id exists, FALSE if not.
	 */
	Boolean existsAssessment(String id);

	/**
	 * Get all the archived assessments in the context.
	 * 
	 * @param context
	 *        The context.
	 * @return The List<Assesment> of all archived assesments in the context, or empty if there are none.
	 */
	List<AssessmentImpl> getArchivedAssessments(String context);

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
	List<AssessmentImpl> getContextAssessments(String context, AssessmentService.AssessmentsSort sort, Boolean publishedOnly);

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
	 * Remove any draw dependencies on this pool from all live assessments.
	 * 
	 * @param pool
	 *        The pool.
	 */
	void removeDependency(Pool pool);

	/**
	 * Remove any pick dependencies on this question from all live assessments.
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
}
