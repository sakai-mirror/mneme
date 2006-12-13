/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.api;

import java.util.List;

import org.sakaiproject.time.api.Time;

/**
 * <p>
 * Assessment models the set of questions and their potential answers. Questions are organized within the assessment into sections.<br />
 * An assessment is always worked with as a single unit, and is referenced by the assessment id.<br />
 * Each section, question, and possible answer are also referenced by ids.
 * </p>
 */
public interface Assessment
{
	/**
	 * Access allow late submissions (after due date) setting for this assessment.
	 * 
	 * @return TRUE if submissions are allowed after the due date, FALSE if not.
	 */
	Boolean getAllowLateSubmit();

	/**
	 * Access the auto-submit (when time limit is expired) for this assessment.
	 * 
	 * @return TRUE if the assessment will auto-submit when time has expired, FALSE if not.
	 */
	Boolean getAutoSubmit();

	/**
	 * Access the context of this assessment.
	 * 
	 * @return The assessment's context string.
	 */
	String getContext();

	/**
	 * Access the creating user's id.
	 * 
	 * @return The creating users id.
	 */
	String getCreatedBy();

	/**
	 * Access the due date. Submissions after this date are considered late, if they are accepted at all.
	 * 
	 * @return The assessment's due date, or null if there is none.
	 */
	Time getDueDate();

	/**
	 * Access the feedback date; the date on which feedback is made available.
	 * 
	 * @return The assessment's feedback date, or null there is no feedback date.
	 */
	Time getFeedbackDate();

	/**
	 * Access the feedback delivery option.
	 * 
	 * @return The assessment's feedback delivery.
	 */
	FeedbackDelivery getFeedbackDelivery();

	/**
	 * Check if feedback is eanbled right now for this assessment, considering when now is and all the feedback settings.
	 * 
	 * @return TRUE if feedback is enabled right now, FALSE if not.
	 */
	Boolean getFeedbackNow();

	/**
	 * Access the "show correct answer" feedback setting; controls if we should show the correct answer to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the correct answer as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowCorrectAnswer();

	/**
	 * Access the "show question" feedback setting; controls if we should show the question feedback to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the question feedback as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowQuestionFeedback();

	/**
	 * Access the "show question score" feedback setting; controls if we should show the individual question scores to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the individual question scores as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowQuestionScore();

	/**
	 * Access the "show score" feedback setting; controls if we should show the score to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the assessment score as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowScore();

	/**
	 * Access the "show statistics" feedback setting; controls if we should show statistics as part of feedback.
	 * 
	 * @return TRUE if we should show statistics as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowStatistics();

	/**
	 * Access the first section
	 * 
	 * @return the first section, of null if there are none.
	 */
	AssessmentSection getFirstSection();

	/**
	 * Access the id of this assessment.
	 * 
	 * @return The assessment's id.
	 */
	String getId();

	/**
	 * Access the choice for which of multiple submissions should be the official graded one; either the lastest submission, or the highest graded one.
	 * 
	 * @return The assessment's MultipleSubmissionSelectionPolicy, or null if we cannot find it.
	 */
	MultipleSubmissionSelectionPolicy getMultipleSubmissionSelectionPolicy();

	/**
	 * Access the count of sections.
	 * 
	 * @return The count of sections.
	 */
	Integer getNumSections();

	/**
	 * Access the number of submissions allowed, if not unlimited.
	 * 
	 * @return The number of submissions allowed, or null if unlimited.
	 */
	Integer getNumSubmissionsAllowed();

	/**
	 * Access one of the questions, by question id.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return the question, or null if not found.
	 */
	AssessmentQuestion getQuestion(String questionId);

	/**
	 * Access the release date. Only after this date (if defined) is the assessment open for submission.
	 * 
	 * @return The assessment's release date, or null if there is none.
	 */
	Time getReleaseDate();

	/**
	 * Access the retract date. Only before this date (if defined) is the assessment open for submission.
	 * 
	 * @return The assessment's retract date, or null if there is none.
	 */
	Time getRetractDate();

	/**
	 * Access one of the sections, by id.
	 * 
	 * @param sectionId
	 *        The section id.
	 * @return the section, or null if not found.
	 */
	AssessmentSection getSection(String sectionId);

	/**
	 * Access the sections.
	 */
	List<? extends AssessmentSection> getSections();

	/**
	 * Access the lifecycle/type status of this assessment.
	 * 
	 * @return The assessment's lifecycle/type status.
	 */
	AssessmentStatus getStatus();

	/**
	 * Access the time limit for taking the assessment (seconds).
	 * 
	 * @return The assessment's time limit, or null for umlimited.
	 */
	Integer getTimeLimit();

	/**
	 * Access the title of this assessment.
	 * 
	 * @return The assessment's title.
	 */
	String getTitle();

	/**
	 * Access the sum of all possible points for this assessment.
	 * 
	 * @return The sum of all possible points for this assessment.
	 */
	Float getTotalPoints();

	/**
	 * Set the allow late submissions (after due date) setting for this assessment
	 * 
	 * @param value
	 *        TRUE if submissions are allowed after the due date, FALSE if not.
	 */
	void setAllowLateSubmit(Boolean value);

	/**
	 * Set the auto-submit (when time limit is expired) for this assessment.
	 * 
	 * @param value
	 *        TRUE if the assessment will auto-submit when time has expired, FALSE if not.
	 */
	void setAutoSubmit(Boolean value);

	/**
	 * Set the context of this assessment.
	 * 
	 * @param context
	 *        The assessment's context string.
	 */
	void setContext(String context);

	/**
	 * Set the creating uesr's id.
	 * 
	 * @param userId
	 *        The creating user's id.
	 */
	void setCreatedBy(String userId);

	/**
	 * Set the due date.
	 * 
	 * @param dueDate
	 *        The assessment's due date, or null to remove it.
	 */
	void setDueDate(Time dueDate);

	/**
	 * Set the feedback date.
	 * 
	 * @param feedbackDate
	 *        The assessment's feedback date, or null to remove it.
	 */
	void setFeedbackDate(Time feedbackDate);

	/**
	 * Set the feedback delivery.
	 * 
	 * @param delivery
	 *        The assessment's feedback delivery.
	 */
	void setFeedbackDelivery(FeedbackDelivery delivery);

	/**
	 * Set the "show correct answer" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show correct answer" feedback setting.
	 */
	void setFeedbackShowCorrectAnswer(Boolean value);

	/**
	 * Set the "show question" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show question" feedback setting.
	 */
	void setFeedbackShowQuestionFeedback(Boolean value);

	/**
	 * Set the "show question score" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show question score" feedback setting.
	 */
	void setFeedbackShowQuestionScore(Boolean value);

	/**
	 * Set the "show score" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show score" feedback setting.
	 */
	void setFeedbackShowScore(Boolean value);

	/**
	 * Set the "show statistics" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show statistics" feedback setting.
	 */
	void setFeedbackShowStatistics(Boolean value);

	/**
	 * Set the choice for which of multiple submissions should be the official graded one; either the lastest submission, or the highest graded one.
	 * 
	 * @param policy
	 *        The assessment's MultipleSubmissionSelectionPolicy, or null to clear the policy (to the default of USE_LATEST).
	 */
	void setMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy policy);

	/**
	 * Set the number of submissions allowed for limited submissions.
	 * 
	 * @param count
	 *        The number of submissions allowed, or null to make it unlimited.
	 */
	void setNumSubmissionsAllowed(Integer count);

	/**
	 * Set the release date. Only after this date (if defined) is the assessment open for submission.
	 * 
	 * @param date
	 *        The assessment's release date, or null if there is none.
	 */
	void setReleaseDate(Time date);

	/**
	 * Set the retract date. Only before this date (if defined) is the assessment open for submission.
	 * 
	 * @param date
	 *        The assessment's retract date, or null if there is none.
	 */
	void setRetractDate(Time date);

	/**
	 * Set the sections to (a deep copy of) this list of sections.
	 * 
	 * @param sections
	 *        The sections.
	 */
	void setSections(List<? extends AssessmentSection> sections);

	/**
	 * Set the lifecycle/type status of this assessment.
	 * 
	 * @param status
	 *        The assessment's lifecycle/type status.
	 */
	void setStatus(AssessmentStatus status);

	/**
	 * Set the time limit for taking the assessment (seconds).
	 * 
	 * @param limit
	 *        The time limit for the assessment, or null for unlimited.
	 */
	void setTimeLimit(Integer limit);

	/**
	 * Set the title of this assessment.
	 * 
	 * @param title
	 *        The assessment's title.
	 */
	void setTitle(String title);
}
