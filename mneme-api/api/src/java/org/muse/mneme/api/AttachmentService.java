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
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
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

	/** Prefix for the MnemeDocs area. */
	static final String DOCS_AREA = "docs";

	/** Application code for Mneme in ContentHosting's private area. */
	static final String MNEME_APPLICATION = "mneme";

	/** This string starts the references to uploaded resources. */
	static final String REFERENCE_ROOT = "/mneme";

	/** Prefix for the submissions upload area in MnemeDocs. */
	static final String SUBMISSIONS_AREA = "submissions";

	/**
	 * Add an attachment from an uploaded file.
	 * 
	 * @param application
	 *        The application prefix for the collection in private.
	 * @param context
	 *        The context associated with the attachment.
	 * @param prefix
	 *        Any prefix path for within the context are of the application in private.
	 * @param uniqueHolder
	 *        If true, a uniquely named folder is created to hold the resource.
	 * @param file
	 *        The attachment file.
	 * @return The Reference to the added attachment.
	 */
	Reference addAttachment(String application, String context, String prefix, boolean uniqueHolder, FileItem file);

	/**
	 * Add an attachment from a reference to a resource in ContentHosting.
	 * 
	 * @param application
	 *        The application prefix for the collection in private.
	 * @param context
	 *        The context associated with the attachment.
	 * @param prefix
	 *        Any prefix path for within the context are of the application in private.
	 * @param uniqueHolder
	 *        If true, a uniquely named folder is created to hold the resource.
	 * @param resource
	 *        The Reference to the resource in ContentHosting.
	 * @return The Reference to the added attachment.
	 */
	Reference addAttachment(String application, String context, String prefix, boolean uniqueHolder, Reference resource);

	/**
	 * Form a Reference object from a reference string.
	 * 
	 * @param refString
	 *        The reference string.
	 * @return The Reference object.
	 */
	Reference getReference(String refString);

	/**
	 * Collect all the attachment references in the html data:<br />
	 * Anything referenced by a src= or href=. in our content docs
	 * 
	 * @param data
	 *        The data string.
	 * @param normalize
	 *        if true, decode the references by URL decoding rules.
	 * @return The set of attachment references.
	 */
	Set<String> harvestAttachmentsReferenced(String data, boolean normalize);

	/**
	 * Remove this attachment.
	 * 
	 * @param ref
	 *        The attachment reference.
	 */
	void removeAttachment(Reference ref);

	/**
	 * Translate any embedded attachment references in the html data, based on the set of translations.
	 * 
	 * @param data
	 *        The html data.
	 * @param translations
	 *        The translations.
	 * @return The translated data.
	 */
	String translateEmbeddedReferences(String data, List<Translation> translations);
}
