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

import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentReview;
import org.muse.mneme.api.ReviewTiming;

/**
 * AssessmentReviewImpl implements AssessmentReview
 */
public class AssessmentReviewImpl implements AssessmentReview
{
	protected transient Assessment assessment = null;

	protected Date date = null;

	protected Boolean showCorrectAnswer = Boolean.FALSE;

	protected Boolean showFeedback = Boolean.FALSE;

	protected ReviewTiming timing = ReviewTiming.never;

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this belongs to.
	 */
	public AssessmentReviewImpl(Assessment assessment)
	{
		this.assessment = assessment;
	}

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this belongs to.
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentReviewImpl(Assessment assessment, AssessmentReviewImpl other)
	{
		set(other);
		this.assessment = assessment;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getDate()
	{
		return this.date;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getNowAvailable()
	{
		// if review timing is date, we can tell without a submission
		if (this.timing == ReviewTiming.date)
		{
			if (this.date != null)
			{
				return this.date.after(new Date());
			}

			// no date? no review
			return Boolean.FALSE;
		}

		// otherwise we need a submission
		if (this.assessment.getSubmissionContext() == null) return Boolean.FALSE;

		// for submittted
		if (this.timing == ReviewTiming.submitted)
		{
			return this.assessment.getSubmissionContext().getIsComplete();
		}

		// for graded
		if (this.timing == ReviewTiming.graded)
		{
			return this.assessment.getSubmissionContext().getIsGraded();
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getShowCorrectAnswer()
	{
		return this.showCorrectAnswer;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getShowFeedback()
	{
		return this.showFeedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReviewTiming getTiming()
	{
		return this.timing;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDate(Date date)
	{
		this.date = date;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowCorrectAnswer(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.showCorrectAnswer = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowFeedback(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.showFeedback = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTiming(ReviewTiming setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.timing = setting;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentReviewImpl other)
	{
		this.date = other.date;
		this.showCorrectAnswer = other.showCorrectAnswer;
		this.showFeedback = other.showFeedback;
		this.timing = other.timing;
	}
}
