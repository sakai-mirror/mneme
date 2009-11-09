/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2009 Etudes, Inc.
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

package org.etudes.mneme.impl;

import java.util.ArrayList;
import java.util.List;

import org.etudes.mneme.api.Changeable;
import org.etudes.mneme.api.Ordering;
import org.etudes.mneme.api.Part;
import org.etudes.mneme.api.PartDetail;

/**
 * PartDetailImpl implements PartDetail
 */
public abstract class PartDetailImpl implements PartDetail, Changeable
{
	public class MyOrdering implements Ordering<PartDetail>
	{
		protected PartDetailImpl detail = null;

		public MyOrdering(PartDetailImpl detail)
		{
			this.detail = detail;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (this.detail.part == null) return true;

			if (this.detail.equals((this.detail.part.getDetails().get(0)))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (this.detail.part == null) return true;

			if (this.detail.equals(this.detail.part.getDetails().get(this.detail.part.getDetails().size() - 1))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public PartDetail getNext()
		{
			if (this.detail.part == null) return null;

			int index = this.detail.part.getDetails().indexOf(detail);
			if (index == this.detail.part.getDetails().size() - 1) return null;

			return this.detail.part.getDetails().get(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (this.detail.part == null) return null;

			int index = this.detail.part.getDetails().indexOf(detail);

			return index + 1;
		}

		public List<Integer> getPositions()
		{
			List<Integer> rv = new ArrayList<Integer>();
			if (detail.part == null) return rv;

			for (int i = 0; i < detail.part.getDetails().size(); i++)
			{
				rv.add(Integer.valueOf(i + 1));
			}

			return rv;
		}

		/**
		 * {@inheritDoc}
		 */
		public PartDetail getPrevious()
		{
			if (this.detail.part == null) return null;

			int index = this.detail.part.getDetails().indexOf(detail);
			if (index == 0) return null;

			return this.detail.part.getDetails().get(index - 1);
		}

		/**
		 * Change the detail's position within the part
		 * 
		 * @param pos
		 *        a new (1 based) position.
		 */
		public void setPosition(Integer pos)
		{
			if (pos == null) return;

			int curPos = getPosition().intValue();
			int newPos = pos.intValue();
			if (curPos == newPos) return;

			// remove
			this.detail.part.getDetails().remove(this.detail);

			// re-insert
			this.detail.part.getDetails().add(newPos - 1, this.detail);

			// mark as changed
			((PartImpl) this.detail.part).setChanged();

			// this is a change that cannot be made to live tests
			((AssessmentImpl) this.detail.part.getAssessment()).lockedChanged = Boolean.TRUE;
		}
	}

	/** True if I've been changed. */
	protected boolean changed = false;

	/** Part detail id. */
	protected String id = null;

	/** My ordering. */
	protected MyOrdering ordering = new MyOrdering(this);

	/** The part context for this draw. */
	protected transient Part part = null;

	/** The sequence in the part at the time this was read (-1 if not set). Note: the actual sequence is the detail's position in the part's details list. */
	protected transient int seq = -1;

	/**
	 * Construct.
	 * 
	 * @param part
	 *        The part.
	 */
	public PartDetailImpl(Part part)
	{
		this.part = part;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearChanged()
	{
		this.changed = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getChanged()
	{
		return this.changed;
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
	public Boolean getIsFirst()
	{
		if (this.part == null) return Boolean.FALSE;

		return (this.part.getDetails().get(0) == this) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<PartDetail> getOrdering()
	{
		return this.ordering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Part getPart()
	{
		return this.part;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setChanged()
	{
		this.changed = true;
		if (this.part != null)
		{
			((PartImpl) this.part).setChanged();
		}
	}

	/**
	 * @return the as-read part sequence.
	 */
	protected int getSeq()
	{
		return this.seq;
	}

	/**
	 * Initialize the detail id.
	 * 
	 * @param id
	 *        The detail id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Initialize the detail's part.
	 * 
	 * @param part
	 *        The detail's part.
	 */
	protected void initPart(Part part)
	{
		this.part = part;
	}

	/**
	 * Initialize the as-read part sequence.
	 * 
	 * @param seq
	 *        The sequence number.
	 */
	protected void initSeq(int seq)
	{
		this.seq = seq;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PartDetailImpl other)
	{
		this.id = other.id;
		this.seq = other.seq;
	}
}
