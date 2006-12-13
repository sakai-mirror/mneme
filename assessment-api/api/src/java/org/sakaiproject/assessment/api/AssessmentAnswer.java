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
 * AssessmentAnswer ...
 * </p>
 */
public interface AssessmentAnswer
{
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
	 * Access the id of this assessment answer.
	 * 
	 * @return The anser's id.
	 */
	String getId();

	/**
	 * Access the answer's correctness.
	 * 
	 * @return TRUE if this is the correct answer, FALSE if not.
	 */
	Boolean getIsCorrect();

	/**
	 * Access the answer's display label.
	 * 
	 * @return The answer's display label.
	 */
	String getLabel();

	/**
	 * Access the back pointer to the question part.
	 * 
	 * @return The back pointer to the question part.
	 */
	QuestionPart getPart();

	/**
	 * Access the position (1 based) for presentation of this answer within the assessment question part.
	 * 
	 * @return The position (1 based) for presentation of this answer within the assessment question part.
	 */
	Integer getPosition();

	/**
	 * Access the answer's display text.
	 * 
	 * @return The answer's display text.
	 */
	String getText();

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
	 * Set the answer's correctness.
	 * 
	 * @param setting
	 *        TRUE if this is the correct answer, FALSE if not.
	 */
	void setIsCorrect(Boolean setting);

	/**
	 * Set the answer's display label.
	 * 
	 * @return The answer's display label.
	 */
	void setLabel(String label);

	/**
	 * Set the answer's display text.
	 * 
	 * @return The answer's display text.
	 */
	void setText(String text);
}
