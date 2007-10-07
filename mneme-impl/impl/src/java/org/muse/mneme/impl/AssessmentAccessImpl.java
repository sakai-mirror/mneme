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

package org.muse.mneme.impl;

import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.Changeable;

/**
 * AssessmentAccessImpl implements AssessmentAccess
 */
public class AssessmentAccessImpl implements AssessmentAccess
{
	protected transient Changeable owner = null;

	protected String password = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentAccessImpl(AssessmentAccessImpl other, Changeable owner)
	{
		this(owner);
		set(other);
	}

	/**
	 * Construct.
	 */
	public AssessmentAccessImpl(Changeable owner)
	{
		this.owner = owner;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean checkPassword(String password)
	{
		if (password == null) return Boolean.FALSE;
		return password.equals(this.password);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPassword(String password)
	{
		if (!Different.different(this.password, password)) return;

		this.password = password;

		this.owner.setChanged();
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentAccessImpl other)
	{
		this.password = other.password;
	}
}
