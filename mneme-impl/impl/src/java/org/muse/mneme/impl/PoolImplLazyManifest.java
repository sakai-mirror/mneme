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

import java.util.List;

import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolManifestService;
import org.muse.mneme.api.PoolService;

/**
 * PoolImpl implements Pool
 */
public class PoolImplLazyManifest extends PoolImpl implements Pool
{
	/** Set to true once the manifest is loaded. */
	protected transient Boolean manifestLoaded = Boolean.FALSE;

	/** Set if we are just made historical. */
	protected transient Boolean newlyHistorical = Boolean.FALSE;

	/** Dependency: PoolManifestService. */
	protected transient PoolManifestService poolService = null;

	/**
	 * Construct.
	 */
	public PoolImplLazyManifest()
	{
		super();
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PoolImplLazyManifest(PoolImpl other)
	{
		super(other);
		this.manifestLoaded = ((PoolImplLazyManifest) other).manifestLoaded;
		this.newlyHistorical = ((PoolImplLazyManifest) other).newlyHistorical;
		this.poolService = ((PoolImplLazyManifest) other).poolService;
	}

	/**
	 * Dependency: PoolManifestService.
	 * 
	 * @param service
	 *        The PoolManifestService.
	 */
	public void setPoolService(PoolManifestService service)
	{
		this.poolService = service;
	}

	/**
	 * Clear the newly historical setting.
	 */
	protected void clearNewlyHistorical()
	{
		this.newlyHistorical = Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected List<String> getFrozenManifest()
	{
		// lazy load the manifest if needed
		if (!this.manifestLoaded)
		{
			this.frozenManifest = this.poolService.getManifest(this);
			this.manifestLoaded = Boolean.TRUE;
		}

		return super.getFrozenManifest();
	}

	/**
	 * Check if we were just made historical.
	 * 
	 * @return TRUE if we were just made historical, FALSE if not.
	 */
	protected Boolean getNewlyHistorical()
	{
		return this.newlyHistorical;
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean makeHistorical(Pool current)
	{
		boolean rv = super.makeHistorical(current);
		if (rv)
		{
			// mark as being just made historical and having its manifest loaded
			this.newlyHistorical = Boolean.TRUE;
			this.manifestLoaded = Boolean.TRUE;
		}

		return rv;
	}
}
