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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.GradesService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;

/**
 * GradesServiceGradebook23Impl implements GradesService, using the Sakai Gradebook, version 2.3, as the grading authority.
 */
public class GradesServiceGradebook23Impl implements GradesService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(GradesServiceGradebook23Impl.class);

	/** Dependency: AssessmentService */
	protected AssessmentService assessmentService = null;

	/** Dependency: GradebookExternalAssessmentService */
	// for 2.4 only: protected GradebookExternalAssessmentService m_gradebookService = null;
	protected GradebookService gradebookService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SubmissionService */
	protected SubmissionService submissionService = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager threadLocalManager = null;

	/** Dependenct: UserDirectoryService. */
	protected UserDirectoryService userDirectoryService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean assessmentReported(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		try
		{
			boolean hasGradebook = gradebookService.isGradebookDefined(assessment.getContext());
			if (hasGradebook)
			{
				boolean reported = gradebookService.isExternalAssignmentDefined(assessment.getContext(), assessment.getTitle());
				return Boolean.valueOf(reported);
			}
		}
		catch (Exception e)
		{
			M_log.warn("assessmentReported:", e);
		}

		return Boolean.FALSE;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
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
	public Boolean reportAssessmentGrades(Assessment assessment)
	{
		if (assessment == null) throw new IllegalArgumentException();

		// make sure we are published, valid and desire gradebook integration
		if (!(assessment.getPublished() && assessment.getGrading().getGradebookIntegration() && assessment.getIsValid())) return Boolean.FALSE;

		M_log.debug("reportAssessmentGrades: " + assessment.getId());

		try
		{
			// make sure there's a gradebook
			boolean hasGradebook = gradebookService.isGradebookDefined(assessment.getContext());
			if (hasGradebook)
			{
				// see if there's an entry
				boolean reported = gradebookService.isExternalAssignmentDefined(assessment.getContext(), assessment.getTitle());
				if (!reported)
				{
					// TODO: what url to use?
					String url = null;

					// make an entry for the assessment
					gradebookService.addExternalAssessment(assessment.getContext(), assessment.getTitle(), url, assessment.getTitle(), assessment
							.getParts().getTotalPoints(), assessment.getDates().getDueDate(), "Test Center");
				}

				// get the "official" submissions map of user id -> Float score (for released completed submissions)
				Map<String, Float> scores = this.submissionService.getAssessmentHighestScores(assessment, Boolean.TRUE);

				// make them double for gb
				Map<String, Double> dScores = new HashMap<String, Double>();
				for (Map.Entry entry : scores.entrySet())
				{
					String key = (String) entry.getKey();
					Float total = (Float) entry.getValue();
					dScores.put(key, (total == null) ? null : Double.valueOf(total.doubleValue()));
				}

				// report them
				gradebookService.updateExternalAssessmentScores(assessment.getContext(), assessment.getTitle(), dScores);

				return Boolean.TRUE;
			}
		}
		catch (GradebookNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}
		catch (AssessmentNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean reportSubmissionGrade(Submission submission)
	{
		if (submission == null) throw new IllegalArgumentException();

		// make sure we are published, valid and desire gradebook integration
		Assessment assessment = submission.getAssessment();
		if (!(assessment.getPublished() && assessment.getGrading().getGradebookIntegration() && assessment.getIsValid())) return Boolean.FALSE;

		// make sure we are complete
		if (!submission.getIsComplete()) return Boolean.FALSE;

		M_log.debug("reportSubmissionGrade: " + submission.getId());

		try
		{
			// make sure there's an entry
			boolean hasGradebook = gradebookService.isGradebookDefined(assessment.getContext());
			if (hasGradebook)
			{
				boolean reported = gradebookService.isExternalAssignmentDefined(assessment.getContext(), assessment.getTitle());
				if (reported)
				{
					Double dScore = null;

					// if not released, report the null score
					if (submission.getIsReleased())
					{
						// get this submission's user's "official" submission for this submission's assessment
						Float score = this.submissionService.getSubmissionOfficialScore(assessment, submission.getUserId());
						if (score != null)
						{
							dScore = Double.valueOf(score.doubleValue());
						}
					}

					// report it
					gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getTitle(), submission.getUserId(), dScore);

					return Boolean.TRUE;
				}
			}
		}
		catch (GradebookNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}
		catch (AssessmentNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean retractAssessmentGrades(Assessment assessment)
	{
		M_log.debug("retractAssessmentGrades: " + assessment.getId());

		try
		{
			boolean hasGradebook = gradebookService.isGradebookDefined(assessment.getContext());
			if (hasGradebook)
			{
				boolean reported = gradebookService.isExternalAssignmentDefined(assessment.getContext(), assessment.getTitle());
				if (reported)
				{
					gradebookService.removeExternalAssessment(assessment.getContext(), assessment.getTitle());
					return Boolean.TRUE;
				}
			}
		}
		catch (GradebookNotFoundException e)
		{
			M_log.warn("retractAssessmentGrades: ", e);
		}
		catch (AssessmentNotFoundException e)
		{
			M_log.warn("retractAssessmentGrades", e);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean retractSubmissionGrade(Submission submission)
	{
		Assessment assessment = submission.getAssessment();

		M_log.debug("retractSubmissionGrade: " + submission.getId());

		try
		{
			// make sure there's an entry
			boolean hasGradebook = gradebookService.isGradebookDefined(assessment.getContext());
			if (hasGradebook)
			{
				boolean reported = gradebookService.isExternalAssignmentDefined(assessment.getContext(), assessment.getTitle());
				if (reported)
				{
					// null retracts the score
					Double score = null;

					// report it
					gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getTitle(), submission.getUserId(), score);

					return Boolean.TRUE;
				}
			}
		}
		catch (GradebookNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}
		catch (AssessmentNotFoundException e)
		{
			M_log.warn("reportAssessmentGrades: " + assessment.getId(), e);
		}

		return Boolean.FALSE;
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Dependency: GradebookService.
	 * 
	 * @param service
	 *        The GradebookService.
	 */
	public void setGradebookService(/* for 2.4 only: GradebookExternalAssessmentService */GradebookService service)
	{
		this.gradebookService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
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
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		this.userDirectoryService = service;
	}

	// /**
	// * {@inheritDoc}
	// */
	// public void updateGradebook(Assessment assessment) throws AssessmentPermissionException
	// {
	// if (assessment == null) throw new IllegalArgumentException();
	//
	// if (M_log.isDebugEnabled()) M_log.debug("updateGradebook: " + assessment.getId());
	//
	// // check permission
	// securityService.secure(sessionManager.getCurrentSessionUserId(), MnemeService.GRADE_PERMISSION, assessment.getContext());
	//
	// // skip if there is no gradebook integration
	// if (!assessment.getGrading().getGradebookIntegration()) return;
	//
	// // try each user with a submission
	// List<String> userIds = this.storage.getUsersSubmitted(assessment);
	//
	// for (String uid : userIds)
	// {
	// // find the user's highest score among the completed submissions
	// Float highestScore = this.storage.getSubmissionHighestScore(assessment, uid);
	//
	// // push this into the GB
	// try
	// {
	// gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), uid, ((highestScore == null) ? null
	// : new Double(highestScore.doubleValue())));
	// }
	// catch (GradebookNotFoundException e)
	// {
	// // if there's no gradebook for this context, oh well...
	// M_log.warn("updateGradebook: (no gradebook for context): " + e);
	// }
	// catch (AssessmentNotFoundException e)
	// {
	// // if the assessment has not been registered in gb, this is a problem
	// M_log.warn("updateGradebook: (assessment has not been registered in context's gb): " + e);
	// }
	// }
	// }

	// /**
	// * Record this submission in the gradebook.
	// *
	// * @param submission
	// * The submission to record in the gradebook.
	// * @param refresh
	// * if true, get the latest score from the db, if false, use the final score from the submission.
	// */
	// protected void addToGradebook(Submission submission, boolean refresh)
	// {
	// if (submission == null) throw new IllegalArgumentException();
	//
	// Assessment assessment = submission.getAssessment();
	//
	// Double points = null;
	//
	// // refresh from the database if requested
	// if (refresh)
	// {
	// // read the final score for this submission from the db
	// points = this.storage.getSubmissionScore(submission).doubleValue();
	// }
	// else
	// {
	// // use the score from the submission record
	// points = submission.getTotalScore().doubleValue();
	// }
	//
	// // find the highest score recorded for this user and this assessment
	// Float highestScore = this.storage.getSubmissionHighestScore(assessment, submission.getUserId());
	// if ((highestScore != null) && (points.doubleValue() < highestScore.doubleValue()))
	// {
	// // if this submission's points is not highest, don't record this in GB
	// return;
	// }
	//
	// // post it
	// try
	// {
	// gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), submission.getUserId(), points);
	// }
	// catch (GradebookNotFoundException e)
	// {
	// // if there's no gradebook for this context, oh well...
	// M_log.warn("addToGradebook: (no gradebook for context): " + e);
	// }
	// catch (AssessmentNotFoundException e)
	// {
	// // if the assessment has not been registered in gb, this is a problem
	// M_log.warn("addToGradebook: (assessment has not been registered in context's gb): " + e);
	// }
	// }

	// /**
	// * Update the gradebook for ths submission.
	// *
	// * @param submission
	// * The submission to record in the gradebook.
	// * @param refresh
	// * if true, get the latest score from the db, if false, use the final score from the submission.
	// */
	// protected void recordInGradebook(Submission submission, boolean refresh)
	// {
	// if (submission.getIsReleased() && submission.getAssessment().getGrading().getGradebookIntegration())
	// {
	// addToGradebook(submission, refresh);
	// }
	//
	// // TODO: remove from gb
	// else
	// {
	//
	// }
	// }
}
