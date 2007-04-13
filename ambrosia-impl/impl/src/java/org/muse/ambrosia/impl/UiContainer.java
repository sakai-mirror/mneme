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

package org.muse.ambrosia.impl;

import java.util.ArrayList;
import java.util.List;

import org.muse.ambrosia.api.Container;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;

/**
 * UiContainer is the base class of all UiControllers that contain collections of other controllers.<br />
 * add() us called to populate the controllers that are contained withing.<br />
 * The controllers that are contained are rendered inside this container.
 */
public class UiContainer extends UiController implements Container
{
	/** Controllers contained in this container. */
	protected List<Controller> contained = new ArrayList<Controller>();

	/**
	 * {@inheritDoc}
	 */
	public Container add(Controller controller)
	{
		contained.add(controller);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Controller> findControllers(String id)
	{
		List<Controller> rv = new ArrayList<Controller>();

		if (id == null) return rv;

		// search the contained
		for (Controller c : this.contained)
		{
			// this one?
			if (id.equals(c.getId())) rv.add(c);

			// if a container, search in there
			if (c instanceof Container)
			{
				List<Controller> found = ((Container) c).findControllers(id);
				rv.addAll(found);
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// render the contained
		for (Controller c : this.contained)
		{
			c.render(context, focus);
		}
	}
}
