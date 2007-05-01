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
 * UiMatch is the assessment tool's matching interface. <br />
 */
public interface Match extends Controller
{
	/**
	 * Set the property reference for items in the choices to get the id.
	 * 
	 * @param propertyReference
	 *        The property reference for items in the choices to get the id.
	 */
	Match setChoiceId(PropertyReference propertyReference);

	/**
	 * Set the property reference for items in the choices to get the label.
	 * 
	 * @param propertyReference
	 *        The property reference for items in the choices to get the label.
	 */
	Match setChoiceLabel(PropertyReference propertyReference);

	/**
	 * Set the property reference for items in the choices to get the text.
	 * 
	 * @param propertyReference
	 *        The property reference for items in the choices to get the text.
	 */
	Match setChoiceText(PropertyReference propertyReference);

	/**
	 * Set the relative icon path to the icon to use to mark already correct entries.
	 * 
	 * @param propertyReference
	 *        The property reference for the set of Booleans marking each existing entry as correct or not.
	 * @param correctIcon
	 *        The relative icon path to the icon to use to mark already correct entries.
	 * @param correctMessage
	 *        The message selector string to use for the alternate text for the correct entry marking.
	 * @param incorrectIcon
	 *        The relative icon path to the icon to use to mark already incorrect entries.
	 * @param incorrectMessage
	 *        The message selector string to use for the alternate text for the incorrect entry marking.
	 * @param decision
	 *        The decision(s) to include the correct marking (if null, it's just included).
	 * @return self.
	 */
	Match setCorrectMarker(PropertyReference propertyReference, String correctIcon, String correctMessage, String incorrectIcon,
			String incorrectMessage, Decision... decision);

	/**
	 * Set the feedback display information.
	 * 
	 * @param propertyReference
	 *        The property reference for the set of Strings that is the feedback for each existing entry.
	 * @param message
	 *        The message selector string to use to format the feedback strings (each has a single {0} parameter) (optional - if null, the feedback is
	 *        used without formatting).
	 * @param decision
	 *        The decision(s) to include feedback (if null, it's just included).
	 * @return self.
	 */
	Match setFeedback(PropertyReference propertyReference, String message, Decision... decision);

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
	Match setOnEmptyAlert(Decision decision, String selector, PropertyReference... references);

	/**
	 * Set the property reference for the collection of parts.
	 * 
	 * @param propertyReference
	 *        The property reference for the collection of headers.
	 */
	Match setParts(PropertyReference propertyReference);

	/**
	 * Set the property reference for the choices in each part.
	 * 
	 * @param propertyReference
	 *        The property reference for the collection of choices.
	 */
	Match setPartsChoices(PropertyReference propertyReference);

	/**
	 * Set the property reference for the title in each part.
	 * 
	 * @param propertyReference
	 *        The property reference for the collection of choices.
	 */
	Match setPartsTitle(PropertyReference propertyReference);

	/**
	 * Set the property reference for the encode / decode.
	 * 
	 * @param propertyReference
	 *        The property reference for encode / decode.
	 */
	Match setProperty(PropertyReference propertyReference);

	/**
	 * Set the read-only setting to the Boolean result of this reference.
	 * 
	 * @param reference
	 *        The property reference to provide the read only setting.
	 * @return self.
	 */
	Match setReadOnly(PropertyReference reference);

	/**
	 * Set the select text.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	Match setSelectText(String selector, PropertyReference... references);

	/**
	 * Set the title text.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	Match setTitle(String selector, PropertyReference... references);

}
