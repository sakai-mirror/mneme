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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.ReviewTiming;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class AssessmentStorageMysql implements AssessmentStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentStorageMysql.class);

	/** Dependency: AssessmentService. */
	protected AssessmentService assessmentService = null;

	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;

	/** Messages bundle name. */
	protected String bundle = null;

	/** Messages. */
	protected transient InternationalizedMessages messages = null;

	/** Dependency: PoolService. */
	protected PoolService poolService = null;

	/** Dependency: QuestionService. */
	protected QuestionService questionService = null;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	protected SubmissionService submissionService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintAssessments(final Date stale)
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
	public Integer countAssessments(String context)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ASSESSMENT A");
		sql.append(" WHERE A.CONTEXT=? AND A.ARCHIVED='0' AND A.MINT='0' AND A.HISTORICAL='0'");
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
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean existsAssessment(String id)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ASSESSMENT A");
		sql.append(" WHERE A.ID=?");
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
	public List<AssessmentImpl> getArchivedAssessments(String context)
	{
		String where = "WHERE A.CONTEXT=? AND A.ARCHIVED='1' AND A.MINT='0' AND A.HISTORICAL='0'";
		String order = "ORDER BY DATES_ARCHIVED ASC";

		Object[] fields = new Object[1];
		fields[0] = context;

		return readAssessments(where, order.toString(), fields);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl getAssessment(String id)
	{
		return readAssessment(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<AssessmentImpl> getContextAssessments(String context, final AssessmentService.AssessmentsSort sort, Boolean publishedOnly)
	{
		String where = "WHERE A.CONTEXT=? AND A.ARCHIVED='0' AND A.MINT='0' AND A.HISTORICAL='0'";
		if (publishedOnly)
		{
			where += " AND A.PUBLISHED='1'";
		}

		// sort
		String order = null;
		switch (sort)
		{
			case published_a:
			{
				order = " ORDER BY A.PUBLISHED ASC, A.TITLE ASC, A.CREATED_BY_DATE ASC";
				break;
			}
			case published_d:
			{
				order = " ORDER BY A.PUBLISHED DESC, A.TITLE DESC, A.CREATED_BY_DATE DESC";
				break;
			}
			case title_a:
			{
				order = " ORDER BY A.TITLE ASC, A.CREATED_BY_DATE ASC";
				break;
			}
			case title_d:
			{
				order = " ORDER BY A.TITLE DESC, A.CREATED_BY_DATE DESC";
				break;
			}
			case type_a:
			{
				// TODO: getType().getSortValue()
				order = " ORDER BY A.TYPE ASC, A.TITLE ASC, A.CREATED_BY_DATE ASC";
				break;
			}
			case type_d:
			{
				// TODO: getType().getSortValue()
				order = " ORDER BY A.TYPE DESC, A.TITLE DESC, A.CREATED_BY_DATE DESC";
				break;
			}
			case odate_a:
			{
				// TODO: null sorts low
				order = " ORDER BY A.DATES_OPEN ASC, A.TITLE ASC, A.CREATED_BY_DATE ASC";
				break;
			}
			case odate_d:
			{
				// TODO: null sorts low
				order = " ORDER BY A.DATES_OPEN DESC, A.TITLE DESC, A.CREATED_BY_DATE DESC";
				break;
			}
			case ddate_a:
			{
				// TODO: null sorts high
				order = " ORDER BY A.DATES_DUE ASC, A.TITLE ASC, A.CREATED_BY_DATE ASC";
				break;
			}
			case ddate_d:
			{
				// TODO: null sorts high
				order = " ORDER BY A.DATES_DUE DESC, A.TITLE DESC, A.CREATED_BY_DATE DESC";
				break;
			}
			case cdate_a:
			{
				order = " ORDER BY A.CREATED_BY_DATE ASC";
				break;
			}
			case cdate_d:
			{
				order = " ORDER BY A.CREATED_BY_DATE DESC";
				break;
			}
		}

		Object[] fields = new Object[1];
		fields[0] = context;

		return readAssessments(where, order.toString(), fields);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_assessment");
		}

		// messages
		if (this.bundle != null) this.messages = new ResourceLoader(this.bundle);

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean liveDependencyExists(Pool pool, boolean directOnly)
	{
		if (directOnly)
		{
			// look for any pool_id that matches from D parts only (question_id is null) for live assessments
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT COUNT(1) FROM MNEME_ASSESSMENT_PART_DETAIL D");
			sql.append(" JOIN MNEME_ASSESSMENT A ON D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
			sql.append(" WHERE D.POOL_ID=? AND D.QUESTION_ID IS NULL");

			Object[] fields = new Object[1];
			fields[0] = Long.valueOf(pool.getId());

			List results = this.sqlService.dbRead(sql.toString(), fields, null);
			if (results.size() > 0)
			{
				int size = Integer.parseInt((String) results.get(0));
				return Boolean.valueOf(size > 0);
			}

			return Boolean.FALSE;
		}

		else
		{
			// look for any pool_id that matches or a question's pool_id that matches for live assessments
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT COUNT(1) FROM MNEME_ASSESSMENT_PART_DETAIL D");
			sql.append(" JOIN MNEME_ASSESSMENT A ON D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
			sql.append(" LEFT OUTER JOIN MNEME_QUESTION Q ON D.QUESTION_ID=Q.ID");
			sql.append(" WHERE D.POOL_ID=? OR (Q.POOL_ID=? AND D.POOL_ID IS NULL)");

			Object[] fields = new Object[2];
			fields[0] = Long.valueOf(pool.getId());
			fields[1] = fields[0];

			List results = this.sqlService.dbRead(sql.toString(), fields, null);
			if (results.size() > 0)
			{
				int size = Integer.parseInt((String) results.get(0));
				return Boolean.valueOf(size > 0);
			}

			return Boolean.FALSE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean liveDependencyExists(Question question)
	{
		// M parts only (question_id is set non-null): match the question_id field for live assessments
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ASSESSMENT_PART_DETAIL D");
		sql.append(" JOIN MNEME_ASSESSMENT A ON D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
		sql.append(" WHERE D.QUESTION_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(question.getId());

		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			int size = Integer.parseInt((String) results.get(0));
			return Boolean.valueOf(size > 0);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public void makeLive(final Assessment assessment)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				makeLiveTx(assessment);
			}
		}, "makeLive: " + assessment.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl newAssessment()
	{
		return new AssessmentImpl(this.assessmentService, this.poolService, this.questionService, this.submissionService, this.messages);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl newAssessment(AssessmentImpl assessment)
	{
		return new AssessmentImpl(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(AssessmentImpl assessment)
	{
		deleteAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeDependency(final Pool pool)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				removeDependencyTx(pool);
			}
		}, "removeDependency(pool): " + pool.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeDependency(final Question question)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				removeDependencyTx(question);
			}
		}, "removeDependency(question): " + question.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(AssessmentImpl assessment)
	{
		// for new assessments
		if (assessment.getId() == null)
		{
			insertAssessment(assessment);
		}

		// for existing assessments
		else
		{
			updateAssessment(assessment);
		}

		// clear changed
		assessment.clearChanged();
		for (AssessmentAccess access : assessment.getSpecialAccess().getAccess())
		{
			((AssessmentAccessImpl) access).clearChanged();
		}
		((AssessmentSpecialAccessImpl) assessment.getSpecialAccess()).clearDeleted();

		for (Part part : assessment.getParts().getParts())
		{
			((PartImpl) part).clearChanged();
		}
		((AssessmentPartsImpl) assessment.getParts()).clearDeleted();
	}

	/**
	 * Set the AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
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
	 * Set the message bundle.
	 * 
	 * @param bundle
	 *        The message bundle.
	 */
	public void setBundle(String name)
	{
		this.bundle = name;
	}

	/**
	 * Set the PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * Set the QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
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
	 * Set the SubmissionService.
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
	 * {@inheritDoc}
	 */
	public void switchLiveDependency(final Pool from, final Pool to, final boolean directOnly)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				switchLiveDependencyTx(from, to, directOnly);
			}
		}, "switchLiveDependency(pool): from: " + from.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchLiveDependency(final Question from, final Question to)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				switchLiveDependencyTx(from, to);
			}
		}, "switchLiveDependency(question): from: " + from.getId());
	}

	/**
	 * Transaction code for clearStaleMintQuestions()
	 */
	protected void clearStaleMintQuestionsTx(Date stale)
	{
		Object[] fields = new Object[1];
		fields[0] = stale.getTime();

		// Note: for now, lets assume that a mint assessment can have no parts or access, else its not mint! -ggolden
		// // access
		// StringBuilder sql = new StringBuilder();
		// sql.append("DELETE FROM MNEME_ASSESSMENT_ACCESS");
		// sql.append(" USING MNEME_ASSESSMENT_ACCESS, MNEME_ASSESSMENT");
		// sql.append(" WHERE MNEME_ASSESSMENT_ACCESS.ASSESSMENT_ID=MNEME_ASSESSMENT.ID");
		// sql.append(" AND MNEME_ASSESSMENT.MINT='1' AND MNEME_ASSESSMENT.CREATED_BY_DATE < ?");
		//
		// if (!this.sqlService.dbWrite(sql.toString(), fields))
		// {
		// throw new RuntimeException("clearStaleMintQuestionsTx(access): db write failed");
		// }
		//
		// // part detail
		// sql = new StringBuilder();
		// sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		// sql.append(" USING MNEME_ASSESSMENT_PART_DETAIL, MNEME_ASSESSMENT");
		// sql.append(" WHERE MNEME_ASSESSMENT_PART_DETAIL.ASSESSMENT_ID=MNEME_ASSESSMENT.ID");
		// sql.append(" AND MNEME_ASSESSMENT.MINT='1' AND MNEME_ASSESSMENT.CREATED_BY_DATE < ?");
		//
		// if (!this.sqlService.dbWrite(sql.toString(), fields))
		// {
		// throw new RuntimeException("clearStaleMintQuestionsTx(part detail): db write failed");
		// }
		//
		// // parts
		// sql = new StringBuilder();
		// sql.append("DELETE FROM MNEME_ASSESSMENT_PART");
		// sql.append(" USING MNEME_ASSESSMENT_PART, MNEME_ASSESSMENT");
		// sql.append(" WHERE MNEME_ASSESSMENT_PART.ASSESSMENT_ID=MNEME_ASSESSMENT.ID");
		// sql.append(" AND MNEME_ASSESSMENT.MINT='1' AND MNEME_ASSESSMENT.CREATED_BY_DATE < ?");
		//
		// if (!this.sqlService.dbWrite(sql.toString(), fields))
		// {
		// throw new RuntimeException("clearStaleMintQuestionsTx(access): db write failed");
		// }

		// assessments
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT");
		sql.append(" WHERE MINT='1' AND CREATED_BY_DATE < ?");

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("clearStaleMintQuestionsTx(assessment): db write failed");
		}
	}

	/**
	 * Delete an assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessment(final AssessmentImpl assessment)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				deleteAssessmentTx(assessment);
			}
		}, "deleteAssessment: " + assessment.getId());
	}

	/**
	 * Delete an assessment's access record (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param access
	 *        The access to delete.
	 */
	protected void deleteAssessmentAccessTx(AssessmentAccess access)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_ACCESS");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(access.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentAccessTx(access): db write failed");
		}
	}

	/**
	 * Delete an assessment's access records (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentAccessTx(AssessmentImpl assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_ACCESS");
		sql.append(" WHERE ASSESSMENT_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentAccessTx(assessment): db write failed");
		}
	}

	/**
	 * Delete an assessment's part detail (draw and pick) records (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentPartDetailTx(AssessmentImpl assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE ASSESSMENT_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentDrawPickTx(assessment): db write failed");
		}
	}

	/**
	 * Delete an assessment's part detail for a single part(draw and pick) records (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentPartDetailTx(Part part)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE PART_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(part.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentDrawPickTx(part): db write failed");
		}
	}

	/**
	 * Delete an assessment's part records (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentPartTx(AssessmentImpl assessment)
	{
		// part pick-draw
		deleteAssessmentPartDetailTx(assessment);

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART");
		sql.append(" WHERE ASSESSMENT_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentPartTx(assessment): db write failed");
		}
	}

	/**
	 * Delete an assessment part record (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentPartTx(Part part)
	{
		// part pick-draw
		deleteAssessmentPartDetailTx(part);

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(part.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentPartTx(part): db write failed");
		}
	}

	/**
	 * Delete an assessment (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void deleteAssessmentTx(AssessmentImpl assessment)
	{
		// access
		deleteAssessmentAccessTx(assessment);

		// parts
		deleteAssessmentPartTx(assessment);

		// assessment
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("deleteAssessmentTx: db write failed");
		}
	}

	/**
	 * Insert a new assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessment(final AssessmentImpl assessment)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertAssessmentTx(assessment);
			}
		}, "insertAssessment: " + assessment.getId());
	}

	/**
	 * Insert a new assessment access (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessmentAccessTx(AssessmentImpl assessment, AssessmentAccessImpl access)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_ACCESS (");
		sql.append(" ASSESSMENT_ID, DATES_ACCEPT_UNTIL, DATES_DUE, DATES_OPEN,");
		sql.append(" OVERRIDE_ACCEPT_UNTIL, OVERRIDE_DUE, OVERRIDE_OPEN, OVERRIDE_PASSWORD,");
		sql.append(" OVERRIDE_TIME_LIMIT, OVERRIDE_TRIES, PASSWORD, TIME_LIMIT, TRIES, USERS)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[14];
		int i = 0;
		fields[i++] = Long.valueOf(assessment.getId());
		fields[i++] = (access.getAcceptUntilDate() == null) ? null : access.getAcceptUntilDate().getTime();
		fields[i++] = (access.getDueDate() == null) ? null : access.getDueDate().getTime();
		fields[i++] = (access.getOpenDate() == null) ? null : access.getOpenDate().getTime();
		fields[i++] = access.getOverrideAcceptUntilDate() ? "1" : "0";
		fields[i++] = access.getOverrideDueDate() ? "1" : "0";
		fields[i++] = access.getOverrideOpenDate() ? "1" : "0";
		fields[i++] = access.getOverridePassword() ? "1" : "0";
		fields[i++] = access.getOverrideTimeLimit() ? "1" : "0";
		fields[i++] = access.getOverrideTries() ? "1" : "0";
		fields[i++] = access.getPassword().getPassword();
		fields[i++] = access.getTimeLimit();
		fields[i++] = access.getTries();
		fields[i++] = SqlHelper.encodeStringArray(access.getUsers().toArray(new String[access.getUsers().size()]));

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertAssessmentAccessTx: dbInsert failed");
		}

		// set the access's id
		access.initId(id.toString());
	}

	/**
	 * Insert a new assessment's parts (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessmentPartDetailTx(AssessmentImpl assessment, Part part)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_PART_DETAIL (");
		sql.append(" ASSESSMENT_ID, NUM_QUESTIONS_SEQ, ORIG_PID, ORIG_QID, PART_ID, POOL_ID, QUESTION_ID)");
		sql.append(" VALUES(?,?,?,?,?,?,?)");

		Object[] fields = new Object[7];
		fields[0] = Long.valueOf(assessment.getId());

		if (part instanceof ManualPartImpl)
		{
			ManualPartImpl mpart = (ManualPartImpl) part;
			int seq = 0;
			for (PoolPick pick : mpart.questions)
			{
				seq++;
				int i = 1;
				fields[i++] = Integer.valueOf(seq);
				fields[i++] = null;
				fields[i++] = (pick.origQuestionId == null) ? null : Long.valueOf(pick.origQuestionId);
				fields[i++] = Long.valueOf(part.getId());
				fields[i++] = (pick.poolId == null) ? null : Long.valueOf(pick.poolId);
				fields[i++] = Long.valueOf(pick.questionId);

				if (!this.sqlService.dbWrite(null, sql.toString(), fields))
				{
					throw new RuntimeException("insertAssessmentDrawPickTx: dbWrite failed");
				}
			}
		}

		else if (part instanceof DrawPartImpl)
		{
			DrawPartImpl dpart = (DrawPartImpl) part;
			for (PoolDraw draw : dpart.pools)
			{
				int i = 1;
				fields[i++] = Integer.valueOf(draw.getNumQuestions());
				fields[i++] = ((PoolDrawImpl) draw).origPoolId == null ? null : Long.valueOf(((PoolDrawImpl) draw).origPoolId);
				fields[i++] = null;
				fields[i++] = Long.valueOf(part.getId());
				fields[i++] = Long.valueOf(draw.getPoolId());
				fields[i++] = null;

				if (!this.sqlService.dbWrite(null, sql.toString(), fields))
				{
					throw new RuntimeException("insertAssessmentDrawPickTx: dbWrite failed");
				}
			}
		}
	}

	/**
	 * Insert a new assessment's parts (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param part
	 *        the part.
	 */
	protected void insertAssessmentPartTx(AssessmentImpl assessment, Part part)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_PART (");
		sql.append(" ASSESSMENT_ID, PRESENTATION_TEXT, SEQUENCE, TITLE, TYPE, RANDOMIZE)");
		sql.append(" VALUES(?,?,?,?,?,?)");

		Object[] fields = new Object[6];
		fields[0] = Long.valueOf(assessment.getId());
		fields[1] = part.getPresentation().getText();
		fields[2] = part.getOrdering().getPosition();
		fields[3] = part.getTitle();
		fields[4] = (part instanceof ManualPart) ? "M" : "D";
		fields[5] = (part instanceof ManualPart) ? (((ManualPart) part).getRandomize() ? "1" : "0") : "0";

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertAssessmentPartsTx: dbInsert failed");
		}

		// set the part's id
		((PartImpl) part).initId(id.toString());

		// part draw-pick
		insertAssessmentPartDetailTx(assessment, part);
	}

	/**
	 * Insert a new assessment (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessmentTx(AssessmentImpl assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT (");
		sql.append(" ARCHIVED, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER,");
		sql.append(" DATES_ACCEPT_UNTIL, DATES_ARCHIVED, DATES_DUE, DATES_OPEN,");
		sql.append(" GRADING_ANONYMOUS, GRADING_AUTO_RELEASE, GRADING_GRADEBOOK,");
		sql.append(" HISTORICAL, HONOR_PLEDGE, LIVE, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER,");
		sql.append(" PARTS_CONTINUOUS, PARTS_SHOW_PRES, PASSWORD, PRESENTATION_TEXT,");
		sql.append(" PUBLISHED, QUESTION_GROUPING, RANDOM_ACCESS,");
		sql.append(" REVIEW_DATE, REVIEW_SHOW_CORRECT, REVIEW_SHOW_FEEDBACK, REVIEW_TIMING,");
		sql.append(" SHOW_HINTS, SUBMIT_PRES_TEXT, TIME_LIMIT, TITLE, TRIES, TYPE)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[34];
		int i = 0;
		fields[i++] = assessment.getArchived() ? "1" : "0";
		fields[i++] = assessment.getContext();
		fields[i++] = assessment.getCreatedBy().getDate().getTime();
		fields[i++] = assessment.getCreatedBy().getUserId();
		fields[i++] = (assessment.getDates().getAcceptUntilDate() == null) ? null : assessment.getDates().getAcceptUntilDate().getTime();
		fields[i++] = (assessment.getDates().getArchivedDate() == null) ? null : assessment.getDates().getArchivedDate().getTime();
		fields[i++] = (assessment.getDates().getDueDate() == null) ? null : assessment.getDates().getDueDate().getTime();
		fields[i++] = (assessment.getDates().getOpenDate() == null) ? null : assessment.getDates().getOpenDate().getTime();
		fields[i++] = assessment.getGrading().getAnonymous() ? "1" : "0";
		fields[i++] = assessment.getGrading().getAutoRelease() ? "1" : "0";
		fields[i++] = assessment.getGrading().getGradebookIntegration() ? "1" : "0";
		fields[i++] = assessment.isHistorical() ? "1" : "0";
		fields[i++] = assessment.getRequireHonorPledge() ? "1" : "0";
		fields[i++] = assessment.getIsLive() ? "1" : "0";
		fields[i++] = assessment.getMint() ? "1" : "0";
		fields[i++] = assessment.getModifiedBy().getDate().getTime();
		fields[i++] = assessment.getModifiedBy().getUserId();
		fields[i++] = assessment.getParts().getContinuousNumbering() ? "1" : "0";
		fields[i++] = ((AssessmentPartsImpl) assessment.getParts()).showPresentation == null ? null
				: (((AssessmentPartsImpl) assessment.getParts()).showPresentation ? "1" : "0");
		fields[i++] = assessment.getPassword().getPassword();
		fields[i++] = assessment.getPresentation().getText();
		fields[i++] = assessment.getPublished() ? "1" : "0";
		fields[i++] = assessment.getQuestionGrouping().toString();
		fields[i++] = assessment.getRandomAccess() ? "1" : "0";
		fields[i++] = (assessment.getReview().getDate() == null) ? null : assessment.getReview().getDate().getTime();
		fields[i++] = assessment.getReview().getShowCorrectAnswer() ? "1" : "0";
		fields[i++] = assessment.getReview().getShowFeedback() ? "1" : "0";
		fields[i++] = assessment.getReview().getTiming().toString();
		fields[i++] = assessment.getShowHints() ? "1" : "0";
		fields[i++] = assessment.getSubmitPresentation().getText();
		fields[i++] = assessment.getTimeLimit();
		fields[i++] = assessment.getTitle();
		fields[i++] = assessment.getTries();
		fields[i++] = assessment.getType().toString();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("updateAssessmentTx: dbInsert failed");
		}

		// set the assessment's id
		assessment.initId(id.toString());

		// access
		for (AssessmentAccess access : assessment.getSpecialAccess().getAccess())
		{
			insertAssessmentAccessTx(assessment, (AssessmentAccessImpl) access);
		}

		// parts
		for (Part part : assessment.getParts().getParts())
		{
			insertAssessmentPartTx(assessment, part);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void makeLiveTx(Assessment assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT");
		sql.append(" SET LIVE='1'");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("makeLiveTx: db write failed");
		}
	}

	/**
	 * Read an assessment
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The assesment.
	 */
	protected AssessmentImpl readAssessment(String id)
	{
		String where = "WHERE A.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List<AssessmentImpl> rv = readAssessments(where, null, fields);
		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Read a selection of assessments
	 * 
	 * @param where
	 *        The where clause
	 * @param order
	 *        The order clause
	 * @param fields
	 *        The bind variables.
	 * @return The assessments.
	 */
	protected List<AssessmentImpl> readAssessments(String where, String order, Object[] fields)
	{
		final List<AssessmentImpl> rv = new ArrayList<AssessmentImpl>();
		final Map<String, AssessmentImpl> assessments = new HashMap<String, AssessmentImpl>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT A.ARCHIVED, A.CONTEXT, A.CREATED_BY_DATE, A.CREATED_BY_USER,");
		sql.append(" A.DATES_ACCEPT_UNTIL, A.DATES_ARCHIVED, A.DATES_DUE, A.DATES_OPEN,");
		sql.append(" A.GRADING_ANONYMOUS, A.GRADING_AUTO_RELEASE, A.GRADING_GRADEBOOK,");
		sql.append(" A.HISTORICAL, A.HONOR_PLEDGE, A.ID, A.LIVE, A.MINT, A.MODIFIED_BY_DATE, A.MODIFIED_BY_USER,");
		sql.append(" A.PARTS_CONTINUOUS, A.PARTS_SHOW_PRES, A.PASSWORD, A.PRESENTATION_TEXT,");
		sql.append(" A.PUBLISHED, A.QUESTION_GROUPING, A.RANDOM_ACCESS,");
		sql.append(" A.REVIEW_DATE, A.REVIEW_SHOW_CORRECT, A.REVIEW_SHOW_FEEDBACK, A.REVIEW_TIMING,");
		sql.append(" A.SHOW_HINTS, A.SUBMIT_PRES_TEXT, A.TIME_LIMIT, A.TITLE, A.TRIES, A.TYPE");
		sql.append(" FROM MNEME_ASSESSMENT A ");
		sql.append(where);
		if (order != null) sql.append(order);

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					int i = 1;
					AssessmentImpl assessment = newAssessment();
					assessment.setArchived(SqlHelper.readBoolean(result, i++));
					assessment.setContext(StringUtil.trimToNull(result.getString(i++)));
					assessment.getCreatedBy().setDate(SqlHelper.readDate(result, i++));
					assessment.getCreatedBy().setUserId(StringUtil.trimToNull(result.getString(i++)));
					assessment.getDates().setAcceptUntilDate(SqlHelper.readDate(result, i++));
					((AssessmentDatesImpl) assessment.getDates()).archived = SqlHelper.readDate(result, i++);
					assessment.getDates().setDueDate(SqlHelper.readDate(result, i++));
					assessment.getDates().setOpenDate(SqlHelper.readDate(result, i++));
					assessment.getGrading().setAnonymous(SqlHelper.readBoolean(result, i++));
					assessment.getGrading().setAutoRelease(SqlHelper.readBoolean(result, i++));
					assessment.getGrading().setGradebookIntegration(SqlHelper.readBoolean(result, i++));
					assessment.initHistorical(SqlHelper.readBoolean(result, i++));
					assessment.setRequireHonorPledge(SqlHelper.readBoolean(result, i++));
					assessment.initId(Long.toString(result.getLong(i++)));
					assessment.initLive(SqlHelper.readBoolean(result, i++));
					assessment.initMint(SqlHelper.readBoolean(result, i++));
					assessment.getModifiedBy().setDate(SqlHelper.readDate(result, i++));
					assessment.getModifiedBy().setUserId(StringUtil.trimToNull(result.getString(i++)));
					assessment.getParts().setContinuousNumbering(SqlHelper.readBoolean(result, i++));
					assessment.getParts().setShowPresentation(SqlHelper.readBoolean(result, i++));
					assessment.getPassword().setPassword(StringUtil.trimToNull(result.getString(i++)));
					assessment.getPresentation().setText(StringUtil.trimToNull(result.getString(i++)));
					assessment.setPublished(SqlHelper.readBoolean(result, i++));
					assessment.setQuestionGrouping(QuestionGrouping.valueOf(StringUtil.trimToNull(result.getString(i++))));
					assessment.setRandomAccess(SqlHelper.readBoolean(result, i++));
					assessment.getReview().setDate(SqlHelper.readDate(result, i++));
					assessment.getReview().setShowCorrectAnswer(SqlHelper.readBoolean(result, i++));
					assessment.getReview().setShowFeedback(SqlHelper.readBoolean(result, i++));
					assessment.getReview().setTiming(ReviewTiming.valueOf(StringUtil.trimToNull(result.getString(i++))));
					assessment.setShowHints(SqlHelper.readBoolean(result, i++));
					assessment.getSubmitPresentation().setText(StringUtil.trimToNull(result.getString(i++)));
					assessment.setTimeLimit(SqlHelper.readLong(result, i++));
					assessment.setTitle(StringUtil.trimToNull(result.getString(i++)));
					assessment.setTries(SqlHelper.readInteger(result, i++));
					assessment.setType(AssessmentType.valueOf(StringUtil.trimToNull(result.getString(i++))));

					rv.add(assessment);
					assessments.put(assessment.getId(), assessment);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessments(assessment): " + e);
					return null;
				}
			}
		});

		// read all the parts for these assessments
		sql = new StringBuilder();
		sql.append("SELECT P.ASSESSMENT_ID, P.ID, P.PRESENTATION_TEXT, P.TITLE, P.TYPE, P.RANDOMIZE");
		sql.append(" FROM MNEME_ASSESSMENT_PART P");
		sql.append(" JOIN MNEME_ASSESSMENT A ON P.ASSESSMENT_ID=A.ID ");
		sql.append(where);
		sql.append(" ORDER BY P.ASSESSMENT_ID ASC, P.SEQUENCE ASC");
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String aid = SqlHelper.readId(result, 1);
					AssessmentImpl a = assessments.get(aid);
					String type = result.getString(5);
					Part part = null;
					if ("M".equals(type))
					{
						part = a.getParts().addManualPart();
						((ManualPart) part).setRandomize(SqlHelper.readBoolean(result, 6));
					}
					else
					{
						part = a.getParts().addDrawPart();
					}

					((PartImpl) part).initId(SqlHelper.readId(result, 2));
					part.getPresentation().setText(SqlHelper.readString(result, 3));
					part.setTitle(SqlHelper.readString(result, 4));

					((PartImpl) part).clearChanged();

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessments(parts): " + e);
					return null;
				}
			}
		});

		// read all the part details for these assessments
		sql = new StringBuilder();
		sql.append("SELECT P.ASSESSMENT_ID, P.NUM_QUESTIONS_SEQ, P.ORIG_PID, P.ORIG_QID, P.PART_ID,");
		sql.append(" P.POOL_ID, P.QUESTION_ID");
		sql.append(" FROM MNEME_ASSESSMENT_PART_DETAIL P");
		sql.append(" JOIN MNEME_ASSESSMENT A ON P.ASSESSMENT_ID=A.ID ");
		sql.append(where);
		sql.append(" ORDER BY P.ASSESSMENT_ID ASC, P.NUM_QUESTIONS_SEQ ASC");
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String aid = SqlHelper.readId(result, 1);
					AssessmentImpl a = assessments.get(aid);
					String pid = SqlHelper.readId(result, 5);
					Part p = a.getParts().getPart(pid);

					if (p instanceof DrawPart)
					{
						Integer numQuestions = SqlHelper.readInteger(result, 2);
						String origPoolId = SqlHelper.readString(result, 3);
						String poolId = SqlHelper.readString(result, 6);
						((DrawPartImpl) p).initDraw(poolId, origPoolId, numQuestions);
					}
					else if (p instanceof ManualPart)
					{
						String questionId = SqlHelper.readString(result, 7);
						String origQid = SqlHelper.readString(result, 4);
						String poolId = SqlHelper.readString(result, 6);
						((ManualPartImpl) p).initPick(questionId, origQid, poolId);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessments(part details): " + e);
					return null;
				}
			}
		});

		// read all the access for these assesments
		sql = new StringBuilder();
		sql.append("SELECT X.ASSESSMENT_ID, X.DATES_ACCEPT_UNTIL, X.DATES_DUE, X.DATES_OPEN, X.ID,");
		sql.append(" X.OVERRIDE_ACCEPT_UNTIL, X.OVERRIDE_DUE, X.OVERRIDE_OPEN, X.OVERRIDE_PASSWORD,");
		sql.append(" X.OVERRIDE_TIME_LIMIT, X.OVERRIDE_TRIES, X.PASSWORD, X.TIME_LIMIT, X.TRIES, X.USERS");
		sql.append(" FROM MNEME_ASSESSMENT_ACCESS X");
		sql.append(" JOIN MNEME_ASSESSMENT A ON X.ASSESSMENT_ID=A.ID ");
		sql.append(where);
		sql.append(" ORDER BY X.ASSESSMENT_ID ASC, X.ID ASC");

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String aid = SqlHelper.readId(result, 1);
					AssessmentImpl a = assessments.get(aid);
					AssessmentAccess access = a.getSpecialAccess().addAccess();

					access.setAcceptUntilDate(SqlHelper.readDate(result, 2));
					access.setDueDate(SqlHelper.readDate(result, 3));
					access.setOpenDate(SqlHelper.readDate(result, 4));
					((AssessmentAccessImpl) access).initId(SqlHelper.readId(result, 5));
					access.setOverrideAcceptUntilDate(SqlHelper.readBoolean(result, 6));
					access.setOverrideDueDate(SqlHelper.readBoolean(result, 7));
					access.setOverrideOpenDate(SqlHelper.readBoolean(result, 8));
					access.setOverridePassword(SqlHelper.readBoolean(result, 9));
					access.setOverrideTimeLimit(SqlHelper.readBoolean(result, 10));
					access.setOverrideTries(SqlHelper.readBoolean(result, 11));
					access.setPasswordValue(SqlHelper.readString(result, 12));
					access.setTimeLimit(SqlHelper.readLong(result, 13));
					access.setTries(SqlHelper.readInteger(result, 14));
					access.setUsers(Arrays.asList(SqlHelper.decodeStringArray(SqlHelper.readString(result, 15))));

					a.changed.clearChanged();

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessments(access): " + e);
					return null;
				}
			}
		});

		// clear changed for the assessments
		for (AssessmentImpl a : rv)
		{
			a.clearChanged();
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void removeDependencyTx(Pool pool)
	{
		// look for any pool_id that matches from D parts only (question_id is null)
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE POOL_ID=? AND QUESTION_ID IS NULL");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(pool.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeDependencyTx(pool): db write failed");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void removeDependencyTx(Question question)
	{
		// M parts only (question_id is set non-null): match the question_id field
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE QUESTION_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeDependencyTx(question): db write failed");
		}
	}

	/**
	 * Transaction code for switchLiveDependency()
	 */
	protected void switchLiveDependencyTx(Pool from, Pool to, boolean directOnly)
	{
		if (directOnly)
		{
			// look for any pool_id that matches from D parts only (question_id is null) for live assessments
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE MNEME_ASSESSMENT_PART_DETAIL D, MNEME_ASSESSMENT A");
			sql.append(" SET D.POOL_ID=?");
			sql.append(" WHERE D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
			sql.append(" AND D.POOL_ID=? AND D.QUESTION_ID IS NULL");

			Object[] fields = new Object[2];
			fields[0] = Long.valueOf(to.getId());
			fields[1] = Long.valueOf(from.getId());

			if (!this.sqlService.dbWrite(sql.toString(), fields))
			{
				throw new RuntimeException("switchLiveDependencyTx(pool-1): dbWrite failed");
			}
		}

		else
		{
			// look for any pool_id that matches or a question's pool_id that matches for live assessments
			// this one for when we have a question id (D part)
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE MNEME_ASSESSMENT_PART_DETAIL D, MNEME_ASSESSMENT A, MNEME_QUESTION Q");
			sql.append(" SET D.POOL_ID=?");
			sql.append(" WHERE D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
			sql.append(" AND D.QUESTION_ID=Q.ID");
			sql.append(" AND D.POOL_ID=? OR (Q.POOL_ID=? AND D.POOL_ID IS NULL)");

			Object[] fields = new Object[3];
			fields[0] = Long.valueOf(to.getId());
			fields[1] = Long.valueOf(from.getId());
			fields[2] = fields[1];

			if (!this.sqlService.dbWrite(sql.toString(), fields))
			{
				throw new RuntimeException("switchLiveDependencyTx(pool-2): dbWrite failed");
			}

			// look for any pool_id that matches or a question's pool_id that matches for live assessments
			// this one for when we have a no question id (M part)
			sql = new StringBuilder();
			sql.append("UPDATE MNEME_ASSESSMENT_PART_DETAIL D, MNEME_ASSESSMENT A");
			sql.append(" SET D.POOL_ID=?");
			sql.append(" WHERE D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
			sql.append(" AND D.POOL_ID=? AND D.QUESTION_ID IS NULL");

			fields = new Object[2];
			fields[0] = Long.valueOf(to.getId());
			fields[1] = Long.valueOf(from.getId());

			if (!this.sqlService.dbWrite(sql.toString(), fields))
			{
				throw new RuntimeException("switchLiveDependencyTx(pool-3): dbWrite failed");
			}
		}
	}

	/**
	 * Transaction code for switchLiveDependency().
	 */
	protected void switchLiveDependencyTx(Question from, Question to)
	{
		// swap the pool to the from's pool's id (only if we don't have a pool id yet)
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT_PART_DETAIL D, MNEME_ASSESSMENT A");
		sql.append(" SET D.POOL_ID=?");
		sql.append(" WHERE D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
		sql.append(" AND D.POOL_ID IS NULL AND D.QUESTION_ID=?");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(from.getPool().getId());
		fields[1] = Long.valueOf(from.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("switchLiveDependencyTx(question-1): dbWrite failed");
		}

		// swap the question
		sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT_PART_DETAIL D, MNEME_ASSESSMENT A");
		sql.append(" SET D.QUESTION_ID=?");
		sql.append(" WHERE D.ASSESSMENT_ID=A.ID AND A.LIVE='1'");
		sql.append(" AND D.QUESTION_ID=?");

		fields = new Object[2];
		fields[0] = Long.valueOf(to.getId());
		fields[1] = Long.valueOf(from.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("switchLiveDependencyTx(question-2): dbWrite failed");
		}
	}

	/**
	 * Update an existing assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void updateAssessment(final AssessmentImpl assessment)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateAssessmentTx(assessment);
			}
		}, "updateAssessment: " + assessment.getId());
	}

	/**
	 * Update an existing assessment access record (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void updateAssessmentAccessTx(AssessmentAccessImpl access)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT_ACCESS SET");
		sql.append(" DATES_ACCEPT_UNTIL=?, DATES_DUE=?, DATES_OPEN=?,");
		sql.append(" OVERRIDE_ACCEPT_UNTIL=?, OVERRIDE_DUE=?, OVERRIDE_OPEN=?, OVERRIDE_PASSWORD=?,");
		sql.append(" OVERRIDE_TIME_LIMIT=?, OVERRIDE_TRIES=?, PASSWORD=?, TIME_LIMIT=?, TRIES=?, USERS=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[14];
		int i = 0;
		fields[i++] = (access.getAcceptUntilDate() == null) ? null : access.getAcceptUntilDate().getTime();
		fields[i++] = (access.getDueDate() == null) ? null : access.getDueDate().getTime();
		fields[i++] = (access.getOpenDate() == null) ? null : access.getOpenDate().getTime();
		fields[i++] = access.getOverrideAcceptUntilDate() ? "1" : "0";
		fields[i++] = access.getOverrideDueDate() ? "1" : "0";
		fields[i++] = access.getOverrideOpenDate() ? "1" : "0";
		fields[i++] = access.getOverridePassword() ? "1" : "0";
		fields[i++] = access.getOverrideTimeLimit() ? "1" : "0";
		fields[i++] = access.getOverrideTries() ? "1" : "0";
		fields[i++] = access.getPassword().getPassword();
		fields[i++] = access.getTimeLimit();
		fields[i++] = access.getTries();
		fields[i++] = SqlHelper.encodeStringArray(access.getUsers().toArray(new String[access.getUsers().size()]));
		fields[i++] = Long.valueOf(access.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateAssessmentAccessTx: dbInsert failed");
		}
	}

	/**
	 * Update an assessment part (transaction code).
	 * 
	 * @param part
	 *        the part.
	 */
	protected void updateAssessmentPartTx(AssessmentImpl assessment, Part part)
	{
		// delete the old part pick-draw
		deleteAssessmentPartDetailTx(part);

		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT_PART SET");
		sql.append(" PRESENTATION_TEXT=?, SEQUENCE=?, TITLE=?, RANDOMIZE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[5];
		fields[0] = part.getPresentation().getText();
		fields[1] = part.getOrdering().getPosition();
		fields[2] = part.getTitle();
		fields[3] = (part instanceof ManualPart) ? (((ManualPart) part).getRandomize() ? "1" : "0") : "0";
		fields[4] = Long.valueOf(part.getId());

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("updateAssessmentPartTx: dbInsert failed");
		}

		// insert the new part draw-pick
		insertAssessmentPartDetailTx(assessment, part);
	}

	/**
	 * Update an existing assessment (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void updateAssessmentTx(AssessmentImpl assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ASSESSMENT SET");
		sql.append(" ARCHIVED=?, CONTEXT=?,");
		sql.append(" DATES_ACCEPT_UNTIL=?, DATES_ARCHIVED=?, DATES_DUE=?, DATES_OPEN=?,");
		sql.append(" GRADING_ANONYMOUS=?, GRADING_AUTO_RELEASE=?, GRADING_GRADEBOOK=?,");
		sql.append(" HISTORICAL=?, HONOR_PLEDGE=?, LIVE=?, MINT=?, MODIFIED_BY_DATE=?, MODIFIED_BY_USER=?,");
		sql.append(" PARTS_CONTINUOUS=?, PARTS_SHOW_PRES=?, PASSWORD=?, PRESENTATION_TEXT=?,");
		sql.append(" PUBLISHED=?, QUESTION_GROUPING=?, RANDOM_ACCESS=?,");
		sql.append(" REVIEW_DATE=?, REVIEW_SHOW_CORRECT=?, REVIEW_SHOW_FEEDBACK=?, REVIEW_TIMING=?,");
		sql.append(" SHOW_HINTS=?, SUBMIT_PRES_TEXT=?, TIME_LIMIT=?, TITLE=?, TRIES=?, TYPE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[33];
		int i = 0;
		fields[i++] = assessment.getArchived() ? "1" : "0";
		fields[i++] = assessment.getContext();
		fields[i++] = (assessment.getDates().getAcceptUntilDate() == null) ? null : assessment.getDates().getAcceptUntilDate().getTime();
		fields[i++] = (assessment.getDates().getArchivedDate() == null) ? null : assessment.getDates().getArchivedDate().getTime();
		fields[i++] = (assessment.getDates().getDueDate() == null) ? null : assessment.getDates().getDueDate().getTime();
		fields[i++] = (assessment.getDates().getOpenDate() == null) ? null : assessment.getDates().getOpenDate().getTime();
		fields[i++] = assessment.getGrading().getAnonymous() ? "1" : "0";
		fields[i++] = assessment.getGrading().getAutoRelease() ? "1" : "0";
		fields[i++] = assessment.getGrading().getGradebookIntegration() ? "1" : "0";
		fields[i++] = assessment.isHistorical() ? "1" : "0";
		fields[i++] = assessment.getRequireHonorPledge() ? "1" : "0";
		fields[i++] = assessment.getIsLive() ? "1" : "0";
		fields[i++] = assessment.getMint() ? "1" : "0";
		fields[i++] = assessment.getModifiedBy().getDate().getTime();
		fields[i++] = assessment.getModifiedBy().getUserId();
		fields[i++] = assessment.getParts().getContinuousNumbering() ? "1" : "0";
		fields[i++] = ((AssessmentPartsImpl) assessment.getParts()).showPresentation == null ? null
				: (((AssessmentPartsImpl) assessment.getParts()).showPresentation ? "1" : "0");
		fields[i++] = assessment.getPassword().getPassword();
		fields[i++] = assessment.getPresentation().getText();
		fields[i++] = assessment.getPublished() ? "1" : "0";
		fields[i++] = assessment.getQuestionGrouping().toString();
		fields[i++] = assessment.getRandomAccess() ? "1" : "0";
		fields[i++] = (assessment.getReview().getDate() == null) ? null : assessment.getReview().getDate().getTime();
		fields[i++] = assessment.getReview().getShowCorrectAnswer() ? "1" : "0";
		fields[i++] = assessment.getReview().getShowFeedback() ? "1" : "0";
		fields[i++] = assessment.getReview().getTiming().toString();
		fields[i++] = assessment.getShowHints() ? "1" : "0";
		fields[i++] = assessment.getSubmitPresentation().getText();
		fields[i++] = assessment.getTimeLimit();
		fields[i++] = assessment.getTitle();
		fields[i++] = assessment.getTries();
		fields[i++] = assessment.getType().toString();
		fields[i++] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateAssessmentTx: db write failed");
		}

		// access
		for (AssessmentAccess access : assessment.getSpecialAccess().getAccess())
		{
			if (access.getId() == null)
			{
				insertAssessmentAccessTx(assessment, (AssessmentAccessImpl) access);
			}
			else if (((AssessmentAccessImpl) access).getChanged())
			{
				updateAssessmentAccessTx((AssessmentAccessImpl) access);
			}
		}
		for (AssessmentAccess access : ((AssessmentSpecialAccessImpl) assessment.getSpecialAccess()).getDeleted())
		{
			deleteAssessmentAccessTx(access);
		}

		// parts
		for (Part part : assessment.getParts().getParts())
		{
			if (part.getId() == null)
			{
				insertAssessmentPartTx(assessment, part);
			}
			else if (((PartImpl) part).getChanged())
			{
				updateAssessmentPartTx(assessment, (PartImpl) part);
			}
		}
		for (Part part : ((AssessmentPartsImpl) assessment.getParts()).getDeleted())
		{
			deleteAssessmentPartTx(part);
		}
	}
}
