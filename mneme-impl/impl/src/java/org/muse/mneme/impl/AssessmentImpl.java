/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyO of the License at
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.user.api.User;

/**
 * AssessmentImpl implements Assessment
 */
public class AssessmentImpl implements Assessment
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentImpl.class);

	protected Boolean archived = Boolean.FALSE;

	/** Track the original archived setting. */
	protected transient Boolean archivedWas = Boolean.FALSE;

	protected transient AssessmentService assessmentService = null;

	/** Track any changes at all. */
	protected transient ChangeableImpl changed = new ChangeableImpl();

	protected String context = "";

	protected Attribution createdBy = null;

	protected AssessmentDates dates = null;

	protected AssessmentGrading grading = null;

	protected Boolean honorPledge = Boolean.FALSE;

	protected String id = null;

	/** The live setting. */
	protected Boolean live = Boolean.FALSE;

	/** The locked setting. */
	protected Boolean locked = Boolean.FALSE;

	/** Track any changes that cannot be made to locked tests. */
	protected transient Boolean lockedChanged = Boolean.FALSE;

	protected transient InternationalizedMessages messages = null;

	/** Stays TRUE until an end-user change to the object occurs, showing it was actually initially set. */
	protected Boolean mint = Boolean.TRUE;

	protected Attribution modifiedBy = null;

	protected AssessmentPartsImpl parts = null;

	protected AssessmentPassword password = null;

	protected transient PoolServiceImpl poolService = null;

	protected Presentation presentation = null;

	protected Boolean published = Boolean.FALSE;

	/** Track the original published setting. */
	protected transient Boolean publishedWas = Boolean.FALSE;

	protected QuestionGrouping questionGrouping = QuestionGrouping.question;

	protected transient QuestionService questionService = null;

	protected Boolean randomAccess = Boolean.TRUE;

	protected AssessmentReview review = null;

	protected Boolean showHints = Boolean.FALSE;

	protected AssessmentSpecialAccess specialAccess = null;

	protected transient Submission submissionContext = null;

	protected transient SubmissionService submissionService = null;

	protected Presentation submitPresentation = null;

	protected Long timeLimit = null;

	protected String title = "";

	/** Track the original title value. */
	protected transient String titleWas = "";

	protected Integer tries = Integer.valueOf(1);

	protected AssessmentType type = AssessmentType.test;

	/**
	 * Construct
	 */
	public AssessmentImpl(AssessmentService assessmentService, PoolService poolService, QuestionService questionService,
			SubmissionService submissionService, InternationalizedMessages messages)
	{
		this.assessmentService = assessmentService;
		this.poolService = (PoolServiceImpl) poolService;
		this.submissionService = submissionService;
		this.questionService = questionService;
		this.messages = messages;

		this.createdBy = new AttributionImpl(this.changed);
		this.dates = new AssessmentDatesImpl(this, this.changed);
		this.grading = new AssessmentGradingImpl(this.changed);
		this.modifiedBy = new AttributionImpl(this.changed);
		this.parts = new AssessmentPartsImpl(this, questionService, submissionService, poolService, this.changed, this.messages);
		this.password = new AssessmentPasswordImpl(this.changed);
		this.presentation = new PresentationImpl(this.changed);
		this.review = new AssessmentReviewImpl(this, this.changed);
		this.specialAccess = new AssessmentSpecialAccessImpl(this, this.changed);
		this.submitPresentation = new PresentationImpl(this.changed);
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
		// two Assessments are equals if they have the same id
		if (this == obj) return true;
		if (obj == null) return false;
		if (getId() == null) return false;
		if (!(obj instanceof Assessment)) return false;
		Assessment a = (Assessment) obj;
		if (a.getId() == null) return false;
		return this.getId().equals(a.getId());
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
	public Boolean getAnonymous()
	{
		// surveys are always anon.
		if (this.type == AssessmentType.survey) return Boolean.TRUE;

		// otherwise use setting
		return getGrading().getAnonymous();
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
	public Boolean getHasPoints()
	{
		return Boolean.valueOf(this.type != AssessmentType.survey);
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
		return this.changed.getChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsLive()
	{
		return this.live;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsLocked()
	{
		return this.locked;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// must have a title
		if (getTitle().length() == 0) return Boolean.FALSE;

		// dates valid
		if (!this.dates.getIsValid()) return Boolean.FALSE;

		// parts valid
		if (!this.parts.getIsValid()) return Boolean.FALSE;

		// grading valid
		if (!this.grading.getIsValid()) return Boolean.FALSE;

		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMint()
	{
		return this.mint;
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
		if (value == null) return;
		if (this.questionGrouping.equals(value)) return;

		this.questionGrouping = value;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAccess(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		if (this.randomAccess.equals(setting)) return;

		this.randomAccess = setting;

		// strict order needs by question
		if (!this.randomAccess)
		{
			setQuestionGrouping(QuestionGrouping.question);
		}

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequireHonorPledge(Boolean honorPledge)
	{
		if (honorPledge == null) throw new IllegalArgumentException();
		if (this.honorPledge.equals(honorPledge)) return;

		this.honorPledge = honorPledge;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowHints(Boolean showHints)
	{
		if (showHints == null) throw new IllegalArgumentException();
		if (this.showHints.equals(showHints)) return;

		this.showHints = showHints;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		// minimum of one minute
		if ((limit != null) && (limit.longValue() < 60000l)) limit = new Long(60000l);

		if (!Different.different(this.timeLimit, limit)) return;

		this.timeLimit = limit;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		// massage the title
		if (title != null)
		{
			title = title.trim();
			if (title.length() > 255) title = title.substring(0, 255);
		}
		else
		{
			title = "";
		}

		if (this.title.equals(title)) return;

		this.title = title;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTries(Integer count)
	{
		// if < 1, set to 1
		if ((count != null) && (count.intValue() < 1))
		{
			count = Integer.valueOf(1);
		}

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
			this.lockedChanged = Boolean.TRUE;
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
		this.lockedChanged = Boolean.FALSE;
	}

	/**
	 * Clear the mint setting.
	 */
	protected void clearMint()
	{
		this.mint = Boolean.FALSE;
	}

	/**
	 * Check if the archived was changed.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	protected Boolean getArchivedChanged()
	{
		Boolean rv = Boolean.valueOf(!this.archived.equals(this.archivedWas));
		return rv;
	}

	/**
	 * Check if any changes have been made that are not allowed if the test is locked.
	 * 
	 * @return TRUE if any changes that are not allowed if locked have been made, FALSE if not.
	 */
	protected Boolean getIsLockedChanged()
	{
		return this.lockedChanged;
	}

	/**
	 * Check if the published setting was changed.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	protected Boolean getPublishedChanged()
	{
		Boolean rv = Boolean.valueOf(!this.published.equals(this.publishedWas));
		return rv;
	}

	/**
	 * Check if the title was changed.
	 * 
	 * @return TRUE if changed, FALSE if not.
	 */
	protected Boolean getTitleChanged()
	{
		Boolean rv = Boolean.valueOf(!this.title.equals(this.titleWas));
		return rv;
	}

	/**
	 * Establish the archived setting.
	 * 
	 * @param archived
	 *        The archived setting.
	 */
	protected void initArchived(Boolean archived)
	{
		this.archived = archived;
		this.archivedWas = archived;
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
	 * Establish the live setting.
	 * 
	 * @param live
	 *        The live setting.
	 */
	protected void initLive(Boolean live)
	{
		this.live = live;
	}

	/**
	 * Establish the locked setting.
	 * 
	 * @param locked
	 *        The locked setting.
	 */
	protected void initLocked(Boolean locked)
	{
		this.locked = locked;
	}

	/**
	 * Establish the mint setting.
	 * 
	 * @param mint
	 *        The mint setting.
	 */
	protected void initMint(Boolean mint)
	{
		this.mint = mint;
	}

	/**
	 * Establish the published setting.
	 * 
	 * @param published
	 *        The published setting.
	 */
	protected void initPublished(Boolean published)
	{
		this.published = published;
		this.publishedWas = published;
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
	 * Establish the title.
	 * 
	 * @param title
	 *        The title;
	 */
	protected void initTitle(String title)
	{
		if (title == null) title = "";
		this.title = title;
		this.titleWas = title;
	}

	/**
	 * Lock the assessment, locking down the dependencies (pools and questions).
	 */
	protected void lock()
	{
		if (this.locked) return;
		initLocked(Boolean.TRUE);

		Map<String, Pool> histories = new HashMap<String, Pool>();
		Map<String, Map<String, String>> oldToNews = new HashMap<String, Map<String, String>>();

		// make a history copy of all used pools and questions
		// switch over the parts
		// make sure questions from the same pool end up in the same pool

		for (Part part : this.parts.parts)
		{
			((PartImpl) part).changed = true;

			if (part instanceof DrawPart)
			{
				for (PoolDraw draw : ((DrawPartImpl) part).pools)
				{
					// if we have not yet made a history for this pool, do so
					Pool history = histories.get(draw.getPoolId());
					if (history == null)
					{
						Map<String, String> oldToNew = new HashMap<String, String>();
						history = this.poolService.makePoolHistory(draw.getPool(), oldToNew);
						histories.put(draw.getPoolId(), history);
						oldToNews.put(draw.getPoolId(), oldToNew);
					}
					draw.setPool(history);
				}
			}
			else if (part instanceof ManualPart)
			{
				for (PoolPick pick : ((ManualPartImpl) part).questions)
				{
					Question q = this.questionService.getQuestion(pick.getQuestionId());
					if (q != null)
					{
						// make sure we have this question's comlete pool
						Pool history = histories.get(q.getPool().getId());
						if (history == null)
						{
							Map<String, String> oldToNew = new HashMap<String, String>();
							history = this.poolService.makePoolHistory(q.getPool(), oldToNew);
							histories.put(q.getPool().getId(), history);
							oldToNews.put(q.getPool().getId(), oldToNew);
						}

						// get the mapping for this pool
						Map<String, String> oldToNew = oldToNews.get(q.getPool().getId());
						String historicalQid = oldToNew.get(q.getId());
						if (historicalQid != null)
						{
							pick.setQuestion(historicalQid);
						}
					}
				}
			}
		}
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
		this.archivedWas = other.archivedWas;
		this.assessmentService = other.assessmentService;
		this.changed = new ChangeableImpl(other.changed);
		this.context = other.context;
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy, this.changed);
		this.dates = new AssessmentDatesImpl(this, (AssessmentDatesImpl) other.dates, this.changed);
		this.grading = new AssessmentGradingImpl((AssessmentGradingImpl) other.grading, this.changed);
		this.honorPledge = other.honorPledge;
		this.id = other.id;
		this.lockedChanged = other.lockedChanged;
		this.messages = other.messages;
		this.live = other.live;
		this.locked = other.locked;
		this.mint = other.mint;
		this.modifiedBy = new AttributionImpl((AttributionImpl) other.modifiedBy, this.changed);
		this.parts = new AssessmentPartsImpl(this, (AssessmentPartsImpl) other.parts, this.changed);
		this.password = new AssessmentPasswordImpl((AssessmentPasswordImpl) other.password, this.changed);
		this.poolService = other.poolService;
		this.presentation = new PresentationImpl((PresentationImpl) other.presentation, this.changed);
		this.published = other.published;
		this.publishedWas = other.publishedWas;
		this.questionGrouping = other.questionGrouping;
		this.questionService = other.questionService;
		this.randomAccess = other.randomAccess;
		this.review = new AssessmentReviewImpl(this, (AssessmentReviewImpl) other.review, this.changed);
		this.showHints = other.showHints;
		this.submissionContext = other.submissionContext;
		this.submissionService = other.submissionService;
		this.submitPresentation = new PresentationImpl((PresentationImpl) other.submitPresentation, this.changed);
		this.specialAccess = new AssessmentSpecialAccessImpl(this, (AssessmentSpecialAccessImpl) other.specialAccess, this.changed);
		this.timeLimit = other.timeLimit;
		this.title = other.title;
		this.titleWas = other.titleWas;
		this.tries = other.tries;
		this.type = other.type;
	}
}
