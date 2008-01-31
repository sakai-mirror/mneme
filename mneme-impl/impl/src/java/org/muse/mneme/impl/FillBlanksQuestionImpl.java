/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.FillIn;
import org.muse.ambrosia.api.HtmlEdit;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.OrDecision;
import org.muse.ambrosia.api.Overlay;
import org.muse.ambrosia.api.PropertyReference;
import org.muse.ambrosia.api.Section;
import org.muse.ambrosia.api.Selection;
import org.muse.ambrosia.api.Text;
import org.muse.ambrosia.api.Toggle;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificQuestion;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.StringUtil;

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

	protected transient QuestionPlugin plugin = null;

	/** The question this is a helper for. */
	protected transient Question question = null;

	/** TRUE means response is textual, FALSE means response is numeric */
	protected Boolean responseTextual = Boolean.TRUE;

	/** The question text. */
	protected String text = null;

	/** Dependency: The UI service (Ambrosia). */
	protected UiService uiService = null;

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
			answerKey.append(", ");
		}

		if (answerKey.length() > 0) answerKey.setLength(answerKey.length() - 2);

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
		// question (with instructions)
		HtmlEdit question = uiService.newHtmlEdit();
		question.setSize(14, 100);
		question.setProperty(this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.text"));
		question.setTitle("question");

		Overlay instructions = this.uiService.newOverlay();
		instructions.setId("instructions");
		instructions
				.add(this.uiService.newText().setText("instructions-title", this.uiService.newIconPropertyReference().setIcon("/icons/test.png")));
		instructions.add(this.uiService.newText().setText("instructions"));
		instructions.add(this.uiService.newGap());
		instructions.add(this.uiService.newToggle().setTarget("instructions").setTitle("close")
				.setIcon("/icons/close.png", Navigation.IconStyle.left));
		instructions.add(this.uiService.newGap());
		instructions.add(this.uiService.newText().setTitle("examples-title"));
		instructions.add(this.uiService.newText().setText("example1").setTitle("example1-title"));
		instructions.add(this.uiService.newText().setText("example2").setTitle("example2-title"));
		instructions.add(this.uiService.newText().setText("example3").setTitle("example3-title"));
		instructions.add(this.uiService.newText().setText("example4").setTitle("example4-title"));
		instructions.add(this.uiService.newText().setText("example5").setTitle("example5-title"));
		instructions.add(this.uiService.newGap());
		instructions.add(this.uiService.newToggle().setTarget("instructions").setTitle("close")
				.setIcon("/icons/close.png", Navigation.IconStyle.left));

		Toggle viewInstructions = this.uiService.newToggle().setTarget("instructions").setTitle("view-instructions").setIcon("/icons/test.png",
				Navigation.IconStyle.left);

		Section questionSection = this.uiService.newSection();
		questionSection.add(question).add(instructions).add(viewInstructions);

		// answer options
		Selection response = this.uiService.newSelection();
		response.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.responseTextual"));
		response.addSelection(this.uiService.newMessage().setMessage("textual"), this.uiService.newMessage().setTemplate("true"));
		response.addSelection(this.uiService.newMessage().setMessage("numeric"), this.uiService.newMessage().setTemplate("false"));
		response.setTitle("answer", this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png"));

		Selection caseSensitive = this.uiService.newSelection();
		caseSensitive.addSelection(this.uiService.newMessage().setMessage("case-sensitive"), this.uiService.newMessage().setTemplate("true"));
		caseSensitive.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.caseSensitive"));

		Selection order = this.uiService.newSelection();
		order.addSelection(this.uiService.newMessage().setMessage("any-order"), this.uiService.newMessage().setTemplate("true"));
		order.setProperty(this.uiService.newPropertyReference().setReference("question.typeSpecificQuestion.anyOrder"));

		Section answerSection = this.uiService.newSection();
		answerSection.add(response).add(caseSensitive).add(order);

		return this.uiService.newFragment().setMessages(this.messages).add(questionSection).add(answerSection);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCaseSensitive()
	{
		return this.caseSensitive.toString();
	}

	/**
	 * Get the correct answers for the question.
	 * 
	 * @return A List containing each correct answer for the fill-ins in the question.
	 */
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
	public String[] getData()
	{
		String[] rv = new String[4];
		rv[0] = this.anyOrder.toString();
		rv[1] = this.caseSensitive.toString();
		rv[2] = this.responseTextual.toString();
		rv[3] = this.text;

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Component getDeliveryUi()
	{
		FillIn fillIn = this.uiService.newFillIn();
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.questionText"))
				.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers")).setWidth(20);
		fillIn.setWidth(20);

		Section section = this.uiService.newSection();
		section.add(fillIn);

		return this.uiService.newFragment().setMessages(this.messages).add(section);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return getQuestionText();
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPlugin getPlugin()
	{
		return this.plugin;
	}

	/**
	 * Produce a string of the question with the answers removed.
	 * 
	 * @return The question text with the answers removed.
	 */
	public String getQuestionText()
	{
		if (this.text == null) return null;

		String text = this.text;
		StringBuffer rv = new StringBuffer();

		while (text.indexOf("{") > -1)
		{
			int left = text.indexOf("{");
			int right = text.indexOf("}");

			String tmp = text.substring(0, left);
			text = text.substring(right + 1);
			rv.append(tmp);
			rv.append("{}");

			// there are no more "}", exit loop
			if (right == -1)
			{
				break;
			}
		}

		rv.append(text);

		return rv.toString();
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
		// should we show correct marks?
		AndDecision mayReviewAndShowCorrect = this.uiService.newAndDecision();
		Decision[] decisionsMayReviewAndShowCorrect = new Decision[2];
		decisionsMayReviewAndShowCorrect[0] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.submission.mayReview"));
		decisionsMayReviewAndShowCorrect[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));
		mayReviewAndShowCorrect.setRequirements(decisionsMayReviewAndShowCorrect);

		OrDecision or = this.uiService.newOrDecision();
		Decision[] decisionsOr = new Decision[2];
		decisionsOr[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
		decisionsOr[1] = mayReviewAndShowCorrect;
		or.setOptions(decisionsOr);

		Decision[] decisionsShowCorrect = new Decision[2];
		decisionsShowCorrect[0] = this.uiService.newCompareDecision().setEqualsConstant(AssessmentType.survey.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.type")).setReversed();
		decisionsShowCorrect[1] = or;
		Decision showCorrect = this.uiService.newAndDecision().setRequirements(decisionsShowCorrect);

		FillIn fillIn = this.uiService.newFillIn();
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.questionText"));
		fillIn.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		fillIn.setWidth(20);
		fillIn.setCorrectDecision(showCorrect);
		fillIn.setReadOnly(this.uiService.newTrueDecision());
		fillIn.setCorrect(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.entryCorrects"));

		Text answerKey = this.uiService.newText();
		PropertyReference[] refs = new PropertyReference[2];
		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
		refs[1] = this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.answerKey");
		answerKey.setText("answer-key", refs);

		Decision[] orInc = new Decision[2];
		orInc[0] = this.uiService.newDecision().setProperty(this.uiService.newPropertyReference().setReference("grading"));
		orInc[1] = this.uiService.newDecision().setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.review.showCorrectAnswer"));

		Decision[] andInc = new Decision[2];
		andInc[0] = this.uiService.newCompareDecision().setEqualsConstant(AssessmentType.survey.toString()).setProperty(
				this.uiService.newPropertyReference().setReference("answer.question.part.assessment.type")).setReversed();
		andInc[1] = this.uiService.newOrDecision().setOptions(orInc);
		answerKey.setIncluded(this.uiService.newAndDecision().setRequirements(andInc));

		Section first = this.uiService.newSection();
		first.add(fillIn);

		Section second = this.uiService.newSection();
		second.add(answerKey);

		return this.uiService.newFragment().setMessages(this.messages).add(first).add(second);
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
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("answer.question.typeSpecificQuestion.questionText"));
		fillIn.setProperty(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.answers"));
		fillIn.setWidth(20);
		fillIn.setCorrectDecision(this.uiService.newTrueDecision());
		fillIn.setReadOnly(this.uiService.newTrueDecision());
		fillIn.setCorrect(this.uiService.newPropertyReference().setReference("answer.typeSpecificAnswer.entryCorrects"));

		return this.uiService.newFragment().setMessages(this.messages).add(fillIn);
	}

	public Component getViewQuestionUi()
	{
		FillIn fillIn = this.uiService.newFillIn();
		fillIn.setText(null, this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.questionText"));
		fillIn.setWidth(20);
		fillIn.setReadOnly(this.uiService.newTrueDecision());

		Text answerKey = this.uiService.newText();
		PropertyReference[] refs = new PropertyReference[2];
		refs[0] = this.uiService.newIconPropertyReference().setIcon("/icons/answer_key.png");
		refs[1] = this.uiService.newHtmlPropertyReference().setReference("question.typeSpecificQuestion.answerKey");
		answerKey.setText("answer-key", refs);

		Section first = this.uiService.newSection();
		first.add(fillIn);

		Section second = this.uiService.newSection();
		second.add(answerKey);

		return this.uiService.newFragment().setMessages(this.messages).add(first).add(second);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnyOrder(String anyOrder)
	{
		if (anyOrder == null) throw new IllegalArgumentException();

		Boolean b = Boolean.valueOf(anyOrder);
		if (!Different.different(b, this.anyOrder)) return;

		this.anyOrder = b;

		this.question.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCaseSensitive(String caseSensitive)
	{
		if (caseSensitive == null) throw new IllegalArgumentException();

		Boolean b = Boolean.valueOf(caseSensitive);
		if (!Different.different(b, this.caseSensitive)) return;

		this.caseSensitive = b;

		this.question.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setData(String[] data)
	{
		if ((data != null) && (data.length == 4))
		{
			this.anyOrder = Boolean.valueOf(data[0]);
			this.caseSensitive = Boolean.valueOf(data[1]);
			this.responseTextual = Boolean.valueOf(data[2]);
			this.text = data[3];
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setResponseTextual(String responseTextual)
	{
		if (responseTextual == null) throw new IllegalArgumentException();

		Boolean b = Boolean.valueOf(responseTextual);
		if (!Different.different(b, this.responseTextual)) return;

		this.responseTextual = b;

		this.question.setChanged();
	}

	/**
	 * Set the question text.
	 * 
	 * @param text
	 *        The question text.
	 */
	public void setText(String text)
	{
		if (!Different.different(this.text, text)) return;

		this.text = StringUtil.trimToNull(text);

		this.question.setChanged();
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
