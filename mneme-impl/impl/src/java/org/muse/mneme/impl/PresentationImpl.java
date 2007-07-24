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

import java.util.ArrayList;
import java.util.List;

import org.muse.mneme.api.Presentation;

/**
 * PresentationImpl implements Presentation
 */
public class PresentationImpl implements Presentation
{
	protected List<String> attachments = new ArrayList<String>();

	protected String text = null;

	/**
	 * Construct.
	 */
	public PresentationImpl()
	{
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PresentationImpl(PresentationImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addAttachment(String reference)
	{
		this.attachments.add(reference);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getAttachments()
	{
		return this.attachments;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsEmpty()
	{
		return ((text == null) && attachments.isEmpty());
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText()
	{
		return this.text;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAttachment(String reference)
	{
		this.attachments.remove(reference);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Set as a copy of the other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PresentationImpl other)
	{
		this.attachments = new ArrayList<String>(other.attachments.size());
		this.attachments.addAll(other.attachments);
		this.text = other.text;
	}
}