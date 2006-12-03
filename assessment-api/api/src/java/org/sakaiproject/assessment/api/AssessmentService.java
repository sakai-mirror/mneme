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

import java.util.Collection;

/**
 * <p>
 * AssessmentService ...
 * </p>
 */
public interface AssessmentService
{
	/** The type string for this application: should not change over time as it may be stored in various parts of persistent entities. */
	static final String APPLICATION_ID = "sakai:assessment";

	/** This string starts the references to resources in this service. */
	static final String REFERENCE_ROOT = "/assessment";

	/** The security function used to check if users can submit to an assessment. */
	static final String SUBMIT_PERMISSION = "assessment.takeAssessment";

	/** The security function used to check if users can submit to an assessment. */
	static final String PUBLISH_PERMISSION = "assessment.createAssessment";

	/** Event tracking event for publishing an assessment. */
	static final String ASSESSMENT_PUBLISH = "assessment.publish";

	/** Event tracking event for adding a submission. */
	static final String SUBMIT_ADD = "submission.add";

	/** Event tracking event for entering a submission. */
	static final String SUBMIT_ENTER = "submission.enter";

	/** Event tracking event for re-entering a submission. */
	static final String SUBMIT_REENTER = "submission.reenter";

	/** Event tracking event for answering a question in a submission. */
	static final String SUBMIT_ANSWER = "submission.answer";

	/** Event tracking event for completing a submission. */
	static final String SUBMIT_COMPLETE = "submission.complete";

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Assessment Access
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Access an assessment by id. Assures that the full assessment information is populated. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, complete, or null if not found.
	 */
	Assessment getAssessment(String id);

	/**
	 * Access an assessment by id, but do not populate any information. Information will be populated as needed. TODO: security
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assessment object, or null if not found.
	 */
	Assessment idAssessment(String id);

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Submission Access
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Access a submission by id. TODO: security
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission object, complete, or null if not found.
	 */
	Submission getSubmission(String id);

	/**
	 * Access a submission by id, but do not populate any information. Information will be populated as needed. TODO: security
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission object, or null if not found.
	 */
	Submission idSubmission(String id);

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Delivery Support
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Check how many additional submissions are allowed to this assessment by this user.<br />
	 * If the user has no permission to submit, has submitted the maximum, or the assessment is closed for submissions as of this time, return 0.
	 * 
	 * @param assessmentId
	 *        The assessment id.
	 * @param userId
	 *        The user id.
	 * @return The count of remaining submissions allowed for this user to this assessment, -1 if it is unlimited, or null if we cannot find the assessment.
	 */
	Integer countRemainingSubmissions(String assessmentId, String userId);

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
	 * @return A collection <Assessment> of the assessments that qualify, or an empty collection if none do.
	 */
	Collection<Assessment> getAvailableAssessments(String context, String userId);

	/**
	 * Find the submissions to assignments in this context made by this user. Consider:
	 * <ul>
	 * <li>published assessments</li>
	 * <li>assessments in this context</li>
	 * <li>assessments this user can submit to and have submitted to</li>
	 * <li>the one (of many for this user) submission that will be the official (graded) (depending on the assessment settings, and submission time and score)</li>
	 * </ul>
	 * 
	 * @param context
	 *        The context to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @return A collection <Submission> of the submissions that are the offical submissions for assessments in the context by this user.
	 */
	Collection<Submission> getOfficialSubmissions(String context, String userId);

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Authoring Support
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Check if the current user is allowed to add an assessment in this context.
	 * 
	 * @param context
	 *        The context.
	 * @return TRUE if the user is allowed to add an assessment in this context, FALSE if not.
	 */
	Boolean allowAddAssessment(String context);

	/**
	 * Create a new Assessment object, currently detached from persistence.
	 * 
	 * @return a new, detached from persistence, assessment object.
	 */
	Assessment newAssessment();

	/**
	 * Create a new Assessment section for this assessment.
	 * 
	 * @param assessment
	 *        The assessment this section goes in.
	 * @return a new section that is in the assessment.
	 */
	AssessmentSection newSection(Assessment assessment);

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
	 * Create a new AssessmentAnswer object in this question part.
	 * 
	 * @param part
	 *        The part this answer is in.
	 * @return a new answer that is in this part.
	 */
	AssessmentAnswer newAssessmentAnswer(QuestionPart part);

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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Submission Support
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Create a new Submission object for this assessment.
	 * 
	 * @return a new submission.
	 */
	Submission newSubmission(Assessment assessment);

	/**
	 * Create a new Answer object for this submission.
	 * 
	 * @param submission
	 *        The submission.
	 * @return a new submission answer.
	 */
	SubmissionAnswer newSubmissionAnswer(Submission submission);

	/**
	 * Creaet a new Entry object for this submission answer
	 * 
	 * @param SubmissionAnswer
	 * @return
	 */
	SubmissionAnswerEntry newEntry(SubmissionAnswer answer);

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
	void addSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException;

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
	Submission enterSubmission(Assessment assessment, String userId) throws AssessmentPermissionException,
			AssessmentClosedException, AssessmentCompletedException;

	/**
	 * Enter or update an answer to a question of an incomplete submission to an assessment. Auto grade. Updated realated info (such as the submission's score).<br />
	 * Complete the submission if indicated.
	 * 
	 * @param answer
	 *        The SubmissionAnswerAnswer containing the submitters answer information to a question
	 * @param completeSubmission
	 *        if TRUE, the submission will be marked complete and submitted for grading
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to submit to this assessment.
	 * @throws AssessmentClosedException
	 *         if assessment is not currently open for submission.
	 * @throws SubmissionCompletedException
	 *         if the submission is already completed.
	 */
	void submitAnswer(SubmissionAnswer answer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException;

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
	void completeSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			SubmissionCompletedException;
}
