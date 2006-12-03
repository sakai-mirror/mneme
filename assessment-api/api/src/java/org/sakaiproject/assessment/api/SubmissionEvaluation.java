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

/**
 * <p>
 * SubmissionEvaluation is an evaluator's comments and manual scoring for a user's submission to an assessment.
 * </p>
 */
public interface SubmissionEvaluation
{
	/**
	 * Access the evaluations for each answer.
	 * 
	 * @return (a deep copy of) the evaluations for each answer.
	 */
	List<SubmissionAnswerEvaluation> getAnswerEvaluations();

	/**
	 * Access the sum of all the manual scores of all the answer evaluations.
	 * 
	 * @return The sum of all the manual scores of all the answer evaluations.
	 */
	Float getAnswersManualScore();

	/**
	 * Access the id of this evaluation
	 * 
	 * @return The evaluation's id.
	 */
	String getId();

	/**
	 * Access the manual score at the submission (not answer) level, to be combined with the answer auto scores and answer manual scores for the submission's total score.
	 * 
	 * @return The submission's manual score, or 0 if there is none.
	 */
	Float getManualScore();

	/**
	 * Access the submission that this evaluation applies to.
	 * 
	 * @return The submission that this evaluation applies to.
	 */
	Assessment getSubmission();

	/**
	 * Access the submissionId that this evaluation applies to.
	 * 
	 * @return The submissionId that this evaluation applies to.
	 */
	String getSubmissionId();

	/**
	 * Access the total manual score for this submission, computed from the sum of the answer evaluation's manual scores and the overallt manual score for the submission.
	 * 
	 * @return The total manual score for this submission.
	 */
	Float getTotalManualScore();

	/**
	 * Set the evaluations for each ansewr to (a deep copy of) this list.
	 * 
	 * @param evaluations
	 *        The evaluations for each answer.
	 */
	void setAnswerEvaluations(List<? extends SubmissionAnswerEvaluation> answers);

	/**
	 * Set the overall manual score for the submission, to be added to the auto score and manual scores of the answers to get the total score for the submission.
	 * 
	 * @param score
	 *        The overall manual score for the submission.
	 */
	void setManualScore(Float score);

	/**
	 * Set the submission id for this evaluation.
	 * 
	 * @param submissionId
	 *        The submissionId for this submission.
	 */
	void setSubmissionId(String submissionId);
}
