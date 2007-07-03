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
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.UiService;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.Expiration;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.tool.AssessmentDeliveryTool.Errors;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /list view for the mneme tool.
 */
public class UploadRemoveView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(UploadRemoveView.class);

	/** Assessment service. */
	protected MnemeService assessmentService = null;

	/** Attachment service. */
	protected AttachmentService attachmentService = null;

	/** Entity manager. */
	protected EntityManager entityManager = null;

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
		// we need three parameters (sid/qid/ref), but ref is really all the rest
		if (params.length < 5)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String questionId = params[3];
		String reference = "/" + StringUtil.unsplit(params, 4, params.length - 4, "/");

		// collect the submission
		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submission, null).booleanValue())
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
		if (!submission.getAssessment().getRandomAccess().booleanValue() && submission.getIsCompleteQuestion(question).booleanValue())
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
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		
		this.ui = this.constructRemove(this.uiService);

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// we need three parameters (sid/qid/ref), but ref is really all the rest
		if (params.length < 5)
		{
			throw new IllegalArgumentException();
		}

		String submissionId = params[2];
		String questionId = params[3];
		String reference = "/" + StringUtil.unsplit(params, 4, params.length - 4, "/");

		// if (!context.getPostExpected())
		// {
		// // redirect to error
		// res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
		// return;
		// }

		Submission submission = assessmentService.idSubmission(submissionId);
		if (submission == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		if (!assessmentService.allowCompleteSubmission(submission, null).booleanValue())
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// read the form
		String destination = uiService.decode(req, context);

		// if we are going to exit, we must cancel the remove and submit (timer expired)
		if (destination.startsWith("/submitted"))
		{
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
	 * Set the assessment service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setAssessmentService(MnemeService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Set the attachment service.
	 * 
	 * @param service
	 *        The attachment service.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		this.attachmentService = service;
	}

	/**
	 * Set the entity manager.
	 * 
	 * @param service
	 *        The entity manager.
	 */
	public void setEntityManager(EntityManager service)
	{
		this.entityManager = service;
	}
	
	/**
	 * The remove interface needs the following entities in the context:
	 * submission - the submission being taken
	 * question - the current quesiton
	 * attachment - List containing he attachment selected for removal
	 */
	protected Component constructRemove(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("remove-title")
				.setHeader("remove-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(ui.newCourier().setDestination(ui.newDestination().setDestination("/courier")).setFrequency(600))
				.add(
					ui.newCountdownTimer()
						.setSubmit()
						.setDurationMessage("timer-duration", ui.newDurationPropertyReference().setConcise().setReference("submission.expiration.limit"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(Expiration.Cause.timeLimit.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newCountdownTimer()
						.setSubmit()
						.setDurationMessage("timer-due", ui.newDatePropertyReference().setReference("submission.expiration.time"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(Expiration.Cause.closedDate.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert()
								.setText("remove-confirm"))
						.add(
							ui.newAttachments()
								.setAttachments(ui.newPropertyReference().setReference("attachment"), null)
								.setSize(false)
								.setTimestamp(true)))
				.add(ui.newGap())
				.add(
					ui.newNavigationBar()
						.setWidth("70em")
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_QUESTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"))	
				.add(
					ui.newNavigationBar()
						.setWidth("70em")
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_SECTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"))		
				.add(
					ui.newNavigationBar()
						.setWidth("70em")
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_ASSESSMENT.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"));
	}
}
