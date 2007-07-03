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
 * MnemeService is the overall service interface for Mneme, providing some application specific support<br />
 * as well as covering the 4 major services of the application.
 */
public interface MnemeService extends PoolService, QuestionService, SubmissionService, AssessmentService
{
	/**
	 * The type string for this application: should not change over time as it may be stored in various parts of persistent entities.
	 */
	static final String APPLICATION_ID = "sakai:mneme";

	/** The sub-type for assessment in references (/mneme/test/...) */
	static final String ASSESSMENT_TYPE = "test";

	/** The security function used to check if users can grade tests. */
	static final String GRADE_PERMISSION = "mneme.grade";

	/** The security function used to check if users can manage tests. */
	static final String MANAGE_PERMISSION = "mneme.manage";

	/** This string starts the references to resources in this service. */
	static final String REFERENCE_ROOT = "/mneme";

	/** Event tracking event for adding a submission. */
	static final String SUBMISSION_ADD = "mneme.submit";

	/** Event tracking event for answering a question in a submission. */
	static final String SUBMISSION_ANSWER = "mneme.answer";

	/** Event tracking event for the system automatically completing a submission. */
	static final String SUBMISSION_AUTO_COMPLETE = "mneme.auto_complete";

	/** Event tracking event for completing a submission. */
	static final String SUBMISSION_COMPLETE = "mneme.complete";

	/** Event tracking event for re-entering a submission. */
	static final String SUBMISSION_CONTINUE = "mneme.continue";

	/** Event tracking event for entering a submission. */
	static final String SUBMISSION_ENTER = "mneme.enter";

	/** Event tracking event for reviewing a submission. */
	static final String SUBMISSION_REVIEW = "mneme.review";

	/** The sub-type for submissions in references (/mneme/submission/...) */
	static final String SUBMISSION_TYPE = "submission";

	/** The security function used to check if users can submit to an assessment. */
	static final String SUBMIT_PERMISSION = "mneme.submit";

	/** Event tracking event for adding a test. */
	static final String TEST_ADD = "mneme.manage";
}
