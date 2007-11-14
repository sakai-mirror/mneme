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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.muse.mneme.api.Changeable;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SubmissionService;

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
	public ManualPartImpl(AssessmentImpl assessment, QuestionService questionService, SubmissionService submissionService, Changeable owner)
	{
		super(assessment, questionService, submissionService, owner);
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
		for (PoolPick pick : other.questions)
		{
			this.questions.add(new PoolPick(pick));
		}
		this.randomize = other.randomize;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addQuestion(Question question)
	{
		if (question == null) throw new IllegalArgumentException();
		// TODO: do we already have this? ignore it?
		this.questions.add(new PoolPick(this.questionService, question.getId()));

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		this.owner.setChanged();
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
	public List<Question> getQuestionsAsAuthored()
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
			ids.add(new PoolPick(this.questionService, id));
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
	 * Count the number of question picks from this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @return The number of question picks from this pool.
	 */
	protected int countPoolPicks(Pool pool)
	{
		int count = 0;
		for (PoolPick pick : this.questions)
		{
			String poolId = pick.getPoolId();
			if (poolId == null)
			{
				Question question = this.questionService.getQuestion(pick.getQuestionId());
				if (question != null)
				{
					poolId = question.getPool().getId();
				}
			}

			if ((poolId != null) && (poolId.equals(pool.getId())))
			{
				count++;
			}
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Boolean dependsOn(Pool pool, boolean directOnly)
	{
		// manual part dependencies are all indirect
		if (directOnly) return Boolean.FALSE;

		for (PoolPick pick : this.questions)
		{
			// use the pick's pool as an override to the question's native pool, if set
			if (pick.getPoolId() != null)
			{
				if (pool.getId().equals(pick.getPoolId()))
				{
					return Boolean.TRUE;
				}
			}
			else
			{
				Question question = this.questionService.getQuestion(pick.getQuestionId());
				if (question != null)
				{
					Pool qp = question.getPool();
					if ((qp != null) && (qp.equals(pool)))
					{
						return Boolean.TRUE;
					}
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Boolean dependsOn(Question question)
	{
		for (PoolPick pick : this.questions)
		{
			if (pick.getQuestionId().equals(question.getId()))
			{
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * Get the question ids that are manually selected from this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @return The question ids that are manually selected from this pool.
	 */
	protected List<String> getPoolPicks(Pool pool)
	{
		List<String> rv = new ArrayList<String>();
		for (PoolPick pick : this.questions)
		{
			String poolId = pick.getPoolId();
			if (poolId == null)
			{
				Question question = this.questionService.getQuestion(pick.getQuestionId());
				if (question != null)
				{
					poolId = question.getPool().getId();
				}
			}

			if ((poolId != null) && (poolId.equals(pool.getId())))
			{
				rv.add(pick.getQuestionId());
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	protected List<PoolPick> getPossibleQuestionPicks()
	{
		return this.questions;
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
	 */
	protected void setOrig()
	{
		for (Iterator i = this.questions.iterator(); i.hasNext();)
		{
			PoolPick pick = (PoolPick) i.next();

			// if we cannot restore the orig. values, remove the pick
			if (!pick.setOrig())
			{
				i.remove();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void switchPool(Pool from, Pool to, boolean directOnly)
	{
		// manual part dependencies are all indirect
		if (directOnly) return;

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
	 * {@inheritDoc}
	 */
	protected void switchQuestion(Question from, Question to)
	{
		for (PoolPick pick : this.questions)
		{
			if (from.getId().equals(pick.getQuestionId()))
			{
				pick.setQuestion(to.getId());

				// if we already have a pool set in the pick, leave it
				if (pick.getPoolId() == null)
				{
					// to is likely the history of from, so won't report a pool - use from's
					pick.setPool(from.getPool().getId());
				}
			}
		}
	}
}
