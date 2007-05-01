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

import org.muse.ambrosia.api.AutoColumn;
import org.muse.ambrosia.api.Context;
import org.sakaiproject.util.Validator;

/**
 * AutoColumn provides automatic numbering for columns in an entity list.
 */
public class UiAutoColumn extends UiEntityListColumn implements AutoColumn
{
	/** The auto values. */
	protected String[] autoValues =
	{
			"A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "I.", "J.", "K.", "L.", "M.", "N.", "O.", "P.", "Q.", "R.", "S.", "T.",
			"U.", "V.", "W.", "X.", "Y.", "Z."
	};

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayText(Context context, Object entity, int row, int idRoot)
	{
		// TODO: watch out for overflow...

		String rv = this.autoValues[row];

		return Validator.escapeHtml(rv);
	}
}
