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
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * MultipleChoiceQuestionImpl handles questions for the multiple choice question type.
 */
public class MultipleChoiceQuestionImpl implements TypeSpecificQuestion
{
	private class MultipleChoiceQuestionChoice
	{
		String id;

		String text;

		public String getId()
		{
			return this.id;
		}

		public String getText()
		{
			return this.text;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public void setText(String text)
		{
			this.text = text;
		}

		public void MultipleChoiceQuestionChoice(String id, String text)
		{
			this.id = id;
			this.text = text;
		}
	}

	/** This hash set holds index numbers of the correct answers */
	protected Set<Integer> correctAnswers = new HashSet<Integer>();

	/** List of choices */
	protected List<String> answerChoices = new ArrayList<String>();

	protected InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** TRUE means single correct answer, FALSE means multiple correct answers */
	protected Boolean singleCorrect = Boolean.TRUE;

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
		this.shuffleChoices = other.shuffleChoices;
		this.messages = other.messages;
		this.singleCorrect = other.singleCorrect;
		this.answerChoices = other.answerChoices;
		this.correctAnswers = other.correctAnswers;
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

	public List<MultipleChoiceQuestionChoice> getChoices()
	{
		List<MultipleChoiceQuestionChoice> rv = new ArrayList<MultipleChoiceQuestionChoice>();
		for (String choice : this.answerChoices)
		{
			// rv.add(new MultipleChoiceQuestionChoice(String.valueOf(this.answerChoices.indexOf(choice)), choice));
			MultipleChoiceQuestionChoice mcqcObj = new MultipleChoiceQuestionChoice();
			mcqcObj.setId(String.valueOf(this.answerChoices.indexOf(choice)));
			mcqcObj.setText(choice);
			rv.add(mcqcObj);
		}

		if (rv.isEmpty()) return null;

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKey()
	{
		// return this.valueSelectors.get(this.correctAnswers);
		/*
		 * Iterator itr = correctAnswers.iterator(); String answerKey[] = new String[correctAnswers.size()]; int i=0; while (itr.hasNext()) {
		 * answerKey[i] = (String)this.answerChoices.get((String)itr.next().intValue()) ; i++; } return answerKey;
		 */
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		if (this.singleCorrect)
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setSingle().setValueProperty(
							this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
							this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswers"))).addColumn(
					this.uiService.newEntityListColumn().add(
							this.uiService.newTextEdit().setSize(5, 50)
									.setProperty(this.uiService.newPropertyReference().setReference("choice.text"))));
		}
		else
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setMultiple().setValueProperty(
							this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
							this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswers"))).addColumn(
					this.uiService.newEntityListColumn().add(
							this.uiService.newTextEdit().setSize(10, 50).setProperty(
									this.uiService.newPropertyReference().setReference("choice.text"))));
		}

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("correct-answer");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * Access the correct answers as a set.
	 * 
	 * @return The correct answers.
	 */
	public Set getCorrectAnswers()
	{
		return this.correctAnswers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		if (this.singleCorrect)
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setSingle().setValueProperty(
							this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
							this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"))).addColumn(
					this.uiService.newPropertyColumn().setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text")));
		}
		else
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setMultiple().setValueProperty(
							this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
							this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"))).addColumn(
					this.uiService.newPropertyColumn().setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text")));
		}
		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("correct-answer");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		// Need to check with Glenn on how to set readonly
		EntityList entityList = this.uiService.newEntityList();
		if (this.singleCorrect)
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setSingle().setReadOnly(
							this.uiService.newDecision().setProperty(this.uiService.newConstantPropertyReference().setValue("true")))
							.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
									this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"))).addColumn(
					this.uiService.newPropertyColumn().setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text")));
		}
		else
		{
			entityList.setStyle(EntityList.Style.form).setIterator(
					this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice").addColumn(
					this.uiService.newSelectionColumn().setSingle().setReadOnly(
							this.uiService.newDecision().setProperty(this.uiService.newConstantPropertyReference().setValue("true")))
							.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id")).setProperty(
									this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"))).addColumn(
					this.uiService.newPropertyColumn().setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text")));
		}
		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("correct-answer");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
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
	public String getSingleCorrect()
	{
		return this.singleCorrect.toString();
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

	public void setAnswerChoices(List answerChoices)
	{
		this.answerChoices = answerChoices;
	}

	/**
	 * Sets the correct answers.
	 * 
	 * @param correctAnswers
	 *        The correct answers.
	 */
	public void setCorrectAnswers(Set correctAnswers)
	{
		this.correctAnswers = correctAnswers;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSingleCorrect(String singleCorrect)
	{
		this.singleCorrect = Boolean.valueOf(singleCorrect);
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
