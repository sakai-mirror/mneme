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

public class GbSpecs
{
	protected String context = null;

	public GbSpecs()
	{
	}

	public GbSpecs(String value)
	{
		String[] values = StringUtil.split(value, "x");
		context = values[0];
	}

	public String getContext()
	{
		return context;
	}

	public void setContext(String value)
	{
		context = value;
	}

	public String toString()
	{
		return context;
	}
}
