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
import org.muse.mneme.api.AssessmentStatus;
import org.muse.mneme.api.Expiration;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.Submission;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.time.api.Time;

/**
 * <p>
 * AssessmentImpl implements Assessment
 * </p>
 */
public class AssessmentImpl implements Assessment
{
	/** Each property may be not yet set, already set from persistence, or modified since. */
	enum PropertyStatus
	{
		inited, modified, unset
	}

	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentImpl.class);;

	protected Boolean allowLateSubmit = null;

	protected PropertyStatus allowLateSubmitStatus = PropertyStatus.unset;

	protected List<Reference> attachments = new ArrayList<Reference>();

	protected PropertyStatus attachmentsStatus = PropertyStatus.unset;

	protected Boolean autoSubmit = null;

	protected PropertyStatus autoSubmitStatus = PropertyStatus.unset;

	protected String context = null;

	protected PropertyStatus contextStatus = PropertyStatus.unset;

	protected Boolean continuousNumbering = null;

	protected PropertyStatus continuousNumberingStatus = PropertyStatus.unset;

	protected String createdBy = null;

	protected PropertyStatus createdByStatus = PropertyStatus.unset;

	protected String description = null;

	protected PropertyStatus descriptionStatus = PropertyStatus.unset;

	protected Time dueDate = null;

	protected PropertyStatus dueDateStatus = PropertyStatus.unset;

	protected Time feedbackDate = null;

	protected PropertyStatus feedbackDateStatus = PropertyStatus.unset;

	protected FeedbackDelivery feedbackDelivery = null;

	protected PropertyStatus feedbackDeliveryStatus = PropertyStatus.unset;

	protected Boolean feedbackShowAnswerFeedback = null;

	protected PropertyStatus feedbackShowAnswerFeedbackStatus = PropertyStatus.unset;

	protected Boolean feedbackShowCorrectAnswer = null;

	protected PropertyStatus feedbackShowCorrectAnswerStatus = PropertyStatus.unset;

	protected Boolean feedbackShowQuestionFeedback = null;

	protected PropertyStatus feedbackShowQuestionFeedbackStatus = PropertyStatus.unset;

	protected Boolean feedbackShowQuestionScore = null;

	protected PropertyStatus feedbackShowQuestionScoreStatus = PropertyStatus.unset;

	protected Boolean feedbackShowStatistics = null;

	protected PropertyStatus feedbackShowStatisticsStatus = PropertyStatus.unset;

	protected Boolean gradebookIntegeration = null;

	protected PropertyStatus gradebookIntegerationStatus = PropertyStatus.unset;

	protected String id = null;

	protected PropertyStatus idStatus = PropertyStatus.unset;

	/** Tracks when we have read the entire main property set. */
	protected PropertyStatus mainStatus = PropertyStatus.unset;

	protected MultipleSubmissionSelectionPolicy mssPolicy = null;

	protected PropertyStatus mssPolicyStatus = PropertyStatus.unset;

	protected Integer numSubmissions = null;

	protected PropertyStatus numSubmissionsStatus = PropertyStatus.unset;

	protected String password = null;

	protected PropertyStatus passwordStatus = PropertyStatus.unset;

	protected QuestionPresentation questionPresentation = null;

	protected PropertyStatus questionPresentationStatus = PropertyStatus.unset;

	protected Boolean randomAccess = null;

	protected PropertyStatus randomAccessStatus = PropertyStatus.unset;

	protected Time releaseDate = null;

	protected PropertyStatus releaseDateStatus = PropertyStatus.unset;

	protected Time retractDate = null;

	protected PropertyStatus retractDateStatus = PropertyStatus.unset;

	protected List<AssessmentSectionImpl> sections = new ArrayList<AssessmentSectionImpl>();

	protected PropertyStatus sectionsStatus = PropertyStatus.unset;

	protected AssessmentServiceImpl service = null;

	protected AssessmentStatus status = null;

	protected PropertyStatus statusStatus = PropertyStatus.unset;

	protected transient Submission submissionContext = null;

	protected String submitMessage = null;

	protected PropertyStatus submitMessageStatus = PropertyStatus.unset;

	protected String submitUrl = null;

	protected PropertyStatus submitUrlStatus = PropertyStatus.unset;

	protected Long timeLimit = null;

	protected PropertyStatus timeLimitStatus = PropertyStatus.unset;

	protected String title = null;

	protected PropertyStatus titleStatus = PropertyStatus.unset;

	/** The total points for all questions in all sections of the assessment (trust me!). Use if present. */
	protected transient Float totalPoints = null;

	/**
	 * Construct
	 */
	public AssessmentImpl(AssessmentServiceImpl service)
	{
		this.service = service;
	}

	/**
	 * Construct as a deep copy of another
	 */
	protected AssessmentImpl(AssessmentImpl other)
	{
		this.setMain(other);
		this.setAttachments(other);
		this.setSections(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean checkPassword(String password)
	{
		String myPw = getPassword();
		if (myPw == null) return Boolean.TRUE;
		if (password == null) return Boolean.FALSE;

		return Boolean.valueOf(password.equals(myPw));
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof Assessment)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare? title?
		int compare = getTitle().compareTo(((Assessment) obj).getTitle());

		return compare;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Assessment)) return false;
		if (this == obj) return true;
		if (this.getId() == null) return false;
		if (((Assessment) obj).getId() == null) return false;
		return ((Assessment) obj).getId().equals(getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAllowLateSubmit()
	{
		// read the basic info if this property has not yet been set
		if (this.allowLateSubmitStatus == PropertyStatus.unset) readMain();

		return this.allowLateSubmit;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Reference> getAttachments()
	{
		// read the attachments if this property has not yet been set
		if (this.attachmentsStatus == PropertyStatus.unset) readAttachments();

		return this.attachments;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getAutoSubmit()
	{
		// read the basic info if this property has not yet been set
		if (this.autoSubmitStatus == PropertyStatus.unset) readMain();

		return this.autoSubmit;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getClosedDate()
	{
		// consider the due date only if late submissions are not allowed
		Time dueDate = null;
		boolean allowLate = getAllowLateSubmit().booleanValue();
		if (!allowLate)
		{
			dueDate = getDueDate();
		}

		// get the retract date
		Time retractDate = getRetractDate();

		// if neiter set, there's no closed date
		if ((dueDate == null) && (retractDate == null)) return null;

		// if only one is set, use that
		if (dueDate == null) return retractDate;
		if (retractDate == null) return dueDate;

		// otherwise, use the first one
		if (dueDate.before(retractDate)) return dueDate;
		return retractDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		// read the basic info if this property has not yet been set
		if (this.contextStatus == PropertyStatus.unset) readMain();

		return this.context;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getContinuousNumbering()
	{
		// read the basic info if this property has not yet been set
		if (this.continuousNumberingStatus == PropertyStatus.unset) readMain();

		return this.continuousNumbering;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCreatedBy()
	{
		// read the basic info if this property has not yet been set
		if (this.createdByStatus == PropertyStatus.unset) readMain();

		return createdBy;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		// read the basic info if this property has not yet been set
		if (this.descriptionStatus == PropertyStatus.unset) readMain();

		return this.description;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getDueDate()
	{
		// read the basic info if this property has not yet been set
		if (this.dueDateStatus == PropertyStatus.unset) readMain();

		return this.dueDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getDurationTillDue()
	{
		Time dueDate = getDueDate();
		// Boolean allowLate = getAllowLateSubmit();

		// if no due date
		if (dueDate == null) return null;

		// if we have started, the clock is running - compute how long from NOW the end is
		long tillDue = dueDate.getTime() - this.service.m_timeService.newTime().getTime();
		if (tillDue <= 0) return new Long(0);

		return new Long(tillDue);
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getFeedbackDate()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackDateStatus == PropertyStatus.unset) readMain();

		return this.feedbackDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public FeedbackDelivery getFeedbackDelivery()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackDeliveryStatus == PropertyStatus.unset) readMain();

		return this.feedbackDelivery;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackNow()
	{
		FeedbackDelivery delivery = getFeedbackDelivery();
		Time feedbackDate = getFeedbackDate();
		if ((delivery == FeedbackDelivery.IMMEDIATE)
				|| ((delivery == FeedbackDelivery.BY_DATE) && ((feedbackDate == null) || (!(feedbackDate.after(this.service.m_timeService.newTime()))))))
		{
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackShowAnswerFeedback()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowAnswerFeedbackStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowAnswerFeedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackShowCorrectAnswer()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowCorrectAnswerStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowCorrectAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackShowQuestionFeedback()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowQuestionFeedbackStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowQuestionFeedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackShowQuestionScore()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowQuestionScoreStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowQuestionScore;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getFeedbackShowStatistics()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowStatisticsStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowStatistics;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSection getFirstSection()
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		if (this.sections.isEmpty()) return null;

		return this.sections.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getGradebookIntegration()
	{
		// read the basic info if this property has not yet been set
		if (this.gradebookIntegerationStatus == PropertyStatus.unset) readMain();

		return this.gradebookIntegeration;
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
	public Boolean getIsClosed()
	{
		Time closedDate = getClosedDate();
		if (closedDate == null) return Boolean.FALSE;

		if (service.m_timeService.newTime().after(closedDate)) return Boolean.TRUE;

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public MultipleSubmissionSelectionPolicy getMultipleSubmissionSelectionPolicy()
	{
		// read the basic info if this property has not yet been set
		if (this.mssPolicyStatus == PropertyStatus.unset) readMain();

		return mssPolicy;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		int count = 0;
		for (AssessmentSection section : this.sections)
		{
			count += section.getNumQuestions();
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumSections()
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		return this.sections.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumSubmissionsAllowed()
	{
		// read the basic info if this property has not yet been set
		if (this.numSubmissionsStatus == PropertyStatus.unset) readMain();

		return this.numSubmissions;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsMultipleSubmissionsAllowed()
	{
		if ((getNumSubmissionsAllowed() == null) || (getNumSubmissionsAllowed().intValue() > 1))
		{
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPassword()
	{
		// read the basic info if this property has not yet been set
		if (this.passwordStatus == PropertyStatus.unset) readMain();

		return this.password;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getQuestion(String questionId)
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		// check with each section
		for (AssessmentSection section : this.sections)
		{
			AssessmentQuestion question = section.getQuestion(questionId);
			if (question != null) return question;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPresentation getQuestionPresentation()
	{
		// read the basic info if this property has not yet been set
		if (this.questionPresentationStatus == PropertyStatus.unset) readMain();

		return this.questionPresentation;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentQuestion> getQuestions()
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		List<AssessmentQuestion> rv = new ArrayList<AssessmentQuestion>();

		for (AssessmentSection s : this.sections)
		{
			rv.addAll(s.getQuestions());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomAccess()
	{
		// read the basic info if this property has not yet been set
		if (this.randomAccessStatus == PropertyStatus.unset) readMain();

		return this.randomAccess;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getReleaseDate()
	{
		// read the basic info if this property has not yet been set
		if (this.releaseDateStatus == PropertyStatus.unset) readMain();

		return this.releaseDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getRetractDate()
	{
		// read the basic info if this property has not yet been set
		if (this.retractDateStatus == PropertyStatus.unset) readMain();

		return this.retractDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getScores()
	{
		return service.m_submissionService.getAssessmentScores(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSection getSection(String sectionId)
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		for (AssessmentSection section : this.sections)
		{
			if (section.getId().equals(sectionId)) return section;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentSection> getSections()
	{
		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		return this.sections;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentStatus getStatus()
	{
		// read the basic info if this property has not yet been set
		if (this.statusStatus == PropertyStatus.unset) readMain();

		return this.status;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmissionContext()
	{
		return this.submissionContext;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSubmitMessage()
	{
		// read the basic info if this property has not yet been set
		if (this.submitMessageStatus == PropertyStatus.unset) readMain();

		return this.submitMessage;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSubmitUrl()
	{
		// read the basic info if this property has not yet been set
		if (this.submitUrlStatus == PropertyStatus.unset) readMain();

		return this.submitUrl;
	}

	public Long getTimeLimit()
	{
		// read the basic info if this property has not yet been set
		if (this.timeLimitStatus == PropertyStatus.unset) readMain();

		return this.timeLimit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		// read the basic info if this property has not yet been set
		if (this.titleStatus == PropertyStatus.unset) readMain();

		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalPoints()
	{
		if (totalPoints != null) return totalPoints;

		// read the section info if this property has not yet been set
		if (this.sectionsStatus == PropertyStatus.unset) readSections();

		float total = 0;

		// check with each section
		for (AssessmentSection section : this.sections)
		{
			total += section.getTotalPoints();
		}

		return new Float(total);
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
	public void setAllowLateSubmit(Boolean value)
	{
		this.allowLateSubmit = value;
		this.allowLateSubmitStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAttachments(List<Reference> attachments)
	{
		this.attachments.clear();
		this.attachmentsStatus = PropertyStatus.modified;
		if (attachments == null) return;

		// deep copy
		this.attachments = new ArrayList<Reference>(attachments.size());
		for (Reference ref : attachments)
		{
			Reference copy = EntityManager.newReference(ref);
			this.attachments.add(copy);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAutoSubmit(Boolean value)
	{
		this.autoSubmit = value;
		this.autoSubmitStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		this.context = context;
		this.contextStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContinuousNumbering(Boolean setting)
	{
		this.continuousNumbering = setting;
		this.continuousNumberingStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCreatedBy(String userId)
	{
		createdBy = userId;
		this.createdByStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDescription(String description)
	{
		this.description = description;
		this.descriptionStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDueDate(Time dueDate)
	{
		this.dueDate = dueDate;
		this.dueDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackDate(Time feedbackDate)
	{
		this.feedbackDate = feedbackDate;
		this.feedbackDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackDelivery(FeedbackDelivery delivery)
	{
		this.feedbackDelivery = delivery;
		this.feedbackDeliveryStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackShowAnswerFeedback(Boolean value)
	{
		this.feedbackShowAnswerFeedback = value;
		this.feedbackShowAnswerFeedbackStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackShowCorrectAnswer(Boolean value)
	{
		this.feedbackShowCorrectAnswer = value;
		this.feedbackShowCorrectAnswerStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackShowQuestionFeedback(Boolean value)
	{
		this.feedbackShowQuestionFeedback = value;
		this.feedbackShowQuestionFeedbackStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackShowQuestionScore(Boolean value)
	{
		this.feedbackShowQuestionScore = value;
		this.feedbackShowQuestionScoreStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackShowStatistics(Boolean value)
	{
		this.feedbackShowStatistics = value;
		this.feedbackShowStatisticsStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setGradebookIntegration(Boolean value)
	{
		this.gradebookIntegeration = value;
		this.gradebookIntegerationStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy policy)
	{
		this.mssPolicy = policy;
		this.mssPolicyStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNumSubmissionsAllowed(Integer count)
	{
		this.numSubmissions = count;
		this.numSubmissionsStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPassword(String password)
	{
		this.password = password;
		this.passwordStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionPresentation(QuestionPresentation value)
	{
		this.questionPresentation = value;
		this.questionPresentationStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAccess(Boolean setting)
	{
		this.randomAccess = setting;
		this.randomAccessStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setReleaseDate(Time date)
	{
		this.releaseDate = date;
		this.releaseDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRetractDate(Time date)
	{
		this.retractDate = date;
		this.retractDateStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSections(List<? extends AssessmentSection> sections)
	{
		this.sections.clear();
		this.sectionsStatus = PropertyStatus.modified;
		if (sections == null) return;

		// deep copy
		this.sections = new ArrayList<AssessmentSectionImpl>(sections.size());
		for (AssessmentSection section : sections)
		{
			AssessmentSectionImpl copy = new AssessmentSectionImpl((AssessmentSectionImpl) section);
			copy.initAssement(this);

			this.sections.add(copy);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStatus(AssessmentStatus status)
	{
		this.status = status;
		this.statusStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmitMessage(String message)
	{
		this.submitMessage = message;
		this.submitMessageStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmitUrl(String url)

	{
		this.submitUrl = url;
		this.submitUrlStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		this.timeLimit = limit;
		this.timeLimitStatus = PropertyStatus.modified;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
		this.titleStatus = PropertyStatus.modified;
	}

	/**
	 * Initialize the allow late submit property.
	 * 
	 * @param value
	 *        The allow late submit property.
	 */
	protected void initAllowLateSubmit(Boolean value)
	{
		this.allowLateSubmit = value;
		this.allowLateSubmitStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the attachments.
	 * 
	 * @param attachments
	 *        The attachments - these are taken exactly, not deep copied.
	 */
	protected void initAttachments(List<Reference> attachments)
	{
		this.attachments = attachments;
		this.attachmentsStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the auto submit property.
	 * 
	 * @param value
	 *        The auto submit property.
	 */
	protected void initAutoSubmit(Boolean value)
	{
		this.autoSubmit = value;
		this.autoSubmitStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the context property.
	 * 
	 * @param context
	 *        The context property.
	 */
	protected void initContext(String context)
	{
		this.context = context;
		this.contextStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the continuous numbering property.
	 * 
	 * @param value
	 *        The continuous numbering property.
	 */
	protected void initContinuousNumbering(Boolean value)
	{
		this.continuousNumbering = value;
		this.continuousNumberingStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the created by property.
	 * 
	 * @param userId
	 *        the created by property.
	 */
	protected void initCreatedBy(String userId)
	{
		createdBy = userId;
		this.createdByStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the description property.
	 * 
	 * @param description
	 *        The description property.
	 */
	protected void initDescription(String description)
	{
		this.description = description;
		this.descriptionStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the due date property.
	 * 
	 * @param dueDate
	 *        The due date property.
	 */
	protected void initDueDate(Time dueDate)
	{
		this.dueDate = dueDate;
		this.dueDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback date property.
	 * 
	 * @param feedbackDate
	 *        The feedback date property.
	 */
	protected void initFeedbackDate(Time feedbackDate)
	{
		this.feedbackDate = feedbackDate;
		this.feedbackDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback delivery property.
	 * 
	 * @param delivery
	 *        The feedback delivery property.
	 */
	protected void initFeedbackDelivery(FeedbackDelivery delivery)
	{
		this.feedbackDelivery = delivery;
		this.feedbackDeliveryStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback show answer feedback property.
	 * 
	 * @param value
	 *        The feedback show answer feedback property.
	 */
	protected void initFeedbackShowAnswerFeedback(Boolean value)
	{
		this.feedbackShowAnswerFeedback = value;
		this.feedbackShowAnswerFeedbackStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback show correct answer property.
	 * 
	 * @param value
	 *        The feedback show correct answer property.
	 */
	protected void initFeedbackShowCorrectAnswer(Boolean value)
	{
		this.feedbackShowCorrectAnswer = value;
		this.feedbackShowCorrectAnswerStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback show question feedback property.
	 * 
	 * @param value
	 *        The feedback show question feedback property.
	 */
	protected void initFeedbackShowQuestionFeedback(Boolean value)
	{
		this.feedbackShowQuestionFeedback = value;
		this.feedbackShowQuestionFeedbackStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback show question score property.
	 * 
	 * @param value
	 *        The feedback show question score property.
	 */
	protected void initFeedbackShowQuestionScore(Boolean value)
	{
		this.feedbackShowQuestionScore = value;
		this.feedbackShowQuestionScoreStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the feedback show statistics property.
	 * 
	 * @param value
	 *        The feedback show statistics property.
	 */
	protected void initFeedbackShowStatistics(Boolean value)
	{
		this.feedbackShowStatistics = value;
		this.feedbackShowStatisticsStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the gradebook integration property.
	 * 
	 * @param value
	 *        The gradebook integration value.
	 */
	protected void initGradebookIntegration(Boolean value)
	{
		this.gradebookIntegeration = value;
		this.gradebookIntegerationStatus = PropertyStatus.inited;
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
	 * Initialize the multiple submission selection policy property.
	 * 
	 * @param policy
	 *        The multiple submission selection policy property.
	 */
	protected void initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy policy)
	{
		this.mssPolicy = policy;
		this.mssPolicyStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the num submissions allowed property.
	 * 
	 * @param count
	 *        The num submissions allowed property.
	 */
	protected void initNumSubmissionsAllowed(Integer count)
	{
		this.numSubmissions = count;
		this.numSubmissionsStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the password property.
	 * 
	 * @param password
	 *        The password property.
	 */
	protected void initPassword(String password)
	{
		this.password = password;
		this.passwordStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the question presentation setting.
	 * 
	 * @param value
	 *        The question presentation setting.
	 */
	protected void initQuestionPresentation(QuestionPresentation value)
	{
		this.questionPresentation = value;
		this.questionPresentationStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the random access property.
	 * 
	 * @param value
	 *        The random access property.
	 */
	protected void initRandomAccess(Boolean value)
	{
		this.randomAccess = value;
		this.randomAccessStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the release date property.
	 * 
	 * @param date
	 *        The release date property.
	 */
	protected void initReleaseDate(Time date)
	{
		this.releaseDate = date;
		this.releaseDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the retract date property.
	 * 
	 * @param date
	 *        The retract date property.
	 */
	protected void initRetractDate(Time date)
	{
		this.retractDate = date;
		this.retractDateStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the sections.
	 * 
	 * @param sections
	 *        The sections - these are taken exactly, not deep copied.
	 */
	protected void initSections(List<AssessmentSectionImpl> sections)
	{
		this.sections = sections;

		// set the back link
		for (AssessmentSectionImpl section : this.sections)
		{
			section.initAssement(this);
		}

		this.sectionsStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the status property.
	 * 
	 * @param status
	 *        The status property.
	 */
	protected void initStatus(AssessmentStatus status)
	{
		this.status = status;
		this.statusStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the submission context.
	 * 
	 * @param submission
	 *        The submission context.
	 */
	protected void initSubmissionContext(Submission submission)
	{
		this.submissionContext = submission;
	}

	/**
	 * Initialize the submitMessage property.
	 * 
	 * @param message
	 *        The submitMessage property.
	 */
	protected void initSubmitMessage(String message)
	{
		this.submitMessage = message;
		this.submitMessageStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the submitUrl property.
	 * 
	 * @param url
	 *        The submitUrl property.
	 */
	protected void initSubmitUrl(String url)
	{
		this.submitUrl = url;
		this.submitUrlStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the time limit property.
	 * 
	 * @param limit
	 *        The time limit property.
	 */
	protected void initTimeLimit(Long limit)
	{
		this.timeLimit = limit;
		this.timeLimitStatus = PropertyStatus.inited;
	}

	/**
	 * Initialize the title property.
	 * 
	 * @param title
	 *        The title property.
	 */
	protected void initTitle(String title)
	{
		this.title = title;
		this.titleStatus = PropertyStatus.inited;
	}

	/**
	 * Init the total points (trust me!) property.
	 * 
	 * @param points
	 *        The total points for the assessment.
	 */
	protected void initTotalPoints(Float points)
	{
		this.totalPoints = points;
	}

	/**
	 * Check if the attachments info has been initialized
	 * 
	 * @return true if the attachments info has been initialized, false if not.
	 */
	protected boolean isAttachmentsInited()
	{
		return this.attachmentsStatus == PropertyStatus.inited;
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
	 * Check if the sections info has been initialized
	 * 
	 * @return true if the sections info has been initialized, false if not.
	 */
	protected boolean isSectionsInited()
	{
		return this.sectionsStatus == PropertyStatus.inited;
	}

	/**
	 * Read the attachments info into this assessment.
	 */
	protected void readAttachments()
	{
		// if our id is not set, we are new, and there's nothing to read
		if (this.id == null) return;

		service.readAssessmentAttachments(this);
	}

	/**
	 * Read the main info into this assessment.
	 */
	protected void readMain()
	{
		// if our id is not set, we are new, and there's nothing to read
		if (this.id == null) return;

		service.readAssessmentMain(this);
		this.mainStatus = PropertyStatus.inited;
	}

	/**
	 * Read the sections info into this assessment.
	 */
	protected void readSections()
	{
		// if our id is not set, we are new, and there's nothing to read
		if (this.id == null) return;

		service.readAssessmentSections(this);
	}

	/**
	 * Set my attachments to exactly match the attachments of this other
	 * 
	 * @param other
	 *        The other assessment to set my parts to match.
	 */
	protected void setAttachments(AssessmentImpl other)
	{
		// deep copy the attachments, preserve the status
		setAttachments(other.attachments);
		this.attachmentsStatus = other.attachmentsStatus;
	}

	/**
	 * Set all the flags to inited.
	 */
	protected void setInited()
	{
		this.mainStatus = PropertyStatus.inited;
		this.allowLateSubmitStatus = PropertyStatus.inited;
		this.attachmentsStatus = PropertyStatus.inited;
		this.autoSubmitStatus = PropertyStatus.inited;
		this.contextStatus = PropertyStatus.inited;
		this.continuousNumberingStatus = PropertyStatus.inited;
		this.createdByStatus = PropertyStatus.inited;
		this.descriptionStatus = PropertyStatus.inited;
		this.dueDateStatus = PropertyStatus.inited;
		this.feedbackDateStatus = PropertyStatus.inited;
		this.feedbackDeliveryStatus = PropertyStatus.inited;
		this.feedbackShowAnswerFeedbackStatus = PropertyStatus.inited;
		this.feedbackShowCorrectAnswerStatus = PropertyStatus.inited;
		this.feedbackShowQuestionFeedbackStatus = PropertyStatus.inited;
		this.feedbackShowQuestionScoreStatus = PropertyStatus.inited;
		this.feedbackShowStatisticsStatus = PropertyStatus.inited;
		this.gradebookIntegerationStatus = PropertyStatus.inited;
		this.idStatus = PropertyStatus.inited;
		this.mssPolicyStatus = PropertyStatus.inited;
		this.numSubmissionsStatus = PropertyStatus.inited;
		this.passwordStatus = PropertyStatus.inited;
		this.randomAccessStatus = PropertyStatus.inited;
		this.releaseDateStatus = PropertyStatus.inited;
		this.retractDateStatus = PropertyStatus.inited;
		this.sectionsStatus = PropertyStatus.inited;
		this.statusStatus = PropertyStatus.inited;
		this.submitMessageStatus = PropertyStatus.inited;
		this.submitUrlStatus = PropertyStatus.inited;
		this.timeLimitStatus = PropertyStatus.inited;
		this.titleStatus = PropertyStatus.inited;
		this.questionPresentationStatus = PropertyStatus.inited;
	}

	/**
	 * Set my main properties to exactly match the main properties of this other
	 * 
	 * @param other
	 *        The other assessment to set my properties to match.
	 */
	protected void setMain(AssessmentImpl other)
	{
		this.service = other.service;
		this.mainStatus = other.mainStatus;
		this.totalPoints = other.totalPoints;

		// direct value copies - don't trigger any reads, and don't change the status of any properties
		// all except questions
		this.allowLateSubmit = other.allowLateSubmit;
		this.allowLateSubmitStatus = other.allowLateSubmitStatus;
		this.autoSubmit = other.autoSubmit;
		this.autoSubmitStatus = other.autoSubmitStatus;
		this.context = other.context;
		this.contextStatus = other.contextStatus;
		this.continuousNumbering = other.continuousNumbering;
		this.continuousNumberingStatus = other.continuousNumberingStatus;
		this.createdBy = other.createdBy;
		this.createdByStatus = other.createdByStatus;
		this.description = other.description;
		this.descriptionStatus = other.descriptionStatus;
		this.dueDate = other.dueDate;
		this.dueDateStatus = other.dueDateStatus;
		this.feedbackDate = other.feedbackDate;
		this.feedbackDateStatus = other.feedbackDateStatus;
		this.feedbackDelivery = other.feedbackDelivery;
		this.feedbackDeliveryStatus = other.feedbackDeliveryStatus;
		this.feedbackShowAnswerFeedback = other.feedbackShowAnswerFeedback;
		this.feedbackShowAnswerFeedbackStatus = other.feedbackShowAnswerFeedbackStatus;
		this.feedbackShowCorrectAnswer = other.feedbackShowCorrectAnswer;
		this.feedbackShowCorrectAnswerStatus = other.feedbackShowCorrectAnswerStatus;
		this.feedbackShowQuestionFeedback = other.feedbackShowQuestionFeedback;
		this.feedbackShowQuestionFeedbackStatus = other.feedbackShowQuestionFeedbackStatus;
		this.feedbackShowQuestionScore = other.feedbackShowQuestionScore;
		this.feedbackShowQuestionScoreStatus = other.feedbackShowQuestionScoreStatus;
		this.feedbackShowStatistics = other.feedbackShowStatistics;
		this.feedbackShowStatisticsStatus = other.feedbackShowStatisticsStatus;
		this.gradebookIntegeration = other.gradebookIntegeration;
		this.gradebookIntegerationStatus = other.gradebookIntegerationStatus;
		this.id = other.id;
		this.idStatus = other.idStatus;
		this.mssPolicy = other.mssPolicy;
		this.mssPolicyStatus = other.mssPolicyStatus;
		this.numSubmissions = other.numSubmissions;
		this.numSubmissionsStatus = other.numSubmissionsStatus;
		this.password = other.password;
		this.passwordStatus = other.passwordStatus;
		this.randomAccess = other.randomAccess;
		this.randomAccessStatus = other.randomAccessStatus;
		this.releaseDate = other.releaseDate;
		this.releaseDateStatus = other.releaseDateStatus;
		this.retractDate = other.retractDate;
		this.retractDateStatus = other.retractDateStatus;
		this.status = other.status;
		this.statusStatus = other.statusStatus;
		this.submitMessage = other.submitMessage;
		this.submitMessageStatus = other.submitMessageStatus;
		this.submitUrl = other.submitUrl;
		this.submitUrlStatus = other.submitUrlStatus;
		this.timeLimit = other.timeLimit;
		this.timeLimitStatus = other.timeLimitStatus;
		this.title = other.title;
		this.titleStatus = other.titleStatus;
		this.questionPresentation = other.questionPresentation;
		this.questionPresentationStatus = other.questionPresentationStatus;
	}

	/**
	 * Set my sections to exactly match the sections of this other
	 * 
	 * @param other
	 *        The other assessment to set my parts to match.
	 */
	protected void setSections(AssessmentImpl other)
	{
		// deep copy the sections, preserve the status
		setSections(other.sections);
		this.sectionsStatus = other.sectionsStatus;
	}

	/**
	 * {@inheritDoc}
	 */
	public Expiration getExpiration()
	{
		// check the thread cache
		String key = "assessment_" + getId() + "_expiration";
		ExpirationImpl rv = (ExpirationImpl) this.service.m_threadLocalManager.get(key);
		if (rv != null) return rv;

		rv = new ExpirationImpl();

		// see if the assessment has a hard due date (w/ no late submissions accepted) or a retract date
		Time closedDate = getClosedDate();

		// compute an end time based on the assessment's closed date
		if (closedDate == null) return null;

		rv.time = closedDate;

		// the closeDate is the end time
		long endTime = closedDate.getTime();

		// if this closed date is more than 2 hours from now, ignore it and say we have no expiration
		if (endTime > this.service.m_timeService.newTime().getTime() + (2l * 60l * 60l * 1000l)) return null;

		// set the limit to 2 hours
		rv.limit = 2l * 60l * 60l * 1000l;

		rv.cause = Expiration.Cause.closedDate;

		// how long from now till endTime?
		long tillExpires = endTime - this.service.m_timeService.newTime().getTime();
		if (tillExpires <= 0) tillExpires = 0;

		rv.duration = new Long(tillExpires);

		// thread cache
		this.service.m_threadLocalManager.set(key, rv);

		return rv;
	}
}
