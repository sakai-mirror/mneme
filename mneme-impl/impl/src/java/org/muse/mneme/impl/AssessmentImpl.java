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

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.AssessmentGrading;
import org.muse.mneme.api.AssessmentParts;
import org.muse.mneme.api.AssessmentReview;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;

/**
 * <p>
 * AssessmentImpl implements Assessment
 * </p>
 */
public class AssessmentImpl implements Assessment
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentImpl.class);

	protected AssessmentAccess access = new AssessmentAccessImpl();

	protected Boolean active = Boolean.FALSE;

	protected String context = null;

	protected Attribution createdBy = new AttributionImpl();

	protected AssessmentDates dates = null;

	protected AssessmentGrading grading = new AssessmentGradingImpl();

	protected String id = null;

	protected Integer numSubmissionsAllowed = null;

	protected AssessmentParts parts = null;

	protected transient PoolService poolService = null;

	protected Presentation presentation = new PresentationImpl();

	protected QuestionGrouping questionGrouping = QuestionGrouping.question;

	protected transient QuestionService questionService = null;

	protected Boolean randomAccess = Boolean.TRUE;

	protected AssessmentReview review = null;

	protected transient Submission submissionContext = null;

	protected transient SubmissionService submissionService = null;

	protected Presentation submitPresentation = new PresentationImpl();

	protected Long timeLimit = null;

	protected String title = null;

	protected AssessmentType type = AssessmentType.test;

	/**
	 * Construct
	 */
	public AssessmentImpl(PoolService poolService, QuestionService questionService, SubmissionService submissionService)
	{
		this.poolService = poolService;
		this.submissionService = submissionService;
		this.questionService = questionService;

		this.dates = new AssessmentDatesImpl();
		this.parts = new AssessmentPartsImpl(this, questionService, poolService);
		this.review = new AssessmentReviewImpl();
	}

	/**
	 * Construct as a deep copy of another
	 */
	protected AssessmentImpl(AssessmentImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two AssessmentImpls are equals if they have the same id
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		return this.id.equals(((AssessmentImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAccess getAccess()
	{
		return this.access;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getActive()
	{
		return this.active;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return this.context;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentDates getDates()
	{
		return this.dates;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentGrading getGrading()
	{
		return this.grading;
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
		if (!this.active) return Boolean.TRUE;

		if (this.dates.getAcceptUntilDate() == null) return Boolean.FALSE;

		Date now = new Date();
		if (now.after(this.dates.getAcceptUntilDate())) return Boolean.TRUE;

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsMultipleSubmissionsAllowed()
	{
		if ((getNumSubmissionsAllowed() == null) || (getNumSubmissionsAllowed() > 1))
		{
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsOpen(Boolean withGrace)
	{
		Date now = new Date();
		long grace = withGrace ? MnemeService.GRACE : 0l;

		if (!this.active) return Boolean.FALSE;

		// if we have a release date and we are not there yet
		if ((this.dates.getOpenDate() != null) && (now.before(dates.getOpenDate()))) return Boolean.FALSE;

		// if we have a retract date and we are past it, considering grace
		if ((this.dates.getAcceptUntilDate() != null) && (now.getTime() > (this.dates.getAcceptUntilDate().getTime() + grace))) return Boolean.FALSE;

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumSubmissionsAllowed()
	{
		return this.numSubmissionsAllowed;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentParts getParts()
	{
		return this.parts;
	}

	/**
	 * {@inheritDoc}
	 */
	public Presentation getPresentation()
	{
		return this.presentation;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionGrouping getQuestionGrouping()
	{
		return this.questionGrouping;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomAccess()
	{
		return this.randomAccess;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentReview getReview()
	{
		return this.review;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getScores()
	{
		return submissionService.getAssessmentScores(this);
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
	public Presentation getSubmitPresentation()
	{
		return this.submitPresentation;
	}

	public Long getTimeLimit()
	{
		return this.timeLimit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentType getType()
	{
		return this.type;
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
	public void setActive(Boolean active)
	{
		if (active == null) throw new IllegalArgumentException();
		this.active = active;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCreatedBy(String userId)
	{
		this.createdBy.setUserId(userId);
		// TODO: date?
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNumSubmissionsAllowed(Integer count)
	{
		this.numSubmissionsAllowed = count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionGrouping(QuestionGrouping value)
	{
		if (value == null) throw new IllegalArgumentException();
		this.questionGrouping = value;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAccess(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.randomAccess = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		this.timeLimit = limit;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setType(AssessmentType type)
	{
		if (type == null) throw new IllegalArgumentException();
		this.type = type;
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
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentImpl other)
	{
		this.access = new AssessmentAccessImpl((AssessmentAccessImpl) other.access);
		this.active = other.active;
		this.context = other.context;
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy);
		this.dates = new AssessmentDatesImpl((AssessmentDatesImpl) other.dates);
		this.grading = new AssessmentGradingImpl((AssessmentGradingImpl) other.grading);
		this.id = other.id;
		this.numSubmissionsAllowed = other.numSubmissionsAllowed;
		this.parts = new AssessmentPartsImpl(this, (AssessmentPartsImpl) other.parts);
		this.poolService = other.poolService;
		this.presentation = new PresentationImpl((PresentationImpl) other.presentation);
		this.questionGrouping = other.questionGrouping;
		this.questionService = other.questionService;
		this.randomAccess = other.randomAccess;
		this.review = new AssessmentReviewImpl((AssessmentReviewImpl) other.review);
		this.submissionContext = other.submissionContext;
		this.submissionService = other.submissionService;
		this.submitPresentation = new PresentationImpl((PresentationImpl) other.submitPresentation);
		this.timeLimit = other.timeLimit;
		this.title = other.title;
		this.type = other.type;
	}
}
