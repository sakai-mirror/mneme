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
 * Ordering provides information about an objcet's order within its container.
 * </p>
 */
public interface Ordering<E>
{
	/**
	 * Check if this is the first.
	 * 
	 * @return TRUE if first, FALSE if not.
	 */
	Boolean getIsFirst();

	/**
	 * Check if this is the last.
	 * 
	 * @return TRUE if last, FALSE if not.
	 */
	Boolean getIsLast();

	/**
	 * Access the next.
	 * 
	 * @return The next, or null if there is none.
	 */
	E getNext();

	/**
	 * Access this one's position (1 based).
	 * 
	 * @return The position (1 based).
	 */
	Integer getPosition();

	/**
	 * Access the previous.
	 * 
	 * @return The previous, or null if there is none.
	 */
	E getPrevious();
}
