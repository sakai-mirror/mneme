/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/SelectAddPartQuestionsView.java $
 * $Id: SelectAddPartQuestionsView.java 11577 2007-08-28 05:39:09Z maheshwarirashmi@foothill.edu $
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
import java.util.ArrayList;

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
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.ManualPart;
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
public class SelectAddPartQuestionsView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SelectAddPartQuestionsView.class);

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Dependency: Pool service. */
	protected AssessmentService assessmentService = null;

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
		String destination = this.uiService.decode(req, context);
		context.put("testsortcode", params[2]);
		context.put("assessmentId", params[3]);
		context.put("partId", params[4]);

		Assessment assessment = assessmentService.getAssessment(params[3]);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("assessmentTitle", assessment.getTitle());

		ManualPart part = (ManualPart) assessment.getParts().getPart(params[4]);

		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("partTitle", part.getTitle());

		String pagingParameter = null;
		Paging paging = uiService.newPaging();

		// paging
		if (pagingParameter == null)
		{
			pagingParameter = "1-30";
		}

		Integer maxQuestions = this.questionService.countQuestions(sessionManager.getCurrentSessionUserId(), null, null);

		paging.setMaxItems(maxQuestions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);
		context.put("pagingParameter", pagingParameter);

		// default sort: title ascending
		QuestionService.FindQuestionsSort sort = QuestionService.FindQuestionsSort.type_a;
		String sortCode = null;
		if (params.length > 5)
		{
			sortCode = params[5];
			if (sortCode != null && sortCode.length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));
				sort = findQuestionSortCode(sortCode);
			}
		}
		else
		{
			context.put("sort_column", '0');
			context.put("sort_direction", 'A');
		}
		// get questions
		List<Question> questions = questionService.findQuestions(sessionManager.getCurrentSessionUserId(), null, sort, null, paging.getCurrent(),
				paging.getSize());

		context.put("questions", questions);

		// for the checkboxes
		Values values = this.uiService.newValues();
		context.put("questionids", values);
		// render
		uiService.render(ui, context);
	}

	private QuestionService.FindQuestionsSort findQuestionSortCode(String sortCode)
	{
		QuestionService.FindQuestionsSort sort = QuestionService.FindQuestionsSort.type_a;
		// 0 is question type
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.type_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.type_d;
		}
		// 1 is pool subject
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.pool_subject_a;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.pool_subject_d;
		}
		// 2 is pool title
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.pool_title_a;
		}
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.pool_title_d;
		}
		// 3 is pool points
		else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.pool_points_a;
		}
		else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.pool_points_d;
		}

		return sort;
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
		Values values = this.uiService.newValues();
		context.put("questionids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		String assessmentId = params[3];
		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("testsortcode", params[2]);

		String partId = params[4];
		ManualPart part = (ManualPart) assessment.getParts().getPart(partId);

		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		String[] selectedQuesIds = values.getValues();
		if (selectedQuesIds != null && selectedQuesIds.length != 0)
		{
			for (String selectedQuesId : selectedQuesIds)
			{
				Question ques = this.questionService.getQuestion(selectedQuesId);
				if (ques != null) part.addQuestion(ques);
			}
		}

		// commit the save
		try
		{
			this.assessmentService.saveAssessment(assessment);
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}
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
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
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
