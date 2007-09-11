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
import org.muse.mneme.api.MultipleAnswerType;
import org.sakaiproject.i18n.InternationalizedMessages;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * MultipleChoiceQuestionImpl handles questions for the true/false question type.
 */
public class MultipleChoiceQuestionImpl implements TypeSpecificQuestion
{
	/** The correct answer is one of the values */
	protected String correctAnswer;

	/** Value Selector pairs. The key is the value, the value is the selector */
	protected Map<String, String> valueSelectors = new HashMap<String, String>();

	protected InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Single or multiple choice * */
	protected MultipleAnswerType answerType = MultipleAnswerType.single;

	/** The correct answer: TRUE or FALSE. */
	protected Boolean shuffleChoices = Boolean.FALSE;

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
	public MultipleChoiceQuestionImpl(InternationalizedMessages messages, UiService uiService, Question question)
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
	public MultipleChoiceQuestionImpl(Question question, MultipleChoiceQuestionImpl other)
	{
		this.question = question;
		this.correctAnswer = other.correctAnswer;
		this.answerType = other.answerType;
		this.shuffleChoices = other.shuffleChoices;
		this.messages = other.messages;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone()
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = (MultipleChoiceQuestionImpl) super.clone();

			// nothing to deep copy

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
		return this.valueSelectors.get(this.correctAnswer);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		if (this.answerType.equals(MultipleAnswerType.single))
		{
			Selection selection = this.uiService.newSelection();
			selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswer"));
			for (Iterator iKeys = valueSelectors.entrySet().iterator(); iKeys.hasNext();)
			{
				Map.Entry e = (Map.Entry) iKeys.next();
				selection.addSelection((String) e.getValue(), (String) e.getKey());
			}
			EntityDisplayRow row = this.uiService.newEntityDisplayRow();
			row.setTitle("correct-answer");
			row.add(selection);

			EntityDisplay display = this.uiService.newEntityDisplay();
			display.addRow(row);

			return this.uiService.newFragment().setMessages(this.messages).add(display);
		}
		else
			return null;
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
		if (this.answerType.equals(MultipleAnswerType.single))
		{
			Selection selection = this.uiService.newSelection();
			selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
			for (Iterator iKeys = valueSelectors.entrySet().iterator(); iKeys.hasNext();)
			{
				Map.Entry e = (Map.Entry) iKeys.next();
				selection.addSelection((String) e.getValue(), (String) e.getKey());
			}
			return this.uiService.newFragment().setMessages(this.messages).add(selection);
		}
		else
			return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		if (this.answerType.equals(MultipleAnswerType.single))
		{
			Selection selection = this.uiService.newSelection();
			selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
			for (Iterator iKeys = valueSelectors.entrySet().iterator(); iKeys.hasNext();)
			{
				Map.Entry e = (Map.Entry) iKeys.next();
				selection.addSelection((String) e.getValue(), (String) e.getKey());
			}

			selection.setReadOnly(this.uiService.newDecision().setProperty(this.uiService.newConstantPropertyReference().setValue("true")));

			return this.uiService.newFragment().setMessages(this.messages).add(selection);
		}
		else
			return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTypeName()
	{
		return this.messages.getString("name");
	}

	/**
	 * Set the correct answer, as a Boolean string.
	 * 
	 * @param correctAnswer
	 *        The correct answer.
	 */
	public void setCorrectAnswer(String correctAnswer)
	{
		this.correctAnswer = correctAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public MultipleAnswerType getAnswerType()
	{
		return this.answerType;
	}

	/**
	 * Access the shuffle choice as a string.
	 * 
	 * @return The shuffle choice.
	 */
	public String getShuffleChoices()
	{
		return this.shuffleChoices.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnswerType(MultipleAnswerType answerType)
	{
		if (answerType == null) throw new IllegalArgumentException();
		this.answerType = answerType;
	}

	public void registerValueSelector(String value, String selector)
	{
		this.valueSelectors.put(value, selector);
	}

	/**
	 * Set the shuffle choice, as a Boolean string.
	 * 
	 * @param shuffleChoices
	 *        The shuffle choice.
	 */
	public void setShuffleChoices(String shuffleChoices)
	{
		this.shuffleChoices = Boolean.valueOf(shuffleChoices);
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
