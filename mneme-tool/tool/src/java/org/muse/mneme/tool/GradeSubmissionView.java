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
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /grade_submission view for the mneme tool.
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
		// [2]sid, [3]next/prev sort (optional- leave out to disable next/prev), optionally followed by a return destination
		if (params.length < 3) throw new IllegalArgumentException();

		Submission submission = this.submissionService.getSubmission(params[2]);
		if (submission == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// next and prev, based on the sort
		String sortCode = "userName_a";
		SubmissionService.FindAssessmentSubmissionsSort sort = null;
		int destinationStartsAt = 4;
		if (params.length > 3) sortCode = params[3];
		try
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.valueOf(sortCode);
		}
		catch (IllegalArgumentException e)
		{
			// no sort, so it must be part of destination
			destinationStartsAt = 3;
		}
		if (sort != null)
		{
			// one submission per user (i.e. 'official' only), except for survey, where we consider them all
			Boolean official = Boolean.valueOf(submission.getAssessment().getType() != AssessmentType.survey);

			String[] nextPrev = submissionService.findPrevNextSubmissionIds(submission, sort, official);
			if (nextPrev[0] != null) context.put("prev", nextPrev[0]);
			if (nextPrev[1] != null) context.put("next", nextPrev[1]);

			context.put("sort", sortCode);
		}

		String destination = null;
		if (params.length > destinationStartsAt)
		{
			destination = "/" + StringUtil.unsplit(params, destinationStartsAt, params.length - destinationStartsAt, "/");
		}

		// if not specified, go to the main grade_assessment page for this assessment
		else
		{
			destination = "/grade_assessment/0A/" + submission.getAssessment().getId();
		}
		context.put("return", destination);

		// needed by some of the delegates to show the score
		context.put("grading", Boolean.TRUE);

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
		// [2]sid, [3]next/prev sort (optional- leave out to disable next/prev), optionally followed by a return destination
		if (params.length < 3) throw new IllegalArgumentException();

		Submission submission = this.submissionService.getSubmission(params[2]);
		if (submission == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// read form
		String destination = this.uiService.decode(req, context);

		// save graded submission
		try
		{
			this.submissionService.evaluateSubmission(submission);
		}
		catch (AssessmentPermissionException e)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
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
