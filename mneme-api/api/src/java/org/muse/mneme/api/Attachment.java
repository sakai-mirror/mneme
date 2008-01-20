/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
 * Attachment models an attachment.
 */
public interface Attachment
{
	/**
	 * Access the display name.
	 * 
	 * @return The display name;
	 */
	String getName();

	/**
	 * Access the reference string.
	 * 
	 * @return the reference string.
	 */
	String getReference();

	/**
	 * Access the full access URL to the thumbnail representation of the attachment.
	 * 
	 * @return the full access URL to the thumbnail representation of the attachment.
	 */
	String getThumbUrl();

	/**
	 * Access the full access URL.
	 * 
	 * @return the full access URL.
	 */
	String getUrl();
}
