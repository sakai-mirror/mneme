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
	 * Sort options for findAssessmentSubmissionsSort() status is GradingSubmissionStatus
	 */
	enum FindAssessmentSubmissionsSort
	{
		final_a, final_d, status_a, status_d, userName_a, userName_d
	}

	/**
	 * Sort options for GetUserContextSubmissions() status is AssessmentSubmissionStatus
	 */
	enum GetUserContextSubmissionsSort
	{
		dueDate_a, dueDate_d, status_a, status_d, title_a, title_d
	}

	/** Phantom submissions start with this, and are followed by the assessment id, another slash, then the user id. */
	static final String PHANTOM_PREFIX = "phantom/";

	/**
	 * Check if the current user is allowed to update or add answers or complete this submission.<br />
	 * Any hard deadlines are extended by a grace period to allow for inaccuracies in timing.<br />
	 * The user must match the submission user.<br />
	 * The submission must be incomplete, the assessment must be open, the user must have submit permission.
	 * 
	 * @param submission
	 *        The submission.
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowCompleteSubmission(Submission submission);

	/**
	 * Check if the current user is allowed to evaluate (grade) submissions in the context.<br />
	 * 
	 * @param context
	 *        The context.
	 * @return TRUE if the user is allowed to evaluate submissions in the context, FALSE if not.
	 */
	Boolean allowEvaluate(String context);

	/**
	 * Check if the current user is allowed to evaluate (grade) this submission.<br />
	 * The submission must be complete.<br />
	 * 
	 * @param submission
	 *        The submission.
	 * @return TRUE if the user is allowed to evaluate the submission, FALSE if not.
	 */
	Boolean allowEvaluate(Submission submission);

	/**
	 * Check if the current user is allowed to review this submission.<br />
	 * The user must match the submission user.<br />
	 * The submission must be complete.
	 * 
	 * @param submission
	 *        The submission.
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowReviewSubmission(Submission submission);

	/**
	 * Check if the current user is allowed to submit to the assessment.<br />
	 * If the user has a submission in progress, this returns true.<br />
	 * Otherwise, the assessment must be open, the user must have submit permission, and not yet submitted the max.
	 * 
	 * @param submission
	 *        The current submission set for this user to the assessment (getUserAssessmentSubmission).
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowSubmit(Submission submission);

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
	 * Count the submissions to the assignment made by all users.<br />
	 * If a user has not yet submitted, a phantom one for that user is included. <br />
	 * Optionally group multiple submissions from a single user and select the in-progress or "best" one.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param official
	 *        if TRUE, clump multiple submissions by the same user behind the best one, else include all.
	 * @return A sorted List<Submission> of the submissions for the assessment.
	 */
	Integer countAssessmentSubmissions(Assessment assessment, Boolean official);

	/**
	 * Count the submission answers to the assignment and question made by all users.<br />
	 * Optionally group multiple submissions from a single user and select the in-progress or "best" one.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param question
	 *        The question.
	 * @param official
	 *        if TRUE, clump multiple submissions by the same user behind the best one, else include all.
	 * @return A sorted List<Answer> of the answers.
	 */
	Integer countSubmissionAnswers(Assessment assessment, Question question, Boolean official);

	/**
	 * Start an end-user in taking an assessment. If there is an incomplete submission already, re-enter that, else create a new one.
	 * 
	 * @param submission
	 *        The current submission set for this user to the assessment (getUserAssessmentSubmission).
	 * @return The submission (found or created), or null if this was unsuccessful.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws AssessmentCompletedException
	 *         if an assessment has been submitted to by the user the maximum number of times.
	 */
	Submission enterSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException, AssessmentCompletedException;

	/**
	 * Record the evaluation changes made to these answers.
	 * 
	 * @param answers
	 *        The List of Answers with their Evaluations.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to evaluate these answers.
	 */
	void evaluateAnswers(List<Answer> answers) throws AssessmentPermissionException;

	/**
	 * Record the evaluation changes made to the overall submission and the submission's answers.
	 * 
	 * @param submission
	 *        The submission
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to evaluate this submission.
	 */
	void evaluateSubmission(Submission submission) throws AssessmentPermissionException;

	/**
	 * Apply an evaluation to all the official completed submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param comment
	 *        The overall comment. If null, no change to comment is made.
	 * @param score
	 *        The overall score adjustment. If null, no change to score is made.
	 * @param markEvaluated
	 *        If TRUE, mark them all as evaluated, otherwise no change to evaluated is made.
	 * @throws AssessmentPermissionException
	 *         If the current user is not allowed to save this Submission.
	 */
	void evaluateSubmissions(Assessment assessment, String comment, Float score, Boolean markEvaluated) throws AssessmentPermissionException;

	/**
	 * Find the submissions to the assignment made by all users.<br />
	 * If a user has not yet submitted, a phantom one for that user is included. <br />
	 * Optionally group multiple submissions from a single user and select the in-progress or "best" one.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param sort
	 *        The sort order.
	 * @param official
	 *        if TRUE, clump multiple submissions by the same user behind the best one, else include all.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return A sorted List<Submission> of the submissions for the assessment.
	 */
	List<Submission> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort, Boolean official, Integer pageNum,
			Integer pageSize);

	/**
	 * Find the submission answers to the assignment and question made by all users.<br />
	 * Optionally group multiple submissions from a single user and select the in-progress or "best" one.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param question
	 *        The question.
	 * @param sort
	 *        The sort order.
	 * @param official
	 *        if TRUE, clump multiple submissions by the same user behind the best one, else include all.
	 * @param pageNum
	 *        The page number (1 based) to display, or null to disable paging and get them all.
	 * @param pageSize
	 *        The number of items for the requested page, or null if we are not paging.
	 * @return A sorted List<Answer> of the answers.
	 */
	List<Answer> findSubmissionAnswers(Assessment assessment, Question question, FindAssessmentSubmissionsSort sort, Boolean official,
			Integer pageNum, Integer pageSize);

	/**
	 * Check if there are any completed submissions that have any null scores for answers for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if there are unscored submissions to this assessment, FALSE if not.
	 */
	Boolean getAssessmentHasUnscoredSubmissions(Assessment assessment);

	/**
	 * Check if this assessment's completed submissions are all released.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if the assessment's completed submissions are all released, FALSE if not.
	 */
	Boolean getAssessmentIsFullyReleased(Assessment assessment);

	/**
	 * Check if there are any completed submissions that have any null scores for answers to this question for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param question
	 *        The question.
	 * @return TRUE if there are unscored submissions to this assessment, FALSE if not.
	 */
	Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question);

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
	 * Get the submission to the assignment made by this user.<br />
	 * Returns the official submission, or one in progress, or a blank one if not yet started.<br />
	 * The sibling count is set.
	 * 
	 * @param assessment
	 *        The assessment to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @return The user's submission for this assessment.
	 */
	Submission getUserAssessmentSubmission(Assessment assessment, String userId);

	/**
	 * Get the submissions to assignments in this context made by this user. Consider:
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
	 * Remove all incomplete submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to remove these submissions.
	 */
	void removeIncompleteAssessmentSubmissions(Assessment assessment) throws AssessmentPermissionException;

	/**
	 * Check if any submissions in any state exist for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if there are any submissions to the assesment.
	 */
	Boolean submissionsExist(Assessment assessment);

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
	 *        The List of Answers.
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
