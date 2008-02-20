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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Question;
import org.sakaiproject.util.ResourceLoader;

/**
 * AssessmentStorageOracle implements AssessmentStorage for MySQL.
 */
public class AssessmentStorageMysql extends AssessmentStorageSql implements AssessmentStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentStorageMysql.class);

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if we are auto-creating our schema, check and create
		if (autoDdl)
		{
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_assessment");
			this.sqlService.ddl(this.getClass().getClassLoader(), "mneme_assessment_1_0-1_1");
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
		sql.append(" GRADING_ANONYMOUS, GRADING_AUTO_RELEASE, GRADING_GRADEBOOK, GRADING_REJECTED,");
		sql.append(" HONOR_PLEDGE, LIVE, LOCKED, MINT, MODIFIED_BY_DATE, MODIFIED_BY_USER,");
		sql.append(" PARTS_CONTINUOUS, PARTS_SHOW_PRES, PASSWORD, PRESENTATION_TEXT,");
		sql.append(" PUBLISHED, QUESTION_GROUPING, RANDOM_ACCESS,");
		sql.append(" REVIEW_DATE, REVIEW_SHOW_CORRECT, REVIEW_SHOW_FEEDBACK, REVIEW_TIMING,");
		sql.append(" SHOW_HINTS, SUBMIT_PRES_TEXT, TIME_LIMIT, TITLE, TRIES, TYPE)");
		sql.append(" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		Object[] fields = new Object[35];
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
		fields[i++] = assessment.getGrading().getGradebookRejectedAssessment() ? "1" : "0";
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
	protected void removeDependencyTx(Pool pool)
	{
		// remove from in non-live assessments part details that use the pool
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM MNEME_ASSESSMENT_PART_DETAIL");
		sql.append(" USING MNEME_ASSESSMENT_PART_DETAIL, MNEME_ASSESSMENT");
		sql.append(" WHERE MNEME_ASSESSMENT_PART_DETAIL.ASSESSMENT_ID=MNEME_ASSESSMENT.ID AND MNEME_ASSESSMENT.LOCKED='0'");
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
		sql.append(" USING MNEME_ASSESSMENT_PART_DETAIL, MNEME_ASSESSMENT");
		sql.append(" WHERE MNEME_ASSESSMENT_PART_DETAIL.ASSESSMENT_ID=MNEME_ASSESSMENT.ID AND MNEME_ASSESSMENT.LOCKED='0'");
		sql.append(" AND QUESTION_ID=?");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(question.getId());

		if (!this.sqlService.dbWrite(sql.toString(), fields, null))
		{
			throw new RuntimeException("removeDependencyTx(question): db write failed");
		}
	}
}
