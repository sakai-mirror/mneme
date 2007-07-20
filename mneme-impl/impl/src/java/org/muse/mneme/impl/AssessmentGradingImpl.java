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

import org.muse.mneme.api.AssessmentGrading;

/**
 * AssessmentGradingImpl implements AssessmentGrading
 */
public class AssessmentGradingImpl implements AssessmentGrading
{
	protected Boolean autoRelease = Boolean.FALSE;

	protected Boolean gradebookIntegration = Boolean.FALSE;

	protected Boolean showIdentities = Boolean.FALSE;

	/**
	 * Construct.
	 */
	public AssessmentGradingImpl()
	{
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentGradingImpl(AssessmentGradingImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAutoRelease()
	{
		return this.autoRelease;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getGradebookIntegration()
	{
		return this.gradebookIntegration;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getShowIdentities()
	{
		return this.showIdentities;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAutoRelease(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.autoRelease = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setGradebookIntegration(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.gradebookIntegration = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowIdentities(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.showIdentities = setting;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentGradingImpl other)
	{
		this.autoRelease = other.autoRelease;
		this.gradebookIntegration = other.gradebookIntegration;
		this.showIdentities = other.showIdentities;
	}
}
