/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.dbcp.SakaiBasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.db.impl.BasicSqlService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.thread_local.impl.ThreadLocalComponent;

/**
 * Test QuestionStorage
 */
public abstract class QuestionStorageTestX extends TestCase
{
	public class MockFunctionManager implements FunctionManager
	{
		/**
		 * {@inheritDoc}
		 */
		public List getRegisteredFunctions()
		{
			return new ArrayList();
		}

		/**
		 * {@inheritDoc}
		 */
		public List getRegisteredFunctions(String prefix)
		{
			return new ArrayList();
		}

		/**
		 * {@inheritDoc}
		 */
		public void registerFunction(String function)
		{
		}
	}

	public class SqlServiceTest extends BasicSqlService
	{
		ThreadLocalManager tlm = null;

		protected void setThreadLocalManager(ThreadLocalManager tlm)
		{
			this.tlm = tlm;
		}

		protected ThreadLocalManager threadLocalManager()
		{
			return tlm;
		}

		protected UsageSessionService usageSessionService()
		{
			// TODO: might have to mock
			return null;
		}
	}

	/** Logger. */
	private static final Log log = LogFactory.getLog(QuestionStorageTestX.class);

	protected final static String CONTEXT = "JUNIT_TEST_JUNIT";

	protected SqlServiceTest sqlService = null;

	protected QuestionStorage storage = null;

	protected ThreadLocalComponent thread_localManager = null;

	protected MnemeServiceImpl mnemeService = null;

	/**
	 * @param arg0
	 */
	public QuestionStorageTestX(String arg0)
	{
		super(arg0);
	}

	/**
	 * Test clearStaleMintQuestions()
	 * 
	 * @throws Exception
	 */
	public void test001clearStaleMintQuestions() throws Exception
	{
		// create a question - leave it mint - make it old
		Date old = new Date(new Date().getTime() - (2 * 24 * 60 * 60 * 1000));
		QuestionImpl question = this.storage.newQuestion();
		question.initContext(CONTEXT);
		question.initPoolId("0");
		question.getCreatedBy().setDate(old);
		question.getCreatedBy().setUserId("admin");
		question.getModifiedBy().setDate(old);
		question.getModifiedBy().setUserId("admin");
		this.storage.saveQuestion(question);

		// it should now exist
		Boolean exists = this.storage.existsQuestion(question.getId());
		assertTrue(exists == Boolean.TRUE);

		// this should leave the pool in place
		this.storage.clearStaleMintQuestions(old);

		// it should now exist
		exists = this.storage.existsQuestion(question.getId());
		assertTrue(exists == Boolean.TRUE);

		// this should remove it
		this.storage.clearStaleMintQuestions(new Date());

		// it should not exist
		exists = this.storage.existsQuestion(question.getId());
		assertTrue(exists == Boolean.FALSE);
	}

	/**
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		// the thread local manager
		ThreadLocalComponent tl = new ThreadLocalComponent();
		tl.init();
		thread_localManager = tl;

		// mneme service
		MnemeServiceImpl m = new MnemeServiceImpl();
		m.setTimeoutCheckSeconds("0");
		m.setFunctionManager(new MockFunctionManager());
		m.init();
		mnemeService = m;

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

		SakaiBasicDataSource ds = setupDataSource();
		if (ds != null)
		{
			// the SqlService
			SqlServiceTest sql = new SqlServiceTest();
			sql.setVendor(vendor());
			sql.setDefaultDataSource(ds);
			sql.setThreadLocalManager(thread_localManager);
			sql.init();
			sqlService = sql;
		}

		// finally, our target...
		QuestionStorage s = setupQuestionStorage();
		storage = s;

		// clean up from any prior tests
		storage.clearContext(CONTEXT);
	}

	protected abstract SakaiBasicDataSource setupDataSource();

	protected abstract QuestionStorage setupQuestionStorage();

	/**
	 */
	protected void tearDown() throws Exception
	{
		// clean up from any prior tests
		storage.clearContext(CONTEXT);

		teardownQuestionStorage();

		if (mnemeService != null) mnemeService.destroy();
		if (sqlService != null) sqlService.destroy();
		if (thread_localManager != null) thread_localManager.destroy();

		super.tearDown();
	}

	protected abstract void teardownQuestionStorage();

	protected abstract String vendor();
}
