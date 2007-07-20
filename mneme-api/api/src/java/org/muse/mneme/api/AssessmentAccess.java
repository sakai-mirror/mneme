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

/**
 * AssessmentAccess contain the details of the special access to an assessment.
 */
public interface AssessmentAccess
{
	/**
	 * Check if the provided password (clear text) matches the defined password for the assessment.
	 * 
	 * @param password
	 *        The clear text password as entered.
	 * @return TRUE if the password is a match, false if not.
	 */
	Boolean checkPassword(String password);

	/**
	 * Access the defined password.
	 * 
	 * @return The password, or null if not defined. TODO: part of special access?
	 */
	String getPassword();

	/**
	 * Set the access password.
	 * 
	 * @param password
	 *        The access password, or null to remove it.
	 */
	void setPassword(String password);
}
