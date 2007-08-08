/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The pools delete view for the mneme tool.
 */
public class PoolsDeleteView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolsDeleteView.class);

	/** Pool Service */
	protected PoolService poolService = null;

	/** tool manager reference. */
	protected ToolManager toolManager = null;

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		String destination = this.uiService.decode(req, context);

		if (params.length < 4) throw new IllegalArgumentException();

		List<Pool> pools = new ArrayList<Pool>(0);
		StringBuffer deletePoolIds = new StringBuffer();

		// pool id's are in the params array from the index 4
		for (int i = 4; i < params.length; i++)
		{
			// get the pool and add to the list to show
			Pool pool = this.poolService.getPool(params[i]);

			if (pool != null) pools.add(pool);
		}

		context.put("pools", pools);
		// sort code
		context.put("sortcode", params[2]);
		
		context.put("pagingParameter", params[3]);

		uiService.render(ui, context);
	}

	/**
	 * @return the poolService
	 */
	public PoolService getPoolService()
	{
		return this.poolService;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (params.length < 4) throw new IllegalArgumentException();

		String destination = this.uiService.decode(req, context);

		if (destination != null && (destination.trim().equalsIgnoreCase("/pools_delete")))
		{
			
				// pool id's are in the params array from the index 3
				for (int i = 4; i < params.length; i++)
				{
					Pool pool = this.poolService.getPool(params[i]);
					if (pool != null)
					{
						try
						{
							this.poolService.removePool(pool, toolManager.getCurrentPlacement().getContext());
						}
						catch (AssessmentPermissionException e)
						{
							//redirect to error
							res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
							return;
						}						
					}
				}
			
		}
		//add sort and paging
		destination = "/pools/" + params[2] + "/"+ params[3];
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * @param poolService
	 *        the poolService to set
	 */
	public void setPoolService(PoolService poolService)
	{
		this.poolService = poolService;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param manager
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager manager)
	{
		toolManager = manager;
	}
}
