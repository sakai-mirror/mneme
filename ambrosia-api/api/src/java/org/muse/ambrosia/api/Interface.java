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
 * UiInterface is the top most container for each user interface.<br />
 * The interface title is rendered as the main (think window or frame) title.<br />
 * The interface has a header that is rendered in the display at the top.
 */
public interface Interface extends Container
{
	/**
	 * Set the user interface header message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	Interface setHeader(String selector, PropertyReference... references);

	/**
	 * Set to disable browser auto-complete for the interface.
	 * 
	 * @return self.
	 */
	Interface setNoAutoComplete();

	/**
	 * Set the user interface title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	Interface setTitle(String selector, PropertyReference... references);
}
