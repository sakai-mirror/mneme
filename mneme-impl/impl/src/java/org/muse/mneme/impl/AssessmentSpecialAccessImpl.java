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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentSpecialAccess;
import org.muse.mneme.api.Changeable;

/**
 * AssessmentSpecialAccessImpl implements AssessmentSpecialAccess
 */
public class AssessmentSpecialAccessImpl implements AssessmentSpecialAccess
{
	protected Changeable owner = null;

	protected List<AssessmentAccess> specialAccess = new ArrayList<AssessmentAccess>();

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 * @param owner
	 *        The change reporting entity.
	 */
	public AssessmentSpecialAccessImpl(AssessmentSpecialAccessImpl other, Changeable owner)
	{
		this.owner = owner;
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param owner
	 *        The change reporting entity.
	 */
	public AssessmentSpecialAccessImpl(Changeable owner)
	{
		this.owner = owner;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess addAccess()
	{
		AssessmentAccessImpl rv = new AssessmentAccessImpl(this.owner);
		this.specialAccess.add(rv);

		this.owner.setChanged();

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<AssessmentAccess> getAccess()
	{
		return this.specialAccess;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess getAccess(String id)
	{
		for (AssessmentAccess access : this.specialAccess)
		{
			if (access.getId().equals(id)) return access;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess getUserAccess(String userId)
	{
		for (AssessmentAccess access : this.specialAccess)
		{
			if (access.isForUser(userId)) return access;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAccess(AssessmentAccess remove)
	{
		for (Iterator i = this.specialAccess.iterator(); i.hasNext();)
		{
			AssessmentAccess access = (AssessmentAccess) i.next();

			if (access.equals(remove))
			{
				i.remove();

				this.owner.setChanged();

				return;
			}
		}
	}

	/**
	 * Set as a copy of another (deep copy).
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentSpecialAccessImpl other)
	{
		this.specialAccess.clear();
		for (AssessmentAccess access : other.specialAccess)
		{
			this.specialAccess.add(new AssessmentAccessImpl((AssessmentAccessImpl) access, this.owner));
		}
	}
}
