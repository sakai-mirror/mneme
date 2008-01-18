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

package org.muse.mneme.impl;

import org.muse.mneme.api.Attachment;

/**
 * AttachmentImpl implements Attachment
 */
public class AttachmentImpl implements Attachment
{
	protected String name = null;

	protected String ref = null;

	protected String url = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AttachmentImpl(AttachmentImpl other)
	{
		set(other);
	}

	/**
	 * Construct.
	 */
	public AttachmentImpl(String name, String ref, String url)
	{
		this.name = name;
		this.ref = ref;
		this.url = url;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two AttachmentImpls are equals if they have the same ref
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if ((this.ref == null) || (((AttachmentImpl) obj).ref == null)) return false;
		return this.ref.equals(((AttachmentImpl) obj).ref);
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
	public String getReference()
	{
		return this.ref;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUrl()
	{
		return this.url;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRef(String ref)
	{
		this.ref = ref;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * Set as a copy of the other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AttachmentImpl other)
	{
		this.name = other.name;
		this.ref = other.ref;
		this.url = other.url;
	}
}
