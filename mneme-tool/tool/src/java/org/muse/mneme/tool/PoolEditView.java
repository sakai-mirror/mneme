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
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The pools delete view for the mneme tool.
 */
public class PoolEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolEditView.class);

	/** Pool Service */
	protected PoolService poolService = null;

	/** Question Service */
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
		// pools sort, pools paging, pool id, optional sort, optional paging
		if ((params.length != 5) && (params.length != 6) && (params.length != 7))
		{
			throw new IllegalArgumentException();
		}

		if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// pools view sort
		String poolsSortCode = null;
		poolsSortCode = params[2];
		context.put("poolsSortCode", poolsSortCode);

		// pools view paging parameter
		String poolsPagingParameter = null;
		poolsPagingParameter = params[3];
		context.put("poolsPagingParameter", poolsPagingParameter);

		// pool
		String pid = params[4];
		Pool pool = this.poolService.getPool(pid);
		if (pool == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("pool", pool);

		// sort
		String sortCode = "0A";
		if (params.length > 5) sortCode = params[5];
		if ((sortCode == null) || (sortCode.length() != 2))
		{
			throw new IllegalArgumentException();
		}
		context.put("sort_column", sortCode.charAt(0));
		context.put("sort_direction", sortCode.charAt(1));
		QuestionService.FindQuestionsSort sort = null;
		// 0 is description
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
			sort = QuestionService.FindQuestionsSort.description_a;
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
			sort = QuestionService.FindQuestionsSort.description_d;
		// 1 is type
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
			sort = QuestionService.FindQuestionsSort.type_a;
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
			sort = QuestionService.FindQuestionsSort.type_d;
		else
		{
			throw new IllegalArgumentException();
		}

		// paging
		String pagingParameter = "1-30";
		if (params.length > 6) pagingParameter = params[6];
		Integer maxQuestions = this.questionService.countQuestions(pool, null);
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxQuestions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// get questions
		List<Question> questions = questionService.findQuestions(pool, sort, null, paging.getCurrent(), paging.getSize());
		context.put("questions", questions);

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
		// pools sort, pools paging, pool id, optional sort, optional paging
		if ((params.length != 5) && (params.length != 6) && (params.length != 7))
		{
			throw new IllegalArgumentException();
		}

		if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// for the selected questions to delete
		Values values = this.uiService.newValues();
		context.put("questionids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		if (destination.equals("DELETE"))
		{
			for (String id : values.getValues())
			{
				Question question = this.questionService.getQuestion(id);
				if (question != null)
				{
					try
					{
						this.questionService.removeQuestion(question);
					}
					catch (AssessmentPermissionException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
						return;
					}
				}
			}

			// stay here
			destination = context.getDestination();
		}

		else if (destination.trim().startsWith("DUPLICATE:"))
		{
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length != 2)
			{
				throw new IllegalArgumentException();
			}
			String qid = parts[1];
			try
			{
				Question question = this.questionService.getQuestion(qid);
				if (question != null)
				{
					// copy within the same pool
					this.questionService.copyQuestion(question, null);
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

		else if ((destination.trim().startsWith("/question_copy")) || (destination.trim().startsWith("/question_move")))
		{
			// add the selected ids to the destination
			StringBuilder buf = new StringBuilder();
			buf.append(destination);
			buf.append("/");
			for (String id : values.getValues())
			{
				buf.append(id);
				buf.append("+");
			}
			buf.setLength(buf.length() - 1);

			destination = buf.toString();
		}

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
