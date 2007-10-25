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
import org.muse.ambrosia.api.AttachmentsEdit;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Container;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.FillIn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.muse.ambrosia.api.Overlay;
import org.muse.ambrosia.api.Toggle;
import org.muse.ambrosia.api.Navigation;

/**
 * FillBlanksQuestionImpl handles questions for the true/false question type.
 */
public class FillBlanksQuestionImpl implements TypeSpecificQuestion
{
	/** TRUE means any order is ok, FALSE means it is not */
	protected Boolean anyOrder = Boolean.FALSE;

	/** TRUE means answer is case sensitive, FALSE means it is not */
	protected Boolean caseSensitive = Boolean.FALSE;

	protected InternationalizedMessages messages = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** TRUE means response is textual, FALSE means response is numeric */
	protected Boolean responseTextual = Boolean.TRUE;

	/** The question text. */
	protected String text = null;

	/** Dependency: The UI service (Ambrosia). */
	protected UiService uiService = null;

	/**
	 * {@inheritDoc}
	 */
	public QuestionPlugin getPlugin()
	{
		return this.plugin;
	}

	protected transient QuestionPlugin plugin = null;

	/**
	 * Construct.
	 * 
	 * @param uiService
	 *        the UiService.
	 * @param question
	 *        The Question this is a helper for.
	 */
	public FillBlanksQuestionImpl(QuestionPlugin plugin, InternationalizedMessages messages, UiService uiService, Question question)
	{
		this.plugin = plugin;
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
		this.anyOrder = other.anyOrder;
		this.caseSensitive = other.caseSensitive;
		this.messages = other.messages;
		this.question = question;
		this.responseTextual = other.responseTextual;
		this.text = other.text;
		this.uiService = other.uiService;
		this.plugin = other.plugin;
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
	public String consolidate(String destination)
	{
		return destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKey()
	{
		StringBuffer answerKey = new StringBuffer();
		List<String> correctAnswers = getCorrectAnswers();

		for (String correctAnswer : correctAnswers)
		{
			answerKey.append(correctAnswer);
			answerKey.append(",");
		}
		answerKey.deleteCharAt(answerKey.length() - 1);
		return answerKey.toString();
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
	public Component getAuthoringUi()
	{
		EntityDisplay display = this.uiService.newEntityDisplay();

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row.setTitle("question");
		HtmlEdit text = uiService.newHtmlEdit();
		text.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.text"));
		row.add(text);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		Container cont = this.uiService.newContainer();
		Overlay ovly = this.uiService.newOverlay();
		ovly.setId("inst");
		ovly.add(this.uiService.newText().setText("view-instructions"));
		ovly.add(this.uiService.newText().setText("instructions"));
		ovly.add(this.uiService.newGap());
		ovly.add(this.uiService.newToggle().setTarget("inst").setTitle("hide-instructions"));
		cont.add(ovly);
		cont.add(this.uiService.newToggle().setTarget("inst").setTitle("view-instructions").setIcon("/icons/test.png", Navigation.IconStyle.left));
		row.add(cont);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
		row.setTitle("attachments");
		AttachmentsEdit attachments = uiService.newAttachmentsEdit();
		attachments.setAttachments(this.uiService.newPropertyReference().setReference("question.presentation.attachments"), null);
		row.add(attachments);
		display.addRow(row);

		row = this.uiService.newEntityDisplayRow();
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
	public String getCaseSensitive()
	{
		return this.caseSensitive.toString();
	}

	public List<String> getCorrectAnswers()
	{
		List<String> correctAnswers = new ArrayList<String>();

		String alltext = getText();
		while (alltext.indexOf("{") > -1)
		{
			int alltextLeftIndex = alltext.indexOf("{");
			int alltextRightIndex = alltext.indexOf("}");

			String tmp = alltext.substring(alltextLeftIndex + 1, alltextRightIndex);
			alltext = alltext.substring(alltextRightIndex + 1);
			correctAnswers.add(tmp);

			// there are no more "}", exit loop
			if (alltextRightIndex == -1)
			{
				break;
			}
		}

		return correctAnswers;
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

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return getParsedText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelAnswer()
	{
		return null;
	}

	public String getParsedText()
	{
		String parsedText = extractFIBTextArray(getText());
		return parsedText;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getResponseTextual()
	{
		return this.responseTextual.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getReviewUi()
	{
		FillIn fillIn = this.uiService.newFillIn();
		AndDecision and = this.uiService.newAndDecision();
		Decision[] decisions = new Decision[2];
		decisions[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
		decisions[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		and.setRequirements(decisions);
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.parsedText"));
		fillIn.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		fillIn.setWidth(20);
		fillIn.setCorrectDecision(and);
		fillIn.setReadOnly(this.uiService.newTrueDecision());
		fillIn.setCorrect(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.entryCorrects"));

		return this.uiService.newFragment().setMessages(this.messages).add(fillIn);
	}

	/**
	 * Access the question text.
	 * 
	 * @return The quesion text.
	 */
	public String getText()
	{
		return this.text;
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
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseReason()
	{
		return Boolean.TRUE;
	}

	public Component getViewAnswerUi()
	{
		FillIn fillIn = this.uiService.newFillIn();
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.parsedText"));
		fillIn.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		fillIn.setWidth(20);
		fillIn.setCorrectDecision(this.uiService.newTrueDecision());
		fillIn.setReadOnly(this.uiService.newTrueDecision());
		fillIn.setCorrect(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.entryCorrects"));

		return this.uiService.newFragment().setMessages(this.messages).add(fillIn);
	}

	public Component getViewQuestionUi()
	{
		Text txt = this.uiService.newText();
		txt.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.text"));
		return this.uiService.newFragment().setMessages(this.messages).add(txt);
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
	public void setCaseSensitive(String caseSensitive)
	{
		this.caseSensitive = Boolean.valueOf(caseSensitive);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setResponseTextual(String responseTextual)
	{
		this.responseTextual = Boolean.valueOf(responseTextual);
	}

	/**
	 * Set the question text.
	 * 
	 * @param text
	 *        The question text.
	 */
	public void setText(String text)
	{
		this.text = text;
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

	protected String extractFIBTextArray(String alltext)
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
}
