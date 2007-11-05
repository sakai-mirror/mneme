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

import org.muse.ambrosia.api.AutoColumn;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.Destination;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.EntityListColumn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Section;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.util.StringUtil;

/**
 * MatchQuestionImpl handles questions for the match question type.
 */
public class MatchQuestionImpl implements TypeSpecificQuestion
{
	public class MatchQuestionPair
	{
		protected String choice = null;

		/** Identifies the choice, different id than that used for the match. */
		protected String choiceId = null;

		/** Identifies the pair, also identifies the match. */
		protected String id = null;

		protected String match = null;

		public MatchQuestionPair(MatchQuestionPair other)
		{
			setChoice(other.choice);
			this.choiceId = other.choiceId;
			this.id = other.id;
			setMatch(other.match);
		}

		public MatchQuestionPair(String choice, String match)
		{
			setChoice(choice);
			this.choiceId = idManager.createUuid();
			this.id = idManager.createUuid();
			setMatch(match);
		}

		public String getChoice()
		{
			return this.choice;
		}

		public String getChoiceId()
		{
			return this.choiceId;
		}

		public String getId()
		{
			return this.id;
		}

		public String getMatch()
		{
			return this.match;
		}

		public void setChoice(String choice)
		{
			this.choice = StringUtil.trimToNull(choice);
		}

		public void setMatch(String match)
		{
			this.match = StringUtil.trimToNull(match);
		}
	}

	/** Index numbers of the correct answers */
	// protected Set<Integer> correctAnswers = new HashSet<Integer>();
	/** String that holds the distractor choice */
	protected MatchQuestionPair distractor = null;

	/** Dependency: IdManager. */
	protected IdManager idManager = null;

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** List of choices */
	protected List<MatchQuestionPair> pairs = new ArrayList<MatchQuestionPair>();

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
		if (other.distractor != null) this.distractor = new MatchQuestionPair(other.distractor);
		this.pairs = new ArrayList<MatchQuestionPair>(other.pairs.size());
		for (MatchQuestionPair choice : other.pairs)
		{
			this.pairs.add(new MatchQuestionPair(choice));
		}
		// this.correctAnswers = new HashSet<Integer>(other.correctAnswers);
		this.messages = other.messages;
		this.question = question;
		this.uiService = other.uiService;
		this.idManager = other.idManager;
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
	public MatchQuestionImpl(QuestionPlugin plugin, InternationalizedMessages messages, UiService uiService, IdManager idManager, Question question)
	{
		this.idManager = idManager;
		this.plugin = plugin;
		this.messages = messages;
		this.uiService = uiService;
		this.question = question;
	}

	/**
	 * Add a pair of choice - match.
	 * 
	 * @param choice
	 *        The Choice.
	 * @param match
	 *        The Match.
	 */
	public void addPair(String choice, String match)
	{
		this.pairs.add(new MatchQuestionPair(choice, match));
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
			((MatchQuestionImpl) rv).pairs = new ArrayList<MatchQuestionPair>(this.pairs.size());
			for (MatchQuestionPair choice : this.pairs)
			{
				((MatchQuestionImpl) rv).pairs.add(new MatchQuestionPair(choice));
			}
			// ((MatchQuestionImpl) rv).correctAnswers = new HashSet<Integer>(this.correctAnswers);
			if (this.distractor != null) ((MatchQuestionImpl) rv).distractor = new MatchQuestionPair(this.distractor);

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
		boolean removeBlanks = true;

		// check for delete
		if (destination.startsWith("DEL:"))
		{
			stayHere = true;
			removeBlanks = false;

			String[] parts = StringUtil.split(destination, ":");
			if (parts.length == 2)
			{
				List newChoices = new ArrayList<MatchQuestionPair>();
				for (MatchQuestionPair pair : this.pairs)
				{
					// ignore the deleted one
					if (!pair.getId().equals(parts[1]))
					{
						newChoices.add(pair);
					}
				}

				this.pairs = newChoices;
			}
		}

		// add more choices
		if (destination.startsWith("ADD:"))
		{
			stayHere = true;
			removeBlanks = false;

			String[] parts = StringUtil.split(destination, ":");
			if (parts.length == 2)
			{
				try
				{
					int more = Integer.parseInt(parts[1]);
					int i = this.pairs.size();
					for (int count = 0; count < more; count++)
					{
						this.pairs.add(new MatchQuestionPair("", ""));
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}

		// remove blank pairs
		if (removeBlanks)
		{
			List newChoices = new ArrayList<MatchQuestionPair>();
			for (MatchQuestionPair pair : this.pairs)
			{
				// ignore the deleted one
				if (!((pair.getChoice() == null) && (pair.getMatch() == null)))
				{
					newChoices.add(pair);
				}
			}

			this.pairs = newChoices;
		}

		if (stayHere) return null;
		return destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKey()
	{
		// what is A, B, C: choice order
		// what is 1, 2, 3: match order

		StringBuffer rv = new StringBuffer();
		// // get the choices as would be presented in delivery
		// List<MatchQuestionPair> choices = getPairs();
		//
		// // that's the A, B, C order, so find each correct one
		// for (Integer correctIndex : this.correctAnswers)
		// {
		// int i = 0;
		// for (MatchQuestionPair choice : choices)
		// {
		// if (choice.id.equals(correctIndex.toString()))
		// {
		// // TODO: hard coding our A, B, Cs?
		// rv.append((char) ('A' + i));
		// rv.append(",");
		// }
		// i++;
		// }
		// }
		//
		// if (rv.length() > 0) rv.setLength(rv.length() - 1);
		return rv.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		// list of choices
		EntityList choices = this.uiService.newEntityList();
		choices.setStyle(EntityList.Style.form);
		choices.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.pairs"), "pair");
		// .setIndexReference("id")

		AutoColumn autoCol = this.uiService.newAutoColumn();
		choices.addColumn(autoCol);

		EntityListColumn col = this.uiService.newEntityListColumn();
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 25);
		edit.setProperty(this.uiService.newPropertyReference().setReference("pair.choice"));
		col.setTitle("choice");
		col.add(edit);
		choices.addColumn(col);

		col = this.uiService.newEntityListColumn();
		edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 25);
		edit.setProperty(this.uiService.newPropertyReference().setReference("pair.match"));
		col.setTitle("match");
		col.add(edit);
		choices.addColumn(col);

		col = this.uiService.newEntityListColumn();
		Navigation nav = this.uiService.newNavigation();
		Destination destination = this.uiService.newDestination();
		destination.setDestination("DEL:{0}", this.uiService.newPropertyReference().setReference("pair.id"));
		nav.setTitle("delete").setIcon("/icons/delete.png", Navigation.IconStyle.left).setStyle(Navigation.Style.link).setSubmit().setDestination(
				destination);
		col.add(nav);
		choices.addColumn(col);

		HtmlEdit distractor = this.uiService.newHtmlEdit();
		distractor.setTitle("distractor", this.uiService.newIconPropertyReference().setIcon("/icons/distractor_add.png"));
		distractor.setSize(5, 25);
		distractor.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.distractor"));

		Selection addMore = uiService.newSelection();
		addMore.addSelection("none", "ADD:0");
		addMore.addSelection("one", "ADD:1");
		addMore.addSelection("two", "ADD:2");
		addMore.addSelection("three", "ADD:3");
		addMore.addSelection("four", "ADD:4");
		addMore.addSelection("five", "ADD:5");
		addMore.setOrientation(Selection.Orientation.dropdown);
		addMore.setSubmitValue();
		addMore.setTitle("more-choices");

		Section choicesSection = this.uiService.newSection();
		choicesSection.setTitle("choices", this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png"));
		choicesSection.add(choices).add(distractor).add(addMore);

		return this.uiService.newFragment().setMessages(this.messages).add(choicesSection);
	}

	// /**
	// * Access the correct answers as an array.
	// *
	// * @return The correct answers.
	// */
	// public String[] getCorrectAnswers()
	// {
	// String[] rv = new String[this.correctAnswers.size()];
	// int i = 0;
	// for (Integer correct : this.correctAnswers)
	// {
	// rv[i++] = correct.toString();
	// }
	//
	// return rv;
	// }

	// /**
	// * Access the correct answers as a set.
	// *
	// * @return The correct answers.
	// */
	// public Set getCorrectAnswerSet()
	// {
	// return this.correctAnswers;
	// }

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
//		EntityList entityList = this.uiService.newEntityList();
//		entityList.setStyle(EntityList.Style.form);
//		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.pairs"), "pair");
//
//		AutoColumn choiceAuto = this.uiService.newAutoColumn();
//		entityList.addColumn(choiceAuto);
//
//		PropertyColumn choice = this.uiService.newPropertyColumn();
//		choice.setProperty(this.uiService.newHtmlPropertyReference().setReference("pair.choice"));
//		entityList.addColumn(choice);
//
//		// match
//
//		AutoColumn matchAuto = this.uiService.newAutoColumn();
//		entityList.addColumn(matchAuto);
//
//		PropertyColumn match = this.uiService.newPropertyColumn();
//		match.setProperty(this.uiService.newHtmlPropertyReference().setReference("pair.match"));
//		entityList.addColumn(match);
		
		Text text = this.uiService.newText().setText("tbd");

		return this.uiService.newFragment().setMessages(this.messages).add(text);
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
		if (this.distractor == null) return null;
		return this.distractor.getChoice();
	}

	/**
	 * Access the pairs as an entity (MatchQuestionChoice) list in as-authored order.
	 * 
	 * @return The pairs as an entity (MatchQuestionChoice) list in as-authored order.
	 */
	public List<MatchQuestionPair> getPairs()
	{
		if (this.pairs.size() == 0)
		{
			consolidate("ADD:4");
		}
		return this.pairs;
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
//		EntityList entityList = this.uiService.newEntityList();
//		entityList.setStyle(EntityList.Style.form);
//		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");
//
//		Text answerKey = this.uiService.newText();
//		PropertyReference[] refs = new PropertyReference[2];
//		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
//		refs[1] = this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.answerKey");
//		answerKey.setText("answer-key", refs);
//
//		// AndDecision and = this.uiService.newAndDecision();
//		// Decision[] decisions = new Decision[2];
//		// decisions[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
//		// decisions[1] = this.uiService.newDecision().setProperty(
//		// this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
//		// and.setRequirements(decisions);
//		//
//		// OrDecision or = this.uiService.newOrDecision();
//		// Decision[] decisionsOr = new Decision[2];
//		// decisionsOr[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
//		// decisionsOr[1] = and;
//		// or.setOptions(decisionsOr);
//		//
//		// ???.setCorrectDecision(or);
//
//		Decision[] orInc = new Decision[2];
//		orInc[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
//		orInc[1] = this.uiService.newDecision().setProperty(
//				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
//		answerKey.setIncluded(this.uiService.newOrDecision().setOptions(orInc));
//
//		AutoColumn autoCol = this.uiService.newAutoColumn();
//		entityList.addColumn(autoCol);
//
//		PropertyColumn propCol = this.uiService.newPropertyColumn();
//		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
//		entityList.addColumn(propCol);

		Text text = this.uiService.newText().setText("tbd");

		return this.uiService.newFragment().setMessages(this.messages).add(text);
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
//		// TODO: just the selected answer, no distractors, and add correct/incorrect marking
//		EntityList entityList = this.uiService.newEntityList();
//		entityList.setStyle(EntityList.Style.form);
//		entityList.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.choices"), "choice");
//
//		AutoColumn autoCol = this.uiService.newAutoColumn();
//		entityList.addColumn(autoCol);
//
//		PropertyColumn propCol = this.uiService.newPropertyColumn();
//		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
//		entityList.addColumn(propCol);

		Text text = this.uiService.newText().setText("tbd");
		return this.uiService.newFragment().setMessages(this.messages).add(text);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
//		// TODO: add correct/incorrect marking
//		EntityList entityList = this.uiService.newEntityList();
//		entityList.setStyle(EntityList.Style.form);
//		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.choicesAsAuthored"), "choice");
//
//		AutoColumn autoCol = this.uiService.newAutoColumn();
//		entityList.addColumn(autoCol);
//
//		PropertyColumn propCol = this.uiService.newPropertyColumn();
//		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("choice.text"));
//		entityList.addColumn(propCol);
//
//		Text answerKey = this.uiService.newText();
//		PropertyReference[] refs = new PropertyReference[2];
//		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
//		refs[1] = this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.answerKey");
//		answerKey.setText("answer-key", refs);

		Text text = this.uiService.newText().setText("tbd");

		return this.uiService.newFragment().setMessages(this.messages).add(text);
	}

	// public void setAnswerPairs(List<String> choices)
	// {
	// this.pairs = new ArrayList<MatchQuestionPair>(choices.size());
	// int i = 0;
	// for (String choice : choices)
	// {
	// this.pairs.add(new MatchQuestionPair(Integer.toString(i++), choice, choice));
	// }
	// }

	// /**
	// * Sets the correct answers.
	// *
	// * @param correctAnswers
	// * The correct answers.
	// */
	// public void setCorrectAnswers(String[] correctAnswers)
	// {
	// this.correctAnswers.clear();
	// if (correctAnswers == null) return;
	// for (String answer : correctAnswers)
	// {
	// this.correctAnswers.add(Integer.valueOf(answer));
	// }
	// }

	// /**
	// * Sets the correct answers as a set.
	// *
	// * @param correctAnswers
	// * The correct answers.
	// */
	// public void setCorrectAnswerSet(Set<Integer> answers)
	// {
	// this.correctAnswers.clear();
	// if (answers == null) return;
	// this.correctAnswers.addAll(answers);
	// }

	public void setDistractor(String distractor)
	{
		if (this.distractor == null)
		{
			this.distractor = new MatchQuestionPair(distractor, null);
		}
		else
		{
			this.distractor.setChoice(distractor);
		}
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
