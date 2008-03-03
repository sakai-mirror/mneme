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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Pool;

/**
 * QuestionStorageMysql implements QuestionStorage for Oracle.
 */
public abstract class QuestionStorageOracle extends QuestionStorageSql implements QuestionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(QuestionStorageOracle.class);

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
	 * copyPoolQuestions (transaction code)
	 */
	protected void copyPoolQuestionsHistoricalTx(String userId, Pool source, Pool destination)
	{
		Date now = new Date();

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION");
		sql.append(" (ID, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" SURVEY, TYPE, VALID, GUEST)");
		sql.append(" SELECT MNEME_QUESTION_SEQ.NEXTVAL, REST.* FROM (SELECT");
		sql.append(" '" + destination.getContext() + "' REST_CONTEXT, " + now.getTime() + " REST_CREATED, '" + userId + "' REST_CUSER,");
		sql.append(" Q.DESCRIPTION, Q.EXPLAIN_REASON, Q.FEEDBACK, Q.HINTS, '1', Q.MINT,");
		sql.append(" " + now.getTime() + " REST_MODIFIED, '" + userId + "' REST_MUSER, " + destination.getId() + " REST_PID,");
		sql.append(" Q.PRESENTATION_TEXT, Q.SURVEY, Q.TYPE, Q.VALID, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q WHERE Q.MINT='0' AND Q.HISTORICAL IN ('0','1') AND Q.POOL_ID=? ORDER BY Q.ID ASC) REST");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(source.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("copyPoolQuestionsTx: dbWrite failed");
		}
	}

	/**
	 * copyPoolQuestions (transaction code)
	 */
	protected void copyPoolQuestionsTx(String userId, Pool source, Pool destination)
	{
		Date now = new Date();

		// Note: adding the order by (?) gave an oracle error ORA-02287, leading to the REST 
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION");
		sql.append(" (ID, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" SURVEY, TYPE, VALID, GUEST)");
		sql.append(" SELECT MNEME_QUESTION_SEQ.NEXTVAL, REST.* FROM (SELECT");
		sql.append(" '" + destination.getContext() + "' REST_CONTEXT, " + now.getTime() + " REST_CREATED, '" + userId + "' REST_CUSER,");
		sql.append(" Q.DESCRIPTION, Q.EXPLAIN_REASON, Q.FEEDBACK, Q.HINTS, Q.HISTORICAL, Q.MINT,");
		sql.append(" " + now.getTime() + " REST_MODIFIED, '" + userId + "' REST_MUSER, " + destination.getId() + " REST_PID,");
		sql.append(" Q.PRESENTATION_TEXT, Q.SURVEY, Q.TYPE, Q.VALID, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q WHERE Q.MINT='0' AND Q.HISTORICAL IN ('0','1') AND Q.POOL_ID=? ORDER BY Q.ID ASC) REST");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(source.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("copyPoolQuestionsTx: dbWrite failed");
		}
	}

	/**
	 * Insert a new question as a copy of another question, marked as history (copyPoolQuestions transaction code).
	 * 
	 * @param userId
	 *        The user id.
	 * @param qid
	 *        The source question id.
	 * @param destination
	 *        The pool for the new question.
	 */
	protected String copyQuestionHistoricalTx(String userId, String qid, Pool destination)
	{
		Date now = new Date();

		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_QUESTION_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION");
		sql.append(" (ID, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" SURVEY, TYPE, VALID, GUEST)");
		sql.append(" SELECT " + id + ",");
		sql.append(" '" + destination.getContext() + "', " + now.getTime() + ", '" + userId + "',");
		sql.append(" Q.DESCRIPTION, Q.EXPLAIN_REASON, Q.FEEDBACK, Q.HINTS, '1', Q.MINT,");
		sql.append(" '" + now.getTime() + "', '" + userId + "', " + destination.getId() + ",");
		sql.append(" Q.PRESENTATION_TEXT, Q.SURVEY, Q.TYPE, Q.VALID, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q WHERE Q.ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(qid);

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("copyQuestionTx: dbWrite failed");
		}

		return id.toString();
	}

	/**
	 * Insert a new question as a copy of another question (copyPoolQuestions transaction code).
	 * 
	 * @param userId
	 *        The user id.
	 * @param qid
	 *        The source question id.
	 * @param destination
	 *        The pool for the new question.
	 */
	protected String copyQuestionTx(String userId, String qid, Pool destination)
	{
		Date now = new Date();

		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_QUESTION_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION");
		sql.append(" (ID, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" SURVEY, TYPE, VALID, GUEST)");
		sql.append(" SELECT " + id + ",");
		sql.append(" '" + destination.getContext() + "', " + now.getTime() + ", '" + userId + "',");
		sql.append(" Q.DESCRIPTION, Q.EXPLAIN_REASON, Q.FEEDBACK, Q.HINTS, Q.HISTORICAL, Q.MINT,");
		sql.append(" '" + now.getTime() + "', '" + userId + "', " + destination.getId() + ",");
		sql.append(" Q.PRESENTATION_TEXT, Q.SURVEY, Q.TYPE, Q.VALID, Q.GUEST");
		sql.append(" FROM MNEME_QUESTION Q WHERE Q.ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(qid);

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("copyQuestionTx: dbWrite failed");
		}

		return id.toString();
	}

	/**
	 * Insert a new question (transaction code).
	 * 
	 * @param question
	 *        The question.
	 */
	protected void insertQuestionTx(QuestionImpl question)
	{
		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_QUESTION_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_QUESTION (ID,");
		sql.append(" CONTEXT, CREATED_BY_DATE, CREATED_BY_USER, DESCRIPTION, EXPLAIN_REASON, FEEDBACK,");
		sql.append(" HINTS, HISTORICAL, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER, POOL_ID, PRESENTATION_TEXT,");
		sql.append(" SURVEY, TYPE, VALID, GUEST )");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[18];
		fields[0] = id;
		fields[1] = question.getContext();
		fields[2] = question.getCreatedBy().getDate().getTime();
		fields[3] = question.getCreatedBy().getUserId();
		fields[4] = limit(question.getDescription(), 255);
		fields[5] = question.getExplainReason() ? "1" : "0";
		fields[6] = question.getFeedback();
		fields[7] = question.getHints();
		fields[8] = question.getIsHistorical() ? "1" : "0";
		fields[9] = question.getMint() ? "1" : "0";
		fields[10] = question.getModifiedBy().getDate().getTime();
		fields[11] = question.getModifiedBy().getUserId();
		fields[12] = (question.poolId == null) ? null : Long.valueOf(question.poolId);
		fields[13] = question.getPresentation().getText();
		fields[14] = question.getIsSurvey() ? "1" : "0";
		fields[15] = question.getType();
		fields[16] = question.getIsValid() ? "1" : "0";
		fields[17] = SqlHelper.encodeStringArray(question.getTypeSpecificQuestion().getData());

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("insertQuestionTx: dbWrite failed");
		}

		// set the question's id
		question.initId(id.toString());
	}
}
