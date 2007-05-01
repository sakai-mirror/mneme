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
import org.muse.ambrosia.api.UserInfoPropertyReference;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.Validator;

/**
 * UiUserInfoPropertyReference handles user id values by providing some user information for the display.
 */
public class UiUserInfoPropertyReference extends UiPropertyReference implements UserInfoPropertyReference
{
	/** The user info we want. */
	protected UserInfoPropertyReference.Selector selector = UserInfoPropertyReference.Selector.displayName;

	/**
	 * {@inheritDoc}
	 */
	public UserInfoPropertyReference setSelector(UserInfoPropertyReference.Selector property)
	{
		this.selector = property;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String format(Context context, Object value)
	{
		if (value == null) return super.format(context, value);
		if (!(value instanceof String)) return super.format(context, value);

		// TODO: assuming displayName for now...

		// context for now is site, so get the site title
		try
		{
			User user = UserDirectoryService.getUser((String) value);
			return Validator.escapeHtml(user.getDisplayName());
		}
		catch (UserNotDefinedException e)
		{
			return Validator.escapeHtml((String) value);
		}
	}
}
