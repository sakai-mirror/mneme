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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.MnemeService;
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

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	protected long nextId = 100;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	protected Map<String, QuestionImpl> questions = new HashMap<String, QuestionImpl>();

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/**
	 * {@inheritDoc}
	 */
	public void copyPoolQuestions(String userId, Pool source, Pool destination)
	{
		List<QuestionImpl> questions = new ArrayList<QuestionImpl>(this.questions.values());
		for (QuestionImpl question : questions)
		{
			if (question.getPool().equals(source))
			{
				QuestionImpl q = new QuestionImpl(question);
				q.setPool(destination);

				// clear the id to make it new
				q.id = null;

				// set the new created info
				q.getCreatedBy().setUserId(userId);
				q.getCreatedBy().setDate(new Date());

				// save
				saveQuestion(q);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(String userId, Pool pool, String search)
	{
		// TODO: search

		int count = 0;
		for (QuestionImpl question : this.questions.values())
		{
			if ((!question.deleted) && question.getCreatedBy().getUserId().equals(userId) && ((pool == null) || (question.getPool().equals(pool))))
			{
				count++;
			}
		}

		return count;
	}

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
	public Boolean existsQuestion(String id)
	{
		fakeIt();

		QuestionImpl question = this.questions.get(id);
		if (question == null) return Boolean.FALSE;
		if (question.deleted) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String userId, Pool pool, final QuestionService.FindQuestionsSort sort, String search, Integer pageNum,
			Integer pageSize)
	{
		fakeIt();

		// TODO: search

		List<Question> rv = new ArrayList<Question>();
		for (QuestionImpl question : this.questions.values())
		{
			if ((!question.deleted) && question.getCreatedBy().getUserId().equals(userId) && ((pool == null) || (question.getPool().equals(pool))))
			{
				rv.add(new QuestionImpl(question));
			}
		}

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				switch (sort)
				{
					case type_a:
					{
						rv = ((Question) arg0).getType().compareTo(((Question) arg1).getTypeName());
						break;
					}
					case type_d:
					{
						rv = -1 * ((Question) arg0).getType().compareTo(((Question) arg1).getTypeName());
						break;
					}
					case description_a:
					{
						rv = ((Question) arg0).getDescription().compareTo(((Question) arg1).getDescription());
						break;
					}
					case description_d:
					{
						rv = -1 * ((Question) arg0).getDescription().compareTo(((Question) arg1).getDescription());
						break;
					}
					case pool_difficulty_a:
					{
						rv = ((Question) arg0).getPool().getDifficulty().compareTo(((Question) arg1).getPool().getDifficulty());
						break;
					}
					case pool_difficulty_d:
					{
						rv = -1 * ((Question) arg0).getPool().getDifficulty().compareTo(((Question) arg1).getPool().getDifficulty());
						break;
					}
					case pool_points_a:
					{
						rv = ((Question) arg0).getPool().getPoints().compareTo(((Question) arg1).getPool().getPoints());
						break;
					}
					case pool_points_d:
					{
						rv = -1 * ((Question) arg0).getPool().getPoints().compareTo(((Question) arg1).getPool().getPoints());
						break;
					}
					case pool_subject_a:
					{
						rv = ((Question) arg0).getPool().getSubject().compareTo(((Question) arg1).getPool().getSubject());
						break;
					}
					case pool_subject_d:
					{
						rv = -1 * ((Question) arg0).getPool().getSubject().compareTo(((Question) arg1).getPool().getSubject());
						break;
					}
					case pool_title_a:
					{
						rv = ((Question) arg0).getPool().getTitle().compareTo(((Question) arg1).getPool().getTitle());
						break;
					}
					case pool_title_d:
					{
						rv = -1 * ((Question) arg0).getPool().getTitle().compareTo(((Question) arg1).getPool().getTitle());
						break;
					}
				}

				return rv;
			}
		});

		// page
		if ((pageNum != null) && (pageSize != null))
		{
			// start at ((pageNum-1)*pageSize)
			int start = ((pageNum - 1) * pageSize);
			if (start < 0) start = 0;
			if (start > rv.size()) start = rv.size() - 1;

			// end at ((pageNum)*pageSize)-1, or max-1, (note: subList is not inclusive for the end position)
			int end = ((pageNum) * pageSize);
			if (end < 0) end = 0;
			if (end > rv.size()) end = rv.size();

			rv = rv.subList(start, end);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getPoolQuestions(Pool pool)
	{
		List<String> rv = new ArrayList<String>();
		for (QuestionImpl question : this.questions.values())
		{
			if ((!question.deleted) && (question.getPool().equals(pool)))
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
		if (rv == null) return null;
		if (rv.deleted) return null;

		// return a copy
		rv = new QuestionImpl(rv);
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
	public void moveQuestion(Question question, Pool pool)
	{
		// get the question
		QuestionImpl fromStorage = this.questions.get(question.getId());
		if (fromStorage == null) return;

		// change the pool id
		fromStorage.poolId = pool.getId();
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
	public QuestionImpl newQuestion(QuestionImpl question)
	{
		QuestionImpl rv = new QuestionImpl(question);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePoolQuestions(Pool pool)
	{
		for (Iterator i = this.questions.values().iterator(); i.hasNext();)
		{
			QuestionImpl question = (QuestionImpl) i.next();
			if (question.getPool().equals(pool))
			{
				question.deleted = Boolean.TRUE;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(QuestionImpl question)
	{
		fakeIt();

		QuestionImpl q = this.questions.get(question.getId());
		if (q != null)
		{
			q.deleted = Boolean.TRUE;
		}
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
			Date now = new Date();

			QuestionImpl q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q1");
			q.setDescription("question one");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("True or False (one)?");
			((TrueFalseQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswer("TRUE");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question one<br />Hints are rich text.");
			q.setFeedback("feedback for question one");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q2");
			q.setDescription("question two");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("True or False (two)?");
			((TrueFalseQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswer("FALSE");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question two.");
			q.setFeedback("feedback for question two");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:MultipleChoice");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q3");
			q.setDescription("question three");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Which value will it be?");
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setSingleCorrect("TRUE");
			List<String> answerChoices = new ArrayList<String>();
			answerChoices.add("This is the first item");
			answerChoices.add("This is the second item");
			answerChoices.add("This is the third item");
			answerChoices.add("This is the fourth item");
			Set<Integer> correctAnswers = new HashSet<Integer>();
			correctAnswers.add(new Integer(0));
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setAnswerChoices(answerChoices);
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setShuffleChoices("TRUE");
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswerSet(correctAnswers);
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question three.");
			q.setFeedback("feedback for question 3");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:LikertScale");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q4");
			q.setDescription("question four");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Is this needed?");
			((LikertScaleQuestionImpl) q.getTypeSpecificQuestion()).setSelectedOption("2");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for q 4.");
			q.setFeedback("feedback for question 4");
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:FillBlanks");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q5");
			q.setDescription("question five");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("He was beaten {black} and {blue}.");
			((FillBlanksQuestionImpl) q.getTypeSpecificQuestion()).setResponseTextual("true");
			((FillBlanksQuestionImpl) q.getTypeSpecificQuestion()).setAnyOrder("true");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			questions.put(q.getId(), q);
		}
	}
}
