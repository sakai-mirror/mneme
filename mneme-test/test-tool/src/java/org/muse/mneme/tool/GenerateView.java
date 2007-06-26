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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionPart;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;

/**
 * The /generate view for the mneme test tool.
 */
public class GenerateView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GenerateView.class);

	/** The assessment service. */
	protected AssessmentService assessmentService = null;

	/** The id manager. */
	protected IdManager idManager = null;

	/** The security service. */
	protected SecurityService securityService = null;

	/** The session manager. */
	protected SessionManager sessionManager = null;

	/** The site service. */
	protected SiteService siteService = null;

	/** The ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/** The time service. */
	protected TimeService timeService = null;

	/** The tool manager. */
	protected ToolManager toolManager = null;

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
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
	{
		// if not logged in as the super user, we won't do anything
		if (!securityService.isSuperUser())
		{
			throw new IllegalArgumentException();
		}

		// one parameter expected
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		// reconstitute the specs
		GenerateSpecs specs = new GenerateSpecs(params[2]);

		// do it
		String rv = generate(specs);

		context.put("rv", rv);

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
	 * Set the id manager.
	 * 
	 * @param service
	 *        The id manager.
	 */
	public void setIdManager(IdManager service)
	{
		this.idManager = service;
	}

	/**
	 * Set the security service.
	 * 
	 * @param service
	 *        The security service.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * Set the session manager.
	 * 
	 * @param service
	 *        The session manager.
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * Set the site service.
	 * 
	 * @param service
	 *        the site service.
	 */
	public void setSiteService(SiteService service)
	{
		this.siteService = service;
	}

	/**
	 * Set the thread local manager.
	 * 
	 * @param service
	 *        The thread local manager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * Set the time service.
	 * 
	 * @param service
	 *        The time service.
	 */
	public void setTimeService(TimeService service)
	{
		this.timeService = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param service
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager service)
	{
		this.toolManager = service;
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
}
