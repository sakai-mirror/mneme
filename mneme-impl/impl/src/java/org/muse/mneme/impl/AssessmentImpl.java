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
import org.muse.mneme.api.AcceptSubmitStatus;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.AssessmentGrading;
import org.muse.mneme.api.AssessmentParts;
import org.muse.mneme.api.AssessmentPassword;
import org.muse.mneme.api.AssessmentReview;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentSpecialAccess;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.user.api.User;

/**
 * AssessmentImpl implements Assessment
 */
public class AssessmentImpl implements Assessment
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentImpl.class);

	protected Boolean archived = Boolean.FALSE;

	protected transient AssessmentService assessmentService = null;

	/** Track any changes at all. */
	protected transient ChangeableImpl changed = new ChangeableImpl();

	protected String context = "";

	protected Attribution createdBy = null;

	protected AssessmentDates dates = null;

	protected AssessmentGrading grading = null;

	protected Boolean historical = Boolean.FALSE;

	/** Track any changes that need history. */
	protected transient ChangeableImpl historyChanged = new ChangeableImpl();

	protected Boolean honorPledge = Boolean.FALSE;

	protected String id = null;

	/** Track any changes that cannot be made to live tests. */
	protected transient Boolean liveChanged = Boolean.FALSE;

	protected Attribution modifiedBy = null;

	protected AssessmentPartsImpl parts = null;

	protected AssessmentPassword password = null;

	protected transient PoolService poolService = null;

	protected Presentation presentation = null;

	protected Boolean published = Boolean.FALSE;

	protected QuestionGrouping questionGrouping = QuestionGrouping.question;

	protected transient QuestionService questionService = null;

	protected Boolean randomAccess = Boolean.TRUE;

	protected AssessmentReview review = null;

	// protected SubmissionCounts submissionCounts = new SubmissionCountsImpl();

	protected Boolean showHints = Boolean.FALSE;

	protected AssessmentSpecialAccess specialAccess = null;

	protected transient Submission submissionContext = null;

	protected transient SubmissionService submissionService = null;

	protected Presentation submitPresentation = null;

	protected Long timeLimit = null;

	protected String title = "";

	protected Integer tries = Integer.valueOf(1);

	protected AssessmentType type = AssessmentType.test;

	/**
	 * Construct
	 */
	public AssessmentImpl(AssessmentService assessmentService, PoolService poolService, QuestionService questionService,
			SubmissionService submissionService)
	{
		this.assessmentService = assessmentService;
		this.poolService = poolService;
		this.submissionService = submissionService;
		this.questionService = questionService;

		this.createdBy = new AttributionImpl(this.changed);
		this.dates = new AssessmentDatesImpl(this, this.changed);
		this.grading = new AssessmentGradingImpl(this.changed);
		this.modifiedBy = new AttributionImpl(this.changed);
		this.parts = new AssessmentPartsImpl(this, questionService, submissionService, poolService, this.historyChanged);
		this.password = new AssessmentPasswordImpl(this.changed);
		this.presentation = new PresentationImpl(this.historyChanged);
		this.review = new AssessmentReviewImpl(this, this.changed);
		this.specialAccess = new AssessmentSpecialAccessImpl(this.changed);
		this.submitPresentation = new PresentationImpl(this.historyChanged);
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
		if ((this.id == null) || (((AssessmentImpl) obj).id == null)) return false;
		return this.id.equals(((AssessmentImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public AcceptSubmitStatus getAcceptSubmitStatus()
	{
		if (this.archived) return AcceptSubmitStatus.closed;
		if (!this.published) return AcceptSubmitStatus.closed;

		Date now = new Date();

		// before open date, we are future (not yet open)
		if ((this.dates.getOpenDate() != null) && (now.before(this.dates.getOpenDate())))
		{
			return AcceptSubmitStatus.future;
		}

		// closed if we are after a defined getSubmitUntilDate
		if ((this.dates.getSubmitUntilDate() != null) && (now.after(this.dates.getSubmitUntilDate())))
		{
			return AcceptSubmitStatus.closed;
		}

		// after due date, we are late
		if ((this.dates.getDueDate() != null) && (now.after(this.dates.getDueDate())))
		{
			return AcceptSubmitStatus.late;
		}

		// otherwise, we are open
		return AcceptSubmitStatus.open;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getArchived()
	{
		return this.archived;
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
	public Boolean getHasMultipleTries()
	{
		if ((getTries() == null) || (getTries() > 1))
		{
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasTimeLimit()
	{
		return Boolean.valueOf(this.timeLimit != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasTriesLimit()
	{
		return Boolean.valueOf(this.tries != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasUnscoredSubmissions()
	{
		return this.submissionService.getAssessmentHasUnscoredSubmissions(this);
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
	public Boolean getIsChanged()
	{
		return this.changed.getChanged() || this.historyChanged.getChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsFullyReleased()
	{
		// TODO: we may want to compute this on read -ggolden
		return this.submissionService.getAssessmentIsFullyReleased(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsLive()
	{
		// TODO: we may want to compute this on read -ggolden
		return this.submissionService.submissionsExist(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// dates valid
		if (!this.dates.getIsValid()) return Boolean.FALSE;

		// parts valid
		if (!this.parts.getIsValid()) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getModifiedBy()
	{
		return modifiedBy;
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
	public AssessmentPassword getPassword()
	{
		return this.password;
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
	public Boolean getPublished()
	{
		return this.published;
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
	public Boolean getRequireHonorPledge()
	{
		return this.honorPledge;
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
	public Boolean getShowHints()
	{
		return this.showHints;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSpecialAccess getSpecialAccess()
	{
		return this.specialAccess;
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

	/**
	 * {@inheritDoc}
	 */
	public List<User> getSubmitUsers()
	{
		return this.assessmentService.getSubmitUsers(this.getContext());
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
	public Integer getTries()
	{
		return this.tries;
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
		return getId() == null ? "null".hashCode() : getId().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setArchived(Boolean archived)
	{
		if (archived == null) throw new IllegalArgumentException();
		if (this.archived.equals(archived)) return;

		this.archived = archived;

		// if now archived, set the date, and un-publish
		if (this.archived)
		{
			((AssessmentDatesImpl) this.dates).archived = new Date();
			this.published = Boolean.FALSE;
		}

		// else clear it
		else
		{
			((AssessmentDatesImpl) this.dates).archived = null;
		}

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		if (context == null) context = "";
		if (this.context.equals(context)) return;

		this.context = context;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setHasTimeLimit(Boolean hasTimeLimit)
	{
		if (hasTimeLimit == null) throw new IllegalArgumentException();

		if ((!hasTimeLimit) && (this.timeLimit != null))
		{
			this.timeLimit = null;

			this.changed.setChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setHasTriesLimit(Boolean hasTriesLimit)
	{
		if (hasTriesLimit == null) throw new IllegalArgumentException();

		if ((!hasTriesLimit) && (this.tries != null))
		{
			this.tries = null;

			this.changed.setChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPublished(Boolean published)
	{
		if (published == null) throw new IllegalArgumentException();
		if (this.published.equals(published)) return;

		this.published = published;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionGrouping(QuestionGrouping value)
	{
		if (value == null) throw new IllegalArgumentException();
		if (this.questionGrouping.equals(value)) return;

		this.questionGrouping = value;

		this.historyChanged.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAccess(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		if (this.randomAccess.equals(setting)) return;

		this.randomAccess = setting;

		this.historyChanged.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequireHonorPledge(Boolean honorPledge)
	{
		if (honorPledge == null) throw new IllegalArgumentException();
		if (this.honorPledge.equals(honorPledge)) return;

		this.honorPledge = honorPledge;

		this.historyChanged.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowHints(Boolean showHints)
	{
		if (showHints == null) throw new IllegalArgumentException();
		if (this.showHints.equals(showHints)) return;

		this.showHints = showHints;

		this.historyChanged.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		if (!Different.different(this.timeLimit, limit)) return;

		this.timeLimit = limit;

		this.historyChanged.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		if (title == null) title = "";
		if (this.title.equals(title)) return;

		this.title = title;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTries(Integer count)
	{
		if (!Different.different(count, this.tries)) return;

		this.tries = count;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setType(AssessmentType type)
	{
		if (type == null) throw new IllegalArgumentException();
		if (this.type.equals(type)) return;

		if (this.type == AssessmentType.survey)
		{
			// this is a change that cannot be made to live tests
			this.liveChanged = Boolean.TRUE;
		}

		this.type = type;

		this.changed.setChanged();
	}

	/**
	 * Clear the changed settings.
	 */
	protected void clearChanged()
	{
		this.changed.clearChanged();
		this.historyChanged.clearChanged();
		this.liveChanged = Boolean.FALSE;
	}

	/**
	 * Check if there were any changes that need to generate history.
	 * 
	 * @return TRUE if there were history changes, FALSE if not.
	 */
	protected Boolean getHistoryChanged()
	{
		return this.historyChanged.getChanged();
	}

	/**
	 * Check if the assessment has been changed in parts that require history to be created.
	 * 
	 * @return TRUE if there's a history-making change, FALSE if not.
	 */
	protected Boolean getIsHistoryChanged()
	{
		return this.historyChanged.getChanged();
	}

	/**
	 * Check if any changes have been made that are not allowed if the test is live.
	 * 
	 * @return TRUE if any changes that are not allowed if live have been made, FALSE if not.
	 */
	protected Boolean getIsLiveChanged()
	{
		return this.liveChanged;
	}

	/**
	 * Set this assessment to be "historical" - used only for history by submissions.
	 */
	protected void initHistorical()
	{
		this.historical = Boolean.TRUE;
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
	 * Check the historical setting of the assessment.
	 * 
	 * @return TRUE if the assessment is used only for submission historical access, FALSE if it is a current assessment.
	 */
	protected Boolean isHistorical()
	{
		return this.historical;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentImpl other)
	{
		this.archived = other.archived;
		this.assessmentService = other.assessmentService;
		this.changed = new ChangeableImpl(other.changed);
		this.context = other.context;
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy, this.changed);
		this.dates = new AssessmentDatesImpl(this, (AssessmentDatesImpl) other.dates, this.changed);
		this.grading = new AssessmentGradingImpl((AssessmentGradingImpl) other.grading, this.changed);
		this.historical = other.historical;
		this.historyChanged = new ChangeableImpl(other.historyChanged);
		this.honorPledge = other.honorPledge;
		this.id = other.id;
		this.liveChanged = other.liveChanged;
		this.modifiedBy = new AttributionImpl((AttributionImpl) other.modifiedBy, this.changed);
		this.parts = new AssessmentPartsImpl(this, (AssessmentPartsImpl) other.parts, this.historyChanged);
		this.password = new AssessmentPasswordImpl((AssessmentPasswordImpl) other.password, this.changed);
		this.poolService = other.poolService;
		this.presentation = new PresentationImpl((PresentationImpl) other.presentation, this.historyChanged);
		this.published = other.published;
		this.questionGrouping = other.questionGrouping;
		this.questionService = other.questionService;
		this.randomAccess = other.randomAccess;
		this.review = new AssessmentReviewImpl(this, (AssessmentReviewImpl) other.review, this.changed);
		this.showHints = other.showHints;
		this.submissionContext = other.submissionContext;
		// this.submissionCounts = new SubmissionCountsImpl((SubmissionCountsImpl) other.submissionCounts);
		this.submissionService = other.submissionService;
		this.submitPresentation = new PresentationImpl((PresentationImpl) other.submitPresentation, this.historyChanged);
		this.specialAccess = new AssessmentSpecialAccessImpl((AssessmentSpecialAccessImpl) other.specialAccess, this.changed);
		this.timeLimit = other.timeLimit;
		this.title = other.title;
		this.tries = other.tries;
		this.type = other.type;
	}
}
