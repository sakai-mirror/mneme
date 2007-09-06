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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The question edit view for the mneme tool.
 */
public class QuestionEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionEditView.class);

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
		// 1 parameter - qid
		// TODO: possible other sort / page preservation parameters
		if ((params.length != 3)) throw new IllegalArgumentException();
		String questionId = params[2];

		// setup the model: the selected pool - pool id is at index 4
		Question question = this.questionService.getQuestion(questionId);
		if (question == null) throw new IllegalArgumentException();

		// check security
		if (!this.questionService.allowEditQuestion(question, toolManager.getCurrentPlacement().getContext(), null))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// put the question in the context
		context.put("question", question);

		uiService.render(ui, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// 1 parameter - qid
		// TODO: possible other sort / page preservation parameters
		if ((params.length != 3)) throw new IllegalArgumentException();
		String questionId = params[2];

		// setup the model: the selected pool - pool id is at index 4
		Question question = this.questionService.getQuestion(questionId);
		if (question == null) throw new IllegalArgumentException();

		// put the question in the context
		context.put("question", question);

		// read form
		String destination = this.uiService.decode(req, context);

		// save
		try
		{
			this.questionService.saveQuestion(question, toolManager.getCurrentPlacement().getContext());
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// redirect
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
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
}
