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
 * UiEntityDisplay presents a multi-row single-item display of properties of one entity from the model.<br />
 * setEntityReference() sets the reference to the entity to display.<br />
 * addRow() sets the rows, each a UiPropertyRow, that references some selector of the entity.<br />
 */
public interface EntityDisplay extends Controller
{
	/**
	 * Add a row to display some selector of the entity.
	 * 
	 * @param row
	 *        A row to display some selector of each entity.
	 */
	EntityDisplay addRow(PropertyRow row);

	/**
	 * Set the reference to the entity to display.
	 * 
	 * @param entityReference
	 *        The reference to the entity to display.
	 */
	EntityDisplay setEntityReference(PropertyReference entityReference);

	/**
	 * Set the display title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	EntityDisplay setTitle(String selector, PropertyReference... references);
}
