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
 * SubmissionService manages submissions.
 */
public interface SubmissionService
{
	/**
	 * Sort options for GetUserContextSubmissions()
	 */
	enum GetUserContextSubmissionsSort
	{
		dueDate_a, dueDate_d, status_a, status_d, title_a, title_d
	}

	/**
	 * Check if the current user is allowed to update or add answers or complete this submission.<br />
	 * Any hard deadlines are extended by a grace period to allow for inaccuracies in timing.<br />
	 * The user must match the submission user.<br />
	 * The submission must be incomplete, the assessment must be open, the user must have submit permission.
	 * 
	 * @param submission
	 *        The submission.
	 * @param userId
	 *        The user taking the assessment (if null, the current user is used).
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowCompleteSubmission(Submission submission, String userId);

	/**
	 * Check if the current user is allowed to review this submission.<br />
	 * The user must match the submission user.<br />
	 * The submission must be complete.
	 * 
	 * @param submission
	 *        The submission.
	 * @param userId
	 *        The user taking the assessment (if null, the current user is used).
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowReviewSubmission(Submission submission, String userId);

	/**
	 * Check if the current user is allowed to submit to the assessment.<br />
	 * If the user has a submission in progress, this returns true.<br />
	 * Otherwise, the assessment must be open, the user must have submit permission, and not yet submitted the max.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user taking the assessment (if null, the current user is used).
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowSubmit(Assessment assessment, String userId);

	/**
	 * Complete this submission. Use this instead of submitAnswer() when there's no answer information to also update.
	 * 
	 * @param submission
	 *        The the submission.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws SubmissionCompletedException
	 *         if the submission is already completed.
	 */
	void completeSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException;

	/**
	 * Check how many additional submissions are allowed to this assessment by this user.<br />
	 * If the user has no permission to submit, has submitted the maximum, or the assessment is closed for submissions as of this time, return 0.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return The count of remaining submissions allowed for this user to this assessment, or null if submissions are unlimited.
	 */
	Integer countRemainingSubmissions(Assessment assessment, String userId);

	/**
	 * Start an end-user in taking an assessment. If there is an incomplete submission already, re-enter that, else create a new one.
	 * 
	 * @param assessment
	 *        The published assessment to submit to.
	 * @param userId
	 *        The user taking the assessment (if null, the current user is used).
	 * @return The submission (found or created), or null if this was unsuccessful.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws AssessmentCompletedException
	 *         if an assessment has been submitted to by the user the maximum number of times.
	 */
	Submission enterSubmission(Assessment assessment, String userId) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException;

	/**
	 * Get the total scores for all completed submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return A List containing all the scores for completed submissions to this assessment, or an empty list if there are none.
	 */
	List<Float> getAssessmentScores(Assessment assessment);

	/**
	 * Get the total scores for this question from all completed submissions to the question's assessment.
	 * 
	 * @param question
	 *        The question.
	 * @return A List containing all the scores for this question from all completed submissions to the question's assessment, or an empty list if
	 *         there are none.
	 */
	List<Float> getQuestionScores(Question question);

	/**
	 * Access a submission by id. TODO: security
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission object, or null if not found.
	 */
	Submission getSubmission(String id);

	/**
	 * Access the assessment's submission count.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The assessment's submission count.
	 */
	SubmissionCounts getSubmissionCounts(Assessment assessment);

	/**
	 * TODO: rename find... Find the submissions to assignments in this context made by this user. Consider:
	 * <ul>
	 * <li>published assessments</li>
	 * <li>assessments in this context</li>
	 * <li>assessments this user can submit to and have submitted to</li>
	 * <li>the one (of many for this user) submission that will be the official (graded) (depending on the assessment settings, and submission time
	 * and score)</li>
	 * </ul>
	 * 
	 * @param context
	 *        The context to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @param sort
	 *        The sort order.
	 * @return A List<Submission> of the submissions that are the offical submissions for assessments in the context by this user, sorted.
	 */
	List<Submission> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort);

	/**
	 * Enter or update an answer to a question of an incomplete submission to an assessment. Auto grade. Updated realated info (such as the
	 * submission's score).<br />
	 * Complete the submission if indicated.
	 * 
	 * @param answer
	 *        The SubmissionAnswerAnswer containing the submitters answer information to a question
	 * @param completeAnswer
	 *        if TRUE, the answer is considerd answered and will be marked so.
	 * @param completeSubmission
	 *        if TRUE, the submission will be marked complete and submitted for grading
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws SubmissionCompletedException
	 *         if the submission is already completed.
	 */
	void submitAnswer(Answer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException;

	/**
	 * Enter or update a set of answers to questions of an incomplete submission to an assessment. Auto grade. Updated realated info (such as the
	 * submission's score).<br />
	 * Complete the submission if indicated.
	 * 
	 * @param answers
	 *        The List of SubmissionAnswerAnswer containing the submitters answer information to questions
	 * @param completeAnswers
	 *        if TRUE, the answers are considerd answered and will be marked so.
	 * @param completeSubmission
	 *        if TRUE, the submission will be marked complete and submitted for grading
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws SubmissionCompletedException
	 *         if the submission is already completed.
	 */
	void submitAnswers(List<Answer> answers, Boolean completeAnswers, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException;

	/**
	 * Update the assessment's gradebook entry for all users with their highest score.
	 * 
	 * @param assessment
	 *        The assessment to update.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to create the assessment.
	 */
	void updateGradebook(Assessment assessment) throws AssessmentPermissionException;
}
