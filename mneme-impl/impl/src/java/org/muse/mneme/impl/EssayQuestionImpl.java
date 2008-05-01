/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
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

package org.muse.mneme.impl;

import org.muse.ambrosia.api.Attachments;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.FileUpload;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.Instructions;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.Overlay;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Section;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.Toggle;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.StringUtil;

/**
 * EssayQuestionImpl handles questions for the essay question type.
 */
public class EssayQuestionImpl implements TypeSpecificQuestion
{
	/** An enumerate type that declares the types of submissions */
	public enum SubmissionType
	{
		attachments, both, inline, none;
	}

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** The model answer */
	protected String modelAnswer = null;

	protected transient QuestionPlugin plugin = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** The type of submission */
	protected SubmissionType submissionType = SubmissionType.inline;

	/** Dependency: The UI service (Ambrosia). */
	protected transient UiService uiService = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public EssayQuestionImpl(Question question, EssayQuestionImpl other)
	{
		this.messages = other.messages;
		this.question = other.question;
		this.modelAnswer = other.modelAnswer;
		this.submissionType = other.submissionType;
		this.uiService = other.uiService;
		this.plugin = other.plugin;
	}

	/**
	 * Construct.
	 * 
	 * @param uiService
	 *        the UiService.
	 * @param question
	 *        The Question this is a helper for.
	 */
	public EssayQuestionImpl(QuestionPlugin plugin, InternationalizedMessages messages, UiService uiService, Question question)
	{
		this.plugin = plugin;
		this.messages = messages;
		this.uiService = uiService;
		this.question = question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone(Question question)
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// set the question
			((EssayQuestionImpl) rv).question = question;

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String consolidate(String destination)
	{
		return destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		// submission type
		Selection type = uiService.newSelection();
		type.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.submissionType"));
		type.addSelection(this.uiService.newMessage().setMessage("inline"), this.uiService.newMessage().setTemplate("inline"));
		type.addSelection(this.uiService.newMessage().setMessage("inline-attachments"), this.uiService.newMessage().setTemplate("both"));
		type.addSelection(this.uiService.newMessage().setMessage("attachments"), this.uiService.newMessage().setTemplate("attachments"));
		type.setTitle("submission", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));

		Section typeSection = this.uiService.newSection();
		typeSection.add(type);

		// model answer
		HtmlEdit modelAnswer = this.uiService.newHtmlEdit();
		modelAnswer.setSize(HtmlEdit.Sizes.tall);
		modelAnswer.setProperty(this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));
		modelAnswer.setTitle("model-answer-edit", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		Section modelAnswerSection = this.uiService.newSection();
		modelAnswerSection.add(modelAnswer);

		return this.uiService.newFragment().setMessages(this.messages).add(typeSection).add(modelAnswerSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getData()
	{
		String[] rv = new String[2];
		rv[0] = this.submissionType.toString();
		rv[1] = this.modelAnswer;

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		Text question = this.uiService.newText();
		question.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.presentation.text"));

		Attachments attachments = this.uiService.newAttachments();
		attachments.setAttachments(this.uiService.newPropertyReference().setReference("answer.question.presentation.attachments"), null);
		attachments.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.presentation.attachments")));

		Section questionSection = this.uiService.newSection();
		questionSection.add(question).add(attachments);

		Section answerSection = this.uiService.newSection();

		// the text entry
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		edit.setSize(HtmlEdit.Sizes.tall);
		edit.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.inline.toString(), SubmissionType.both.toString())
				.setProperty(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));
		edit.setProperty(this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));
		edit.setOnEmptyAlert(this.uiService.newDecision().setReversed().setProperty(
				this.uiService.newPropertyReference().setReference("answer.submission.assessment.randomAccess")), "linear-missing");
		answerSection.add(edit);

		// the upload
		FileUpload upload = this.uiService.newFileUpload();
		upload.setTitle("upload-title");
		upload.setUpload("upload-button");
		upload.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.upload"));
		upload.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.attachments.toString(),
				SubmissionType.both.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));

		Attachments uploaded = this.uiService.newAttachments();
		uploaded.setAttachments(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded"), "attachment");
		uploaded.setSize(false).setTimestamp(false);
		uploaded.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.attachments.toString(),
				SubmissionType.both.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));

		Navigation remove = this.uiService.newNavigation();
		remove.setTitle("upload-remove").setStyle(Navigation.Style.link).setSubmit().setSmall();
		remove.setIcon("/icons/delete.png", Navigation.IconStyle.none);
		remove.setDestination(this.uiService.newDestination().setDestination("STAY_REMOVE:{0}",
				this.uiService.newPropertyReference().setReference("attachment.reference")));
		remove.setConfirm(this.uiService.newTrueDecision(), "cancel", "/icons/cancel.gif", "confirm-remove");
		uploaded.addNavigation(remove);

		answerSection.add(upload).add(uploaded);

		// if no submission
		Instructions noSub = this.uiService.newInstructions();
		noSub.setText("no-submission");
		noSub.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.none.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(answerSection).add(noSub);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.question.getPresentation().getText();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasCorrect()
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasPoints()
	{
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getInvalidMessage()
	{
		// we need text
		if (this.question.getPresentation().getText() == null) return "<ul>" + this.messages.getString("invalid") + "</ul>";

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsSurvey()
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// we need presentation set
		if (this.question.getPresentation().getText() == null) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * @return the modelAnswer (rich text)
	 */
	public String getModelAnswer()
	{
		return this.modelAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPlugin getPlugin()
	{
		return this.plugin;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		Text question = this.uiService.newText();
		question.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.presentation.text"));

		Attachments attachments = this.uiService.newAttachments();
		attachments.setAttachments(this.uiService.newPropertyReference().setReference("answer.question.presentation.attachments"), null);
		attachments.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.presentation.attachments")));

		Section questionSection = this.uiService.newSection();
		questionSection.add(question).add(attachments);

		Section answerSection = this.uiService.newSection();

		// for review mode, show just the evaluated answer text, if released
		Text answer = this.uiService.newText();
		answer.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		answer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerEvaluated"));
		answer.setIncluded(this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.submission.isReleased")), this.uiService.newHasValueDecision()
				.setProperty(this.uiService.newPropertyReference().setReference("review")), this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData")));
		answerSection.add(answer);

		// or if not released, the actual answer text
		Text answer2 = this.uiService.newText();
		answer2.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		answer2.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));
		answer2.setIncluded(this.uiService.newDecision().setReversed().setProperty(
				this.uiService.newPropertyReference().setReference("answer.submission.isReleased")), this.uiService.newHasValueDecision()
				.setProperty(this.uiService.newPropertyReference().setReference("review")), this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData")));
		answerSection.add(answer2);

		// for grading, include the editor for marking up the answer
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		edit.setSize(HtmlEdit.Sizes.tall);
		edit.setIncluded(this.uiService.newHasValueDecision().setProperty(this.uiService.newPropertyReference().setReference("grading")),
				this.uiService.newHasValueDecision().setProperty(
						this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData")));
		edit.setProperty(this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerEvaluated"));
		answerSection.add(edit);

		Attachments uploaded = this.uiService.newAttachments();
		uploaded.setAttachments(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded"), "attachment");
		uploaded.setSize(false).setTimestamp(false);
		uploaded.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded")));
		answerSection.add(uploaded);

		// if no submission
		Instructions noSub = this.uiService.newInstructions();
		noSub.setText("no-submission");
		noSub.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.none.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));
		answerSection.add(noSub);

		// model answer
		Text modelAnswerTitle = this.uiService.newText();
		modelAnswerTitle.setText("model-answer", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		Text modelAnswer = this.uiService.newText();
		modelAnswer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.modelAnswer"));

		// overlay for the model answer
		Overlay modelAnswerOverlay = this.uiService.newOverlay();
		modelAnswerOverlay.setId("modelanswer");
		modelAnswerOverlay.add(modelAnswerTitle).add(modelAnswer).add(this.uiService.newGap());
		modelAnswerOverlay.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("close").setIcon("/icons/close.png",
				Navigation.IconStyle.left));

		// control to show the model answer
		Toggle showModelAnswer = this.uiService.newToggle();
		showModelAnswer.setTarget("modelanswer");
		showModelAnswer.setTitle("view-model-answer");
		showModelAnswer.setIcon("/icons/model_answer.png", Navigation.IconStyle.left);

		Section showModelAnswerSection = this.uiService.newSection();
		showModelAnswerSection.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.modelAnswer")));
		showModelAnswerSection.add(modelAnswerOverlay).add(showModelAnswer);

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(answerSection).add(showModelAnswerSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionType getSubmissionType()
	{
		return this.submissionType;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseFeedback()
	{
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseHints()
	{
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseQuestionPresentation()
	{
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseReason()
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewAnswerUi()
	{
		// editor for marking up the answer
		HtmlEdit answer = this.uiService.newHtmlEdit();
		answer.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		answer.setSize(HtmlEdit.Sizes.tall);
		answer.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData")));
		answer.setProperty(this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerEvaluated"));

		Attachments uploaded = this.uiService.newAttachments();
		uploaded.setAttachments(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded"), "attachment");
		uploaded.setSize(false).setTimestamp(false);
		uploaded.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded")));

		return this.uiService.newFragment().setMessages(this.messages).add(answer).add(uploaded);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
		Text question = this.uiService.newText();
		question.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.presentation.text"));

		Attachments attachments = this.uiService.newAttachments();
		attachments.setAttachments(this.uiService.newPropertyReference().setReference("question.presentation.attachments"), null);
		attachments.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("question.presentation.attachments")));

		Section questionSection = this.uiService.newSection();
		questionSection.add(question).add(attachments);

		// submission type
		Selection type = uiService.newSelection();
		type.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.submissionType"));
		type.addSelection(this.uiService.newMessage().setMessage("inline"), this.uiService.newMessage().setTemplate("inline"));
		type.addSelection(this.uiService.newMessage().setMessage("inline-attachments"), this.uiService.newMessage().setTemplate("both"));
		type.addSelection(this.uiService.newMessage().setMessage("attachments"), this.uiService.newMessage().setTemplate("attachments"));
		type.setReadOnly(this.uiService.newTrueDecision());
		type.setTitle("submission", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));

		Section typeSection = this.uiService.newSection();
		typeSection.add(type);

		// model answer
		Text modelAnswerTitle = this.uiService.newText();
		modelAnswerTitle.setText("model-answer", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		Text modelAnswer = this.uiService.newText();
		modelAnswer.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));

		// overlay for the model answer
		Overlay modelAnswerOverlay = this.uiService.newOverlay();
		modelAnswerOverlay.setId("modelanswer");
		modelAnswerOverlay.add(modelAnswerTitle).add(modelAnswer).add(this.uiService.newGap());
		modelAnswerOverlay.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("close").setIcon("/icons/close.png",
				Navigation.IconStyle.left));

		// control to show the model answer
		Toggle showModelAnswer = this.uiService.newToggle();
		showModelAnswer.setTarget("modelanswer");
		showModelAnswer.setTitle("view-model-answer");
		showModelAnswer.setIcon("/icons/model_answer.png", Navigation.IconStyle.left);

		Section showModelAnswerSection = this.uiService.newSection();
		showModelAnswerSection.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer")));
		showModelAnswerSection.add(modelAnswerOverlay).add(showModelAnswer);

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(typeSection).add(showModelAnswerSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewStatsUi()
	{
		Text question = this.uiService.newText();
		question.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.presentation.text"));

		Attachments attachments = this.uiService.newAttachments();
		attachments.setAttachments(this.uiService.newPropertyReference().setReference("question.presentation.attachments"), null);
		attachments.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("question.presentation.attachments")));

		Section questionSection = this.uiService.newSection();
		questionSection.add(question).add(attachments);

		// submission type
		Selection type = uiService.newSelection();
		type.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.submissionType"));
		type.addSelection(this.uiService.newMessage().setMessage("inline"), this.uiService.newMessage().setTemplate("inline"));
		type.addSelection(this.uiService.newMessage().setMessage("inline-attachments"), this.uiService.newMessage().setTemplate("both"));
		type.addSelection(this.uiService.newMessage().setMessage("attachments"), this.uiService.newMessage().setTemplate("attachments"));
		type.setReadOnly(this.uiService.newTrueDecision());
		type.setTitle("submission", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));

		Section typeSection = this.uiService.newSection();
		typeSection.add(type);

		// model answer
		Text modelAnswerTitle = this.uiService.newText();
		modelAnswerTitle.setText("model-answer", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		Text modelAnswer = this.uiService.newText();
		modelAnswer.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));

		// overlay for the model answer
		Overlay modelAnswerOverlay = this.uiService.newOverlay();
		modelAnswerOverlay.setId("modelanswer");
		modelAnswerOverlay.add(modelAnswerTitle).add(modelAnswer).add(this.uiService.newGap());
		modelAnswerOverlay.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("close").setIcon("/icons/close.png",
				Navigation.IconStyle.left));

		// control to show the model answer
		Toggle showModelAnswer = this.uiService.newToggle();
		showModelAnswer.setTarget("modelanswer");
		showModelAnswer.setTitle("view-model-answer");
		showModelAnswer.setIcon("/icons/model_answer.png", Navigation.IconStyle.left);

		Section showModelAnswerSection = this.uiService.newSection();
		showModelAnswerSection.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer")));
		showModelAnswerSection.add(modelAnswerOverlay).add(showModelAnswer);

		Text answer = this.uiService.newText();
		answer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));

		Attachments uploaded = this.uiService.newAttachments();
		uploaded.setAttachments(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded"), "attachment");
		uploaded.setSize(false).setTimestamp(false);
		uploaded.setIncluded(this.uiService.newHasValueDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded")));

		Section section = this.uiService.newSection();
		PropertyReference iteratorRef = this.uiService.newPropertyReference().setReference("submissions").setFormatDelegate(
				this.uiService.getFormatDelegate("AccessSubmissionsQuestionAnswers", "sakai.mneme"));
		section.setIterator(iteratorRef, "answer", this.uiService.newMessage().setMessage("no-answers"));
		section.setEntityIncluded(this.uiService.newOrDecision().setOptions(
				this.uiService.newHasValueDecision().setProperty(
						this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData")),
				this.uiService.newHasValueDecision().setProperty(
						this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.uploaded"))));
		section.add(answer).add(uploaded);
		section.setTitle("answer-summary");

		Section unansweredSection = this.uiService.newSection();
		unansweredSection.setTitle("unanswered-summary");
		Text unanswered = this.uiService.newText().setText(
				null,
				this.uiService.newHtmlPropertyReference().setFormatDelegate(
						this.uiService.getFormatDelegate("FormatUnansweredPercent", "sakai.mneme")));
		unansweredSection.add(unanswered);

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(typeSection).add(showModelAnswerSection).add(section)
				.add(unansweredSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setData(String[] data)
	{
		if ((data != null) && (data.length == 2))
		{
			this.submissionType = SubmissionType.valueOf(data[0]);
			this.modelAnswer = data[1];
		}
	}

	/**
	 * Set the model answer.
	 * 
	 * @param modelAnswer
	 *        The model answer. Must be well formed HTML or plain text.
	 */
	public void setModelAnswer(String modelAnswer)
	{
		modelAnswer = StringUtil.trimToNull(modelAnswer);

		if (!Different.different(modelAnswer, this.modelAnswer)) return;

		this.modelAnswer = modelAnswer;

		this.question.setChanged();
	}

	/**
	 * Set the submission type
	 * 
	 * @param setting
	 *        The submission type.
	 */
	public void setSubmissionType(SubmissionType setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		if (!Different.different(setting, this.submissionType)) return;

		this.submissionType = setting;

		this.question.setChanged();
	}

	/**
	 * Set the UI service.
	 * 
	 * @param service
	 *        The UI service.
	 */
	public void setUi(UiService service)
	{
		this.uiService = service;
	}
}
