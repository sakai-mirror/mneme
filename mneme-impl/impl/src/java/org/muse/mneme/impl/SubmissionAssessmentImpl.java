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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AcceptSubmitStatus;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPassword;
import org.muse.mneme.api.AssessmentDates;
import org.muse.mneme.api.AssessmentGrading;
import org.muse.mneme.api.AssessmentParts;
import org.muse.mneme.api.AssessmentReview;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.Submission;

/**
 * SubmissionAssessmentImpl implements Assessment, and implements a submission's dual connection to assessment defintions.
 */
public class SubmissionAssessmentImpl implements Assessment
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionAssessmentImpl.class);

	protected AssessmentService assessmentService = null;

	/** The assessment id that the parts, presentation and properties come from. */
	protected String historicalAssessmentId = null;

	/** The assessment id the submission is part of, also used for getting submission-global settings. */
	protected String mainAssessmentId = null;

	protected SubmissionImpl submission = null;

	/**
	 * Construct
	 */
	public SubmissionAssessmentImpl(String mainAssessmentId, String historicalAssessmentId, SubmissionImpl submission, AssessmentService service)
	{
		this.mainAssessmentId = mainAssessmentId;
		this.historicalAssessmentId = historicalAssessmentId;
		this.assessmentService = service;
		this.submission = submission;
	}

	/**
	 * Construct as a deep copy of another
	 */
	protected SubmissionAssessmentImpl(SubmissionAssessmentImpl other, SubmissionImpl submission)
	{
		set(other, submission);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		return getMainAssessment().equals(obj);
	}

	/**
	 * {@inheritDoc}
	 */
	public AcceptSubmitStatus getAcceptSubmitStatus()
	{
		return getMainAssessment().getAcceptSubmitStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentPassword getPassword()
	{
		return getMainAssessment().getPassword();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getArchived()
	{
		return getMainAssessment().getArchived();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		return getMainAssessment().getContext();
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getCreatedBy()
	{
		return getMainAssessment().getCreatedBy();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentDates getDates()
	{
		return getMainAssessment().getDates();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentGrading getGrading()
	{
		return getMainAssessment().getGrading();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return getMainAssessment().getId();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsClosed()
	{
		return getMainAssessment().getIsClosed();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsFullyReleased()
	{
		return getMainAssessment().getIsFullyReleased();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsLive()
	{
		return getMainAssessment().getIsLive();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsMultipleSubmissionsAllowed()
	{
		return getMainAssessment().getIsMultipleSubmissionsAllowed();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsOpen(Boolean withGrace)
	{
		return getMainAssessment().getIsOpen(withGrace);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsValid()
	{
		// TODO: which? both? needed?
		return getMainAssessment().getIsValid();
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getModifiedBy()
	{
		return getMainAssessment().getModifiedBy();
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumSubmissionsAllowed()
	{
		return getMainAssessment().getNumSubmissionsAllowed();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentParts getParts()
	{
		return getHistoricalAssessment().getParts();
	}

	/**
	 * {@inheritDoc}
	 */
	public Presentation getPresentation()
	{
		return getHistoricalAssessment().getPresentation();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getPublished()
	{
		return getMainAssessment().getPublished();
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionGrouping getQuestionGrouping()
	{
		return getHistoricalAssessment().getQuestionGrouping();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomAccess()
	{
		return getHistoricalAssessment().getRandomAccess();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRequireHonorPledge()
	{
		return getHistoricalAssessment().getRequireHonorPledge();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentReview getReview()
	{
		return getMainAssessment().getReview();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getScores()
	{
		return getMainAssessment().getScores();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getShowHints()
	{
		return getHistoricalAssessment().getShowHints();
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmissionContext()
	{
		// TODO: return this.submission?
		return getMainAssessment().getSubmissionContext();
	}

	/**
	 * {@inheritDoc}
	 */
	public Presentation getSubmitPresentation()
	{
		return getHistoricalAssessment().getSubmitPresentation();
	}

	/**
	 * {@inheritDoc}
	 */
	public Long getTimeLimit()
	{
		return getHistoricalAssessment().getTimeLimit();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		return getMainAssessment().getTitle();
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentType getType()
	{
		return getMainAssessment().getType();
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return getMainAssessment().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setArchived(Boolean archived)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContext(String context)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNumSubmissionsAllowed(Integer count)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPublished(Boolean published)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionGrouping(QuestionGrouping value)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAccess(Boolean setting)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequireHonorPledge(Boolean honorPledge)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowHints(Boolean showHints)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeLimit(Long limit)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setType(AssessmentType type)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * Access the historical assessment.
	 * 
	 * @return The historical assessment.
	 */
	protected Assessment getHistoricalAssessment()
	{
		// TODO: cache the actual assessment for the thread, to avoid the real assessment cache's copy-out policy

		AssessmentImpl rv = (AssessmentImpl) this.assessmentService.getAssessment(this.historicalAssessmentId);
		rv.initSubmissionContext(this.submission);
		return rv;
	}

	/**
	 * Access the main assessment.
	 * 
	 * @return The main assessment.
	 */
	protected Assessment getMainAssessment()
	{
		// TODO: cache the actual assessment for the thread, to avoid the real assessment cache's copy-out policy

		AssessmentImpl rv = (AssessmentImpl) this.assessmentService.getAssessment(this.mainAssessmentId);
		rv.initSubmissionContext(this.submission);
		return rv;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(SubmissionAssessmentImpl other, SubmissionImpl submission)
	{
		this.assessmentService = other.assessmentService;
		this.mainAssessmentId = other.mainAssessmentId;
		this.historicalAssessmentId = other.historicalAssessmentId;
		this.submission = submission;
	}
}
