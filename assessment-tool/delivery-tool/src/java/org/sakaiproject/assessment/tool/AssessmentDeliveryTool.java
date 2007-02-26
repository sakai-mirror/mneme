/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2007 The Sakai Foundation.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.sakaiproject.assessment.api.AttachmentService;
import org.sakaiproject.assessment.api.QuestionPresentation;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionCompletedException;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.sludge.api.Context;
import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.UiService;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * Assessment Delivery Tool.
 */
public class AssessmentDeliveryTool extends HttpServlet
{
	/** Our tool destinations. */
	enum Destinations
	{
		enter, error, final_review, list, question, remove, review, submitted, toc
	}

	/** Our errors. */
	enum Errors
	{
		invalid, invalidpost, linear, unauthorized, unexpected, unknown, upload
	}

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AssessmentDeliveryTool.class);

	/** Localized messages. */
	protected static ResourceLoader messages = new ResourceLoader("assessment-delivery-tool");

	/** Our self-injected assessment service reference. */
	protected AssessmentService assessmentService = null;

	/** Our self-injected attachment service reference. */
	protected AttachmentService attachmentService = null;;

	/** Our self-injected entity manager reference. */
	protected EntityManager entityManager = null;

	/** Our self-injected session manager reference. */
	protected SessionManager sessionManager = null;

	/** Our self-injected time service reference. */
	protected TimeService timeService = null;

	/** Our self-injected tool manager reference. */
	protected ToolManager toolManager = null;

	/** Our self-injected ui service reference. */
	protected UiService ui = null;

	/** The enter interface. */
	protected Controller uiEnter = null;

	/** The error interface. */
	protected Controller uiError = null;

	/** The list interface. */
	protected Controller uiList = null;

	/** The question interface. */
	protected Controller uiQuestion = null;

	/** The remove interface. */
	protected Controller uiRemove = null;

	/** The submitted interface. */
	protected Controller uiSubmitted = null;

	/** The table of contents interface. */
	protected Controller uiToc = null;

	/** set of static resource paths. */
	protected Set<String> resourcePaths = new HashSet<String>();

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
		this.sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
		this.toolManager = (ToolManager) ComponentManager.get(ToolManager.class);
		this.assessmentService = (AssessmentService) ComponentManager.get(AssessmentService.class);
		this.attachmentService = (AttachmentService) ComponentManager.get(AttachmentService.class);
		this.timeService = (TimeService) ComponentManager.get(TimeService.class);
		this.ui = (UiService) ComponentManager.get(UiService.class);
		this.entityManager = (EntityManager) ComponentManager.get(EntityManager.class);

		// make the uis
		this.uiList = DeliveryControllers.constructList(ui);
		this.uiEnter = DeliveryControllers.constructEnter(ui);
		this.uiQuestion = DeliveryControllers.constructQuestion(ui);
		this.uiSubmitted = DeliveryControllers.constructSubmitted(ui);
		this.uiToc = DeliveryControllers.constructToc(ui);
		this.uiRemove = DeliveryControllers.constructRemove(ui);
		this.uiError = DeliveryControllers.constructError(ui);

		// setup the resource paths
		this.resourcePaths.add("icons");

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
		// handle static resource requests
		if (ui.dispatchResource(req, res, getServletContext(), this.resourcePaths)) return;

		// handle pathless requests
		if (ui.redirectToCurrentDestination(req, res, Destinations.list.toString())) return;

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
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		switch (destination)
		{
			case list:
			{
				// optional sort parameter
				String sort = null;
				if (parts.length == 3)
				{
					sort = parts[2];
				}
				listGet(req, res, sort, context);
				break;
			}
			case review:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					reviewGet(req, res, parts[2], context);
				}
				break;
			}
			case enter:
			{
				// we need a single parameter (aid)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
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
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					tocGet(req, res, parts[2], context);
				}
				break;
			}
			case question:
			{
				// we need two parameters (sid/quesiton selector) and an optional third (/feedback)
				if ((parts.length != 4) && (parts.length != 5))
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					questionGet(req, res, parts[2], parts[3], (parts.length == 5) ? parts[4] : null, context);
				}
				break;
			}
			case final_review:
			{
				// we need one parameter (sid)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					finalReviewGet(req, res, parts[2], context);
				}
				break;
			}
			case submitted:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					submittedGet(req, res, parts[2], context);
				}
				break;
			}
			case remove:
			{
				// we need three parameters (sid/qid/ref), but ref is really all the rest
				if (parts.length < 5)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
				else
				{
					removeGet(req, res, parts[2], parts[3], "/" + StringUtil.unsplit(parts, 4, parts.length - 4, "/"), context);
				}
				break;
			}
			case error:
			{
				// we would like a single parameter (error code), and perhaps one more
				String error = null;
				String param = null;
				if (parts.length >= 3)
				{
					error = parts[2];

					if (parts.length >= 4)
					{
						param = parts[3];
					}
				}

				errorGet(req, res, error, param, context);
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
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
			return;
		}

		switch (destination)
		{
			case enter:
			{
				// we need a single parameter (aid)
				if (parts.length != 3)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
					return;
				}
				else
				{
					enterPost(req, res, context, parts[2]);
				}
				break;
			}
			case question:
			{
				// we need two parameters (sid/qid), and tolerate but ignore a third (/feedback)
				if ((parts.length != 4) && (parts.length != 5))
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
					return;
				}
				else
				{
					questionPost(req, res, context, parts[2], parts[3]);
				}
				break;
			}
			case remove:
			{
				// we need three parameters (sid/qid/ref), but ref is really all the rest
				if (parts.length < 5)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
					return;
				}
				else
				{
					removePost(req, res, parts[2], parts[3], "/" + StringUtil.unsplit(parts, 4, parts.length - 4, "/"), context);
				}
				break;
			}
			case final_review:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
					return;
				}
				else
				{
					finalReviewPost(req, res, context, parts[2]);
				}
				break;
			}
			case toc:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
					return;
				}
				else
				{
					tocPost(req, res, context, parts[2]);
				}
				break;
			}
			default:
			{
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalidpost)));
				return;
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
			throws IOException
	{
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment != null)
		{
			// security check (submissions count / allowed check)
			if (assessmentService.allowSubmit(assessmentId, null).booleanValue())
			{
				// collect information: the selected assessment (id the request)
				context.put("assessment", assessmentService.idAssessment(assessmentId));

				// for this assessment, we need to know how many completed submissions the current use has already made
				Integer count = assessmentService.countRemainingSubmissions(assessmentId, null);
				context.put("remainingSubmissions", count);

				// render
				ui.render(uiEnter, context);
				return;
			}

			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
		return;
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
		// check expected
		if (!context.getPostExpected())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}

		// read form: for now, nothing to read
		String destination = ui.decode(req, context);

		// process: enter the assessment for this user, find the submission id and starting question
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

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

		// question, section or all?
		QuestionPresentation presentation = assessment.getQuestionPresentation();

		// for by section
		if ((presentation != null) && (presentation == QuestionPresentation.BY_SECTION))
		{
			AssessmentSection section = assessment.getFirstSection();
			if (section != null)
			{
				destination = "/" + Destinations.question + "/" + submission.getId() + "/s" + section.getId();

				// redirect
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
		}

		// for all
		else if ((presentation != null) && (presentation == QuestionPresentation.BY_ASSESSMENT))
		{
			destination = "/" + Destinations.question + "/" + submission.getId() + "/a";

			// redirect
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
			return;
		}

		// otherwise by quesion
		String questionId = null;

		// for linear assessments, start at the first incomplete question
		if (!assessment.getRandomAccess().booleanValue())
		{
			AssessmentQuestion question = submission.getFirstIncompleteQuestion();
			if (question != null)
			{
				questionId = question.getId();
			}

			// otherwise send the user to the toc/final review view
			// Note: this is unlikely, since there's no way to mark the last question as complete without a "finish" -ggolden
			else
			{
				destination = "/" + Destinations.final_review + "/" + submission.getId();
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
		}

		// for random access, start at the first question of the first part
		else
		{
			AssessmentSection part = assessment.getFirstSection();
			if (part != null)
			{
				AssessmentQuestion question = part.getFirstQuestion();
				if (question != null)
				{
					questionId = question.getId();
				}
			}
		}

		if (questionId != null)
		{
			// next destination: first question of submission
			destination = "/" + Destinations.question + "/" + submission.getId() + "/q" + questionId;

			// redirect
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
			return;
		}

		// we are here because there are no questions!
		destination = "/" + Destinations.final_review + "/" + submission.getId();
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Get the UI for the error destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param errorCode
	 *        The error code string.
	 * @param param
	 *        The extra parameter.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void errorGet(HttpServletRequest req, HttpServletResponse res, String errorCode, String param, Context context)
	{
		// which error?
		Errors error = Errors.unknown;
		if (errorCode != null)
		{
			try
			{
				error = Errors.valueOf(errorCode);
			}
			catch (IllegalArgumentException e)
			{
			}
		}

		switch (error)
		{
			case invalid:
			{
				context.put("invalidUrl", context.getPreviousDestination());
				break;
			}

			case invalidpost:
			{
				context.put("invalidPost", Boolean.TRUE);
				break;
			}

			case unauthorized:
			{
				context.put("unauthorized", Boolean.TRUE);
				break;
			}

			case unexpected:
			{
				context.put("unexpected", Boolean.TRUE);
				break;
			}

			case linear:
			{
				context.put("unauthorized", Boolean.TRUE);

				if (param != null)
				{
					// treat the param as a submission id
					Submission s = assessmentService.idSubmission(param);
					if (s != null)
					{
						AssessmentQuestion question = s.getFirstIncompleteQuestion();
						if (question != null)
						{
							// next destination: first question of submission
							String destination = "/" + Destinations.question + "/" + s.getId() + "/q" + question.getId();
							context.put("testUrl", destination);
						}
					}
				}
				break;
			}

			case upload:
			{
				context.put("upload", Boolean.TRUE);

				// let them re-enter where they were
				context.put("testUrl", context.getPreviousDestination());
				
				// the size (megs) that was exceeded
				context.put("uploadMax", param);
			}
		}

		// render
		ui.render(uiError, context);
		return;
	}

	/**
	 * Get the UI for the final review destination
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submisssionId
	 *        The selected submission id.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void finalReviewGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
			throws IOException
	{
		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			if (assessmentService.allowCompleteSubmission(submissionId, null).booleanValue())
			{
				context.put("submission", submission);

				context.put("finalReview", Boolean.TRUE);

				// render
				ui.render(uiToc, context);
				return;
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
		return;
	}

	/**
	 * Read the input for the final review destination, process, and redirect to the next destination.
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
	protected void finalReviewPost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId)
			throws IOException
	{
		// this post is from the timer, or the "submit" button, and completes the submission
		submissionCompletePost(req, res, context, submissionId);
	}

	/**
	 * Get the UI for the list destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param sort
	 *        The sort parameter.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void listGet(HttpServletRequest req, HttpServletResponse res, String sort, Context context) throws IOException
	{
		// SORT: 0|1 A|D 0|1|2|3|4 A|D - 4 chars, the first two for the assessment list, the second two for the submissions list
		if ((sort != null) && (sort.length() != 4))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		AssessmentService.GetAvailableAssessmentsSort assessmentsSort = AssessmentService.GetAvailableAssessmentsSort.title_a;
		if (sort != null)
		{
			context.put("assessment_sort_choice", sort.charAt(0));
			context.put("assessment_sort_ad", sort.charAt(1));

			if ((sort.charAt(0) == '0') && (sort.charAt(1) == 'A'))
			{
				assessmentsSort = AssessmentService.GetAvailableAssessmentsSort.title_a;
			}
			else if ((sort.charAt(0) == '0') && (sort.charAt(1) == 'D'))
			{
				assessmentsSort = AssessmentService.GetAvailableAssessmentsSort.title_d;
			}
			else if ((sort.charAt(0) == '1') && (sort.charAt(1) == 'A'))
			{
				assessmentsSort = AssessmentService.GetAvailableAssessmentsSort.dueDate_a;
			}
			else if ((sort.charAt(0) == '1') && (sort.charAt(1) == 'D'))
			{
				assessmentsSort = AssessmentService.GetAvailableAssessmentsSort.dueDate_d;
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
				return;
			}
		}

		AssessmentService.GetOfficialSubmissionsSort submissionsSort = AssessmentService.GetOfficialSubmissionsSort.title_a;
		if (sort != null)
		{
			context.put("submission_sort_choice", sort.charAt(2));
			context.put("submission_sort_ad", sort.charAt(3));

			if ((sort.charAt(2) == '0') && (sort.charAt(3) == 'A'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.title_a;
			}
			else if ((sort.charAt(2) == '0') && (sort.charAt(3) == 'D'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.title_d;
			}
			else if ((sort.charAt(2) == '1') && (sort.charAt(3) == 'A'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.feedbackDate_a;
			}
			else if ((sort.charAt(2) == '1') && (sort.charAt(3) == 'D'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.feedbackDate_d;
			}
			else if ((sort.charAt(2) == '2') && (sort.charAt(3) == 'A'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.score_a;
			}
			else if ((sort.charAt(2) == '2') && (sort.charAt(3) == 'D'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.score_d;
			}
			else if ((sort.charAt(2) == '3') && (sort.charAt(3) == 'A'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.time_a;
			}
			else if ((sort.charAt(2) == '3') && (sort.charAt(3) == 'D'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.time_d;
			}
			else if ((sort.charAt(2) == '4') && (sort.charAt(3) == 'A'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.submittedDate_a;
			}
			else if ((sort.charAt(2) == '4') && (sort.charAt(3) == 'D'))
			{
				submissionsSort = AssessmentService.GetOfficialSubmissionsSort.submittedDate_d;
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
				return;
			}
		}

		if (sort == null)
		{
			context.put("assessment_sort_choice", '0');
			context.put("assessment_sort_ad", 'A');
			context.put("submission_sort_choice", '0');
			context.put("submission_sort_ad", 'A');
		}

		// collect information: assessments
		List assessments = assessmentService.getAvailableAssessments(toolManager.getCurrentPlacement().getContext(), null,
				assessmentsSort);
		context.put("assessments", assessments);

		// submissions
		List submissions = assessmentService.getOfficialSubmissions(toolManager.getCurrentPlacement().getContext(), null,
				submissionsSort);
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
	 * @param questionSelector
	 *        Which question(s) to put on the page: q followed by a questionId picks one, s followed by a sectionId picks a sections
	 *        worth, and a picks them all.
	 * @param feedback
	 *        The feedback parameter which could indicate (if it is "feedback") that the user wants feedback
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void questionGet(HttpServletRequest req, HttpServletResponse res, String submissionId, String questionSelector,
			String feedback, Context context) throws IOException
	{
		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		Errors err = questionSetup(submissionId, questionSelector, feedback, context, answers, true);

		if (err != null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + err + "/" + submissionId)));
			return;
		}

		// render
		ui.render(uiQuestion, context);
		return;
	}

	/**
	 * Read the input for the question destination, process, and redirect to the next destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        The UiContext
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionSelector
	 *        Which question(s) to put on the page: q followed by a questionId picks one, s followed by a sectionId picks a sections
	 * @param expected
	 *        true if this post was expected, false if not.
	 */
	protected void questionPost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId,
			String questionSelector) throws IOException
	{
		if (!context.getPostExpected())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}

		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		// setup receiving context
		Errors err = questionSetup(submissionId, questionSelector, "", context, answers, false);
		if (Errors.invalid == err) err = Errors.invalidpost;
		if (err != null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + err)));
			return;
		}

		// read form
		String destination = ui.decode(req, context);

		// check for file upload error
		boolean uploadError = ((req.getAttribute("upload.status") != null) && (!req.getAttribute("upload.status").equals("ok")));

		// if we are going to submitted, we must complete the submission (unless there was an upload error)
		Boolean complete = Boolean.valueOf((!uploadError) && destination.startsWith("/submitted"));

		// unless we are going to list, remove, or feedback, or this very same question, or we have a file upload error, mark the answers as complete
		Boolean answersComplete = Boolean.valueOf(!(uploadError || destination.startsWith("/list") || destination.startsWith("/remove")
				|| destination.endsWith("/feedback") || context.getPreviousDestination().equals(destination)));

		// submit all answers
		try
		{
			assessmentService.submitAnswers(answers, answersComplete, complete);

			// if there was an upload error, send to the upload error
			if ((req.getAttribute("upload.status") != null) && (!req.getAttribute("upload.status").equals("ok")))
			{
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.upload + "/" + req.getAttribute("upload.limit"))));
				return;
			}

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

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
	}

	/**
	 * Setup the context for question get and post
	 * 
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionSelector
	 *        Which question(s) to put on the page: q followed by a questionId picks one, s followed by a sectionId picks a sections
	 *        worth, and a picks them all.
	 * @param feedback
	 *        The feedback parameter which could indicate (if it is "feedback") that the user wants feedback
	 * @param context
	 *        UiContext.
	 * @param answers
	 *        A list to fill in with the answers for this page.
	 * @param out
	 *        Output writer.
	 * @return null if all went well, else an Errors to indicate what went wrong.
	 */
	protected Errors questionSetup(String submissionId, String questionSelector, String feedback, Context context,
			List<SubmissionAnswer> answers, boolean linearCheck)
	{
		// check on feedback
		if ("feedback".equalsIgnoreCase(feedback))
		{
			context.put("feedback", Boolean.TRUE);
		}

		// not in review mode
		context.put("review", Boolean.FALSE);

		// put in the selector
		context.put("questionSelector", questionSelector);

		// get the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			return Errors.invalid;
		}

		if (!assessmentService.allowCompleteSubmission(submissionId, null).booleanValue())
		{
			return Errors.unauthorized;
		}

		context.put("submission", submission);

		// for requests for a single question
		if (questionSelector.startsWith("q"))
		{
			String questionId = questionSelector.substring(1);
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question == null)
			{
				return Errors.invalid;
			}

			List<AssessmentQuestion> questions = new ArrayList<AssessmentQuestion>(1);
			questions.add(question);
			context.put("finishReady", submission.getIsAnswered(questions));

			// if we need to do our linear assessment check, and this is a linear assessment,
			// we will reject if the question has been marked as 'complete'
			if (linearCheck && !question.getSection().getAssessment().getRandomAccess().booleanValue()
					&& submission.getIsCompleteQuestion(question).booleanValue())
			{
				return Errors.linear;
			}

			// find the answer (or have one created) for this submission / question
			SubmissionAnswer answer = submission.getAnswer(question);
			if (answer != null)
			{
				answers.add(answer);
			}

			// tell the UI that we are doing single question
			context.put("question", question);
		}

		// for requests for a section
		else if (questionSelector.startsWith("s"))
		{
			String sectionId = questionSelector.substring(1);
			AssessmentSection section = submission.getAssessment().getSection(sectionId);
			if (section == null)
			{
				return Errors.invalid;
			}

			// get all the answers for this section
			List<AssessmentQuestion> questions = new ArrayList<AssessmentQuestion>();
			for (AssessmentQuestion question : section.getQuestions())
			{
				SubmissionAnswer answer = submission.getAnswer(question);
				answers.add(answer);
				questions.add(question);
			}

			context.put("finishReady", submission.getIsAnswered(questions));

			// tell the UI that we are doing single section
			context.put("section", section);
		}

		// for requests for the entire assessment
		else if (questionSelector.startsWith("a"))
		{
			// get all the answers to all the questions in all sections
			List<AssessmentQuestion> questions = new ArrayList<AssessmentQuestion>();
			for (AssessmentSection section : submission.getAssessment().getSections())
			{
				for (AssessmentQuestion question : section.getQuestions())
				{
					SubmissionAnswer answer = submission.getAnswer(question);
					answers.add(answer);
					questions.add(question);
				}
			}

			context.put("finishReady", submission.getIsAnswered(questions));
		}

		context.put("answers", answers);
		return null;
	}

	/**
	 * Get the UI for the remove destination
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionId
	 *        The current question id.
	 * @param reference
	 *        The attachment (to remove) reference
	 * @param context
	 *        UiContext.
	 */
	protected void removeGet(HttpServletRequest req, HttpServletResponse res, String submissionId, String questionId,
			String reference, Context context) throws IOException
	{
		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submissionId, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// collect the question
		AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
		if (question == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// if this is a linear assessment, we will reject if the question has been marked as 'complete'
		if (!submission.getAssessment().getRandomAccess().booleanValue()
				&& submission.getIsCompleteQuestion(question).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.linear + "/" + submissionId)));
			return;
		}

		context.put("question", question);

		Reference ref = entityManager.newReference(reference);
		List<Reference> attachment = new ArrayList<Reference>(1);
		attachment.add(ref);
		context.put("attachment", attachment);

		// render
		ui.render(uiRemove, context);
	}

	/**
	 * Read the input for the remove destination, process, and redirect to the next destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submisssionId
	 *        The selected submission id.
	 * @param questionId
	 *        The current question id.
	 * @param reference
	 *        The attachment (to remove) reference
	 * @param context
	 *        UiContext.
	 */
	protected void removePost(HttpServletRequest req, HttpServletResponse res, String submissionId, String questionId,
			String reference, Context context) throws IOException
	{
		if (!context.getPostExpected())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submissionId, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// read the form
		String destination = ui.decode(req, context);

		// if we are going to exit, we must cancel the remove and submit (timer expired)
		if (destination.startsWith("/submitted"))
		{
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
			return;
		}

		// not a submit, so it's a remove

		// remove the referenced attachment from the answer
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
		if (question == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		SubmissionAnswer answer = submission.getAnswer(question);
		if (answer == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// remove this one
		answer.removeAnswerText(reference);
		attachmentService.removeAttachment(entityManager.newReference(reference));

		// submit the user's answer
		try
		{
			assessmentService.submitAnswer(answer, Boolean.FALSE, Boolean.FALSE);

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

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
	}

	/**
	 * Get the UI for the review destination
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param submisssionId
	 *        The selected submission id.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void reviewGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
			throws IOException
	{
		// yes feedback, and we are in review
		context.put("feedback", Boolean.TRUE);
		context.put("review", Boolean.TRUE);

		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowReviewSubmission(submissionId, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);
		context.put("assessment", submission.getAssessment());

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

		// render using the question interface
		ui.render(uiQuestion, context);
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
	protected void submissionCompletePost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId)
			throws IOException
	{
		if (!context.getPostExpected())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}

		// read form
		String destination = ui.decode(req, context);

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

	/**
	 * Get the UI for the submitted destination.
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
	protected void submittedGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
			throws IOException
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
				ui.render(uiSubmitted, context);
				return;
			}

			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		// redirect to error
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
		return;
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
	protected void tocGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context) throws IOException
	{
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submissionId, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// collect information: the selected assessment (id the request)
		context.put("submission", submission);

		context.put("finalReview", Boolean.FALSE);

		// render
		ui.render(uiToc, context);
		return;
	}

	/**
	 * Read the input for the TOC destination, process, and redirect to the next destination.
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
		// this post is from the timer, or the "submit" button, and completes the submission
		submissionCompletePost(req, res, context, submissionId);
	}
}
