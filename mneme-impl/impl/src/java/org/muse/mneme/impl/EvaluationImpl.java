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
import org.muse.mneme.api.Evaluation;

/**
 * EvaluationImpl implements Evaluation
 */
public class EvaluationImpl implements Evaluation
{
	protected AttributionImpl attribution = new AttributionImpl();

	protected String comment = null;

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
	public Float getScore()
	{
		return this.score;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setComment(String comment)
	{
		this.comment = comment;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScore(Float score)
	{
		this.score = score;
	}

	/**
	 * Set values to a copy of other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(EvaluationImpl other)
	{
		this.attribution = new AttributionImpl(other.attribution);
		this.comment = other.comment;
		this.score = other.score;
	}
}
