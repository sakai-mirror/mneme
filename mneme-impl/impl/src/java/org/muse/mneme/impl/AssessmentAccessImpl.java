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
import java.util.Date;
import java.util.List;

import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentPassword;
import org.muse.mneme.api.Changeable;

/**
 * AssessmentAccessImpl implements AssessmentAccess
 */
public class AssessmentAccessImpl implements AssessmentAccess
{
	protected Date acceptUntilDate = null;

	protected Date dueDate = null;

	protected String id = null;

	protected Date openDate = null;

	protected Boolean overrideAcceptUntilDate = Boolean.FALSE;

	protected Boolean overrideDueDate = Boolean.FALSE;

	protected Boolean overrideOpenDate = Boolean.FALSE;

	protected Boolean overridePassword = Boolean.FALSE;

	protected Boolean overrideTimeLimit = Boolean.FALSE;

	protected Boolean overrideTries = Boolean.FALSE;

	protected Changeable owner = null;

	protected AssessmentPasswordImpl password = null;

	protected Long timeLimit = null;

	protected Integer tries = null;

	protected List<String> userIds = new ArrayList<String>();

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentAccessImpl(AssessmentAccessImpl other, Changeable owner)
	{
		this.owner = owner;
		set(other);
	}

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param assessmentService
	 *        The AssessmentService.
	 * @param questionService
	 *        The QuestionService.
	 * @param poolService
	 *        The PoolService.
	 */
	public AssessmentAccessImpl(Changeable owner)
	{
		this.owner = owner;
		this.password = new AssessmentPasswordImpl(owner);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two are equals if they have the same id
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if ((this.id == null) || (((AssessmentAccessImpl) obj).id == null)) return false;
		return this.id.equals(((AssessmentAccessImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getAcceptUntilDate()
	{
		return this.acceptUntilDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDueDate()
	{
		return this.dueDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasTimeLimit()
	{
		return Boolean.valueOf(this.timeLimit != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasTriesLimit()
	{
		return Boolean.valueOf(this.tries != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getOpenDate()
	{
		return this.openDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverrideAcceptUntilDate()
	{
		return this.overrideAcceptUntilDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverrideDueDate()
	{
		return this.overrideDueDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverrideOpenDate()
	{
		return this.overrideOpenDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverridePassword()
	{
		return this.overridePassword;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverrideTimeLimit()
	{
		return this.overrideTimeLimit;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getOverrideTries()
	{
		return this.overrideTries;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentPassword getPassword()
	{
		return this.password;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getTimeLimit()
	{
		return this.timeLimit;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getTries()
	{
		return this.tries;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getUsers()
	{
		return this.userIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return this.id == null ? "null".hashCode() : this.id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean isForUser(String userId)
	{
		return Boolean.valueOf(this.userIds.contains(userId));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAcceptUntilDate(Date date)
	{
		if (!Different.different(date, this.acceptUntilDate)) return;

		this.acceptUntilDate = date;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Date date)
	{
		if (!Different.different(date, this.dueDate)) return;

		this.dueDate = date;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setHasTimeLimit(Boolean hasTimeLimit)
	{
		if (hasTimeLimit == null) throw new IllegalArgumentException();

		if ((!hasTimeLimit) && (this.timeLimit != null))
		{
			this.timeLimit = null;

			this.owner.setChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setHasTriesLimit(Boolean hasTriesLimit)
	{
		if (hasTriesLimit == null) throw new IllegalArgumentException();

		if ((!hasTriesLimit) && (this.tries != null))
		{
			this.tries = null;

			this.owner.setChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOpenDate(Date date)
	{
		if (!Different.different(date, this.openDate)) return;

		this.openDate = date;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverrideAcceptUntilDate(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overrideAcceptUntilDate)) return;

		this.overrideAcceptUntilDate = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverrideDueDate(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overrideDueDate)) return;

		this.overrideDueDate = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverrideOpenDate(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overrideOpenDate)) return;

		this.overrideOpenDate = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverridePassword(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overridePassword)) return;

		this.overridePassword = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverrideTimeLimit(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overrideTimeLimit)) return;

		this.overrideTimeLimit = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOverrideTries(Boolean override)
	{
		if (override == null) throw new IllegalArgumentException();
		if (override.equals(this.overrideTries)) return;

		this.overrideTries = override;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		if (!Different.different(limit, this.timeLimit)) return;

		this.timeLimit = limit;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTries(Integer count)
	{
		if (!Different.different(count, this.tries)) return;

		this.tries = count;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUsers(List<String> userIds)
	{
		// TODO: change tracking
		this.userIds.clear();
		if (userIds != null)
		{
			this.userIds.addAll(userIds);
		}

		this.owner.setChanged();
	}

	/**
	 * Establish the id.
	 * 
	 * @param id
	 *        The part id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Set as a copy of another (deep copy).
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentAccessImpl other)
	{
		this.acceptUntilDate = other.acceptUntilDate;
		this.dueDate = other.dueDate;
		this.id = other.id;
		this.openDate = other.openDate;
		this.overrideAcceptUntilDate = other.overrideAcceptUntilDate;
		this.overrideDueDate = other.overrideDueDate;
		this.overrideOpenDate = other.overrideOpenDate;
		this.overridePassword = other.overridePassword;
		this.overrideTimeLimit = other.overrideTimeLimit;
		this.overrideTries = other.overrideTries;
		this.password = new AssessmentPasswordImpl(other.password, this.owner);
		this.timeLimit = other.timeLimit;
		this.tries = other.tries;
		this.userIds = new ArrayList<String>(other.userIds);
	}
}
