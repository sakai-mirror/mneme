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

import org.sakaiproject.user.api.User;

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
	 * Check if student identities are invisible to the grader when grading.<br />
	 * Use this instead of the AssessmentGrading version for logic, use the other for editing settings.
	 * 
	 * @return TRUE if student identities are invisible to the grader when grading, FALSE if not.
	 */
	Boolean getAnonymous();

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
	 * Are multiple submissions allowed?
	 * 
	 * @return TRUE if multiple submissions are allowed, FALSE if not.
	 */
	Boolean getHasMultipleTries();

	/**
	 * Check if we have a time limmit.
	 * 
	 * @return TRUE if a time limit is defined, FALSE if not.
	 */
	Boolean getHasTimeLimit();

	/**
	 * Check if we have a tries limit.
	 * 
	 * @return TRUE if a tries limit is defined, FALSE if not.
	 */
	Boolean getHasTriesLimit();

	/**
	 * Check if there are any completed submissions that have any null scores for answered answers.
	 * 
	 * @return TRUE if there are unscored submissions to this assessment, FALSE if not.
	 */
	Boolean getHasUnscoredSubmissions();

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
	 * Check if the assessment is live; i.e. has at least one started submission.
	 * 
	 * @return TRUE if the assessment is live, FALSE if not.
	 */
	Boolean getIsLive();

	/**
	 * Check if the assessment is locked; i.e. will not reflect any pool or question changes, nor can the makeup of the assessment be changed.
	 * 
	 * @return TRUE if the assessment is locked, FALSE if not.
	 */
	Boolean getIsLocked();

	/**
	 * Check if the assessment is valid; i.e. has no inconsistencies in its definition.
	 * 
	 * @return TRUE if the assessment is valid, FALSE if not.
	 */
	Boolean getIsValid();

	/**
	 * Check if the end user has never made initial settings.
	 * 
	 * @return TRUE if this has not been modified since creation, FALSE if it has.
	 */
	Boolean getMint();

	/**
	 * Access the modified-by attribution (user and date).
	 * 
	 * @return The modified-by attribution.
	 */
	Attribution getModifiedBy();

	/**
	 * Access the assessment part information.
	 * 
	 * @return The assessment part information.
	 */
	AssessmentParts getParts();

	/**
	 * Access the password.
	 * 
	 * @return The password.
	 */
	AssessmentPassword getPassword();

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
	 * Access the set of special access definitions for the assessment.
	 * 
	 * @return The set of special access definitions for the assessment.
	 */
	AssessmentSpecialAccess getSpecialAccess();

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
	 * Get a list of Users who can submit.
	 * 
	 * @return a List of Users who can submit.
	 */
	List<User> getSubmitUsers();

	/**
	 * Access the time limit for taking the assessment (ms).
	 * 
	 * @return The time limit for taking the assessment (ms), or null if it is untimed.
	 */
	Long getTimeLimit();

	/**
	 * Access the title of this assessment.
	 * 
	 * @return The assessment's title. Will be blank (never null) if not defined.
	 */
	String getTitle();

	/**
	 * Access the number of submissions allowed, if not unlimited.
	 * 
	 * @return The number of submissions allowed, or null if unlimited.
	 */
	Integer getTries();

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
	 * An alternate way to clear the time limit if set to false.
	 * 
	 * @param hasTimeLimit
	 *        if FALSE, clear the time limit.
	 */
	void setHasTimeLimit(Boolean hasTimeLimit);

	/**
	 * An alternate way to clear the tries limit if set to false.
	 * 
	 * @param hasTriesLimit
	 *        if FALSE, clear the tries.
	 */
	void setHasTriesLimit(Boolean hasTriesLimit);

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
	 * Set the number of submissions allowed for limited submissions.
	 * 
	 * @param count
	 *        The number of submissions allowed, or null to make it unlimited.
	 */
	void setTries(Integer count);

	/**
	 * Set the type of this assessment.
	 * 
	 * @param type
	 *        The assessment's type.
	 */
	void setType(AssessmentType type);
}
