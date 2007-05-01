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
import org.muse.ambrosia.api.Destination;
import org.muse.ambrosia.api.PropertyReference;

/**
 * UiDestination forms a tool destination, from a template, with possible embedded fill-in-the-blanks, and property references to fill them in.<br />
 * The format is the same as for international messages, i.e. text {0} more text {1} etc
 */
public class UiDestination implements Destination
{
	/** A set of additional properties to put in the message. */
	protected PropertyReference[] references = null;

	/** The template. */
	protected String template = null;

	/**
	 * {@inheritDoc}
	 */
	public Destination setDestination(String template, PropertyReference... references)
	{
		this.template = template;
		this.references = references;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDestination(Context context, Object focus)
	{
		if ((this.references == null) || (this.references.length == 0)) return this.template;

		// put the property reference into args for the message
		int i = 0;
		String rv = this.template;
		for (PropertyReference ref : references)
		{
			// read the value
			String value = ref.read(context, focus);
			if (value == null) value = "";
			
			// replace
			rv = rv.replaceAll("\\{" + Integer.toString(i++) + "\\}", value);
		}

		return rv;
	}
}
