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

/**
 * <p>
 * SubmissionAnswerEvaluation is an evaluation of the user's response to one question of an assessment.<br />
 * </p>
 */
public interface SubmissionAnswerEvaluation
{
	/**
	 * Access the evaluator's comments for this answer.
	 * 
	 * @return The evaluator's comments for this answer, or null if there are none.
	 */
	String getComments();

	/**
	 * Access the evaluator's manual score, which adds to the auto-score for this answer.
	 * 
	 * @return The evaluator's manual score.
	 */
	Float getManualScore();

	/**
	 * Access the id of the submission answer to which this evaluation applies.
	 * 
	 * @return The id of the submission answer to which this evaluation applies.
	 */
	String getSubmissionAnswerId();

	/**
	 * Access the back pointer to the evaluation of the entire submission.
	 * 
	 * @return The back pointer to the evaluation of the entire submission.
	 */
	SubmissionEvaluation getSubmissionEvaluation();

	/**
	 * Set the evaluator's comments for this answer.
	 * 
	 * @param comments
	 *        The evaluator's comments for this answe, or null if there is none.
	 */
	void setComments(String comments);

	/**
	 * Set the answer's manual score, to be combined with the auto score for the total score.
	 * 
	 * @param score
	 *        The answer's manual score, or 0 if there is none.
	 */
	void setManualScore(Float score);

	/**
	 * Set the id of the submission answer to which this evaluation applies.
	 * 
	 * @param submissionAnswerId
	 *        The id of the submission answer to which this evaluation applies.
	 */
	void setSubmissionAnswerId(String submissionAnswerId);
}
