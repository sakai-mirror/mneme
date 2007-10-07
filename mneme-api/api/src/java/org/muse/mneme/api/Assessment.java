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
 * Assessment models organizations of questions in parts.
 */
public interface Assessment
{
	/**
	 * Get the submitable status for the assessment - this indicates if we are expecting submissions now.
	 * 
	 * @return The submitable status
	 */
	AcceptSubmitStatus getAcceptSubmitStatus();

	/**
	 * Access the special access settings.
	 * 
	 * @return The special access settings.
	 */
	AssessmentAccess getAccess();

	/**
	 * Check if the assessment is archived.
	 * 
	 * @return TRUE if the assessment is archived, FALSE if not.
	 */
	Boolean getArchived();

	/**
	 * Access the context of this assessment.
	 * 
	 * @return The assessment's context string.
	 */
	String getContext();

	/**
	 * Access the created-by attribution (user and date).
	 * 
	 * @return The created-by attribution.
	 */
	Attribution getCreatedBy();

	/**
	 * Access the availability dates for the assessment.
	 * 
	 * @return The availability dates for the assessment.
	 */
	AssessmentDates getDates();

	/**
	 * Access the assessment grading settings.
	 * 
	 * @return The assessment grading settings.
	 */
	AssessmentGrading getGrading();

	/**
	 * Access the id of this assessment.
	 * 
	 * @return The assessment's id.
	 */
	String getId();

	/**
	 * Check if the assessment has been changed.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	Boolean getIsChanged();

	/**
	 * Check if the assessment is closed for submissions - unpublished, archived, not yet open or past submit-until date.
	 * 
	 * @return TRUE if closed for submission, FALSE if not.
	 */
	Boolean getIsClosed();

	/**
	 * Check if all the completed submissions to this assessment have been released.
	 * 
	 * @return TRUE if all completed submissions (so far) to this assessment have been released, FALSE if not.
	 */
	Boolean getIsFullyReleased();

	/**
	 * Check if the assessment is live; i.e. has at least one started submission.
	 * 
	 * @return TRUE if the assessment is live, FALSE if not.
	 */
	Boolean getIsLive();

	/**
	 * Are multiple submissions allowed?
	 * 
	 * @return TRUE if multiple submissions are allowed, FALSE if not.
	 */
	Boolean getIsMultipleSubmissionsAllowed();

	/**
	 * Check if the assessment is open for submissions - published, not archived, past open date, before submit-until date.
	 * 
	 * @param withGrace
	 *        TRUE to consider the grace period, FALSE not to.
	 * @return TRUE if open for submission, FALSE if not.
	 */
	Boolean getIsOpen(Boolean withGrace);

	/**
	 * Check if the assessment is valid; i.e. has no inconsistencies in its definition.
	 * 
	 * @return TRUE if the assessment is valid, FALSE if not.
	 */
	Boolean getIsValid();

	/**
	 * Access the modified-by attribution (user and date).
	 * 
	 * @return The modified-by attribution.
	 */
	Attribution getModifiedBy();

	/**
	 * Access the number of submissions allowed, if not unlimited.
	 * 
	 * @return The number of submissions allowed, or null if unlimited.
	 */
	Integer getNumSubmissionsAllowed();

	/**
	 * Access the assessment part information.
	 * 
	 * @return The assessment part information.
	 */
	AssessmentParts getParts();

	/**
	 * Access the assessment's presentation; the rich tet and attachments that describe the assessment.
	 * 
	 * @return The assessment's presentation.
	 */
	Presentation getPresentation();

	/**
	 * Check if the assessment is published.
	 * 
	 * @return TRUE if the assessment is published, FALSE if not.
	 */
	Boolean getPublished();

	/**
	 * Access how questions are to be grouped per page for delivery..
	 * 
	 * @return The question gropuing setting.
	 */
	QuestionGrouping getQuestionGrouping();

	/**
	 * Access the random access flag that controlls an assessment taker's random or linear access to the questions of the assessment.
	 * 
	 * @return TRUE if random question access is supported, FALSE if only linear question access is supported.
	 */
	Boolean getRandomAccess();

	/**
	 * Access the require-honor-pledge setting.
	 * 
	 * @return TRUE if this assessment requires an "honor pledge" from the user, FALSE if not.
	 */
	Boolean getRequireHonorPledge();

	/**
	 * Access the assessment review settings.
	 * 
	 * @return The assessment review settings.
	 */
	AssessmentReview getReview();

	/**
	 * Get the total scores for all completed submissions to this assessment.
	 * 
	 * @param assessmentId
	 *        The assessment id.
	 * @return A List containing all the scores for completed submissions to this assessment, or an empty list if there are none.
	 */
	List<Float> getScores();

	/**
	 * Access the "show-hints" setting
	 * 
	 * @return TRUE to show authored hints, FALSE to ignore them.
	 */
	Boolean getShowHints();

	/**
	 * Access the submission that is referencing this assessment (so we can answer submission specific questions like question order).
	 * <p>
	 * Note: this will be null unless the assessment is accessed throught Submission.getAssessment().
	 * </p>
	 * 
	 * @return The submission, or null if there is none.
	 */
	Submission getSubmissionContext();

	/**
	 * Access the presentation to show after submit.
	 * 
	 * @return The presentation to show after submit.
	 */
	Presentation getSubmitPresentation();

	/**
	 * Access the time limit for taking the assessment (ms).
	 * 
	 * @return The assessment's time limit, or null for umlimited.
	 */
	Long getTimeLimit();

	/**
	 * Access the title of this assessment.
	 * 
	 * @return The assessment's title.
	 */
	String getTitle();

	/**
	 * Access the assessment type.
	 * 
	 * @return The asssessment type.
	 */
	AssessmentType getType();

	/**
	 * Set the assessment's archived setting.
	 * 
	 * @param archived
	 *        The archived setting.
	 */
	void setArchived(Boolean archived);

	/**
	 * Set the context of this assessment.
	 * 
	 * @param context
	 *        The assessment's context string.
	 */
	void setContext(String context);

	/**
	 * Set the number of submissions allowed for limited submissions.
	 * 
	 * @param count
	 *        The number of submissions allowed, or null to make it unlimited.
	 */
	void setNumSubmissionsAllowed(Integer count);

	/**
	 * Set the assessment's published setting.
	 * 
	 * @param published
	 *        The published setting.
	 */
	void setPublished(Boolean published);

	/**
	 * Set how questions are grouped per page for delivery.
	 * 
	 * @param grouping
	 *        The question grouping setting.
	 */
	void setQuestionGrouping(QuestionGrouping grouping);

	/**
	 * Set the random access flag that controlls an assessment taker's random or linear access to the questions of the assessment.
	 * 
	 * @param setting
	 *        TRUE if random question access is supported, FALSE if only linear question access is supported.
	 */
	void setRandomAccess(Boolean setting);

	/**
	 * Set the require-honor-pledge setting.
	 * 
	 * @param honorPledge
	 *        TRUE if this assessment requires an "honor pledge" from the user, FALSE if not.
	 */
	void setRequireHonorPledge(Boolean honorPledge);

	/**
	 * Set the "show-hints" setting
	 * 
	 * @param showHints
	 *        TRUE to show authored hints, FALSE to ignore them.
	 */
	void setShowHints(Boolean showHints);

	/**
	 * Set the time limit for taking the assessment (ms).
	 * 
	 * @param limit
	 *        The time limit for the assessment, or null for unlimited.
	 */
	void setTimeLimit(Long limit);

	/**
	 * Set the title of this assessment.
	 * 
	 * @param title
	 *        The assessment's title.
	 */
	void setTitle(String title);

	/**
	 * Set the type of this assessment.
	 * 
	 * @param type
	 *        The assessment's type.
	 */
	void setType(AssessmentType type);
}
