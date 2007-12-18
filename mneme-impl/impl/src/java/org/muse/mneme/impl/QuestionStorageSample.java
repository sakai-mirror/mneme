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
import java.util.LinkedHashMap;
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
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.util.StringUtil;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class QuestionStorageSample implements QuestionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionStorageSample.class);

	protected boolean fakedAlready = false;

	protected Object idGenerator = new Object();

	/** Dependency: MnemeService */
	protected MnemeService mnemeService = null;

	protected long nextId = 100;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	protected Map<String, QuestionImpl> questions = new LinkedHashMap<String, QuestionImpl>();

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintQuestions(Date stale)
	{
		// find them
		List<String> delete = new ArrayList<String>();
		for (QuestionImpl question : this.questions.values())
		{
			if (question.getMint() && question.getCreatedBy().getDate().before(stale))
			{
				delete.add(question.getId());
			}
		}

		// remove them
		for (String id : delete)
		{
			this.questions.remove(id);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void copyPoolQuestions(String userId, Pool source, Pool destination, boolean asHistory)
	{
		List<QuestionImpl> questions = new ArrayList<QuestionImpl>(this.questions.values());
		for (QuestionImpl question : questions)
		{
			if (!question.getMint() && question.getPool().equals(source))
			{
				QuestionImpl q = new QuestionImpl(question);

				// set the destination as the pool
				q.setPool(destination);

				// clear the id to make it new
				q.id = null;

				Date now = new Date();

				// set the new created and modified info
				q.getCreatedBy().setUserId(userId);
				q.getCreatedBy().setDate(now);
				q.getModifiedBy().setUserId(userId);
				q.getModifiedBy().setDate(now);

				if (asHistory) q.makeHistorical();

				// save
				saveQuestion(q);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countContextQuestions(String context)
	{
		int count = 0;
		for (QuestionImpl question : this.questions.values())
		{
			if (question.getIsHistorical()) continue;
			if (question.getMint()) continue;
			if (!question.getContext().equals(context)) continue;

			count++;
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPoolQuestions(Pool pool)
	{
		int count = 0;
		for (QuestionImpl question : this.questions.values())
		{
			if (question.getMint()) continue;
			if (!question.getPool().equals(pool)) continue;

			count++;
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Integer> countPoolQuestions(String context)
	{
		Map<String, Integer> rv = new HashMap<String, Integer>();
		List<Pool> pools = this.poolService.findPools(context, null, null);
		for (Pool pool : pools)
		{
			if (!pool.getIsHistorical())
			{
				rv.put(pool.getId(), countPoolQuestions(pool));
			}
		}

		return rv;
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

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<QuestionImpl> findContextQuestions(String context, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum,
			Integer pageSize)
	{
		return findQuestions(context, null, sort, questionType, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<QuestionImpl> findPoolQuestions(Pool pool, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum,
			Integer pageSize)
	{
		return findQuestions(null, pool, sort, questionType, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getPoolQuestions(Pool pool)
	{
		List<String> rv = new ArrayList<String>();
		for (QuestionImpl question : this.questions.values())
		{
			if ((!question.getMint()) && (question.getPool().equals(pool)))
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
		QuestionImpl rv = new QuestionImpl(poolService, questionService, submissionService);
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
	public void removeQuestion(QuestionImpl question)
	{
		QuestionImpl q = this.questions.remove(question.getId());
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
	 * {@inheritDoc}
	 */
	public void setPool(Question question, Pool pool)
	{
		QuestionImpl q = this.questions.get(question.getId());
		if (q != null)
		{
			q.initPoolId((pool == null) ? null : pool.getId());
		}
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

	/**
	 * Dependency: SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}

	protected void fakeIt()
	{
		// if we have not set up our questions, do so now
		if (!fakedAlready)
		{
			fakedAlready = true;

			Date now = new Date();

			QuestionImpl q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q1");
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
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:TrueFalse");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q2");
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
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:MultipleChoice");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q3");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Which value will it be?");
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setSingleCorrect("FALSE");
			List<String> answerChoices = new ArrayList<String>();
			answerChoices.add("This is the first item");
			answerChoices.add("This is the second item");
			answerChoices.add("This is the third item");
			answerChoices.add("This is the fourth item");
			Set<Integer> correctAnswers = new HashSet<Integer>();
			correctAnswers.add(new Integer(0));
			correctAnswers.add(new Integer(1));
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setAnswerChoices(answerChoices);
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setShuffleChoices("TRUE");
			((MultipleChoiceQuestionImpl) q.getTypeSpecificQuestion()).setCorrectAnswerSet(correctAnswers);
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question three.");
			q.setFeedback("feedback for question 3");
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:LikertScale");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q4");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Is this needed?");
			((LikertScaleQuestionImpl) q.getTypeSpecificQuestion()).setScale("2");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setFeedback("feedback for question 4");
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:FillBlanks");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q5");
			q.setExplainReason(Boolean.TRUE);
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			((FillBlanksQuestionImpl) q.getTypeSpecificQuestion()).setText("Roses are {red} and violets are {blue}.");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:Match");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q6");
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Match the following");
			((MatchQuestionImpl) q.getTypeSpecificQuestion()).addPair("First choice", "First Match");
			((MatchQuestionImpl) q.getTypeSpecificQuestion()).addPair("Second choice", "Second Match");
			((MatchQuestionImpl) q.getTypeSpecificQuestion()).addPair("Third choice", "Third Match");
			((MatchQuestionImpl) q.getTypeSpecificQuestion()).setDistractor("Distractor");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question six.");
			q.setFeedback("feedback for question 6");
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:Essay");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q7");
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Tell me a little bit about yourself.");
			((EssayQuestionImpl) q.getTypeSpecificQuestion()).setModelAnswer("I need more space, this space is too short.");
			((EssayQuestionImpl) q.getTypeSpecificQuestion()).setSubmissionType(EssayQuestionImpl.SubmissionType.both);
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question seven.");
			q.setFeedback("feedback for question 7");
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);

			q = newQuestion();
			q.initType("mneme:Task");
			q.initTypeSpecificQuestion(mnemeService.getQuestionPlugin(q.getType()).newQuestion(q));
			q.initId("q8");
			q.setPool(poolService.getPool("b1"));
			q.getCreatedBy().setUserId("admin");
			q.getPresentation().setText("Do this presentation and discuss it in class.");
			((TaskQuestionImpl) q.getTypeSpecificQuestion()).setModelAnswer("Review tutorial 5.");
			q.getCreatedBy().setUserId("admin");
			q.getCreatedBy().setDate(now);
			q.getModifiedBy().setUserId("admin");
			q.getModifiedBy().setDate(now);
			q.setHints("hints for question eight.");
			q.setFeedback("feedback for question 8");
			q.clearChanged();
			q.clearMint();
			questions.put(q.getId(), q);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected List<QuestionImpl> findQuestions(String context, Pool pool, final QuestionService.FindQuestionsSort sort, String questionType,
			Integer pageNum, Integer pageSize)
	{
		if (context == null && pool == null) throw new IllegalArgumentException();
		if (context != null && pool != null) throw new IllegalArgumentException();

		fakeIt();

		List<QuestionImpl> rv = new ArrayList<QuestionImpl>();
		for (QuestionImpl question : this.questions.values())
		{
			// skip historical unless looking at a specific pool
			if (question.getIsHistorical() && (pool == null)) continue;
			if (question.getMint()) continue;
			if ((questionType != null) && (!question.getType().equals(questionType))) continue;
			if ((pool != null) && (!question.getPool().equals(pool))) continue;
			if ((context != null) && (!question.getContext().equals(context))) continue;

			rv.add(new QuestionImpl(question));
		}

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				QuestionService.FindQuestionsSort secondary = null;
				switch (sort)
				{
					case type_a:
					case type_d:
					{
						// compare based on the localized type name
						rv = -1
								* ((Question) arg0).getTypeSpecificQuestion().getPlugin().getPopularity().compareTo(
										((Question) arg1).getTypeSpecificQuestion().getPlugin().getPopularity());
						if (rv == 0)
						{
							rv = ((Question) arg0).getTypeName().compareTo(((Question) arg1).getTypeName());
						}
						secondary = QuestionService.FindQuestionsSort.description_a;
						break;
					}
					case description_a:
					case description_d:
					{
						String s0 = StringUtil.trimToZero(((Question) arg0).getDescription());
						String s1 = StringUtil.trimToZero(((Question) arg1).getDescription());
						rv = s0.compareToIgnoreCase(s1);
						secondary = QuestionService.FindQuestionsSort.cdate_a;
						break;
					}
					case pool_difficulty_a:
					case pool_difficulty_d:
					{
						rv = ((Question) arg0).getPool().getDifficulty().compareTo(((Question) arg1).getPool().getDifficulty());
						secondary = QuestionService.FindQuestionsSort.description_a;
						break;
					}
					case pool_points_a:
					case pool_points_d:
					{
						rv = ((Question) arg0).getPool().getPoints().compareTo(((Question) arg1).getPool().getPoints());
						secondary = QuestionService.FindQuestionsSort.description_a;
						break;
					}
					case pool_title_a:
					case pool_title_d:
					{
						String s0 = StringUtil.trimToZero(((Question) arg0).getPool().getTitle());
						String s1 = StringUtil.trimToZero(((Question) arg1).getPool().getTitle());
						rv = s0.compareToIgnoreCase(s1);
						secondary = QuestionService.FindQuestionsSort.description_a;
						break;
					}
					case cdate_a:
					case cdate_d:
					{
						rv = ((Question) arg0).getCreatedBy().getDate().compareTo(((Question) arg1).getCreatedBy().getDate());
						break;
					}
				}

				// kick in the secondary if needed
				QuestionService.FindQuestionsSort third = null;
				if ((rv == 0) && (secondary != null))
				{
					switch (secondary)
					{
						case description_a:
						case description_d:
						{
							String s0 = StringUtil.trimToZero(((Question) arg0).getDescription());
							String s1 = StringUtil.trimToZero(((Question) arg1).getDescription());
							rv = s0.compareToIgnoreCase(s1);

							third = QuestionService.FindQuestionsSort.cdate_a;
							break;
						}
						case cdate_a:
						case cdate_d:
						{
							rv = ((Question) arg0).getCreatedBy().getDate().compareTo(((Question) arg1).getCreatedBy().getDate());
							break;
						}
					}
				}

				// third sort
				if ((rv == 0) && (third != null))
				{
					switch (third)
					{
						case cdate_a:
						case cdate_d:
						{
							rv = ((Question) arg0).getCreatedBy().getDate().compareTo(((Question) arg1).getCreatedBy().getDate());
							break;
						}
					}
				}

				return rv;
			}
		});

		// reverse if descending
		switch (sort)
		{
			case cdate_d:
			case description_d:
			case pool_difficulty_d:
			case pool_points_d:
			case pool_title_d:
			case type_d:
			{
				Collections.reverse(rv);
			}
		}

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
}
