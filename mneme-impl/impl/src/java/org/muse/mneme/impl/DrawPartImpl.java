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
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * DrawPartImpl implements DrawPart
 */
public class DrawPartImpl extends PartImpl implements DrawPart
{
	protected PoolService poolService = null;

	List<PoolDraw> pools = new ArrayList<PoolDraw>();

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param questionService
	 *        The QuestionService.
	 * @param poolService
	 *        The PoolService.
	 */
	public DrawPartImpl(AssessmentImpl assessment, QuestionService questionService, PoolService poolService, Changeable owner)
	{
		super(assessment, questionService, owner);
		this.poolService = poolService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 * @param assessment
	 *        The assessment this is the parts for.
	 */
	public DrawPartImpl(DrawPartImpl other, AssessmentImpl assessment, Changeable owner)
	{
		super(other, assessment, owner);
		this.pools = new ArrayList<PoolDraw>(other.pools.size());
		for (PoolDraw draw : other.pools)
		{
			this.pools.add(new PoolDrawImpl((PoolDrawImpl) draw));
		}
		this.poolService = other.poolService;
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolDraw addPool(Pool pool, Integer numQuestions)
	{
		// do we have this pool already?
		for (PoolDraw already : this.pools)
		{
			if (already.getPoolId().equals(pool.getId()))
			{
				if (already.getNumQuestions().equals(numQuestions))
				{
					// no change, we are done
					return already;
				}

				// change the count
				already.setNumQuestions(numQuestions);

				// this is a change that cannot be made to live tests
				this.assessment.liveChanged = Boolean.TRUE;

				this.owner.setChanged();

				return already;
			}
		}

		// add this to the pools
		PoolDraw rv = new PoolDrawImpl(this.poolService, pool, numQuestions);
		pools.add(rv);

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		this.owner.setChanged();

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolDraw> getDraws()
	{
		return this.pools;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolDraw> getDrawsForPools(String context, PoolService.FindPoolsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		// get all the pools we need
		List<Pool> allPools = this.poolService.findPools(context, sort, search, pageNum, pageSize);

		List<PoolDraw> rv = new ArrayList<PoolDraw>();

		// prepare draws - virtual, not part of the DrawPart
		for (Pool pool : allPools)
		{
			PoolDraw draw = new PoolDrawImpl(this.poolService, pool, 0);
			if (this.pools.contains(draw))
			{
				PoolDraw myDraw = this.pools.get(this.pools.indexOf(draw));
				draw.setNumQuestions(myDraw.getNumQuestions());
			}
			rv.add(draw);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getFirstQuestion()
	{
		List<PoolPick> order = getQuestionPickOrder();
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(order.get(0).getQuestionId());

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);
		question.initPoolContext(order.get(0).getPoolId());
		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// we must have draws
		if (this.pools.isEmpty()) return Boolean.FALSE;

		// each pool must have enough questions to draw
		for (PoolDraw draw : this.pools)
		{
			Pool pool = draw.getPool();
			if (pool == null)
			{
				return Boolean.FALSE;
			}
			if (pool.getNumQuestions() < draw.getNumQuestions())
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
		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(order.get(order.size() - 1).getQuestionId());

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);
		question.initPoolContext(order.get(order.size() - 1).getPoolId());

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		int count = 0;
		for (PoolDraw draw : this.pools)
		{
			Pool pool = draw.getPool();
			if (pool != null)
			{
				count += draw.getNumQuestions();
			}
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> getQuestionsAsAuthored()
	{
		List<Question> rv = new ArrayList<Question>();

		for (PoolDraw draw : this.pools)
		{
			List<String> ids = draw.getAllQuestionIds();

			for (String id : ids)
			{
				QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(id);
				if (question != null)
				{
					// set the assessment, part and submission context
					question.initSubmissionContext(this.assessment.getSubmissionContext());
					question.initPartContext(this);
					question.initPoolContext(draw.getPoolId());

					rv.add(question);
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalPoints()
	{
		float total = 0f;
		for (PoolDraw draw : this.pools)
		{
			Pool pool = draw.getPool();
			if (pool != null)
			{
				total += (draw.getNumQuestions() * pool.getPoints());
			}
		}

		return total;
	}

	/**
	 * {@inheritDoc}
	 */
	public PoolDraw getVirtualDraw(Pool pool)
	{
		PoolDraw rv = new PoolDrawImpl(this.poolService, pool, 0);
		if (this.pools.contains(rv))
		{
			PoolDraw myDraw = this.pools.get(this.pools.indexOf(rv));
			rv.setNumQuestions(myDraw.getNumQuestions());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool)
	{
		this.pools.remove(new PoolDrawImpl(this.poolService, pool, 0));

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		this.owner.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateDraws(List<PoolDraw> draws)
	{
		if (draws == null) throw new IllegalArgumentException();

		for (PoolDraw draw : draws)
		{
			if (draw.getPool() != null)
			{
				// do we have this pool already?
				if (this.pools.contains(draw))
				{
					// if the new count is 0, remove it
					if (draw.getNumQuestions() == 0)
					{
						removePool(draw.getPool());
					}

					else
					{
						// is our count different?
						PoolDraw myDraw = this.pools.get(this.pools.indexOf(draw));
						if (!myDraw.getNumQuestions().equals(draw.getNumQuestions()))
						{
							// update the count
							myDraw.setNumQuestions(draw.getNumQuestions());

							// this is a change that cannot be made to live tests
							this.assessment.liveChanged = Boolean.TRUE;

							this.owner.setChanged();
						}
					}
				}

				// else we need a new one (if not 0 count)
				else if (draw.getNumQuestions() > 0)
				{
					addPool(draw.getPool(), draw.getNumQuestions());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected Boolean dependsOn(Pool pool, boolean directOnly)
	{
		// draw part dependencies are all direct
		for (PoolDraw draw : this.pools)
		{
			if (draw.getPoolId().equals(pool.getId()))
			{
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Boolean dependsOn(Question question)
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	protected List<PoolPick> getQuestionPickOrder()
	{
		long seed = seed();

		// random draw from the pools, randomize the results
		List<PoolPick> rv = new ArrayList<PoolPick>();
		for (PoolDraw draw : this.pools)
		{
			List<String> draws = draw.drawQuestionIds(seed);
			for (String id : draws)
			{
				PoolPick pick = new PoolPick(id, draw.getPoolId());
				rv.add(pick);
			}
		}

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed));

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void switchPool(Pool from, Pool to, boolean directOnly)
	{
		for (PoolDraw draw : this.pools)
		{
			if (draw.getPoolId().equals(from.getId()))
			{
				draw.setPool(to);
			}
		}
	}
}
