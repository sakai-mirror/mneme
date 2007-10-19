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
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.OrderColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * MatchQuestionImpl handles questions for the match question type.
 */
public class MatchQuestionImpl implements TypeSpecificQuestion
{
	public class MatchQuestionChoice
	{
		protected String answer;

		protected Boolean deleted = Boolean.FALSE;

		protected String id;

		protected String text;

		public MatchQuestionChoice(MatchQuestionChoice other)
		{
			this.deleted = other.deleted;
			this.id = other.id;
			this.text = other.text;
			this.answer = other.answer;
		}

		public MatchQuestionChoice(String id, String text, String answer)
		{
			this.id = id;
			this.text = text;
			this.answer = answer;
		}

		public String getAnswer()
		{
			return this.answer;
		}

		public Boolean getDeleted()
		{
			return this.deleted;
		}

		public String getId()
		{
			return this.id;
		}

		public String getText()
		{
			return this.text;
		}

		public void setAnswer(String answer)
		{
			this.answer = answer;
		}

		public void setDeleted(Boolean deleted)
		{
			this.deleted = deleted;
		}

		public void setText(String text)
		{
			this.text = text;
		}
	}

	/** List of choices */
	protected List<String> answerChoices = new ArrayList<String>();

	/** Index numbers of the correct answers */
	protected Set<Integer> correctAnswers = new HashSet<Integer>();

	/** String that holds the distractor choice */
	protected String distractor;

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** A request for more choices. */
	protected transient Integer moreChoices = null;

	protected transient QuestionPlugin plugin = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Dependency: The UI service (Ambrosia). */
	protected transient UiService uiService = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public MatchQuestionImpl(Question question, MatchQuestionImpl other)
	{
		this.answerChoices = new ArrayList<String>(other.answerChoices);
		this.correctAnswers = new HashSet<Integer>(other.correctAnswers);
		this.messages = other.messages;
		this.question = question;
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
	public MatchQuestionImpl(QuestionPlugin plugin, InternationalizedMessages messages, UiService uiService, Question question)
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

			// deep copy these
			((MatchQuestionImpl) rv).answerChoices = new ArrayList<String>(this.answerChoices);
			((MatchQuestionImpl) rv).correctAnswers = new HashSet<Integer>(this.correctAnswers);

			// set the question
			((MatchQuestionImpl) rv).question = question;

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
		StringBuffer rv = new StringBuffer();
		// get the choices as would be presented in delivery
		List<MatchQuestionChoice> choices = getChoices();

		// that's the A, B, C order, so find each correct one
		for (Integer correctIndex : this.correctAnswers)
		{
			int i = 0;
			for (MatchQuestionChoice choice : choices)
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
		EntityDisplay display = this.uiService.newEntityDisplay();

		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choices"), "choice");

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		EntityListColumn col = this.uiService.newEntityListColumn();
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 25);
		edit.setProperty(this.uiService.newPropertyReference().setReference("choice.text"));
		col.setTitle("choice");
		col.add(edit);
		entityList.addColumn(col);

		col = this.uiService.newEntityListColumn();
		edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 25);
		edit.setProperty(this.uiService.newPropertyReference().setReference("choice.answer"));
		col.setTitle("match");
		col.add(edit);
		entityList.addColumn(col);

		col = this.uiService.newEntityListColumn();
		Navigation nav = this.uiService.newNavigation();
		nav.setTitle("delete").setIcon("/icons/delete.png", Navigation.IconStyle.left).setStyle(Navigation.Style.link).setSubmit().setDestination(
				null);
		col.add(nav);
		entityList.addColumn(col);

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("choices");
		row.add(entityList);

		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("distractor");
		edit = this.uiService.newHtmlEdit();
		edit.setTitle("distractor-description");
		edit.setSize(5, 50);
		edit.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.distractor"));
		row.add(edit);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		Selection selection = uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.moreChoices"));
		selection.addSelection("none", "0");
		selection.addSelection("one", "1");
		selection.addSelection("two", "2");
		selection.addSelection("three", "3");
		selection.addSelection("four", "4");
		selection.addSelection("five", "5");
		selection.setOrientation(Selection.Orientation.dropdown);
		row.add(selection);
		row.setTitle("more-choices");
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * Access the choices as an entity (MatchQuestionChoice) list in as-authored order.
	 * 
	 * @return The choices as an entity (MatchQuestionChoice) list in as-authored order.
	 */
	public List<MatchQuestionChoice> getChoices()
	{
		if (this.answerChoices.size() == 0)
		{
			answerChoices.add("");
			answerChoices.add("");
			answerChoices.add("");
			answerChoices.add("");
		}
		List<MatchQuestionChoice> rv = new ArrayList<MatchQuestionChoice>(this.answerChoices.size());

		for (String choice : this.answerChoices)
		{
			rv.add(new MatchQuestionChoice(String.valueOf(this.answerChoices.indexOf(choice)), choice, choice));
		}

		return rv;
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
	public String getDescription()
	{
		return this.question.getPresentation().getText();
	}

	public String getDistractor()
	{
		return this.distractor;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelAnswer()
	{
		return null;
	}

	/**
	 * The "getter" for the moreChoices - always set to 0.
	 * 
	 * @return The initial '0" value for the more choices.
	 */
	public String getMoreChoices()
	{
		return "0";
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
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");

		/*
		 * AndDecision and = this.uiService.newAndDecision(); Decision[] decisions = new Decision[2]; decisions[0] =
		 * this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview")); decisions[1] =
		 * this.uiService.newDecision().setProperty(
		 * this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		 * and.setRequirements(decisions); selCol.setCorrectDecision(and); entityList.addColumn(selCol);
		 */

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
		// we suppress the question presentation, using our own fields to capture the question.
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
		// TODO: just the selected answer, no distractors, and add correct/incorrect marking
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");

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

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
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

	public void setDistractor(String distractor)
	{
		this.distractor = distractor;
	}

	/**
	 * Set a request for more choices.
	 * 
	 * @param more
	 *        The number of more choices requested.
	 */
	public void setMoreChoices(String more)
	{
		// defer to consolidate
		this.moreChoices = Integer.valueOf(more);
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
