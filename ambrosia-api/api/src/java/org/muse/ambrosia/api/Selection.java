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
 * UiSelection presents a selection for the user to choose or not.<br />
 * The text can be either a property reference or a message.
 */
public interface Selection extends Controller
{
	/**
	 * Set the property reference for the encode / decode.
	 * 
	 * @param propertyReference
	 *        The property reference for encode / decode.
	 */
	Selection setProperty(PropertyReference propertyReference);

	/**
	 * Set the read-only setting to the Boolean result of this reference.
	 * 
	 * @param reference
	 *        The property reference to provide the read only setting.
	 * @return self.
	 */
	Selection setReadOnly(PropertyReference reference);

	/**
	 * Set the value that is decoded when the user makes the selection.
	 * 
	 * @param value
	 *        The value decoded when the user make the selection.
	 * @return self.
	 */
	Selection setSelectedValue(String value);

	/**
	 * Set the title text.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	Selection setTitle(String selector, PropertyReference... references);
}
