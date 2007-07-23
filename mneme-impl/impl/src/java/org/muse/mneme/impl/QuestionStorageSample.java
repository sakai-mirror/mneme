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
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.TypeSpecificQuestion;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class QuestionStorageSample implements QuestionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionStorageSample.class);

	protected Object idGenerator = new Object();

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	protected long nextId = 100;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	protected Map<String, QuestionImpl> questions = new HashMap<String, QuestionImpl>();

	/** Dependency: QuestionService */
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

		// if found, return a copy
		QuestionImpl rv = this.questions.get(id);
		if (rv != null)
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
		QuestionImpl rv = new QuestionImpl(poolService, questionService);
		return rv;
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
	 * Dependency: MnemeService.
	 * 
	 * @param service
	 *        The MnemeService.
	 */
	public void setMnemeService(MnemeService service)
	{
		this.mnemeService = service;
	}

	/**
	 * Dependency: PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
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
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q1");
			q.setDescription("question one");
			q.setRequireRationale(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (one)?");
			((TrueFalseQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswer("TRUE");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q2");
			q.setDescription("question two");
			q.setRequireRationale(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (two)?");
			((TrueFalseQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswer("FALSE");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q3");
			q.setDescription("question three");
			q.setRequireRationale(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getAttribution().setUserId("admin");
			q.getPresentation().setText("True or False (three)?");
			((TrueFalseQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswer("TRUE");
			questions.put(q.getId(), q);
		}
	}
}
