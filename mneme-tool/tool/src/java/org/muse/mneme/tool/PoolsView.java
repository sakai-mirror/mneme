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
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.SessionManager;
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

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Dependency: Pool service. */
	protected PoolService poolService = null;

	/** Dependency: Question service. */
	protected QuestionService questionService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

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
		if ((params.length != 2) && (params.length != 3) && (params.length != 4))
		{
			throw new IllegalArgumentException();
		}

		// sort parameter
		String sortCode = null;
		if (params.length > 2)
		{
			// sort is in param array at index 2
			sortCode = params[2];
		}

		// paging parameter
		String pagingParameter = null;
		if (params.length > 3)
		{
			// paging parameter is in param array at index 3
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
		Integer maxPools = this.poolService.countPools(toolManager.getCurrentPlacement().getContext(), null, null);

		// default paging
		if (pagingParameter == null)
		{
			// TODO: other than 2 size!
			pagingParameter = "1-30";
		}

		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxPools);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);
		context.put("pagingParameter", pagingParameter);

		// collect the pools to show
		List<Pool> pools = this.poolService.findPools(toolManager.getCurrentPlacement().getContext(), null, sort, null, paging.getCurrent(), paging
				.getSize());
		context.put("pools", pools);

		// for the checkboxes
		Values values = this.uiService.newValues();
		context.put("poolids", values);

		// the question types
		List<QuestionPlugin> questionTypes = this.mnemeService.getQuestionPlugins();
		context.put("questionTypes", questionTypes);

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

			if (destination.startsWith("/pools_delete"))
			{
				// delete the pools
				if (selectedPoolIds != null && (selectedPoolIds.length > 0))
				{
					StringBuffer path = new StringBuffer();
					String separator = "+";

					path.append(destination);
					path.append("/");

					for (String selectedPoolId : selectedPoolIds)
					{
						path.append(selectedPoolId);
						path.append(separator);
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}

			else if (destination.trim().startsWith("/pool_properties"))
			{
				try
				{
					// create new pool and redirect to pool properties
					Pool newPool = this.poolService.newPool(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId());

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination + "/" + newPool.getId())));
					return;
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}
			else if (destination.trim().startsWith("/COMBINE"))
			{
				try
				{
					// create new pool and redirect to pool properties
					Pool newPool = this.poolService.newPool(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId());

					if (selectedPoolIds != null && (selectedPoolIds.length > 0))
					{
						for (String selectedPoolId : selectedPoolIds)
						{
							Pool sourcePool = this.poolService.getPool(selectedPoolId);
							this.questionService.copyPoolQuestions(toolManager.getCurrentPlacement().getContext(), sessionManager
									.getCurrentSessionUserId(), sourcePool, newPool);
						}

						destination = destination.replace("COMBINE", "pool_properties");
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination + "/" + newPool.getId())));
						return;
					}
					else
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
						return;
					}
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}
			else if (destination.trim().startsWith("/pool_duplicate"))
			{
				try
				{
					Pool pool = this.poolService.getPool(destination.substring(destination.lastIndexOf("/") + 1));
					if (pool != null)
						this.poolService.copyPool(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId(), pool);
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}

			// handle adding a question
			else if (destination.startsWith("ADDQ:"))
			{
				// require one pool selected
				if ((selectedPoolIds == null) || (selectedPoolIds.length != 1))
				{
					// TODO: do this better!
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}

				Pool pool = this.poolService.getPool(selectedPoolIds[0]);
				if (pool == null)
				{
					// TODO: do this better!
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}

				// parse the type (after the : in the destination)
				String type = StringUtil.splitFirst(destination, ":")[1];

				// check security
				if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext(), null))
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				// create the question of the appropriate type (all the way to save)
				Question newQuestion = null;
				try
				{
					newQuestion = this.questionService.newQuestion(toolManager.getCurrentPlacement().getContext(), null, pool, type);
					this.questionService.saveQuestion(newQuestion, toolManager.getCurrentPlacement().getContext());
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				// form the destionation
				// TODO: preserve sort / paging parameters
				destination = "/question_edit/" + newQuestion.getId();

				// redirect
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
		}

		if (params.length == 4)
			destination = "/pools/" + params[2] + "/" + params[3];
		else
			destination = "/pools/";

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the MnemeService.
	 * 
	 * @param service
	 *        the MnemeService.
	 */
	public void setMnemeService(MnemeService sevice)
	{
		this.mnemeService = sevice;
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
	 * @param sessionManager
	 *        the SessionManager.
	 */
	public void setSessionManager(SessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
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
