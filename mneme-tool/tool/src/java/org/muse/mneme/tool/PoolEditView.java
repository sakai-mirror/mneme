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
 * The pools delete view for the mneme tool.
 */
public class PoolEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolEditView.class);

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Pool Service */
	protected PoolService poolService = null;

	/** Question Service */
	protected QuestionService questionService = null;

	/** Dependency: ToolManager */
	protected ToolManager toolManager = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

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
		if ((params.length != 4) && (params.length != 5) && (params.length != 6) && (params.length != 7)) throw new IllegalArgumentException();

		// pools - sort at index 2, paging at index 3. pool id at index 4. Move pool_edit sort to index 5, paging to index 6
		// pools sort parameter is in param array at index 2
		String poolsSortCode = null;
		poolsSortCode = params[2];
		context.put("poolsSortCode", poolsSortCode);

		// pools paging parameter - is in param array at index 3
		String poolsPagingParameter = null;
		poolsPagingParameter = params[3];
		context.put("poolsPagingParameter", poolsPagingParameter);

		// setup the model: the selected pool - pool id is at index 4
		Pool pool = this.poolService.getPool(params[4]);
		context.put("pool", pool);

		// sort parameter - sort is in param array at index 5
		String sortCode = null;
		if (params.length > 5) sortCode = params[5];

		// paging parameter - is in param array at index 6
		String pagingParameter = null;
		if (params.length == 7) pagingParameter = params[6];

		// default sort is title ascending
		QuestionService.FindQuestionsSort sort;
		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));

				// 0 is title
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
					sort = QuestionService.FindQuestionsSort.type_a;
				else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
					sort = QuestionService.FindQuestionsSort.type_d;
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
			sort = QuestionService.FindQuestionsSort.type_a;

			context.put("sort_column", '0');
			context.put("sort_direction", 'A');
		}

		// default paging
		if (pagingParameter == null)
		{
			// TODO: other than 2 size!
			pagingParameter = "1-30";
		}
		// total questions
		Integer maxQuestions = this.questionService.countQuestions(pool, null);

		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxQuestions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);
		context.put("pagingParameter", pagingParameter);

		// get questions
		List<Question> questions = questionService.findQuestions(pool, sort, null, paging.getCurrent(), paging.getSize());
		context.put("questions", questions);

		// the question types
		List<QuestionPlugin> questionTypes = this.mnemeService.getQuestionPlugins();
		context.put("questionTypes", questionTypes);

		// for the checkboxes
		Values values = this.uiService.newValues();
		context.put("questionids", values);

		uiService.render(ui, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if ((params.length != 3) && (params.length != 4) && (params.length != 5) && (params.length != 6) && (params.length != 7))
			throw new IllegalArgumentException();

		String poolsSortCode = null;
		if (params.length > 2) poolsSortCode = params[2];
		context.put("poolsSortCode", poolsSortCode);

		// pools paging parameter - is in param array at index 3
		String poolsPagingParameter = null;
		if (params.length > 3) poolsPagingParameter = params[3];
		context.put("poolsPagingParameter", poolsPagingParameter);

		// for the selected questions to delete
		Values values = this.uiService.newValues();
		context.put("questionids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		String[] selectedQuestionIds = values.getValues();

		if (destination != null)
		{
			if (destination.startsWith("/questions_delete"))
			{
				// delete the questions
				if (selectedQuestionIds != null && (selectedQuestionIds.length > 0))
				{
					StringBuffer path = new StringBuffer();
					String separator = "+";

					path.append(destination);
					path.append("/");

					for (String selectedQuestionId : selectedQuestionIds)
					{
						path.append(selectedQuestionId);
						path.append(separator);
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}
			else if (destination.trim().startsWith("/question_duplicate"))
			{
				try
				{
					String destinationParams[] = destination.split("/");
					Pool pool = this.poolService.getPool(destinationParams[4]);
					Question question = this.questionService.getQuestion(destinationParams[5]);

					if (pool != null && question != null)
						this.questionService.copyQuestion(question, pool);

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, context.getDestination())));
					return;
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
				if (selectedQuestionIds != null && (selectedQuestionIds.length > 0))
				{
					StringBuffer path = new StringBuffer();
					String separator = "+";

					path.append(destination);
					path.append("/");

					for (String selectedQuestionId : selectedQuestionIds)
					{
						path.append(selectedQuestionId);
						path.append(separator);
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}
			// handle adding a question
			else if (destination.startsWith("/select_question_type"))
			{
				Pool pool = this.poolService.getPool(params[4]);

				if (pool == null)
				{
					// TODO: do this better!
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}

				// check security
				if (!this.poolService.allowManagePools(toolManager.getCurrentPlacement().getContext()))
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
				// redirect
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
			else
			{
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
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

	/**
	 * @param mnemeService
	 *        the mnemeService to set
	 */
	public void setMnemeService(MnemeService mnemeService)
	{
		this.mnemeService = mnemeService;
	}

}
