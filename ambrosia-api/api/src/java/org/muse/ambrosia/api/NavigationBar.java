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

package org.muse.ambrosia.api;

/**
 * NavigationBar is a container to hold a set of "buttons" (Navigations), usually displayed at the bottom the UI.
 */
public interface NavigationBar extends Section
{
	/**
	 * Set the width to some css value ("60em" or "100px" or "90%" etc.)
	 * 
	 * @param width
	 *        The css width for the bar.
	 * @return self.
	 */
	NavigationBar setWidth(String width);
}
