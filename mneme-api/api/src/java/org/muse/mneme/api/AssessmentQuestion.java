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

import org.sakaiproject.entity.api.Reference;

/**
 * <p>
 * AssessmentQuestion is the definition of one question in the assessment.
 * </p>
 */
public interface AssessmentQuestion
{
	/**
	 * Access an answer, by id, from any part
	 * 
	 * @param answerId
	 *        the answer id.
	 * @return The answer, or null if not found.
	 */
	AssessmentAnswer getAnswer(String answerId);

	/**
	 * Get the correct answer(s) formatted into an answer key, considering the possibly user-randomized answer order for the
	 * question.
	 * 
	 * @return The answer key.
	 */
	String getAnswerKey();

	/**
	 * Get the correct answer(s) formatted into an answer key, using the as-authored answer order.
	 * 
	 * @return The answer key.
	 */
	String getAnswerKeyAsAuthored();

	/**
	 * Access the ordering information about this question relative to the entire assessment.
	 * 
	 * @return The ordering information about this question relative to the entire assessment.
	 */
	Ordering<AssessmentQuestion> getAssessmentOrdering();

	/**
	 * Access the attachment list for the assessment.
	 * 
	 * @return The List of Reference objects that are the attachments for the assessment, or an empty list if there are no
	 *         attachments.
	 */
	List<Reference> getAttachments();

	/**
	 * Access the case sensitive setting for the question (applies to the fill-in question types for scoring).
	 * 
	 * @return The case sensitive setting for the question, or null if it does not apply.
	 */
	Boolean getCaseSensitive();

	/**
	 * Access the correct feedback string.
	 * 
	 * @return The correct feedback string, if defined, of null if not.
	 */
	String getFeedbackCorrect();

	/**
	 * Access the general feedback string.
	 * 
	 * @return The general feedback string, if defined, of null if not.
	 */
	String getFeedbackGeneral();

	/**
	 * Access the incorrect feedback string.
	 * 
	 * @return The incorrect feedback string, if defined, of null if not.
	 */
	String getFeedbackIncorrect();

	/**
	 * Access the id of this assessment question.
	 * 
	 * @return The assessment's id.
	 */
	String getId();

	/**
	 * Access the question's display instructions.
	 * 
	 * @return The question's display instructions.
	 */
	String getInstructions();

	/**
	 * Access the mutually exclusive setting for the question (applies to the fill-in question types for scoring).
	 * 
	 * @return The mutually exclusive setting for the question, or null if it does not apply.
	 */
	Boolean getMutuallyExclusive();

	/**
	 * Access a single part.
	 * 
	 * @return The single part.
	 */
	QuestionPart getPart();

	/**
	 * Access a part by id.
	 * 
	 * @param partId
	 *        The id.
	 * @return The part with this id, or null if not found.
	 */
	QuestionPart getPart(String partId);

	/**
	 * Access the parts.
	 * 
	 * @return The parts.
	 */
	List<? extends QuestionPart> getParts();

	/**
	 * Access the points that a correct answer to this question contributes to the assessment.
	 * 
	 * @return The question's points if correct.
	 */
	Float getPoints();

	/**
	 * Access the random answer ordering setting; if true, answer are to be presented in random order withing the question.
	 * 
	 * @return The random answer ordering setting.
	 */
	Boolean getRandomAnswerOrder();

	/**
	 * Access the require rationale setting for this question - if TRUE, the end-user can enter a rational with the answer.
	 * 
	 * @return The requuire rational setting for this question.
	 */
	Boolean getRequireRationale();

	/**
	 * Get the total scores for this question from all completed submissions to the question's assessment.
	 * 
	 * @return A List containing all the scores for this question from all completed submissions to the question's assessment, or an
	 *         empty list if there are none.
	 */
	List<Float> getScores();

	/**
	 * Access the back pointer to the section.
	 * 
	 * @return The back pointer to the section.
	 */
	AssessmentSection getSection();

	/**
	 * Access the ordering information relative to the section.
	 * 
	 * @return The ordering information relative to the section.
	 */
	Ordering<AssessmentQuestion> getSectionOrdering();

	/**
	 * Access the question's display title - either the instructions or the single part's title, depending on type.
	 * 
	 * @return The question's display title.
	 */
	String getTitle();

	/**
	 * Access the type for this question.
	 * 
	 * @return The question's type.
	 */
	QuestionType getType();

	/**
	 * Set the attachments to (a deep copy of) this list of attachments.
	 * 
	 * @param attachments
	 *        The attachments.
	 */
	void setAttachments(List<Reference> attachments);

	/**
	 * Set the case sensitive setting for the question (applies to the fill-in question types for scoring)
	 * 
	 * @param value
	 *        The case sensitive setting for the question, or null if it does not apply.
	 */
	void setCaseSensitive(Boolean value);

	/**
	 * Set the correct feedback string.
	 * 
	 * @param feedback
	 *        The correct feedback string.
	 */
	void setFeedbackCorrect(String feedback);

	/**
	 * Set the general feedback string.
	 * 
	 * @param feedback
	 *        The general feedback string.
	 */
	void setFeedbackGeneral(String feedback);

	/**
	 * Set the incorrect feedback string.
	 * 
	 * @param feedback
	 *        The incorrect feedback string.
	 */
	void setFeedbackIncorrect(String feedback);

	/**
	 * Set the question's display instructions.
	 * 
	 * @param instructions
	 *        The question's display instructions.
	 */
	void setInstructions(String instructions);

	/**
	 * Set the mutually exclusive setting for the question (applies to the fill-in question types for scoring)
	 * 
	 * @param value
	 *        The mutually exclusive setting for the question, or null if it does not apply.
	 */
	void setMutuallyExclusive(Boolean value);

	/**
	 * Set the parts to (a deep copy of) this list of parts.
	 * 
	 * @param parts
	 *        The parts.
	 */
	void setParts(List<? extends QuestionPart> parts);

	/**
	 * Set the random answer ordering setting.
	 * 
	 * @param setting
	 *        The random answer ordering setting.
	 */
	void setRandomAnswerOrder(Boolean setting);

	/**
	 * Set the require rationale setting for this question - if TRUE, the end-user can enter a rational with the answer.
	 * 
	 * @param setting
	 *        The requuire rational setting for this question.
	 */
	void setRequireRationale(Boolean setting);

	/**
	 * Set the score that a correct answer to this question contributes to the assessment.
	 * 
	 * @param score
	 *        The question's score.
	 */
	void setScore(Float score);

	/**
	 * Set the type for this question.
	 * 
	 * @param type
	 *        The question's type.
	 */
	void setType(QuestionType type);
}
