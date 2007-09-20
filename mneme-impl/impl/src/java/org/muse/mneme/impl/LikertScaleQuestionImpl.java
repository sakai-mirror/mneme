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

import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.EntityListColumn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.AutoColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * LikertScaleQuestionImpl handles questions for the multiple choice question type.
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

	/** List of choices */
	private String answerOptions[][] = { {"Strongly Agree", "Agree", "Undecided", "Disagree", "Strongly Disagree"},
			{"Excellent", "Good", "Poor", "Unacceptable"}, {"Above Average", "Average", "Below Average"}, {"Yes", "No"}, {"5", "4", "3", "2", "1"}};

	/*
	 * private String answerOptions[][] = {{{"Strongly Agree"} , {"Agree"} , {"Undecided"} , {"Disagree"} , {"Strongly Disagree"}}, {{"Excellent"} ,
	 * {"Good"} , {"Poor"} , {"Unacceptable"}}, {{"Above Average"} , {"Average"} , {"Below Average"}}, {{"Yes"} , {"No"}}, {{"5"}, {"4"}, {"3"},
	 * {"2"}, {"1"}} };
	 */

	/** This hash set holds index numbers of the correct answers */
	protected Set<Integer> correctAnswers = new HashSet<Integer>();

	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** This is the value of the option selected by the instructor */
	protected Integer selectedOption;

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
		// TODO: deep copy?
		this.selectedOption = other.selectedOption;
		this.messages = other.messages;
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

			// TODO|: ? nothing to deep copy

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
	public String getAnswerKey()
	{
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
		row.setTitle("answer");
		row.add(entityList);

		EntityDisplay display = this.uiService.newEntityDisplay();
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * Access the options as an entity (LikertScaleQuestionChoice) list.
	 * 
	 * @return The options as an entity (LikertScaleQuestionChoice) list.
	 */
	public List<LikertScaleQuestionChoice> getOptions()
	{
		List<LikertScaleQuestionChoice> rv = new ArrayList<LikertScaleQuestionChoice>(this.answerOptions.length);
		for (int i = 0; i < this.answerOptions.length; i++)
		{
			StringBuffer optionText = new StringBuffer();
			for (int j = 0; j < this.answerOptions[i].length; j++)
			{
				optionText.append(this.answerOptions[i][j]);
				optionText.append("/");
			}
			optionText.deleteCharAt(optionText.length() - 1);
			rv.add(new LikertScaleQuestionChoice(String.valueOf(i), optionText.toString()));
		}

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
		List<LikertScaleQuestionChoice> rv = new ArrayList<LikertScaleQuestionChoice>(this.answerOptions[optionIndex].length);
		for (int j = 0; j < this.answerOptions[optionIndex].length; j++)
		{
			rv.add(new LikertScaleQuestionChoice(String.valueOf(j), this.answerOptions[optionIndex][j]));
		}

		return rv;
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
