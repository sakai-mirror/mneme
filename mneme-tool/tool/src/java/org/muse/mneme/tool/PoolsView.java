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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /pools view for the mneme tool.
 */
public class PoolsView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolsView.class);

	/** Assessment service. */
	protected PoolService poolService = null;

	/** tool manager */
	protected ToolManager toolManager = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

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
		if ((params.length != 2) && (params.length != 3) && (params.length != 4))
		{
			throw new IllegalArgumentException();
		}

		// sort parameter
		String sortCode = null;
		if (params.length > 2)
		{
			//sort is in param array at index 2
			sortCode = params[2];
		}
		
		// paging parameter
		String pagingParameter = null;
		if (params.length > 3)
		{
			//paging parameter is in param array at index 3
			pagingParameter = params[3];
		}

		// default sort is title ascending
		PoolService.FindPoolsSort sort;

		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));

				// 0 is subject
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
				{
					sort = PoolService.FindPoolsSort.subject_a;
				}
				else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
				{
					sort = PoolService.FindPoolsSort.subject_d;
				}
				// 1 is title
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
				{
					sort = PoolService.FindPoolsSort.title_a;
				}
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
				{
					sort = PoolService.FindPoolsSort.title_d;
				}
				else
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
				return;
			}
		}
		else
		{
			// default sort: title ascending
			sort = PoolService.FindPoolsSort.title_a;

			context.put("sort_column", '1');
			context.put("sort_direction", 'A');

		}

		// how many pools, total?
		Integer maxPools = this.poolService.countPools(null, null);

		// default paging
		if (pagingParameter == null)
		{
			// TODO: other than 2 size!
			pagingParameter = "1-2";
		}

		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxPools);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);
		context.put("pagingParameter", pagingParameter);

		try
		{
			// collect the pools to show
			List<Pool> pools = this.poolService.findPools(null, sort, null, paging.getCurrent(), paging.getSize());
			context.put("pools", pools);
		}
		catch (Exception e)
		{
			if (M_log.isErrorEnabled()) M_log.error(e.toString());
			e.printStackTrace();
		}

		// for the checkboxes
		Values values = this.uiService.newValues();
		context.put("poolids", values);

		// render
		uiService.render(ui, context);
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
		// throw new IllegalArgumentException();
		// for the selected pools to delete
		Values values = this.uiService.newValues();
		context.put("poolids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		String[] selectedPoolIds = values.getValues();

		if (destination != null)
		{
			if (destination.trim().equalsIgnoreCase("/pools_delete"))
			{
				// delete the pools
				if (selectedPoolIds != null && (selectedPoolIds.length > 0))
				{
					StringBuffer path = new StringBuffer();
					String separator = "/";

					path.append(destination);

					// for sort
					if (params.length > 2)
					{
						path.append(separator);
						path.append(params[2]);
					}
					else
					{
						// default sort - title ascending
						path.append(separator);
						path.append("1A");
					}
					
					//for paging 
					if (params.length > 3)
					{
						path.append(separator);
						path.append(params[3]);
					}
					else
					{
						// default paging 
						path.append(separator);
						path.append("1-2");
					}

					for (String selectedPoolId : selectedPoolIds)
					{
						path.append(separator);
						path.append(selectedPoolId);
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}
			else if (destination.trim().equalsIgnoreCase("/pool_properties"))
			{
				try
				{
					// create new pool and redirect to pool properties
					Pool newPool = this.poolService.newPool(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId());
					//title and subject are required as sort may fail on pools page
					newPool.setTitle("");
					newPool.setSubject("");
					this.poolService.savePool(newPool, toolManager.getCurrentPlacement().getContext());

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination + "/" + newPool.getId())));
					return;
				}
				catch (Exception e)
				{
					if (M_log.isErrorEnabled()) M_log.error(e.toString());
					e.printStackTrace();
				}
			}
		}

		if (params.length == 3)
			destination = "/pools/" + params[2];
		else
			destination = "/pools/";

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}
	
	/**
	 * @param sessionManager
	 *        the sessionManager to set
	 */
	public void setSessionManager(SessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
	}

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
