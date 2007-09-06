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
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /question_copy view for the mneme tool.
 */
public class QuestionsCopyMoveView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionsCopyMoveView.class);

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Pool Service */
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
		if (params.length != 8) throw new IllegalArgumentException();
		context.put("saveDestination", context.getDestination());

		// pools sort param is in params array at index 2
		context.put("poolsSortCode", params[2]);

		// pools paging parameter - is in params array at index 3
		context.put("poolsPagingParameter", params[3]);

		// header and icon dependent on which function
		if (path.startsWith("question_copy"))
		{
			context.put("headerText", messages.get("copy-header-text"));
			context.put("headerIcon", "/icons/copy.png");
		}
		else if (path.startsWith("question_move"))
		{
			context.put("headerText", messages.get("move-header-text"));
			context.put("headerIcon", "/icons/page_go.png");
		}

		// pools id is in params array at index 4
		context.put("poolid", params[4]);

		Pool pool = this.poolService.getPool(params[4]);

		List<Pool> pools = this.poolService.findPools(toolManager.getCurrentPlacement().getContext(), null, null, null, null, null);
		pools.remove(pool);
		context.put("pools", pools);

		// pools sort param is in params array at index 5
		context.put("poolsEditSortCode", params[5]);

		// pools paging parameter - is in params array at index 6
		context.put("poolsEditPagingParameter", params[6]);

		// for the selected pool
		Value value = this.uiService.newValue();
		context.put("selectedPoolId", value);

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
		if (params.length != 8) throw new IllegalArgumentException();

		// for the selected pool
		Value value = this.uiService.newValue();
		context.put("selectedPoolId", value);

		// read form
		String destination = this.uiService.decode(req, context);

		String selectedPoolId = value.getValue();

		if (selectedPoolId != null)
		{
			Pool pool = this.poolService.getPool(selectedPoolId);

			Question question = null;

			try
			{
				// question id's are in the params array at the index 7
				String questionIds[] = params[7].split("\\+");

				for (String questionId : questionIds)
				{
					// get the question
					question = this.questionService.getQuestion(questionId);

					// which function to perform
					if (path.startsWith("question_copy"))
					{
						this.questionService.copyQuestion(toolManager.getCurrentPlacement().getContext(), null, pool, question);
					}
					else if (path.startsWith("question_move"))
					{
						this.questionService.moveQuestion(toolManager.getCurrentPlacement().getContext(), null, question, pool);
					}
				}
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}

			if (params.length > 6) destination = "/pool_edit/" + params[2] + "/" + params[3] + "/" + params[4] + "/" + params[5] + "/" + params[6];

			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
			return;
		}
		else
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
	}

	/**
	 * @param mnemeService
	 *        the mnemeService to set
	 */
	public void setMnemeService(MnemeService mnemeService)
	{
		this.mnemeService = mnemeService;
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
	 * @param questionService
	 *        the questionService to set
	 */
	public void setQuestionService(QuestionService questionService)
	{
		this.questionService = questionService;
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
