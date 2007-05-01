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
 * UiEntityList presents a multi-column multi-item listing of entites from the model.<br />
 * setEntityReferemce() sets the reference to the Collection of model items, one for each row.<br />
 * addColumn() sets the columns, each a UiPropertyColumn, that references some selector of the entities in the model. Columns may be sortable.<br />
 * setSelection() sets the SelectionController. If set, then each row can be selected by the user. When selected, the identity of the item is reported
 * as part of the tool destination of the user request.<br />
 * setEmptyTitle() sets an alternate title to use if the list is empty.<br />
 * setIncluded() establishes include control over the entire list.
 */
public interface EntityList extends Controller
{
	/** rendering styles. */
	enum Style
	{
		flat, form
	};

	/**
	 * Add a column to display some selector of each entity.
	 * 
	 * @param column
	 *        A column to display some selector of each entity.
	 */
	EntityList addColumn(EntityListColumn column);

	/**
	 * Add a heading, based on this decision, and showing navigation.
	 * 
	 * @param decision
	 *        The heading decision.
	 * @param navigation
	 *        The navigation to make the header clickable (optional).
	 * @return self.
	 */
	EntityList addHeading(Decision decision, Navigation navigation);

	/**
	 * Add a heading, based on this decision, and showing the message from this selector and properties.
	 * 
	 * @param decision
	 *        The heading decision.
	 * @param selector
	 *        The message selector.
	 * @param properties
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 * @return self.
	 */
	EntityList addHeading(Decision decision, String selector, PropertyReference... properties);

	/**
	 * Set the message for text to display instead of the title if there are no items in the list.
	 * 
	 * @param selector
	 *        The message selector for text to display instead of the title if there are no items in the list.
	 * @param properties
	 *        one or more (or an array) of UiPropertyReferences to form the additional values in the formatted message.
	 */
	EntityList setEmptyTitle(String selector, PropertyReference... properties);

	/**
	 * Set the decision to include each entity.
	 * 
	 * @param inclusionDecision
	 *        The decision for inclusion of each entity.
	 * @return self.
	 */
	EntityList setEntityIncluded(Decision inclusionDecision);

	/**
	 * Set a reference to an array of Collection of entities to iterate over for the rows.
	 * 
	 * @param reference
	 *        The reference to an array or collection to iterate over.
	 * @param name
	 *        The context name for the current item in the iteration.
	 * @return self.
	 */
	EntityList setIterator(PropertyReference reference, String name);

	/**
	 * Set the style.
	 * 
	 * @param style
	 *        The style.
	 * @return self.
	 */
	EntityList setStyle(Style style);

	/**
	 * Set the list title message.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param properties
	 *        one or more (or an array) of PropertyReferences to form the additional values in the formatted message.
	 */
	EntityList setTitle(String selector, PropertyReference... properties);

	/**
	 * Set the decision to include the title or not.
	 * 
	 * @param decision
	 *        The decision, or set of decisions, all of which must pass to include the title.
	 * @return self.
	 */
	EntityList setTitleIncluded(Decision... decision);
}
