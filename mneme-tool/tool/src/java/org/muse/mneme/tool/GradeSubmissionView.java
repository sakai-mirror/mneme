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
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradeSubmissionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeSubmissionView.class);

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
		if (params.length != 4 && params.length != 5 && params.length != 6 && params.length != 7 && params.length != 8)
			throw new IllegalArgumentException();
		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		// submission id is in params array at index 7
		Submission submission = this.submissionService.getSubmission(params[7]);
		context.put("submission", submission);

		List<Submission> submissions = null;
		/*
		 * for previous and next - get submissions based on grade_assessment sort & view. sort is at index 4 and view is at index 6 in params
		 */
		if (params[6].equalsIgnoreCase("all"))
		{
			SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, params[4]);
			// get all Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.FALSE, null, null);

		}
		else if (params[6].equalsIgnoreCase("highest"))
		{
			SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, params[4]);
			// get official Assessment submissions
			submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.TRUE, null, null);
		}
		else
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		parsePreviousNext(context, submission, submissions);

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// destination path for grade submission
		String destinationPath = context.getDestination();
		destinationPath = destinationPath.substring(destinationPath.indexOf("/", 1) + 1, destinationPath.lastIndexOf("/"));
		context.put("destinationPath", destinationPath);
		uiService.render(ui, context);

	}

	/**
	 * parse for previous and next
	 * @param context
	 * @param submission
	 * @param submissions
	 */
	private void parsePreviousNext(Context context, Submission submission, List<Submission> submissions)
	{
		//prev and next
		if (submissions != null)
		{
			Submission submissionPresent = null;
			for (int i = 0; i < submissions.size(); i++)
			{
				submissionPresent = submissions.get(i);
				if (submissionPresent.getId().equals(submission.getId()))
				{
					if (i == 0 && (i == submissions.size() - 1))
					{
						context.put("prevSubmission", "");
						context.put("nextSubmission", "");
					}
					else
					{
						//current is first submission
						if (i == 0)
						{
							Submission submissionNext = null;
							// loop for next submission
							for (int j = i; j < submissions.size(); j++)
							{
								submissionNext = submissions.get(j);
								if (submissionNext.getIsComplete())
								{
									context.put("nextSubmission", submissionNext);
									break;
								}
							}
						}
						//current is last submission
						else if (i == submissions.size() - 1)
						{
							Submission submissionPrev = null;
							// loop for prev submission
							for (int j = i; j >= 0; j--)
							{
								submissionPrev = submissions.get(j);
								if (submissionPrev.getIsComplete())
								{
									context.put("prevSubmission", submissionPrev);
									break;
								}
							}
						}
						//current is somewhere between first and last
						else
						{
							Submission submissionNext = null;
							// loop for next submission
							for (int j = i; j < submissions.size(); j++)
							{
								submissionNext = submissions.get(j);
								if (submissionNext.getIsComplete())
								{
									context.put("nextSubmission", submissionNext);
									break;
								}
							}
							
							Submission submissionPrev = null;
							// loop for prev submission
							for (int j = i; j >= 0; j--)
							{
								submissionPrev = submissions.get(j);
								if (submissionPrev.getIsComplete())
								{
									context.put("prevSubmission", submissionPrev);
									break;
								}
							}
						}
					}
					break;
				}
			}
		}
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
		if (params.length != 4 && params.length != 5 && params.length != 6 && params.length != 7 && params.length != 8)
			throw new IllegalArgumentException();

		// submission id is in params array at index 7
		Submission submission = this.submissionService.getSubmission(params[7]);
		context.put("submission", submission);

		// read form
		String destination = this.uiService.decode(req, context);

		if (destination != null)
		{
			if (destination.startsWith("/grade_submission_save"))
			{
				try
				{
					// save submission
					if (submission.getAnswers() != null)
					{
						this.submissionService.evaluateSubmission(submission);
					}
				}
				catch (AssessmentPermissionException e)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				destination = destination.replace("grade_submission_save", "grade_assessment");
			}
			else if (destination.startsWith("NEXT") || destination.startsWith("PREV"))
			{
				try
				{
					// save submission
					if (submission.getAnswers() != null)
					{
						this.submissionService.evaluateSubmission(submission);
					}
				}
				catch (AssessmentPermissionException e)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				destination = context.getDestination();
			}

		}

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
}
