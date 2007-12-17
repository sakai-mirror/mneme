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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.muse.mneme.api.Changeable;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.StringUtil;

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
	 * @param submissionService
	 *        The SubmissionService.
	 * @param poolService
	 *        The PoolService.
	 * @param owner
	 *        A Changeable to report changes to.
	 * @param messages
	 *        A messages bundle.
	 */
	public DrawPartImpl(AssessmentImpl assessment, QuestionService questionService, SubmissionService submissionService, PoolService poolService,
			Changeable owner, InternationalizedMessages messages)
	{
		super(assessment, questionService, submissionService, owner, messages);
		this.poolService = poolService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param owner
	 *        A Changeable to report changes to.
	 */
	public DrawPartImpl(DrawPartImpl other, AssessmentImpl assessment, Changeable owner)
	{
		super(other, assessment, owner);
		this.pools = new ArrayList<PoolDraw>(other.pools.size());
		for (PoolDraw draw : other.pools)
		{
			this.pools.add(new PoolDrawImpl(this.assessment, (PoolDrawImpl) draw));
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
				if (!Different.different(already.getNumQuestions(), numQuestions))
				{
					// no change, we are done
					return already;
				}

				// change the count
				already.setNumQuestions(numQuestions);

				// this is a change that cannot be made to live tests
				this.assessment.liveChanged = Boolean.TRUE;

				setChanged();

				return already;
			}
		}

		// add this to the pools
		PoolDraw rv = new PoolDrawImpl(this.assessment, this.poolService, pool, numQuestions);
		pools.add(rv);

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		setChanged();

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
	public List<PoolDraw> getDraws(final PoolService.FindPoolsSort sort)
	{
		List<PoolDraw> rv = new ArrayList<PoolDraw>(this.pools);

		// sort
		if (sort != null)
		{
			Collections.sort(rv, new Comparator()
			{
				public int compare(Object arg0, Object arg1)
				{
					int rv = 0;
					switch (sort)
					{
						case title_a:
						{
							String s0 = StringUtil.trimToZero(((PoolDraw) arg0).getPool().getTitle());
							String s1 = StringUtil.trimToZero(((PoolDraw) arg1).getPool().getTitle());
							rv = s0.compareToIgnoreCase(s1);
							break;
						}
						case title_d:
						{
							String s0 = StringUtil.trimToZero(((PoolDraw) arg0).getPool().getTitle());
							String s1 = StringUtil.trimToZero(((PoolDraw) arg1).getPool().getTitle());
							rv = -1 * s0.compareToIgnoreCase(s1);
							break;
						}
						case points_a:
						{
							Float f0 = ((PoolDraw) arg0).getPool().getPoints();
							if (f0 == null) f0 = Float.valueOf(0f);
							Float f1 = ((PoolDraw) arg1).getPool().getPoints();
							if (f1 == null) f1 = Float.valueOf(0f);
							rv = f0.compareTo(f1);
							break;
						}
						case points_d:
						{
							Float f0 = ((PoolDraw) arg0).getPool().getPoints();
							if (f0 == null) f0 = Float.valueOf(0f);
							Float f1 = ((PoolDraw) arg1).getPool().getPoints();
							if (f1 == null) f1 = Float.valueOf(0f);
							rv = -1 * f0.compareTo(f1);
							break;
						}
					}

					return rv;
				}
			});
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolDraw> getDrawsForPools(String context, PoolService.FindPoolsSort sort, String search)
	{
		// get all the pools we need
		List<Pool> allPools = this.poolService.findPools(context, sort, search);

		List<PoolDraw> rv = new ArrayList<PoolDraw>();

		// prepare draws - virtual, not part of the DrawPart
		for (Pool pool : allPools)
		{
			PoolDraw draw = new PoolDrawImpl(this.assessment, this.poolService, pool, null);
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
	public String getInvalidMessage()
	{
		Object[] args = new Object[1];
		args[0] = this.getOrdering().getPosition().toString();

		// we must have draws
		if (this.pools.isEmpty())
		{
			return messages.getFormattedMessage("invalid-part-empty", args);
		}

		// each pool must have enough questions to draw
		for (PoolDraw draw : this.pools)
		{
			Pool pool = draw.getPool();
			if (pool == null)
			{
				return messages.getFormattedMessage("invalid-draw-part-deleted-pool", args);
			}
			if (draw.getPoolNumAvailableQuestions() < draw.getNumQuestions())
			{
				return messages.getFormattedMessage("invalid-draw-part-overdraw", args);
			}
		}

		return null;
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
			if (draw.getPoolNumAvailableQuestions() < draw.getNumQuestions())
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
		PoolDraw rv = new PoolDrawImpl(this.assessment, this.poolService, pool, null);
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
		this.pools.remove(new PoolDrawImpl(this.assessment, this.poolService, pool, null));

		// this is a change that cannot be made to live tests
		this.assessment.liveChanged = Boolean.TRUE;

		setChanged();
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
					if ((draw.getNumQuestions() == null) || (draw.getNumQuestions().equals(0)))
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

							setChanged();
						}
					}
				}

				// else we need a new one (if not 0 count)
				else if ((draw.getNumQuestions() != null) && (draw.getNumQuestions() > 0))
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
	protected List<PoolPick> getPossibleQuestionPicks()
	{
		List<PoolPick> rv = new ArrayList<PoolPick>();
		for (PoolDraw draw : this.pools)
		{
			List<String> draws = draw.getAllQuestionIds();
			for (String id : draws)
			{
				PoolPick pick = new PoolPick(this.questionService, id, draw.getPoolId());
				rv.add(pick);
			}
		}

		return rv;
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
				PoolPick pick = new PoolPick(this.questionService, id, draw.getPoolId());
				rv.add(pick);
			}
		}

		// randomize the questions in the copy
		Collections.shuffle(rv, new Random(seed));

		return rv;
	}

	/**
	 * Reconstruct a draw.
	 * 
	 * @param poolId
	 *        The poolId value.
	 * @param modernPoolId
	 *        The modern pool id.
	 * @param modernDeleted
	 *        set if the draw would not be in a modern version of the assessment.
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param numQuestions
	 *        The number of questions.
	 */
	protected void initDraw(String poolId, String modernPoolId, Boolean modernDeleted, Integer numQuestions)
	{
		PoolDraw draw = new PoolDrawImpl(this.assessment, this.poolService, poolId, modernPoolId, modernDeleted, numQuestions);
		pools.add(draw);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setModern()
	{
		for (Iterator i = this.pools.iterator(); i.hasNext();)
		{
			PoolDrawImpl draw = (PoolDrawImpl) i.next();

			// if the modern version is deleted, remove it
			if (!draw.setModern())
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
		for (PoolDraw draw : this.pools)
		{
			if (draw.getPoolId().equals(from.getId()))
			{
				draw.setPool(to);
			}
		}
	}
}
