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
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.QuestionPart;
import org.muse.mneme.api.QuestionType;

public class QuestionPartImpl implements QuestionPart
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionPartImpl.class);;

	protected List<AssessmentAnswerImpl> answers = new ArrayList<AssessmentAnswerImpl>();

	protected String id = null;

	/** The back pointer to the question */
	protected transient AssessmentQuestionImpl question = null;

	protected String title = null;

	/**
	 * Construct
	 */
	public QuestionPartImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public QuestionPartImpl(QuestionPartImpl other)
	{
		setAnswers(other.answers);
		initId(other.getId());
		this.question = other.question;
		setTitle(other.getTitle());
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof QuestionPart)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare? position?
		// int compare = getSectionOrdering().getPosition().compareTo(((QuestionPart) obj).getSectionOrdering().getPosition());

		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof QuestionPart)) return false;
		if (this == obj) return true;
		if (this.getId() == null) return false;
		if (((QuestionPart) obj).getId() == null) return false;
		return ((QuestionPart) obj).getId().equals(getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAnswer getAnswer()
	{
		if (this.answers.size() != 1)
		{
			M_log.warn("getAnswer: to a multi-answer question");
		}

		return this.answers.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAnswer getAnswer(String answerId)
	{
		for (AssessmentAnswer answer : this.answers)
		{
			if (answer.getId().equals(answerId)) return answer;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentAnswer> getAnswersAsAuthored()
	{
		return this.answers;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentAnswer> getAnswers()
	{
		// randomize if needed (by authored setting, or if it's for a matching type question)
		if (((this.question.getRandomAnswerOrder() != null) && (this.question.getRandomAnswerOrder().booleanValue()))
				|| (this.question.getType() == QuestionType.matching))
		{
			// deep copy the answers
			List<AssessmentAnswerImpl> rv = new ArrayList<AssessmentAnswerImpl>(this.answers.size());
			for (AssessmentAnswerImpl answer : this.answers)
			{
				rv.add(new AssessmentAnswerImpl(answer));
			}

			// set the seed based on the current user id and the question id (question id, not part id, so that each part's answers randomize the same within the question -ggolden)
			long seed = (this.question.section.assessment.service.m_sessionManager.getCurrentSessionUserId() + this.question.id).hashCode();

			// mix up the answers
			Collections.shuffle(rv, new Random(seed));

			// but... we want the lables to remain in their initial ordering
			for (int i = 0; i < this.answers.size(); i++)
			{
				rv.get(i).setLabel(this.answers.get(i).getLabel());
			}

			return rv;
		}

		return this.answers;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<AssessmentAnswer> getCorrectAnswers()
	{
		List<AssessmentAnswer> rv = new ArrayList<AssessmentAnswer>();
		for (AssessmentAnswer answer : this.answers)
		{
			if ((answer.getIsCorrect() != null) && answer.getIsCorrect().booleanValue())
			{
				rv.add(answer);
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<AssessmentAnswer> getIncorrectAnswers()
	{
		List<AssessmentAnswer> rv = new ArrayList<AssessmentAnswer>();
		for (AssessmentAnswer answer : this.answers)
		{
			if ((answer.getIsCorrect() != null) && (!answer.getIsCorrect().booleanValue()))
			{
				rv.add(answer);
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getQuestion()
	{
		return this.question;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return getId().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnswers(List<? extends AssessmentAnswer> answers)
	{
		this.answers.clear();

		if (answers == null)
		{
			return;
		}

		// deep copy
		for (AssessmentAnswer answer : answers)
		{
			AssessmentAnswerImpl copy = new AssessmentAnswerImpl((AssessmentAnswerImpl) answer);
			this.answers.add(copy);
			copy.initPart(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * Initialize the answers.
	 * 
	 * @param answers
	 *        The answers - these are taken exactly, not deep copied.
	 */
	protected void initAnswers(List<AssessmentAnswerImpl> answers)
	{
		this.answers = answers;

		// set the back link
		for (AssessmentAnswerImpl answer : this.answers)
		{
			answer.initPart(this);
		}
	}

	/**
	 * Establish the id.
	 * 
	 * @param id
	 *        The question id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Establish the link to the containing question.
	 * 
	 * @param question
	 *        The containing question.
	 */
	protected void initQuestion(AssessmentQuestionImpl question)
	{
		this.question = question;
	}
}
