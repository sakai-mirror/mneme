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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.muse.ambrosia.api.AndDecision;
import org.muse.ambrosia.api.AutoColumn;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.EntityListColumn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * MultipleChoiceQuestionImpl handles questions for the multiple choice question type.
 */
public class MultipleChoiceQuestionImpl implements TypeSpecificQuestion
{
	public class MultipleChoiceQuestionChoice
	{
		protected String id;

		protected String text;

		public MultipleChoiceQuestionChoice(MultipleChoiceQuestionChoice other)
		{
			this.id = other.id;
			this.text = other.text;
		}

		public MultipleChoiceQuestionChoice(String id, String text)
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

		public void setText(String text)
		{
			this.text = text;
		}
	}

	/** List of choices */
	protected List<MultipleChoiceQuestionChoice> answerChoices = new ArrayList<MultipleChoiceQuestionChoice>();

	/** Index numbers of the correct answers */
	protected Set<Integer> correctAnswers = new HashSet<Integer>();

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** The shuffle choices setting. */
	protected Boolean shuffleChoices = Boolean.FALSE;

	/** TRUE means single correct answer, FALSE means multiple correct answers */
	protected Boolean singleCorrect = Boolean.TRUE;

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
		this.answerChoices = new ArrayList<MultipleChoiceQuestionChoice>(other.answerChoices.size());
		for (MultipleChoiceQuestionChoice choice : other.answerChoices)
		{
			this.answerChoices.add(new MultipleChoiceQuestionChoice(choice));
		}
		this.correctAnswers = new HashSet<Integer>(other.correctAnswers);
		this.messages = other.messages;
		this.question = question;
		this.shuffleChoices = other.shuffleChoices;
		this.singleCorrect = other.singleCorrect;
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

			// deep copy these
			((MultipleChoiceQuestionImpl) rv).answerChoices = new ArrayList<MultipleChoiceQuestionChoice>(this.answerChoices.size());
			for (MultipleChoiceQuestionChoice choice : this.answerChoices)
			{
				((MultipleChoiceQuestionImpl) rv).answerChoices.add(new MultipleChoiceQuestionChoice(choice));
			}

			((MultipleChoiceQuestionImpl) rv).correctAnswers = new HashSet<Integer>(this.correctAnswers);

			// set the question
			((MultipleChoiceQuestionImpl) rv).question = question;

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
		StringBuffer rv = new StringBuffer();
		// get the choices as would be presented in delivery
		List<MultipleChoiceQuestionChoice> choices = getChoices();

		// that's the A, B, C order, so find each correct one
		for (Integer correctIndex : this.correctAnswers)
		{
			int i = 0;
			for (MultipleChoiceQuestionChoice choice : choices)
			{
				if (choice.id.equals(correctIndex.toString()))
				{
					// TODO: hard coding our A, B, Cs?
					rv.append((char) ('A' + i));
					rv.append(",");
				}
				i++;
			}
		}

		if (rv.length() > 0) rv.setLength(rv.length() - 1);
		return rv.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choicesAsAuthored"), "choice");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		if (this.singleCorrect)
		{
			selCol.setSingle();
		}
		else
		{
			selCol.setMultiple();
		}
		selCol.setValueProperty(this.uiService.newPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswers"));
		entityList.addColumn(selCol);

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		EntityListColumn col = this.uiService.newEntityListColumn();
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 50);
		edit.setProperty(this.uiService.newPropertyReference().setReference("choice.text"));
		col.add(edit);
		entityList.addColumn(col);

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("answer");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("shuffle");
		Selection selection = uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.shuffleChoices"));
		row.add(selection);
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * Access the choices as an entity (MultipleChoiceQuestionChoice) list.
	 * 
	 * @return The choices as an entity (MultipleChoiceQuestionChoice) list.
	 */
	public List<MultipleChoiceQuestionChoice> getChoices()
	{
		// get the list in order
		List<MultipleChoiceQuestionChoice> rv = getChoicesAsAuthored();

		// shuffle them if desired (and we are in a submission context)
		if (this.shuffleChoices && (this.question.getPart().getAssessment().getSubmissionContext() != null))
		{
			// set the seed based on the submissionid and the question id
			long seed = (this.question.getPart().getAssessment().getSubmissionContext().getId() + this.question.getId()).hashCode();

			// mix up the answers
			Collections.shuffle(rv, new Random(seed));
		}

		return rv;
	}

	/**
	 * Access the actual choices as authored.  The choices in  the list can be altered, changing the question definition.
	 * 
	 * @return The choices as authored.
	 */
	public List<MultipleChoiceQuestionChoice> getChoicesAsAuthored()
	{
		return this.answerChoices;
	}

	/**
	 * Access the correct answers as an array.
	 * 
	 * @return The correct answers.
	 */
	public String[] getCorrectAnswers()
	{
		String[] rv = new String[this.correctAnswers.size()];
		int i = 0;
		for (Integer correct : this.correctAnswers)
		{
			rv[i++] = correct.toString();
		}

		return rv;
	}

	/**
	 * Access the correct answers as a set.
	 * 
	 * @return The correct answers.
	 */
	public Set getCorrectAnswerSet()
	{
		return this.correctAnswers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		if (this.singleCorrect)
		{
			selCol.setSingle();
		}
		else
		{
			selCol.setMultiple();
		}
		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		entityList.addColumn(selCol);

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		if (this.singleCorrect)
		{
			selCol.setSingle();
		}
		else
		{
			selCol.setMultiple();
		}
		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		selCol.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswers"));

		AndDecision and = this.uiService.newAndDecision();
		Decision[] decisions = new Decision[2];
		decisions[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
		decisions[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		and.setRequirements(decisions);
		selCol.setCorrectDecision(and);

		entityList.addColumn(selCol);

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
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
	public String getSingleCorrect()
	{
		return this.singleCorrect.toString();
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
		// TODO: just the selected answer, no distractors, and add correct/incorrect marking
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		if (this.singleCorrect)
		{
			selCol.setSingle();
		}
		else
		{
			selCol.setMultiple();
		}
		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		selCol.setCorrect(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.correctAnswers"));
		entityList.addColumn(selCol);

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
		// TODO: add correct/incorrect marking
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choicesAsAuthored"), "choice");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		if (this.singleCorrect)
		{
			selCol.setSingle();
		}
		else
		{
			selCol.setMultiple();
		}
		selCol.setValueProperty(this.uiService.newPropertyReference().setReference("choice.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswers"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		selCol.setCorrect(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.correctAnswers"));
		entityList.addColumn(selCol);

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * Set the entire set of choices to these values.
	 * 
	 * @param choices
	 *        The choice values.
	 */
	public void setAnswerChoices(List<String> choices)
	{
		this.answerChoices = new ArrayList<MultipleChoiceQuestionChoice>(choices.size());
		int i = 0;
		for (String choice : choices)
		{
			this.answerChoices.add(new MultipleChoiceQuestionChoice(Integer.toString(i++), choice));
		}
	}

	/**
	 * Sets the correct answers.
	 * 
	 * @param correctAnswers
	 *        The correct answers.
	 */
	public void setCorrectAnswers(String[] correctAnswers)
	{
		this.correctAnswers.clear();
		if (correctAnswers == null) return;
		for (String answer : correctAnswers)
		{
			this.correctAnswers.add(Integer.valueOf(answer));
		}
	}

	/**
	 * Sets the correct answers as a set.
	 * 
	 * @param correctAnswers
	 *        The correct answers.
	 */
	public void setCorrectAnswerSet(Set<Integer> answers)
	{
		this.correctAnswers.clear();
		if (answers == null) return;
		this.correctAnswers.addAll(answers);
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
	 * {@inheritDoc}
	 */
	public void setSingleCorrect(String singleCorrect)
	{
		this.singleCorrect = Boolean.valueOf(singleCorrect);
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
