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

import org.sakaiproject.time.api.Time;

/**
 * <p>
 * Submission is a user's response to an assessment. Each time the user takes the assessment, a new submission is made.
 * </p>
 */
public interface Submission
{
	/**
	 * If the submission is "over" (as defined by getIsOver()), close it. Update the submission to reflect its changed status.
	 * 
	 * @return TRUE if it was over and is now closed, FALSE if not.
	 */
	Boolean completeIfOver();

	/**
	 * Find (or create) the answer for this question.
	 * 
	 * @param question
	 *        The assessment question for which this will be an answer.
	 * @return The answer for this question.
	 */
	Answer getAnswer(Question question);

	/**
	 * Access the submission's answers.
	 * 
	 * @return The submission's answers.
	 */
	List<? extends Answer> getAnswers();

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
	 * Get the assessment submission status for a user's submission to an assessment (the submissions's user and assessment)
	 * 
	 * @return The assessment submission status for a user's submission to an assessment.
	 */
	AssessmentSubmissionStatus getAssessmentSubmissionStatus();

	/**
	 * Get the best submission to this same assignment from the same user. May not be known.
	 * 
	 * @return The best submission to this same assignment from the same user, or NULL if not known.
	 */
	Submission getBest();

	/**
	 * Access the time taken to make this submission, in ms, based on the start and lastest submission dates.
	 * 
	 * @return The time taken to make this submission, in ms (or null if it was not timed or not available).
	 */
	Long getElapsedTime();

	/**
	 * Access the evaluation for the overall submission.
	 * 
	 * @return The evaluation for the overall submission, or null if there is none.
	 */
	SubmissionEvaluation getEvaluation();

	/**
	 * Access the expiration information for the submission.
	 * 
	 * @return The expiration information for the submission.
	 */
	Expiration getExpiration();

	/**
	 * Find the first assessment question that has not been marked as complete.
	 * 
	 * @return The first incomplete assessment question, or null if they have all been completed.
	 */
	Question getFirstIncompleteQuestion();

	/**
	 * Access the id of this submission.
	 * 
	 * @return The submission's id.
	 */
	String getId();

	/**
	 * Check if all the questions have been answered and not marked for review.
	 * 
	 * @return TRUE if the asssessment's questions are all answered, FALSE if not.
	 */
	Boolean getIsAnswered();

	/**
	 * Access the complete flag for the submission.
	 * 
	 * @return TRUE if the submission is complete, FALSE if still in progress.
	 */
	Boolean getIsComplete();

	/**
	 * Check if this the answer to this question has been marked "complete" - this is different than "fully answered" as in getIsAnswered().
	 * 
	 * @param question
	 *        The assessment question
	 * @return TRUE if the question has been marked "complete", FALSE if not.
	 */
	Boolean getIsCompleteQuestion(Question question);

	/**
	 * Check if the submission has been graded or not.
	 * 
	 * @return TRUE if the assessment has been graded, FALSE if not.
	 */
	Boolean getIsGraded();

	/**
	 * Check if the submission is past its time limit, due or accept until date, or is to an assessment that is inactive.
	 * 
	 * @param qasOf
	 *        The effective time of the check.
	 * @param grace
	 *        A grace period (in ms) to extend any hard deadline or timeout.
	 * @return TRUE if the submission is over, FALSE if not.
	 */
	Boolean getIsOver(Time asOf, long grace);

	/**
	 * Check if the submission may be started - the user must have permission, the submission must be a placeholder, the assessment must be open.
	 * 
	 * @return TRUE if the submission may be started, FALSE if not.
	 */
	Boolean getMayBegin();

	/**
	 * Check if the submission may be started for an nth attempt - the user must have permission, the submission must be a complete, the sibling count
	 * must be < the assessment's limit, and the assessment must be open.
	 * 
	 * @return TRUE if the submission may be started, FALSE if not.
	 */
	Boolean getMayBeginAgain();

	/**
	 * Check if the submission may be re-entered for more work - the user must be the submission user, the submission must be incomplete.
	 * 
	 * @return TRUE if the submission may be re-entered, FALSE if not.
	 */
	Boolean getMayContinue();

	/**
	 * Check if the submission may be reviewed - the user must be the submission user, and the submission must be complete and not from a retracted
	 * assessment.
	 * 
	 * @return TRUE if the submission may be reviewed, FALSE if not.
	 */
	Boolean getMayReview();

	/**
	 * Check if the submission may be reviewed at some later point - the user must be the submission user, and the submission must be complete and not
	 * from a retracted assessment, and the assessment must be set to eventually allow review.
	 * 
	 * @return TRUE if the submission may be reviewed later, FALSE if not.
	 */
	Boolean getMayReviewLater();

	/**
	 * Access the reference of this submission.
	 * 
	 * @return The submission's reference.
	 */
	String getReference();

	/**
	 * Get the total count of submissions, including this one, to this same assignment from the same user. May not be known.
	 * 
	 * @return The total count of submissions to the assignment by the user, or NULL if not known.
	 */
	Integer getSiblingCount();

	/**
	 * Access the start date for this submission.
	 * 
	 * @return the start date for this submission, or null if there is none.
	 */
	Time getStartDate();

	/**
	 * Access the submission date for this submission.
	 * 
	 * @return the submission date for this submission, or null if there is none.
	 */
	Time getSubmittedDate();

	/**
	 * Access the total score of the submission - the total of the auto scores from the answers and the evaluation scores from the answers and
	 * overall.
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
	 * Compute the 'over' date for the submission - when it would be over based on time limit, due or accept-until dates.
	 * 
	 * @return The 'over' time for the submission, or NULL if there is none.
	 */
	Time getWhenOver();

	/**
	 * Set the complete flag for the submission.
	 * 
	 * @param complete
	 *        True if the submission is complete, False if it is still in progress.
	 */
	void setIsComplete(Boolean complete);

	/**
	 * Set the graded flag for the submission.
	 * 
	 * @param graded
	 *        True if the submission is graded, False if it is not yet.
	 */
	void setIsGraded(Boolean graded);

	/**
	 * Set the start date for this submission - the earliest date that this submission was altered by the submitter.
	 * 
	 * @param startDate
	 *        the submission date for this submission.
	 */
	void setStartDate(Time startDate);

	/**
	 * Set the submission date for this submission - the latest date that this submission was altered by the submitter.
	 * 
	 * @param submittedDate
	 *        the submission date for this submission.
	 */
	void setSubmittedDate(Time submittedDate);
}
