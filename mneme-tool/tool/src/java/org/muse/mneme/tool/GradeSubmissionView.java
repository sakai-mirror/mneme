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
		if (params.length != 4 && params.length != 5 && params.length != 6 && params.length != 7) throw new IllegalArgumentException();
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

		// submission id is in params array at index 6
		Submission submission = this.submissionService.getSubmission(params[6]);
		context.put("submission", submission);

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
		if (params.length != 4 && params.length != 5 && params.length != 6 && params.length != 7) throw new IllegalArgumentException();

		// submission id is in params array at index 6
		Submission submission = this.submissionService.getSubmission(params[6]);
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
