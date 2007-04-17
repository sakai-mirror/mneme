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

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Courier;
import org.muse.ambrosia.api.Destination;

/**
 * UiCourier implements Courier.
 */
public class UiCourier extends UiController implements Courier
{
	/** The destination. */
	protected Destination destination = null;

	/** The frequency (seconds). */
	protected int frequency = 0;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// if not fully defined
		if ((this.destination == null) || (this.frequency < 1)) return;

		// included?
		if (!isIncluded(context, focus)) return;

		// here's the URL for the courier's GET
		String url = context.get("sakai.return.url") + this.destination.getDestination(context, focus);

		// TODO: works ONLY if we have ONLY one in the view... also depends on Sakai's headscript.js (might move to our own and let there be many)

		context.addScript("updateTime = " + this.frequency + "000;\n");
		context.addScript("updateUrl = \"" + url + "\"\n");
		context.addScript("scheduleUpdate();\n");
	}

	/**
	 * {@inheritDoc}
	 */
	public Courier setDestination(Destination destination)
	{
		this.destination = destination;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Courier setFrequency(int seconds)
	{
		this.frequency = seconds;
		return this;
	}
}
