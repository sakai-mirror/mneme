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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.db.api.SqlService;

/**
 * MnemeServiceImpl implements MnemeService
 */
public class MnemeServiceImpl implements MnemeService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(MnemeServiceImpl.class);

	/** Dependency: AssessmentService */
	protected AssessmentService m_assessmentService = null;

	/** Dependency: AttachmentService */
	protected AttachmentService m_attachmentService = null;

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/** Dependency: FunctionManager */
	protected FunctionManager m_functionManager = null;

	/** Dependency: PoolService */
	protected PoolService m_poolService = null;

	/** Dependency: QuestionService */
	protected QuestionService m_questionService = null;

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService m_submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission, String userId)
	{
		return m_submissionService.allowCompleteSubmission(submission, userId);
	}

	/**
	 * {@inheritDoc}
	 */

	public Boolean allowEditAssessment(Assessment assessment, String userId)
	{
		return m_assessmentService.allowEditAssessment(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditPool(Pool pool, String context, String userId)
	{
		return m_poolService.allowEditPool(pool, context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question, String context, String userId)
	{
		return m_questionService.allowEditQuestion(question, context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context, String userId)
	{
		return m_assessmentService.allowListDeliveryAssessment(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageAssessments(String context, String userId)
	{
		return m_assessmentService.allowManageAssessments(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManagePools(String context, String userId)
	{
		return m_poolService.allowManagePools(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission, String userId)
	{
		return m_submissionService.allowReviewSubmission(submission, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Assessment assessment, String userId)
	{
		return m_submissionService.allowSubmit(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			SubmissionCompletedException
	{
		m_submissionService.completeSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessments(String context)
	{
		return m_assessmentService.countAssessments(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(Assessment assessment, String userId)
	{
		return m_submissionService.countRemainingSubmissions(assessment, userId);
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
	public Submission enterSubmission(Assessment assessment, String userId) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		return m_submissionService.enterSubmission(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String userId)
	{
		return m_poolService.findPools(userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String userId)
	{
		return m_questionService.findQuestions(userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getAssessment(String id)
	{
		return m_assessmentService.getAssessment(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		return m_submissionService.getAssessmentScores(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context)
	{
		return m_assessmentService.getContextAssessments(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool(String poolId)
	{
		return m_poolService.getPool(poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion(String questionId)
	{
		return m_questionService.getQuestion(questionId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		return m_submissionService.getQuestionScores(question);
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(String id)
	{
		return m_submissionService.getSubmission(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort)
	{
		return m_submissionService.getUserContextSubmissions(context, userId, sort);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				m_sqlService.ddl(this.getClass().getClassLoader(), "mneme");
			}

			// register functions
			m_functionManager.registerFunction(SUBMIT_PERMISSION);
			m_functionManager.registerFunction(MANAGE_PERMISSION);
			m_functionManager.registerFunction(GRADE_PERMISSION);

			M_log.info("init()");
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment newAssessment(String context) throws AssessmentPermissionException
	{
		return m_assessmentService.newAssessment(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool newPool(String context, String userId) throws AssessmentPermissionException
	{
		return m_poolService.newPool(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question newQuestion(String context, String userId) throws AssessmentPermissionException
	{
		return m_questionService.newQuestion(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(Assessment assessment) throws AssessmentPermissionException
	{
		m_assessmentService.removeAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool, String context) throws AssessmentPermissionException
	{
		m_poolService.removePool(pool, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question, String context) throws AssessmentPermissionException
	{
		m_questionService.removeQuestion(question, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(Assessment assessment) throws AssessmentPermissionException
	{
		m_assessmentService.saveAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(Pool pool, String context) throws AssessmentPermissionException
	{
		m_poolService.savePool(pool, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question, String context) throws AssessmentPermissionException
	{
		m_questionService.saveQuestion(question, context);
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		m_assessmentService = service;
	}

	/**
	 * Dependency: AttachmentService.
	 * 
	 * @param service
	 *        The AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		m_attachmentService = service;
	}

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/**
	 * Dependency: FunctionManager.
	 * 
	 * @param service
	 *        The FunctionManager.
	 */
	public void setFunctionManager(FunctionManager service)
	{
		m_functionManager = service;
	}

	/**
	 * Dependency: PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		m_poolService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		m_questionService = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Dependency: SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		m_submissionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(Answer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		m_submissionService.submitAnswer(answer, completeAnswer, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswers(List<Answer> answers, Boolean completeAnswers, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		m_submissionService.submitAnswers(answers, completeAnswers, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateGradebook(Assessment assessment) throws AssessmentPermissionException
	{
		m_submissionService.updateGradebook(assessment);
	}
}
