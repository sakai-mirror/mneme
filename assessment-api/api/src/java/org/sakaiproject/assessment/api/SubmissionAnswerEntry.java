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
 * SubmissionEntry is the user's entry to a single assessment question as part of a submission answer; there can be multiple entries for a single answer to a question.
 * </p>
 */
public interface SubmissionAnswerEntry
{
	/**
	 * Access the free-form answer text for this entry, if any.
	 * 
	 * @return The answer's free-form answer text, or null if there is none.
	 */
	String getAnswerText();

	/**
	 * Access the assessment question answer that was choosen for this submission answer entry.
	 * 
	 * @return The entry's assessment question answer.
	 */
	AssessmentAnswer getAssessmentAnswer();

	/**
	 * Access the assessment question answer id that was choosen for this submission answer entry.
	 * 
	 * @return The entry's assessment question answer id.
	 */
	String getAssessmentAnswerId();

	/**
	 * Access the entry's automatic scoring value.
	 * 
	 * @return The entry's auto-score.
	 */
	Float getAutoScore();

	/**
	 * Access the back pointer to the submission answer.
	 * 
	 * @return The back pointer to the submission answer.
	 */
	SubmissionAnswer getSubmissionAnswer();

	/**
	 * Set the free-form answer text for this answer entry, if any.
	 * 
	 * @param text
	 *        The answer's free-form answer text, or null if there is none.
	 */
	void setAnswerText(String text);

	/**
	 * Set the assessment question answer that was choosen for this answer entry.
	 * 
	 * @param answer
	 *        The assessment question answer that was choosen for this answer entry.
	 */
	void setAssessmentAnswer(AssessmentAnswer answer);

	/**
	 * Set the assessment question answer id that was choosen for this answer entry.
	 * 
	 * @param answerId
	 *        The assessment question answer id that was choosen for this answer entry.
	 */
	void setAssessmentAnswerId(String answerId);
}
