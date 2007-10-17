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
import org.muse.ambrosia.api.Container;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.EntityDisplay;
import org.muse.ambrosia.api.EntityDisplayRow;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.EntityListColumn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.PropertyColumn;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.SelectionColumn;
import org.muse.ambrosia.api.Overlay;
import org.muse.ambrosia.api.Toggle;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.OrderColumn;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * NonsubQuestionImpl handles questions for the Nonsub question type.
 */
public class NonsubQuestionImpl implements TypeSpecificQuestion
{
	/** Our messages. */
	protected transient InternationalizedMessages messages = null;

	/** This property holds the model answer */
	protected String modelAnswer = null;

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
	public NonsubQuestionImpl(InternationalizedMessages messages, UiService uiService, Question question)
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
	public NonsubQuestionImpl(Question question, NonsubQuestionImpl other)
	{
		this.messages = other.messages;
		this.question = other.question;
		this.modelAnswer = other.modelAnswer;
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

			// set the question
			((NonsubQuestionImpl) rv).question = question;

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
		EntityDisplay display = this.uiService.newEntityDisplay();

		EntityDisplayRow row = this.uiService.newEntityDisplayRow();
		row = this.uiService.newEntityDisplayRow();
		row.setTitle("model-answer");
		HtmlEdit edit = this.uiService.newHtmlEdit();
		edit.setSize(5, 50);
		edit.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer"));
		row.add(edit);
		display.addRow(row);

		return this.uiService.newFragment().setMessages(this.messages).add(display);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.question.getPresentation().getText();
	}

	/**
	 * @return the modelAnswer (rich text)
	 */
	public String getModelAnswer()
	{
		return this.modelAnswer;
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
	public Boolean getUseFeedback()
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getUseHints()
	{
		return Boolean.FALSE;
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
		Text txt = this.uiService.newText();
		txt.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.typeSpecificAnswer.answerData"));
		return this.uiService.newFragment().setMessages(this.messages).add(txt);
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getViewQuestionUi()
	{
		Container cont = this.uiService.newContainer();
		Overlay ovly = this.uiService.newOverlay();

		ovly.setId("modelanswer");
		ovly.add(this.uiService.newText().setText("modelAnswer"));
		ovly.add(this.uiService.newText().setText(null,
				this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.modelAnswer")));
		ovly.add(this.uiService.newGap());
		ovly.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("hide-model-answer"));
		cont.add(ovly);
		cont.add(this.uiService.newToggle().setTarget("modelanswer").setTitle("view-model-answer").setIcon("/icons/answer_key2.png", null));

		return this.uiService.newFragment().setMessages(this.messages).add(cont);
	}

	public void setModelAnswer(String modelAnswer)
	{
		this.modelAnswer = modelAnswer;
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
