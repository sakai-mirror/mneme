/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
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
import org.sakaiproject.assessment.api.AssessmentStatus;
import org.sakaiproject.assessment.api.FeedbackDelivery;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;

/**
 * <p>
 * AssessmentImpl is ...
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

	protected Boolean autoSubmit = null;

	protected PropertyStatus autoSubmitStatus = PropertyStatus.unset;

	protected String context = null;

	protected PropertyStatus contextStatus = PropertyStatus.unset;

	protected Boolean continuousNumbering = null;

	protected PropertyStatus continuousNumberingStatus = PropertyStatus.unset;

	protected String createdBy = null;

	protected PropertyStatus createdByStatus = PropertyStatus.unset;

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

	protected Boolean feedbackShowScore = null;

	protected PropertyStatus feedbackShowScoreStatus = PropertyStatus.unset;

	protected Boolean feedbackShowStatistics = null;

	protected PropertyStatus feedbackShowStatisticsStatus = PropertyStatus.unset;

	protected String id = null;

	protected PropertyStatus idStatus = PropertyStatus.unset;

	/** Tracks when we have read the entire main property set. */
	protected PropertyStatus mainStatus = PropertyStatus.unset;

	protected MultipleSubmissionSelectionPolicy mssPolicy = null;

	protected PropertyStatus mssPolicyStatus = PropertyStatus.unset;

	protected Integer numSubmissions = null;

	protected PropertyStatus numSubmissionsStatus = PropertyStatus.unset;

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

	protected Integer timeLimit = null;

	protected PropertyStatus timeLimitStatus = PropertyStatus.unset;

	protected String title = null;

	protected PropertyStatus titleStatus = PropertyStatus.unset;

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
		this.setSections(other);
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
	public Boolean getAutoSubmit()
	{
		// read the basic info if this property has not yet been set
		if (this.autoSubmitStatus == PropertyStatus.unset) readMain();

		return this.autoSubmit;
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
	public Time getDueDate()
	{
		// read the basic info if this property has not yet been set
		if (this.dueDateStatus == PropertyStatus.unset) readMain();

		return this.dueDate;
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
				|| ((delivery == FeedbackDelivery.BY_DATE) && ((feedbackDate == null) || (!(feedbackDate.after(TimeService
						.newTime()))))))
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
	public Boolean getFeedbackShowScore()
	{
		// read the basic info if this property has not yet been set
		if (this.feedbackShowScoreStatus == PropertyStatus.unset) readMain();

		return this.feedbackShowScore;
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
	public String getId()
	{
		return this.id;
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

	public Integer getTimeLimit()
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
	public void setFeedbackShowScore(Boolean value)
	{
		this.feedbackShowScore = value;
		this.feedbackShowScoreStatus = PropertyStatus.modified;
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
	public void setTimeLimit(Integer limit)
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
	 * Initialize the feedback show score property.
	 * 
	 * @param value
	 *        The feedback show score property.
	 */
	protected void initFeedbackShowScore(Boolean value)
	{
		this.feedbackShowScore = value;
		this.feedbackShowScoreStatus = PropertyStatus.inited;
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
	 * Initialize the time limit property.
	 * 
	 * @param limit
	 *        The time limit property.
	 */
	protected void initTimeLimit(Integer limit)
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
	 * Set all the flags to inited.
	 */
	protected void setInited()
	{
		this.mainStatus = PropertyStatus.inited;
		this.allowLateSubmitStatus = PropertyStatus.inited;
		this.autoSubmitStatus = PropertyStatus.inited;
		this.contextStatus = PropertyStatus.inited;
		this.continuousNumberingStatus = PropertyStatus.inited;
		this.createdByStatus = PropertyStatus.inited;
		this.dueDateStatus = PropertyStatus.inited;
		this.feedbackDateStatus = PropertyStatus.inited;
		this.feedbackDeliveryStatus = PropertyStatus.inited;
		this.feedbackShowAnswerFeedbackStatus = PropertyStatus.inited;
		this.feedbackShowCorrectAnswerStatus = PropertyStatus.inited;
		this.feedbackShowQuestionFeedbackStatus = PropertyStatus.inited;
		this.feedbackShowQuestionScoreStatus = PropertyStatus.inited;
		this.feedbackShowScoreStatus = PropertyStatus.inited;
		this.feedbackShowStatisticsStatus = PropertyStatus.inited;
		this.idStatus = PropertyStatus.inited;
		this.mssPolicyStatus = PropertyStatus.inited;
		this.numSubmissionsStatus = PropertyStatus.inited;
		this.randomAccessStatus = PropertyStatus.inited;
		this.releaseDateStatus = PropertyStatus.inited;
		this.retractDateStatus = PropertyStatus.inited;
		this.sectionsStatus = PropertyStatus.inited;
		this.statusStatus = PropertyStatus.inited;
		this.timeLimitStatus = PropertyStatus.inited;
		this.titleStatus = PropertyStatus.inited;
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

		// direct value copies - don't trigger any reads, and don't change the status of any properties
		// all except questions
		this.allowLateSubmit = other.allowLateSubmit;
		this.allowLateSubmitStatus = other.allowLateSubmitStatus;
		this.autoSubmit = other.autoSubmit;
		this.autoSubmitStatus = other.autoSubmitStatus;
		this.context = other.context;
		this.contextStatus = other.createdByStatus;
		this.continuousNumbering = other.continuousNumbering;
		this.continuousNumberingStatus = other.continuousNumberingStatus;
		this.createdBy = other.createdBy;
		this.createdByStatus = other.createdByStatus;
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
		this.feedbackShowScore = other.feedbackShowScore;
		this.feedbackShowScoreStatus = other.feedbackShowScoreStatus;
		this.feedbackShowStatistics = other.feedbackShowStatistics;
		this.feedbackShowStatisticsStatus = other.feedbackShowStatisticsStatus;
		this.id = other.id;
		this.idStatus = other.idStatus;
		this.mssPolicy = other.mssPolicy;
		this.mssPolicyStatus = other.mssPolicyStatus;
		this.numSubmissions = other.numSubmissions;
		this.numSubmissionsStatus = other.numSubmissionsStatus;
		this.randomAccess = other.randomAccess;
		this.randomAccessStatus = other.randomAccessStatus;
		this.releaseDate = other.releaseDate;
		this.releaseDateStatus = other.releaseDateStatus;
		this.retractDate = other.retractDate;
		this.retractDateStatus = other.retractDateStatus;
		this.status = other.status;
		this.statusStatus = other.statusStatus;
		this.timeLimit = other.timeLimit;
		this.timeLimitStatus = other.timeLimitStatus;
		this.title = other.title;
		this.titleStatus = other.titleStatus;
	}

	/**
	 * Set my sections to exactly match the sections of this other
	 * 
	 * @param other
	 *        The other assessment to set my parts to match.
	 */
	protected void setSections(AssessmentImpl other)
	{
		// deep copy the questions, preserve the status
		setSections(other.sections);
		this.sectionsStatus = other.sectionsStatus;
	}
}
