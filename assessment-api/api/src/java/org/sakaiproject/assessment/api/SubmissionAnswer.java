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

import org.sakaiproject.time.api.Time;

/**
 * <p>
 * SubmissionAnswer is the user's response to one question of an assessment.<br />
 * For some question types, multiple entries from the user are required or allowed to form a single answer.
 * </p>
 */
public interface SubmissionAnswer
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
	 * Access the one or more entries that make up this answer.
	 * 
	 * @return The one or more entries that make up this answer.
	 */
	List<? extends SubmissionAnswerEntry> getEntries();

	// /**
	// * Access the single entry for single-entry type questions (such as true/false, multiple choice).
	// *
	// * @return The single entry for single-entry type questions.
	// */
	// SubmissionAnswerEntry getEntry();

	/**
	 * Access the assessment question answer ids that are currently in the entries as an array.
	 * 
	 * @return The assessment question answer ids that are currently in the entries as an array.
	 */
	String[] getEntryAnswerIds();

	/**
	 * Access the answer text for a single-entry type question.
	 * 
	 * @return The answer text for a single-entry type question.
	 */
	String getEntryAnswerText();

	/**
	 * Access assessment question answer texts that are currently in the entries as an array.
	 * 
	 * @return The assessment question answer texts that are currently in the entries as an array.
	 */
	String[] getEntryAnswerTexts();

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
	AssessmentQuestion getQuestion();

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
	 * Set the entry list with the entries for this answer.
	 * 
	 * @param entries
	 *        The entry list with the entries for this answer
	 */
	void setEntries(List<? extends SubmissionAnswerEntry> entries);

	/**
	 * Adjust the set of answer entries so that they cover exactly these answer ids - any others are removed, new ones added if needed.
	 * 
	 * @param answerIds
	 *        zero, one or more assessment question answer ids.
	 */
	void setEntryAnswerIds(String... answerIds);

	/**
	 * Set the answer text for a single-entry type question.
	 * 
	 * @param answerText
	 *        The answer text for a single-entry type question.
	 */
	void setEntryAnswerText(String answerText);

	/**
	 * Adjust the set of answer entries so that they cover exactly these answer texts - any others are removed, new ones added if needed.
	 * 
	 * @param answerTexts
	 *        zero, one or more assessment question answer texts.
	 */
	void setEntryAnswerTexts(String... answerTexts);

	/**
	 * Set the "mark for review" setting for this answer.
	 * 
	 * @param forReview
	 *        The answer's "mark for review" setting; TRUE if the user has marked this answer for review, FALSE if not.
	 */
	void setMarkedForReview(Boolean forReview);

	/**
	 * Set the assessment question that this is an answer to.
	 * 
	 * @param question
	 *        The assessment question.
	 */
	void setQuestion(AssessmentQuestion question);

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
