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
 * UiMessage is a message from the message bundle that can have property reference parameters.
 */
public interface Message
{
	/**
	 * Format the message from the message selector and the array of property references.
	 * 
	 * @param context
	 *        The UiContext.
	 * @param focus
	 *        The entity object focus.
	 * @return The formatted message.
	 */
	String getMessage(Context context, Object focus);

	/**
	 * Set the message selector and optional property references.
	 * 
	 * @param selector
	 * @param references
	 * @return self.
	 */
	Message setMessage(String selector, PropertyReference... references);
}
