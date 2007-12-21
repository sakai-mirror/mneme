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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /pools view for the mneme tool.
 */
public class SelectAddPartQuestionsView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SelectAddPartQuestionsView.class);

	/** Dependency: Pool service. */
	protected AssessmentService assessmentService = null;

	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;

	/** Dependency: PoolService. */
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
		// [2]sort for /assessment, [3]aid |[4] pid |optional->| [5]our sort, [6]our page, [7] our type filter, [8] our pool filter
		if (params.length < 5 || params.length > 9) throw new IllegalArgumentException();

		// assessment view sort
		String assessmentSort = params[2];
		context.put("assessmentSort", assessmentSort);

		// assessment
		String assessmentId = params[3];
		Assessment assessment = assessmentService.getAssessment(params[3]);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("assessment", assessment);

		// part
		String partId = params[4];
		ManualPart part = (ManualPart) assessment.getParts().getPart(params[4]);
		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("part", part);

		// sort
		String sortCode = "0A";
		if (params.length > 5) sortCode = params[5];
		if ((sortCode == null) || (sortCode.length() != 2))
		{
			throw new IllegalArgumentException();
		}
		context.put("sort_column", sortCode.charAt(0));
		context.put("sort_direction", sortCode.charAt(1));
		QuestionService.FindQuestionsSort sort = findQuestionSortCode(sortCode);

		String typeFilter = (params.length > 7) ? params[7] : "0";
		context.put("typeFilter", typeFilter);

		// the question types
		List<QuestionPlugin> questionTypes = this.mnemeService.getQuestionPlugins();
		context.put("questionTypes", questionTypes);

		String poolFilter = (params.length > 8) ? params[8] : "0";
		context.put("poolFilter", poolFilter);
		Pool pool = poolFilter.equals("0") ? null : this.poolService.getPool(poolFilter);

		// the pools
		List<Pool> pools = this.poolService.getPools(toolManager.getCurrentPlacement().getContext());
		context.put("pools", pools);

		// paging
		Integer maxQuestions = null;
		if (pool == null)
		{
			maxQuestions = this.questionService.countQuestions(this.toolManager.getCurrentPlacement().getContext(), null,
					(typeFilter.equals("0") ? null : typeFilter));
		}
		else
		{
			maxQuestions = this.questionService.countQuestions(pool, null, (typeFilter.equals("0") ? null : typeFilter));
		}
		String pagingParameter = "1-30";
		if (params.length > 6) pagingParameter = params[6];
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxQuestions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// get questions
		List<Question> questions = null;

		if (pool == null)
		{
			questions = questionService.findQuestions(this.toolManager.getCurrentPlacement().getContext(), sort, null, (typeFilter.equals("0") ? null
					: typeFilter), paging.getCurrent(), paging.getSize());
		}
		else
		{
			questions = questionService.findQuestions(pool, sort, null, (typeFilter.equals("0") ? null : typeFilter), paging.getCurrent(), paging
					.getSize());
		}
		context.put("questions", questions);

		// the paging is now more accurate than our current destination - we may have asked for page 1, but have to deliver page 0 if we have nothing
		// get a new destination
		// [2]sort for /assessment, [3]aid |[4] pid |optional->| [5]our sort, [6]our page, [7] our type filter, [8] our pool filter
		String newDestination = "/" + params[1] + "/" + params[2] + "/" + params[3] + "/" + params[4] + "/" + sortCode + "/"
				+ paging.getCurrent().toString() + "-" + paging.getSize().toString() + "/" + typeFilter + "/" + poolFilter;

		// for the selected question type - pre-select the top of the list
		Value value = this.uiService.newValue();
		value.setValue(newDestination);
		context.put("selectedQuestionType", value);

		// for the selected pool - pre-select the top of the list
		value = this.uiService.newValue();
		value.setValue(newDestination);
		context.put("selectedPool", value);

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
		// [2]sort for /assessment, [3]aid |[4] pid |optional->| [5]our sort, [6]our page, [7] our type filter, [8] our pool filter
		if (params.length < 5 || params.length > 9) throw new IllegalArgumentException();

		String assessmentId = params[3];
		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		String partId = params[4];
		ManualPart part = (ManualPart) assessment.getParts().getPart(partId);
		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		Values values = this.uiService.newValues();
		context.put("questionids", values);

		// read form
		String destination = this.uiService.decode(req, context);

		for (String id : values.getValues())
		{
			Question question = this.questionService.getQuestion(id);
			if (question != null)
			{
				part.addQuestion(question);
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
		catch (AssessmentPolicyException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.policy)));
			return;
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
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
	 * @param mnemeService
	 *        the mnemeService to set
	 */
	public void setMnemeService(MnemeService mnemeService)
	{
		this.mnemeService = mnemeService;
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

	/**
	 * Figure out the sort from the code.
	 * 
	 * @param sortCode
	 *        The sort code.
	 * @return The sort.
	 */
	protected QuestionService.FindQuestionsSort findQuestionSortCode(String sortCode)
	{
		QuestionService.FindQuestionsSort sort = QuestionService.FindQuestionsSort.type_a;
		// 0 is question type
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.pool_title_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.pool_title_d;
		}
		// 1 is pool title
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.type_a;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.type_d;
		}
		// 2 is pool points
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.pool_points_a;
		}
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.pool_points_d;
		}
		// 3 is question description
		else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'A'))
		{
			sort = QuestionService.FindQuestionsSort.description_a;
		}
		else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'D'))
		{
			sort = QuestionService.FindQuestionsSort.description_d;
		}

		return sort;
	}
}
