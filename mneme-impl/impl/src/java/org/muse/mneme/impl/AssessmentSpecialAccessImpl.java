/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentSpecialAccess;
import org.muse.mneme.api.Changeable;

/**
 * AssessmentSpecialAccessImpl implements AssessmentSpecialAccess
 */
public class AssessmentSpecialAccessImpl implements AssessmentSpecialAccess
{
	/** The assessment we are for. */
	protected transient Assessment assessment = null;

	/** Track the recently deleted entries. */
	protected transient List<AssessmentAccess> deleted = new ArrayList<AssessmentAccess>();

	/** For container change tracking. */
	protected Changeable owner = null;

	/** The special access definitions. */
	protected List<AssessmentAccess> specialAccess = new ArrayList<AssessmentAccess>();

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 * @param owner
	 *        The change reporting entity.
	 */
	public AssessmentSpecialAccessImpl(Assessment assessment, AssessmentSpecialAccessImpl other, Changeable owner)
	{
		this.owner = owner;
		this.assessment = assessment;
		set(assessment, other);
	}

	/**
	 * Construct.
	 * 
	 * @param owner
	 *        The change reporting entity.
	 */
	public AssessmentSpecialAccessImpl(Assessment assessment, Changeable owner)
	{
		this.owner = owner;
		this.assessment = assessment;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess addAccess()
	{
		AssessmentAccessImpl rv = new AssessmentAccessImpl(this.assessment, this.owner);
		this.specialAccess.add(rv);

		this.owner.setChanged();

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess assureUserAccess(String userId)
	{
		for (AssessmentAccess access : this.specialAccess)
		{
			// look for one with ONLY this one user
			List<String> users = access.getUsers();
			if ((users.size() == 1) && (users.get(0).equals(userId))) return access;
		}

		// TODO: what if there's one for this user and others... remove the user from that? Start the new one from that?

		// we need to create one
		AssessmentAccess access = addAccess();
		List<String> userIds = new ArrayList<String>(1);
		userIds.add(userId);
		access.setUsers(userIds);

		return access;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clear()
	{
		if (this.specialAccess.isEmpty()) return;

		this.specialAccess.clear();

		this.owner.setChanged();
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
				this.deleted.add(access);

				return;
			}
		}
	}

	/**
	 * Clear out the deleted definitions.
	 */
	protected void clearDeleted()
	{
		this.deleted.clear();
	}

	/**
	 * Access the deleted definitions.
	 * 
	 * @return The List of deleted definitions.
	 */
	protected List<AssessmentAccess> getDeleted()
	{
		return this.deleted;
	}

	/**
	 * Set as a copy of another (deep copy).
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(Assessment assessment, AssessmentSpecialAccessImpl other)
	{
		this.specialAccess.clear();
		for (AssessmentAccess access : other.specialAccess)
		{
			this.specialAccess.add(new AssessmentAccessImpl(assessment, (AssessmentAccessImpl) access, this.owner));
		}
	}
}
