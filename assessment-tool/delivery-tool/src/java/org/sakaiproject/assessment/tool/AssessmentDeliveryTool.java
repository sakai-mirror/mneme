/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.tool;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.AssessmentClosedException;
import org.sakaiproject.assessment.api.AssessmentCompletedException;
import org.sakaiproject.assessment.api.AssessmentPermissionException;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.AssessmentService;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionCompletedException;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.sludge.api.Context;
import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.UiService;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.Web;

/**
 * Assessment Delivery Tool.
 */
public class AssessmentDeliveryTool extends HttpServlet
{
	/** Our tool destinations. */
	enum Destinations
	{
		enter, error, exit, list, question, review, toc
	}

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AssessmentDeliveryTool.class);

	/** Localized messages. */
	protected static ResourceLoader messages = new ResourceLoader("assessment-delivery-tool");

	/** Our self-injected assessment service reference. */
	protected AssessmentService assessmentService = null;

	/** Our self-injected session manager reference. */
	protected SessionManager sessionManager = null;;

	/** Our self-injected time service reference. */
	protected TimeService timeService = null;

	/** Our self-injected tool manager reference. */
	protected ToolManager toolManager = null;

	/** Our self-injected ui service reference. */
	protected UiService ui = null;

	/** The enter interface. */
	protected Controller uiEnter = null;

	/** The exit interface. */
	protected Controller uiExit = null;

	/** The list interface. */
	protected Controller uiList = null;

	/** The question interface. */
	protected Controller uiQuestion = null;

	/** The table of contents interface. */
	protected Controller uiToc = null;

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.info("destroy()");

		super.destroy();
	}

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "Sakai Assessment Delivery";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		// self-inject
		sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
		toolManager = (ToolManager) ComponentManager.get(ToolManager.class);
		assessmentService = (AssessmentService) ComponentManager.get(AssessmentService.class);
		timeService = (TimeService) ComponentManager.get(TimeService.class);
		ui = (UiService) ComponentManager.get(UiService.class);

		// make the uis
		uiList = DeliveryControllers.constructList(ui);
		uiEnter = DeliveryControllers.constructEnter(ui);
		uiQuestion = DeliveryControllers.constructQuestion(ui);
		uiExit = DeliveryControllers.constructExit(ui);
		uiToc = DeliveryControllers.constructToc(ui);

		M_log.info("init()");
	}

	/**
	 * Respond to requests.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @throws ServletException.
	 * @throws IOException.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		Context context = ui.prepareGet(req, res, messages, Destinations.list.toString());

		// split up the tool destination: 0 parts means must "/", otherwise parts[0] = "", parts[1] = the first part, etc.
		String[] parts = context.getDestination().split("/");

		// which destination?
		Destinations destination = Destinations.error;
		try
		{
			destination = Destinations.valueOf(parts[1]);
		}
		catch (IllegalArgumentException e)
		{
			// not a know destination
		}

		switch (destination)
		{
			case list:
			{
				listGet(req, res, context);
				break;
			}
			case review:
			{
				errorGet(req, res, context);
				break;
			}
			case enter:
			{
				// we need a single parameter (aid)
				if (parts.length != 3)
				{
					errorGet(req, res, context);
				}
				else
				{
					enterGet(req, res, parts[2], context);
				}
				break;
			}
			case toc:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					errorGet(req, res, context);
				}
				else
				{
					tocGet(req, res, parts[2], context);
				}
				break;
			}
			case question:
			{
				// we need two parameters (sid/qid)
				if (parts.length != 4)
				{
					errorGet(req, res, context);
				}
				else
				{
					questionGet(req, res, parts[2], parts[3], context);
				}
				break;
			}
			case exit:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					errorGet(req, res, context);
				}
				else
				{
					exitGet(req, res, parts[2], context);
				}
				break;
			}
			case error:
			{
				errorGet(req, res, context);
				break;
			}
		}
	}

	/**
	 * Respond to requests.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @throws ServletException.
	 * @throws IOException.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		Context context = ui.preparePost(req, res, messages, Destinations.list.toString());

		// get and split up the tool destination: 0 parts means must "/", otherwise parts[0] = "", parts[1] = the first part, etc.
		String path = context.getDestination();
		String[] parts = context.getDestination().split("/");

		// which destination?
		Destinations destination = Destinations.error;
		try
		{
			destination = Destinations.valueOf(parts[1]);
		}
		catch (IllegalArgumentException e)
		{
			// not a know destination
		}

		switch (destination)
		{
			case enter:
			{
				// we need a single parameter (aid)
				if (parts.length != 3)
				{
					redirectError(req, res);
				}
				else
				{
					enterPost(req, res, context, parts[2]);
				}
				break;
			}
			case question:
			{
				// we need two parameters (sid/qid)
				if (parts.length != 4)
				{
					redirectError(req, res);
				}
				else
				{
					questionPost(req, res, context, parts[2], parts[3]);
				}
				break;
			}
			case toc:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					redirectError(req, res);
				}
				else
				{
					tocPost(req, res, context, parts[2]);
				}
				break;
			}
			default:
			{
				// redirect to error
				redirectError(req, res);
				break;
			}
		}
	}

	/**
	 * Get the UI for the enter destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param assessmentId
	 *        The selected assessment id.
	 * @param context
	 *        UiContext.
	 */
	protected void enterGet(HttpServletRequest req, HttpServletResponse res, String assessmentId, Context context)
	{
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment != null)
		{
			// security check (submissions count / allowed check)
			if (assessmentService.allowSubmit(assessmentId, null))
			{
				// collect information: the selected assessment (id the request)
				context.put("assessment", assessmentService.idAssessment(assessmentId));

				// for this assessment, we need to know how many completed submission the current use has already made
				Integer count = assessmentService.countRemainingSubmissions(assessmentId, null);
				context.put("remainingSubmissions", count);

				// render
				ui.render(uiEnter, context);
				return;
			}
		}

		errorGet(req, res, context);
	}

	/**
	 * Read the input for the enter destination, process, and redirect to the next destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        The UiContext.
	 * @param assessmentId
	 *        the selected assessment id.
	 */
	protected void enterPost(HttpServletRequest req, HttpServletResponse res, Context context, String assessmentId)
			throws IOException
	{
		// TODO: check expected

		// read form: for now, nothing to read
		String destination = ui.decode(req, context);

		// process: enter the assessment for this user, find the submission id and first question id from the first part
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		try
		{
			Submission submission = assessmentService.enterSubmission(assessment, null);
			if (submission != null)
			{
				if (assessment != null)
				{
					AssessmentSection part = assessment.getFirstSection();
					if (part != null)
					{
						AssessmentQuestion question = part.getFirstQuestion();
						if (question != null)
						{
							String questionId = question.getId();

							// next destination: first question of submission
							destination = "/" + Destinations.question + "/" + submission.getId() + "/" + questionId;

							// redirect
							res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
							return;
						}
					}
				}
			}
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

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
	}

	/**
	 * Get the UI for the error destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void errorGet(HttpServletRequest req, HttpServletResponse res, Context context)
	{
		throw new RuntimeException("tool error");
	}

	/**
	 * Get the UI for the exit destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submissionId
	 *        The completed submission id.
	 * @param context
	 *        UiContext.
	 */
	protected void exitGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
	{
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			// make sure this is a completed submission
			if ((submission.getIsComplete() != null) && (submission.getIsComplete().booleanValue()))
			{
				context.put("submission", submission);

				// for this assessment, we need to know how many completed submission the current use has already made
				Integer count = assessmentService.countRemainingSubmissions(submission.getAssessment().getId(), null);
				context.put("remainingSubmissions", count);

				// render
				ui.render(uiExit, context);
				return;
			}
		}

		errorGet(req, res, context);
	}

	/**
	 * Get the UI for the list destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void listGet(HttpServletRequest req, HttpServletResponse res, Context context)
	{
		// collect information: assessments
		Collection assessments = assessmentService.getAvailableAssessments(toolManager.getCurrentPlacement().getContext(), null);
		context.put("assessments", assessments);

		// submissions
		Collection submissions = assessmentService.getOfficialSubmissions(toolManager.getCurrentPlacement().getContext(), null);
		context.put("submissions", submissions);

		// render
		ui.render(uiList, context);
	}

	/**
	 * Get the UI for the quesiton destination
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionId
	 *        The current question id.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void questionGet(HttpServletRequest req, HttpServletResponse res, String submissionId, String questionId,
			Context context)
	{
		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			// TODO: security check (user matches submission user)
			// TODO: check that the assessment is open
			context.put("submission", submission);

			// collect the question
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question != null)
			{
				context.put("question", question);

				// find the answer (or have one created) for this submission / question
				SubmissionAnswer answer = submission.getAnswer(question);
				if (answer != null)
				{
					context.put("answer", answer);

					// render
					ui.render(uiQuestion, context);
					return;
				}
			}
		}

		errorGet(req, res, context);
	}

	/**
	 * Read the input for the enter destination, process, and redirect to the next destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        The UiContext
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionId
	 *        The current question id.
	 * @param expected
	 *        true if this post was expected, false if not.
	 */
	protected void questionPost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId,
			String questionId) throws IOException
	{
		// TODO: deal with unexpected

		// setup receiving context

		// find the answer (or have one created) for this submission / question
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question != null)
			{
				SubmissionAnswer answer = submission.getAnswer(question);
				if (answer != null)
				{
					context.put("answer", answer);

					// read form
					String destination = ui.decode(req, context);

					// if we are going to exit, we must complete the submission
					boolean complete = false;
					if (destination.startsWith("/exit"))
					{
						complete = true;
					}

					// submit the user's answer
					try
					{
						assessmentService.submitAnswer(answer, complete);

						// redirect to the next destination
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
				}
			}
		}

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
	}

	/**
	 * Send a redirect to the error destination.
	 * 
	 * @param req
	 * @param res
	 * @throws IOException
	 */
	protected void redirectError(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		String error = Web.returnUrl(req, "/" + Destinations.error);
		res.sendRedirect(res.encodeRedirectURL(error));
	}

	/**
	 * Get the UI for the toc destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submissionId
	 *        The selected submission id.
	 * @param context
	 *        UiContext.
	 */
	protected void tocGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
	{
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			// TODO: security check (user matches submission user) user may submit
			// TODO: check that the assessment is open, submission is open
			if (/* assessmentService.allowSubmit(assessmentId, null) */true)
			{
				// collect information: the selected assessment (id the request)
				context.put("submission", submission);

				// render
				ui.render(uiToc, context);
				return;
			}
		}

		errorGet(req, res, context);
	}

	/**
	 * Read the input for the toc destination, process, and redirect to the next destination.
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
	protected void tocPost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId)
			throws IOException
	{
		// TODO: check expected

		// the post is for "submit for grading".

		// read form: for now, nothing to read
		String destination = ui.decode(req, context);

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
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
	}
}
