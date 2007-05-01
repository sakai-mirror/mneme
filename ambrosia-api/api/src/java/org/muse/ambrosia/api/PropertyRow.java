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
 * UiPropertyRow describes one row of a UiEntityDisplay...
 */
public interface PropertyRow
{
	/**
	 * Get the selector reference for the column. This will provide the value to display for each entity in this column.
	 * 
	 * @return The selector reference for the column.
	 */
	PropertyReference getProperty();

	/**
	 * Access the row title message.
	 * 
	 * @return The row title message.
	 */
	Message getTitle();

	/**
	 * Check if the row is included.
	 * 
	 * @param context
	 *        The UiContext
	 * @param focus
	 *        the entity.
	 * @return true if the row is included, false if not.
	 */
	boolean included(Context context, Object focus);

	/**
	 * Set the decision to include the row.
	 * 
	 * @param decision
	 *        The decision to include the row.
	 * @return self.
	 */
	PropertyRow setIncluded(Decision decision);

	/**
	 * Set the selector reference for the column. This will provide the value to display for each entity in this column.
	 * 
	 * @param propertyReference
	 *        The selector reference for the column.
	 */
	PropertyRow setProperty(PropertyReference propertyReference);

	/**
	 * Set the row title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	PropertyRow setTitle(String selector, PropertyReference... references);
}
