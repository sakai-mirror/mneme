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
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The questions delete view for the mneme tool.
 */
public class QuestionsDeleteView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionsDeleteView.class);

	/** Question Service */
	protected QuestionService questionService = null;
	
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
		if (params.length != 8) throw new IllegalArgumentException();

		String destination = this.uiService.decode(req, context);

		List<Question> questions = new ArrayList<Question>(0);
		
		// question id's are in the params array at the index 7
		String questionIds[] = params[7].split("\\+");

		for (String selectedQuestionId : questionIds)
		{
			Question question = null;
			// get the question and add to the list
			question = this.questionService.getQuestion(selectedQuestionId);

			if (question != null) questions.add(question);
		}
		
		//pools sort code is in params array at index 2
		context.put("poolsSortCode", params[2]);

		// pools paging parameter - is in param array at index 3
		context.put("poolsPagingParameter", params[3]);

		context.put("questions", questions);
		
		context.put("poolid", params[4]);

		context.put("sortcode", params[5]);

		context.put("pagingParameter", params[6]);

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
		
		String destination = this.uiService.decode(req, context);
		
		if (destination != null && (destination.trim().equalsIgnoreCase("/questions_delete")))
		{
			//question id's are in the params array at the index 7
			String questionIds[] = params[7].split("\\+");

			for (String selectedQuestionId : questionIds)
			{
				Question question = null;
				question = this.questionService.getQuestion(selectedQuestionId);
				if (question != null)
				{
					try
					{
						this.questionService.removeQuestion(question, toolManager.getCurrentPlacement().getContext());
					}
					catch (AssessmentPermissionException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
						return;
					}
				}
			}
		}
		// add pools sort code, pools paging, pool id, sort and paging
		destination = "/pool_edit/" + params[2] + "/" + params[3] + "/" + params[4] + "/" + params[5] + "/" + params[6];
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
	 * @param toolManager the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
