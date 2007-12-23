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

import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.Changeable;
import org.muse.mneme.api.Evaluation;

/**
 * EvaluationImpl implements Evaluation
 */
public class EvaluationImpl implements Evaluation
{
	protected AttributionImpl attribution = new AttributionImpl(null);

	/** Track any changes. */
	protected transient Changeable changed = new ChangeableImpl();

	protected String comment = null;

	protected Boolean evaluated = Boolean.FALSE;

	protected Float score = null;

	/**
	 * Construct.
	 */
	public EvaluationImpl()
	{
	}

	/**
	 * Construct as a copy of other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public EvaluationImpl(EvaluationImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getAttribution()
	{
		return this.attribution;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getComment()
	{
		return this.comment;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getEvaluated()
	{
		return this.evaluated;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getScore()
	{
		return this.score;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setComment(String comment)
	{
		if (!Different.different(this.comment, comment)) return;

		this.comment = comment;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEvaluated(Boolean evaluated)
	{
		if (evaluated == null) throw new IllegalArgumentException();
		if (this.evaluated.equals(evaluated)) return;

		this.evaluated = evaluated;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScore(Float s)
	{
		if (s == null)
		{
			if (!Different.different(this.score, s)) return;

			this.score = s;

			this.changed.setChanged();
		}

		else
		{
			// round
			Float newScore = Float.valueOf(((float) Math.round(s.floatValue() * 100.0f)) / 100.0f);

			if (!Different.different(this.score, newScore)) return;

			this.score = newScore;

			this.changed.setChanged();
		}
	}

	/**
	 * Clear the is-changed flag.
	 */
	protected void clearIsChanged()
	{
		this.changed.clearChanged();
	}

	/**
	 * Check if there was any change.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	protected Boolean getIsChanged()
	{
		return this.changed.getChanged();
	}

	/**
	 * Initialize the comment.
	 * 
	 * @param comment
	 *        The comment.
	 */
	protected void initComment(String comment)
	{
		this.comment = comment;
	}

	/**
	 * Initialize evaluated.
	 * 
	 * @param evaluated
	 *        The evaluated setting.
	 */
	protected void initEvaluated(Boolean evaluated)
	{
		this.evaluated = evaluated;
	}

	/**
	 * Initialize the score.
	 * 
	 * @param score
	 *        The score.
	 */
	protected void initScore(Float score)
	{
		this.score = (score == null) ? null : Float.valueOf(((float) Math.round(score.floatValue() * 100.0f)) / 100.0f);
	}

	/**
	 * Set values to a copy of other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(EvaluationImpl other)
	{
		this.attribution = new AttributionImpl(other.attribution, null);
		this.changed = new ChangeableImpl(other.changed);
		this.comment = other.comment;
		this.evaluated = other.evaluated;
		this.score = other.score;
	}
}
