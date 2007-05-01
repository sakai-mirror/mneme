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

import org.muse.ambrosia.api.CompareDecision;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.StringUtil;

/**
 * UiCompareDecision is a UiDecision that compares the value to some other value.
 */
public class UiCompareDecision extends UiDecision implements CompareDecision
{
	/** The PropertyReference for the comparison. */
	protected PropertyReference compareReference = null;

	/** If we don't have a compare property, use this value. */
	protected String[] compareReferenceValue = null;

	/**
	 * {@inheritDoc}
	 */
	public CompareDecision setEqualsConstant(String... value)
	{
		this.compareReferenceValue = value;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CompareDecision setEqualsProperty(PropertyReference propertyReference)
	{
		this.compareReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean makeDecision(Context context, Object focus)
	{
		// read the property as a formatted string
		if (this.propertyReference != null)
		{
			String value = this.propertyReference.read(context, focus);
			if (value != null)
			{
				// check against the reference, if set
				if (this.compareReference != null)
				{
					String compare = this.compareReference.read(context, focus);
					if (compare != null)
					{
						return value.equalsIgnoreCase(compare);
					}
				}

				// or against the constant, if set
				else if (this.compareReferenceValue != null)
				{
					return StringUtil.contains(this.compareReferenceValue, value);
				}
			}
		}

		return false;
	}
}
