/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolGetService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionGetService;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.SubmissionUnscoredQuestionService;

/**
 * Test Question.
 */
public class QuestionTest extends TestCase
{
	public class MyMnemeService implements MnemeService
	{
		/**
		 * {@inheritDoc}
		 */
		public QuestionPlugin getQuestionPlugin(String type)
		{
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public List<QuestionPlugin> getQuestionPlugins()
		{
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public void registerQuestionPlugin(QuestionPlugin plugin)
		{
			// TODO Auto-generated method stub

		}
	}

	public class MyPoolGetService implements PoolGetService
	{
		/**
		 * {@inheritDoc}
		 */
		public Pool getPool(String poolId)
		{
			PoolImpl rv = new PoolImpl();
			rv.initId(poolId);
			rv.setContext("context");
			return rv;
		}
	}

	public class MyQuestionGetService implements QuestionGetService
	{
		/**
		 * {@inheritDoc}
		 */
		public Question getQuestion(String questionId)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class MySubmissionUnscoredQuestionService implements SubmissionUnscoredQuestionService
	{
		/**
		 * {@inheritDoc}
		 */
		public Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(QuestionTest.class);

	protected MnemeService mnemeService = null;

	protected PoolGetService poolGetService = null;

	protected QuestionImpl question = null;

	protected QuestionGetService questionGetService = null;

	protected SubmissionUnscoredQuestionService submissionUnscoredQuestionService = null;

	/**
	 * @param arg0
	 */
	public QuestionTest(String arg0)
	{
		super(arg0);
	}

	public void testAttribution1() throws Exception
	{
		Date now = new Date();
		final String TESTER = "tester";

		question.getCreatedBy().setDate(now);
		question.getCreatedBy().setUserId(TESTER);
		assertTrue(question.getCreatedBy().getDate().equals(now));
		assertTrue(question.getCreatedBy().getUserId().equals(TESTER));

		// too long (length=100)
		final String OVERLONG = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		final String JUSTRIGHT = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

		question.getCreatedBy().setUserId(JUSTRIGHT);
		assertTrue(question.getCreatedBy().getUserId().equals(JUSTRIGHT));

		try
		{
			question.getCreatedBy().setUserId(OVERLONG);
			fail("expected illegal argument");
		}
		catch (IllegalArgumentException e)
		{
		}
		assertTrue(question.getCreatedBy().getUserId().equals(JUSTRIGHT));

		question.getCreatedBy().setDate(null);
		question.getCreatedBy().setUserId(null);
		assertTrue(question.getCreatedBy().getDate() == null);
		assertTrue(question.getCreatedBy().getUserId() == null);
	}

	public void testAttribution2() throws Exception
	{
		Date now = new Date();
		final String TESTER = "tester";

		question.getModifiedBy().setDate(now);
		question.getModifiedBy().setUserId(TESTER);
		assertTrue(question.getModifiedBy().getDate().equals(now));
		assertTrue(question.getModifiedBy().getUserId().equals(TESTER));

		// too long (length=100)
		final String OVERLONG = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		final String JUSTRIGHT = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

		question.getModifiedBy().setUserId(JUSTRIGHT);
		assertTrue(question.getModifiedBy().getUserId().equals(JUSTRIGHT));

		try
		{
			question.getModifiedBy().setUserId(OVERLONG);
			fail("expected illegal argument");
		}
		catch (IllegalArgumentException e)
		{
		}
		assertTrue(question.getModifiedBy().getUserId().equals(JUSTRIGHT));

		question.getModifiedBy().setDate(null);
		question.getModifiedBy().setUserId(null);
		assertTrue(question.getModifiedBy().getDate() == null);
		assertTrue(question.getModifiedBy().getUserId() == null);
	}

	/**
	 * Test the context
	 * 
	 * @throws Exception
	 */
	public void testContext() throws Exception
	{
		// normal
		final String CONTEXT_NORMAL = "contextX";
		question.initContext(CONTEXT_NORMAL);
		assertTrue(question.getContext().equals(CONTEXT_NORMAL));

		Pool pool = poolGetService.getPool("1");
		question.setPool(pool);
		assertTrue(question.getContext().equals("context"));
	}

	public void testGetDescription() throws Exception
	{
		// String[] data = new String[0];
		// question.getTypeSpecificQuestion().setData(data);
		question.getPresentation().setText(null);
		assertTrue(question.getDescription() == null);

		question.getPresentation().setText("");
		assertTrue(question.getDescription() != null);
		assertTrue(question.getDescription().equals(""));

		question.getPresentation().setText("the question");
		assertTrue(question.getDescription().equals("the question"));

		question.getPresentation().setText("  title   ");
		assertTrue(question.getDescription().equals("  title   "));

		final String TITLE_OVERLONG = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		final String TITLE_JUSTRIGHT = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";

		question.getPresentation().setText(TITLE_OVERLONG);
		assertTrue(question.getDescription().equals(TITLE_JUSTRIGHT));

		question.getPresentation().setText("");
		assertTrue(question.getDescription().equals(""));
		question.getPresentation().setText(TITLE_JUSTRIGHT);
		assertTrue(question.getDescription().equals(TITLE_JUSTRIGHT));
		
		question.getPresentation().setText("<p>this is some <b>html</b></p>");
		assertTrue(question.getDescription().equals("this is some html"));
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		questionGetService = new MyQuestionGetService();
		poolGetService = new MyPoolGetService();
		submissionUnscoredQuestionService = new MySubmissionUnscoredQuestionService();
		mnemeService = new MyMnemeService();

		// plugins
		TrueFalsePlugin tf = new TrueFalsePlugin();
		tf.setBundle("mnemeTrueFalse");
		tf.setMnemeService(mnemeService);
		tf.init();

		TaskPlugin tk = new TaskPlugin();
		tk.setBundle("mnemeTask");
		tk.setMnemeService(mnemeService);
		tk.init();

		MultipleChoicePlugin mp = new MultipleChoicePlugin();
		mp.setBundle("mnemeMultipleChoice");
		mp.setMnemeService(mnemeService);
		mp.init();

		MatchPlugin mt = new MatchPlugin();
		mt.setBundle("mnemeMatch");
		mt.setMnemeService(mnemeService);
		mt.init();

		LikertScalePlugin lk = new LikertScalePlugin();
		lk.setBundle("mnemeLikertScale");
		lk.setMnemeService(mnemeService);
		lk.init();

		FillBlanksPlugin fb = new FillBlanksPlugin();
		fb.setBundle("mnemeFillBlanks");
		fb.setMnemeService(mnemeService);
		fb.init();

		EssayPlugin ey = new EssayPlugin();
		ey.setBundle("mnemeEssay");
		ey.setMnemeService(mnemeService);
		ey.init();

		// question
		QuestionImpl q = new QuestionImpl();
		q.setQuestionService(questionGetService);
		q.setPoolService(poolGetService);
		q.setSubmissionService(submissionUnscoredQuestionService);
		q.initTypeSpecificQuestion(tf.newQuestion(q));
		question = q;
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
}
