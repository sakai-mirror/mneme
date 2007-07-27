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
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SubmissionService;

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
	 * Count the assessments in a context.
	 * 
	 * @param context
	 *        The context.
	 * @return The count of assessment defined in a context.
	 */
	Integer countAssessments(String context);

	/**
	 * Access a assessment by id.
	 * 
	 * @param id
	 *        the submiassessmentssion id.
	 * @return The assessment with this id, or null if not found.
	 */
	AssessmentImpl getAssessment(String id);

	/**
	 * Get all the assessments defined in this context, sorted.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort specification.
	 * @return The list of Assessments defined in the context, sorted..
	 */
	List<Assessment> getContextAssessments(String context, AssessmentService.AssessmentsSort sort);

	/**
	 * Construct a new Assessment object.
	 * 
	 * @return A new Assessment object.
	 */
	AssessmentImpl newAssessment();

	/**
	 * Remove a assessment from storage.
	 * 
	 * @param assessment
	 *        The assessment to remove.
	 */
	void removeAssessment(AssessmentImpl assessment);

	/**
	 * Save changes made to this assessment.
	 * 
	 * @param assessment
	 *        the assessment to save.
	 */
	void saveAssessment(AssessmentImpl assessment);

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

	/**
	 * Set the SubmissionService
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	void setSubmissionService(SubmissionService service);
}
