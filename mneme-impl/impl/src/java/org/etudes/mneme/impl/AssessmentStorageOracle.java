/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Etudes, Inc.
 * 
 * Portions completed before September 1, 2008
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

package org.etudes.mneme.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.mneme.api.AssessmentAccess;
import org.etudes.mneme.api.Part;
import org.etudes.mneme.api.PartDetail;
import org.etudes.mneme.api.Pool;
import org.etudes.mneme.api.PoolDraw;
import org.etudes.mneme.api.Question;
import org.etudes.mneme.api.QuestionPick;
import org.etudes.mneme.api.ReviewShowCorrect;
import org.sakaiproject.util.ResourceLoader;

/**
 * AssessmentStorageOracle implements AssessmentStorage for Oracle.
 */
public class AssessmentStorageOracle extends AssessmentStorageSql implements AssessmentStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentStorageOracle.class);

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
	 * Insert a new assessment access (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessmentAccessTx(AssessmentImpl assessment, AssessmentAccessImpl access)
	{
		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_ASSESSMENT_ACCESS_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_ACCESS (ID,");
		sql.append(" ASSESSMENT_ID, DATES_ACCEPT_UNTIL, DATES_DUE, DATES_OPEN, HIDE_UNTIL_OPEN,");
		sql.append(" OVERRIDE_ACCEPT_UNTIL, OVERRIDE_DUE, OVERRIDE_OPEN, OVERRIDE_PASSWORD,");
		sql.append(" OVERRIDE_TIME_LIMIT, OVERRIDE_TRIES, OVERRIDE_HIDE_UNTIL_OPEN, PASSWORD, TIME_LIMIT, TRIES, USERS)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[17];
		int i = 0;
		fields[i++] = id;
		fields[i++] = Long.valueOf(assessment.getId());
		fields[i++] = (access.getAcceptUntilDate() == null) ? null : access.getAcceptUntilDate().getTime();
		fields[i++] = (access.getDueDate() == null) ? null : access.getDueDate().getTime();
		fields[i++] = (access.getOpenDate() == null) ? null : access.getOpenDate().getTime();
		fields[i++] = access.getHideUntilOpen() ? "1" : "0";
		fields[i++] = access.getOverrideAcceptUntilDate() ? "1" : "0";
		fields[i++] = access.getOverrideDueDate() ? "1" : "0";
		fields[i++] = access.getOverrideOpenDate() ? "1" : "0";
		fields[i++] = access.getOverridePassword() ? "1" : "0";
		fields[i++] = access.getOverrideTimeLimit() ? "1" : "0";
		fields[i++] = access.getOverrideTries() ? "1" : "0";
		fields[i++] = access.getOverrideHideUntilOpen() ? "1" : "0";
		fields[i++] = access.getPassword().getPassword();
		fields[i++] = access.getTimeLimit();
		fields[i++] = access.getTries();
		fields[i++] = SqlHelper.encodeStringArray(access.getUsers().toArray(new String[access.getUsers().size()]));

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("insertAssessmentAccessTx: dbWrite failed");
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
	protected void insertAssessmentPartDetailTx(AssessmentImpl assessment, PartImpl part, PartDetailImpl detail)
	{
		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_ASSESSMENT_DETAIL_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_PART_DETAIL (ID,");
		sql.append(" ASSESSMENT_ID, NUM_QUESTIONS_SEQ, ORIG_PID, ORIG_QID, PART_ID, POOL_ID, QUESTION_ID, SEQ, POINTS)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[10];
		fields[0] = id;
		fields[1] = Long.valueOf(assessment.getId());
		int i = 2;

		if (detail instanceof QuestionPick)
		{
			QuestionPick pick = (QuestionPick) detail;

			fields[i++] = Integer.valueOf(1);
			fields[i++] = null;
			fields[i++] = (pick.getOrigQuestionId() == null) ? null : Long.valueOf(pick.getOrigQuestionId());
			fields[i++] = Long.valueOf(part.getId());
			fields[i++] = null;
			fields[i++] = Long.valueOf(pick.getQuestionId());
			fields[i++] = Integer.valueOf(((PartDetailImpl) detail).getSeq());
			fields[i++] = detail.getPoints();
		}

		else if (detail instanceof PoolDraw)
		{
			PoolDraw draw = (PoolDraw) detail;

			fields[i++] = Integer.valueOf(draw.getNumQuestions());
			fields[i++] = draw.getOrigPoolId() == null ? null : Long.valueOf(draw.getOrigPoolId());
			fields[i++] = null;
			fields[i++] = Long.valueOf(part.getId());
			fields[i++] = Long.valueOf(draw.getPoolId());
			fields[i++] = null;
			fields[i++] = Integer.valueOf(((PartDetailImpl) detail).getSeq());
			fields[i++] = detail.getPoints();
		}

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("insertAssessmentPartDetailTx: dbWrite failed");
		}

		// set the detail's id
		((PartDetailImpl) detail).initId(id.toString());
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
		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_ASSESSMENT_PART_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT_PART (ID,");
		sql.append(" ASSESSMENT_ID, PRESENTATION_TEXT, SEQUENCE, TITLE, TYPE, RANDOMIZE)");
		sql.append(" VALUES(?,?,?,?,?,?,?)");

		Object[] fields = new Object[7];
		fields[0] = id;
		fields[1] = Long.valueOf(assessment.getId());
		fields[2] = part.getPresentation().getText();
		fields[3] = part.getOrdering().getPosition();
		fields[4] = part.getTitle();
		fields[5] = "H";
		fields[6] = part.getRandomize() ? "1" : "0";

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("insertAssessmentPartsTx: dbWrite failed");
		}

		// set the part's id
		((PartImpl) part).initId(id.toString());

		// part details
		for (PartDetail detail : part.getDetails())
		{
			insertAssessmentPartDetailTx(assessment, (PartImpl) part, (PartDetailImpl) detail);
		}
	}

	/**
	 * Insert a new assessment (transaction code).
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void insertAssessmentTx(AssessmentImpl assessment)
	{
		// get the next id
		Long id = this.sqlService.getNextSequence("MNEME_ASSESSMENT_SEQ", null);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO MNEME_ASSESSMENT (ID,");
		sql.append(" ARCHIVED, CONTEXT, CREATED_BY_DATE, CREATED_BY_USER,");
		sql.append(" DATES_ACCEPT_UNTIL, DATES_ARCHIVED, DATES_DUE, DATES_OPEN, HIDE_UNTIL_OPEN,");
		sql.append(" GRADING_ANONYMOUS, GRADING_AUTO_RELEASE, GRADING_GRADEBOOK, GRADING_REJECTED, FORMAL_EVAL, NOTIFY_EVAL, EVAL_SENT, RESULTS_EMAIL,");
		sql.append(" RESULTS_SENT, HONOR_PLEDGE, LIVE, LOCKED, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER,");
		sql.append(" PARTS_CONTINUOUS, PARTS_SHOW_PRES, PASSWORD, PRESENTATION_TEXT,");
		sql.append(" PUBLISHED, FROZEN, QUESTION_GROUPING, RANDOM_ACCESS,");
		sql.append(" REVIEW_DATE, REVIEW_SHOW_CORRECT, REVIEW_SHOW_FEEDBACK, REVIEW_SHOW_SUMMARY,  REVIEW_TIMING, MIN_SCORE_SET, MIN_SCORE, ");
		sql.append(" SHOW_HINTS, SHOW_MODEL_ANSWER, SUBMIT_PRES_TEXT, TIME_LIMIT, TITLE, TRIES, TYPE, POOL, NEEDSPOINTS, SHUFFLE_CHOICES)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[50];
		int i = 0;
		fields[i++] = id;
		fields[i++] = assessment.getArchived() ? "1" : "0";
		fields[i++] = assessment.getContext();
		fields[i++] = assessment.getCreatedBy().getDate().getTime();
		fields[i++] = assessment.getCreatedBy().getUserId();
		fields[i++] = (assessment.getDates().getAcceptUntilDate() == null) ? null : assessment.getDates().getAcceptUntilDate().getTime();
		fields[i++] = (assessment.getDates().getArchivedDate() == null) ? null : assessment.getDates().getArchivedDate().getTime();
		fields[i++] = (assessment.getDates().getDueDate() == null) ? null : assessment.getDates().getDueDate().getTime();
		fields[i++] = (assessment.getDates().getOpenDate() == null) ? null : assessment.getDates().getOpenDate().getTime();
		fields[i++] = assessment.getDates().getHideUntilOpen() ? "1" : "0";
		fields[i++] = assessment.getGrading().getAnonymous() ? "1" : "0";
		fields[i++] = assessment.getGrading().getAutoRelease() ? "1" : "0";
		fields[i++] = assessment.getGrading().getGradebookIntegration() ? "1" : "0";
		fields[i++] = assessment.getGrading().getGradebookRejectedAssessment() ? "1" : "0";
		fields[i++] = assessment.getFormalCourseEval() ? "1" : "0";
		fields[i++] = assessment.getNotifyEval() ? "1" : "0";
		fields[i++] = (assessment.getEvaluationSent() == null) ? null : assessment.getEvaluationSent().getTime();
		fields[i++] = (assessment.getResultsEmail() != null && assessment.getResultsEmail().length() > 255) ? assessment.getResultsEmail().substring(0, 255): assessment.getResultsEmail();
		fields[i++] = (assessment.getResultsSent() == null) ? null : assessment.getResultsSent().getTime();
		fields[i++] = assessment.getRequireHonorPledge() ? "1" : "0";
		fields[i++] = assessment.getIsLive() ? "1" : "0";
		fields[i++] = assessment.getIsLocked() ? "1" : "0";
		fields[i++] = assessment.getMint() ? "1" : "0";
		fields[i++] = assessment.getModifiedBy().getDate().getTime();
		fields[i++] = assessment.getModifiedBy().getUserId();
		fields[i++] = assessment.getParts().getContinuousNumbering() ? "1" : "0";
		fields[i++] = ((AssessmentPartsImpl) assessment.getParts()).showPresentation == null ? null
				: (((AssessmentPartsImpl) assessment.getParts()).showPresentation ? "1" : "0");
		fields[i++] = assessment.getPassword().getPassword();
		fields[i++] = assessment.getPresentation().getText();
		fields[i++] = assessment.getPublished() ? "1" : "0";
		fields[i++] = assessment.getFrozen() ? "1" : "0";
		fields[i++] = assessment.getQuestionGrouping().toString();
		fields[i++] = assessment.getRandomAccess() ? "1" : "0";
		fields[i++] = (assessment.getReview().getDate() == null) ? null : assessment.getReview().getDate().getTime();
		fields[i++] = assessment.getReview().getShowCorrectAnswer().equals(ReviewShowCorrect.yes) ? "1" : (assessment.getReview()
				.getShowCorrectAnswer().equals(ReviewShowCorrect.no) ? "0" : (assessment.getReview().getShowCorrectAnswer().equals(ReviewShowCorrect.correct_only) ? "C" : "I"));
		fields[i++] = assessment.getReview().getShowFeedback() ? "1" : "0";
		fields[i++] = assessment.getReview().getShowSummary() ? "1" : "0";
		fields[i++] = assessment.getReview().getTiming().toString();
		fields[i++] = (assessment.getMinScoreSet() && assessment.getMinScore() != null) ? "1" : "0";
		fields[i++] = assessment.getMinScore();
		
		fields[i++] = assessment.getShowHints() ? "1" : "0";
		fields[i++] = assessment.getShowModelAnswer() ? "1" : "0";
		fields[i++] = assessment.getSubmitPresentation().getText();
		fields[i++] = assessment.getTimeLimit();
		fields[i++] = assessment.getTitle();
		fields[i++] = assessment.getTries();
		fields[i++] = assessment.getType().toString();
		fields[i++] = ((AssessmentImpl) assessment).poolId == null ? null : Long.valueOf(((AssessmentImpl) assessment).poolId);
		fields[i++] = assessment.getNeedsPoints() ? "1" : "0";
		fields[i++] = assessment.getShuffleChoicesOverride() == null ? null : (assessment.getShuffleChoicesOverride() ? "1" : "0");

		if (!this.sqlService.dbWrite(null, sql.toString(), fields))
		{
			throw new RuntimeException("updateAssessmentTx: dbWrite failed");
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
	protected void removeDependencyTx(Pool pool)
	{
		// remove from non-live assessments part details that use the pool
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE ASSESSMENT_ID IN (SELECT ID FROM MNEME_ASSESSMENT WHERE LOCKED='0')");
		sql.append(" AND POOL_ID=?");

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
		// remove from in non-live assessments part details that use the question
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" WHERE ASSESSMENT_ID IN (SELECT ID FROM MNEME_ASSESSMENT WHERE LOCKED='0')");
		sql.append(" AND QUESTION_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeDependencyTx(question): db write failed");
		}
	}
}
