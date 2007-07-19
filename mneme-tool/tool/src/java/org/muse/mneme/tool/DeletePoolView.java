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
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.util.Web;

/**
 * The pools delete view for the mneme tool.
 */
public class DeletePoolView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(DeletePoolView.class);

	/** Pool Service*/
	protected PoolService poolService = null;

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}

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

		if (params.length < 3)
		{
			throw new IllegalArgumentException();
		}
		List<Pool> pools = new ArrayList<Pool>(0);
		StringBuffer deletePoolIds = new StringBuffer();

		//pool id's are in the params array from the index 2
		for (int i = 2; i < params.length; i++)
		{
			//get the pool and add to the list to show			
			Pool pool = this.poolService.idPool(params[i]);

			if (pool != null) pools.add(pool);
		}

		context.put("pools", pools);
		uiService.render(ui, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		//throw new IllegalArgumentException();
		String destination = this.uiService.decode(req, context);

		if (destination != null && (destination.trim().equalsIgnoreCase("/pools_delete")))
		{
			try
			{
				//pool id's are in the params array from the index 2
				for (int i = 2; i < params.length; i++)
				{
					Pool pool = this.poolService.idPool(params[i]);
					if (pool != null)
					{
						this.poolService.removePool(pool);
					}
				}
			}
			catch (Exception e)
			{
				if (M_log.isErrorEnabled()) M_log.error(e.toString());
				e.printStackTrace();
			}
		}
		destination = "/pools";
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * @return the poolService
	 */
	public PoolService getPoolService()
	{
		return this.poolService;
	}

	/**
	 * @param poolService the poolService to set
	 */
	public void setPoolService(PoolService poolService)
	{
		this.poolService = poolService;
	}

}
