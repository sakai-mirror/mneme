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

package org.sakaiproject.assessment.impl;

import org.sakaiproject.assessment.api.Attachment;
import org.sakaiproject.time.api.Time;

/**
 * <p>
 * AttachmentImpl is ...
 * </p>
 */
public class AttachmentImpl implements Attachment
{
	protected String fileSystemPath = null;

	protected String id = null;

	protected Long length = null;

	protected String name = null;

	protected Time timestamp = null;

	protected String type = null;

	public AttachmentImpl(String id, Long length, String name, Time timestamp, String type, String fileSystemPath)
	{
		this.id = id;
		this.length = length;
		this.name = name;
		this.timestamp = timestamp;
		this.type = type;
		this.fileSystemPath = fileSystemPath;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFileSystemPath()
	{
		return this.fileSystemPath;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getLength()
	{
		return this.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getTimestamp()
	{
		return this.timestamp;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType()
	{
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFileSystemPath(String path)
	{
		this.fileSystemPath = path;
	}
}
