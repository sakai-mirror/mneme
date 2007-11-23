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

import java.io.InputStream;

import org.sakaiproject.entity.api.Reference;

/**
 * AttachmentService manages attachments.
 */
public interface AttachmentService
{
	/**
	 * The type string for this application: should not change over time as it may be stored in various parts of persistent entities.
	 */
	static final String APPLICATION_ID = "sakai:mneme";

	/** This string starts the references to uploaded resources. */
	static final String REFERENCE_ROOT = "/mneme";

	/**
	 * Add the attachment.
	 * 
	 * @param application
	 *        The application prefix for the collection in private.
	 * @param context
	 *        The context associated with the attachment.
	 * @param prefix
	 *        Any prefix path for within the context are of the application in private.
	 * @param uniqueHolder
	 *        If true, a uniquely named folder is created to hold the resource.
	 * @param name
	 *        The resource name.
	 * @param body
	 *        The resource body bytes stream.
	 * @param type
	 *        The resource mime type.
	 * @return The attachment reference.
	 */
	Reference addAttachment(String application, String context, String prefix, boolean uniqueHolder, String name, InputStream body, String type);

	/**
	 * Remove this attachment.
	 * 
	 * @param ref
	 *        The attachment reference.
	 */
	void removeAttachment(Reference ref);
}
