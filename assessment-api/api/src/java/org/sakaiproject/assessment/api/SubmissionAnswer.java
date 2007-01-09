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

import org.sakaiproject.entity.api.Reference;
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
	 * Get an array of the assessment answer feedback strings for each entry, from that entry's selected assessment answer<br />
	 * correct or incorrect depending on the correctness of the answer.
	 * 
	 * @return The appropriate feedback strings for each entry.
	 */
	String[] getAnswerFeedbacks();

	/**
	 * Access the answer's automatic scoring value - the sum of the auto scores of the entries.
	 * 
	 * @return The answer's auto-score.
	 */
	Float getAutoScore();

	/**
	 * Access the one or more entries that make up this answer. There needs to be an entry for each part of the assessment question.
	 * 
	 * @return The one or more entries that make up this answer.
	 */
	List<? extends SubmissionAnswerEntry> getEntries();

	/**
	 * Access assessment question answer texts that are currently in the entries as a List of attachment References.
	 * 
	 * @return The assessment question answer texts that are currently in the entries a List array of attachment References.
	 */
	List<Reference> getEntryAnswerAttachments();

	/**
	 * Access the assessment question answer ids that are currently in the entries as a List, in entry (question part) order, one per entry (per question part).
	 * 
	 * @return The assessment question answer ids that are currently in the entries as a List.
	 */
	List<String> getEntryAnswerIds();

	/**
	 * Access the answer text for a single-entry type question.
	 * 
	 * @return The answer text for a single-entry type question.
	 */
	String getEntryAnswerText();

	/**
	 * Access assessment question answer texts that are currently in the entries as a Listy, in entry (question part) order, one per entry (per question part).
	 * 
	 * @return The assessment question answer texts that are currently in the entries as a List.
	 */
	List<String> getEntryAnswerTexts();

	/**
	 * Access the correctness of each of the entry - TRUE if correct, FALSE if not, null if not answered, in entry (question part) order, one per entry (per question part).
	 * 
	 * @return The correctness of each of the entry - TRUE if correct, FALSE if not, null if not answered.
	 */
	List<Boolean> getEntryCorrects();

	/**
	 * Check if the question is answered; if the user has made the answer entries. Answers that have only "mark for review" or a rational are not considered answered.
	 * 
	 * @return TRUE if the question is considered to be answered, FALSE if not.
	 */
	Boolean getIsAnswered();

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
	 * Set these answer ids, ordered by question part, as the new answer ids for our entries. There must be an id for each entry (and for each question part) in order.
	 * 
	 * @param answerIds
	 *        The ordered set of assessment question answer ids for our entries. If null, all entry ids will be cleared.
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
	 * Set these answer texts, ordered by question part, as the new answer texts for our entries. There must be an text for each entry (and for each question part) in order.
	 * 
	 * @param answerTexts
	 *        The ordered set of answer texts for our entries. If null, all entry texts will be cleared
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
