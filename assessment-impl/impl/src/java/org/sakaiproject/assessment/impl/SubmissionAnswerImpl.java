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
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.util.StringUtil;

public class SubmissionAnswerImpl implements SubmissionAnswer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionAnswerImpl.class);

	/** Entries are ordered to match the assessment question part order, and there's one entry per part. */
	protected List<SubmissionAnswerEntryImpl> entries = new ArrayList<SubmissionAnswerEntryImpl>();

	protected Boolean markedForReview = Boolean.FALSE;

	/** Don't store the assessment question here. */
	protected String questionId = null;

	protected String rationale = null;

	/** back pointer to the submission this answer is part of. */
	protected transient SubmissionImpl submission = null;

	protected Time submittedDate = null;

	/**
	 * Construct
	 */
	public SubmissionAnswerImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public SubmissionAnswerImpl(SubmissionAnswerImpl other)
	{
		this.submission = other.submission;
		questionId = other.questionId;
		setEntries(other.getEntries());
		setMarkedForReview(other.getMarkedForReview());
		setRationale(other.getRationale());
		setSubmittedDate(other.getSubmittedDate());
	}

	/**
	 * {@inheritDoc}
	 */
	public void autoScore()
	{
		this.submission.service.scoreAnswer(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof SubmissionAnswer)) throw new ClassCastException();

		// no natural ordering?
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SubmissionAnswer)) return false;
		if (this == obj) return true;
		if (!this.submission.getId().equals(((SubmissionAnswerImpl) obj).submission.getId())) return false;
		if (!this.questionId.equals(((SubmissionAnswerImpl) obj).questionId)) return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		autoScore();

		float score = countAutoScore();
		return new Float(score);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends SubmissionAnswerEntry> getEntries()
	{
		return this.entries;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getEntryAnswerIds()
	{
		// one for each entry, in order
		String[] rv = new String[this.entries.size()];
		int pos = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv[pos++] = entry.answerId;
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntryAnswerText()
	{
		if (this.entries.size() < 1) return null;

		return this.entries.get(0).getAnswerText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getEntryAnswerTexts()
	{
		// one for each entry, in order
		String[] rv = new String[this.entries.size()];
		int pos = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv[pos++] = entry.getAnswerText();
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean[] getEntryCorrects()
	{
		// one for each entry, in order
		Boolean[] rv = new Boolean[this.entries.size()];
		int pos = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv[pos++] = entry.getIsCorrect();
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMarkedForReview()
	{
		return this.markedForReview;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getQuestion()
	{
		return this.submission.getAssessment().getQuestion(this.questionId);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRationale()
	{
		return this.rationale;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission()
	{
		return this.submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public Time getSubmittedDate()
	{
		return this.submittedDate;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		return (this.submission.getId() + this.questionId).hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntries(List<? extends SubmissionAnswerEntry> entries)
	{
		this.entries.clear();
		if (entries == null) return;

		// deep copy
		for (SubmissionAnswerEntry entry : entries)
		{
			SubmissionAnswerEntryImpl copy = new SubmissionAnswerEntryImpl((SubmissionAnswerEntryImpl) entry);
			this.entries.add(copy);
			copy.initAnswer(this);
		}

		// assure that the new entries align with out question's parts
		if (this.entries.size() != this.getQuestion().getParts().size())
		{
			M_log.warn("setEntries: entries do not align with question parts: num entries: " + this.entries.size()
					+ " num question parts: " + this.getQuestion().getParts().size());
			throw new RuntimeException();
		}
		for (int i = 0; i < this.entries.size(); i++)
		{
			SubmissionAnswerEntryImpl entry = this.entries.get(i);
			QuestionPart part = this.getQuestion().getParts().get(i);
			if (!entry.getQuestionPart().equals(part))
			{
				M_log.warn("setEntries: entries do not align with question parts: entry id: " + entry.getId() + " part id: "
						+ part.getId());
				throw new RuntimeException();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerIds(String... answerIds)
	{
		// the ids size must match our entries size
		if ((answerIds != null) && (answerIds.length != this.entries.size()))
		{
			M_log.warn("setEntryAnswerIds: provided array does not match the entries");
			throw new RuntimeException();
		}

		// set each answer id into the position-corresponding entry
		int i = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			// treat an empty string as a missing id
			String aid = (answerIds == null) ? null : StringUtil.trimToNull(answerIds[i++]);

			// if not null, the answer id must be to our assessment question, and it must be in our question part
			if (aid != null)
			{
				if (this.getQuestion().getAnswer(aid) == null)
				{
					M_log.warn("setEntryAnswerIds: provided answerId not to our assessment question: answerId: " + aid
							+ " questionId: " + this.getQuestion().getId());
					throw new RuntimeException();
				}

				if (!(this.getQuestion().getAnswer(aid).getPart().getId().equals(entry.getQuestionPart().getId())))
				{
					M_log.warn("setEntryAnswerIds: provided answerId not to our assessment question part: answerId: "
							+ aid + " partId: " + entry.getQuestionPart().getId());
					throw new RuntimeException();
				}
			}

			// store the new answer id
			entry.initAssessmentAnswerId(aid);

			// clear the auto score
			entry.initAutoScore(null);

			// clear any text
			entry.setAnswerText(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerText(String answerText)
	{
		// assure that we have only a single entry
		if (this.entries.size() != 1)
		{
			M_log.warn("setEntryAnswerText: number of entries does not match 1: " + this.entries.size());
			throw new RuntimeException();
		}

		this.entries.get(0).setAnswerText(answerText);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerTexts(String... answerTexts)
	{
		// the texts size must match our entries size
		if ((answerTexts != null) || (answerTexts.length != this.entries.size()))
		{
			M_log.warn("setEntryAnswerTexts: provided array does not match the entries");
			throw new RuntimeException();
		}

		// set each answer text into the position-corresponding entry
		int i = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			String answerText = (answerTexts == null) ? null : StringUtil.trimToNull(answerTexts[i++]);

			// store the new answer text
			entry.setAnswerText(answerText);

			// clear the auto score
			entry.initAutoScore(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMarkedForReview(Boolean forReview)
	{
		this.markedForReview = forReview == null ? Boolean.FALSE : forReview;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestion(AssessmentQuestion question)
	{
		if (!question.getId().equals(this.questionId))
		{
			this.questionId = question.getId();

			// align the entries with this question's parts, all unanswered
			this.entries.clear();

			if (question != null)
			{
				for (QuestionPart part : question.getParts())
				{
					// make an entry for this part
					SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
					entry.initQuestionPartId(part.getId());
					entry.initAnswer(this);
					this.entries.add(entry);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRationale(String rationale)
	{
		this.rationale = rationale;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmittedDate(Time submitted)
	{
		this.submittedDate = submitted;
	}

	/**
	 * Count up the entry's auto scores - don't re-score.
	 * 
	 * @return The sum of the entry's auto-scores.
	 */
	protected float countAutoScore()
	{
		float score = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			score += entry.getAutoScore();
		}
		return score;
	}

	/**
	 * Access the question id.
	 * 
	 * @return The question id.
	 */
	protected String getQuestionId()
	{
		return this.questionId;
	}

	/**
	 * Establish the entries. No copy.
	 * 
	 * @param entries
	 *        The new list of entries to use.
	 */
	protected void initEntries(List<SubmissionAnswerEntryImpl> entries)
	{
		this.entries = entries;

		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			entry.initAnswer(this);
		}
	}

	/**
	 * Establish the question id.
	 * 
	 * @param questionId
	 *        The question id.
	 */
	protected void initQuestionId(String questionId)
	{
		this.questionId = questionId;
	}

	/**
	 * Establish the back pointer to the submission that this answer is part of.
	 * 
	 * @param submission
	 *        The back pointer to the submission that this answer is part of.
	 */
	protected void initSubmission(SubmissionImpl submission)
	{
		this.submission = submission;
	}
}
