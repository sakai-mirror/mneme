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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * ManualPartImpl implements ManualPart
 */
public class ManualPartImpl extends PartImpl implements ManualPart
{
	protected List<String> questionIds = new ArrayList<String>();

	protected Boolean randomize = Boolean.FALSE;

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param questionService
	 *        The QuestionService.
	 */
	public ManualPartImpl(AssessmentImpl assessment, QuestionService questionService)
	{
		super(assessment, questionService);
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public ManualPartImpl(ManualPartImpl other)
	{
		super(other);
		this.questionIds = new ArrayList<String>(other.questionIds.size());
		this.questionIds.addAll(other.questionIds);
		this.randomize = other.randomize;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		this.questionIds.add(question.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getFirstQuestion()
	{
		List<String> order = getQuestionOrder();
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(order.get(0));

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getLastQuestion()
	{
		List<String> order = getQuestionOrder();
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(order.get(order.size() - 1));

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		return this.questionIds.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends Question> getQuestions()
	{
		List<String> order = getQuestionOrder();
		List<Question> rv = new ArrayList<Question>(order.size());
		for (String id : order)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(id);

			// set the assessment, part and submission context
			question.initSubmissionContext(this.assessment.getSubmissionContext());
			question.initPartContext(this);

			rv.add(question);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends Question> getQuestionsAsAuthored()
	{
		List<Question> rv = new ArrayList<Question>(this.questionIds.size());
		for (String id : this.questionIds)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(id);

			// set the assessment, part and submission context
			question.initSubmissionContext(this.assessment.getSubmissionContext());
			question.initPartContext(this);

			rv.add(question);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomize()
	{
		return this.randomize;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalPoints()
	{
		float total = 0f;
		for (String id : this.questionIds)
		{
			total += this.questionService.getQuestion(id).getPool().getPoints();
		}

		return total;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		this.questionIds.remove(question.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionOrder(String[] questionIds)
	{
		if (questionIds == null) throw new IllegalArgumentException();
		List<String> ids = new ArrayList(Arrays.asList(questionIds));

		// remove anything from the new list not in our questions
		ids.retainAll(this.questionIds);

		// remove these from our list
		this.questionIds.removeAll(ids);

		// add to the end of the new list any remaining quesitions from our list
		ids.addAll(this.questionIds);

		// take the new list
		this.questionIds = ids;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomize(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.randomize = setting;
	}

	/**
	 * Get the list of questions as they should be presented for the submission context.
	 * 
	 * @return The list of questions as they should be presented for the submission context.
	 */
	protected List<String> getQuestionOrder()
	{
		if ((!this.randomize) || (this.assessment == null) || (this.assessment.getSubmissionContext() == null)) return this.questionIds;

		// set the seed based on the id of the submission context,
		// so each submission has a different unique ordering,
		// and the part id, so the randomization of questions in each part within the same submission differs
		long seed = (this.assessment.getSubmissionContext() + "_" + this.id).hashCode();

		// copy the questions
		List<String> rv = new ArrayList<String>(this.questionIds);

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed));

		return rv;
	}
}
