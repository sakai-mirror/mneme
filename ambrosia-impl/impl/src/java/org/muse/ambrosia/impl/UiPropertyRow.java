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

package org.muse.ambrosia.impl;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.PropertyRow;

/**
 * UiPropertyRow describes one row of a UiEntityDisplay...
 */
public class UiPropertyRow implements PropertyRow
{
	/** The include decision. */
	protected Decision included = null;

	/** The PropertyReference for this row. */
	protected PropertyReference propertyReference = null;

	/** The message for the column title. */
	protected Message title = null;

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference getProperty()
	{
		return this.propertyReference;
	}

	/**
	 * {@inheritDoc}
	 */
	public Message getTitle()
	{
		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean included(Context context, Object focus)
	{
		if ((this.included != null) && (!this.included.decide(context, focus))) return false;

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyRow setIncluded(Decision decision)
	{
		this.included = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyRow setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyRow setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}
}
