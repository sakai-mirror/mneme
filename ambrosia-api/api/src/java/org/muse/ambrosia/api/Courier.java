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
 * Courier sets up a courier in the view.
 */
public interface Courier extends Controller
{
	/**
	 * Set the tool destination to use when the courier fires (this tool destination will get a GET and return javascript that will be run).
	 * 
	 * @param destination
	 *        The tool destination.
	 * @return self.
	 */
	Courier setDestination(Destination destination);

	/**
	 * Set the frequency, in seconds, at which the courier will run.
	 * 
	 * @param seconds
	 *        The time between courier runs.
	 * @return self.
	 */
	Courier setFrequency(int seconds);
}
