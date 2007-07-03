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

package org.muse.mneme.impl;

import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.Question;

/**
 * QuestionImpl implements Question
 */
public class QuestionImpl implements Question
{
	protected Object data = null;

	protected String description = null;

	protected String id = null;

	protected Presentation presentation = null;

	protected String type = null;

	protected String version = "only";

	public QuestionImpl(String id)
	{
		this.id = id;
	}

	public QuestionImpl(String id, Object data, String description, Presentation presentation, String type)
	{
		this.id = id;
		this.data = data;
		this.description = description;
		this.presentation = presentation;
		this.type = type;
	}

	public Object getData()
	{
		return this.data;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.description;
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
	public Presentation getPresentation()
	{
		return this.presentation;
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
	public String getVersion()
	{
		return this.version;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setData(Object data)
	{
		this.data = data;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setType(String type)
	{
		this.type = type;
	}
}
