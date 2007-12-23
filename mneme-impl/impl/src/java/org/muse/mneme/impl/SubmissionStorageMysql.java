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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;

/**
 * SubmissionStorageMysql handles submission storage for mysql.
 */
public class SubmissionStorageMysql implements SubmissionStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionStorageMysql.class);

	/** Dependency: AssessmentService. */
	protected AssessmentService assessmentService = null;

	/** Configuration: to run the ddl on init or not. */
	protected boolean autoDdl = false;

	/** Dependency: MnemeService. */
	protected MnemeService mnemeService = null;

	/** Dependency: SecurityService. */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager. */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService. */
	protected SqlService sqlService = null;

	/** Dependency: SubmissionService. */
	protected SubmissionServiceImpl submissionService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

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
	public List<String> findPartQuestions(Part part)
	{
		// get all question ids from submission answers to this part's assessment,
		// in this part, complete submissions and answered questions
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT A.QUESTION_ID FROM MNEME_ANSWER A");
		sql.append(" JOIN MNEME_SUBMISSION S ON A.SUBMISSION_ID=S.ID AND S.ASSESSMENT_ID=? AND S.COMPLETE='1' AND S.TEST_DRIVE='0'");
		sql.append(" WHERE A.PART_ID=? AND A.ANSWERED='1'");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(part.getAssessment().getId());
		fields[1] = Long.valueOf(part.getId());

		final List<String> rv = new ArrayList<String>();
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String qid = SqlHelper.readId(result, 1);
					rv.add(qid);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("findPartQuestions: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Answer getAnswer(String answerId)
	{
		// get the submission so the answer has its full context
		StringBuilder where = new StringBuilder();
		where.append("JOIN MNEME_ANSWER AA ON AA.SUBMISSION_ID=S.ID");
		where.append(" WHERE AA.ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(answerId);

		List<SubmissionImpl> submissions = readSubmissions(where.toString(), null, fields);
		if (submissions.size() > 0)
		{
			// find the answer
			Answer rv = submissions.get(0).getAnswer(answerId);
			return rv;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getAssessmentCompleteSubmissions(Assessment assessment)
	{
		String where = "WHERE S.ASSESSMENT_ID=? AND COMPLETE='1'";
		String order = "ORDER BY S.SUBMITTED_DATE ASC";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		List<SubmissionImpl> rv = readSubmissions(where, order, fields);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentHasUnscoredSubmissions(Assessment assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ANSWER A");
		sql.append(" JOIN MNEME_SUBMISSION S ON A.SUBMISSION_ID=S.ID AND S.ASSESSMENT_ID=? AND S.COMPLETE='1' AND S.TEST_DRIVE='0'");
		sql.append(" WHERE A.ANSWERED='1' AND A.EVAL_SCORE IS NULL AND A.AUTO_SCORE IS NULL");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

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
	public Map<String, Float> getAssessmentHighestScores(Assessment assessment, Boolean releasedOnly)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.ID, S.USER, S.EVAL_SCORE, SUM(A.EVAL_SCORE), SUM(A.AUTO_SCORE) FROM MNEME_SUBMISSION S");
		sql.append(" JOIN  MNEME_ANSWER A ON S.ID=A.SUBMISSION_ID");
		sql.append(" WHERE S.ASSESSMENT_ID=? AND S.COMPLETE='1' AND S.TEST_DRIVE='0'");
		if (releasedOnly)
		{
			sql.append(" AND S.RELEASED='1'");
		}
		sql.append(" GROUP BY S.ID");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		final Map<String, Float> scores = new HashMap<String, Float>();
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String sid = SqlHelper.readId(result, 1);
					String user = SqlHelper.readString(result, 2);
					Float sEval = SqlHelper.readFloat(result, 3);
					Float aEval = SqlHelper.readFloat(result, 4);
					Float aAuto = SqlHelper.readFloat(result, 5);
					Float total = Float.valueOf((sEval == null ? 0f : sEval.floatValue()) + (aEval == null ? 0f : aEval.floatValue())
							+ (aAuto == null ? 0f : aAuto.floatValue()));

					// if the user has an entry already, replace it if this score is higher
					Float prior = scores.get(user);
					if (prior != null)
					{
						if (prior.floatValue() < total.floatValue())
						{
							scores.put(user, total);
						}
					}
					else
					{
						scores.put(user, total);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentHighestScores: " + e);
					return null;
				}
			}
		});

		return scores;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ANSWER A");
		sql.append(" JOIN MNEME_SUBMISSION S ON A.SUBMISSION_ID=S.ID AND S.ASSESSMENT_ID=? AND COMPLETE='1'");
		sql.append(" WHERE A.QUESTION_ID=? AND A.ANSWERED='1' AND A.EVAL_SCORE IS NULL AND A.AUTO_SCORE IS NULL");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(assessment.getId());
		fields[1] = Long.valueOf(question.getId());

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
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		List<Float> rv = new ArrayList<Float>();
		// TODO:
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getAssessmentSubmissions(Assessment assessment)
	{
		// collect the submissions to this assessment
		String where = "WHERE S.ASSESSMENT_ID=? AND S.TEST_DRIVE='0'";
		String order = "ORDER BY S.SUBMITTED_DATE ASC";

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		List<SubmissionImpl> rv = readSubmissions(where, order, fields);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getOpenSubmissions()
	{
		// collect the submissions to this assessment
		String where = "WHERE S.COMPLETE='0'";
		String order = "ORDER BY S.SUBMITTED_DATE ASC";

		List<SubmissionImpl> rv = readSubmissions(where, order, null);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		List<Float> rv = new ArrayList<Float>();
		// TODO:
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl getSubmission(String id)
	{
		return readSubmission(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getSubmissionHighestScore(Assessment assessment, String userId)
	{
		// TODO: pre-compute into MNEME_SUBMISSION.TOTAL_SCORE? -ggolden

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.ID, S.EVAL_SCORE, SUM(A.EVAL_SCORE), SUM(A.AUTO_SCORE) FROM MNEME_SUBMISSION S");
		sql.append(" JOIN  MNEME_ANSWER A ON S.ID=A.SUBMISSION_ID");
		sql.append(" WHERE S.ASSESSMENT_ID=? AND S.USER=? AND S.COMPLETE='1'");
		sql.append(" GROUP BY S.ID");

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(assessment.getId());
		fields[1] = userId;

		final Map<String, Float> scores = new HashMap<String, Float>();
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String sid = SqlHelper.readId(result, 1);
					Float sEval = SqlHelper.readFloat(result, 2);
					Float aEval = SqlHelper.readFloat(result, 3);
					Float aAuto = SqlHelper.readFloat(result, 4);
					Float total = Float.valueOf((sEval == null ? 0f : sEval.floatValue()) + (aEval == null ? 0f : aEval.floatValue())
							+ (aAuto == null ? 0f : aAuto.floatValue()));
					scores.put(sid, total);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getSubmissionHighestScore: " + e);
					return null;
				}
			}
		});

		// find the submission with the highest score
		String highestId = null;
		Float highestTotal = null;
		for (Map.Entry entry : scores.entrySet())
		{
			String sid = (String) entry.getKey();
			Float total = (Float) entry.getValue();
			if (highestId == null)
			{
				highestId = sid;
				highestTotal = total;
			}
			else if (total.floatValue() > highestTotal.floatValue())
			{
				highestId = sid;
				highestTotal = total;
			}
		}

		return highestTotal;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getSubmissionScore(Submission submission)
	{
		// TODO: pre-compute into MNEME_SUBMISSION.TOTAL_SCORE? -ggolden

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.EVAL_SCORE, SUM(A.EVAL_SCORE), SUM(A.AUTO_SCORE) FROM MNEME_SUBMISSION S");
		sql.append(" JOIN  MNEME_ANSWER A ON S.ID=A.SUBMISSION_ID");
		sql.append(" WHERE S.ID=?");
		sql.append(" GROUP BY S.ID");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(submission.getId());

		// to collect the score (we just need something that we can change that is also Final)
		final List<Float> score = new ArrayList<Float>();
		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					Float sEval = SqlHelper.readFloat(result, 1);
					Float aEval = SqlHelper.readFloat(result, 2);
					Float aAuto = SqlHelper.readFloat(result, 3);
					Float total = Float.valueOf((sEval == null ? 0f : sEval.floatValue()) + (aEval == null ? 0f : aEval.floatValue())
							+ (aAuto == null ? 0f : aAuto.floatValue()));
					score.add(total);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getSubmissionScore: " + e);
					return null;
				}
			}
		});

		if (score.size() > 0)
		{
			return score.get(0);
		}

		// TODO: return null here? sample returns 0f -ggolden
		return Float.valueOf(0f);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getUserAssessmentSubmissions(Assessment assessment, String userId)
	{
		String where = "WHERE S.ASSESSMENT_ID=? AND S.USER=?";
		String order = "ORDER BY S.SUBMITTED_DATE ASC";

		Object[] fields = new Object[2];
		fields[0] = Long.valueOf(assessment.getId());
		fields[1] = userId;

		List<SubmissionImpl> rv = readSubmissions(where, order, fields);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SubmissionImpl> getUserContextSubmissions(String context, String userId, Boolean publishedOnly)
	{
		StringBuilder where = new StringBuilder();
		where.append("JOIN MNEME_ASSESSMENT AA ON S.ASSESSMENT_ID=AA.ID AND AA.ARCHIVED='0'");
		if (publishedOnly)
		{
			where.append(" AND AA.PUBLISHED='1'");
		}
		where.append(" WHERE S.CONTEXT=? AND S.USER=?");
		String order = "ORDER BY S.SUBMITTED_DATE ASC";

		Object[] fields = new Object[2];
		fields[0] = context;
		fields[1] = userId;

		List<SubmissionImpl> rv = readSubmissions(where.toString(), order, fields);
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getUsersSubmitted(Assessment assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT S.USER FROM MNEME_SUBMISSION S");
		sql.append(" WHERE S.ASSESSMENT_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		List rv = this.sqlService.dbRead(sql.toString(), fields, null);

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_submission");
		}

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public AnswerImpl newAnswer()
	{
		return new AnswerImpl(mnemeService);
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl newSubmission()
	{
		return new SubmissionImpl(assessmentService, securityService, submissionService, sessionManager);
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionImpl newSubmission(SubmissionImpl other)
	{
		return new SubmissionImpl(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTestDriveSubmissions(final Assessment assessment)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				removeTestDriveSubmissionsTx(assessment);
			}
		}, "removeTestDriveSubmissions: " + assessment.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTestDriveSubmissions(final String context)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				removeTestDriveSubmissionsTx(context);
			}
		}, "removeTestDriveSubmissions(context): " + context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAnswers(List<Answer> answers)
	{
		// for each answer, update or insert
		for (Answer a : answers)
		{
			if (a.getId() == null)
			{
				// insert
				insertAnswer((AnswerImpl) a);
			}
			else
			{
				// update
				updateAnswer((AnswerImpl) a);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAnswersEvaluation(List<Answer> answers)
	{
		for (Answer a : answers)
		{
			updateAnswerEval((AnswerImpl) a);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmission(SubmissionImpl submission)
	{
		// if new
		if (submission.getId() == null)
		{
			// insert
			insertSubmission(submission);
		}

		// reject phantoms
		else if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// update
		else
		{
			updateSubmission(submission);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmissionEvaluation(SubmissionImpl submission)
	{
		if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// has to be an existing saved submission
		if (submission.getId() == null) throw new IllegalArgumentException();

		updateSubmissionEval(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmissionReleased(SubmissionImpl submission)
	{
		if (submission.getId().startsWith(SubmissionService.PHANTOM_PREFIX))
		{
			// lets not save phanton submissions
			throw new IllegalArgumentException();
		}

		// has to be an existing saved submission
		if (submission.getId() == null) throw new IllegalArgumentException();

		updateSubmissionReleased(submission);
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 */
	public void setMnemeService(MnemeService service)
	{
		this.mnemeService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionService(SubmissionServiceImpl service)
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
	public Boolean submissionsDependsOn(Question question)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_ANSWER A");
		sql.append(" WHERE A.QUESTION_ID=?");

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
	public Boolean submissionsExist(Assessment assessment)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) FROM MNEME_SUBMISSION S");
		sql.append(" WHERE S.ASSESSMENT_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		List results = this.sqlService.dbRead(sql.toString(), fields, null);
		if (results.size() > 0)
		{
			int size = Integer.parseInt((String) results.get(0));
			return Boolean.valueOf(size > 0);
		}

		return Boolean.FALSE;
	}

	/**
	 * Insert a new answer.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void insertAnswer(final AnswerImpl answer)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertAnswerTx(answer);
			}
		}, "insertAnswer: " + answer.getId());
	}

	/**
	 * Insert a new pool (transaction code).
	 * 
	 * @param pool
	 *        The pool.
	 */
	protected void insertAnswerTx(AnswerImpl answer)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ANSWER (");
		sql.append(" ANSWERED, AUTO_SCORE, GUEST, EVAL_ATRIB_DATE, EVAL_ATRIB_USER, EVAL_COMMENT, EVAL_EVALUATED, EVAL_SCORE,");
		sql.append(" PART_ID, QUESTION_ID, QUESTION_TYPE, REASON, REVIEW, SUBMISSION_ID, SUBMITTED_DATE)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[15];
		fields[0] = answer.getIsAnswered();
		fields[1] = answer.getAutoScore();
		fields[2] = SqlHelper.encodeStringArray(answer.getTypeSpecificAnswer().getData());
		fields[3] = (answer.getEvaluation().getAttribution().getDate() == null) ? null : answer.getEvaluation().getAttribution().getDate().getTime();
		fields[4] = answer.getEvaluation().getAttribution().getUserId();
		fields[5] = answer.getEvaluation().getComment();
		fields[6] = answer.getEvaluation().getEvaluated() ? "1" : "0";
		fields[7] = answer.getEvaluation().getScore() == null ? null : Float.valueOf(answer.getEvaluation().getScore());
		fields[8] = Long.valueOf(answer.getPartId());
		Question q = answer.getQuestion();
		fields[9] = Long.valueOf(q.getId());
		fields[10] = q.getType();
		fields[11] = answer.getReason();
		fields[12] = answer.getMarkedForReview() ? "1" : "0";
		fields[13] = Long.valueOf(answer.getSubmission().getId());
		fields[14] = (answer.getSubmittedDate() == null) ? null : answer.getSubmittedDate().getTime();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertPoolTx: dbInsert failed");
		}

		// set the answer's id
		answer.initId(id.toString());
	}

	/**
	 * Insert a new submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void insertSubmission(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				insertSubmissionTx(submission);
			}
		}, "insertSubmission: " + submission.getId());
	}

	/**
	 * Insert a new submission (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void insertSubmissionTx(SubmissionImpl submission)
	{
		// new submissions have no answers yet

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_SUBMISSION (");
		sql.append(" ASSESSMENT_ID, COMPLETE, CONTEXT, EVAL_ATRIB_DATE, EVAL_ATRIB_USER,");
		sql.append(" EVAL_COMMENT, EVAL_EVALUATED, EVAL_SCORE, RELEASED, START_DATE, SUBMITTED_DATE, TEST_DRIVE, USER )");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[13];
		fields[0] = Long.valueOf(submission.getAssessment().getId());
		fields[1] = submission.getIsComplete() ? "1" : "0";
		fields[2] = submission.getAssessment().getContext();
		fields[3] = (submission.getEvaluation().getAttribution().getDate() == null) ? null : submission.getEvaluation().getAttribution().getDate()
				.getTime();
		fields[4] = submission.getEvaluation().getAttribution().getUserId();
		fields[5] = submission.getEvaluation().getComment();
		fields[6] = submission.getEvaluation().getEvaluated() ? "1" : "0";
		fields[7] = submission.getEvaluation().getScore() == null ? null : Float.valueOf(submission.getEvaluation().getScore());
		fields[8] = submission.getIsReleased() ? "1" : "0";
		fields[9] = (submission.getStartDate() == null) ? null : submission.getStartDate().getTime();
		fields[10] = (submission.getSubmittedDate() == null) ? null : submission.getSubmittedDate().getTime();
		fields[11] = submission.getIsTestDrive() ? "1" : "0";
		fields[12] = submission.getUserId();

		Long id = this.sqlService.dbInsert(null, sql.toString(), fields, "ID");
		if (id == null)
		{
			throw new RuntimeException("insertSubmissionTx: dbInsert failed");
		}

		// set the submission's id
		submission.initId(id.toString());
	}

	/**
	 * Read an submission
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission.
	 */
	protected SubmissionImpl readSubmission(String id)
	{
		String where = "WHERE S.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(id);
		List<SubmissionImpl> rv = readSubmissions(where, null, fields);
		if (rv.size() > 0)
		{
			return rv.get(0);
		}

		return null;
	}

	/**
	 * Read a selection of submissions.
	 * 
	 * @param where
	 *        The where clause
	 * @param order
	 *        The order clause
	 * @param fields
	 *        The bind variables.
	 * @return The submissions.
	 */
	protected List<SubmissionImpl> readSubmissions(String where, String order, Object[] fields)
	{
		final List<SubmissionImpl> rv = new ArrayList<SubmissionImpl>();
		final Map<String, SubmissionImpl> submissions = new HashMap<String, SubmissionImpl>();

		// submissions
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.ASSESSMENT_ID, S.COMPLETE, S.EVAL_ATRIB_DATE,");
		sql.append(" S.EVAL_ATRIB_USER, S.EVAL_COMMENT, S.EVAL_EVALUATED, S.EVAL_SCORE,");
		sql.append(" S.ID, S.RELEASED, S.START_DATE, S.SUBMITTED_DATE, S.TEST_DRIVE, S.USER");
		sql.append(" FROM MNEME_SUBMISSION S ");
		sql.append(where);
		if (order != null)
		{
			sql.append(" ");
			sql.append(order);
		}

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					int i = 1;
					SubmissionImpl submission = newSubmission();
					submission.initAssessmentId(SqlHelper.readId(result, i++));
					submission.setIsComplete(SqlHelper.readBoolean(result, i++));
					((AttributionImpl) submission.getEvaluation().getAttribution()).initDate(SqlHelper.readDate(result, i++));
					((AttributionImpl) submission.getEvaluation().getAttribution()).initUserId(SqlHelper.readString(result, i++));
					((EvaluationImpl) submission.getEvaluation()).initComment(SqlHelper.readString(result, i++));
					((EvaluationImpl) submission.getEvaluation()).initEvaluated(SqlHelper.readBoolean(result, i++));
					((EvaluationImpl) submission.getEvaluation()).initScore(SqlHelper.readFloat(result, i++));
					submission.initId(SqlHelper.readId(result, i++));
					submission.initReleased(SqlHelper.readBoolean(result, i++));
					submission.setStartDate(SqlHelper.readDate(result, i++));
					submission.setSubmittedDate(SqlHelper.readDate(result, i++));
					submission.initTestDrive(SqlHelper.readBoolean(result, i++));
					submission.initUserId(SqlHelper.readString(result, i++));

					submission.clearIsChanged();
					rv.add(submission);
					submissions.put(submission.getId(), submission);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissions(submission): " + e);
					return null;
				}
			}
		});

		// read all the answers for these submissions
		sql = new StringBuilder();
		sql.append("SELECT A.GUEST, A.EVAL_ATRIB_DATE, A.EVAL_ATRIB_USER, A.EVAL_COMMENT, A.EVAL_EVALUATED,");
		sql.append(" A.EVAL_SCORE, A.ID, A.PART_ID, A.QUESTION_ID, A.QUESTION_TYPE, A.REASON, A.REVIEW,");
		sql.append(" A.SUBMISSION_ID, A.SUBMITTED_DATE");
		sql.append(" FROM MNEME_ANSWER A");
		sql.append(" JOIN MNEME_SUBMISSION S ON A.SUBMISSION_ID=S.ID ");
		sql.append(where);
		sql.append(" ORDER BY A.SUBMISSION_ID ASC");

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String sid = SqlHelper.readId(result, 13);
					SubmissionImpl s = submissions.get(sid);
					AnswerImpl a = newAnswer();

					((AttributionImpl) a.getEvaluation().getAttribution()).initDate(SqlHelper.readDate(result, 2));
					((AttributionImpl) a.getEvaluation().getAttribution()).initUserId(SqlHelper.readString(result, 3));
					((EvaluationImpl) a.getEvaluation()).initComment(SqlHelper.readString(result, 4));
					((EvaluationImpl) a.getEvaluation()).initEvaluated(SqlHelper.readBoolean(result, 5));
					((EvaluationImpl) a.getEvaluation()).initScore(SqlHelper.readFloat(result, 6));
					a.initId(SqlHelper.readId(result, 7));
					a.initPartId(SqlHelper.readId(result, 8));
					a.initQuestion(SqlHelper.readId(result, 9), SqlHelper.readString(result, 10));
					a.getTypeSpecificAnswer().setData(SqlHelper.decodeStringArray(StringUtil.trimToNull(result.getString(1))));
					a.setReason(SqlHelper.readString(result, 11));
					a.setMarkedForReview(SqlHelper.readBoolean(result, 12));
					a.setSubmittedDate(SqlHelper.readDate(result, 14));

					a.clearIsChanged();
					a.initSubmission(s);
					s.initAnswer(a);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissions(answers): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void removeTestDriveSubmissionsTx(Assessment assessment)
	{
		// answer
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ANSWER");
		sql.append(" USING MNEME_ANSWER, MNEME_SUBMISSION");
		sql.append(" WHERE MNEME_ANSWER.SUBMISSION_ID=MNEME_SUBMISSION.ID AND MNEME_SUBMISSION.ASSESSMENT_ID=? AND MNEME_SUBMISSION.TEST_DRIVE='1'");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(assessment.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeTestDriveSubmissions(answer): db write failed");
		}

		// submission
		sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_SUBMISSION");
		sql.append(" WHERE MNEME_SUBMISSION.ASSESSMENT_ID=? AND MNEME_SUBMISSION.TEST_DRIVE='1'");

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeTestDriveSubmissions(submission): db write failed");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void removeTestDriveSubmissionsTx(String context)
	{
		// answer
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ANSWER");
		sql.append(" USING MNEME_ANSWER, MNEME_SUBMISSION");
		sql.append(" WHERE MNEME_ANSWER.SUBMISSION_ID=MNEME_SUBMISSION.ID AND MNEME_SUBMISSION.CONTEXT=? AND MNEME_SUBMISSION.TEST_DRIVE='1'");

		Object[] fields = new Object[1];
		fields[0] = context;

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeTestDriveSubmissions(context,answer): db write failed");
		}

		// submission
		sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_SUBMISSION");
		sql.append(" WHERE MNEME_SUBMISSION.CONTEXT=? AND MNEME_SUBMISSION.TEST_DRIVE='1'");

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeTestDriveSubmissions(context,submission): db write failed");
		}
	}

	/**
	 * Update an existing submission answer.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswer(final AnswerImpl answer)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateAnswerTx(answer);
			}
		}, "updateAnswer: " + answer.getId());
	}

	/**
	 * Update an existing submission answer evaluation.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswerEval(final AnswerImpl answer)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateAnswerEvalTx(answer);
			}
		}, "updateAnswer: " + answer.getId());
	}

	/**
	 * Update an existing submission answer eval. (transaction code).
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswerEvalTx(AnswerImpl answer)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ANSWER SET");
		sql.append(" AUTO_SCORE=?, EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?, EVAL_SCORE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[7];
		fields[0] = answer.getAutoScore();
		fields[1] = (answer.getEvaluation().getAttribution().getDate() == null) ? null : answer.getEvaluation().getAttribution().getDate().getTime();
		fields[2] = answer.getEvaluation().getAttribution().getUserId();
		fields[3] = answer.getEvaluation().getComment();
		fields[4] = answer.getEvaluation().getEvaluated() ? "1" : "0";
		fields[5] = answer.getEvaluation().getScore() == null ? null : Float.valueOf(answer.getEvaluation().getScore());
		fields[6] = Long.valueOf(answer.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateAnswerEvalTx: db write failed");
		}
	}

	/**
	 * Update an existing submission answer (transaction code).
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void updateAnswerTx(AnswerImpl answer)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_ANSWER SET");
		sql.append(" ANSWERED=?, AUTO_SCORE=?, GUEST=?, EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?,");
		sql.append(" EVAL_SCORE=?, REASON=?, REVIEW=?, SUBMITTED_DATE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[12];
		fields[0] = answer.getIsAnswered();
		fields[1] = answer.getAutoScore();
		fields[2] = SqlHelper.encodeStringArray(answer.getTypeSpecificAnswer().getData());
		fields[3] = (answer.getEvaluation().getAttribution().getDate() == null) ? null : answer.getEvaluation().getAttribution().getDate().getTime();
		fields[4] = answer.getEvaluation().getAttribution().getUserId();
		fields[5] = answer.getEvaluation().getComment();
		fields[6] = answer.getEvaluation().getEvaluated() ? "1" : "0";
		fields[7] = answer.getEvaluation().getScore() == null ? null : Float.valueOf(answer.getEvaluation().getScore());
		fields[8] = answer.getReason();
		fields[9] = answer.getMarkedForReview() ? "1" : "0";
		fields[10] = (answer.getSubmittedDate() == null) ? null : answer.getSubmittedDate().getTime();
		fields[11] = Long.valueOf(answer.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateAnswerTx: db write failed");
		}
	}

	/**
	 * Update an existing submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmission(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateSubmissionTx(submission);
			}
		}, "updateSubmission: " + submission.getId());
	}

	/**
	 * Update an existing submission Eval.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionEval(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateSubmissionEvalTx(submission);
			}
		}, "updateSubmissionEval: " + submission.getId());
	}

	/**
	 * Update an existing submission Eval. (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionEvalTx(SubmissionImpl submission)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_SUBMISSION SET");
		sql.append(" EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?, EVAL_SCORE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[6];
		fields[0] = (submission.getEvaluation().getAttribution().getDate() == null) ? null : submission.getEvaluation().getAttribution().getDate()
				.getTime();
		fields[1] = submission.getEvaluation().getAttribution().getUserId();
		fields[2] = submission.getEvaluation().getComment();
		fields[3] = submission.getEvaluation().getEvaluated() ? "1" : "0";
		fields[4] = submission.getEvaluation().getScore() == null ? null : Float.valueOf(submission.getEvaluation().getScore());
		fields[5] = Long.valueOf(submission.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateSubmissionEvalTx: db write failed");
		}
	}

	/**
	 * Update an existing submission's released status.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionReleased(final SubmissionImpl submission)
	{
		this.sqlService.transact(new Runnable()
		{
			public void run()
			{
				updateSubmissionReleasedTx(submission);
			}
		}, "updateSubmissionReleased: " + submission.getId());
	}

	/**
	 * Update an existing submission's released status (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionReleasedTx(SubmissionImpl submission)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_SUBMISSION SET");
		sql.append(" RELEASED=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[2];
		fields[0] = submission.getIsReleased() ? "1" : "0";
		fields[1] = Long.valueOf(submission.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateSubmissionReleasedTx: db write failed");
		}
	}

	/**
	 * Update an existing submission (transaction code).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void updateSubmissionTx(SubmissionImpl submission)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE MNEME_SUBMISSION SET");
		sql.append(" COMPLETE=?, EVAL_ATRIB_DATE=?, EVAL_ATRIB_USER=?, EVAL_COMMENT=?, EVAL_EVALUATED=?,");
		sql.append(" EVAL_SCORE=?, RELEASED=?, SUBMITTED_DATE=?");
		sql.append(" WHERE ID=?");

		Object[] fields = new Object[9];
		fields[0] = submission.getIsComplete() ? "1" : "0";
		fields[1] = (submission.getEvaluation().getAttribution().getDate() == null) ? null : submission.getEvaluation().getAttribution().getDate()
				.getTime();
		fields[2] = submission.getEvaluation().getAttribution().getUserId();
		fields[3] = submission.getEvaluation().getComment();
		fields[4] = submission.getEvaluation().getEvaluated() ? "1" : "0";
		fields[5] = submission.getEvaluation().getScore() == null ? null : Float.valueOf(submission.getEvaluation().getScore());
		fields[6] = submission.getIsReleased() ? "1" : "0";
		fields[7] = (submission.getSubmittedDate() == null) ? null : submission.getSubmittedDate().getTime();
		fields[8] = Long.valueOf(submission.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields))
		{
			throw new RuntimeException("updateSubmissionTx: db write failed");
		}
	}
}
