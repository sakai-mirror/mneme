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

/**
 * AssessmentAccess holds details of a single special access grant for select users to an assessment.
 */
public interface AssessmentAccess
{
	/**
	 * Access the special availability dates for the assessment.
	 * 
	 * @return The special availability dates for the assessment.
	 */
	AssessmentDates getDates();

	/**
	 * Check if we have a time limmit.
	 * 
	 * @return TRUE if a time limit is defined, FALSE if not.
	 */
	Boolean getHasTimeLimit();

	/**
	 * Check if we have a tries limit.
	 * 
	 * @return TRUE if a tries limit is defined, FALSE if not.
	 */
	Boolean getHasTriesLimit();

	/**
	 * Access the id.
	 * 
	 * @return The id.
	 */
	String getId();

	/**
	 * Access the special password.
	 * 
	 * @return The special password.
	 */
	AssessmentPassword getPassword();

	/**
	 * Access the special time limit for taking the assessment (ms).
	 * 
	 * @return The special time limit for taking the assessment (ms), or null if it is untimed.
	 */
	Long getTimeLimit();

	/**
	 * Access the special number of submissions allowed, if not unlimited.
	 * 
	 * @return The special number of submissions allowed, or null if unlimited.
	 */
	Integer getTries();

	/**
	 * Access the list of users for which this access applies.
	 * 
	 * @return The List of user ids for which this access applies.
	 */
	List<String> getUsers();

	/**
	 * Access the list of users in human readable form.
	 * 
	 * @return The list of users in human readable form.
	 */
	String getUsersDisplay();

	/**
	 * Check if this access applies to this user.
	 * 
	 * @param userId
	 *        The user id to check.
	 * @return TRUE if the access applies to this user, FALSE if not.
	 */
	Boolean isForUser(String userId);

	/**
	 * An alternate way to clear the time limit if set to false.
	 * 
	 * @param hasTimeLimit
	 *        if FALSE, clear the time limit.
	 */
	void setHasTimeLimit(Boolean hasTimeLimit);

	/**
	 * An alternate way to clear the tries limit if set to false.
	 * 
	 * @param hasTriesLimit
	 *        if FALSE, clear the tries.
	 */
	void setHasTriesLimit(Boolean hasTriesLimit);

	/**
	 * Set the time limit for taking the assessment (ms).
	 * 
	 * @param limit
	 *        The time limit for the assessment, or null for unlimited.
	 */
	void setTimeLimit(Long limit);

	/**
	 * Set the special number of submissions allowed for limited submissions.
	 * 
	 * @param count
	 *        The special number of submissions allowed, or null to make it unlimited.
	 */
	void setTries(Integer count);

	/**
	 * Set the users for which this access applies.
	 * 
	 * @param userIds
	 *        The List of user ids for which this access applies.
	 */
	void setUsers(List<String> userIds);
}
