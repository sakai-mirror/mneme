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
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.api.Decision;

/**
 * UiController implements Controller.
 */
public class UiController implements Controller
{
	/** The id of this element - can be referenced by an alias, for instance. */
	protected String id = null;

	/** The include decision array. */
	protected Decision[] included = null;

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
	public boolean isIncluded(Context context, Object focus)
	{
		if (this.included == null) return true;
		for (Decision decision : this.included)
		{
			if (!decision.decide(context, focus))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public Controller setId(String id)
	{
		this.id = id;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Controller setIncluded(Decision... decision)
	{
		this.included = decision;
		return this;
	}
}
