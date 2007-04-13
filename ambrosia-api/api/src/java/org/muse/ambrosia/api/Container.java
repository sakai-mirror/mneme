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

import java.util.List;

/**
 * UiContainer is the base class of all UiControllers that contain collections of other controllers.<br />
 * add() us called to populate the controllers that are contained withing.<br />
 * The controllers that are contained are rendered inside this container.
 */
public interface Container extends Controller
{
	/**
	 * Add a controller to the container.
	 * 
	 * @param controller
	 *        the controller to add.
	 */
	Container add(Controller controller);

	/**
	 * Find the contained controllers with this id.
	 * 
	 * @param id
	 *        The container id.
	 * @return The contained controllers with this id, or an empty list not found.
	 */
	List<Controller> findControllers(String id);
}
