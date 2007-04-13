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
 * SelectionColumn is a column of a UiEntityList that presents a selection control to the user, allowing the selection of the entity in this row.
 */
public interface SelectionColumn extends EntityListColumn
{
	/**
	 * Set the format and properties to form the label.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 */
	SelectionColumn setLabel(String selector, PropertyReference... references);

	/**
	 * Set to support multiple selection.
	 * 
	 * @return self.
	 */
	SelectionColumn setMultiple();

	/**
	 * Set an alert that will triger once on submit if the field is empty.
	 * 
	 * @param decision
	 *        The decision to include the alert (if null, the alert is unconditionally included).
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 */
	SelectionColumn setOnEmptyAlert(Decision decision, String selector, PropertyReference... references);

	/**
	 * Set the property reference for encoding / decoding.
	 * 
	 * @param propertyReference
	 *        The property reference for encoding / decoding.
	 */
	SelectionColumn setProperty(PropertyReference propertyReference);

	/**
	 * Set the read-only setting to the Boolean result of this reference.
	 * 
	 * @param reference
	 *        The property reference to provide the read only setting.
	 * @return self.
	 */
	SelectionColumn setReadOnly(PropertyReference reference);

	/**
	 * Set to support a single selection only.
	 * 
	 * @return self.
	 */
	SelectionColumn setSingle();

	/**
	 * Set a decision to use to determine if we are going to be single select (true) or multiple select (false).
	 * 
	 * @param decision
	 *        The decision to use to determine if we are going to be single select (true) or multiple select (false).
	 * @return self.
	 */
	SelectionColumn setSingleSelectDecision(Decision decision);

	/**
	 * Set the property reference for the value encoded into the column. This will be reported if this row is selected.
	 * 
	 * @param propertyReference
	 *        The property reference for the value encoded into the column.
	 * @return self.
	 */
	SelectionColumn setValueProperty(PropertyReference propertyReference);
}
