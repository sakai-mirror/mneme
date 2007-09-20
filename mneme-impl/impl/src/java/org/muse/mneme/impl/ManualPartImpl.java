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
import org.muse.mneme.api.Pool;
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

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;
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
	public Boolean getIsValid()
	{
		// we must have questions
		if (this.questionIds.isEmpty()) return Boolean.FALSE;

		// the questions must exist
		for (String questionId : this.questionIds)
		{
			if (!this.questionService.existsQuestion(questionId))
			{
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
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
		int count = 0;
		for (String id : this.questionIds)
		{
			if (this.questionService.existsQuestion(id)) count++;
		}

		return count;
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
			if (question != null)
			{
				// set the assessment, part and submission context
				question.initSubmissionContext(this.assessment.getSubmissionContext());
				question.initPartContext(this);

				rv.add(question);
			}
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
			if (question != null)
			{
				// set the assessment, part and submission context
				question.initSubmissionContext(this.assessment.getSubmissionContext());
				question.initPartContext(this);

				rv.add(question);
			}
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
			Question question = this.questionService.getQuestion(id);
			if (question != null)
			{
				Pool pool = question.getPool();
				if (pool != null)
				{
					total += pool.getPoints();
				}
			}
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

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionOrder(String[] questionIds)
	{
		if (questionIds == null) return;
		List<String> ids = new ArrayList(Arrays.asList(questionIds));

		// make a copy of our current list
		List<String> current = new ArrayList<String>(this.questionIds);

		// remove anything from the new list not in our questions
		ids.retainAll(current);

		// remove these from our current list
		current.removeAll(ids);

		// add to the end of the new list any remaining quesitions from our current list
		ids.addAll(current);

		// if the order is the same as when we started, ignore it.
		boolean changed = false;
		for (int i = 0; i < ids.size(); i++)
		{
			if (!this.questionIds.get(i).equals(ids.get(i)))
			{
				changed = true;
				break;
			}
		}

		// ignore if no changes
		if (!changed) return;

		// take the new list
		this.questionIds = ids;

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;
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
