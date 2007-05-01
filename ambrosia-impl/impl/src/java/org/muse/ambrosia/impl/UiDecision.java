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
import org.muse.ambrosia.api.DecisionDelegate;
import org.muse.ambrosia.api.PropertyReference;

/**
 * UiDecision controls making an Entity selector based decision.
 */
public class UiDecision implements Decision
{
	/** The delegate who, if defined, will make the decision. */
	protected DecisionDelegate delegate = null;

	/** The PropertyReference for this decision. */
	protected PropertyReference propertyReference = null;

	/** If true, the test is reveresed. */
	protected boolean reversed = false;

	/**
	 * {@inheritDoc}
	 */
	public boolean decide(Context context, Object focus)
	{
		boolean decision = false;

		// delegte if setup to do so
		if (this.delegate != null)
		{
			decision = delegate.decide(this, context, focus);
		}
		else
		{
			// decide
			decision = makeDecision(context, focus);
		}

		// reverse if needed
		if (this.reversed) decision = !decision;

		return decision;
	}

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
	public Decision setDelegate(DecisionDelegate delegate)
	{
		this.delegate = delegate;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Decision setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Decision setReversed()
	{
		this.reversed = true;
		return this;
	}

	/**
	 * Make the decision.
	 * 
	 * @param context
	 *        The Context.
	 * @param focus
	 *        The entity object focus.
	 * @return the decision.
	 */
	protected boolean makeDecision(Context context, Object focus)
	{
		// read the property as a formatted string
		if (this.propertyReference != null)
		{
			String value = this.propertyReference.read(context, focus);
			if (value != null)
			{
				if (Boolean.parseBoolean(value))
				{
					return true;
				}

				// TODO: other interpretations of "true"?
			}
		}

		return false;
	}
}
