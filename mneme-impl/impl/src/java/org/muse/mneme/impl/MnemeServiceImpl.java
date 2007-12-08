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
import java.util.Collection;
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
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionCompletedException;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.user.api.User;

/**
 * MnemeServiceImpl implements MnemeService
 */
public class MnemeServiceImpl implements MnemeService, Runnable
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(MnemeServiceImpl.class);

	/** Dependency: AssessmentService */
	protected AssessmentService assessmentService = null;

	/** Dependency: AttachmentService */
	protected AttachmentService attachmentService = null;

	/** The checker thread. */
	protected Thread checkerThread = null;

	/** Dependency: FunctionManager */
	protected FunctionManager functionManager = null;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Question type plugins. */
	protected Map<String, QuestionPlugin> questionPlugins = new HashMap<String, QuestionPlugin>();

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService submissionService = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager threadLocalManager = null;

	/** The thread quit flag. */
	protected boolean threadStop = false;

	/** How long to wait (ms) between checks for timed-out submission in the db. 0 disables. */
	protected long timeoutCheckMs = 1000L * 60L * 60L * 12L;

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission)
	{
		return submissionService.allowCompleteSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */

	public Boolean allowEditAssessment(Assessment assessment)
	{
		return assessmentService.allowEditAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEditQuestion(Question question)
	{
		return questionService.allowEditQuestion(question);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(String context)
	{
		return this.submissionService.allowEvaluate(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowEvaluate(Submission submission)
	{
		return this.submissionService.allowEvaluate(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context)
	{
		return assessmentService.allowListDeliveryAssessment(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManageAssessments(String context)
	{
		return assessmentService.allowManageAssessments(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowManagePools(String context)
	{
		return poolService.allowManagePools(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowRemoveAssessment(Assessment assessment)
	{
		return this.assessmentService.allowRemoveAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission)
	{
		return submissionService.allowReviewSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Submission submission)
	{
		return submissionService.allowSubmit(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintAssessments()
	{
		this.assessmentService.clearStaleMintAssessments();
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintPools()
	{
		this.poolService.clearStaleMintPools();
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStaleMintQuestions()
	{
		this.questionService.clearStaleMintQuestions();
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
	public Assessment copyAssessment(String context, Assessment assessment) throws AssessmentPermissionException
	{
		return assessmentService.copyAssessment(context, assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool copyPool(String context, Pool pool) throws AssessmentPermissionException
	{
		return this.poolService.copyPool(context, pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public void copyPoolQuestions(Pool source, Pool destination) throws AssessmentPermissionException
	{
		questionService.copyPoolQuestions(source, destination);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question copyQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		return questionService.copyQuestion(question, pool);
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
	public Integer countAssessmentSubmissions(Assessment assessment, Boolean official, String allUid)
	{
		return submissionService.countAssessmentSubmissions(assessment, official, allUid);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(Pool pool, String search)
	{
		return this.questionService.countQuestions(pool, search);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countQuestions(String context, String search)
	{
		return questionService.countQuestions(context, search);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countSubmissionAnswers(Assessment assessment, Question question, Boolean official)
	{
		return submissionService.countSubmissionAnswers(assessment, question, official);
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		// stop the checking thread
		stop();

		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission enterSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		return submissionService.enterSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateAnswers(Collection<Answer> answers) throws AssessmentPermissionException
	{
		submissionService.evaluateAnswers(answers);
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateSubmission(Submission submission) throws AssessmentPermissionException
	{
		submissionService.evaluateSubmission(submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void evaluateSubmissions(Assessment assessment, String comment, Float score) throws AssessmentPermissionException
	{
		this.evaluateSubmissions(assessment, comment, score);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean existsPool(String poolId)
	{
		return this.poolService.existsPool(poolId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean existsQuestion(String questionid)
	{
		return this.questionService.existsQuestion(questionid);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> findAssessmentSubmissions(Assessment assessment, FindAssessmentSubmissionsSort sort, Boolean official, String allUid,
			Integer pageNum, Integer pageSize)
	{
		return this.submissionService.findAssessmentSubmissions(assessment, sort, official, allUid, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findPartQuestions(Part part)
	{
		return submissionService.findPartQuestions(part);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pool> findPools(String context, PoolService.FindPoolsSort sort, String search)
	{
		return poolService.findPools(context, sort, search);
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] findPrevNextSubmissionIds(Submission submission, FindAssessmentSubmissionsSort sort, Boolean official)
	{
		return submissionService.findPrevNextSubmissionIds(submission, sort, official);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(Pool pool, FindQuestionsSort sort, String search, String questionType, Integer pageNum, Integer pageSize)
	{
		return questionService.findQuestions(pool, sort, search, questionType, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> findQuestions(String context, FindQuestionsSort sort, String search, String questionType, Integer pageNum, Integer pageSize)
	{
		return questionService.findQuestions(context, sort, search, questionType, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Answer> findSubmissionAnswers(Assessment assessment, Question question, FindAssessmentSubmissionsSort sort, Boolean official,
			Integer pageNum, Integer pageSize)
	{
		return this.findSubmissionAnswers(assessment, question, sort, official, pageNum, pageSize);
	}

	/**
	 * {@inheritDoc}
	 */
	public Answer getAnswer(String answerId)
	{
		return submissionService.getAnswer(answerId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getArchivedAssessments(String context)
	{
		return this.assessmentService.getArchivedAssessments(context);
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
	public Boolean getAssessmentHasUnscoredSubmissions(Assessment assessment)
	{
		return submissionService.getAssessmentHasUnscoredSubmissions(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAssessmentQuestionHasUnscoredSubmissions(Assessment assessment, Question question)
	{
		return submissionService.getAssessmentQuestionHasUnscoredSubmissions(assessment, question);
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
	public List<Assessment> getContextAssessments(String context, AssessmentService.AssessmentsSort sort, Boolean publishedOnly)
	{
		return assessmentService.getContextAssessments(context, sort, publishedOnly);
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getNewUserAssessmentSubmission(Assessment assessment, String userId)
	{
		return submissionService.getNewUserAssessmentSubmission(assessment, userId);
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
	public List<Pool> getPools(String context)
	{
		return this.poolService.getPools(context);
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

		// sort- popularity (desc), then localized type name (asc)
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				// compare based on the localized type name
				int rv = -1 * ((QuestionPlugin) arg0).getPopularity().compareTo(((QuestionPlugin) arg1).getPopularity());
				if (rv == 0)
				{
					rv = ((QuestionPlugin) arg0).getTypeName().compareTo(((QuestionPlugin) arg1).getTypeName());
				}
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
	public Submission getSubmission(String id)
	{
		return submissionService.getSubmission(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<User> getSubmitUsers(String context)
	{
		return assessmentService.getSubmitUsers(context);
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
			// register functions
			functionManager.registerFunction(GRADE_PERMISSION);
			functionManager.registerFunction(MANAGE_PERMISSION);
			functionManager.registerFunction(SUBMIT_PERMISSION);

			// start the checking thread
			if (timeoutCheckMs > 0)
			{
				start();
			}

			M_log.info("init(): timout check seconds: " + timeoutCheckMs / 1000);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void moveQuestion(Question question, Pool pool) throws AssessmentPermissionException
	{
		questionService.moveQuestion(question, pool);
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
	public Pool newPool(String context) throws AssessmentPermissionException
	{
		return poolService.newPool(context);
	}

	/**
	 * {@inheritDoc}
	 */
	public Question newQuestion(Pool pool, String type) throws AssessmentPermissionException
	{
		return questionService.newQuestion(pool, type);
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
	public void releaseSubmissions(Assessment assessment, Boolean evaluatedOnly) throws AssessmentPermissionException
	{
		submissionService.releaseSubmissions(assessment, evaluatedOnly);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException
	{
		assessmentService.removeAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePool(Pool pool) throws AssessmentPermissionException
	{
		poolService.removePool(pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeQuestion(Question question) throws AssessmentPermissionException
	{
		questionService.removeQuestion(question);
	}

	/**
	 * {@inheritDoc}
	 */
	public void retractSubmissions(Assessment assessment) throws AssessmentPermissionException
	{
		this.submissionService.retractSubmissions(assessment);
	}

	/**
	 * Run the event checking thread.
	 */
	public void run()
	{
		// since we might be running while the component manager is still being created and populated,
		// such as at server startup, wait here for a complete component manager
		ComponentManager.waitTillConfigured();

		// loop till told to stop
		while ((!threadStop) && (!Thread.currentThread().isInterrupted()))
		{
			try
			{
				// ask the various services to clear their stale mints
				clearStaleMintQuestions();
				clearStaleMintPools();
				clearStaleMintAssessments();
			}
			catch (Throwable e)
			{
				M_log.warn("run: will continue: ", e);
			}
			finally
			{
				// clear out any current current bindings
				this.threadLocalManager.clear();
			}

			// take a small nap
			try
			{
				Thread.sleep(timeoutCheckMs);
			}
			catch (Exception ignore)
			{
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(Assessment assessment) throws AssessmentPermissionException, AssessmentPolicyException
	{
		assessmentService.saveAssessment(assessment);
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePool(Pool pool) throws AssessmentPermissionException
	{
		poolService.savePool(pool);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQuestion(Question question) throws AssessmentPermissionException
	{
		questionService.saveQuestion(question);
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
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * Set the # seconds to wait between db checks for timed-out submissions.
	 * 
	 * @param time
	 *        The # seconds to wait between db checks for timed-out submissions.
	 */
	public void setTimeoutCheckSeconds(String time)
	{
		this.timeoutCheckMs = Integer.parseInt(time) * 1000L;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean submissionsExist(Assessment assessment)
	{
		return this.submissionService.submissionsExist(assessment);
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

	/**
	 * Start the clean and report thread.
	 */
	protected void start()
	{
		threadStop = false;

		checkerThread = new Thread(this, getClass().getName());
		checkerThread.start();
	}

	/**
	 * Stop the clean and report thread.
	 */
	protected void stop()
	{
		if (checkerThread == null) return;

		// signal the thread to stop
		threadStop = true;

		// wake up the thread
		checkerThread.interrupt();

		checkerThread = null;
	}
}
