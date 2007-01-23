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
import java.util.List;

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
	public class Qa
	{
		protected SubmissionAnswer a = null;

		protected AssessmentQuestion q = null;

		public Qa(AssessmentQuestion question, SubmissionAnswer answer)
		{
			this.q = question;
			this.a = answer;
		}

		public SubmissionAnswer getAnswer()
		{
			return this.a;
		}

		public AssessmentQuestion getQuestion()
		{
			return this.q;
		}
	}

	/** Our tool destinations. */
	enum Destinations
	{
		enter, error, exit, list, question, remove, review, submit, toc
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

	/** The exit interface. */
	protected Controller uiExit = null;

	/** The list interface. */
	protected Controller uiList = null;

	/** The question interface. */
	protected Controller uiQuestion = null;

	/** The remove interface. */
	protected Controller uiRemove = null;

	/** The sbmit interface. */
	protected Controller uiSubmit = null;

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
		attachmentService = (AttachmentService) ComponentManager.get(AttachmentService.class);
		timeService = (TimeService) ComponentManager.get(TimeService.class);
		ui = (UiService) ComponentManager.get(UiService.class);
		entityManager = (EntityManager) ComponentManager.get(EntityManager.class);

		// make the uis
		uiList = DeliveryControllers.constructList(ui);
		uiEnter = DeliveryControllers.constructEnter(ui);
		uiQuestion = DeliveryControllers.constructQuestion(ui);
		uiExit = DeliveryControllers.constructExit(ui);
		uiToc = DeliveryControllers.constructToc(ui);
		uiSubmit = DeliveryControllers.constructSubmit(ui);
		uiRemove = DeliveryControllers.constructRemove(ui);

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
		if (ui.dispatchResource(req, res, getServletContext())) return;

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
					errorGet(req, res, context);
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
				// we need two parameters (sid/quesiton selector) and an optional third (/feedback)
				if ((parts.length != 4) && (parts.length != 5))
				{
					errorGet(req, res, context);
				}
				else
				{
					questionGet(req, res, parts[2], parts[3], (parts.length == 5) ? parts[4] : null, context);
				}
				break;
			}
			case submit:
			{
				// we need one parameter (sid)
				if (parts.length != 3)
				{
					errorGet(req, res, context);
				}
				else
				{
					submitGet(req, res, parts[2], context);
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
			case remove:
			{
				// we need three parameters (sid/qid/ref), but ref is really all the rest
				if (parts.length < 5)
				{
					errorGet(req, res, context);
				}
				else
				{
					removeGet(req, res, parts[2], parts[3], "/" + StringUtil.unsplit(parts, 4, parts.length - 4, "/"), context);
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
				// we need two parameters (sid/qid), and tolerate but ignore a third (/feedback)
				if ((parts.length != 4) && (parts.length != 5))
				{
					redirectError(req, res);
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
					errorGet(req, res, context);
				}
				else
				{
					removePost(req, res, parts[2], parts[3], "/" + StringUtil.unsplit(parts, 4, parts.length - 4, "/"), context);
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
			case submit:
			{
				// we need a single parameter (sid)
				if (parts.length != 3)
				{
					redirectError(req, res);
				}
				else
				{
					submitPost(req, res, context, parts[2]);
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

		// process: enter the assessment for this user, find the submission id and starting question
		Assessment assessment = assessmentService.idAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
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
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
			return;
		}

		// question, section or all?
		QuestionPresentation presentation = assessment.getQuestionPresentation();

		// for by quesion
		if ((presentation == null) || (presentation == QuestionPresentation.BY_QUESTION))
		{
			String questionId = null;

			// for linear assessments, start at the first unseen question
			if (!assessment.getRandomAccess())
			{
				AssessmentQuestion question = submission.getFirstUnseenQuestion();
				if (question != null)
				{
					questionId = question.getId();
				}

				// otherwise send the user to the submit view
				else
				{
					destination = "/" + Destinations.submit + "/" + submission.getId();
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
		}

		// for by section
		else if (presentation == QuestionPresentation.BY_SECTION)
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
		else if (presentation == QuestionPresentation.BY_ASSESSMENT)
		{
			destination = "/" + Destinations.question + "/" + submission.getId() + "/a";

			// redirect
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
			return;
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
	 * @param sort
	 *        The sort parameter.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void listGet(HttpServletRequest req, HttpServletResponse res, String sort, Context context)
	{
		// SORT: 0|1 A|D 0|1|2|3|4 A|D - 4 chars, the first two for the assessment list, the second two for the submissions list
		if ((sort != null) && (sort.length() != 4))
		{
			errorGet(req, res, context);
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
				errorGet(req, res, context);
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
				errorGet(req, res, context);
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
			String feedback, Context context)
	{
		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		if (!questionSetup(submissionId, questionSelector, feedback, context, answers))
		{
			errorGet(req, res, context);
			return;
		}

		// render
		ui.render(uiQuestion, context);
		return;

		// // if the assessment is linear and this question has been seen already, we don't allow entry
		// if ((!question.getSection().getAssessment().getRandomAccess()) && submission.getSeenQuestion(question))
		// {
		// // TODO: better error reporting!
		// errorGet(req, res, context);
		// return;
		// }
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
		// TODO: deal with unexpected

		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		// setup receiving context
		if (!questionSetup(submissionId, questionSelector, "", context, answers))
		{
			errorGet(req, res, context);
			return;
		}

		// read form
		String destination = ui.decode(req, context);

		// if we are going to exit, we must complete the submission (last answer only)
		boolean complete = false;
		if (destination.startsWith("/exit"))
		{
			complete = true;
		}

		// submit all answers
		try
		{
			assessmentService.submitAnswers(answers, complete);
		}
		catch (AssessmentClosedException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
			return;
		}
		catch (SubmissionCompletedException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
			return;
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error")));
			return;
		}

		// redirect to the next destination
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
		return;
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
	 * @return true if all was well, false if there was an error
	 */
	protected boolean questionSetup(String submissionId, String questionSelector, String feedback, Context context,
			List<SubmissionAnswer> answers)
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
		if (submission != null)
		{
			// TODO: security check (user matches submission user)
			// TODO: check that the assessment is open
			context.put("submission", submission);

			// for requests for a single question
			if (questionSelector.startsWith("q"))
			{
				String questionId = questionSelector.substring(1);
				AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
				if (question == null)
				{
					return false;
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
					return false;
				}

				// get all the answers for this section
				for (AssessmentQuestion question : section.getQuestions())
				{
					SubmissionAnswer answer = submission.getAnswer(question);
					answers.add(answer);
				}

				// tell the UI that we are doing single section
				context.put("section", section);
			}

			// for requests for the entire assessment
			else if (questionSelector.startsWith("a"))
			{
				// get all the answers to all the questions in all sections
				for (AssessmentSection section : submission.getAssessment().getSections())
				{
					for (AssessmentQuestion question : section.getQuestions())
					{
						SubmissionAnswer answer = submission.getAnswer(question);
						answers.add(answer);
					}
				}
			}

			else
			{
				return false;
			}

			context.put("answers", answers);
			return true;
		}

		// // if the assessment is linear and this question has been seen already, we don't allow entry
		// if ((!question.getSection().getAssessment().getRandomAccess()) && submission.getSeenQuestion(question))
		// {
		// // TODO: better error reporting!
		// errorGet(req, res, context);
		// return;
		// }

		return false;
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
			String reference, Context context)
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

				Reference ref = entityManager.newReference(reference);
				List<Reference> attachment = new ArrayList<Reference>(1);
				attachment.add(ref);
				context.put("attachment", attachment);

				// render
				ui.render(uiRemove, context);
				return;
			}
		}

		errorGet(req, res, context);
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
		// TODO: deal with unexpected

		// read the form
		String destination = ui.decode(req, context);

		// remove the referenced attachment from the answer
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question != null)
			{
				SubmissionAnswer answer = submission.getAnswer(question);
				if (answer != null)
				{
					// remove this one
					answer.removeAnswerText(reference);
					attachmentService.removeAttachment(entityManager.newReference(reference));

					// submit the user's answer
					try
					{
						assessmentService.submitAnswer(answer, false);

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
	{
		// yes feedback, and we are in review
		context.put("feedback", Boolean.TRUE);
		context.put("review", Boolean.TRUE);

		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			// TODO: security check (user matches submission user)
			// TODO: check that the submission is closed
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
			return;
		}

		errorGet(req, res, context);
	}

	/**
	 * Get the UI for the submit destination
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
	protected void submitGet(HttpServletRequest req, HttpServletResponse res, String submissionId, Context context)
	{
		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission != null)
		{
			// TODO: security check (user matches submission user)
			// TODO: check that the assessment is open
			context.put("submission", submission);

			Assessment assessment = submission.getAssessment();
			if (assessment != null)
			{
				context.put("assessment", assessment);

				// render
				ui.render(uiSubmit, context);
				return;
			}
		}

		errorGet(req, res, context);
	}

	/**
	 * Read the input for the submit destination, process, and redirect to the next destination.
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
	protected void submitPost(HttpServletRequest req, HttpServletResponse res, Context context, String submissionId)
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
