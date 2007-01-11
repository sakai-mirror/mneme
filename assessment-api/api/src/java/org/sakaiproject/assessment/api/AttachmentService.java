/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
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
 * AttachmentService ...
 * </p>
 */
public interface AttachmentService
{
	/** The type string for this application: should not change over time as it may be stored in various parts of persistent entities. */
	static final String APPLICATION_ID = "sakai:attachment";

	/** This string starts the references to resources in this service. */
	static final String REFERENCE_ROOT = "/attachment";

	/** Event tracking attachment reading. */
	static final String ATTACHMENT_READ = "attachment.read";

	/**
	 * Form a attachment reference for this attachment id.
	 * 
	 * @param container
	 *        The id of the attachment's container.
	 * @param id
	 *        the attachment id.
	 * @return the attachment reference for this attachment id.
	 */
	String getAttachmentReference(String container, String id);

	/**
	 * Create a new file upload receiving object.
	 * 
	 * @param answer
	 *        The submision answer that gets the upload.
	 * @return a new AttachmentUpload instance.
	 */
	AttachmentUpload newUpload(SubmissionAnswer answer);
}
