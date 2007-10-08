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

import org.muse.mneme.api.Changeable;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * ManualPartImpl implements ManualPart
 */
public class ManualPartImpl extends PartImpl implements ManualPart
{
	protected List<PoolPick> questions = new ArrayList<PoolPick>();

	protected Boolean randomize = Boolean.FALSE;

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param questionService
	 *        The QuestionService.
	 */
	public ManualPartImpl(AssessmentImpl assessment, QuestionService questionService, Changeable owner)
	{
		super(assessment, questionService, owner);
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 * @param assessment
	 *        The assessment this is the parts for.
	 */
	public ManualPartImpl(ManualPartImpl other, AssessmentImpl assessment, Changeable owner)
	{
		super(other, assessment, owner);
		this.questions = new ArrayList<PoolPick>(other.questions.size());
		this.questions.addAll(other.questions);
		this.randomize = other.randomize;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		// TODO: do we already have this? ignore it?
		this.questions.add(new PoolPick(question.getId()));

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean dependsOn(Pool pool)
	{
		for (PoolPick pick : this.questions)
		{
			if (pool.getId().equals(pick.getPoolId()))
			{
				return Boolean.TRUE;
			}

			Question question = this.questionService.getQuestion(pick.getQuestionId());
			if (question != null)
			{
				if (question.getPool().equals(pool))
				{
					return Boolean.TRUE;
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getFirstQuestion()
	{
		List<PoolPick> order = getQuestionPickOrder();
		PoolPick pick = order.get(0);
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(pick.getQuestionId());

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);
		question.initPoolContext(pick.getPoolId());

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// we must have questions
		if (this.questions.isEmpty()) return Boolean.FALSE;

		// the questions must exist
		for (PoolPick pick : this.questions)
		{
			if (!this.questionService.existsQuestion(pick.getQuestionId()))
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
		List<PoolPick> order = getQuestionPickOrder();
		PoolPick pick = order.get(order.size() - 1);
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(pick.getQuestionId());

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);
		question.initPoolContext(pick.getPoolId());

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		int count = 0;
		for (PoolPick pick : this.questions)
		{
			if (this.questionService.existsQuestion(pick.getQuestionId())) count++;
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends Question> getQuestionsAsAuthored()
	{
		List<Question> rv = new ArrayList<Question>(this.questions.size());
		for (PoolPick pick : this.questions)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(pick.getQuestionId());
			if (question != null)
			{
				// set the assessment, part and submission context
				question.initSubmissionContext(this.assessment.getSubmissionContext());
				question.initPartContext(this);
				question.initPoolContext(pick.getPoolId());

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
		for (PoolPick pick : this.questions)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(pick.getQuestionId());
			if (question != null)
			{
				question.initPoolContext(pick.getPoolId());
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

		PoolPick remove = null;
		for (PoolPick pick : this.questions)
		{
			if (pick.getQuestionId().equals(question.getId()))
			{
				remove = pick;
				break;
			}
		}

		if (remove != null)
		{
			this.questions.remove(remove);

			// this is a change that cannot be made to live tests
			this.assessment.liveChanged = Boolean.TRUE;

			this.owner.setChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionOrder(String[] questionIds)
	{
		if (questionIds == null) return;

		List<PoolPick> ids = new ArrayList<PoolPick>();
		for (String id : questionIds)
		{
			ids.add(new PoolPick(id));
		}

		// make a copy of our current list
		List<PoolPick> current = new ArrayList<PoolPick>(this.questions);

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
			if (!this.questions.get(i).equals(ids.get(i)))
			{
				changed = true;
				break;
			}
		}

		// ignore if no changes
		if (!changed) return;

		// take the new list
		this.questions = ids;

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomize(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		if (this.randomize.equals(setting)) return;

		this.randomize = setting;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchPool(Pool from, Pool to)
	{
		for (PoolPick pick : this.questions)
		{
			if (from.getId().equals(pick.getPoolId()))
			{
				pick.setPool(to.getId());
			}

			else if (pick.getPoolId() == null)
			{
				Question question = this.questionService.getQuestion(pick.getQuestionId());
				if (question != null)
				{
					if (question.getPool().equals(from))
					{
						pick.setPool(to.getId());
					}
				}
			}
		}
	}

	/**
	 * Get the list of question picks as they should be presented for the submission context.
	 * 
	 * @return The list of question picks as they should be presented for the submission context.
	 */
	protected List<PoolPick> getQuestionPickOrder()
	{
		if ((!this.randomize) || (this.assessment == null) || (this.assessment.getSubmissionContext() == null)) return this.questions;

		// copy the questions
		List<PoolPick> rv = new ArrayList<PoolPick>(this.questions);

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed()));

		return rv;
	}
}
