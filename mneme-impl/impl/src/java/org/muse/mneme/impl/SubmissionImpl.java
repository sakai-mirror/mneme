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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentSubmissionStatus;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.Expiration;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.util.StringUtil;

/**
 * <p>
 * SubmissionImpl is ...
 * </p>
 */
public class SubmissionImpl implements Submission
{
	/** Each property may be not yet set, already set from persistence, or modified since. */
	enum PropertyStatus
	{
		inited, modified, unset
	}

	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionImpl.class);

	protected List<SubmissionAnswerImpl> answers = new ArrayList<SubmissionAnswerImpl>();

	protected PropertyStatus answersStatus = PropertyStatus.unset;

	/** Don't hold (anc cache) the actual assessment. */
	protected String assessmentId = null;

	protected PropertyStatus assessmentIdStatus = PropertyStatus.unset;

	protected String bestSubmissionId = null;

	protected String evalComment = null;

	protected PropertyStatus evalCommentStatus = PropertyStatus.unset;

	protected Float evalScore = null;

	protected PropertyStatus evalScoreStatus = PropertyStatus.unset;

	protected String id = null;

	protected PropertyStatus idStatus = PropertyStatus.unset;

	protected Boolean isComplete = null;

	protected PropertyStatus isCompleteStatus = PropertyStatus.unset;

	/** Tracks when we have read the entire main property set. */
	protected PropertyStatus mainStatus = PropertyStatus.unset;

	protected AssessmentServiceImpl service = null;

	protected Integer siblingCount = null;

	protected Time startDate = null;

	protected PropertyStatus startDateStatus = PropertyStatus.unset;

	protected Integer status = null;

	protected PropertyStatus statusStatus = PropertyStatus.unset;

	protected Time submittedDate = null;

	protected PropertyStatus submittedDateStatus = PropertyStatus.unset;

	/** This is a pre-compute for the total score (trust me!), to be used if set and we don't have the answers & evaluations. */
	protected Float totalScore = null;

	protected String userId = null;

	protected PropertyStatus userIdStatus = PropertyStatus.unset;

	/**
	 * Construct
	 */
	public SubmissionImpl(AssessmentServiceImpl service)
	{
		this.service = service;
	}

	/**
	 * Construct as a deep copy of another
	 */
	protected SubmissionImpl(SubmissionImpl other)
	{
		setMain(other);
		setAnswers(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof Submission)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare?
		int compare = getId().compareTo(((Submission) obj).getId());

		return compare;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean completeIfOver()
	{
		if (getIsOver(null, 0).booleanValue())
		{
			Time over = getWhenOver();
			service.completeTheSubmission(over, this);
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Submission)) return false;
		if (this == obj) return true;
		if (this.getId() == null) return false;
		if (((Submission) obj).getId() == null) return false;
		return ((Submission) obj).getId().equals(getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionAnswer getAnswer(AssessmentQuestion question)
	{
		if (question == null) return null;

		// read the answers if needed
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		SubmissionAnswerImpl answer = findAnswer(question.getId());
		if (answer != null) return answer;

		// not found, add one
		answer = new SubmissionAnswerImpl();
		answer.initSubmission(this);
		this.answers.add(answer);

		answer.initQuestion(question);

		this.answersStatus = PropertyStatus.modified;

		return answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends SubmissionAnswer> getAnswers()
	{
		// read the answers info if this property has not yet been set
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		return this.answers;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAnswersAutoScore()
	{
		// read the answers info if this property has not yet been set
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		// count the answer auto scores
		float total = 0;
		for (SubmissionAnswerImpl answer : this.answers)
		{
			// add it up
			total += answer.getAutoScore().floatValue();
		}

		return new Float(total);
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getAssessment()
	{
		// read the basic info if this property has not yet been set
		if (this.assessmentIdStatus == PropertyStatus.unset) readMain();

		// cache the actual assessment for the thread, to avoid the real assessment cache's copy-out policy

		// Note: this is safe!  We have to assure two things:
		// 1- when we modify the assessment, we don't get concurrent mod exception, and
		// 2- when the assessment's lazy parts are updated, that is not repeated
		// 1 is ok because we are sharing ONLY in the single thread.
		// 2 is good because when the shared object gets un-lazyed, the object is updated, so when we next access the shared object it will have the lazy parts read.
		// -ggolden

		String key = "submissionAssessment_" + this.id + "_" + this.assessmentId;
		AssessmentImpl assessment = (AssessmentImpl) this.service.m_threadLocalManager.get(key);
		if (assessment == null)
		{
			assessment = (AssessmentImpl) this.service.idAssessment(this.assessmentId);

			// set the submision context
			assessment.initSubmissionContext(this);

			// thread cache it
			this.service.m_threadLocalManager.set(key, assessment);
		}

		return assessment;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSubmissionStatus getAssessmentSubmissionStatus()
	{
		Time now = TimeService.newTime();
		Assessment assessment = getAssessment();

		// if not open yet...
		if ((assessment.getReleaseDate() != null) && now.before(assessment.getReleaseDate()))
		{
			return AssessmentSubmissionStatus.future;
		}

		// overdue?
		boolean overdue = (assessment.getDueDate() != null) && now.after(assessment.getDueDate())
				&& ((assessment.getAllowLateSubmit() == null) || (!assessment.getAllowLateSubmit().booleanValue()));

		// todo (not overdue)
		if ((getStartDate() == null) && !overdue)
		{
			return AssessmentSubmissionStatus.ready;
		}

		// if in progress...
		if (((getIsComplete() == null) || (!getIsComplete().booleanValue())) && (getStartDate() != null))
		{
			// if timed, add an alert
			if (assessment.getTimeLimit() != null)
			{
				return AssessmentSubmissionStatus.inProgressAlert;
			}

			return AssessmentSubmissionStatus.inProgress;
		}

		// completed
		if ((getIsComplete() != null) && (getIsComplete().booleanValue()))
		{
			// if there are fewer sibs than allowed, add the todo image as well
			if (!overdue
					&& (getSiblingCount() != null)
					&& ((assessment.getNumSubmissionsAllowed() == null) || (getSiblingCount().intValue() < assessment.getNumSubmissionsAllowed()
							.intValue())))
			{
				return AssessmentSubmissionStatus.completeReady;
			}

			return AssessmentSubmissionStatus.complete;
		}

		// overdue, not in progress, never completed
		if (overdue)
		{
			return AssessmentSubmissionStatus.over;
		}

		return AssessmentSubmissionStatus.other;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getBest()
	{
		return this.bestSubmissionId == null ? this : this.service.idSubmission(this.bestSubmissionId);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getConfirmation()
	{
		StringBuffer rv = new StringBuffer();
		rv.append(this.id);
		rv.append('_');
		rv.append(this.assessmentId);
		rv.append('_');
		rv.append(this.userId);
		rv.append('_');
		rv.append(this.submittedDate.toString());

		return rv.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getElapsedTime()
	{
		// read the basic info if this property has not yet been set
		if ((this.submittedDateStatus == PropertyStatus.unset) || (this.startDateStatus == PropertyStatus.unset)) readMain();

		if ((submittedDate == null) || (startDate == null)) return null;

		return new Long(submittedDate.getTime() - startDate.getTime());
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEvalComment()
	{
		// read the basic info if this property has not yet been set
		if (this.evalCommentStatus == PropertyStatus.unset) readMain();

		return this.evalComment;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getEvalScore()
	{
		// read the basic info if this property has not yet been set
		if (this.evalScoreStatus == PropertyStatus.unset) readMain();

		return this.evalScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public Expiration getExpiration()
	{
		// check the thread cache
		String key = "submission_" + getId() + "_expiration";
		ExpirationImpl rv = (ExpirationImpl) this.service.m_threadLocalManager.get(key);
		if (rv != null) return rv;

		rv = new ExpirationImpl();

		// the end might be from a time limit, or because we are near the closed date
		long endTime = 0;

		// see if the assessment has a hard due date (w/ no late submissions accepted) or a retract date
		Time closedDate = getAssessment().getClosedDate();
		rv.time = closedDate;

		// if we have a time limit, compute the end time based on that limit
		Long limit = getAssessment().getTimeLimit();
		if (limit != null)
		{
			rv.limit = limit;

			// if we have started, compute the end from the start
			long startTime = 0;
			Time startDate = getStartDate();
			if (startDate != null)
			{
				startTime = startDate.getTime();
			}

			// if we have not started, compute the end from now
			else
			{
				startTime = this.service.m_timeService.newTime().getTime();
			}

			// a full time limit duration would end here
			endTime = startTime + limit.longValue();

			// if there's a closed date on the assessment, that falls before that full duration would be, that's the end time
			if ((closedDate != null) && (closedDate.getTime() < endTime))
			{
				endTime = closedDate.getTime();
				rv.cause = Expiration.Cause.closedDate;
			}

			else
			{
				rv.cause = Expiration.Cause.timeLimit;
			}
		}

		// if we are not timed, compute an end time based on the assessment's closed date
		else
		{
			// not timed, no close date, we don't expire
			if (closedDate == null) return null;

			// the closeDate is the end time
			endTime = closedDate.getTime();

			// if this closed date is more than 2 hours from now, ignore it and say we have no expiration
			if (endTime > this.service.m_timeService.newTime().getTime() + (2l * 60l * 60l * 1000l)) return null;

			// set the limit to 2 hours
			rv.limit = 2l * 60l * 60l * 1000l;

			rv.cause = Expiration.Cause.closedDate;
		}

		// how long from now till endTime?
		long tillExpires = endTime - this.service.m_timeService.newTime().getTime();
		if (tillExpires <= 0) tillExpires = 0;

		rv.duration = new Long(tillExpires);

		// thread cache
		this.service.m_threadLocalManager.set(key, rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getFirstIncompleteQuestion()
	{
		Assessment assessment = getAssessment();

		for (AssessmentSection section : assessment.getSections())
		{
			for (AssessmentQuestion question : section.getQuestions())
			{
				if (!getIsCompleteQuestion(question).booleanValue())
				{
					return question;
				}
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswered(List<AssessmentQuestion> questionsToSkip)
	{
		// read the answers info if this property has not yet been set
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		Assessment a = getAssessment();

		// for each section / question, make sure we have an answer and not a mark for review
		// only consider questions that are selected to be part of the test for this submision!
		for (AssessmentSection section : a.getSections())
		{
			for (AssessmentQuestion question : section.getQuestions())
			{
				// we may be asked to skip checking this question
				if ((questionsToSkip != null) && (questionsToSkip.contains(question))) continue;

				SubmissionAnswerImpl answer = this.findAnswer(question.getId());
				if (answer == null) return Boolean.FALSE;
				if (answer.getSubmittedDate() == null) return Boolean.FALSE;
				if ((answer.getIsAnswered() == null) || (!answer.getIsAnswered().booleanValue())) return Boolean.FALSE;
				if ((answer.getMarkedForReview() != null && (answer.getMarkedForReview().booleanValue()))) return Boolean.FALSE;
				// if ((question.getRequireRationale() != null) && (question.getRequireRationale().booleanValue())
				// && (StringUtil.trimToNull(answer.getRationale()) == null)) return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswersChanged()
	{
		if (answersStatus == PropertyStatus.unset) return Boolean.FALSE;
		for (SubmissionAnswer answer : this.answers)
		{
			if (answer.getIsChanged().booleanValue())
			{
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsComplete()
	{
		// read the basic info if this property has not yet been set
		if (this.isCompleteStatus == PropertyStatus.unset) readMain();

		return this.isComplete;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsCompleteQuestion(AssessmentQuestion question)
	{
		if (question == null) return false;

		// read the answers info if this property has not yet been set
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		SubmissionAnswerImpl answer = findAnswer(question.getId());
		if ((answer != null) && (answer.getIsComplete().booleanValue()))
		{
			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsGraded()
	{
		// base this for now of the assessment setting - if the assessment is marked to send grades to gradebook, consider the submission graded.
		// also, only if complete.
		if ((getAssessment().getGradebookIntegration() != null) && getAssessment().getGradebookIntegration().booleanValue())
		{
			if (this.getIsComplete().booleanValue()) return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsOver(Time asOf, long grace)
	{
		Time over = getWhenOver();
		if (over == null) return Boolean.FALSE;

		// set the time to now if missing
		if (asOf == null) asOf = service.m_timeService.newTime();

		return Boolean.valueOf(asOf.getTime() > over.getTime() + grace);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMayBegin()
	{
		// ASSSUMPTION: this will be a submission with sibling count, and it will be:
		// - the placeholder, if none other exist
		// - the one in progress, if there is one
		// - the "official" completed one, if any

		// not yet started
		if (getStartDate() != null) return Boolean.FALSE;

		// assessment is open
		if (!this.service.isAssessmentOpen(getAssessment(), this.service.m_timeService.newTime(), 0)) return Boolean.FALSE;

		// permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		if (!this.service.checkSecurity(this.service.m_sessionManager.getCurrentSessionUserId(), service.SUBMIT_PERMISSION, getAssessment()
				.getContext())) return Boolean.FALSE;

		// under limit
		if ((getAssessment().getNumSubmissionsAllowed() != null)
				&& (this.getSiblingCount().intValue() >= getAssessment().getNumSubmissionsAllowed().intValue())) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMayBeginAgain()
	{
		// ASSSUMPTION: this will be a submission with sibling count, and it will be:
		// - the placeholder, if none other exist
		// - the one in progress, if there is one
		// - the "official" completed one, if any

		// submission is complete
		if ((getIsComplete() == null) || (!getIsComplete().booleanValue())) return Boolean.FALSE;

		// assessment is open
		if (!this.service.isAssessmentOpen(getAssessment(), this.service.m_timeService.newTime(), 0)) return Boolean.FALSE;

		// under limit
		if ((getAssessment().getNumSubmissionsAllowed() != null)
				&& (this.getSiblingCount().intValue() >= getAssessment().getNumSubmissionsAllowed().intValue())) return Boolean.FALSE;

		// permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		if (!this.service.checkSecurity(this.service.m_sessionManager.getCurrentSessionUserId(), service.SUBMIT_PERMISSION, getAssessment()
				.getContext())) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMayContinue()
	{
		// submission has been started
		if (getStartDate() == null) return Boolean.FALSE;

		// submission not complete
		if ((getIsComplete() != null) && (getIsComplete().booleanValue())) return Boolean.FALSE;

		// same user
		if (!this.service.m_sessionManager.getCurrentSessionUserId().equals(getUserId())) return Boolean.FALSE;

		// assessment is open
		if (!this.service.isAssessmentOpen(getAssessment(), this.service.m_timeService.newTime(), 0)) return Boolean.FALSE;

		// permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		if (!this.service.checkSecurity(this.service.m_sessionManager.getCurrentSessionUserId(), service.SUBMIT_PERMISSION, getAssessment()
				.getContext())) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMayReview()
	{
		// same user
		if (!this.service.m_sessionManager.getCurrentSessionUserId().equals(getUserId())) return Boolean.FALSE;

		// submission complete
		if ((this.getIsComplete() == null) || (!getIsComplete().booleanValue())) return Boolean.FALSE;

		// not retracted
		if ((getAssessment().getRetractDate() != null) && (this.service.m_timeService.newTime().after(getAssessment().getRetractDate())))
			return Boolean.FALSE;

		// assessment feedback (i.e. review) enabled
		if (!getAssessment().getFeedbackNow().booleanValue()) return Boolean.FALSE;

		// TODO: permission?

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMayReviewLater()
	{
		// same user
		if (!this.service.m_sessionManager.getCurrentSessionUserId().equals(getUserId())) return Boolean.FALSE;

		// submission complete
		if ((this.getIsComplete() == null) || (!getIsComplete().booleanValue())) return Boolean.FALSE;

		// not retracted
		if ((getAssessment().getRetractDate() != null) && (this.service.m_timeService.newTime().after(getAssessment().getRetractDate())))
			return Boolean.FALSE;

		// assessment not set to no review
		if (getAssessment().getFeedbackDelivery() == FeedbackDelivery.NONE) return Boolean.FALSE;

		// TODO: permission?

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getReference()
	{
		return this.service.getSubmissionReference(this.id);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getSiblingCount()
	{
		return this.siblingCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getStartDate()
	{
		// read the basic info if this property has not yet been set
		if (this.startDateStatus == PropertyStatus.unset) readMain();

		return this.startDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getStatus()
	{
		// read the basic info if this property has not yet been set
		if (this.statusStatus == PropertyStatus.unset) readMain();

		return this.status;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getSubmittedDate()
	{
		// read the basic info if this property has not yet been set
		if (this.submittedDateStatus == PropertyStatus.unset) readMain();

		return this.submittedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalScore()
	{
		// Note: treat this as getGrade() - later make getGrade() and let this always return the score, graded or not -ggolden
		if (!getIsGraded().booleanValue()) return new Float(0);

		// if our special "total score" is set, use this, otherwise we compute
		if (this.totalScore != null) return this.totalScore;

		// read the basic info if this property has not yet been set
		if (this.mainStatus == PropertyStatus.unset) readMain();

		// read the answers info if this property has not yet been set
		if (this.answersStatus == PropertyStatus.unset) readAnswers();

		// add the answer auto scores, the answer evaluations, (these are combined into the answer total scores) and the overall
		// evaluation
		float total = 0;

		for (SubmissionAnswer answer : answers)
		{
			total += answer.getTotalScore();
		}

		if (this.evalScore != null)
		{
			total += this.evalScore.floatValue();
		}

		return new Float(total);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserId()
	{
		// read the basic info if this property has not yet been set
		if (this.userIdStatus == PropertyStatus.unset) readMain();

		return this.userId;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getWhenOver()
	{
		// if we have not been started, we are not over
		if (getStartDate() == null) return null;

		// if we are complete, we are not over
		if ((getIsComplete() != null) && (getIsComplete().booleanValue())) return null;

		Assessment a = getAssessment();
		Time rv = null;

		// for timed
		if ((a.getTimeLimit() != null) && (a.getTimeLimit().intValue() > 0))
		{
			// pick up the end time
			rv = service.m_timeService.newTime(getStartDate().getTime() + a.getTimeLimit().longValue());
		}

		// check the retract date
		if (a.getRetractDate() != null)
		{
			if ((rv == null) || (a.getRetractDate().before(rv)))
			{
				rv = a.getRetractDate();
			}
		}

		// for hard due date
		if ((a.getDueDate() != null) && ((a.getAllowLateSubmit() == null) || (!a.getAllowLateSubmit().booleanValue())))
		{
			if ((rv == null) || (a.getDueDate().before(rv)))
			{
				rv = a.getDueDate();
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return getId().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAnswers(List<? extends SubmissionAnswer> answers)
	{
		this.answers.clear();
		this.answersStatus = PropertyStatus.modified;
		if (answers == null) return;

		// deep copy
		for (SubmissionAnswer answer : answers)
		{
			SubmissionAnswerImpl copy = new SubmissionAnswerImpl((SubmissionAnswerImpl) answer);
			copy.initSubmission(this);
			this.answers.add(copy);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAssessment(Assessment assessment)
	{
		this.assessmentId = assessment.getId();
		this.assessmentIdStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEvalComment(String comment)
	{
		this.evalComment = comment;
		this.evalCommentStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEvalScore(Float score)
	{
		this.evalScore = score;
		this.evalScoreStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setIsComplete(Boolean complete)
	{
		this.isComplete = complete;
		this.isCompleteStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStartDate(Time startDate)
	{
		this.startDate = startDate;
		this.startDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStatus(Integer status)
	{
		this.status = status;
		this.statusStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmittedDate(Time submittedDate)
	{
		this.submittedDate = submittedDate;
		this.submittedDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUserId(String userId)
	{
		this.userId = userId;
		this.userIdStatus = PropertyStatus.modified;
	}

	/**
	 * Find an existing answer in the submission for this question id.
	 * 
	 * @param questionId
	 *        The question id.
	 * @return The existing answer in the submission for this question id, or null if not found.
	 */
	protected SubmissionAnswerImpl findAnswer(String questionId)
	{
		// find the answer to this assessment question
		for (SubmissionAnswerImpl answer : this.answers)
		{
			if (answer.questionId.equals(questionId))
			{
				return answer;
			}
		}

		return null;
	}

	/**
	 * Access the assessment id.
	 * 
	 * @return The assessment id.
	 */
	protected String getAssessmentId()
	{
		// read the basic info if this property has not yet been set
		if (this.assessmentIdStatus == PropertyStatus.unset) readMain();

		return this.assessmentId;
	}

	/**
	 * Initialize the answers property.
	 * 
	 * @param answers
	 *        The exact answers property (not deep copied).
	 */
	protected void initAnswers(List<SubmissionAnswerImpl> answers)
	{
		this.answers = answers;

		for (SubmissionAnswerImpl answer : this.answers)
		{
			answer.initSubmission(this);
		}

		this.answersStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the assessment id property.
	 * 
	 * @param id
	 *        The assessment id property.
	 */
	protected void initAssessmentId(String id)
	{
		this.assessmentId = id;
		this.assessmentIdStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the best.
	 */
	protected void initBest(Submission best)
	{
		this.bestSubmissionId = best.getId();
	}

	/**
	 * Initialize the evaluation comments
	 * 
	 * @param comment
	 *        The evaluation comment.
	 */
	protected void initEvalComment(String comment)
	{
		this.evalComment = comment;
		this.evalCommentStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the evaluation score.
	 * 
	 * @param score
	 *        The evaluation score.
	 */
	protected void initEvalScore(Float score)
	{
		this.evalScore = score;
		this.evalScoreStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the id property.
	 * 
	 * @param id
	 *        The id property.
	 */
	protected void initId(String id)
	{
		this.id = id;
		this.idStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the is complete property.
	 * 
	 * @param complete
	 *        The is complete property.
	 */
	protected void initIsComplete(Boolean complete)
	{
		this.isComplete = complete;
		this.isCompleteStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the sibling count.
	 */
	protected void initSiblingCount(Integer count)
	{
		this.siblingCount = count;
	}

	/**
	 * Initialize the start date property.
	 * 
	 * @param startDate
	 *        The start date property.
	 */
	protected void initStartDate(Time startDate)
	{
		this.startDate = startDate;
		this.startDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the status property.
	 * 
	 * @param status
	 *        The status property.
	 */
	protected void initStatus(Integer status)
	{
		this.status = status;
		this.statusStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the submitted date property.
	 * 
	 * @param submittedDate
	 *        The submitted date property.
	 */
	protected void initSubmittedDate(Time submittedDate)
	{
		this.submittedDate = submittedDate;
		this.submittedDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize a total score value, sum of all answers manual and auto scores, plus the manual score for the whole submission.<br />
	 * This is a pre-compute for the total score (trust me!), to be used if set and we don't have the answers & evaluations
	 * 
	 * @param score
	 *        The total score pre-compute.
	 */
	protected void initTotalScore(Float score)
	{
		this.totalScore = score;
	}

	/**
	 * Initialize the user id property.
	 * 
	 * @param userId
	 *        The user id property.
	 */
	protected void initUserId(String userId)
	{
		this.userId = userId;
		this.userIdStatus = PropertyStatus.inited;
	}

	/**
	 * Check if the answers info has been initialized
	 * 
	 * @return true if the answers info has been initialized, false if not.
	 */
	protected boolean isAnswersInited()
	{
		return this.answersStatus == PropertyStatus.inited;
	}

	/**
	 * Check if the main info has been initialized
	 * 
	 * @return true if the main info has been initialized, false if not.
	 */
	protected boolean isMainInited()
	{
		return this.mainStatus == PropertyStatus.inited;
	}

	/**
	 * Read the answers info into this submission.
	 */
	protected void readAnswers()
	{
		// if our id is not set, we are new, and there's nothing to read
		if (this.id == null) return;

		service.readSubmissionAnswers(this);
	}

	/**
	 * Read the main info into this submission.
	 */
	protected void readMain()
	{
		// if our id is not set, we are new, and there's nothing to read
		if (this.id == null) return;

		service.readSubmissionMain(this);
		this.mainStatus = PropertyStatus.inited;
	}

	/**
	 * Set my answers properties to exactly match the answers properties of this other
	 * 
	 * @param other
	 *        The other submission to set my answers to match.
	 */
	protected void setAnswers(SubmissionImpl other)
	{
		// deep copy the answers, preserve the status
		setAnswers(other.answers);
		this.answersStatus = other.answersStatus;
	}

	/**
	 * Set all the flags to inited.
	 */
	protected void setInited()
	{
		this.answersStatus = PropertyStatus.inited;
		this.assessmentIdStatus = PropertyStatus.inited;
		this.evalCommentStatus = PropertyStatus.inited;
		this.evalScoreStatus = PropertyStatus.inited;
		this.idStatus = PropertyStatus.inited;
		this.isCompleteStatus = PropertyStatus.inited;
		this.mainStatus = PropertyStatus.inited;
		this.startDateStatus = PropertyStatus.inited;
		this.statusStatus = PropertyStatus.inited;
		this.submittedDateStatus = PropertyStatus.inited;
		this.userIdStatus = PropertyStatus.inited;
	}

	/**
	 * Set my main properties to exactly match the main properties of this other
	 * 
	 * @param other
	 *        The other submission to set my properties to match.
	 */
	protected void setMain(SubmissionImpl other)
	{
		this.service = other.service;
		this.mainStatus = other.mainStatus;

		this.assessmentId = other.assessmentId;
		this.assessmentIdStatus = other.assessmentIdStatus;
		this.evalComment = other.evalComment;
		this.evalCommentStatus = other.evalCommentStatus;
		this.evalScore = other.evalScore;
		this.evalScoreStatus = other.evalScoreStatus;
		this.id = other.id;
		this.idStatus = other.idStatus;
		this.isComplete = other.isComplete;
		this.isCompleteStatus = other.isCompleteStatus;
		this.startDate = other.startDate;
		this.startDateStatus = other.startDateStatus;
		this.status = other.status;
		this.statusStatus = other.statusStatus;
		this.submittedDate = other.submittedDate;
		this.submittedDateStatus = other.submittedDateStatus;
		this.userId = other.userId;
		this.userIdStatus = other.userIdStatus;

		this.totalScore = other.totalScore;
	}
}
