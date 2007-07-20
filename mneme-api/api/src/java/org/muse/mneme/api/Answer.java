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

import org.sakaiproject.time.api.Time;

/**
 * Answer is the user's response to one question of an assessment.
 */
public interface Answer
{
	/**
	 * Perform auto-scoring now.
	 */
	void autoScore();

	/**
	 * Access the answer's automatic scoring value - the sum of the auto scores of the entries.
	 * 
	 * @return The answer's auto-score.
	 */
	Float getAutoScore();

	/**
	 * Get the question-type-specific entry data for the answer.
	 * 
	 * @return The question-type-specific entry data for the answer.
	 */
	Object getData();

	/**
	 * Access the evaluation for the answer.
	 * 
	 * @return The evaluation for the answer, or null if there is none.
	 */
	AnswerEvaluation getEvaluation();

	/**
	 * Access the Answer id.
	 * 
	 * @return The Answer id.
	 */
	String getId();

	/**
	 * Check if the question is answered; if the user has made the answer entries. Answers that have only "mark for review" or a rational are not
	 * considered answered.
	 * 
	 * @return TRUE if the question is considered to be answered, FALSE if not.
	 */
	Boolean getIsAnswered();

	/**
	 * Check if this answer has been changed by a setter.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	Boolean getIsChanged();

	/**
	 * Check if the question is marked as complete.
	 * 
	 * @return TRUE if the question is marked as complete, FALSE if not.
	 */
	Boolean getIsComplete();

	/**
	 * Check if the answer is correct or not.
	 * 
	 * @return TRUE if the answer is correct, FALSE if not, and null if the answer cannot be auto-scored.
	 */
	Boolean getIsCorrect();

	/**
	 * Access the "mark for review" setting for this answer.
	 * 
	 * @return The answer's "mark for review" setting; TRUE if the user has marked this answer for review, FALSE if not.
	 */
	Boolean getMarkedForReview();

	/**
	 * Access the assessment question that this is an answer to.
	 * 
	 * @return The answer's assessment question.
	 */
	Question getQuestion();

	/**
	 * Access the appropriate assessment question level feedback for this answer, depending on the correctness of the answer.
	 * 
	 * @return The appropriate assessment question level feedback for this answer, depending on the correctness of the answer.
	 */
	String getQuestionFeedback();

	/**
	 * Access the rationale text, if any, provided as part of this answer.
	 * 
	 * @return The answer's rationale text, or null if there is none.
	 */
	String getRationale();

	/**
	 * Access the back pointer to the submission.
	 * 
	 * @return The the back pointer to the submission.
	 */
	Submission getSubmission();

	/**
	 * Access the latest time that this answer was submitted.
	 * 
	 * @return The answer's submitted date.
	 */
	Time getSubmittedDate();

	/**
	 * Access the total score of the answer - the total of the auto score and the evaluation score.
	 * 
	 * @return The total score of the answer.
	 */
	Float getTotalScore();

	/**
	 * Set the question-type-specific entry data for the answer.
	 * 
	 * @param data
	 *        The question-type-specific entry data for the answer.
	 */
	void setData(Object data);

	/**
	 * Set the "mark for review" setting for this answer.
	 * 
	 * @param forReview
	 *        The answer's "mark for review" setting; TRUE if the user has marked this answer for review, FALSE if not.
	 */
	void setMarkedForReview(Boolean forReview);

	/**
	 * Set the rationale text, if any, provided as part of this answer.
	 * 
	 * @param rationale
	 *        The answer's rationale text, or null if there is none.
	 */
	void setRationale(String rationale);

	/**
	 * Set the latest time that this answer was submitted.
	 * 
	 * @param submitted
	 *        The answer's submitted date.
	 */
	void setSubmittedDate(Time submitted);
}
