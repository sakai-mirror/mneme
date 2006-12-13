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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentClosedException;
import org.sakaiproject.assessment.api.AssessmentCompletedException;
import org.sakaiproject.assessment.api.AssessmentPermissionException;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.AssessmentService;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sludge.api.Context;
import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.UiService;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.Web;

/**
 * A Simple Sludge Servlet Sakai Sample tool...
 */
public class AssessmentTestTool extends HttpServlet
{
	/** Our tool destinations. */
	enum Destinations
	{
		error, home
	}

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AssessmentTestTool.class);

	/** Localized messages. */
	protected static ResourceLoader messages = new ResourceLoader("assessment-test-tool");

	/** Our self-injected assessment service reference. */
	protected AssessmentService assessmentService = null;

	/** Our self-injected id manager reference. */
	protected IdManager idManager = null;;

	/** Our self-injected session manager reference. */
	protected SessionManager sessionManager = null;

	/** Our self-injected site service reference. */
	protected SiteService siteService = null;

	/** Our self-injected time service reference. */
	protected TimeService timeService = null;

	/** Our self-injected tool manager reference. */
	protected ToolManager toolManager = null;

	/** Our self-injected security service reference. */
	protected SecurityService securityService = null;

	/** Our self-injected ui service reference. */
	protected UiService ui = null;

	/** The home interface. */
	protected Controller uiHome = null;

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
		return "Sakai Assessment Test";
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
		siteService = (SiteService) ComponentManager.get(SiteService.class);
		idManager = (IdManager) ComponentManager.get(IdManager.class);
		ui = (UiService) ComponentManager.get(UiService.class);
		securityService = (SecurityService) ComponentManager.get(SecurityService.class);

		uiHome = TestControllers.constructHome(ui);

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
		Context context = ui.prepareGet(req, res, messages, Destinations.home.toString());

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
			case home:
			{
				homeGet(req, res, context);
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
		Context context = ui.preparePost(req, res, messages, Destinations.home.toString());

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
			case home:
			{
				homePost(req, res, context);
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
	 * Get the UI for the error destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        UiContext.
	 */
	protected void errorGet(HttpServletRequest req, HttpServletResponse res, Context context)
	{
	}

	/**
	 * Generate some assessments and submissions
	 */
	protected void generate()
	{
		 // "Vivie" numbers
		 final int contextsWithAssessments = 500;
		 final int assessmentsPerContext = 25;
		 final int submissionsPerStudent = 2;
		 final int contextStudents = 50;
		 final int itemsPerAssessment = 10;

//		// small
//		final int contextsWithAssessments = 2;
//		final int assessmentsPerContext = 2;
//		final int submissionsPerStudent = 2;
//		final int contextStudents = 2;
//		final int itemsPerAssessment = 2;

		// the real context
		String context = toolManager.getCurrentPlacement().getContext();

		// if not at least assessment add in the current context, reject
		if (!assessmentService.allowAddAssessment(context)) return;

		// get the session and current user info
		Session sakaiSession = sessionManager.getCurrentSession();
		String curUserId = sakaiSession.getUserId();

		// setup a security advisor to gloss over all the bogus contexts we reference (with no security defined)
		securityService.pushAdvisor(new SecurityAdvisor()
		{
			public SecurityAdvice isAllowed(String userId, String function, String reference)
			{
				return SecurityAdvice.ALLOWED;
			}
		});

		try
		{
			// collect the assessments generated
			Collection<Assessment> assessments = new ArrayList<Assessment>();

			// make assessments for a bunch of other contexts (with randomly generated contexts)
			for (int contextCount = 0; contextCount < contextsWithAssessments; contextCount++)
			{
				String randomContext = idManager.createUuid();

				for (int assessmentCount = 1; assessmentCount <= assessmentsPerContext; assessmentCount++)
				{
					// generate the assessment
					Assessment a = generateAssessment(randomContext, assessmentCount, itemsPerAssessment);

					// save the assessment
					try
					{
						assessmentService.addAssessment(a);

						// record the assessment
						assessments.add(a);
					}
					catch (AssessmentPermissionException e)
					{
						M_log.warn("generate: permission exception adding assessment: " + e.toString());
					}
				}
			}

			// submit a few for each user for these other assessments
			for (int studentCount = 0; studentCount < contextStudents; studentCount++)
			{
				String userId = "user" + studentCount;

				for (int submissionCount = 0; submissionCount < submissionsPerStudent; submissionCount++)
				{
					for (Iterator iAssessments = assessments.iterator(); iAssessments.hasNext();)
					{
						Submission s = generateSubmission((Assessment) iAssessments.next(), userId);
						try
						{
							// switch users to this user to pass security!
							sakaiSession.setUserId(s.getUserId());

							assessmentService.addSubmission(s);

							// switch back
							sakaiSession.setUserId(curUserId);
						}
						catch (AssessmentPermissionException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
						catch (AssessmentClosedException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
						catch (AssessmentCompletedException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
					}
				}
			}

			// roster (of students) for the context
			Site site = siteService.getSite(context);
			Set studentIds = site.getUsersHasRole("Student");

			// roster (of instructors) for the context
			Set instructorIds = site.getUsersHasRole("Instructor");

			// collect the assessment ids generated
			assessments = new ArrayList<Assessment>();

			// make a bunch of assessments for the current context
			for (int assessmentCount = 1; assessmentCount <= assessmentsPerContext; assessmentCount++)
			{
				// generate the assessment
				Assessment a = generateAssessment(context, assessmentCount, itemsPerAssessment);

				try
				{
					// save the assessment
					assessmentService.addAssessment(a);

					// record the assessment
					assessments.add(a);
				}
				catch (AssessmentPermissionException e)
				{
					M_log.warn("generate: permission exception adding assessment: " + e.toString());
				}
			}

			// each user makes a few submissions to each assessment
			for (Iterator userIterator = studentIds.iterator(); userIterator.hasNext();)
			{
				String userId = (String) userIterator.next();

				for (int submissionCount = 0; submissionCount < submissionsPerStudent; submissionCount++)
				{
					for (Iterator iAssessments = assessments.iterator(); iAssessments.hasNext();)
					{
						Submission s = generateSubmission((Assessment) iAssessments.next(), userId);
						try
						{
							// switch users to this user to pass security!
							sakaiSession.setUserId(s.getUserId());

							assessmentService.addSubmission(s);

							// switch back
							sakaiSession.setUserId(curUserId);
						}
						catch (AssessmentPermissionException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
						catch (AssessmentClosedException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
						catch (AssessmentCompletedException e)
						{
							M_log.warn("generate: adding assessment: " + e.toString());
						}
					}
				}
			}
		}
		catch (IdUnusedException e)
		{
			M_log.warn("generate: ", e);
		}
		finally
		{
			// restore the correct session user
			sakaiSession.setUserId(curUserId);
			
			// clear the advisor
			securityService.popAdvisor();
		}
	}

	/**
	 * Generate one assessment
	 * 
	 * @param context
	 *        For this context.
	 * @param count
	 *        A counter used in the title.
	 * @param numQuestions
	 *        With this number of items.
	 * @return The assessment.
	 */
	protected Assessment generateAssessment(String context, int count, int numQuestions)
	{
		// Assessment -- Section -- Question -- Part -- Answer

		Assessment a = assessmentService.newAssessment();
		a.setContext(context);
		a.setTitle(context + " assessment " + count);
		a.setMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED);

		// add a section
		AssessmentSection s = assessmentService.newSection(a);
		s.setTitle("Part One");

		// add questions
		for (int i = 1; i <= numQuestions; i++)
		{
			AssessmentQuestion question = assessmentService.newQuestion(s);

			question.setRequireRationale(Boolean.TRUE);
			question.setScore(new Float(10));
			question.setType(QuestionType.trueFalse);

			// a part
			QuestionPart part = assessmentService.newQuestionPart(question);
			part.setTitle("question");

			// answers
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(Boolean.TRUE);
			answer.setText("true");
			answer.setLabel("A");

			answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(Boolean.FALSE);
			answer.setText("false");
			answer.setLabel("B");
		}

		return a;
	}

	/**
	 * Generate a submission.
	 * 
	 * @param assessmentId
	 *        To this assessment.
	 * @param userId
	 *        For this user.
	 * @return The submission.
	 */
	protected Submission generateSubmission(Assessment assessment, String userId)
	{
		// Submission -- Answser -- Entry
		Submission s = assessmentService.newSubmission(assessment);
		s.setUserId(userId);
		s.setStartDate(timeService.newTime());
		s.setSubmittedDate(timeService.newTime());
		s.setStatus(new Integer(1));
		s.setIsComplete(Boolean.TRUE);

		// Answer each question (of the first section)
		List items = new Vector();
		for (AssessmentQuestion question : assessment.getFirstSection().getQuestions())
		{
			SubmissionAnswer answer = assessmentService.newSubmissionAnswer(s, question);

			answer.setSubmittedDate(timeService.newTime());

			// setQuestion establised an entry per question part, which is one for us.
			SubmissionAnswerEntry entry = answer.getEntries().get(0);

			// pick the answer (assume true/false and two possible answers)
			if (Math.random() > 0.5)
			{
				// pick a correct answer
				List<AssessmentAnswer> correctAnswers = question.getPart().getCorrectAnswers();
				if ((correctAnswers != null) && (correctAnswers.size() > 0))
				{
					entry.setAssessmentAnswer(correctAnswers.get(0));
				}
			}

			// pick an incorrect answer
			else
			{
				List<AssessmentAnswer> incorrectAnswers = question.getPart().getIncorrectAnswers();
				if ((incorrectAnswers != null) && (incorrectAnswers.size() > 0))
				{
					entry.setAssessmentAnswer(incorrectAnswers.get(0));
				}
			}
		}

		return s;
	}

	/**
	 * Get the UI for the home destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param assessmentId
	 *        The selected assessment id.
	 * @param context
	 *        UiContext.
	 * @param out
	 *        Output writer.
	 */
	protected void homeGet(HttpServletRequest req, HttpServletResponse res, Context context)
	{
		// render
		ui.render(uiHome, context);
	}

	/**
	 * Read the input for the home destination, process, and redirect to the next destination.
	 * 
	 * @param req
	 *        Servlet request.
	 * @param res
	 *        Servlet response.
	 * @param context
	 *        The UiContext.
	 * @param expected
	 *        true if this post was expected, false if not.
	 */
	protected void homePost(HttpServletRequest req, HttpServletResponse res, Context context) throws IOException
	{
		if (!context.getPostExpected())
		{
			redirectError(req, res);
			return;
		}

		// read form: for now, nothing to read
		String destination = ui.decode(req, context);

		// generate
		generate();

		// redirect to home
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/home")));
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
}
