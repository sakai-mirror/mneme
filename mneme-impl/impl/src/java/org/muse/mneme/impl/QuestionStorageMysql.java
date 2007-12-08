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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.util.StringUtil;

/**
 * QuestionStorageMysql handles storage for questions under MySQL.
 */
public class QuestionStorageMysql implements QuestionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionStorageMysql.class);

	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Dependency: QuestionService */
	protected QuestionServiceImpl questionService = null;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService submissionService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintQuestions(final Date stale)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				clearStaleMintQuestionsTx(stale);
			}
		}, "clearStaleMintQuestions: " + stale.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	public void copyPoolQuestions(final String userId, final Pool source, final Pool destination)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				copyPoolQuestionsTx(userId, source, destination);
			}
		}, "copyPoolQuestions: " + source.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countContextQuestions(String context)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_QUESTION Q");
		sql.append(" LEFT OUTER JOIN MNEME_POOL P ON Q.POOL_ID=P.ID");
		sql.append(" WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND P.CONTEXT=?");
		Object[] fields = new Object[1];
		fields[0] = context;
		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			return Integer.valueOf((String) results.get(0));
		}

		return Integer.valueOf(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPoolQuestions(Pool pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_QUESTION Q");
		sql.append(" WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND Q.POOL_ID=?");
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(pool.getId());
		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			return Integer.valueOf((String) results.get(0));
		}

		return Integer.valueOf(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Integer> countPoolQuestions(String context)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT P.ID, COUNT(Q.ID)");
		sql.append(" FROM MNEME_POOL P");
		sql.append(" LEFT OUTER JOIN MNEME_QUESTION Q");
		sql.append(" ON P.ID=Q.POOL_ID AND P.CONTEXT=? AND P.MINT='0' AND P.HISTORICAL='0' AND Q.MINT='0'");
		sql.append(" GROUP BY P.ID");

		Object[] fields = new Object[1];
		fields[0] = context;

		final Map<String, Integer> rv = new HashMap<String, Integer>();
		List results = this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String id = StringUtil.trimToNull(result.getString(1));
					Integer count = Integer.valueOf(result.getInt(2));
					rv.put(id, count);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("countPoolQuestions: " + e);
					return null;
				}
			}
		});

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
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_QUESTION Q");
		sql.append(" WHERE Q.ID=?");
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			int size = Integer.parseInt((String) results.get(0));
			return Boolean.valueOf(size == 1);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<QuestionImpl> findContextQuestions(String context, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum,
			Integer pageSize)
	{
		// the where and order by
		StringBuilder whereOrder = new StringBuilder();
		whereOrder.append("LEFT OUTER JOIN MNEME_POOL P ON Q.POOL_ID=P.ID WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND P.CONTEXT=?"
				+ ((questionType != null) ? " AND Q.TYPE=?" : "") + " ORDER BY ");
		whereOrder.append(sortToSql(sort));

		Object[] fields = new Object[(questionType == null) ? 1 : 2];
		fields[0] = context;
		if (questionType != null)
		{
			fields[1] = questionType;
		}

		List<QuestionImpl> rv = readQuestions(whereOrder.toString(), fields);

		// TODO: page in the SQL...
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
	public List<QuestionImpl> findPoolQuestions(Pool pool, QuestionService.FindQuestionsSort sort, String questionType, Integer pageNum,
			Integer pageSize)
	{
		// the where and order by
		StringBuilder whereOrder = new StringBuilder();
		whereOrder.append("LEFT OUTER JOIN MNEME_POOL P ON Q.POOL_ID=P.ID WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND Q.POOL_ID=?"
				+ ((questionType != null) ? " AND Q.TYPE=?" : "") + " ORDER BY ");
		whereOrder.append(sortToSql(sort));

		Object[] fields = new Object[(questionType == null) ? 1 : 2];
		fields[0] = Long.valueOf(pool.getId());
		if (questionType != null)
		{
			fields[1] = questionType;
		}

		List<QuestionImpl> rv = readQuestions(whereOrder.toString(), fields);

		// TODO: page in the SQL...
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
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT Q.ID");
		sql.append(" FROM MNEME_QUESTION Q ");
		sql.append(" WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND Q.POOL_ID=?");
		sql.append(" ORDER BY CREATED_BY_DATE ASC");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(pool.getId());

		return new ArrayList<String>(this.sqlService.dbRead(sql.toString(), fields, null));
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionImpl getQuestion(String id)
	{
		return readQuestion(id);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_question");
		}

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void moveQuestion(final Question question, final Pool pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				moveQuestionTx(question, pool);
			}
		}, "moveQuestion: question: " + question.getId() + " pool: " + pool.getId());
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
		deleteQuestion(question);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(QuestionImpl question)
	{
		// for new questions
		if (question.getId() == null)
		{
			insertQuestion(question);
		}

		// for existing questions
		else
		{
			updateQuestion(question);
		}
	}

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		autoDdl = new Boolean(value).booleanValue();
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
	public void setQuestionService(QuestionServiceImpl service)
	{
		this.questionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
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

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		threadLocalManager = service;
	}

	/**
	 * Transaction code for clearStaleMintQuestions()
	 */
	protected void clearStaleMintQuestionsTx(Date stale)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_QUESTION");
		sql.append(" WHERE MINT='1' AND CREATED_BY_DATE < ?");

		Object[] fields = new Object[1];
		fields[0] = stale.getTime();

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("clearStaleMintQuestionsTx: db write failed");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void copyPoolQuestionsTx(String userId, Pool source, Pool destination)
	{
		Date now = new Date();

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION");
		sql.append(" (CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" TYPE, GUEST)");
		sql.append(" SELECT");
		sql.append(" '" + destination.getContext() + "', " + now.getTime() + ", '" + userId + "',");
		sql.append(" Q.DESCRIPTION, Q.EXPLAIN_REASON, Q.FEEDBACK, Q.HINTS, Q.HISTORICAL, Q.MINT,");
		sql.append(" '" + now.getTime() + "', '" + userId + "', " + destination.getId() + ",");
		sql.append(" Q.PRESENTATION_TEXT, Q.TYPE, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q WHERE Q.MINT='0' AND Q.HISTORICAL='0' AND Q.POOL_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(source.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("copyPoolQuestionsTx: db write failed");
		}
	}

	/**
	 * Delete a question.
	 * 
	 * @param question
	 *        The question.
	 */
	protected void deleteQuestion(final QuestionImpl question)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				deleteQuestionTx(question);
			}
		}, "deleteQuestion: " + question.getId());
	}

	/**
	 * Delete a question (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void deleteQuestionTx(QuestionImpl question)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_QUESTION");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteQuestionTx: db write failed");
		}
	}

	/**
	 * Insert a new question.
	 * 
	 * @param question
	 *        The question.
	 */
	protected void insertQuestion(final QuestionImpl question)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertQuestionTx(question);
			}
		}, "insertQuestion: " + question.getId());
	}

	/**
	 * Insert a new question (transaction code).
	 * 
	 * @param question
	 *        The question.
	 */
	protected void insertQuestionTx(QuestionImpl question)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION (");
		sql.append(" CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" TYPE, GUEST )");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[15];
		fields[0] = question.getContext();
		fields[1] = question.getCreatedBy().getDate().getTime();
		fields[2] = question.getCreatedBy().getUserId();
		fields[3] = limit(question.getDescription(), 255);
		fields[4] = question.getExplainReason() ? "1" : "0";
		fields[5] = question.getFeedback();
		fields[6] = question.getHints();
		fields[7] = question.getIsHistorical() ? "1" : "0";
		fields[8] = question.getMint() ? "1" : "0";
		fields[9] = question.getModifiedBy().getDate().getTime();
		fields[10] = question.getModifiedBy().getUserId();
		fields[11] = (question.poolId == null) ? null : Long.valueOf(question.poolId);
		fields[12] = question.getPresentation().getText();
		fields[13] = question.getType();
		fields[14] = SqlHelper.encodeStringArray(question.getTypeSpecificQuestion().getData());

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertQuestionTx: db write failed");
		}

		// set the question's id
		question.initId(id.toString());
	}

	/**
	 * If the string is non-nul and longer than length, return a trimmed version, else return it.
	 * 
	 * @param value
	 *        The value to work on.
	 * @param length
	 *        The maximum length.
	 * @return The value trimmed to the maximum length, or unchanged if null or shorter than that maximum.
	 */
	protected String limit(String value, int length)
	{
		if (value == null) return null;
		if (value.length() > length)
		{
			return value.substring(0, length);
		}
		return value;
	}

	/**
	 * Transaction code for moveQuestion()
	 */
	protected void moveQuestionTx(Question question, Pool pool)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_QUESTION SET");
		sql.append(" POOL_ID=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(pool.getId());
		fields[1] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("moveQuestionTx: db write failed");
		}
	}

	/**
	 * Read a question
	 * 
	 * @param id
	 *        The question id.
	 * @return The question.
	 */
	protected QuestionImpl readQuestion(String id)
	{
		String whereOrder = "WHERE Q.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List<QuestionImpl> rv = readQuestions(whereOrder, fields);
		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Read a selection of questions.
	 * 
	 * @param whereOrder
	 *        The WHERE and ORDER BY sql clauses
	 * @param fields
	 *        The bind variables.
	 * @return The questions.
	 */
	protected List<QuestionImpl> readQuestions(String whereOrder, Object[] fields)
	{
		final List<QuestionImpl> rv = new ArrayList<QuestionImpl>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT Q.CONTEXT, Q.CREATED_BY_DATE, Q.CREATED_BY_USER, Q.EXPLAIN_REASON, Q.FEEDBACK,");
		sql.append(" Q.HINTS, Q.HISTORICAL, Q.ID, Q.MINT, Q.MODIFIED_BY_DATE, Q.MODIFIED_BY_USER, Q.POOL_ID,");
		sql.append(" Q.PRESENTATION_TEXT, Q.TYPE, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q ");
		sql.append(whereOrder);

		final QuestionServiceImpl qService = this.questionService;
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					QuestionImpl question = newQuestion();
					question.initContext(StringUtil.trimToNull(result.getString(1)));
					question.getCreatedBy().setDate(new Date(result.getLong(2)));
					question.getCreatedBy().setUserId(StringUtil.trimToNull(result.getString(3)));
					question.setExplainReason(Boolean.valueOf("1".equals(StringUtil.trimToNull(result.getString(4)))));
					question.setFeedback(StringUtil.trimToNull(result.getString(5)));
					question.setHints(StringUtil.trimToNull(result.getString(6)));
					question.initHistorical(Boolean.valueOf("1".equals(StringUtil.trimToNull(result.getString(7)))));
					question.initId(Long.toString(result.getLong(8)));
					question.initMint(Boolean.valueOf("1".equals(StringUtil.trimToNull(result.getString(9)))));
					question.getModifiedBy().setDate(new Date(result.getLong(10)));
					question.getModifiedBy().setUserId(StringUtil.trimToNull(result.getString(11)));
					long poolIdLong = result.getLong(12);
					question.initPool((poolIdLong == 0) ? null : Long.toString(poolIdLong));
					question.getPresentation().setText(StringUtil.trimToNull(result.getString(13)));
					qService.setType(StringUtil.trimToNull(result.getString(14)), question);
					question.getTypeSpecificQuestion().setData(SqlHelper.decodeStringArray(StringUtil.trimToNull(result.getString(15))));

					question.changed.clearChanged();
					rv.add(question);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readQuestions: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Convert a FindQuestionsSort to a sql sort clause
	 * 
	 * @param sort
	 *        The sort.
	 * @return The SQL.
	 */
	protected String sortToSql(QuestionService.FindQuestionsSort sort)
	{
		switch (sort)
		{
			case type_a:
			{
				// TODO: localized
				return "Q.TYPE ASC, Q.DESCRIPTION ASC, Q.CREATED_BY_DATE ASC";
			}
			case type_d:
			{
				// TODO: localized
				return "Q.TYPE DESC, Q.DESCRIPTION DESC, Q.CREATED_BY_DATE DESC";
			}
			case description_a:
			{
				return "Q.DESCRIPTION ASC, Q.CREATED_BY_DATE ASC";
			}
			case description_d:
			{
				return "Q.DESCRIPTION DESC, Q.CREATED_BY_DATE DESC";
			}
			case pool_difficulty_a:
			{
				return "P.DIFFICULTY ASC, Q.DESCRIPTION ASC, Q.CREATED_BY_DATE ASC";
			}
			case pool_difficulty_d:
			{
				return "P.DIFFICULTY DESC, Q.DESCRIPTION DESC, Q.CREATED_BY_DATE DESC";
			}
			case pool_points_a:
			{
				return "P.POINTS ASC, Q.DESCRIPTION ASC, Q.CREATED_BY_DATE ASC";
			}
			case pool_points_d:
			{
				return "P.POINTS DESC, Q.DESCRIPTION DESC, Q.CREATED_BY_DATE DESC";
			}
			case pool_title_a:
			{
				return "P.TITLE ASC, Q.DESCRIPTION ASC, Q.CREATED_BY_DATE ASC";
			}
			case pool_title_d:
			{
				return "P.TITLE DESC, Q.DESCRIPTION DESC, Q.CREATED_BY_DATE DESC";
			}
			case cdate_a:
			{
				return "Q.CREATED_BY_DATE ASC";
			}
			case cdate_d:
			{
				return "Q.CREATED_BY_DATE DESC";
			}
		}
		return "";
	}

	/**
	 * Update an existing pool.
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void updateQuestion(final QuestionImpl question)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateQuestionTx(question);
			}
		}, "updateQuestion: " + question.getId());
	}

	/**
	 * Update an existing pool (transaction code).
	 * 
	 * @param question
	 *        The pool.
	 */
	protected void updateQuestionTx(QuestionImpl question)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_QUESTION SET");
		sql.append(" CONTEXT=?, DESCRIPTION=?, EXPLAIN_REASON=?, FEEDBACK=?, HINTS=?, HISTORICAL=?,");
		sql.append(" MINT=?, MODIFIED_BY_DATE=?, MODIFIED_BY_USER=?, POOL_ID=?,");
		sql.append(" PRESENTATION_TEXT=?, GUEST=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[13];
		fields[0] = question.getContext();
		fields[1] = limit(question.getDescription(), 255);
		fields[2] = question.getExplainReason() ? "1" : "0";
		fields[3] = question.getFeedback();
		fields[4] = question.getHints();
		fields[5] = question.getIsHistorical() ? "1" : "0";
		fields[6] = question.getMint() ? "1" : "0";
		fields[7] = question.getModifiedBy().getDate().getTime();
		fields[8] = question.getModifiedBy().getUserId();
		fields[9] = (question.poolId == null) ? null : Long.valueOf(question.poolId);
		fields[10] = question.getPresentation().getText();
		fields[11] = SqlHelper.encodeStringArray(question.getTypeSpecificQuestion().getData());
		fields[12] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateQuestionTx: db write failed");
		}
	}
}
