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

package org.muse.mneme.api;

/**
 * <p>
 * FeedbackDelivery enumerates feedback options
 * </p>
 */
public class FeedbackDelivery
{
	private final Integer id;

	private final String name;

	private FeedbackDelivery(int id, String name)
	{
		this.id = new Integer(id);
		this.name = name;
	}

	public String toString()
	{
		return this.name;
	}

	public Integer dbEncoding()
	{
		return this.id;
	}

	static public FeedbackDelivery parse(int id)
	{
		switch (id)
		{
			case 1:
				return IMMEDIATE;
			case 2:
				return BY_DATE;
			case 3:
				return NONE;
			default:
				// TODO: what is the default?
				return IMMEDIATE;
		}
	}

	/** Immediate. */
	public static final FeedbackDelivery IMMEDIATE = new FeedbackDelivery(1, "IMMEDIATE_FEEDBACK");

	/** After the feedback date. */
	public static final FeedbackDelivery BY_DATE = new FeedbackDelivery(2, "FEEDBACK_BY_DATE");

	/** None. */
	public static final FeedbackDelivery NONE = new FeedbackDelivery(3, "NO_FEEDBACK");
}
