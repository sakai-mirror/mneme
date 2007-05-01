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

import java.io.PrintWriter;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.api.Divider;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.NavigationBar;

/**
 * UiNavigationBar implements NavigationBar
 */
public class UiNavigationBar extends UiSection implements NavigationBar
{
	/** The width (in some css value such as "60em" or "100px" or "90%" etc.) */
	protected String width = null;

	/**
	 * {@inheritDoc}
	 */
	public NavigationBar setWidth(String width)
	{
		this.width = width;
		return this;
	}

	/**
	 * Render the navigation bar
	 * 
	 * @param context
	 *        The context.
	 * @param focus
	 *        The focus object.
	 */
	protected void renderContents(Context context, Object focus)
	{
		PrintWriter response = context.getResponseWriter();

		// the bar
		response.println("<div class=\"ambrosiaNavigationBar\"" + (this.width != null ? (" style=\"width: " + this.width + ";\"") : "") + ">");

		// wrap the items
		response.println("<div class=\"ambrosiaNavigationItems\">");

		// render
		for (Controller c : this.contained)
		{
			c.render(context, focus);
		}

		response.println("</div></div>");
	}
}
