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
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradeAssessmentView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeAssessmentView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** Submission Service */
	protected SubmissionService submissionService = null;

	/** Dependency: ToolManager */
	protected ToolManager toolManager = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

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
		if (params.length != 4 && params.length != 5 && params.length != 6) throw new IllegalArgumentException();

		String saveDestination = context.getDestination();
		saveDestination = saveDestination.replace(params[1], "grade_assessment_save");
		context.put("saveDestination", saveDestination);

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// grades sort parameter is in params array at index 2
		String gradesSortCode = params[2];
		context.put("gradesSortCode", gradesSortCode);

		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		// sort parameter - sort is in param array at index 4
		String sortCode = null;
		if (params.length > 4) sortCode = params[4];

		SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, sortCode);

		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		List submissions = null;
		if (params.length == 6 && params[5].equalsIgnoreCase("all"))
		{
			// get all Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.FALSE, null, null);
			context.put("official", "FALSE");
		}
		else
		{
			// get official Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.TRUE, null, null);
			context.put("official", "TRUE");
		}
		context.put("submissions", submissions);

		Integer maxSubmissions = 0;
		if (submissions != null) maxSubmissions = submissions.size();

		String pagingParameter = "1-30";
		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxSubmissions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// for Adjust every student's test submission by
		Value submissionAdjust = this.uiService.newValue();
		context.put("submissionAdjust", submissionAdjust);

		// for "Adjust every student's test submission by" comments
		Value submissionAdjustComments = this.uiService.newValue();
		context.put("submissionAdjustComments", submissionAdjustComments);

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
		if (params.length != 4 && params.length != 5 && params.length != 6) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext(), sessionManager.getCurrentSessionUserId()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// for Adjust every student's test submission by
		Value submissionAdjustValue = this.uiService.newValue();
		context.put("submissionAdjust", submissionAdjustValue);

		// for "Adjust every student's test submission by" comments
		Value submissionAdjustCommentsValue = this.uiService.newValue();
		context.put("submissionAdjustComments", submissionAdjustCommentsValue);

		// setup the model: the assessment
		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);

		List<Submission> submissions = null;

		// sort parameter - sort is in param array at index 4
		String sortCode = null;
		if (params.length > 4) sortCode = params[4];

		SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, sortCode);

		if (params.length == 6 && params[5].equalsIgnoreCase("all"))
		{
			// get all Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.FALSE, null, null);
		}
		else
		{
			// get official Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.TRUE, null, null);
		}

		context.put("submissions", submissions);

		// read form
		String destination = this.uiService.decode(req, context);

		String submissionAdjustScore = submissionAdjustValue.getValue();
		String submissionAdjustComments = submissionAdjustCommentsValue.getValue();

		if (destination != null)
		{
			if (destination.startsWith("/grade_assessment_save"))
			{
				// save adjusted score for the assessment - global adjustment
				if (submissionAdjustScore != null && submissionAdjustScore.trim().length() > 0)
				{
					try
					{
						Float score = new Float(submissionAdjustScore);
						if (assessment != null)
							this.submissionService.evaluateSubmissions(assessment, submissionAdjustComments, score, Boolean.FALSE);
						else
						{
							// redirect to error
							res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
							return;
						}
					}
					catch (NumberFormatException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
						return;
					}
					catch (AssessmentPermissionException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
						return;
					}
				}

				// save adjusted score for each student's test submission
				if (submissions != null && submissions.size() > 0)
				{
					for (Submission submission : submissions)
					{
						if (submission.getAssessment().equals(assessment))
						{
							try
							{
								// save submission //to adjust to zero after first adjustment user should provide zero
								if (submission.getEvaluation().getScore() != null)
									this.submissionService.evaluateSubmission(submission);
							}
							catch (AssessmentPermissionException e)
							{
								res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
								return;
							}
						}
					}
				}

				destination = destination.replace("grade_assessment_save", "grade_assessment");
			}
		}

		// destination = "/grades/" + params[2];
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * get the sort based on sort code
	 * 
	 * @param context
	 * @param sortCode
	 *        sort code
	 * @return SubmissionService.FindAssessmentSubmissionsSort
	 */
	private SubmissionService.FindAssessmentSubmissionsSort getSort(Context context, String sortCode)
	{
		// default sort is user name ascending
		SubmissionService.FindAssessmentSubmissionsSort sort;
		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));

				// 0 is title
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
				else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.status_a;
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.status_d;
				else
				{
					throw new IllegalArgumentException();
				}
			}
			else
			{
				throw new IllegalArgumentException();
			}
		}
		else
		{
			// default sort: user name ascending
			sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;

			context.put("sort_column", '0');
			context.put("sort_direction", 'A');
		}

		return sort;
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

	/**
	 * @param sessionManager
	 *        the sessionManager to set
	 */
	public void setSessionManager(SessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
	}
}
