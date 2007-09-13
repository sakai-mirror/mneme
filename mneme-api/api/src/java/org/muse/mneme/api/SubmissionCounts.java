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
 * SubmissionCounts contain counts of submissions: in-progress, completed, completed-not-graded, completed-graded
 */
public interface SubmissionCounts
{
	/**
	 * Access how many submissions are completed (released or not).
	 * 
	 * @return The number of completed (released or not) submissions.
	 */
	Integer getCompleted();

	/**
	 * Access how many submissions are in-progress.
	 * 
	 * @return The number of in-progress submissions.
	 */
	Integer getInProgress();

	/**
	 * Access how many submissions are completed and released.
	 * 
	 * @return The number of completed and released submissions.
	 */
	Integer getReleased();

	/**
	 * Access how many submissions are completed, but not yet released.
	 * 
	 * @return The number of completed, but not yet released submissions.
	 */
	Integer getUnreleased();

	/**
	 * Set how many submissions are completed (graded or not).
	 * 
	 * @param count
	 *        The count.
	 */
	void setCompleted(Integer count);

	/**
	 * Set how many submissions are in-progress.
	 * 
	 * @param count
	 *        The count.
	 */
	void setInProgress(Integer count);

	/**
	 * Set how many submissions are completed and released.
	 * 
	 * @param count
	 *        The count.
	 */
	void setReleased(Integer count);

	/**
	 * Set how many submissions are completed, but not yet graded.
	 * 
	 * @param count
	 *        The count.
	 */
	void setUnreleased(Integer count);
}
