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
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.util.ViewImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.tool.AssessmentDeliveryTool.Errors;
import org.sakaiproject.util.Web;

/**
 * The /submitted view for the mneme tool.
 */
public class SubmittedView extends ViewImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SubmittedView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

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
		// we need a single parameter (sid)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];

		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// make sure this is a completed submission
		if ((submission.getIsComplete() == null) || (!submission.getIsComplete().booleanValue()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// if we have no authored message or URL, skip right to ...
		if ((submission.getAssessment().getSubmitMessage() == null) && (submission.getAssessment().getSubmitUrl() == null))
		{
			// if the assessment review is allowed, go to review, else to list
			String dest = "/list";
			if (submission.getAssessment().getFeedbackNow().booleanValue())
			{
				dest = "/review/" + submission.getId();
			}

			// redirect
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, dest)));
			return;
		}

		context.put("submission", submission);

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
		throw new IllegalArgumentException();
	}

	/**
	 * Set the assessment service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}
}
