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
 * FileUpload presents a file upload control.
 */
public interface FileUpload extends Controller
{
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
	FileUpload setOnEmptyAlert(Decision decision, String selector, PropertyReference... references);

	/**
	 * Set the property reference for the encode / decode.
	 * 
	 * @param propertyReference
	 *        The property reference for encode / decode.
	 */
	FileUpload setProperty(PropertyReference propertyReference);

	/**
	 * Set the read-only setting to the Boolean result of this reference.
	 * 
	 * @param reference
	 *        The property reference to provide the read only setting.
	 * @return self.
	 */
	FileUpload setReadOnly(PropertyReference reference);

	/**
	 * Set the title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	FileUpload setTitle(String selector, PropertyReference... references);

	/**
	 * Set the decision to include the title or not.
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to include the title.
	 * @return self.
	 */
	FileUpload setTitleIncluded(Decision... decision);

	/**
	 * Set that we want an upload submit button, using this text.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	FileUpload setUpload(String selector, PropertyReference... references);
}
