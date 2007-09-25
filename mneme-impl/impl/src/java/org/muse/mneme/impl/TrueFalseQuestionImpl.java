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

import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * TrueFalseQuestionImpl handles questions for the true/false question type.
 */
public class TrueFalseQuestionImpl implements TypeSpecificQuestion
{
	/** The correct answer: TRUE or FALSE. */
	protected Boolean correctAnswer = Boolean.TRUE;

	protected InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Dependency: The UI service (Ambrosia). */
	protected UiService uiService = null;

	/**
	 * Construct.
	 * 
	 * @param uiService
	 *        the UiService.
	 * @param question
	 *        The Question this is a helper for.
	 */
	public TrueFalseQuestionImpl(InternationalizedMessages messages, UiService uiService, Question question)
	{
		this.messages = messages;
		this.uiService = uiService;
		this.question = question;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public TrueFalseQuestionImpl(Question question, TrueFalseQuestionImpl other)
	{
		this.question = question;
		this.correctAnswer = other.correctAnswer;
		this.messages = other.messages;
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

			// nothing to deep copy

			// set the question
			((TrueFalseQuestionImpl) rv).question = question;

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
	public String getAnswerKey()
	{
		return this.correctAnswer ? this.messages.getString("true") : this.messages.getString("false");
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		Selection selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswer"));
		selection.addSelection("true", "true");
		selection.addSelection("false", "false");

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("correct-answer");
		row.add(selection);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * Access the correct answer as a string.
	 * 
	 * @return The correct answer.
	 */
	public String getCorrectAnswer()
	{
		return this.correctAnswer.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		Selection selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selection.addSelection("true", "true");
		selection.addSelection("false", "false");

		return this.uiService.newFragment().setMessages(this.messages).add(selection);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		Selection selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selection.addSelection("true", "true");
		selection.addSelection("false", "false");
		selection.setReadOnly(this.uiService.newTrueDecision());
		selection.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswer"));

		return this.uiService.newFragment().setMessages(this.messages).add(selection);
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
	public Component getViewAnswerUi()
	{
		// TODO: just the selected answer, no distractors
		Selection selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selection.addSelection("true", "true");
		selection.addSelection("false", "false");
		selection.setReadOnly(this.uiService.newTrueDecision());
		selection.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswer"));

		return this.uiService.newFragment().setMessages(this.messages).add(selection);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
		Selection selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswer"));
		selection.addSelection("true", "true");
		selection.addSelection("false", "false");
		selection.setReadOnly(this.uiService.newTrueDecision());
		selection.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswer"));

		return this.uiService.newFragment().setMessages(this.messages).add(selection);
	}

	/**
	 * Set the correct answer, as a Boolean string.
	 * 
	 * @param correctAnswer
	 *        The correct answer.
	 */
	public void setCorrectAnswer(String correctAnswer)
	{
		this.correctAnswer = Boolean.valueOf(correctAnswer);
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
