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

import java.util.List;
import java.util.ArrayList;

import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.FillIn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * FillBlanksQuestionImpl handles questions for the true/false question type.
 */
public class FillBlanksQuestionImpl implements TypeSpecificQuestion
{
	/** The correct answers. */
	protected List<String> correctAnswers = new ArrayList<String>();

	protected InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** Dependency: The UI service (Ambrosia). */
	protected UiService uiService = null;

	/** TRUE means answer is case sensitive, FALSE means it is not */
	protected Boolean caseSensitive = Boolean.FALSE;

	/** TRUE means any order is ok, FALSE means it is not */
	protected Boolean anyOrder = Boolean.FALSE;

	/** TRUE means response is textual, FALSE means response is numeric */
	protected Boolean responseTextual = Boolean.TRUE;

	/** This variable contains the parsed presentation text of the question */
	protected String parsedText = null;

	/**
	 * Construct.
	 * 
	 * @param uiService
	 *        the UiService.
	 * @param question
	 *        The Question this is a helper for.
	 */
	public FillBlanksQuestionImpl(InternationalizedMessages messages, UiService uiService, Question question)
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
	public FillBlanksQuestionImpl(Question question, FillBlanksQuestionImpl other)
	{
		this.question = question;
		this.correctAnswers = other.correctAnswers;
		this.messages = other.messages;
		this.caseSensitive = other.caseSensitive;
		this.anyOrder = other.anyOrder;
		this.responseTextual = other.responseTextual;
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
			((FillBlanksQuestionImpl) rv).question = question;

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
		// return this.correctAnswer ? this.messages.getString("true") : this.messages.getString("false");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getAuthoringUi()
	{
		EntityDisplay display = this.uiService.newEntityDisplay();

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("case-sensitive");
		Selection selection = uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.caseSensitive"));
		row.add(selection);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("any-order");
		selection = uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.anyOrder"));
		row.add(selection);
		display.addRow(row);

		selection = this.uiService.newSelection();
		selection.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.responseTextual"));
		selection.addSelection("textual", "true");
		selection.addSelection("numeric", "false");

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("response");
		row.add(selection);
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);

	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		FillIn fillIn = this.uiService.newFillIn();
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.parsedText")).setProperty(
				this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers")).setWidth(20);

		return this.uiService.newFragment().setMessages(this.messages).add(fillIn);

	}

	private static String extractFIBTextArray(String alltext)
	{
		StringBuffer strBuf = new StringBuffer();

		while (alltext.indexOf("{") > -1)
		{
			int alltextLeftIndex = alltext.indexOf("{");
			int alltextRightIndex = alltext.indexOf("}");

			String tmp = alltext.substring(0, alltextLeftIndex);
			alltext = alltext.substring(alltextRightIndex + 1);
			strBuf.append(tmp);
			strBuf.append("{}");
			// there are no more "}", exit loop
			if (alltextRightIndex == -1)
			{
				break;
			}
		}
		strBuf.append(alltext);
		return strBuf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
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
	 * {@inheritDoc}
	 */
	public String getCaseSensitive()
	{
		return this.caseSensitive.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnyOrder()
	{
		return this.anyOrder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getResponseTextual()
	{
		return this.responseTextual.toString();
	}

	public String getParsedText()
	{
		this.parsedText = extractFIBTextArray(this.question.getPresentation().getText());
		return this.parsedText;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCaseSensitive(String caseSensitive)
	{
		this.caseSensitive = Boolean.valueOf(caseSensitive);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnyOrder(String anyOrder)
	{
		this.anyOrder = Boolean.valueOf(anyOrder);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setResponseTextual(String responseTextual)
	{
		this.responseTextual = Boolean.valueOf(responseTextual);
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
