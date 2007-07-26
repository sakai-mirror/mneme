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

import java.util.Date;

/**
 * AssessmentDates contain the details of the availability dates of an assessment.
 */
public interface AssessmentDates
{
	/**
	 * Access the date after which submissions will not be accepted.
	 * 
	 * @return The date after which submissions will not be accepted, or null if there is none.
	 */
	Date getAcceptUntilDate();

	/**
	 * Access the due date. Submissions after this date are considered late, if they are accepted at all.
	 * 
	 * @return The assessment's due date, or null if there is none.
	 */
	Date getDueDate();

	/**
	 * Access the number of ms from now that the due date on this assessment will be reached.
	 * 
	 * @return The number of ms from now that the due date on this assessment will be reached, 0 if it has already been reached, or null if it has no
	 *         due date.
	 */
	Long getDurationTillDue();

	/**
	 * Access the expiration information for the assessment.
	 * 
	 * @return The expiration information for the assessment.
	 */
	Expiration getExpiration();

	/**
	 * Access the open date. Only after this date (if defined) is the assessment open for submission.
	 * 
	 * @return The assessment's open date, or null if there is none.
	 */
	Date getOpenDate();

	/**
	 * Set the date after which submissions will not be accepted.
	 * 
	 * @param date
	 *        The date after which submissions will not be accepted, or null if there is none.
	 */
	void setAcceptUntilDate(Date date);

	/**
	 * Set the due date. Submissions after this date are considered late, if they are accepted at all.
	 * 
	 * @param date
	 *        The assessment's due date, or null if there is none.
	 */
	void setDueDate(Date date);

	/**
	 * Set the release date. Only after this date (if defined) is the assessment open for submission.
	 * 
	 * @param date
	 *        The assessment's release date, or null if there is none.
	 */
	void setOpenDate(Date date);
}
