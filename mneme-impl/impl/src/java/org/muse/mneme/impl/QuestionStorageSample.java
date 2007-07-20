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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class QuestionStorageSample implements QuestionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionStorageSample.class);

	protected Object idGenerator = new Object();

	protected long nextId = 100;

	protected PoolService poolService = null;

	protected Map<String, QuestionImpl> questions = new HashMap<String, QuestionImpl>();

	protected QuestionService questionService = null;

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String userId)
	{
		fakeIt();

		List<Question> rv = new ArrayList<Question>();
		for (QuestionImpl question : this.questions.values())
		{
			if (question.getAttribution().getUserId().equals(userId))
			{
				rv.add(new QuestionImpl(question));
			}
		}

		return rv;
	}

	/**
	 * Find all the questions in the pool
	 * 
	 * @param pool
	 *        The pool.
	 * @return The List of question ids that are in the pool.
	 */
	public List<String> getPoolQuestions(Pool pool)
	{
		List<String> rv = new ArrayList<String>();
		for (QuestionImpl question : this.questions.values())
		{
			if (question.getPool().equals(pool))
			{
				rv.add(question.getId());
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionImpl getQuestion(String id)
	{
		fakeIt();

		QuestionImpl rv = this.questions.get(id);
		if (rv == null)
		{
			rv = newQuestion();
			rv.initId(id);
		}
		else
		{
			rv = new QuestionImpl(rv);
		}

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionImpl newQuestion()
	{
		return new QuestionImpl(poolService, questionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean questionExists(String id)
	{
		fakeIt();

		return (this.questions.get(id) != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(QuestionImpl question)
	{
		fakeIt();

		this.questions.remove(question.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(QuestionImpl question)
	{
		fakeIt();

		// assign an id
		if (question.getId() == null)
		{
			long id = 0;
			synchronized (this.idGenerator)
			{
				id = this.nextId;
				this.nextId++;
			}
			question.initId("q" + Long.toString(id));
		}

		this.questions.put(question.getId(), new QuestionImpl(question));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	protected void fakeIt()
	{
		// if we have not set up our questions, do so now
		if (questions.isEmpty())
		{
			QuestionImpl q = newQuestion();
			q.initId("q1");
			q.setDescription("question one");
			q.setRequireRationale(Boolean.TRUE);
			q.setType("mneme:true/false");
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (one)?");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initId("q2");
			q.setDescription("question two");
			q.setRequireRationale(Boolean.TRUE);
			q.setType("mneme:true/false");
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (two)?");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initId("q3");
			q.setDescription("question three");
			q.setRequireRationale(Boolean.TRUE);
			q.setType("mneme:true/false");
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (three)?");
			questions.put(q.getId(), q);
		}
	}
}
