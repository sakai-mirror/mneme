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

import java.util.ArrayList;
import java.util.List;

import org.muse.ambrosia.api.AndDecision;
import org.muse.ambrosia.api.CompareDecision;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.muse.mneme.impl.MultipleChoiceQuestionImpl.MultipleChoiceQuestionChoice;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * TrueFalseQuestionImpl handles questions for the true/false question type.
 */
public class TrueFalseQuestionImpl implements TypeSpecificQuestion
{
	public class TrueFalseQuestionChoice
	{
		protected String id;

		protected String text;

		public TrueFalseQuestionChoice(MultipleChoiceQuestionChoice other)
		{
			this.id = other.id;
			this.text = other.text;
		}

		public TrueFalseQuestionChoice(String id, String text)
		{
			this.id = id;
			this.text = text;
		}

		public String getId()
		{
			return this.id;
		}

		public String getText()
		{
			return this.text;
		}
	}

	/** The correct answer: TRUE or FALSE. */
	protected Boolean correctAnswer = Boolean.TRUE;

	protected transient InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Dependency: The UI service (Ambrosia). */
	protected transient UiService uiService = null;

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
		this.correctAnswer = other.correctAnswer;
		this.messages = other.messages;
		this.question = question;
		this.uiService = other.uiService;
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
	public void consolidate()
	{
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
	 * Access the choices as an entity (TrueFalseQuestionChoice) list.
	 * 
	 * @return The choices as an entity (TrueFalseQuestionChoice) list.
	 */
	public List<TrueFalseQuestionChoice> getChoices()
	{
		// get the list in order
		List<TrueFalseQuestionChoice> rv = new ArrayList<TrueFalseQuestionChoice>(2);
		rv.add(new TrueFalseQuestionChoice("true", this.messages.getString("true")));
		rv.add(new TrueFalseQuestionChoice("false", this.messages.getString("false")));

		return rv;
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
	public String getDescription()
	{
		return this.question.getPresentation().getText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelAnswer()
	{
		return null;
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

		AndDecision and = this.uiService.newAndDecision();
		Decision[] decisions = new Decision[2];
		decisions[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
		decisions[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		and.setRequirements(decisions);
		selection.setCorrectDecision(and);

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
	public Boolean getUseQuestionPresentation()
	{
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewAnswerUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");
		entityList.setEmptyTitle("no-answers");

		// include each choice only if the choice has been selected by the user
		PropertyReference entityIncludedProperty = this.uiService.newPropertyReference().setReference("choice.id");
		PropertyReference entityIncludedComparison = this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer");
		CompareDecision entityIncludedDecision = this.uiService.newCompareDecision();
		entityIncludedDecision.setProperty(entityIncludedProperty);
		entityIncludedDecision.setEqualsProperty(entityIncludedComparison);
		entityList.setEntityIncluded(entityIncludedDecision);

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		selCol.setSingle();
		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		selCol.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswer"));
		entityList.addColumn(selCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);

		// // TODO: just the selected answer, no distractors
		// Selection selection = this.uiService.newSelection();
		// selection.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		// selection.addSelection("true", "true");
		// selection.addSelection("false", "false");
		// selection.setReadOnly(this.uiService.newTrueDecision());
		// selection.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswer"));
		//
		// return this.uiService.newFragment().setMessages(this.messages).add(selection);
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
		selection.setCorrect(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswer"));

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
