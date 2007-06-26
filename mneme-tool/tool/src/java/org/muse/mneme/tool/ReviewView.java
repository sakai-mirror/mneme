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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.util.Web;

/**
 * The /review view for the mneme tool.
 */
public class ReviewView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ReviewView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** Event tracking service. */
	protected EventTrackingService eventTrackingService = null;

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
		// we need two parameters (sid/quesiton selector)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];

		// yes feedback, and we are in review
		context.put("review", Boolean.TRUE);
		context.put("actionTitle", messages.getString("question-header-review"));

		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowReviewSubmission(submission, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// collect all the answers for review
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();
		for (AssessmentSection section : submission.getAssessment().getSections())
		{
			for (AssessmentQuestion question : section.getQuestions())
			{
				SubmissionAnswer answer = submission.getAnswer(question);
				answers.add(answer);
			}
		}

		context.put("answers", answers);

		// in this special case, since there's no real action in the service to do this, we need to generate an event
		eventTrackingService.post(eventTrackingService.newEvent(AssessmentService.SUBMISSION_REVIEW, submission.getReference(), false));

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

	/**
	 * Set the event tracking service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		this.eventTrackingService = service;
	}
}
