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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentSpecialAccess;
import org.muse.mneme.api.Changeable;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

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

		// the user list of one
		List<String> userIds = new ArrayList<String>(1);
		userIds.add(userId);

		// see if there's one that pertains to this user, although it is not only for this user - add a copy if so
		AssessmentAccess access = getUserAccess(userId);
		if (access != null)
		{
			// make a copy
			access = new AssessmentAccessImpl(this.assessment, (AssessmentAccessImpl) access, this.owner);
			((AssessmentAccessImpl) access).id = null;
			this.specialAccess.add(access);
			this.owner.setChanged();
		}

		// else we need to create a new one
		else
		{
			access = addAccess();
		}

		// set the user
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
	public Boolean getIsDefined()
	{
		return Boolean.valueOf(!this.specialAccess.isEmpty());
	}

	/**
	 * {@inheritDoc}
	 */
	public List<AssessmentAccess> getOrderedAccess()
	{
		List<AssessmentAccess> rv = new ArrayList<AssessmentAccess>(this.specialAccess);

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				AssessmentAccess a0 = (AssessmentAccess) arg0;
				AssessmentAccess a1 = (AssessmentAccess) arg1;
				List<User> users0 = UserDirectoryService.getUsers(a0.getUsers());
				List<User> users1 = UserDirectoryService.getUsers(a1.getUsers());

				// sort the multiple lists to find the first one to use
				if (users0.size() > 1)
				{
					Collections.sort(users0, new Comparator()
					{
						public int compare(Object arg0, Object arg1)
						{
							int rv = ((User) arg0).getSortName().compareTo(((User) arg1).getSortName());
							return rv;
						}
					});
				}

				if (users1.size() > 1)
				{
					Collections.sort(users1, new Comparator()
					{
						public int compare(Object arg0, Object arg1)
						{
							int rv = ((User) arg0).getSortName().compareTo(((User) arg1).getSortName());
							return rv;
						}
					});
				}

				if ((users0.isEmpty()) && (users1.isEmpty()))
				{
					return 0;
				}
				else if (users0.isEmpty())
				{
					return -1;
				}
				else if (users1.isEmpty())
				{
					return 1;
				}
				return users0.get(0).getSortName().compareTo(users1.get(0).getSortName());
			}
		});

		return rv;
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
	 * Make sure no other access is defined for the users in this one.
	 * 
	 * @param target
	 *        The access
	 */
	protected void assureSingleAccessForUser(AssessmentAccess target)
	{
		List<AssessmentAccess> toRemove = new ArrayList<AssessmentAccess>();

		for (AssessmentAccess access : this.specialAccess)
		{
			// skip the target
			if (access.equals(target)) continue;

			// check each of the target's users
			for (String userId : target.getUsers())
			{
				// if this access was for that user
				if (access.isForUser(userId))
				{
					// remove the user from the access
					List<String> userIds = access.getUsers();
					userIds.remove(userId);
					((AssessmentAccessImpl) access).setChanged();

					// if the result is an access empty of users, remove it
					if (access.getUsers().isEmpty())
					{
						toRemove.add(access);
					}
				}
			}
		}

		// remove those we cleared out all users from
		for (AssessmentAccess access : toRemove)
		{
			removeAccess(access);
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
	 * Before saving the assessment, remove any saved (i.e. with id) definitions with no settings or no users.
	 */
	protected void consolidate()
	{
		// if any stored definitions override nothing, or have no users, remove them
		for (Iterator i = this.specialAccess.iterator(); i.hasNext();)
		{
			AssessmentAccess access = (AssessmentAccess) i.next();
			if (access.getId() == null) continue;

			boolean remove = access.getUsers().isEmpty();

			if (!remove)
			{
				remove = (!access.getOverrideAcceptUntilDate()) && (!access.getOverrideDueDate()) && (!access.getOverrideOpenDate())
						&& (!access.getOverridePassword()) && (!access.getOverrideTimeLimit()) && (!access.getOverrideTries());
			}

			if (remove)
			{
				i.remove();
				this.owner.setChanged();
				this.deleted.add(access);
			}
		}
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
