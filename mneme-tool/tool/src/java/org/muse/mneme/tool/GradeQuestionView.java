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
		// if (params.length > 4) throw new IllegalArgumentException();

		context.put("gradeSortCode", params[2]);
		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		// get Questions
		List questions = assessment.getParts().getQuestionsAsAuthored();
		context.put("questions", questions);

		Question question = null;
		if (questions != null)
		{
			if (params.length <= 4)
			{
				// get First Question
				question = (Question) questions.get(0);
				String destination = "/grade_question/" + params[2] + "/" + params[3] + "/" + question.getId() +"/0A";
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
			else
			{
				question = assessment.getParts().getQuestion(params[4]);
			}
			context.put("question", question);

			// FindAssessmentSubmissionsSort.username_a
			SubmissionService.FindAssessmentSubmissionsSort sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
			context.put("sort_column", '0');
			context.put("sort_direction", 'A');

			if (params.length >= 6)
			{
				String sortCode = params[5];
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D')) sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));
			}

			List answers = null;
			if (params.length >= 7)
			{
				String viewAll = params[6];
				// get Answers for the first question
				answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.FALSE, null, null);
				context.put("official", "FALSE");
			}
			else
			{
				// get Answers for the first question
				answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.TRUE, null, null);
				context.put("official", "TRUE");
			}
			
			context.put("answers", answers);
			
			Integer maxAnswers = 0;
			if (answers != null) maxAnswers = answers.size();

			String pagingParameter = "1-30";
			// paging
			Paging paging = uiService.newPaging();
			paging.setMaxItems(maxAnswers);
			paging.setCurrentAndSize(pagingParameter);
			context.put("paging", paging);
		}
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

		// read form
		String destination = this.uiService.decode(req, context);
	
		if (destination.startsWith("/grade_question"))
		{
			// get Assessment - assessment id is in params at index 3
			Assessment assessment = this.assessmentService.getAssessment(params[3]);
			Question question = assessment.getParts().getQuestion(params[4]);

			// FindAssessmentSubmissionsSort.username_a
			SubmissionService.FindAssessmentSubmissionsSort sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;

			if (params.length >= 6)
			{
				String sortCode = params[5];
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D')) sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
			}
			
			List answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.TRUE, null, null);
			context.put("answers", answers);
			context.put("official", "TRUE");
			
			try
			{
				this.submissionService.evaluateAnswers(answers);
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
				return;
			}
		}
		
		if(destination.equals("VIEW_ALL"))
		{		
			// get Assessment - assessment id is in params at index 3
			Assessment assessment = this.assessmentService.getAssessment(params[3]);
			Question question = assessment.getParts().getQuestion(params[4]);
			destination = "/grade_question/" + params[2] + "/" + params[3] + "/" + params[4];
			
			// FindAssessmentSubmissionsSort.username_a
			SubmissionService.FindAssessmentSubmissionsSort sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
			if (params.length == 6)
			{
				String sortCode = params[5];
				destination = destination + "/" + params[5];
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D')) sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
			}
			List answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, Boolean.FALSE, null, null);
			context.put("answers", answers);
			context.put("official", "FALSE");
			destination = destination + "/all";
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
}
