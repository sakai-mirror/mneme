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
 * UiSection is a container within a user interface. Each interface should have one section, and may have many.<br />
 * The section title is rendered, along with the controllers added to the section container.<br />
 * A section may be declared to reference Collection of entities so that the section is repeated, in sequence, for each entity.
 */
public interface Section extends Container
{
	/** separator styles. */
	enum Separator
	{
		line, none, space
	};

	/**
	 * Set the section link anchor (internal page address)
	 * 
	 * @param selection
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	Section setAnchor(String selection, PropertyReference... references);

	/**
	 * Set the decision to include each entity (if the EntityReference is set to a Collection)
	 * 
	 * @param inclusionDecision
	 *        The decision for inclusion of each entity.
	 * @return self.
	 */
	Section setEntityIncluded(Decision inclusionDecision);

	/**
	 * Set a reference to an object to have the focus while rendering the children of this section.
	 * 
	 * @param reference
	 *        The reference to an entity to focus on.
	 * @return self.
	 */
	Section setFocus(PropertyReference reference);

	/**
	 * Set a reference to an array of Collection of entities to iterate over.<br />
	 * The section will be repeated for each entity. Each repeat will set additional entries in the context.
	 * 
	 * @param reference
	 *        The reference to an array or collection to iterate over.
	 * @param name
	 *        The context name for the current iteration item.
	 * @return self.
	 */
	Section setIterator(PropertyReference reference, String name);

	/**
	 * Set the section title message.
	 * 
	 * @param selection
	 *        The message selector.
	 * @param references
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	Section setTitle(String selection, PropertyReference... references);

	/**
	 * Set the decision to include the title.
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to include the title.
	 * @return self.
	 */
	Section setTitleIncluded(Decision... decision);
}
