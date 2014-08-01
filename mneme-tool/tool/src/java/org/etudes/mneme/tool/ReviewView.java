/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Etudes, Inc.
 * 
 * Portions completed before September 1, 2008
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

package org.etudes.mneme.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.ambrosia.api.Context;
import org.etudes.ambrosia.util.ControllerImpl;
import org.etudes.mneme.api.Answer;
import org.etudes.mneme.api.AssessmentService;
import org.etudes.mneme.api.AssessmentType;
import org.etudes.mneme.api.MnemeService;
import org.etudes.mneme.api.ReviewShowCorrect;
import org.etudes.mneme.api.Submission;
import org.etudes.mneme.api.SubmissionCompletionStatus;
import org.etudes.mneme.api.SubmissionService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /review view for the mneme tool.
 */
public class ReviewView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ReviewView.class);

	/** Dependency: AssessmentService. */
	protected AssessmentService assessmentService = null;

	/** Event tracking service. */
	protected EventTrackingService eventTrackingService = null;

	/** Dependency: SubmissionService. */
	protected SubmissionService submissionService = null;

	/** tool manager reference. */
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
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// sid and return
		if (params.length < 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String destination = null;
		if (params.length > 3)
		{
			destination = "/" + StringUtil.unsplit(params, 3, params.length - 3, "/");
		}

		// if not specified, go to the main list view
		else
		{
			destination = "/list";
		}
		context.put("return", destination);

		// yes feedback, and we are in review
		context.put("review", Boolean.TRUE);
		context.put("actionTitle", messages.getString("question-header-review"));

		// collect the submission
		Submission submission = submissionService.getSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		boolean instructorViewWork = submission.getMayViewWork();
		context.put("viewWork", Boolean.valueOf(instructorViewWork));

		// must have permission for review, or for view work
		if ((!submissionService.allowReviewSubmission(submission)) && (!instructorViewWork))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// validity check
		if ((!submission.getAssessment().getIsValid()) && (!instructorViewWork))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// collect the other submissions from this user to the assessment
		List<? extends Submission> allSubmissions = submissionService.getMultipleSubmissions(submission.getAssessment(), submission.getUserId());

		// compute position within the group, pick the next and prev
		int size = allSubmissions.size();
		int position = 1;
		for (Submission s : allSubmissions)
		{
			if (s.equals(submission)) break;
			position++;
		}

		// prev: wrap if needed
		Submission prev = null;
		if (position > 1)
		{
			prev = allSubmissions.get(position - 2);
		}
		else
		{
			prev = allSubmissions.get(size - 1);
		}

		// next: wrap if needed
		Submission next = null;
		if (position < size)
		{
			next = allSubmissions.get(position);
		}
		else
		{
			next = allSubmissions.get(0);
		}

		if (size > 0) context.put("count", Integer.valueOf(size));
		context.put("position", Integer.valueOf(position));
		if (prev != null) context.put("prevSubmissionId", prev.getId());
		if (next != null) context.put("nextSubmissionId", next.getId());

		// best is grade related, so only for assessments with points, released, and only if review is available (or we are doing instructor view)
		// also only if there are multiple submissions
		if ((size > 1) && (submission.getIsReleased().booleanValue()) && (submission.getAssessment().getHasPoints().booleanValue())
				&& (allSubmissions.get(position - 1).getBest().equals(submission)) && (instructorViewWork || submission.getMayReview().booleanValue()))
		{
			context.put("best", Boolean.TRUE);
			context.put("submission", allSubmissions.get(position - 1).getBest());
		}
		else
		{
			context.put("submission", submission);
		}

		// collect all the answers for review
		List<Answer> answers = submission.getAnswers();
		context.put("answers", answers);

		// these only for student view, not instructor work view
		if (!instructorViewWork)
		{
			// in this special case, since there's no real action in the service to do this, we need to generate an event
			eventTrackingService.post(eventTrackingService.newEvent(MnemeService.SUBMISSION_REVIEW, submission.getReference(), false));

			// record the review date
			submissionService.markReviewed(submission);
		}

		if ((!instructorViewWork) && (submission.getAssessment().getReview().getShowIncorrectQuestions() == ReviewShowCorrect.incorrect_only))
		{
			boolean incorrectExists = false;
			// Check to see if there is at least one non-essay, non-task and non-likert-scale question
			for (Answer a : answers)
			{
				String questionType = a.getQuestion().getType();
				if (questionType.equals("mneme:FillBlanks") || questionType.equals("mneme:Match") || questionType.equals("mneme:MultipleChoice")
						|| questionType.equals("mneme:TrueFalse"))
				{
					incorrectExists = true;
					break;
				}
			}

			if (incorrectExists)
			{
				context.put("questionIncorrect", messages.getString("question-incorrect"));
				context.put("showIncorrect", Boolean.TRUE);
			}
		}

		SubmissionCompletionStatus subComp = submission.getCompletionStatus();
		boolean noneAnswered = false;
		if (subComp.equals(SubmissionCompletionStatus.evaluationNonSubmit)
				|| ((subComp.equals(SubmissionCompletionStatus.autoComplete) || subComp.equals(SubmissionCompletionStatus.userFinished)) && submission
						.getIsUnanswered() == Boolean.TRUE)) noneAnswered = true;
		if (submission.getAssessment().getReview().getShowSummary()
				&& submission.getAssessment().getReview().getShowCorrectAnswer().equals(ReviewShowCorrect.yes) && !noneAnswered)
		{
			if (submission.getAssessment().getType().equals(AssessmentType.survey) && submission.getAssessment().getFrozen())
			{
				context.put("showsummary", Boolean.TRUE);
			}
			if (!submission.getAssessment().getType().equals(AssessmentType.survey))
			{
				if ((size > 1) && (submission.getIsReleased().booleanValue()) && (submission.getAssessment().getHasPoints().booleanValue())
						&& (allSubmissions.get(position - 1).getBest().equals(submission)) && submission.getMayReview().booleanValue())
				{
					context.put("showsummary", Boolean.TRUE);
				}
				if (size == 1)
				{
					context.put("showsummary", Boolean.TRUE);
				}
			}
		}

		// for the tool navigation
		if (this.assessmentService.allowManageAssessments(toolManager.getCurrentPlacement().getContext()))
		{
			context.put("maintainer", Boolean.TRUE);
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
		// read form
		String destination = this.uiService.decode(req, context);
		if (params.length < 3)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		// collect the submission
		Submission submission = submissionService.getSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// go there
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
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
	 * Set the event tracking service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		this.eventTrackingService = service;
	}

	/**
	 * Set the submission service.
	 * 
	 * @param service
	 *        The submission service.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param manager
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager manager)
	{
		toolManager = manager;
	}
}
