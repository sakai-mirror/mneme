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
import org.muse.ambrosia.api.IconPropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiIconPropertyReference implements IconPropertyReference
 */
public class UiIconPropertyReference extends UiPropertyReference implements IconPropertyReference
{
	protected String name = null;

	/**
	 * {@inheritDoc}
	 */
	public String read(Context context, Object focus)
	{
		// alt=\"" + Validator.escapeHtml(name) + "\"
		return "<img style=\"vertical-align:middle\" src=\"" + context.get("sakai.return.url") + name + "\" />";
	}

	/**
	 * {@inheritDoc}
	 */
	public Object readObject(Context context, Object focus)
	{
		return read(context, focus);
	}

	/**
	 * {@inheritDoc}
	 */
	public IconPropertyReference setIcon(String name)
	{
		this.name = name;
		return this;
	}
}
