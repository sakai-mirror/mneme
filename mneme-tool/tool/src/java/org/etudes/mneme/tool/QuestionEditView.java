/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 Etudes, Inc.
 * 
 * Portions completed before September 1, 2008
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

package org.etudes.mneme.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.ambrosia.api.Context;
import org.etudes.ambrosia.api.Value;
import org.etudes.ambrosia.util.ControllerImpl;
import org.etudes.mneme.api.AssessmentPermissionException;
import org.etudes.mneme.api.MnemeService;
import org.etudes.mneme.api.Question;
import org.etudes.mneme.api.QuestionPlugin;
import org.etudes.mneme.api.QuestionService;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The question_edit view for the mneme tool.
 */
public class QuestionEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionEditView.class);

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Question Service */
	protected QuestionService questionService = null;

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
		// we need a qid, then any number of parameters to form the return destination
		if (params.length < 3)
		{
			throw new IllegalArgumentException();
		}

		String destination = null;
		if (params.length > 3)
		{
			destination = "/" + StringUtil.unsplit(params, 3, params.length - 3, "/");
		}

		// if not specified, go to the main pools page
		else
		{
			destination = "/pools";
		}
		context.put("return", destination);

		String questionId = params[2];

		// get the question to work on
		Question question = this.questionService.getQuestion(questionId);
		if (question == null) throw new IllegalArgumentException();

		// check security
		if (!this.questionService.allowEditQuestion(question))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// put the question in the context
		context.put("question", question);

		// the question types
		List<QuestionPlugin> questionTypes = this.mnemeService.getQuestionPlugins();
		context.put("questionTypes", questionTypes);

		// select the question's current type
		Value value = this.uiService.newValue();
		value.setValue(question.getType());
		context.put("selectedQuestionType", value);

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
		// we need a question id, then any number of parameters to form the return destination
		if (params.length < 3)
		{
			throw new IllegalArgumentException();
		}

		String returnDestination = null;
		if (params.length > 3)
		{
			returnDestination = "/" + StringUtil.unsplit(params, 3, params.length - 3, "/");
		}

		// if not specified, go to the main pools page
		else
		{
			returnDestination = "/pools";
		}

		String questionId = params[2];

		// get the question to work on
		Question question = this.questionService.getQuestion(questionId);
		if (question == null) throw new IllegalArgumentException();

		// put the question in the context
		context.put("question", question);

		// for the selected question type
		Value newType = this.uiService.newValue();
		context.put("selectedQuestionType", newType);

		// read form
		String destination = this.uiService.decode(req, context);

		// consolidate the question
		destination = question.getTypeSpecificQuestion().consolidate(destination);

		// save
		try
		{
			if ("RETYPE".equals(destination))
			{
				// save and re-type
				this.questionService.saveQuestionAsType(question, newType.getValue());
			}

			else
			{
				// just save
				this.questionService.saveQuestion(question);
			}

			if ("ADD".equals(destination))
			{
				destination = null;

				// setup a new question of the same type in the same pool
				try
				{
					Question newQuestion = this.questionService.newQuestion(question.getPool(), question.getType());

					// TODO: added to any assessment part?

					// edit it
					destination = "/question_edit/" + newQuestion.getId() + returnDestination;
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// if destination became null, or is the stay here
		if ((destination == null) || ("STAY".equals(destination) || "RETYPE".equals(destination)))
		{
			destination = context.getDestination();
		}

		// redirect
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
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
	 * @param questionService
	 *        the questionService to set
	 */
	public void setQuestionService(QuestionService questionService)
	{
		this.questionService = questionService;
	}
}
