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
import org.muse.ambrosia.api.UiService;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.tool.AssessmentDeliveryTool.Destinations;
import org.muse.mneme.tool.AssessmentDeliveryTool.Errors;
import org.sakaiproject.util.Web;

/**
 * The /question view for the mneme tool.
 */
public class TocView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TocView.class);

	/** Assessment service. */
	protected MnemeService assessmentService = null;

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
		// we need one parameter (sid)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];

		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submission, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// linear is not allowed in here
		if (!submission.getAssessment().getRandomAccess().booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.linear + "/" + submissionId)));
			return;
		}

		// collect information: the selected assessment (id the request)
		context.put("submission", submission);

		context.put("finalReview", Boolean.FALSE);

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
		// we need two parameters (sid/quesiton selector)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];

		// this post is from the timer, or the "submit" button, and completes the submission
		submissionCompletePost(req, res, context, submissionId, this.uiService, this.assessmentService);
	}

	/**
	 * Set the assessment service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setAssessmentService(MnemeService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Handle the many cases of a post that completes the submission
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        The UiContext.
	 * @param submissionId
	 *        the selected submission id.
	 */
	protected static void submissionCompletePost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId,
			UiService uiService, MnemeService assessmentService) throws IOException
	{
		// if (!context.getPostExpected())
		// {
		// // redirect to error
		// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
		// return;
		// }

		// read form
		String destination = uiService.decode(req, context);

		if (destination.equals("SUBMIT"))
		{
			Submission submission = assessmentService.idSubmission(submissionId);

			// if linear, or the submission is all answered, we can go to submitted
			if ((!submission.getAssessment().getRandomAccess().booleanValue()) || (submission.getIsAnswered(null).booleanValue()))
			{
				destination = "/" + Destinations.submitted + "/" + submissionId;
				// we will complete below
			}

			// if not linear, and there are unanswered parts, send to final review
			else
			{
				destination = "/" + Destinations.final_review + "/" + submissionId;

				// we do not want to complete - redirect now
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
		}

		// we need to be headed to submitted...
		if (!destination.startsWith("/submitted"))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
			return;
		}

		Submission submission = assessmentService.idSubmission(submissionId);
		try
		{
			assessmentService.completeSubmission(submission);

			// if no exception, it worked! redirect
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
			return;
		}
		catch (AssessmentClosedException e)
		{
		}
		catch (SubmissionCompletedException e)
		{
		}
		catch (AssessmentPermissionException e)
		{
		}

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
	}
}
