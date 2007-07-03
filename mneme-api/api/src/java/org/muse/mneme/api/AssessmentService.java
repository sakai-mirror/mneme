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
 * AssessmentService manages assessments (tests, surveys, assignments, etc).
 */
public interface AssessmentService
{
	/**
	 * Create a new persistent assessment from the given information.<br />
	 * The id will be re-generated, default values set, and the parameter assessment updated.
	 * 
	 * @param a
	 *        The information from which to make the new assesment.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to create the assessment.
	 */
	void addAssessment(Assessment a) throws AssessmentPermissionException;

	/**
	 * Check if the current user is allowed to add an assessment in this context.
	 * 
	 * @param context
	 *        The context.
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowAddAssessment(String context);

	/**
	 * Check if the current user is allowed to list delivery assessments in this context.
	 * 
	 * @param context
	 *        The context.
	 * @return TRUE if the user is allowed to list delivery assessments in this context, FALSE if not.
	 */
	Boolean allowListDeliveryAssessment(String context);

	/**
	 * Count the published assessments in the context - all of them!
	 * 
	 * @param context
	 *        The context.
	 * @return The count of published assessments in the context.
	 */
	Integer countAssessments(String context);

	/**
	 * Access an assessment by id. Assures that the full assessment information is populated. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, complete, or null if not found.
	 */
	Assessment getAssessment(String id);

	/**
	 * Get all the assessments for the context.
	 * 
	 * @param context
	 *        The context.
	 * @return The List <Assessment> of all assessments in the context, or empty if there are none.
	 */
	List<Assessment> getContextAssessments(String context);

	/**
	 * Access an assessment by id, but do not populate any information. Information will be populated as needed. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, or null if not found.
	 */
	Assessment idAssessment(String id);

	/**
	 * Create a new Assessment object, currently detached from persistence.
	 * 
	 * @return a new, detached from persistence, assessment object.
	 */
	Assessment newAssessment();

	/**
	 * Create a new AssessmentAnswer object in this question part.
	 * 
	 * @param part
	 *        The part this answer is in.
	 * @return a new answer that is in this part.
	 */
	AssessmentAnswer newAssessmentAnswer(QuestionPart part);

	/**
	 * Create a new question object in this section.
	 * 
	 * @param section
	 *        The assessment section to hold the question.
	 * @return a new question that is in the section.
	 */
	AssessmentQuestion newQuestion(AssessmentSection section);

	/**
	 * Create a new question part object in this question.
	 * 
	 * @param question
	 *        The question that this part is in.
	 * @return a new part that is in the question.
	 */
	QuestionPart newQuestionPart(AssessmentQuestion question);

	/**
	 * Create a new Assessment section for this assessment.
	 * 
	 * @param assessment
	 *        The assessment this section goes in.
	 * @return a new section that is in the assessment.
	 */
	AssessmentSection newSection(Assessment assessment);
}
