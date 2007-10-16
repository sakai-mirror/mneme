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

import org.muse.ambrosia.api.CompareDecision;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * LikertScaleQuestionImpl handles questions for the Likert question type.
 */
public class LikertScaleQuestionImpl implements TypeSpecificQuestion
{
	public class LikertScaleQuestionChoice
	{
		protected String id;

		protected String text;

		public LikertScaleQuestionChoice(String id, String text)
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

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Which scale option to use for this question (0-agree 1-good 2-average 3-yes 4-numbers. */
	protected Integer selectedOption = null;

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
	public LikertScaleQuestionImpl(InternationalizedMessages messages, UiService uiService, Question question)
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
	public LikertScaleQuestionImpl(Question question, LikertScaleQuestionImpl other)
	{
		this.selectedOption = other.selectedOption;
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
			((LikertScaleQuestionImpl) rv).question = question;

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
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.options"), "option");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		selCol.setSingle();

		selCol.setValueProperty(this.uiService.newPropertyReference().setReference("option.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.selectedOption"));
		entityList.addColumn(selCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("option.text"));
		entityList.addColumn(propCol);

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("scale");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList
				.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.optionValues"), "optionValue");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		selCol.setSingle();

		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("optionValue.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		entityList.addColumn(selCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("optionValue.text"));
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

	/**
	 * {@inheritDoc}
	 */
	public String getModelAnswer()
	{
		return null;
	}

	/**
	 * Access the options as an entity (LikertScaleQuestionChoice) list.
	 * 
	 * @return The options as an entity (LikertScaleQuestionChoice) list.
	 */
	public List<LikertScaleQuestionChoice> getOptions()
	{
		List<LikertScaleQuestionChoice> rv = new ArrayList<LikertScaleQuestionChoice>(5);
		StringBuffer optionText = new StringBuffer();
		optionText.append(this.messages.getString("strongly-agree"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("agree"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("undecided"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("disagree"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("strongly-disagree"));

		rv.add(new LikertScaleQuestionChoice("0", optionText.toString()));
		optionText.setLength(0);

		optionText.append(this.messages.getString("excellent"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("good"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("poor"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("unacceptable"));

		rv.add(new LikertScaleQuestionChoice("1", optionText.toString()));
		optionText.setLength(0);

		optionText.append(this.messages.getString("above-average"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("average"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("below-average"));

		rv.add(new LikertScaleQuestionChoice("2", optionText.toString()));
		optionText.setLength(0);

		optionText.append(this.messages.getString("yes"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("no"));

		rv.add(new LikertScaleQuestionChoice("3", optionText.toString()));
		optionText.setLength(0);

		optionText.append(this.messages.getString("five"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("four"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("three"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("two"));
		optionText.append(" / ");
		optionText.append(this.messages.getString("one"));

		rv.add(new LikertScaleQuestionChoice("4", optionText.toString()));

		return rv;
	}

	/**
	 * Access the option values as an entity (LikertScaleQuestionChoice) list.
	 * 
	 * @return The option values as an entity (LikertScaleQuestionChoice) list.
	 */
	public List<LikertScaleQuestionChoice> getOptionValues()
	{
		int optionIndex = this.selectedOption.intValue();
		List<LikertScaleQuestionChoice> rv = null;
		if (optionIndex == 0)
		{
			rv = new ArrayList<LikertScaleQuestionChoice>(5);
			rv.add(new LikertScaleQuestionChoice("0", this.messages.getString("strongly-agree")));
			rv.add(new LikertScaleQuestionChoice("1", this.messages.getString("agree")));
			rv.add(new LikertScaleQuestionChoice("2", this.messages.getString("undecided")));
			rv.add(new LikertScaleQuestionChoice("3", this.messages.getString("disagree")));
			rv.add(new LikertScaleQuestionChoice("4", this.messages.getString("strongly-disagree")));
		}
		if (optionIndex == 1)
		{
			rv = new ArrayList<LikertScaleQuestionChoice>(4);
			rv.add(new LikertScaleQuestionChoice("0", this.messages.getString("excellent")));
			rv.add(new LikertScaleQuestionChoice("1", this.messages.getString("good")));
			rv.add(new LikertScaleQuestionChoice("2", this.messages.getString("poor")));
			rv.add(new LikertScaleQuestionChoice("3", this.messages.getString("unacceptable")));
		}
		if (optionIndex == 2)
		{
			rv = new ArrayList<LikertScaleQuestionChoice>(3);
			rv.add(new LikertScaleQuestionChoice("0", this.messages.getString("above-average")));
			rv.add(new LikertScaleQuestionChoice("1", this.messages.getString("average")));
			rv.add(new LikertScaleQuestionChoice("2", this.messages.getString("below-average")));
		}
		if (optionIndex == 3)
		{
			rv = new ArrayList<LikertScaleQuestionChoice>(2);
			rv.add(new LikertScaleQuestionChoice("0", this.messages.getString("yes")));
			rv.add(new LikertScaleQuestionChoice("1", this.messages.getString("no")));
		}
		if (optionIndex == 4)
		{
			rv = new ArrayList<LikertScaleQuestionChoice>(5);
			rv.add(new LikertScaleQuestionChoice("0", this.messages.getString("five")));
			rv.add(new LikertScaleQuestionChoice("1", this.messages.getString("four")));
			rv.add(new LikertScaleQuestionChoice("2", this.messages.getString("three")));
			rv.add(new LikertScaleQuestionChoice("3", this.messages.getString("two")));
			rv.add(new LikertScaleQuestionChoice("4", this.messages.getString("one")));
		}
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList
				.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.optionValues"), "optionValue");

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		selCol.setSingle();

		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("optionValue.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		entityList.addColumn(selCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("optionValue.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSelectedOption()
	{
		return this.selectedOption.toString();
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
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewAnswerUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList
				.setIterator(this.uiService.newPropertyReference().setReference("answer.question.typeSpecificQuestion.optionValues"), "optionValue");

		// include each choice only if the choice has been selected by the user
		PropertyReference entityIncludedProperty = this.uiService.newPropertyReference().setReference("optionValue.id");
		PropertyReference entityIncludedComparison = this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer");
		CompareDecision entityIncludedDecision = this.uiService.newCompareDecision();
		entityIncludedDecision.setProperty(entityIncludedProperty);
		entityIncludedDecision.setEqualsProperty(entityIncludedComparison);
		entityList.setEntityIncluded(entityIncludedDecision);

		SelectionColumn selCol = this.uiService.newSelectionColumn();
		selCol.setSingle();

		selCol.setValueProperty(this.uiService.newTextPropertyReference().setReference("optionValue.id"));
		selCol.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answer"));
		selCol.setReadOnly(this.uiService.newTrueDecision());
		entityList.addColumn(selCol);

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("optionValue.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
		EntityList entityList = this.uiService.newEntityList();
		entityList.setStyle(EntityList.Style.form);
		entityList.setIterator(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.optionValues"), "optionValue");

		PropertyColumn propCol = this.uiService.newPropertyColumn();
		propCol.setProperty(this.uiService.newHtmlPropertyReference().setReference("optionValue.text"));
		entityList.addColumn(propCol);

		return this.uiService.newFragment().setMessages(this.messages).add(entityList);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelectedOption(String selectedOption)
	{
		this.selectedOption = Integer.valueOf(selectedOption);
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
