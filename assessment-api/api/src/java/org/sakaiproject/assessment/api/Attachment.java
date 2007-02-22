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

import org.sakaiproject.time.api.Time;

/**
 * <p>
 * Attachment...
 * </p>
 */
public interface Attachment
{
	/**
	 * Access the full path on the file system if the attachment body is stored in a file instead of the db.
	 * 
	 * @return The full path on the file system if the attachment body is stored in a file instead of the db, or null if the
	 *         attachment body is in the db.
	 */
	String getFileSystemPath();

	/**
	 * Access the attachment id.
	 * 
	 * @return The attachment id.
	 */
	String getId();

	/**
	 * Access the attachment length (in bytes).
	 * 
	 * @return The attachment length.
	 */
	Long getLength();

	/**
	 * Access the attachment name (file name).
	 * 
	 * @return The attachment name.
	 */
	String getName();

	/**
	 * Access the attachment timestamp - the last modified date.
	 * 
	 * @return The attachment timestamp.
	 */
	Time getTimestamp();

	/**
	 * Access the attachment mime type.
	 * 
	 * @return The attachment type.
	 */
	String getType();

	/**
	 * Set the full path on the file system if the attachment body is stored in a file instead of the db.
	 * 
	 * @param path
	 *        The full path on the file system if the attachment body is stored in a file instead of the db, or null if the
	 *        attachment body is in the db.
	 */
	void setFileSystemPath(String path);
}
