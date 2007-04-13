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

import java.io.PrintWriter;
import java.util.List;

import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * UiContext contains the runtime specific information needed with the UiController tree to work a response.<br />
 * This is a set of internationalized messages that the UI tree references, along with a map of named objects that the UI tree references.
 */
public interface Context
{
	/** The name of the Boolean value for fragment or not in the context objects. */
	static final String FRAGMENT = "sakai:fragment";

	/**
	 * Add some javascript code.
	 * 
	 * @param validation
	 *        Some javascript code.
	 */
	void addScript(String code);

	/**
	 * Add some javascript code for an on-submit validation for the interface.
	 * 
	 * @param validation
	 *        The javascript validation code fragment.
	 */
	void addValidation(String validation);

	/**
	 * Clear the objects from the context.
	 */
	void clear();

	/**
	 * Find a controllers in the interface being rendered with this id.
	 * 
	 * @param id
	 *        The id to search for.
	 * @return The controllers in the interface that has this id, or an empty list if not found.
	 */
	List<Controller> findControllers(String id);

	/**
	 * Access the named object's value in the context.
	 * 
	 * @param name
	 *        The object name.
	 * @return The named object's value in the context, or null if missing.
	 */
	Object get(String name);

	/**
	 * Get any text collected while in collecting mode.
	 * 
	 * @return Any text collected while in collecting mode.
	 */
	String getCollected();

	/**
	 * Access the tool destination of the current request.
	 * 
	 * @return The tool destination of the current request.
	 */
	String getDestination();

	/**
	 * Access the named object's encoding in the context.
	 * 
	 * @param name
	 *        The object name.
	 * @return The named object's encoding in the context, or null if missing.
	 */
	String getEncoding(String name);

	/**
	 * Access the name of the form that wraps the entire interface.
	 * 
	 * @return The interface's form name.
	 */
	String getFormName();

	/**
	 * Access the internationalized messages.
	 * 
	 * @return The internationalized messages.
	 */
	InternationalizedMessages getMessages();

	/**
	 * Check if the post was expected or not.
	 * 
	 * @return true if the post was expected, or false if not (or if this was not a post).
	 */
	boolean getPostExpected();

	/**
	 * Acces the tool destination that we were in just before the current request.
	 * 
	 * @return The tool destination that we were in just before the current request.
	 */
	String getPreviousDestination();

	/**
	 * Access the writer over the response stream.
	 * 
	 * @return The writer over the response stream.
	 */
	PrintWriter getResponseWriter();

	/**
	 * Access the javascript code collected for the interface.
	 * 
	 * @return The javascript code collected for the interface, or null if there was none.
	 */
	String getScript();

	/**
	 * Set the top controller of the interface being rendered.
	 * 
	 * @retur The top controller of the interface being rendered.
	 */
	Controller getUi();

	/**
	 * Get a number to use in making a unique (in-context) id.
	 * 
	 * @return The unique id number.
	 */
	int getUniqueId();

	/**
	 * Access the javascript code collected for on-submit validation for the interface.
	 * 
	 * @return The javascript code collected for on-submit validation for the interface, or null if there was none.
	 */
	String getValidation();

	/**
	 * Add an object to context.
	 * 
	 * @param name
	 *        The object name.
	 * @param value
	 *        The object value.
	 */
	void put(String name, Object value);

	/**
	 * Add an object to context with a value and an encoding.
	 * 
	 * @param name
	 *        The object name.
	 * @param value
	 *        The object value.
	 * @param encoding
	 *        The encode - decode value to use instead of the object name when encoding for later decode.
	 */
	void put(String name, Object value, String encoding);

	/**
	 * Remove the named object from the context.
	 * 
	 * @param name
	 *        The name of the object to remove.
	 */
	void remove(String name);

	/**
	 * Go into collecting mode : any text that would be sent out is instead collected.
	 */
	void setCollecting();

	/**
	 * Set the tool destination for the current request.
	 * 
	 * @param destination
	 *        The tool destination for the current request.
	 */
	void setDestination(String destination);

	/**
	 * Set the name for the form that wraps the entire interface.
	 * 
	 * @param name
	 *        The form name.
	 */
	void setFormName(String name);

	/**
	 * Set the internationalized messages.
	 * 
	 * @param messages
	 *        The internationalized messages.
	 */
	void setMessages(InternationalizedMessages messages);

	/**
	 * Set the post expected flag.
	 * 
	 * @param expected
	 *        The post expected flag.
	 */
	void setPostExpected(boolean expected);

	/**
	 * Set the previous tool destination.
	 * 
	 * @param destination
	 *        The the previous tool destination.
	 */
	void setPreviousDestination(String destination);

	/**
	 * Set the top controller of the interface being rendered.
	 * 
	 * @param ui
	 *        The top controller of the interface being rendered.
	 */
	void setUi(Controller ui);
}
