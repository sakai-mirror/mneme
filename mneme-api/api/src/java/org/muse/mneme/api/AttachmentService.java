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

import org.sakaiproject.entity.api.Reference;

/**
 * <p>
 * AttachmentService ...
 * </p>
 */
public interface AttachmentService
{
	/**
	 * The type string for this application: should not change over time as it may be stored in various parts of persistent
	 * entities.
	 */
	static final String APPLICATION_ID = "sakai:attachment";

	/** Event tracking attachment reading. */
	static final String ATTACHMENT_READ = "attachment.read";

	/** Event tracking attachment deleting. */
	static final String ATTACHMENT_DELETE = "attachment.delete";

	/** This string starts the references to resources in this service. */
	static final String REFERENCE_ROOT = "/attachment";

	/**
	 * Read the attachment from storage (just the meta-data, not the body)
	 * 
	 * @param attachment
	 *        The attachment Reference.
	 * @return The Attachment (meta data only, no body), or null if not found.
	 */
	Attachment getAttachment(Reference attachment);

	/**
	 * Form a attachment reference for this attachment id.
	 * 
	 * @param container
	 *        The id of the attachment's container.
	 * @param id
	 *        the attachment id.
	 * @param name
	 *        The file name for the attachment.
	 * @return the attachment reference for this attachment id.
	 */
	String getAttachmentReference(String container, String id, String name);

	/**
	 * Remove this attachment
	 * 
	 * @param attachment
	 *        The attachment Reference.
	 */
	void removeAttachment(Reference attachment);
}
