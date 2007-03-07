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

/**
 * <p>
 * AssessmentService ...
 * </p>
 */
public interface AssessmentService
{
	/**
	 * Sort options for getAvailableAssessments()
	 */
	enum GetAvailableAssessmentsSort
	{
		dueDate_a, dueDate_d, title_a, title_d
	}

	enum GetOfficialSubmissionsSort
	{
		feedbackDate_a, feedbackDate_d, score_a, score_d, submittedDate_a, submittedDate_d, time_a, time_d, title_a, title_d
	}

	enum GetUserContextSubmissionsSort
	{
		dueDate_a, dueDate_d, status_a, status_d, title_a, title_d
	}

	/**
	 * The type string for this application: should not change over time as it may be stored in various parts of persistent entities.
	 */
	static final String APPLICATION_ID = "sakai:assessment";

	/** Event tracking event for publishing an assessment. */
	static final String ASSESSMENT_PUBLISH = "assessment.publish";

	/** The sub-type for assessment in references (/assessment/assessment/...) */
	static final String ASSESSMENT_TYPE = "assessment";

	/** The security function used to check if users can publish an assessment. */
	static final String PUBLISH_PERMISSION = "assessment.createAssessment";

	/** This string starts the references to resources in this service. */
	static final String REFERENCE_ROOT = "/assessment";

	/** The sub-type for submissions in references (/assessment/submission/...) */
	static final String SUBMISSION_TYPE = "submission";

	/** Event tracking event for adding a submission. */
	static final String SUBMIT_ADD = "submission.add";

	/** Event tracking event for answering a question in a submission. */
	static final String SUBMIT_ANSWER = "submission.answer";

	/** Event tracking event for completing a submission. */
	static final String SUBMIT_COMPLETE = "submission.complete";

	/*************************************************************************************************************************************************
	 * Assessment Access
	 ************************************************************************************************************************************************/

	/** Event tracking event for entering a submission. */
	static final String SUBMIT_ENTER = "submission.enter";

	/** The security function used to check if users can submit to an assessment. */
	static final String SUBMIT_PERMISSION = "assessment.takeAssessment";

	/*************************************************************************************************************************************************
	 * Submission Access
	 ************************************************************************************************************************************************/

	/** Event tracking event for re-entering a submission. */
	static final String SUBMIT_REENTER = "submission.reenter";

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

	/*************************************************************************************************************************************************
	 * Delivery Support
	 ************************************************************************************************************************************************/

	/**
	 * Create a new persistent submission from the given information.
	 * 
	 * @param submission
	 *        The information from which to make the new submission.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to the assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws AssessmentCompletedException
	 *         if an assessment has been submitted to by the user the maximum number of times.
	 */
	void addSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException, AssessmentCompletedException;

	/**
	 * Check if the current user is allowed to add an assessment in this context.
	 * 
	 * @param context
	 *        The context.
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowAddAssessment(String context);

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
	 * Count the published assessments in the context - all of them!
	 * 
	 * @param context
	 *        The context.
	 * @return The count of published assessments in the context.
	 */
	Integer countAssessments(String context);

	/**
	 * Check how many additional submissions are allowed to this assessment by this user.<br />
	 * If the user has no permission to submit, has submitted the maximum, or the assessment is closed for submissions as of this time, return 0.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return The count of remaining submissions allowed for this user to this assessment, -1 if it is unlimited, or null if we cannot find the
	 *         assessment.
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

	/*************************************************************************************************************************************************
	 * Authoring Support
	 ************************************************************************************************************************************************/

	/**
	 * Access an assessment by id. Assures that the full assessment information is populated. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, complete, or null if not found.
	 */
	Assessment getAssessment(String id);

	/**
	 * Get the total scores for all completed submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return A List containing all the scores for completed submissions to this assessment, or an empty list if there are none.
	 */
	List<Float> getAssessmentScores(Assessment assessment);

	/**
	 * Find the assessments that are available for taking in this context by this user. Consider:
	 * <ul>
	 * <li>published assessments</li>
	 * <li>assessments in this context</li>
	 * <li>assessments this user has permission to take</li>
	 * <li>assessments that are released as of the time specified and not yet retracted</li>
	 * <li>assessments that, based on the due date and late submission policy, still can be submitted to</li>
	 * <li>assessments that, based on their re-submit policy and count, and this user's count of submissions, can be submitted to again by this user</li>
	 * <li>(assessments that accept late submissions and are past due date that the use has submitted to already are not included)</li>
	 * </ul>
	 * 
	 * @param context
	 *        The context to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @param sort
	 *        The sort for the return List (title_a if null).
	 * @return A List<Assessment> of the assessments that qualify, sorted, or an empty collection if none do.
	 */
	List<Assessment> getAvailableAssessments(String context, String userId, GetAvailableAssessmentsSort sort);

	/**
	 * Find the submissions to assignments in this context made by this user. Consider:
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
	 *        The sort for the return list (title_a if null).
	 * @return A List<Submission> of the submissions that are the offical submissions for assessments in the context by this user, sorted.
	 */
	List<Submission> getOfficialSubmissions(String context, String userId, GetOfficialSubmissionsSort sort);

	/**
	 * Get the total scores for this question from all completed submissions to the question's assessment.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return A List containing all the scores for this question from all completed submissions to the question's assessment, or an empty list if
	 *         there are none.
	 */
	List<Float> getQuestionScores(String questionId);

	/**
	 * Access a submission by id. TODO: security
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission object, complete, or null if not found.
	 */
	Submission getSubmission(String id);

	/**
	 * TODO: Find the submissions to assignments in this context made by this user. Consider:
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
	 * Access an assessment by id, but do not populate any information. Information will be populated as needed. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, or null if not found.
	 */
	Assessment idAssessment(String id);

	/*************************************************************************************************************************************************
	 * Submission Support
	 ************************************************************************************************************************************************/

	/**
	 * Access a submission by id, but do not populate any information. Information will be populated as needed. TODO: security
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission object, or null if not found.
	 */
	Submission idSubmission(String id);

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

	/**
	 * Create a new Submission object for this assessment.
	 * 
	 * @return a new submission.
	 */
	Submission newSubmission(Assessment assessment);

	/**
	 * Create a new Answer object for this submission, that answers this question.
	 * 
	 * @param submission
	 *        The submission.
	 * @param question
	 *        The assessment question that this answers.
	 * @return a new submission answer.
	 */
	SubmissionAnswer newSubmissionAnswer(Submission submission, AssessmentQuestion question);

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
	void submitAnswer(SubmissionAnswer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
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
	void submitAnswers(List<SubmissionAnswer> answers, Boolean completeAnswers, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException;
}
