/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/GradeQuestionView.java $
 * $Id: GradeQuestionView.java 11997 2007-09-13 18:13:11Z maheshwarirashmi@foothill.edu $
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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.SubmissionService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Answer;
import org.sakaiproject.util.Web;
import org.muse.ambrosia.api.PopulatingSet;
import org.muse.ambrosia.api.PopulatingSet.Factory;
import org.muse.ambrosia.api.PopulatingSet.Id;
import org.sakaiproject.tool.api.ToolManager;

/**
 * The /grading view for the mneme tool.
 */
public class GradeQuestionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeQuestionView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** Submission Service */
	protected SubmissionService submissionService = null;

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
		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}
		
		context.put("gradeSortCode", params[2]);
		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		Question question = assessment.getParts().getQuestion(params[4]);
		context.put("question", question);

		// FindAssessmentSubmissionsSort.username_a
		SubmissionService.FindAssessmentSubmissionsSort sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
		context.put("sort_column", '0');
		context.put("sort_direction", 'A');

		if (params.length >= 6)
		{
			String sortCode = params[5];
			if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D')) sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
			if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A')) sort = SubmissionService.FindAssessmentSubmissionsSort.final_a;
			if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D')) sort = SubmissionService.FindAssessmentSubmissionsSort.final_d;
			context.put("sort_column", sortCode.charAt(0));
			context.put("sort_direction", sortCode.charAt(1));
		}

		List<Answer> answers = null;
		String pagingParameter = "1-30";

		if (params.length >= 7)
		{
			String viewAll = params[6];
			// get Answers
			if (viewAll.equals("all"))
			{
				answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.FALSE, null, null);
				context.put("official", "FALSE");
			}
			else
			{
				// get Answers
				answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.TRUE, null, null);
				context.put("official", "TRUE");
				pagingParameter = params[6];
				if (pagingParameter == null)
				{
					pagingParameter = "1-30";
				}
			}
		}
		else
		{
			// get Answers
			answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.TRUE, null, null);
			context.put("official", "TRUE");
		}
		context.put("answers", answers);
		Integer maxAnswers = 0;
		if (answers != null) maxAnswers = answers.size();

		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxAnswers);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);	
		
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
		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}
		
		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);

	 	PopulatingSet answers = null;
		final SubmissionService submissionService = this.submissionService;
		answers = uiService.newPopulatingSet(new Factory()
		{
			public Object get(String id)
			{
				Answer answer = submissionService.getAnswer(id);
				return answer;
			}
		}, new Id()
		{
			public String getId(Object o)
			{
				return ((Answer) o).getId();
			}
		});
		context.put("answers", answers);

		// read form
		String destination = this.uiService.decode(req, context);

		// save evaluation
		try
		{
			this.submissionService.evaluateAnswers(answers.getSet());
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}
		
		// open all submissions of a user
		if (destination.equals("VIEW_ALL"))
		{
			destination = "/grade_question/" + params[2] + "/" + params[3] + "/" + params[4];
			if (params.length < 6)
			{

				destination = destination + "/0A/all";
			}
			else destination = destination + "/" + params[5] + "/all";			
		}
		 
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * @param assessmentService
	 *        the assessmentService to set
	 */
	public void setAssessmentService(AssessmentService assessmentService)
	{
		this.assessmentService = assessmentService;
	}

	/**
	 * @param submissionService
	 *        the submissionService to set
	 */
	public void setSubmissionService(SubmissionService submissionService)
	{
		this.submissionService = submissionService;
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
