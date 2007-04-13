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
 * Navigation presents a navigation control (button or text link) to the user. The result of the press is a navigation to some tool
 * destination. A form submit is optional.
 */
public interface Navigation extends Controller
{
	/** Icon styles. */
	enum IconStyle
	{
		left, right
	};

	/** rendering styles. */
	enum Style
	{
		button, link
	};

	/**
	 * Access the tool destination for the navigation.
	 * 
	 * @param context
	 *        The UiContext.
	 * @param focus
	 *        The focus.
	 * @return The tool destination for the selection link for this item.
	 */
	String getDestination(Context context, Object focus);

	/**
	 * Set the access key for the navigation to the character produced by this message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        One or more PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	Navigation setAccessKey(String selector, PropertyReference... references);

	/**
	 * Set the decision about makeing this a two step / confirmed process
	 * 
	 * @param decision
	 *        The decicion.
	 * @return self.
	 */
	Navigation setConfirm(Decision decision);

	/**
	 * Set this as a default choice.
	 * 
	 * @return self.
	 */
	Navigation setDefault();

	/**
	 * Set the decision to make this a default choice or not.
	 * 
	 * @param defaultDecision
	 *        The decision, or set of decisions, all of which must pass to make this the default choice.
	 * @return self.
	 */
	Navigation setDefault(Decision... defaultDecision);

	/**
	 * Set the descriptive text for the navigation.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        One or more PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	Navigation setDescription(String selector, PropertyReference... references);

	/**
	 * Set the tool destination to use when clicked.
	 * 
	 * @param destination
	 *        The tool destination.
	 * @return self.
	 */
	Navigation setDestination(Destination destination);

	/**
	 * Set the decision to be disabled (inactive, but visible).
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to be disabled.
	 * @return self.
	 */
	Navigation setDisabled(Decision... decision);

	/**
	 * Set the icon for the navigation
	 * 
	 * @param url
	 *        The full URL to the icon.
	 * @param style
	 *        The icon style (left or right placement).
	 * @return self.
	 */
	Navigation setIcon(String url, IconStyle style);

	/**
	 * Set the decision to be included.
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to be included.
	 * @return self.
	 */
	Navigation setIncluded(Decision... decision);

	/**
	 * Set the format style.
	 * 
	 * @param style
	 *        The format style.
	 * @return self.
	 */
	Navigation setStyle(Style style);

	/**
	 * Indicate that the navigation needs to submit the form.
	 * 
	 * @return self.
	 */
	Navigation setSubmit();

	/**
	 * Set the mavigation title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param references
	 *        one or more PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	Navigation setTitle(String selector, PropertyReference... references);

	/**
	 * Set the decision about forcing form validation when this navigation (submit only) is pressed.
	 * 
	 * @param decision
	 *        The decicion.
	 * @return self.
	 */
	Navigation setValidation(Decision decision);
}
