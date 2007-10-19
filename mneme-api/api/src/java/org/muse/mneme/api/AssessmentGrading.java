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
 * GradingOptions contain the details of how submissions are graded.
 */
public interface AssessmentGrading
{
	/**
	 * Check if student identities are invisible to the grader when grading.
	 * 
	 * @return TRUE if student identities are invisible to the grader when grading, FALSE if not.
	 */
	Boolean getAnonymous();

	/**
	 * Check if submissions are to be considered graded as soon as submitted (based only on the auto-scoring).
	 * 
	 * @return TRUE if the submissions are considered graded on submission, FALSE for manual grading.
	 */
	Boolean getAutoRelease();

	/**
	 * Check if grades are to be sent to the Gradebook application.
	 * 
	 * @return TRUE if the assessment's grades are to be placed into the Gradebook, FALSE if not.
	 */
	Boolean getGradebookIntegration();

	/**
	 * Set if student identities are invisible to the grader when grading.
	 * 
	 * @return TRUE if student identities are invisible to the grader when grading, FALSE if not.
	 */
	void setAnonymous(Boolean setting);

	/**
	 * Set if submissions are to be considered graded as soon as submitted (based only on the auto-scoring).
	 * 
	 * @return TRUE if the submissions are considered graded on submission, FALSE for manual grading.
	 */
	void setAutoRelease(Boolean setting);

	/**
	 * Set if grades are to be sent to the Gradebook application.
	 * 
	 * @return TRUE if the assessment's grades are to be placed into the Gradebook, FALSE if not.
	 */
	void setGradebookIntegration(Boolean setting);
}
