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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentSubmissionStatus;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionPart;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.SubmissionCompletedException;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.api.UiService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * A Simple Sludge Servlet Sakai Sample tool...
 */
public class AssessmentTestTool extends HttpServlet
{
	// "Vivie" numbers
	// final int contextsWithAssessments = 500;
	// final int assessmentsPerContext = 25;
	// final int submissionsPerStudent = 2;
	// final int contextStudents = 50;
	// final int itemsPerAssessment = 10;

	// // small
	// final int contextsWithAssessments = 2;
	// final int assessmentsPerContext = 2;
	// final int submissionsPerStudent = 2;
	// final int contextStudents = 2;
	// final int itemsPerAssessment = 2;

	public class GenerateSpecs
	{
		protected Integer assessmentsPerContext = new Integer(0);

		protected Integer contextStudents = new Integer(0);

		protected Integer contextsWithAssessments = new Integer(0);

		protected Integer itemsPerAssessment = new Integer(0);

		protected Integer submissionsPerStudent = new Integer(0);

		public GenerateSpecs()
		{
		}

		public GenerateSpecs(String value)
		{
			String[] values = StringUtil.split(value, "x");
			assessmentsPerContext = Integer.valueOf(values[0]);
			contextStudents = Integer.valueOf(values[1]);
			contextsWithAssessments = Integer.valueOf(values[2]);
			itemsPerAssessment = Integer.valueOf(values[3]);
			submissionsPerStudent = Integer.valueOf(values[4]);
		}

		public Integer getAssessmentsPerContext()
		{
			return assessmentsPerContext;
		}

		public Integer getContextStudents()
		{
			return contextStudents;
		}

		public Integer getContextsWithAssessments()
		{
			return contextsWithAssessments;
		}

		public Integer getItemsPerAssessment()
		{
			return itemsPerAssessment;
		}

		public Integer getSubmissionsPerStudent()
		{
			return submissionsPerStudent;
		}

		public void setAssessmentsPerContext(Integer value)
		{
			assessmentsPerContext = value;
		}

		public void setContextStudents(Integer value)
		{
			contextStudents = value;
		}

		public void setContextsWithAssessments(Integer value)
		{
			contextsWithAssessments = value;
		}

		public void setItemsPerAssessment(Integer value)
		{
			itemsPerAssessment = value;
		}

		public void setSubmissionsPerStudent(Integer value)
		{
			submissionsPerStudent = value;
		}

		public String toString()
		{
			return assessmentsPerContext.toString() + "x" + contextStudents.toString() + "x" + contextsWithAssessments.toString() + "x"
					+ itemsPerAssessment.toString() + "x" + submissionsPerStudent.toString();
		}
	}

	public class SimulateSpecs
	{
		protected Integer numUsers = 0;

		protected Integer startGap = 0;

		protected Integer thinkTime = 0;

		public SimulateSpecs()
		{
		}

		public SimulateSpecs(String value)
		{
			String[] values = StringUtil.split(value, "x");
			numUsers = Integer.valueOf(values[0]);
			startGap = Integer.valueOf(values[1]);
			thinkTime = Integer.valueOf(values[2]);
		}

		public Integer getNumUsers()
		{
			return numUsers;
		}

		public Integer getStartGap()
		{
			return this.startGap;
		}

		public Integer getThinkTime()
		{
			return this.thinkTime;
		}

		public void setNumUsers(Integer value)
		{
			numUsers = value;
		}

		public void setStartGap(Integer startGap)
		{
			this.startGap = startGap;
		}

		public void setThinkTime(Integer thinkTime)
		{
			this.thinkTime = thinkTime;
		}

		public String toString()
		{
			return numUsers.toString() + "x" + startGap.toString() + "x" + thinkTime.toString();
		}
	}

	/** Our tool destinations. */
	enum Destinations
	{
		error, generate, home, simulate
	}

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AssessmentTestTool.class);

	/** Localized messages. */
	protected static ResourceLoader messages = new ResourceLoader("mneme-test-tool");

	/** Our self-injected assessment service reference. */
	protected AssessmentService assessmentService = null;;

	/** Our self-injected id manager reference. */
	protected IdManager idManager = null;

	/** Lables used for answers. */
	protected String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
			"X", "Y", "Z"};

	/** Our self-injected security service reference. */
	protected SecurityService securityService = null;

	/** Our self-injected session manager reference. */
	protected SessionManager sessionManager = null;

	/** Our self-injected site service reference. */
	protected SiteService siteService = null;

	/** Our self-injected thread local manager reference. */
	protected ThreadLocalManager threadLocalManager = null;

	/** Our self-injected time service reference. */
	protected TimeService timeService = null;

	/** Our self-injected tool manager reference. */
	protected ToolManager toolManager = null;

	/** Our self-injected ui service reference. */
	protected UiService ui = null;

	/** The generate interface. */
	protected Controller uiGenerate = null;

	/** The home interface. */
	protected Controller uiHome = null;

	/** The simulate interface. */
	protected Controller uiSimulate = null;

	/** Our self-injected user directory service reference. */
	protected UserDirectoryService userDirectoryService = null;

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
		threadLocalManager = (ThreadLocalManager) ComponentManager.get(ThreadLocalManager.class);
		userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);

		uiHome = TestControllers.constructHome(ui);
		uiGenerate = TestControllers.constructGenerate(ui);
		uiSimulate = TestControllers.constructSimulate(ui);

		M_log.info("init()");
	}

	protected void answerEssay(AssessmentQuestion question, SubmissionAnswer answer)
	{
		answer.setEntryAnswerText("Essay text.");

		answerRationale(question, answer);
	}

	protected void answerFillIn(AssessmentQuestion question, SubmissionAnswer answer)
	{
		// The corrects list the answers (text). One part.
		List<AssessmentAnswer> answers = question.getPart().getCorrectAnswers();
		if ((answers == null) || (answers.isEmpty()))
		{
			return;
		}

		// pack the answer text into an array
		String[] answerIds = new String[answers.size()];
		int index = 0;
		for (AssessmentAnswer a : answers)
		{
			answerIds[index++] = a.getText();
		}

		// set the answer
		answer.setEntryAnswerTexts(answerIds);

		answerRationale(question, answer);
	}

	protected void answerMatch(AssessmentQuestion question, SubmissionAnswer answer)
	{
		// get a correct from each part
		List<AssessmentAnswer> answers = new ArrayList<AssessmentAnswer>();
		for (QuestionPart part : question.getParts())
		{
			List<AssessmentAnswer> partAnswers = part.getCorrectAnswers();
			answers.addAll(partAnswers);
		}

		// pack the answer ids into an array
		String[] answerIds = new String[answers.size()];
		int index = 0;
		for (AssessmentAnswer a : answers)
		{
			answerIds[index++] = a.getId();
		}

		// set the answer
		answer.setEntryAnswerIds(answerIds);

		answerRationale(question, answer);
	}

	protected void answerRationale(AssessmentQuestion question, SubmissionAnswer answer)
	{
		// rational required?
		if ((question.getRequireRationale() != null) && (question.getRequireRationale().booleanValue()))
		{
			answer.setRationale("rationale");
		}
	}

	protected void answerTF_MC_Survey(AssessmentQuestion question, SubmissionAnswer answer)
	{
		// which is correct? One part.
		List<AssessmentAnswer> answers = question.getPart().getCorrectAnswers();
		if ((answers == null) || (answers.isEmpty()))
		{
			// no correct answer, pick one.
			answers = new ArrayList<AssessmentAnswer>(1);
			answers.add(question.getPart().getAnswers().get(0));
		}

		// pack the answer ids into an array
		String[] answerIds = new String[answers.size()];
		int index = 0;
		for (AssessmentAnswer a : answers)
		{
			answerIds[index++] = a.getId();
		}

		// set the answer
		answer.setEntryAnswerIds(answerIds);

		answerRationale(question, answer);
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
			case generate:
			{
				// we need a single parameter (specs)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/")));
					return;
				}
				generateGet(req, res, context, parts[2]);
				break;
			}
			case simulate:
			{
				// we need a single parameter (specs)
				if (parts.length != 3)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/")));
					return;
				}
				simulateGet(req, res, context, parts[2]);
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
	 * Establish this as the "current" user.
	 * 
	 * @param userEid
	 *        The user eid.
	 */
	protected User establishUser(User user, String userEid)
	{
		// get the postmaster user
		try
		{
			if (user == null)
			{
				user = userDirectoryService.getUserByEid(userEid);
			}

			Session s = sessionManager.getCurrentSession();
			if (s != null)
			{
				s.setUserId(user.getId());
				s.setUserEid(user.getEid());
			}

			return user;
		}
		catch (UserNotDefinedException e)
		{
			return null;
		}
	}

	/**
	 * Generate some assessments and submissions
	 */
	protected String generate(GenerateSpecs specs)
	{
		// suppress event trackin events for better performance
		threadLocalManager.set("sakai.event.suppress", Boolean.TRUE);

		// the real context
		String context = toolManager.getCurrentPlacement().getContext();

		// if not at least assessment add in the current context, reject
		if (!assessmentService.allowAddAssessment(context)) return "not permitted";

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
			for (int contextCount = 0; contextCount < specs.contextsWithAssessments; contextCount++)
			{
				String randomContext = idManager.createUuid();

				for (int assessmentCount = 1; assessmentCount <= specs.assessmentsPerContext; assessmentCount++)
				{
					// generate the assessment
					String title = randomContext + " assessment " + assessmentCount;
					Assessment a = generateAssessment(randomContext, title, specs.itemsPerAssessment);

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
			for (int studentCount = 0; studentCount < specs.contextStudents; studentCount++)
			{
				String userId = "user" + studentCount;

				for (int submissionCount = 0; submissionCount < specs.submissionsPerStudent; submissionCount++)
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

			// how many assessments do we have already?
			int currentCount = assessmentService.countAssessments(context);

			// make a bunch of assessments for the current context
			for (int assessmentCount = 0; assessmentCount < specs.assessmentsPerContext; assessmentCount++)
			{
				// generate the assessment
				String title = context + " assessment " + (currentCount + assessmentCount);
				Assessment a = generateAssessment(context, title, specs.itemsPerAssessment);

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

				for (int submissionCount = 0; submissionCount < specs.submissionsPerStudent; submissionCount++)
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

		return "ok";
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
	protected Assessment generateAssessment(String context, String title, int numQuestions)
	{
		// Assessment -- Section -- Question -- Part -- Answer

		Assessment a = assessmentService.newAssessment();
		a.setContext(context);
		a.setTitle(title);
		a.setMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED);
		a.setAllowLateSubmit(Boolean.TRUE);
		a.setContinuousNumbering(Boolean.TRUE);
		a.setFeedbackDelivery(FeedbackDelivery.IMMEDIATE);
		a.setFeedbackShowAnswerFeedback(Boolean.TRUE);
		a.setFeedbackShowCorrectAnswer(Boolean.TRUE);
		a.setFeedbackShowQuestionFeedback(Boolean.TRUE);
		a.setFeedbackShowQuestionScore(Boolean.TRUE);
		a.setFeedbackShowStatistics(Boolean.TRUE);
		a.setNumSubmissionsAllowed(null);
		a.setRandomAccess(Boolean.TRUE);

		// add a section
		AssessmentSection s = assessmentService.newSection(a);
		s.setTitle("Part One");

		// add questions
		for (int i = 1; i <= numQuestions; i++)
		{
			AssessmentQuestion question = generateTrueFalse(s, true, 10, "question " + i, "correct!", "incorect :-(");

			i++;
			if (i > numQuestions) break;

			String[] answers = {"a", "b", "c", "d"};
			String[] feedbacks = {"aaa", "bbb", "ccc", "ddd"};
			generateMultipleChoice(s, true, 10, "question " + i, "yes!", "well, not quite", answers, 0, feedbacks, true);

			i++;
			if (i > numQuestions) break;
			Boolean[] corrects = {Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE};
			generateMultipleCorrect(s, true, 10, "question " + i, "got it!", "try again", answers, corrects, feedbacks, true);

			i++;
			if (i > numQuestions) break;
			generateSurvey(s, "question " + i, "thanks.");

			i++;
			if (i > numQuestions) break;
			generateEssay(s, "question " + i, 10, "feedback", "model");

			i++;
			if (i > numQuestions) break;
			String[] parts = {"red", "blue"};
			generateFillIn(s, "roses are {}, violets are {}", 10, "that is correct", "that is not correct", parts, false, false);

			i++;
			if (i > numQuestions) break;
			String[] parts2 = {"9"};
			generateNumeric(s, "3*3={}", 10, "Oui!", "Non :-(", parts2);

			i++;
			if (i > numQuestions) break;
			String[] choices = {"choice one", "choice two", "choice three", "choice four"};
			String[] matches = {"match one", "match two", "match three", "match four"};
			String[] corrects2 = {"correct one", "correct two", "correct three", "correct four"};
			String[] incorrects2 = {"incorrect one", "incorrect two", "incorrect three", "incorrect four"};
			generateMatch(s, 10, "match these", "correct", "incorrect", choices, matches, corrects2, incorrects2);
		}

		return a;
	}

	/**
	 * Generate an essay question.
	 * 
	 * @return The essay question.
	 */
	protected AssessmentQuestion generateEssay(AssessmentSection section, String title, float points, String feedback, String modelAnswer)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.essay);
		question.setRequireRationale(Boolean.FALSE);
		question.setScore(new Float(points));
		question.setFeedbackGeneral(feedback);

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		// answer
		AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
		answer.setText(modelAnswer);

		return question;
	}

	/**
	 * Generate a fill-in question
	 * 
	 * @return The fill-in question.
	 */
	protected AssessmentQuestion generateFillIn(AssessmentSection section, String title, float points, String correctFeedback,
			String incorrectFeedback, String[] answers, boolean mutuallyExclusive, boolean caseSensitive)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.fillIn);
		question.setRequireRationale(Boolean.FALSE);
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);
		question.setMutuallyExclusive(Boolean.valueOf(mutuallyExclusive));
		question.setCaseSensitive(Boolean.valueOf(caseSensitive));

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		// answers
		for (int i = 0; i < answers.length; i++)
		{
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(Boolean.TRUE);
			answer.setText(answers[i]);
		}

		return question;
	}

	/**
	 * Get the UI for the generate destination.
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
	protected void generateGet(HttpServletRequest req, HttpServletResponse res, Context context, String specsStr)
	{
		// reconstitute the specs
		GenerateSpecs specs = new GenerateSpecs(specsStr);

		// do it
		String rv = generate(specs);

		context.put("rv", rv);

		// render
		ui.render(uiGenerate, context);
	}

	/**
	 * Generate a multiple choice question.
	 * 
	 * @return The multiple choice question.
	 */
	protected AssessmentQuestion generateMatch(AssessmentSection section, float points, String title, String correctFeedback,
			String incorrectFeedback, String[] choices, String[] matches, String[] corrects, String[] incorrects)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.matching);
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);
		question.setInstructions(title);

		String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};

		// one part for each choice
		for (int c = 0; c < choices.length; c++)
		{
			QuestionPart part = assessmentService.newQuestionPart(question);
			part.setTitle(choices[c]);

			// an answer in each part for each match - correct if the sequence matches the part sequence
			for (int m = 0; m < matches.length; m++)
			{
				AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
				answer.setIsCorrect(m == c);
				answer.setText(matches[m]);
				answer.setFeedbackCorrect(corrects[m]);
				answer.setFeedbackIncorrect(incorrects[m]);
				answer.setLabel(labels[m]);
			}
		}

		return question;
	}

	/**
	 * Generate a multiple choice question.
	 * 
	 * @return The multiple choice question.
	 */
	protected AssessmentQuestion generateMultipleChoice(AssessmentSection section, boolean requireRational, float points, String title,
			String correctFeedback, String incorrectFeedback, String[] answers, int correctAnswerIndex, String[] feedbacks, boolean randomize)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.multipleChoice);
		question.setRequireRationale(Boolean.valueOf(requireRational));
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);
		question.setRandomAnswerOrder(Boolean.valueOf(randomize));

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};

		// answers
		for (int i = 0; i < answers.length; i++)
		{
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(i == correctAnswerIndex);
			answer.setText(answers[i]);
			answer.setFeedbackGeneral(feedbacks[i]);
			answer.setLabel(labels[i]);
		}

		return question;
	}

	/**
	 * Generate a multiple choice / multiple correct question.
	 * 
	 * @return The multiple choice / multiple correct question.
	 */
	protected AssessmentQuestion generateMultipleCorrect(AssessmentSection section, boolean requireRational, float points, String title,
			String correctFeedback, String incorrectFeedback, String[] answers, Boolean[] corrects, String[] feedbacks, boolean randomize)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.multipleCorrect);
		question.setRequireRationale(Boolean.valueOf(requireRational));
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);
		question.setRandomAnswerOrder(Boolean.valueOf(randomize));

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};

		// answers
		for (int i = 0; i < answers.length; i++)
		{
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(corrects[i]);
			answer.setText(answers[i]);
			answer.setFeedbackGeneral(feedbacks[i]);
			answer.setLabel(labels[i]);
		}

		return question;
	}

	/**
	 * Generate a numeric fill-in question
	 * 
	 * @return The numeric fill-in question.
	 */
	protected AssessmentQuestion generateNumeric(AssessmentSection section, String title, float points, String correctFeedback,
			String incorrectFeedback, String[] answers)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.numeric);
		question.setRequireRationale(Boolean.FALSE);
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		// answers
		for (int i = 0; i < answers.length; i++)
		{
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setIsCorrect(Boolean.TRUE);
			answer.setText(answers[i]);
		}

		return question;
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

		// answer each question
		for (AssessmentSection section : assessment.getSections())
		{
			for (AssessmentQuestion question : section.getQuestionsAsAuthored())
			{
				SubmissionAnswer answer = assessmentService.newSubmissionAnswer(s, question);

				answer.setSubmittedDate(timeService.newTime());

				if (question.getType() == QuestionType.essay)
				{
					answer.setEntryAnswerText("this is the response");
				}

				else if ((question.getType() == QuestionType.fillIn) || (question.getType() == QuestionType.numeric))
				{
					// how many answers
					List<? extends AssessmentAnswer> answers = question.getPart().getAnswers();
					if ((answers != null) && (answers.size() > 0))
					{
						String[] answerTexts = new String[answers.size()];

						// pick the answer
						if (Math.random() > 0.5)
						{
							for (int i = 0; i < answerTexts.length; i++)
							{
								answerTexts[i] = answers.get(i).getText();
							}
						}

						else
						{
							for (int i = 0; i < answerTexts.length; i++)
							{
								answerTexts[i] = "no";
							}
						}
						answer.setEntryAnswerTexts(answerTexts);
					}
				}

				else if (question.getType() == QuestionType.matching)
				{
					// how many answers
					List<? extends QuestionPart> parts = question.getParts();
					if ((parts != null) && (parts.size() > 0))
					{
						String[] answerIds = new String[parts.size()];
						for (int i = 0; i < answerIds.length; i++)
						{
							if (Math.random() > 0.5)
							{
								// pick the part's correct answer
								List<AssessmentAnswer> correctAnswers = parts.get(i).getCorrectAnswers();
								if ((correctAnswers != null) && (correctAnswers.size() > 0))
								{
									answerIds[i] = correctAnswers.get(0).getId();
								}
							}

							else
							{
								// pick the part's incorrect answer
								List<AssessmentAnswer> incorrectAnswers = parts.get(i).getIncorrectAnswers();
								if ((incorrectAnswers != null) && (incorrectAnswers.size() > 0))
								{
									answerIds[i] = incorrectAnswers.get(0).getId();
								}
							}
						}

						answer.setEntryAnswerIds(answerIds);
					}
				}

				else if (question.getType() == QuestionType.multipleChoice)
				{
					// pick the answer
					if (Math.random() > 0.5)
					{
						// pick a correct answer
						List<AssessmentAnswer> correctAnswers = question.getPart().getCorrectAnswers();
						if ((correctAnswers != null) && (correctAnswers.size() > 0))
						{
							answer.setEntryAnswerIds(correctAnswers.get(0).getId());
						}
					}

					// pick an incorrect answer
					else
					{
						List<AssessmentAnswer> incorrectAnswers = question.getPart().getIncorrectAnswers();
						if ((incorrectAnswers != null) && (incorrectAnswers.size() > 0))
						{
							answer.setEntryAnswerIds(incorrectAnswers.get(0).getId());
						}
					}
				}

				else if (question.getType() == QuestionType.multipleCorrect)
				{
					// pick the answer
					if (Math.random() > 0.5)
					{
						// pick all the correct answers
						List<AssessmentAnswer> correctAnswers = question.getPart().getCorrectAnswers();
						if ((correctAnswers != null) && (correctAnswers.size() > 0))
						{
							String[] answerIds = new String[correctAnswers.size()];
							for (int i = 0; i < answerIds.length; i++)
							{
								answerIds[i] = correctAnswers.get(i).getId();
							}
							answer.setEntryAnswerIds(answerIds);
						}
					}

					// pick an incorrect answer
					else
					{
						List<AssessmentAnswer> incorrectAnswers = question.getPart().getIncorrectAnswers();
						if ((incorrectAnswers != null) && (incorrectAnswers.size() > 0))
						{
							answer.setEntryAnswerIds(incorrectAnswers.get(0).getId());
						}
					}
				}

				else if (question.getType() == QuestionType.survey)
				{
					// just pick an answer
					List<? extends AssessmentAnswer> answers = question.getPart().getAnswers();
					if ((answers != null) && (answers.size() > 0))
					{
						answer.setEntryAnswerIds(answers.get(0).getId());
					}
				}

				else if (question.getType() == QuestionType.trueFalse)
				{
					// pick the answer (assume true/false and two possible answers)
					if (Math.random() > 0.5)
					{
						// pick a correct answer
						List<AssessmentAnswer> correctAnswers = question.getPart().getCorrectAnswers();
						if ((correctAnswers != null) && (correctAnswers.size() > 0))
						{
							answer.setEntryAnswerIds(correctAnswers.get(0).getId());
						}
					}

					// pick an incorrect answer
					else
					{
						List<AssessmentAnswer> incorrectAnswers = question.getPart().getIncorrectAnswers();
						if ((incorrectAnswers != null) && (incorrectAnswers.size() > 0))
						{
							answer.setEntryAnswerIds(incorrectAnswers.get(0).getId());
						}
					}
				}
			}
		}

		return s;
	}

	/**
	 * Generate a survey question.
	 * 
	 * @return The survey question.
	 */
	protected AssessmentQuestion generateSurvey(AssessmentSection section, String title, String feedback)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.survey);
		question.setRequireRationale(Boolean.FALSE);
		question.setScore(new Float(0));
		question.setFeedbackGeneral(feedback);

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		// answers
		for (int i = 1; i <= 5; i++)
		{
			AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
			answer.setText(Integer.toString(i));
		}

		return question;
	}

	/**
	 * Generate a true/false question.
	 * 
	 * @return The true/false question.
	 */
	protected AssessmentQuestion generateTrueFalse(AssessmentSection section, boolean requireRational, float points, String title,
			String correctFeedback, String incorrectFeedback)
	{
		AssessmentQuestion question = assessmentService.newQuestion(section);

		question.setType(QuestionType.trueFalse);
		question.setRequireRationale(Boolean.valueOf(requireRational));
		question.setScore(new Float(points));
		question.setFeedbackCorrect(correctFeedback);
		question.setFeedbackIncorrect(incorrectFeedback);

		// one part
		QuestionPart part = assessmentService.newQuestionPart(question);
		part.setTitle(title);

		// answers
		AssessmentAnswer answer = assessmentService.newAssessmentAnswer(part);
		answer.setIsCorrect(Boolean.TRUE);
		answer.setText("true");

		answer = assessmentService.newAssessmentAnswer(part);
		answer.setIsCorrect(Boolean.FALSE);
		answer.setText("false");

		return question;
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
		context.put("gspecs", new GenerateSpecs());
		context.put("sspecs", new SimulateSpecs());

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

		// read form
		GenerateSpecs gspecs = new GenerateSpecs();
		context.put("gspecs", gspecs);
		SimulateSpecs sspecs = new SimulateSpecs();
		context.put("sspecs", sspecs);
		String destination = ui.decode(req, context);

		// look for special codes in the destination
		if ("/generate".equals(destination))
		{
			// add the specs
			destination = destination + "/" + gspecs.toString();
		}

		else if ("/simulate".equals(destination))
		{
			// add the specs
			destination = destination + "/" + sspecs.toString();
		}

		// redirect to home
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
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

	protected String simulate(final SimulateSpecs specs)
	{
		final String sakaiContext = toolManager.getCurrentPlacement().getContext();
		final StringBuffer results = new StringBuffer();

		// create a thread for each user
		Thread[] threads = new Thread[specs.numUsers];
		for (int i = 0; i < specs.numUsers; i++)
		{
			final String userEid = "student" + (i + 1);

			threads[i] = new Thread(new Runnable()
			{
				public void run()
				{
					String rv = simulateSubmission(sakaiContext, userEid, specs.getThinkTime().intValue());
					results.append(rv + "<br />");
				};
			});
		}

		// start them all
		for (int i = 0; i < specs.numUsers; i++)
		{
			threads[i].start();

			// pause a bit between starts
			if (specs.getStartGap().intValue() > 0)
			{
				try
				{
					Thread.sleep(specs.getStartGap().intValue());
				}
				catch (InterruptedException ignore)
				{
				}
			}
		}

		// wait for them all
		for (int i = 0; i < specs.numUsers; i++)
		{
			try
			{
				threads[i].join();
			}
			catch (InterruptedException e)
			{
			}
		}

		return results.toString();
	}

	/**
	 * Get the UI for the generate destination.
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
	protected void simulateGet(HttpServletRequest req, HttpServletResponse res, Context context, String specsStr)
	{
		SimulateSpecs specs = new SimulateSpecs(specsStr);

		// do the simulation
		String rv = simulate(specs);

		context.put("rv", rv);

		// render
		ui.render(uiSimulate, context);
	}

	/**
	 * Submit the correct answers to a test for a user.
	 * 
	 * @param context
	 *        The context in which to find and submit an assessment.
	 * @param userEid
	 *        The user EID for which we will simulate a submission.
	 * @param thinkTime
	 *        The time (ms) to wait between seeing a question and answering it.
	 */
	protected String simulateSubmission(String context, String userEid, int thinkTime)
	{
		Time start = timeService.newTime();

		User currentUser = null;
		try
		{
			currentUser = userDirectoryService.getUser(sessionManager.getCurrentSessionUserId());
		}
		catch (UserNotDefinedException e)
		{
		}

		try
		{
			User user = establishUser(null, userEid);

			// get the list of assessments (simulate list view)
			AssessmentService.GetUserContextSubmissionsSort sort = AssessmentService.GetUserContextSubmissionsSort.status_d;
			List<Submission> submissions = assessmentService.getUserContextSubmissions(context, null, sort);

			// shuffle these to get a random available one
			Collections.shuffle(submissions, new Random(userEid.hashCode()));

			// clear the thread to simulate a new requst / response cycle
			threadLocalManager.clear();
			if (thinkTime > 0)
			{
				try
				{
					Thread.sleep(thinkTime);
				}
				catch (InterruptedException ignore)
				{
				}
			}
			establishUser(user, userEid);

			// pick one that is ready to take
			String aid = null;
			for (Submission submission : submissions)
			{
				AssessmentSubmissionStatus status = submission.getAssessmentSubmissionStatus();
				if ((status == AssessmentSubmissionStatus.completeReady) || (status == AssessmentSubmissionStatus.ready))
				{
					aid = submission.getAssessment().getId();
					break;
				}
			}
			if (aid == null) return "no assessment";

			// simulate the enter view get

			// useless code
			Assessment assessment = assessmentService.idAssessment(aid);
			assessment.getIsClosed();
			assessmentService.allowSubmit(assessment, null);
			if ((assessment.getPassword() == null) && (assessment.getDescription() == null) && (assessment.getAttachments().isEmpty())
					&& (assessment.getRandomAccess().booleanValue()) && (assessment.getTimeLimit() == null))
			{
			}
			// useless code

			// enter the submission (simulate enter view post)

			// useless code
			assessment = assessmentService.idAssessment(aid);
			assessment.getPassword();
			// useless code

			String sid = null;
			try
			{
				Submission submission = assessmentService.enterSubmission(assessmentService.idAssessment(aid), null);
				sid = submission.getId();

				// useless code
				assessment = submission.getAssessment();
				QuestionPresentation presentation = assessment.getQuestionPresentation();
				assessment.getRandomAccess();
				// useless code
			}
			catch (AssessmentClosedException e)
			{
				return e.toString();
			}
			catch (AssessmentCompletedException e)
			{
				return e.toString();
			}
			catch (AssessmentPermissionException e)
			{
				return e.toString();
			}

			if (sid == null) return "could not enter submission";

			// pick the first question
			AssessmentQuestion firstQuestion = assessmentService.idSubmission(sid).getAssessment().getFirstSection().getFirstQuestion();
			if (firstQuestion == null) return "no first quesiton";
			String qid = firstQuestion.getId();

			// loop for the entire test
			int count = 0;
			while (qid != null)
			{
				// emulate the question view get

				// useless code
				Submission submission = assessmentService.idSubmission(sid);
				submission.completeIfOver();
				submission = assessmentService.idSubmission(sid);
				assessmentService.allowCompleteSubmission(submission, null);
				AssessmentQuestion question = submission.getAssessment().getQuestion(qid);
				question.getSection().getAssessment().getRandomAccess();
				submission.getIsCompleteQuestion(question);
				// useless code

				// clear the thread to simulate a new requst / response cycle
				threadLocalManager.clear();
				if (thinkTime > 0)
				{
					try
					{
						Thread.sleep(thinkTime);
					}
					catch (InterruptedException ignore)
					{
					}
				}
				establishUser(user, userEid);

				// emulate the quesiton view post
				// form an answer to qid
				submission = assessmentService.idSubmission(sid);
				question = submission.getAssessment().getQuestion(qid);
				SubmissionAnswer answer = submission.getAnswer(question);

				// answer the question
				switch (question.getType())
				{
					case trueFalse:
					case multipleChoice:
					case multipleCorrect:
					case survey:
					{
						answerTF_MC_Survey(question, answer);
						break;
					}

					case essay:
					{
						answerEssay(question, answer);
						break;
					}

					case fillIn:
					case numeric:
					{
						answerFillIn(question, answer);
						break;
					}

					case matching:
					{
						answerMatch(question, answer);
						break;
					}
						// TODO: fileUpload, audioRecording
				}

				// get the next question
				AssessmentQuestion nextQuesiton = question.getAssessmentOrdering().getNext();
				if (nextQuesiton != null)
				{
					qid = nextQuesiton.getId();
				}
				else
				{
					qid = null;
				}

				// submit it
				try
				{
					assessmentService.submitAnswer(answer, Boolean.TRUE, Boolean.FALSE);
					count++;
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

			// finish
			try
			{
				assessmentService.completeSubmission(assessmentService.idSubmission(sid));
			}
			catch (AssessmentClosedException e)
			{
				return e.toString();
			}
			catch (SubmissionCompletedException e)
			{
				return e.toString();
			}
			catch (AssessmentPermissionException e)
			{
				return e.toString();
			}

			Time end = timeService.newTime();
			return "user: " + userEid + " submission: " + sid + " assessment: " + aid + " questions: " + count + " time(ms): "
					+ (end.getTime() - start.getTime());
		}

		finally
		{
			threadLocalManager.clear();

			if (currentUser != null)
			{
				establishUser(currentUser, null);
			}
		}
	}
}
