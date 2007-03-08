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
import java.util.Collection;
import java.util.Iterator;
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
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentClosedException;
import org.sakaiproject.assessment.api.AssessmentCompletedException;
import org.sakaiproject.assessment.api.AssessmentPermissionException;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.AssessmentService;
import org.sakaiproject.assessment.api.FeedbackDelivery;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
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
import org.sakaiproject.thread_local.api.ThreadLocalManager;
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

	public class Specs
	{
		protected int assessmentsPerContext = 0;

		protected int contextStudents = 0;

		protected int contextsWithAssessments = 0;

		protected int itemsPerAssessment = 0;

		protected int submissionsPerStudent = 0;

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
	}

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

	/** Our self-injected time service reference. */
	protected TimeService timeService = null;

	/** Our self-injected tool manager reference. */
	protected ToolManager toolManager = null;

	/** Our self-injected thread local manager reference. */
	protected ThreadLocalManager threadLocalManager = null;

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
		threadLocalManager = (ThreadLocalManager) ComponentManager.get(ThreadLocalManager.class);

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
	protected void generate(Specs specs)
	{
		// suppress event trackin events for better performance
		threadLocalManager.set("sakai.event.suppress", Boolean.TRUE);

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
		context.put("specs", new Specs());

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
		Specs specs = new Specs();
		context.put("specs", specs);
		String destination = ui.decode(req, context);

		// generate (one)
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
		generate(specs);

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
