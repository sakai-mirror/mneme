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
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /pools view for the mneme tool.
 */
public class PoolsView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolsView.class);

	/** Dependency: Pool service. */
	protected PoolService poolService = null;

	/** Dependency: Question service. */
	protected QuestionService questionService = null;

	/** Dependency: ToolManager */
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
		// sort, paging (both optional)
		if ((params.length != 2) && (params.length != 3) && (params.length != 4))
		{
			throw new IllegalArgumentException();
		}

		// security
		if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// sort parameter
		String sortCode = "0A";
		if (params.length > 2) sortCode = params[2];
		if ((sortCode == null) || (sortCode.length() != 2))
		{
			throw new IllegalArgumentException();
		}
		context.put("sort_column", sortCode.charAt(0));
		context.put("sort_direction", sortCode.charAt(1));

		// 0 is title
		PoolService.FindPoolsSort sort = null;
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = PoolService.FindPoolsSort.title_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = PoolService.FindPoolsSort.title_d;
		}
		else
		{
			throw new IllegalArgumentException();
		}

		// paging parameter
		String pagingParameter = "1-30";
		if (params.length > 3) pagingParameter = params[3];
		Integer maxPools = this.poolService.countPools(toolManager.getCurrentPlacement().getContext(), null);
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxPools);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// collect the pools to show
		List<Pool> pools = this.poolService.findPools(toolManager.getCurrentPlacement().getContext(), sort, null, paging.getCurrent(), paging
				.getSize());
		context.put("pools", pools);

		// disable the tool navigation to this view
		context.put("disablePools", Boolean.TRUE);

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
		// sort, paging (both optional)
		if ((params.length != 2) && (params.length != 3) && (params.length != 4))
		{
			throw new IllegalArgumentException();
		}

		// security
		if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// sort parameter
		String sortCode = "0A";
		if (params.length > 2) sortCode = params[2];

		// paging parameter
		String pagingParameter = "1-30";
		if (params.length > 3) pagingParameter = params[3];

		// for the selected pools to delete
		Values values = this.uiService.newValues();
		context.put("poolids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		if (destination.equals("DELETE"))
		{
			for (String id : values.getValues())
			{
				Pool pool = this.poolService.getPool(id);
				if (pool != null)
				{
					try
					{
						this.poolService.removePool(pool);
					}
					catch (AssessmentPermissionException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
						return;
					}
				}
			}

			destination = context.getDestination();
		}

		else if (destination.equals("ADD"))
		{
			try
			{
				// create new pool
				Pool newPool = this.poolService.newPool(toolManager.getCurrentPlacement().getContext());

				// edit it next
				destination = "/pool_properties/" + sortCode + "/" + pagingParameter + "/" + newPool.getId();
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		else if (destination.equals("COMBINE"))
		{
			try
			{
				// create a new pool
				Pool newPool = this.poolService.newPool(toolManager.getCurrentPlacement().getContext());

				// copy in all the questions
				for (String id : values.getValues())
				{
					Pool sourcePool = this.poolService.getPool(id);
					this.questionService.copyPoolQuestions(sourcePool, newPool);
				}

				// edit it next
				destination = "/pool_properties/" + sortCode + "/" + pagingParameter + "/" + newPool.getId();
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		else if (destination.startsWith("DUPLICATE:"))
		{
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length != 2)
			{
				throw new IllegalArgumentException();
			}
			String pid = parts[1];
			try
			{
				Pool pool = this.poolService.getPool(pid);
				if (pool != null)
				{
					this.poolService.copyPool(toolManager.getCurrentPlacement().getContext(), pool);
				}

				// stay here
				destination = context.getDestination();
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

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
	 * Set the QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * @param toolManager
	 *        the ToolManager.
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
