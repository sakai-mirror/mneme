/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

/**
 * Question defines the questions.
 */
public interface Question
{
	/**
	 * Access the ordering information within the context of an assessment, ordering within the entire assessment (counting all parts).
	 * 
	 * @return The ordering information within the assessment.
	 */
	Ordering<Question> getAssessmentOrdering();

	/**
	 * Access the context of this question.
	 * 
	 * @return The question's context string.
	 */
	String getContext();

	/**
	 * Access the created-by (owner / date created)
	 * 
	 * @return The created-by for the question.
	 */
	Attribution getCreatedBy();

	/**
	 * Access the description of the question.  Will be <= 255 characters plain text.
	 * 
	 * @return The description of the question.
	 */
	String getDescription();

	/**
	 * Access the explain-reason setting.
	 * 
	 * @return TRUE if this question also collects "reason" from the user, FALSE if not.
	 */
	Boolean getExplainReason();

	/**
	 * Access the feedback (rich) text for the question.
	 * 
	 * @return The feedback text for the question, or null if there is none.
	 */
	String getFeedback();

	/**
	 * Check if there are any completed submissions to this question's assessment that have any null scores for answered answers to this quesiton.
	 * 
	 * @return TRUE if there are unscored submissions to this assessment, FALSE if not.
	 */
	Boolean getHasUnscoredSubmissions();

	/**
	 * Access the hints (rich) text for the question.
	 * 
	 * @return The hints text for the question, or null if there is none.
	 */
	String getHints();

	/**
	 * Access the id of this question.
	 * 
	 * @return The question's id.
	 */
	String getId();

	/**
	 * Check if the question has been changed.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	Boolean getIsChanged();

	/**
	 * Check the historical setting of the question.
	 * 
	 * @return TRUE if the question is used only for historical access, FALSE if it is a current question.
	 */
	Boolean getIsHistorical();

	/**
	 * Check if the end user has never made initial settings.
	 * 
	 * @return TRUE if this has not been modified since creation, FALSE if it has.
	 */
	Boolean getMint();

	/**
	 * Access the modified-by (owner / date created)
	 * 
	 * @return The modified-by for the question.
	 */
	Attribution getModifiedBy();

	/**
	 * Access the assessment part, within the context of an assessment, that this question is being used in.
	 * 
	 * @return The assessment part that this question is being used in.
	 */
	Part getPart();

	/**
	 * Access the ordering information within the context of an assessment, ordering within a single part in which the question is being used.
	 * 
	 * @return The ordering information within the part in the assessment.
	 */
	Ordering<Question> getPartOrdering();

	/**
	 * Access the question pool that holds this question.
	 * 
	 * @return The question pool that holds this question.
	 */
	Pool getPool();

	/**
	 * Access question's presentation (rich 'text' and attachments)
	 * 
	 * @return The question's presentation.
	 */
	Presentation getPresentation();

	/**
	 * Access the question type.
	 * 
	 * @return The question type.
	 */
	String getType();

	/**
	 * Access the question type in a localized, human-readable form.
	 * 
	 * @return The question type in a localized, human-readable form.
	 */
	String getTypeName();

	/**
	 * Access the question's type-specific handler.
	 * 
	 * @return The question's type-specific handler.
	 */
	TypeSpecificQuestion getTypeSpecificQuestion();

	/**
	 * Mark this question as changed.
	 */
	void setChanged();

	/**
	 * Set the explain-reason setting.
	 * 
	 * @param explainReason
	 *        TRUE if this question also collects "reason" from the user, FALSE if not.
	 */
	void setExplainReason(Boolean explainReason);

	/**
	 * Set the feedback (rich) text for the question.
	 * 
	 * @param feedback
	 *        The feedback text for the question.
	 */
	void setFeedback(String feedback);

	/**
	 * Set the hints (rich) text for the question.
	 * 
	 * @param hints
	 *        The hints text for the question.
	 */
	void setHints(String hints);

	/**
	 * Set the question pool that holds this question.
	 * 
	 * @param pool
	 *        The question pool to hold this question.
	 */
	void setPool(Pool pool);
}
