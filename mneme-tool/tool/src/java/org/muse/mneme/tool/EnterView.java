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
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.Submission;
import org.muse.mneme.tool.AssessmentDeliveryTool.Destinations;
import org.muse.mneme.tool.AssessmentDeliveryTool.Errors;
import org.sakaiproject.util.Web;

/**
 * The /enter view for the mneme tool.
 */
public class EnterView extends ViewImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(EnterView.class);

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
		// we need a single parameter (aid)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[2];

		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check for closed
		if (assessment.getIsClosed().booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.closed)));
			return;
		}

		// security check (submissions count / allowed check)
		if (!assessmentService.allowSubmit(assessment, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// see if we can skip the enter view and go right to the quesiton
		// if ((assessment.getPassword() == null) && (assessment.getDescription() == null) && (assessment.getAttachments().isEmpty())
		// && (assessment.getRandomAccess().booleanValue()) && (assessment.getTimeLimit() == null))
		// {
		// enterSubmission(req, res, assessment);
		// return;
		// }

		// collect information: the selected assessment (id the request)
		context.put("assessment", assessment);

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
		// we need a single parameter (aid)
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[2];

		// // check expected
		// if (!context.getPostExpected())
		// {
		// // redirect to error
		// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
		// return;
		// }

		// for the password
		Value value = this.uiService.newValue();
		context.put("password", value);

		// read form
		String destination = this.uiService.decode(req, context);

		// process: enter the assessment for this user, find the submission id and starting question
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check password
		if ((assessment.getPassword() != null) && (!assessment.checkPassword(value.getValue()).booleanValue()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.password)));
			return;
		}

		enterSubmission(req, res, assessment);
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
	 * Send the user into the submission.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param assessment
	 *        The assessment to take.
	 * @throws IOException
	 */
	protected void enterSubmission(HttpServletRequest req, HttpServletResponse res, Assessment assessment) throws IOException
	{
		Submission submission = null;
		try
		{
			submission = assessmentService.enterSubmission(assessment, null);
		}
		catch (AssessmentClosedException e)
		{
		}
		catch (AssessmentCompletedException e)
		{
		}
		catch (AssessmentPermissionException e)
		{
		}

		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		redirectToQuestion(req, res, submission, false, true);
	}

	/**
	 * Redirect to the appropriate question screen for this submission
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submission
	 *        The submission.
	 * @param toc
	 *        if true, send to TOC if possible (not possible for linear).
	 * @param instructions
	 *        if true, send to section instructions for first question.
	 */
	protected void redirectToQuestion(HttpServletRequest req, HttpServletResponse res, Submission submission, boolean toc, boolean instructions)
			throws IOException
	{
		String destination = null;
		Assessment assessment = submission.getAssessment();

		// if we are random access, and allowed, send to TOC
		if (toc && assessment.getRandomAccess().booleanValue())
		{
			destination = "/" + Destinations.toc + "/" + submission.getId();
		}

		else
		{
			// find the first incomplete question
			AssessmentQuestion question = submission.getFirstIncompleteQuestion();

			// if we don't have one, we will go to the toc (or final_review for linear)
			if (question == null)
			{
				if (!assessment.getRandomAccess().booleanValue())
				{
					destination = "/" + Destinations.final_review + "/" + submission.getId();
				}
				else
				{
					destination = "/" + Destinations.toc + "/" + submission.getId();
				}
			}

			else
			{
				// send to the section instructions if it's a first question and by-question
				if (instructions && (question.getSectionOrdering().getIsFirst().booleanValue())
						&& (!question.getSection().getIsMerged().booleanValue())
						&& (assessment.getQuestionPresentation() == QuestionPresentation.BY_QUESTION))
				{
					// to instructions
					destination = "/" + Destinations.section_instructions + "/" + submission.getId() + "/" + question.getSection().getId();
				}

				// or to the question
				else
				{
					if (assessment.getQuestionPresentation() == QuestionPresentation.BY_QUESTION)
					{
						destination = "/" + Destinations.question + "/" + submission.getId() + "/q" + question.getId();
					}
					else if (assessment.getQuestionPresentation() == QuestionPresentation.BY_SECTION)
					{
						destination = "/" + Destinations.question + "/" + submission.getId() + "/s" + question.getSection().getId();

						// include the question target if not the first quesiton in the section
						if (!question.getSectionOrdering().getIsFirst().booleanValue())
						{
							destination = destination + "#" + question.getId();
						}
					}
					else
					{
						destination = "/" + Destinations.question + "/" + submission.getId() + "/a";

						// include the question target if not the first quesiton in the assessment
						if (!question.getAssessmentOrdering().getIsFirst().booleanValue())
						{
							destination = destination + "#" + question.getId();
						}
					}
				}
			}
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
		return;
	}
}
