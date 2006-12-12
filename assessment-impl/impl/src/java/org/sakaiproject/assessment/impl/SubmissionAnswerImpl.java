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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.util.StringUtil;

public class SubmissionAnswerImpl implements SubmissionAnswer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SubmissionAnswerImpl.class);

	protected List<SubmissionAnswerEntryImpl> entries = new ArrayList<SubmissionAnswerEntryImpl>();

	protected Boolean markedForReview = Boolean.FALSE;

	/** Don't store the assessment question here. */
	protected String questionId = null;

	protected String rationale = null;

	/** back pointer to the submission this answer is part of. */
	protected transient SubmissionImpl submission = null;

	protected Time submittedDate = null;

	/** These entries are allocated but not currently in use. */
	protected List<SubmissionAnswerEntryImpl> unusedEntries = new ArrayList<SubmissionAnswerEntryImpl>();

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
		setEntries(other.getEntries());
		setMarkedForReview(other.getMarkedForReview());
		questionId = other.questionId;
		setRationale(other.getRationale());
		this.submission = other.submission;
		setSubmittedDate(other.getSubmittedDate());
		initUnusedEntries(other.unusedEntries);
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
		if (this.submission != ((SubmissionAnswerImpl) obj).submission) return false;
		if (this.questionId != ((SubmissionAnswerImpl) obj).questionId) return false;
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
		// one for each entry
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
		// make sure we have (at least) a singe entry
		if (this.entries.isEmpty())
		{
			// add one
			SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
			entry.initAnswer(this);
			this.entries.add(entry);
		}

		return this.entries.get(0).getAnswerText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getEntryAnswerTexts()
	{
		// one for each entry
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
		// one for each entry
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
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerIds(String... answerIds)
	{
		// move to unused any entry that is not in the answerIds
		for (Iterator i = this.entries.iterator(); i.hasNext();)
		{
			SubmissionAnswerEntryImpl entry = (SubmissionAnswerEntryImpl) i.next();

			if (!StringUtil.contains(answerIds, entry.answerId))
			{
				this.unusedEntries.add(entry);
				entry.setAssessmentAnswerId(null);
				entry.setAnswerText(null);
				entry.initAutoScore(null);
				i.remove();
			}
		}

		int answerIdsLength = 0;
		if (answerIds != null) answerIdsLength = answerIds.length;

		// increase the entry count if needed
		int needed = answerIdsLength - (this.entries.size() + this.unusedEntries.size());
		while (needed > 0)
		{
			SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
			entry.initAnswer(this);
			this.unusedEntries.add(entry);
			needed--;
		}

		// find each answerId in the entries, or add it, and collect them in answerId order
		if (answerIds != null)
		{
			for (String aid : answerIds)
			{
				boolean found = false;
				for (SubmissionAnswerEntryImpl entry : this.entries)
				{
					if ((entry.answerId != null) && (entry.answerId.equals(aid)))
					{
						found = true;
						break;
					}
				}

				// add if needed (we made sure there is room in unused for this)
				if (!found)
				{
					SubmissionAnswerEntryImpl entry = this.unusedEntries.remove(this.unusedEntries.size() - 1);
					entry.answerId = aid;
					entry.initAutoScore(null);
					this.entries.add(entry);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerText(String answerText)
	{
		// make sure we have (at least) a singe entry
		if (this.entries.isEmpty())
		{
			// add one
			SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
			entry.initAnswer(this);
			this.entries.add(entry);
		}

		this.entries.get(0).setAnswerText(answerText);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEntryAnswerTexts(String... answerTexts)
	{
		// Note: this works with single part questions

		// adjust the entries so that there is one for each question answer, pointing at that answer

		// TODO: assume that we are either empty or correct already
		AssessmentQuestion question = this.getQuestion();
		if (this.entries.isEmpty())
		{
			// create the entries
			for (AssessmentAnswer questionAnswer : question.getPart().getAnswers())
			{
				SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
				entry.initAnswer(this);
				entry.setAssessmentAnswer(questionAnswer);
				this.entries.add(entry);
			}
		}
		else
		{
			// check
			for (int i = 0; i < this.entries.size(); i++)
			{
				if (!question.getPart().getAnswers().get(i).equals(this.entries.get(i).getAssessmentAnswer()))
				{
					M_log.warn("setEntryAnswerTexts: existing entries don't match question answers");
					break;
				}
			}
		}

		// apply the answerTexts to the answers in order
		int i = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			if ((answerTexts != null) && (answerTexts.length > i))
			{
				entry.setAnswerText(answerTexts[i]);
			}
			else
			{
				M_log.warn("setEntryAnswerTexts: ran out of values to set: " + i);
				entry.setAnswerText(null);
			}
			i++;
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
		this.questionId = question.getId();
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

	/**
	 * {@inheritDoc}
	 */
	protected void initUnusedEntries(List<? extends SubmissionAnswerEntry> entries)
	{
		this.unusedEntries.clear();
		if (entries == null) return;

		// deep copy
		for (SubmissionAnswerEntry entry : entries)
		{
			SubmissionAnswerEntryImpl copy = new SubmissionAnswerEntryImpl((SubmissionAnswerEntryImpl) entry);
			this.unusedEntries.add(copy);
			copy.initAnswer(this);
		}
	}
}
