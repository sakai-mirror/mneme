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

import org.muse.mneme.api.AssessmentService;
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
		this.pools.addAll(other.pools);
		this.poolService = other.poolService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPool(Pool pool, Integer numQuestions)
	{
		pools.add(new PoolDrawImpl(this.poolService, pool, numQuestions));
	}

	/**
	 * {@inheritDoc}
	 */
	public List<PoolDraw> getDraws()
	{
		return pools;
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
		int count = 0;
		for (PoolDraw draw : this.pools)
		{
			count += draw.getNumQuestions();
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
	public Float getTotalPoints()
	{
		float total = 0f;
		for (PoolDraw draw : this.pools)
		{
			total += (draw.getNumQuestions() * draw.getPool().getPoints());
		}

		return total;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool)
	{
		this.pools.remove(new PoolDrawImpl(this.poolService, pool, 0));
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
		long seed = (this.assessment.getSubmissionContext().getId() + "_" + this.id).hashCode();

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
}
