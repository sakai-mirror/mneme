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
 * Controller is the base interface of all user interface controller interfaces.
 */
public interface Controller
{
	/**
	 * Access the id.
	 * 
	 * @return The id.
	 */
	String getId();

	/**
	 * Check if this controller is to be included in the interface.
	 * 
	 * @param context
	 *        The Context.
	 * @param focus
	 *        The object focus.
	 * @return true if included, false if not.
	 */
	boolean isIncluded(Context context, Object focus);

	/**
	 * Render the controller.
	 * 
	 * @param context
	 *        The UI context.
	 * @param focus
	 *        An optional entity that is the focus of the rendering.
	 */
	void render(Context context, Object focus);

	/**
	 * Set the id of this controller, which can be referenced by an Alias, for example.
	 * 
	 * @param id
	 *        The controller's id.
	 * @return self.
	 */
	Controller setId(String id);

	/**
	 * Set the decision to include this in the interface.
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to be included.
	 * @return self.
	 */
	Controller setIncluded(Decision... decision);
}
