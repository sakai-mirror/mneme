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

package org.muse.mneme.tool;

import org.sakaiproject.util.StringUtil;

public class SimulateSpecs
{
	protected Integer numUsers = 0;

	protected Integer startGap = 0;

	protected Integer thinkTime = 0;

	public SimulateSpecs()
	{
	}

	public SimulateSpecs(String value)
	{
		String[] values = StringUtil.split(value, "x");
		numUsers = Integer.valueOf(values[0]);
		startGap = Integer.valueOf(values[1]);
		thinkTime = Integer.valueOf(values[2]);
	}

	public Integer getNumUsers()
	{
		return numUsers;
	}

	public Integer getStartGap()
	{
		return this.startGap;
	}

	public Integer getThinkTime()
	{
		return this.thinkTime;
	}

	public void setNumUsers(Integer value)
	{
		numUsers = value;
	}

	public void setStartGap(Integer startGap)
	{
		this.startGap = startGap;
	}

	public void setThinkTime(Integer thinkTime)
	{
		this.thinkTime = thinkTime;
	}

	public String toString()
	{
		return numUsers.toString() + "x" + startGap.toString() + "x" + thinkTime.toString();
	}
}