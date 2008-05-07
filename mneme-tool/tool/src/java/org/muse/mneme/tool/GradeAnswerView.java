/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /grade_answer view for the mneme tool.
 */
public class GradeAnswerView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeAnswerView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** AttachmentService service. */
	protected AttachmentService attachmentService = null;

	/** Submission Service */
	protected SubmissionService submissionService = null;

	/** Dependency: ToolManager */
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
		// [2]sid, [3] answerId, [4]next/prev sort (optional- leave out to disable next/prev), optionally followed by a return destination
		if (params.length < 4) throw new IllegalArgumentException();

		Submission submission = this.submissionService.getSubmission(params[2]);
		if (submission == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// get answer id from [3]
		String answerId = params[3];
		Answer answer = submission.getAnswer(answerId);
		if (answer == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("answer", answer);

		// compute next and prev answers
		Question nextQuestion = answer.getQuestion().getAssessmentOrdering().getNext();
		if (nextQuestion != null)
		{
			Answer nextAnswer = submission.getAnswer(nextQuestion);
			if (nextAnswer != null)
			{
				context.put("next", nextAnswer.getId());
			}
		}

		Question prevQuestion = answer.getQuestion().getAssessmentOrdering().getPrevious();
		if (prevQuestion != null)
		{
			Answer prevAnswer = submission.getAnswer(prevQuestion);
			if (prevAnswer != null)
			{
				context.put("prev", prevAnswer.getId());
			}
		}

		String sortCode = "userName_a";
		SubmissionService.FindAssessmentSubmissionsSort sort = null;
		int destinationStartsAt = 5;
		if (params.length > 4) sortCode = params[4];
		try
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.valueOf(sortCode);
		}
		catch (IllegalArgumentException e)
		{
			// no sort, so it must be part of destination
			destinationStartsAt = 4;
			sortCode = "userName_a";
		}
		context.put("sort", sortCode);

		String destination = null;
		if (params.length > destinationStartsAt)
		{
			destination = "/" + StringUtil.unsplit(params, destinationStartsAt, params.length - destinationStartsAt, "/");
		}

		// if not specified, go to the main grade_assessment page for this assessment
		else
		{
			destination = "/grade_assessment/0A/" + submission.getAssessment().getId();
		}
		context.put("return", destination);

		// needed by some of the delegates to show the score
		context.put("grading", Boolean.TRUE);

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
		// [2]sid, [3] answerId, [4]next/prev sort (optional- leave out to disable next/prev), optionally followed by a return destination
		if (params.length < 4) throw new IllegalArgumentException();

		Submission submission = this.submissionService.getSubmission(params[2]);
		if (submission == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		context.put("submission", submission);

		// get answer id from [3]
		String answerId = params[3];
		Answer answer = submission.getAnswer(answerId);
		if (answer == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("answer", answer);

		// read form
		String destination = this.uiService.decode(req, context);

		// check for remove
		if (destination.startsWith("REMOVE:"))
		{
			String[] parts = StringUtil.splitFirst(destination, ":");
			if (parts.length == 2)
			{
				parts = StringUtil.splitFirst(parts[1], ":");
				if (parts.length == 2)
				{
					Reference ref = this.attachmentService.getReference(parts[1]);
					this.attachmentService.removeAttachment(ref);

					// if this is for the overall evaluation
					if (parts[0].equals("SUBMISSION"))
					{
						submission.getEvaluation().removeAttachment(ref);
					}
					else
					{
						// find the answer, id=parts[0], ref=parts[1]
						Answer a = submission.getAnswer(parts[0]);
						if (a != null)
						{
							a.getEvaluation().removeAttachment(ref);
						}
					}
				}
			}

			destination = context.getDestination();
		}

		// save graded submission
		try
		{
			this.submissionService.evaluateSubmission(submission);
		}
		catch (AssessmentPermissionException e)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// if there was an upload error, send to the upload error
		if ((req.getAttribute("upload.status") != null) && (!req.getAttribute("upload.status").equals("ok")))
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.upload + "/" + req.getAttribute("upload.limit"))));
			return;
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * @param assessmentService
	 *        the assessmentService to set
	 */
	public void setAssessmentService(AssessmentService assessmentService)
	{
		this.assessmentService = assessmentService;
	}

	/**
	 * Set the AttachmentService.
	 * 
	 * @param service
	 *        the AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		this.attachmentService = service;
	}

	/**
	 * @param submissionService
	 *        the submissionService to set
	 */
	public void setSubmissionService(SubmissionService submissionService)
	{
		this.submissionService = submissionService;
	}

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
