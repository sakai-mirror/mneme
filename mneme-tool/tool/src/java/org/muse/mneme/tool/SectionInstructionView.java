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
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.Submission;
import org.sakaiproject.util.Web;

/**
 * The /section_instructions view for the mneme tool.
 */
public class SectionInstructionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SectionInstructionView.class);

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
		// we need two parameters (submissionId, sectionId)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String sectionId = params[3];

		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// make sure by-question is valid for this assessment
		if (submission.getAssessment().getQuestionPresentation() != QuestionPresentation.BY_QUESTION)
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

		// collect the section
		AssessmentSection section = submission.getAssessment().getSection(sectionId);
		if (section == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		context.put("actionTitle", messages.getString("question-header-instructions"));
		context.put("submission", submission);
		context.put("section", section);
		// context.put("mode", mode);

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
		// we need two parameters (submissionId, sectionId)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String sectionId = params[3];

		// this post is from the timer, and completes the submission
		TocView.submissionCompletePost(req, res, context, submissionId, this.uiService, this.assessmentService);
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
