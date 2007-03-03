/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionExpiration;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.util.StringUtil;

/**
 * <p>
 * SubmissionImpl is ...
 * </p>
 */
public class SubmissionImpl implements Submission
{
	public class MySubmissionExpiration implements SubmissionExpiration
	{
		protected Cause cause;

		protected Long duration;

		protected Long limit;

		protected Time time;

		public Cause getCause()
		{
			return this.cause;
		}

		public Long getDuration()
		{
			return this.duration;
		}

		public Long getLimit()
		{
			return this.limit;
		}

		public Time getTime()
		{
			return this.time;
		}
	}

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

		return this.service.idAssessment(this.assessmentId);
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
	public SubmissionExpiration getExpiration()
	{
		// check the thread cache
		String key = "submission_" + getId() + "_expiration";
		MySubmissionExpiration rv = (MySubmissionExpiration) this.service.m_threadLocalManager.get(key);
		if (rv != null) return rv;

		rv = new MySubmissionExpiration();

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
				rv.cause = SubmissionExpiration.Cause.closedDate;
			}

			else
			{
				rv.cause = SubmissionExpiration.Cause.timeLimit;
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

			rv.cause = SubmissionExpiration.Cause.closedDate;
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

		// for each section / question, make sure we have an answer, and if there's rationale, make sure it's entered
		for (AssessmentSection section : a.getSections())
		{
			for (AssessmentQuestion question : section.getQuestionsAsAuthored())
			{
				// we may be asked to skip checking this question
				if ((questionsToSkip != null) && (questionsToSkip.contains(question))) continue;

				SubmissionAnswerImpl answer = this.findAnswer(question.getId());
				if (answer == null) return Boolean.FALSE;
				if (answer.getSubmittedDate() == null) return Boolean.FALSE;
				if (!answer.getIsAnswered()) return Boolean.FALSE;
				if ((answer.getMarkedForReview() != null && (answer.getMarkedForReview().booleanValue()))) return Boolean.FALSE;
				if ((question.getRequireRationale() != null) && (question.getRequireRationale().booleanValue())
						&& (StringUtil.trimToNull(answer.getRationale()) == null)) return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
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
		if ((answer != null) && (answer.getSubmittedDate() != null))
		{
			return true;
		}

		return false;
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
