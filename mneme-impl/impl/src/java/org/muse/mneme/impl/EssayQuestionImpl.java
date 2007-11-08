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

package org.muse.mneme.impl;

import org.muse.ambrosia.api.Attachments;
import org.muse.ambrosia.api.AttachmentsEdit;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.Overlay;
import org.muse.ambrosia.api.Section;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.Toggle;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

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
		type.addSelection("inline", "inline");
		type.addSelection("inline-attachments", "both");
		type.addSelection("attachments", "attachments");
		type.setTitle("submission", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));

		Section typeSection = this.uiService.newSection();
		typeSection.add(type);

		// model answer
		HtmlEdit modelAnswer = this.uiService.newHtmlEdit();
		modelAnswer.setSize(5, 50);
		modelAnswer.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));
		modelAnswer.setTitle("model-answer-edit", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		Section modelAnswerSection = this.uiService.newSection();
		modelAnswerSection.add(modelAnswer);

		return this.uiService.newFragment().setMessages(this.messages).add(typeSection).add(modelAnswerSection);
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
		edit.setSize(5, 50);
		edit.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.inline.toString(), SubmissionType.both.toString())
				.setProperty(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));
		edit.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));
		answerSection.add(edit);

		// the upload
		// TODO: this should be upload, not attachments...
		AttachmentsEdit upload = this.uiService.newAttachmentsEdit();
		upload.setIncluded(this.uiService.newCompareDecision().setEqualsConstant(SubmissionType.attachments.toString(),
				SubmissionType.both.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.submissionType")));
		answerSection.add(upload);

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(answerSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.question.getPresentation().getText();
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

		Text answer = this.uiService.newText();
		answer.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));
		answer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));
		answerSection.add(answer);

		// TODO: add to the answerSection the uploaded links

		// model answer
		Text modelAnswer = this.uiService.newText();
		modelAnswer.setTitle("model-answer", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));
		modelAnswer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.modelAnswer"));

		// section for the model answer
		Section modelAnswerSection = this.uiService.newSection();
		modelAnswerSection.add(modelAnswer);

		// overlay for the model answer
		Overlay modelAnswerOverlay = this.uiService.newOverlay();
		modelAnswerOverlay.setId("modelanswer");
		modelAnswerOverlay.add(modelAnswerSection);
		modelAnswerOverlay.add(this.uiService.newGap());
		modelAnswerOverlay.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("hide-model-answer"));

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
	public String getTypeName()
	{
		return this.messages.getString("name");
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
		Text answer = this.uiService.newText();
		answer.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));

		return this.uiService.newFragment().setMessages(this.messages).add(answer);
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
		type.addSelection("inline", "inline");
		type.addSelection("inline-attachments", "both");
		type.addSelection("attachments", "attachments");
		type.setReadOnly(this.uiService.newTrueDecision());
		type.setTitle("submission", this.uiService.newIconPropertyReference().setIcon("/icons/answer.png"));

		Section typeSection = this.uiService.newSection();
		typeSection.add(type);

		// model answer
		Text modelAnswer = this.uiService.newText();
		modelAnswer.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));
		modelAnswer.setTitle("model-answer", this.uiService.newIconPropertyReference().setIcon("/icons/model_answer.png"));

		// section for the model answer
		Section modelAnswerSection = this.uiService.newSection();
		modelAnswerSection.add(modelAnswer);

		// overlay for the model answer
		Overlay modelAnswerOverlay = this.uiService.newOverlay();
		modelAnswerOverlay.setId("modelanswer");
		modelAnswerOverlay.add(modelAnswerSection);
		modelAnswerOverlay.add(this.uiService.newGap());
		modelAnswerOverlay.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("hide-model-answer"));

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

	public void setModelAnswer(String modelAnswer)
	{
		this.modelAnswer = modelAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionType(SubmissionType setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.submissionType = setting;
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
