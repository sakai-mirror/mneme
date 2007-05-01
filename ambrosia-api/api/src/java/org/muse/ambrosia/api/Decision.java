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
 * UiDecision controls making an Entity selector based decision.
 */
public interface Decision
{
	/**
	 * Make the decision, based on a boolean interpretation of the selector of this entity object.
	 * 
	 * @param context
	 *        The UiContext.
	 * @param entity
	 *        The entity to get the selector value from.
	 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
	 */
	boolean decide(Context context, Object focus);

	/**
	 * Get the property reference that will provide the comparison value.
	 * 
	 * @return The property reference that will provide the comparison value.
	 */
	PropertyReference getProperty();

	/**
	 * Set the decision delegate - who will make the actual decision.
	 * 
	 * @param delegate
	 *        The decision delegate.
	 * @return self.
	 */
	Decision setDelegate(DecisionDelegate delegate);

	/**
	 * Set the property reference that will provide the comparison value.
	 * 
	 * @param propertyReference
	 *        The property reference that will provide the comparison value.
	 */
	Decision setProperty(PropertyReference propertyReference);

	/**
	 * Set the decision to look for the reverse of the set conditions.
	 * 
	 * @return self.
	 */
	Decision setReversed();
}
