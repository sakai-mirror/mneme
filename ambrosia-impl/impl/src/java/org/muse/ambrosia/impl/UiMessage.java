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
import org.muse.ambrosia.api.PropertyReference;

/**
 * UiMessage is a message from the message bundle that can have property reference parameters.
 */
public class UiMessage implements Message
{
	/** A set of additional properties to put in the message. */
	protected PropertyReference[] references = null;

	/** The message selector. */
	protected String selector = null;

	/**
	 * {@inheritDoc}
	 */
	public Message setMessage(String selector, PropertyReference... references)
	{
		this.selector = selector;
		this.references = references;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getMessage(Context context, Object focus)
	{
		// if no references, use just the selector message
		if ((references == null) || (references.length == 0))
		{
			if (selector != null)
			{
				return context.getMessages().getString(selector);
			}
			return null;
		}
		
		// if there is no selector, just read the first reference as the value
		if (selector == null)
		{
			if ((references != null) && (references.length == 1))
			{
				return references[0].read(context, focus);
			}
			return null;
		}

		// put the property reference into args for the message
		Object args[] = new Object[references.length];
		int i = 0;
		for (PropertyReference reference : references)
		{
			String value = reference.read(context, focus);
			
			// if any are null, null the entire message
			// TODO: make this an option rather than default behavior? -ggolden
			// if (value == null) value = "";
			if (value == null) return null;

			args[i++] = value;
		}

		return context.getMessages().getFormattedMessage(selector, args);
	}
}
