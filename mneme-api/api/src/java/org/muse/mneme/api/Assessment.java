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

import org.sakaiproject.entity.api.Reference;
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
	 * Check if the provided password (clear text) matches the defined password for the assessment.
	 * 
	 * @param password
	 *        The clear text password as entered.
	 * @return TRUE if the password is a match, false if not.
	 */
	Boolean checkPassword(String password);

	/**
	 * Access allow late submissions (after due date) setting for this assessment.
	 * 
	 * @return TRUE if submissions are allowed after the due date, FALSE if not.
	 */
	Boolean getAllowLateSubmit();

	/**
	 * Access the attachment list for the assessment.
	 * 
	 * @return The List of Reference objects that are the attachments for the assessment, or an empty list if there are no attachments.
	 */
	List<Reference> getAttachments();

	/**
	 * Access the auto-submit (when time limit is expired) for this assessment.
	 * 
	 * @return TRUE if the assessment will auto-submit when time has expired, FALSE if not.
	 */
	Boolean getAutoSubmit();

	/**
	 * Compute the submit closed date. This is the date the assignment is retracted, or the date it is due if late submissions are not accepted,
	 * whichever is first.<br />
	 * 
	 * @return The computed submit close date, or null if there is none.
	 */
	Time getClosedDate();

	/**
	 * Access the context of this assessment.
	 * 
	 * @return The assessment's context string.
	 */
	String getContext();

	/**
	 * Access the continuous numbering flag that controlls the numbering of questions across section boundaries.
	 * 
	 * @return TRUE if numbering is continuous across the section boundaries, FALSE if numbering resets for each section.
	 */
	Boolean getContinuousNumbering();

	/**
	 * Access the creating user's id.
	 * 
	 * @return The creating users id.
	 */
	String getCreatedBy();

	/**
	 * Access the description of this assessment.
	 * 
	 * @return The assessment's description.
	 */
	String getDescription();

	/**
	 * Access the due date. Submissions after this date are considered late, if they are accepted at all.
	 * 
	 * @return The assessment's due date, or null if there is none.
	 */
	Time getDueDate();

	/**
	 * Access the number of ms from now that the due date on this assessment will be reached.
	 * 
	 * @return The number of ms from now that the due date on this assessment will be reached, 0 if it has already been reached, or null if it has no
	 *         due date.
	 */
	Long getDurationTillDue();

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
	 * Access the answer level feedback setting; controls if we should show the answer level feedback to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the answer level feedback as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowAnswerFeedback();

	/**
	 * Access the "show correct answer" feedback setting; controls if we should show the correct answer to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the correct answer as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowCorrectAnswer();

	/**
	 * Access the question level feedback setting; controls if we should show the question level feedback to the assessment taker as part of feedback.
	 * 
	 * @return TRUE if we should show the question level feedback as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowQuestionFeedback();

	/**
	 * Access the "show question score" feedback setting; controls if we should show the individual question scores to the assessment taker as part of
	 * feedback.
	 * 
	 * @return TRUE if we should show the individual question scores as part of feedback, FALSE if not.
	 */
	Boolean getFeedbackShowQuestionScore();

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
	 * Access the gradebook integration flag; is TRUE, the assessment's grades are placed into the Gradebook.
	 * 
	 * @return TRUE if the assessment's grades are to be placed into the Gradebook, FALSE if not.
	 */
	Boolean getGradebookIntegration();

	/**
	 * Access the id of this assessment.
	 * 
	 * @return The assessment's id.
	 */
	String getId();

	/**
	 * Check if the assessment is closed - past retract or hard deadline.
	 * 
	 * @return TRUE if closed, FALSE if not.
	 */
	Boolean getIsClosed();

	/**
	 * Access the choice for which of multiple submissions should be the official graded one; either the lastest submission, or the highest graded
	 * one.
	 * 
	 * @return The assessment's MultipleSubmissionSelectionPolicy, or null if we cannot find it.
	 */
	MultipleSubmissionSelectionPolicy getMultipleSubmissionSelectionPolicy();

	/**
	 * Access the count of questions in all sections.
	 * 
	 * @return The count of questions in all sections.
	 */
	Integer getNumQuestions();

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
	 * Access the defined password.
	 * 
	 * @return The password, or null if not defined.
	 */
	String getPassword();

	/**
	 * Access one of the questions, by question id.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return the question, or null if not found.
	 */
	AssessmentQuestion getQuestion(String questionId);

	/**
	 * Access the question presentation setting which determines how many questions (one, a section's worth or all) to put on each page of the
	 * assessment.
	 * 
	 * @return The question presentation setting which determines how many questions (one, a section's worth or all) to put on each page of the
	 *         assessment.
	 */
	QuestionPresentation getQuestionPresentation();

	/**
	 * Access the questions across all sections. The order will be in section order, and in each section, random order (if enabled) based on the
	 * current user.
	 * 
	 * @return The questions across all sections. The order will be in section order, and in each section, random order (if enabled) based on the
	 *         current user.
	 */
	List<? extends AssessmentQuestion> getQuestions();

	/**
	 * Access the random access flag that controlls an assessment taker's random or linear access to the questions of the assessment.
	 * 
	 * @return TRUE if random question access is supported, FALSE if only linear question access is supported.
	 */
	Boolean getRandomAccess();

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
	 * Get the total scores for all completed submissions to this assessment.
	 * 
	 * @param assessmentId
	 *        The assessment id.
	 * @return A List containing all the scores for completed submissions to this assessment, or an empty list if there are none.
	 */
	List<Float> getScores();

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
	 * Access the submission that is referencing this assessment (so we can answer submission specific questions like question order).
	 * <p>
	 * Note: this will be null unless the assessment is accessed throught Submission.getAssessment().
	 * </p>
	 * 
	 * @return The submission, or null if there is none.
	 */
	Submission getSubmissionContext();

	/**
	 * Access the rich-text / html message to display when submitted.
	 * 
	 * @return The rich-text / html message to display when submitted.
	 */
	String getSubmitMessage();

	/**
	 * Access the URL to provide when submitted.
	 * 
	 * @return The URL to provide when submitted.
	 */
	String getSubmitUrl();

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
	 * Set the attachments to (a deep copy of) this list of attachments.
	 * 
	 * @param attachments
	 *        The attachments.
	 */
	void setAttachments(List<Reference> attachments);

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
	 * Set the continuous numbering flag that controlls the numbering of questions across section boundaries.
	 * 
	 * @param setting
	 *        TRUE if numbering is continuous across the section boundaries, FALSE if numbering resets for each section.
	 */
	void setContinuousNumbering(Boolean setting);

	/**
	 * Set the creating uesr's id.
	 * 
	 * @param userId
	 *        The creating user's id.
	 */
	void setCreatedBy(String userId);

	/**
	 * Set the description of this assessment.
	 * 
	 * @param title
	 *        The assessment's description.
	 */
	void setDescription(String description);

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
	 * Set the answer level feedback setting.
	 * 
	 * @param value
	 *        The assessment's answer level feedback setting.
	 */
	void setFeedbackShowAnswerFeedback(Boolean value);

	/**
	 * Set the "show correct answer" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show correct answer" feedback setting.
	 */
	void setFeedbackShowCorrectAnswer(Boolean value);

	/**
	 * Set the question level feedback setting.
	 * 
	 * @param value
	 *        The assessment's question level feedback setting.
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
	 * Set the "show statistics" feedback setting.
	 * 
	 * @param value
	 *        The assessment's "show statistics" feedback setting.
	 */
	void setFeedbackShowStatistics(Boolean value);

	/**
	 * Set the gradebook integration flag; is TRUE, the assessment's grades are placed into the Gradebook.
	 * 
	 * @param value
	 *        TRUE if the assessment's grades are to be placed into the Gradebook, FALSE if not.
	 */
	void setGradebookIntegration(Boolean value);

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
	 * Set the password.
	 * 
	 * @param password
	 *        The password.
	 */
	void setPassword(String password);

	/**
	 * Set the question presentation setting which determines how many questions (one, a section's worth or all) to put on each page of the
	 * assessment.
	 * 
	 * @value The question presentation setting which determines how many questions (one, a section's worth or all) to put on each page of the
	 *        assessment.
	 */
	void setQuestionPresentation(QuestionPresentation value);

	/**
	 * Set the random access flag that controlls an assessment taker's random or linear access to the questions of the assessment.
	 * 
	 * @param setting
	 *        TRUE if random question access is supported, FALSE if only linear question access is supported.
	 */
	void setRandomAccess(Boolean setting);

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
	 * Set the rich-text / html message to display when submitted.
	 * 
	 * @param message
	 *        The rich-text / html message to display when submitted.
	 */
	void setSubmitMessage(String message);

	/**
	 * Set the URL to provide when submitted.
	 * 
	 * @param url
	 *        The URL to provide when submitted.
	 */
	void setSubmitUrl(String url);

	/**
	 * Set the time limit for taking the assessment (seconds).
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
}
