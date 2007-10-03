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
	 * @param assessmentId
	 *        The assessment this is the parts for.
	 * @param questionService
	 *        The QuestionService.
	 * @param poolService
	 *        The PoolService.
	 */
	public DrawPartImpl(AssessmentImpl assessment, QuestionService questionService, PoolService poolService)
	{
		super(assessment, questionService);
		this.poolService = poolService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public DrawPartImpl(DrawPartImpl other)
	{
		super(other);
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

				return already;
			}
		}

		// add this to the pools
		PoolDraw rv = new PoolDrawImpl(this.poolService, pool, numQuestions);
		pools.add(rv);

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

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
		List<String> order = getQuestionOrderAsAuthored();
		List<Question> rv = new ArrayList<Question>(order.size());
		for (String id : order)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(id);
			if (question != null)
			{
				// set the assessment, part context
				question.initPartContext(this);
				rv.add(question);
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
	 * Get the list of questions as they should be presented for the submission context.
	 * 
	 * @return The list of questions as they should be presented for the submission context.
	 */
	protected List<String> getQuestionOrder()
	{
		// set the seed based on the id of the submission context,
		// so each submission has a different unique ordering,
		// and the part id, so the randomization of questions in each part within the same submission differs
		long seed = 0;
		if (this.assessment.getSubmissionContext() != null)
		{
			seed = (this.assessment.getSubmissionContext().getId() + "_" + this.id).hashCode();
		}
		
		// if no submission context, just the part id
		else
		{
			seed = this.id.hashCode();
		}

		// random draw from the pools, randomize the results
		List<String> rv = new ArrayList<String>();
		for (PoolDraw draw : this.pools)
		{
			rv.addAll(draw.drawQuestionIds(seed));
		}

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed));

		return rv;
	}

	/**
	 * Get the list of questions as they should be presented for the submission context.
	 * 
	 * @return The list of questions as they should be presented for the submission context.
	 */
	protected List<String> getQuestionOrderAsAuthored()
	{
		// random draw from the pools, randomize the results
		List<String> rv = new ArrayList<String>();
		for (PoolDraw draw : this.pools)
		{
			rv.addAll(draw.getAllQuestionIds());
		}

		return rv;
	}
}
