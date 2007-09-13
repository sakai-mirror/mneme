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

package org.muse.mneme.impl;

import java.util.List;

import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCounts;
import org.muse.mneme.api.SubmissionService.FindAssessmentSubmissionsSort;
import org.muse.mneme.api.SubmissionService.GetUserContextSubmissionsSort;
import org.sakaiproject.tool.api.SessionManager;

/**
 * SubmissionStorage defines the storage interface for Submissions.
 */
public interface SubmissionStorage
{
	/**
	 * Count the completed submissions by this user to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user.
	 * @return The count of completed submissions by this user to this assessment.
	 */
	Integer countCompleteSubmissions(Assessment assessment, String userId);

	/**
	 * Find the submissions to the assignment made by all users.<br />
	 * If a user has not yet submitted, an empty one for that user is included. <br />
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param sort
	 *        The sort order.
	 * @return A sorted List<Submission> of the submissions for the assessment.
	 */
	List<SubmissionImpl> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort);

	/**
	 * Check if the completed submissions to this assessment are all released.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if the completed submissions to this assessment are all released, FALSE if not.
	 */
	Boolean getAssessmentIsFullyReleased(Assessment assessment);

	/**
	 * Access all the submission scores to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return A list of the submission scores to this assessment.
	 */
	List<Float> getAssessmentScores(Assessment assessment);

	/**
	 * Get all the in-progress (open) submissions (all users, all assessments, all contexts).
	 * 
	 * @return The list of all in-progress (open) submissions.
	 */
	List<SubmissionImpl> getOpenSubmissions();

	/**
	 * Access all the submission scores to this question.
	 * 
	 * @param question
	 *        The question.
	 * @return A list of the submission scores to this question.
	 */
	List<Float> getQuestionScores(Question question);

	/**
	 * Access a submission by id.
	 * 
	 * @param id
	 *        the submission id.
	 * @return The submission with this id, or null if not found.
	 */
	SubmissionImpl getSubmission(String id);

	/**
	 * Access the assessment's submission count.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The assessment's submission count.
	 */
	SubmissionCounts getSubmissionCounts(Assessment assessment);

	/**
	 * Find the highest submission score for this user to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user.
	 * @return The highest submission score to this assessment by this user (or 0 if there are no submissions).
	 */
	Float getSubmissionHighestScore(Assessment assessment, String userId);

	/**
	 * Find the submission in-progress for this user to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return The submission in-progress for this user to this assessment, or null if there is not one.
	 */
	SubmissionImpl getSubmissionInProgress(Assessment assessment, String userId);

	/**
	 * Access this submission's total score.
	 * 
	 * @param submission
	 *        The submission.
	 * @return The total score for this submission.
	 */
	Float getSubmissionScore(Submission submissionImpl);

	/**
	 * Get all the submissions by this user in this context, sorted.<br />
	 * Each assessment in the context is represented by at least one submission.<br />
	 * If the user has not started a submission yet for an assessment, an empty submission (no start date).
	 * 
	 * @param context
	 *        The context.
	 * @param userId
	 *        The user.
	 * @param sort
	 *        The sort specification.
	 * @return The list of submissions by this user in this context, sorted.
	 */
	List<SubmissionImpl> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort);

	/**
	 * Get all the users who have submitted to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The list of user (ids) who have submitted to this assessment.
	 */
	List<String> getUsersSubmitted(Assessment assessment);

	/**
	 * Construct a new Answer object.
	 * 
	 * @return A new Answer object.
	 */
	AnswerImpl newAnswer();

	/**
	 * Construct a new Submission object.
	 * 
	 * @return A new Submission object.
	 */
	SubmissionImpl newSubmission();

	/**
	 * Remove all incomplete submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	void removeIncompleteAssessmentSubmissions(Assessment assessment);

	/**
	 * Remove a submission from storage.
	 * 
	 * @param submission
	 *        The submission to remove.
	 */
	void removeSubmission(SubmissionImpl submission);

	/**
	 * Save changes made to these answers.
	 * 
	 * @param answers
	 *        the answers to save.
	 */
	void saveAnswers(List<Answer> answers);

	/**
	 * Save changes made to this submission.
	 * 
	 * @param submission
	 *        the submission to save.
	 */
	void saveSubmission(SubmissionImpl submission);

	/**
	 * Set the AssessmentService
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	void setAssessmentService(AssessmentService service);

	/**
	 * Set the MnemeService
	 * 
	 * @param service
	 *        The MnemeService.
	 */
	void setMnemeService(MnemeService service);

	/**
	 * Set the QuestionService
	 * 
	 * @param service
	 *        The QuestionsService.
	 */
	void setQuestionService(QuestionService service);

	/**
	 * Set the SecurityService
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	void setSecurityService(SecurityService service);

	/**
	 * Set the SessionManager
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	void setSessionManager(SessionManager service);

	/**
	 * Set the SubmissionService
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	void setSubmissionService(SubmissionServiceImpl service);

	/**
	 * Check if an submission by this id exists.
	 * 
	 * @param id
	 *        The submission id
	 * @return TRUE if the submission with this id exists, FALSE if not.
	 */
	Boolean submissionExists(String id);

	/**
	 * Check if any submissions in any state exist for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return TRUE if there are any submissions to the assesment.
	 */
	Boolean submissionsExist(Assessment assessment);
}
