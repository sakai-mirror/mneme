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
import org.muse.ambrosia.api.OrDecision;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.Destination;
import org.muse.ambrosia.api.OrderColumn;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.StringUtil;

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
	protected List<MatchQuestionChoice> answerChoices = new ArrayList<MatchQuestionChoice>();

	/** Index numbers of the correct answers */
	protected Set<Integer> correctAnswers = new HashSet<Integer>();

	/** String that holds the distractor choice */
	protected String distractor;

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

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
		this.answerChoices = new ArrayList<MatchQuestionChoice>(other.answerChoices.size());
		for (MatchQuestionChoice choice : other.answerChoices)
		{
			this.answerChoices.add(new MatchQuestionChoice(choice));
		}
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
			((MatchQuestionImpl) rv).answerChoices = new ArrayList<MatchQuestionChoice>(this.answerChoices.size());
			for (MatchQuestionChoice choice : this.answerChoices)
			{
				((MatchQuestionImpl) rv).answerChoices.add(new MatchQuestionChoice(choice));
			}
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
	public String consolidate(String destination)
	{
		boolean stayHere = false;
		// check for delete
		if (destination.startsWith("DEL:"))
		{
			stayHere = true;
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length == 2)
			{
				List newChoices = new ArrayList<MatchQuestionChoice>();
				int i = 0;
				for (MatchQuestionChoice choice : this.answerChoices)
				{
					// ignore the deleted one
					if (!choice.getId().equals(parts[1]))
					{
						// new position
						choice.id = Integer.toString(i++);
						newChoices.add(choice);
					}
				}

				this.answerChoices = newChoices;
			}
		}
		// add more choices
		if (destination.startsWith("ADD:"))
		{
			stayHere = true;
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length == 2)
			{
				try
				{
					int more = Integer.parseInt(parts[1]);
					int i = this.answerChoices.size();
					for (int count = 0; count < more; count++)
					{
						this.answerChoices.add(new MatchQuestionChoice(Integer.toString(i++), "", ""));
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}

		if (stayHere) return null;
		return destination;
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
		Destination destination = this.uiService.newDestination();
		destination.setDestination("DEL:{0}", this.uiService.newPropertyReference().setReference("choice.id"));
		nav.setTitle("delete").setIcon("/icons/delete.png", Navigation.IconStyle.left).setStyle(Navigation.Style.link).setSubmit().setDestination(
				destination);
		col.add(nav);
		entityList.addColumn(col);

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("choices", this.uiService.newIconPropertyReference().setIcon("/icons/answer_key2.png"));
		row.add(entityList);

		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("distractor", this.uiService.newIconPropertyReference().setIcon("/icons/distractor_add.png"));
		edit = this.uiService.newHtmlEdit();
		edit.setTitle("distractor-description");
		edit.setSize(5, 50);
		edit.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.distractor"));
		row.add(edit);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		Selection selection = uiService.newSelection();
		selection.addSelection("none", "ADD:0");
		selection.addSelection("one", "ADD:1");
		selection.addSelection("two", "ADD:2");
		selection.addSelection("three", "ADD:3");
		selection.addSelection("four", "ADD:4");
		selection.addSelection("five", "ADD:5");
		selection.setOrientation(Selection.Orientation.dropdown);
		selection.setSubmitValue();
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
			consolidate("ADD:4");
		}
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

		Text answerKey = this.uiService.newText();
		PropertyReference[] refs = new PropertyReference[2];
		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
		refs[1] = this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.answerKey");
		answerKey.setText("answer-key", refs);

		// AndDecision and = this.uiService.newAndDecision();
		// Decision[] decisions = new Decision[2];
		// decisions[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
		// decisions[1] = this.uiService.newDecision().setProperty(
		// this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		// and.setRequirements(decisions);
		//
		// OrDecision or = this.uiService.newOrDecision();
		// Decision[] decisionsOr = new Decision[2];
		// decisionsOr[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
		// decisionsOr[1] = and;
		// or.setOptions(decisionsOr);
		//
		// ???.setCorrectDecision(or);

		Decision[] orInc = new Decision[2];
		orInc[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
		orInc[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		answerKey.setIncluded(this.uiService.newOrDecision().setOptions(orInc));

		AutoColumn autoCol = this.uiService.newAutoColumn();
		entityList.addColumn(autoCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList).add(answerKey);
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

		Text answerKey = this.uiService.newText();
		PropertyReference[] refs = new PropertyReference[2];
		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
		refs[1] = this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.answerKey");
		answerKey.setText("answer-key", refs);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList).add(answerKey);
	}

	public void setAnswerChoices(List<String> choices)
	{
		this.answerChoices = new ArrayList<MatchQuestionChoice>(choices.size());
		int i = 0;
		for (String choice : choices)
		{
			this.answerChoices.add(new MatchQuestionChoice(Integer.toString(i++), choice, choice));
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

	public void setDistractor(String distractor)
	{
		this.distractor = distractor;
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
