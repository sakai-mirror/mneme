/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2007 The Sakai Foundation.
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
 * Submission is a user's response to an assessment. Each time the user takes the assessment, a new submission is made.
 * </p>
 */
public interface Submission
{
	/**
	 * Find (or create) the answer for this question.
	 * 
	 * @param question
	 *        The assessment question for which this will be an answer.
	 * @return The answer for this question.
	 */
	SubmissionAnswer getAnswer(AssessmentQuestion question);

	/**
	 * Access the submission's answers.
	 * 
	 * @return The submission's answers.
	 */
	List<? extends SubmissionAnswer> getAnswers();

	/**
	 * Access the sum of all auto score's for this answers of this submission.
	 * 
	 * @return The auto score for this submission, or 0 if there is none.
	 */
	Float getAnswersAutoScore();

	/**
	 * Access the assessment that this is a submission to.
	 * 
	 * @return The assessment this is a submission to.
	 */
	Assessment getAssessment();

	/**
	 * Access the confirmation string for this submission.
	 * 
	 * @return The confirmation string for this submission.
	 */
	String getConfirmation();

	/**
	 * Access the number of ms from now that the time limit on this submission will be up.
	 * 
	 * @return The number of ms from now that the time limit on this submission will be up, 0 if it has already expired, or null if
	 *         it is not timed.
	 */
	Long getDurationTillExpires();

	/**
	 * Access the time taken to make this submission, in seconds, based on the start and lastest submission dates.
	 * 
	 * @return The time taken to make this submission, in seconds (or null if it was not timed or not available).
	 */
	Long getElapsedTime();

	/**
	 * Find the first assessment question that has not been seen yet in this submission.
	 * 
	 * @return The first unseen assessment question, or null if they have all been seen.
	 */
	AssessmentQuestion getFirstUnseenQuestion();

	/**
	 * Access the id of this submission.
	 * 
	 * @return The submission's id.
	 */
	String getId();

	/**
	 * Access the complete flag for the submission.
	 * 
	 * @return TRUE if the submission is complete, FALSE if still in progress.
	 */
	Boolean getIsComplete();

	/**
	 * Check if this question has ever been seen in this submission - answered or not.
	 * 
	 * @param question
	 *        The assessment question
	 * @return TRUE if the question has ever been seen, false if not.
	 */
	Boolean getSeenQuestion(AssessmentQuestion question);

	/**
	 * Access the start date for this submission.
	 * 
	 * @return the start date for this submission, or null if there is none.
	 */
	Time getStartDate();

	/**
	 * Access the submission status.
	 * 
	 * @return The submission status.
	 */
	Integer getStatus();

	/**
	 * Access the submission date for this submission.
	 * 
	 * @return the submission date for this submission, or null if there is none.
	 */
	Time getSubmittedDate();

	/**
	 * Access the total score of the submission - the total of the auto scores from the answers and the evaluation manual scores
	 * from the answers and overall.
	 * 
	 * @return The total score of the submission.
	 */
	Float getTotalScore();

	/**
	 * Access the user who made this submission.
	 * 
	 * @return The user id of the user who made the submission.
	 */
	String getUserId();

	/**
	 * Set the submission's answers to (a deep copy of) this list of SubmissionAnswer.
	 * 
	 * @param answers
	 *        The submission's answers.
	 */
	void setAnswers(List<? extends SubmissionAnswer> answers);

	/**
	 * Set the assessment for this submission.
	 * 
	 * @param assessment
	 *        The assessment for this submission.
	 */
	void setAssessment(Assessment assessment);

	/**
	 * Set the complete flag for the submission.
	 * 
	 * @param complete
	 *        True if the submission is complete, False if it is still in progress.
	 */
	void setIsComplete(Boolean complete);

	/**
	 * Set the start date for this submission - the earliest date that this submission was altered by the submitter.
	 * 
	 * @param startDate
	 *        the submission date for this submission.
	 */
	void setStartDate(Time startDate);

	/**
	 * Set the submission status.
	 * 
	 * @param status
	 *        The submission status.
	 */
	void setStatus(Integer status);

	/**
	 * Set the submission date for this submission - the latest date that this submission was altered by the submitter.
	 * 
	 * @param submittedDate
	 *        the submission date for this submission.
	 */
	void setSubmittedDate(Time submittedDate);

	/**
	 * Set the user id for this submission.
	 * 
	 * @param userId
	 *        The user id who made this submission.
	 */
	void setUserId(String userId);
}
