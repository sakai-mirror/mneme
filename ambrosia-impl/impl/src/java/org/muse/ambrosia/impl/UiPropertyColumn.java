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
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.PropertyReference;

/**
 * UiPropertyColumn describes one column of a UiEntityList...
 */
public class UiPropertyColumn extends UiEntityListColumn implements PropertyColumn
{
	/** An alternate source for the column display values - formatted with a message and properties. */
	protected Message propertyMessage = null;

	/** The PropertyReference for this column. */
	protected PropertyReference propertyReference = null;

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayText(Context context, Object entity, int row, int idRoot)
	{
		String rv = "";

		// use the formatted property
		if (this.propertyMessage != null)
		{
			rv = this.propertyMessage.getMessage(context, entity);
		}

		// or the plain property
		else if (this.propertyReference != null)
		{
			rv = this.propertyReference.read(context, entity);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyColumn setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyColumn setProperty(String selector, PropertyReference... references)
	{
		this.propertyMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

}
