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
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.tool.AssessmentDeliveryTool.Destinations;
import org.muse.mneme.tool.AssessmentDeliveryTool.Errors;
import org.sakaiproject.util.Web;

/**
 * The /question view for the mneme tool.
 */
public class QuestionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionView.class);

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
		// we need two parameters (sid/quesiton selector)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String questionSelector = params[3];

		Submission submission = assessmentService.idSubmission(submissionId);

		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// handle our 'z' selector - redirect to the appropriate question for this submission
		if ("z".equals(questionSelector))
		{
			redirectToQuestion(req, res, submission, true, false);
			return;
		}

		// if the submission has past a hard deadline or ran out of time, close it and tell the user
		if (submission.completeIfOver().booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.over)));
			return;
		}

		context.put("actionTitle", messages.getString("question-header-work"));

		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		Errors err = questionSetup(submission, questionSelector, context, answers, true);
		if (err != null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + err + "/" + submissionId)));
			return;
		}

		// check that the answers are all unchanged
		if (submission.getIsAnswersChanged().booleanValue())
		{
			M_log.warn("quesitonGet: submission has modified answers: " + submissionId);

			// // redirect to error
			// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			// return;
		}

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
		// we need two parameters (sid/quesiton selector)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String questionSelector = params[3];

		// if (!context.getPostExpected())
		// {
		// // redirect to error
		// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
		// return;
		// }

		// collect the questions (actually their answers) to put on the page
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>();

		// get the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// setup receiving context
		Errors err = questionSetup(submission, questionSelector, context, answers, false);
		if (Errors.invalid == err) err = Errors.invalidpost;
		if (err != null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + err)));
			return;
		}

		// check that the answers are all unchanged
		if (submission.getIsAnswersChanged().booleanValue())
		{
			M_log.warn("quesitonPost: submission has modified answers: " + submissionId);

			// // redirect to error
			// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			// return;
		}

		// read form
		String destination = uiService.decode(req, context);

		// check for file upload error
		boolean uploadError = ((req.getAttribute("upload.status") != null) && (!req.getAttribute("upload.status").equals("ok")));

		// if we are going to submitted, we must complete the submission (unless there was an upload error)
		Boolean complete = Boolean.valueOf((!uploadError) && destination.startsWith("/submitted"));

		// unless we are going to list, remove, instructions (or soon hints), or this very same question, or we have a file upload error, mark the
		// answers as complete
		// TODO: when hints are in, this counts (adding ~ || destination.endsWith("/feedback"))
		Boolean answersComplete = Boolean.valueOf(!(uploadError || destination.startsWith("/list") || destination.startsWith("/remove")
				|| destination.startsWith("/instructions") || context.getPreviousDestination().equals(destination)));

		// and if we are working in a random access test, answers are always complete
		if (submission.getAssessment().getRandomAccess().booleanValue()) answersComplete = Boolean.TRUE;

		// where are we going?
		destination = questionChooseDestination(destination, questionSelector, submissionId);

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

			if (destination.equals("SUBMIT"))
			{
				// get the submission again, to make sure that the answers we just posted are reflected
				submission = assessmentService.idSubmission(submissionId);

				// if linear, or the submission is all answered, we can complete the submission and go to submitted
				if ((!submission.getAssessment().getRandomAccess().booleanValue()) || (submission.getIsAnswered(null).booleanValue()))
				{
					assessmentService.completeSubmission(submission);

					destination = "/" + Destinations.submitted + "/" + submissionId;
				}

				// if not linear, and there are unanswered parts, send to final review
				else
				{
					destination = "/" + Destinations.final_review + "/" + submissionId;
				}
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
	 * for PREV and NEXT, choose the destination.
	 * 
	 * @param destination
	 *        The destination encoded in the request.
	 * @param questionSelector
	 *        Which question(s) to put on the page: q followed by a questionId picks one, s followed by a sectionId picks a sections
	 * @param submisssionId
	 *        The selected submission id.
	 */
	protected String questionChooseDestination(String destination, String questionSelector, String submissionId)
	{
		// get the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			return "/" + Destinations.error + "/" + Errors.invalid;
		}

		// for requests for a single question
		if (questionSelector.startsWith("q"))
		{
			// make sure by-question is valid for this assessment
			if (submission.getAssessment().getQuestionPresentation() != QuestionPresentation.BY_QUESTION)
			{
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			String questionId = questionSelector.substring(1);
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question == null)
			{
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			if ("NEXT".equals(destination))
			{
				// if the question is not the last of the section, go to the next quesiton
				if (!question.getSectionOrdering().getIsLast())
				{
					return "/" + Destinations.question + "/" + submissionId + "/q" + question.getAssessmentOrdering().getNext().getId();
				}

				// if there's a next section
				if (!question.getSection().getOrdering().getIsLast().booleanValue())
				{
					// if the next section is not merged
					AssessmentSection next = question.getSection().getOrdering().getNext();
					if (!next.getIsMerged().booleanValue())
					{
						// choose the section instructions
						return "/" + Destinations.section_instructions + "/" + submissionId + "/" + next.getId();
					}

					// otherwise choose the first question of the next section
					return "/" + Destinations.question + "/" + submissionId + "/q" + next.getFirstQuestion().getId();
				}

				// no next part, this is an error
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			else if ("PREV".equals(destination))
			{
				// if the question is not the first of the section, go to the prev quesiton
				if (!question.getSectionOrdering().getIsFirst())
				{
					return "/" + Destinations.question + "/" + submissionId + "/q" + question.getAssessmentOrdering().getPrevious().getId();
				}

				// prev into this section's instructions... if this section is not merged
				AssessmentSection section = question.getSection();
				if (!section.getIsMerged().booleanValue())
				{
					// choose the section instructions
					return "/" + Destinations.section_instructions + "/" + submissionId + "/" + section.getId();
				}

				// otherwise choose the last question of the prev section, if we have one
				AssessmentSection prev = section.getOrdering().getPrevious();
				if (prev != null)
				{
					return "/" + Destinations.question + "/" + submissionId + "/q" + prev.getLastQuestion().getId();
				}

				// no prev part, this is an error
				return "/" + Destinations.error + "/" + Errors.invalid;
			}
		}

		// for section-per-page
		else if (questionSelector.startsWith("s"))
		{
			// make sure by-section is valid for this assessment
			if (submission.getAssessment().getQuestionPresentation() != QuestionPresentation.BY_SECTION)
			{
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			String sectionId = questionSelector.substring(1);
			AssessmentSection section = submission.getAssessment().getSection(sectionId);
			if (section == null)
			{
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			if ("NEXT".equals(destination))
			{
				// if there's a next section, go there
				if (!section.getOrdering().getIsLast().booleanValue())
				{
					AssessmentSection next = section.getOrdering().getNext();
					return "/" + Destinations.question + "/" + submissionId + "/s" + next.getId();
				}

				// no next section, this is an error
				return "/" + Destinations.error + "/" + Errors.invalid;
			}

			else if ("PREV".equals(destination))
			{
				// if there's a prev section, choose to enter that
				if (!section.getOrdering().getIsFirst().booleanValue())
				{
					AssessmentSection prev = section.getOrdering().getPrevious();
					return "/" + Destinations.question + "/" + submissionId + "/s" + prev.getId();
				}

				// no prev section, this is an error
				return "/" + Destinations.error + "/" + Errors.invalid;
			}
		}

		return destination;
	}

	/**
	 * Setup the context for question get and post
	 * 
	 * @param submisssion
	 *        The selected submission.
	 * @param questionSelector
	 *        Which question(s) to put on the page: q followed by a questionId picks one, s followed by a sectionId picks a sections worth, and a
	 *        picks them all.
	 * @param context
	 *        UiContext.
	 * @param answers
	 *        A list to fill in with the answers for this page.
	 * @param out
	 *        Output writer.
	 * @return null if all went well, else an Errors to indicate what went wrong.
	 */
	protected Errors questionSetup(Submission submission, String questionSelector, Context context, List<SubmissionAnswer> answers,
			boolean linearCheck)
	{
		// not in review mode
		context.put("review", Boolean.FALSE);

		// put in the selector
		context.put("questionSelector", questionSelector);

		if (!assessmentService.allowCompleteSubmission(submission, null).booleanValue())
		{
			return Errors.unauthorized;
		}

		context.put("submission", submission);

		// for requests for a single question
		if (questionSelector.startsWith("q"))
		{
			// TODO: assure the test is by-question

			String questionId = questionSelector.substring(1);
			AssessmentQuestion question = submission.getAssessment().getQuestion(questionId);
			if (question == null)
			{
				return Errors.invalid;
			}

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
			// TODO: assure the test is by-section

			String sectionId = questionSelector.substring(1);
			AssessmentSection section = submission.getAssessment().getSection(sectionId);
			if (section == null)
			{
				return Errors.invalid;
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
			// TODO: assure the test is by-test

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

		context.put("answers", answers);
		return null;
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
