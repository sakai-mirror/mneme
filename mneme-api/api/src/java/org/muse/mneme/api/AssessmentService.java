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
	 * Sort options for getContextAssessments()
	 */
	enum AssessmentsSort
	{
		ddate_a, ddate_d, odate_a, odate_d, published_a, published_d, title_a, title_d, type_a, type_d
	}

	/**
	 * Check if the user is allowed to edit this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the user is allowed, FALSE if not.
	 */
	Boolean allowEditAssessment(Assessment assessment, String userId);

	/**
	 * Check if the user is allowed to list delivery assessments in this context.
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the user is allowed to list delivery assessments in this context, FALSE if not.
	 */
	Boolean allowListDeliveryAssessment(String context, String userId);

	/**
	 * Check if the user is allowed to manage assessments in this context.
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the user is allowed to manage assessments in this context, FALSE if not.
	 */
	Boolean allowManageAssessments(String context, String userId);

	/**
	 * Check if the assessment may be removed at this time by this user.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user (if null, the current user is used).
	 * @return TRUE if the assessment may be removed by this user, FALSE if not.
	 */
	Boolean allowRemoveAssessment(Assessment assessment, String userId);

	/**
	 * Create a new Assessment in the context that is a copy of another.<br />
	 * The new assessment is non-archived and un-published.
	 * 
	 * @param context
	 *        The context in which the assessment lives.
	 * @param assessment
	 *        The assessment to copy.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create assessments in this context.
	 * @return The new Assessment.
	 */
	Assessment copyAssessment(String context, Assessment assessment) throws AssessmentPermissionException;

	/**
	 * Count the assessments in the context - all of them that are not archived.
	 * 
	 * @param context
	 *        The context.
	 * @return The count of assessments in the context.
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
	 * Access an assessment by id.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, or null if not found.
	 */
	Assessment getAssessment(String id);

	/**
	 * Get all the assessments for the context, sorted. Does not include archived assessments.
	 * 
	 * @param context
	 *        The context.
	 * @param sort
	 *        The sort specification.
	 * @param publishedOnly
	 *        if TRUE, return only published assessments, else return unpublished as well.
	 * @return The List<Assessment> of all assessments in the context, sorteds, or empty if there are none.
	 */
	List<Assessment> getContextAssessments(String context, AssessmentsSort sort, Boolean publishedOnly);

	/**
	 * Create a new Assessment in the context.
	 * 
	 * @param context
	 *        The context in which the assessment lives.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to create assessments in this context.
	 * @return The new Assessment.
	 */
	Assessment newAssessment(String context) throws AssessmentPermissionException;

	/**
	 * Remove this assessment.
	 * 
	 * @param assessment
	 *        The assessment to remove.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to remove this assessment.
	 * @throws AssessmentPolicyException
	 *         if the assessment may not be removed due to API policy.
	 */
	void removeAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException;

	/**
	 * Save changes made to this assessment.
	 * 
	 * @param assessment
	 *        The assessment to save.
	 * @throws AssessmentPermissionException
	 *         if the current user is not allowed to edit this assessment.
	 * @throws AssessmentPolicyException
	 *         if the changes are not allowed to be saved due to policy violation.
	 */
	void saveAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException;
}
