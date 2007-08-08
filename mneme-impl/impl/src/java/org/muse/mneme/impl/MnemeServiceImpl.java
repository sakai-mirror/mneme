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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.muse.mneme.api.PolicyException;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionCounts;
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
	protected AssessmentService assessmentService = null;

	/** Dependency: AttachmentService */
	protected AttachmentService attachmentService = null;

	/** Dependency: FunctionManager */
	protected FunctionManager functionManager = null;

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Question type plugins. */
	protected Map<String, QuestionPlugin> questionPlugins = new HashMap<String, QuestionPlugin>();

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission, String userId)
	{
		return submissionService.allowCompleteSubmission(submission, userId);
	}

	/**
	 * {@inheritDoc}
	 */

	public Boolean allowEditAssessment(Assessment assessment, String userId)
	{
		return assessmentService.allowEditAssessment(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditPool(Pool pool, String context, String userId)
	{
		return poolService.allowEditPool(pool, context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question, String context, String userId)
	{
		return questionService.allowEditQuestion(question, context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context, String userId)
	{
		return assessmentService.allowListDeliveryAssessment(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageAssessments(String context, String userId)
	{
		return assessmentService.allowManageAssessments(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManagePools(String context, String userId)
	{
		return poolService.allowManagePools(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowRemoveAssessment(Assessment assessment, String userId)
	{
		return this.assessmentService.allowRemoveAssessment(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission, String userId)
	{
		return submissionService.allowReviewSubmission(submission, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Assessment assessment, String userId)
	{
		return submissionService.allowSubmit(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			SubmissionCompletedException
	{
		submissionService.completeSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessments(String context)
	{
		return assessmentService.countAssessments(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countPools(String context, String userId, String search)
	{
		return poolService.countPools(context, userId, search);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(String userId, Pool pool, String search)
	{
		return this.questionService.countQuestions(userId, pool, search);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(Assessment assessment, String userId)
	{
		return submissionService.countRemainingSubmissions(assessment, userId);
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
		return submissionService.enterSubmission(assessment, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String context, String userId, PoolService.FindPoolsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		return poolService.findPools(context, userId, sort, search, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String userId, Pool pool, FindQuestionsSort sort, String search, Integer pageNum, Integer pageSize)
	{
		return questionService.findQuestions(userId, pool, sort, search, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getAssessment(String id)
	{
		return assessmentService.getAssessment(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		return submissionService.getAssessmentScores(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context, AssessmentService.AssessmentsSort sort)
	{
		return assessmentService.getContextAssessments(context, sort);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool(String poolId)
	{
		return poolService.getPool(poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion(String questionId)
	{
		return questionService.getQuestion(questionId);
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPlugin getQuestionPlugin(String type)
	{
		return this.questionPlugins.get(type);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<QuestionPlugin> getQuestionPlugins()
	{
		List<QuestionPlugin> rv = new ArrayList<QuestionPlugin>(this.questionPlugins.values());

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				// compare based on the localized type name
				int rv = ((QuestionPlugin) arg0).getTypeName().compareTo(((QuestionPlugin) arg1).getTypeName());
				return rv;
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(Question question)
	{
		return submissionService.getQuestionScores(question);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSubjects(String context, String userId)
	{
		return this.poolService.getSubjects(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(String id)
	{
		return submissionService.getSubmission(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionCounts getSubmissionCounts(Assessment assessment)
	{
		return this.submissionService.getSubmissionCounts(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(String context, String userId, GetUserContextSubmissionsSort sort)
	{
		return submissionService.getUserContextSubmissions(context, userId, sort);
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
				sqlService.ddl(this.getClass().getClassLoader(), "mneme");
			}

			// register functions
			functionManager.registerFunction(SUBMIT_PERMISSION);
			functionManager.registerFunction(MANAGE_PERMISSION);
			functionManager.registerFunction(GRADE_PERMISSION);

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
		return assessmentService.newAssessment(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool newPool(String context, String userId) throws AssessmentPermissionException
	{
		return poolService.newPool(context, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question newQuestion(String context, String userId, Pool pool, String type) throws AssessmentPermissionException
	{
		return questionService.newQuestion(context, userId, pool, type);
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerQuestionPlugin(QuestionPlugin plugin)
	{
		this.questionPlugins.put(plugin.getType(), plugin);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(Assessment assessment) throws AssessmentPermissionException, PolicyException
	{
		assessmentService.removeAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool, String context) throws AssessmentPermissionException
	{
		poolService.removePool(pool, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question, String context) throws AssessmentPermissionException
	{
		questionService.removeQuestion(question, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(Assessment assessment) throws AssessmentPermissionException
	{
		assessmentService.saveAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(Pool pool, String context) throws AssessmentPermissionException
	{
		poolService.savePool(pool, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question, String context) throws AssessmentPermissionException
	{
		questionService.saveQuestion(question, context);
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		assessmentService = service;
	}

	/**
	 * Dependency: AttachmentService.
	 * 
	 * @param service
	 *        The AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		attachmentService = service;
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
		functionManager = service;
	}

	/**
	 * Dependency: PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		poolService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		questionService = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		sqlService = service;
	}

	/**
	 * Dependency: SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		submissionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(Answer answer, Boolean completeAnswer, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		submissionService.submitAnswer(answer, completeAnswer, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswers(List<Answer> answers, Boolean completeAnswers, Boolean completeSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		submissionService.submitAnswers(answers, completeAnswers, completeSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateGradebook(Assessment assessment) throws AssessmentPermissionException
	{
		submissionService.updateGradebook(assessment);
	}
}
