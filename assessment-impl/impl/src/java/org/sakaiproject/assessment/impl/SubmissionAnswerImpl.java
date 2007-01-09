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
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.cover.EntityManager;
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

	/** Entries to delete (or reuse). */
	protected List<SubmissionAnswerEntryImpl> recycle = new ArrayList<SubmissionAnswerEntryImpl>();

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
		initEntries(other.getEntries());
		setMarkedForReview(other.getMarkedForReview());
		setRationale(other.getRationale());
		setSubmittedDate(other.getSubmittedDate());
		initRecycle(other.recycle);
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
	public String[] getAnswerFeedbacks()
	{
		String[] rv = new String[this.entries.size()];
		int i = 0;
		for (SubmissionAnswerEntry entry : this.entries)
		{
			AssessmentAnswer answer = entry.getAssessmentAnswer();
			if (answer == null)
			{
				rv[i++] = null;
			}
			else if ((answer.getIsCorrect() != null) && answer.getIsCorrect().booleanValue())
			{
				rv[i++] = answer.getFeedbackCorrect();
			}
			else
			{
				rv[i++] = answer.getFeedbackIncorrect();
			}
		}

		return rv;
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
	public List<Reference> getEntryAnswerAttachments()
	{
		List<String> answers = getEntryAnswerTexts();
		List<Reference> rv = new ArrayList<Reference>(answers.size());
		for (String answer : answers)
		{
			rv.add(EntityManager.newReference(answer));
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getEntryAnswerIds()
	{
		// one for each entry, in order
		List<String> rv = new ArrayList<String>(this.entries.size());
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv.add(entry.answerId);
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
	public List<String> getEntryAnswerTexts()
	{
		// one for each entry, in order
		List<String> rv = new ArrayList<String>(this.entries.size());
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv.add(entry.getAnswerText());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Boolean> getEntryCorrects()
	{
		// one for each entry, in order
		List<Boolean> rv = new ArrayList<Boolean>(this.entries.size());
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			rv.add(entry.getIsCorrect());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswered()
	{
		QuestionType type = getQuestion().getType();

		// fill in, numeric, essay, needs some text in an entry
		if ((type == QuestionType.essay) || (type == QuestionType.fillIn) || (type == QuestionType.numeric))
		{
			if (this.entries.size() == 0) return false;
			for (SubmissionAnswerEntryImpl entry : this.entries)
			{
				if ((entry.answerText != null) && (entry.answerText.length() > 0))
				{
					return true;
				}
			}
		}

		// matching, t/f, survey, multi choice and multi correct need at least one entry with a question answer id
		else
		{
			if (this.entries.size() == 0) return false;
			for (SubmissionAnswerEntryImpl entry : this.entries)
			{
				if ((entry.answerId != null) && (entry.answerId.length() > 0))
				{
					return true;
				}
			}
		}

		return Boolean.FALSE;
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
	public String getQuestionFeedback()
	{
		AssessmentQuestion question = this.getQuestion();

		// for survey and essay, use the general feedback
		if ((question.getType() == QuestionType.survey) || (question.getType() == QuestionType.essay))
		{
			return question.getFeedbackGeneral();
		}

		// otherwise check and use the correct or incorrect
		else if (this.submission.service.checkAnswer(this))
		{
			return question.getFeedbackCorrect();
		}

		return question.getFeedbackIncorrect();
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
	public void setEntryAnswerIds(String... answerIds)
	{
		// possibly adjust, if the question type allows variable number of entries, to the size of answerIds
		resizeEntries((answerIds == null) ? 0 : answerIds.length);

		// the ids size must match our entries size
		if ((answerIds != null) && (answerIds.length != this.entries.size()))
		{
			M_log.warn("setEntryAnswerIds: provided array does not match the entries");
			throw new RuntimeException();
		}

		AssessmentQuestion question = this.getQuestion();

		// set each answer id into the position-corresponding entry
		int i = 0;
		for (SubmissionAnswerEntryImpl entry : this.entries)
		{
			// treat an empty string as a missing id
			String aid = (answerIds == null) ? null : StringUtil.trimToNull(answerIds[i++]);

			// if not null, the answer id must be to our assessment question, and it must be in our question part
			if (aid != null)
			{
				if (question.getAnswer(aid) == null)
				{
					M_log.warn("setEntryAnswerIds: provided answerId not to our assessment question: answerId: " + aid
							+ " questionId: " + question.getId());
					throw new RuntimeException();
				}

				if (!(question.getAnswer(aid).getPart().getId().equals(entry.getQuestionPart().getId())))
				{
					M_log.warn("setEntryAnswerIds: provided answerId not to our assessment question part: answerId: " + aid
							+ " partId: " + entry.getQuestionPart().getId());
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

		// make sure all is well
		verifyEntries();
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
		// possibly adjust, if the question type allows variable number of entries, to the size of answerIds
		resizeEntries((answerTexts == null) ? 0 : answerTexts.length);

		// the texts size must match our entries size
		if ((answerTexts != null) && (answerTexts.length != this.entries.size()))
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
	 * Align the entries to match the question type and structure.
	 */
	protected void alignEntries()
	{
		// assure we have no entries already
		if (!this.entries.isEmpty())
		{
			M_log.warn("alignEntries: entries not null");
			throw new RuntimeException();
		}

		// assure we have a question
		AssessmentQuestion question = getQuestion();

		if (question == null)
		{
			M_log.warn("alignEntries: no question");
			throw new RuntimeException();
		}

		// fillin and numeric need an entry per assessment question (single part) answer, with answer ids set (answer text null)
		if ((question.getType() == QuestionType.fillIn) || (question.getType() == QuestionType.numeric))
		{
			for (AssessmentAnswer answer : question.getPart().getAnswersAsAuthored())
			{
				// make an entry for this question answer (of the single part), setting the question answer id, leaving the text null
				SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
				entry.initQuestionPartId(question.getPart().getId());
				entry.initAssessmentAnswerId(answer.getId());

				entry.initAnswer(this);
				this.entries.add(entry);
			}
		}

		// matching needs an entry per assessment question part
		else if (question.getType() == QuestionType.matching)
		{
			for (QuestionPart part : question.getParts())
			{
				// make an entry for this part, leaving the answer id and text null
				SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
				entry.initQuestionPartId(part.getId());

				entry.initAnswer(this);
				this.entries.add(entry);
			}
		}

		// all others need a single entry for the single part - multi-correct might expand this
		else
		{
			SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
			entry.initQuestionPartId(question.getPart().getId());

			entry.initAnswer(this);
			this.entries.add(entry);
		}
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
	 * Establish the entries as a deep copy of another - only if we do not have any entries to begin with.
	 * 
	 * @param entries
	 *        the entries to deep copy.
	 */
	protected void initEntries(List<? extends SubmissionAnswerEntry> entries)
	{
		// assure that we have no entries
		if (!this.entries.isEmpty())
		{
			M_log.warn("initEntries: entries already exist");
			throw new RuntimeException();
		}

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
	 * Init the assessment question that this is an answer to, and align the entries to match.
	 * 
	 * @param question
	 *        The assessment question.
	 */
	protected void initQuestion(AssessmentQuestion question)
	{
		this.questionId = question.getId();

		// align the entries with this question
		alignEntries();

		// make sure all is well
		verifyEntries();
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

		// do NOT align the entries...
	}

	/**
	 * Deep copy a set of entries into our recycle
	 * 
	 * @param entries
	 *        The set of entries to copy.
	 */
	protected void initRecycle(List<? extends SubmissionAnswerEntry> entries)
	{
		if (entries == null) return;

		// deep copy, adding to our recycle
		for (SubmissionAnswerEntry entry : entries)
		{
			SubmissionAnswerEntryImpl copy = new SubmissionAnswerEntryImpl((SubmissionAnswerEntryImpl) entry);
			this.recycle.add(copy);
			copy.initAnswer(this);
		}
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
	 * Adjust the entries for question types that are variable to match this number of entries
	 * 
	 * @param answerIds
	 *        the set of answer ids to size to.
	 */
	protected void resizeEntries(int size)
	{
		AssessmentQuestion question = getQuestion();

		// expand or contract for multi-correct and file upload
		if ((question.getType() == QuestionType.multipleCorrect) || (question.getType() == QuestionType.fileUpload))
		{
			// count the answers provided = minimum of one
			if (size < 1) size = 1;
			int excess = this.entries.size() - size;

			// if we have too few, pull out of the recycle or create new
			while (excess < 0)
			{
				SubmissionAnswerEntryImpl entry = null;

				if (this.recycle.size() > 0)
				{
					entry = this.recycle.remove(this.recycle.size() - 1);
				}
				else
				{
					entry = new SubmissionAnswerEntryImpl();
				}

				// set to this question part, clear the rest (preserve the id if it was set)
				entry.initQuestionPartId(question.getPart().getId());
				entry.setAnswerText(null);
				entry.setAssessmentAnswer(null);

				entry.initAnswer(this);
				this.entries.add(entry);

				excess++;
			}

			// if we have too many send a few to the recycle
			while (excess > 0)
			{
				this.recycle.add(this.entries.remove(this.entries.size() - 1));

				excess--;
			}
		}
	}

	/**
	 * Check that the entries are properly aligned to our question type and structure
	 */
	protected void verifyEntries()
	{
		// assure we have a question
		AssessmentQuestion question = getQuestion();

		if (question == null)
		{
			M_log.warn("alignEntries: no question");
			throw new RuntimeException();
		}

		// fillin and numeric need an entry per assessment question (single part) answer, with answer ids set
		if ((question.getType() == QuestionType.fillIn) || (question.getType() == QuestionType.numeric))
		{
			if (this.entries.size() != question.getPart().getAnswersAsAuthored().size())
			{
				M_log.warn("verifyEntries: fillin/numeric: num answers: " + question.getPart().getAnswersAsAuthored().size()
						+ " doesn't match num entries: " + this.entries.size() + " submission: " + this.getSubmission().getId()
						+ " question: " + question.getId());
				throw new RuntimeException();
			}
			for (int i = 0; i < this.entries.size(); i++)
			{
				// check that the entry is to the part and answer
				SubmissionAnswerEntryImpl entry = this.entries.get(i);
				AssessmentAnswer answer = question.getPart().getAnswersAsAuthored().get(i);
				if (!entry.questionPartId.equals(question.getPart().getId()))
				{
					M_log.warn("verifyEntries: fillin/numeric: entry / answer part not aligned: entry part: "
							+ entry.questionPartId + " question single part: " + question.getPart().getId() + " submission: "
							+ this.getSubmission().getId() + " question: " + question.getId());
					throw new RuntimeException();
				}

				if (!entry.answerId.equals(answer.getId()))
				{
					M_log.warn("verifyEntries: fillin/numeric: entry / answer answer id not aligned: entry answer id: "
							+ entry.answerId + " answer id: " + answer.getId() + " submission: " + this.getSubmission().getId()
							+ " question: " + question.getId());
					throw new RuntimeException();
				}
			}
		}

		// matching needs an entry per assessment question part
		else if (question.getType() == QuestionType.matching)
		{
			if (this.entries.size() != question.getParts().size())
			{
				M_log.warn("verifyEntries: matching: num parts: " + question.getParts().size() + " doesn't match num entries: "
						+ this.entries.size() + " submission: " + this.getSubmission().getId() + " question: " + question.getId());
				throw new RuntimeException();
			}
			for (int i = 0; i < this.entries.size(); i++)
			{
				// check that the entry is to the part
				SubmissionAnswerEntryImpl entry = this.entries.get(i);
				QuestionPart part = question.getParts().get(i);
				if (!entry.questionPartId.equals(part.getId()))
				{
					M_log.warn("verifyEntries: matching: entry / question part not aligned: entry part: " + entry.questionPartId
							+ " question part: " + part.getId() + " submission: " + this.getSubmission().getId() + " question: "
							+ question.getId());
					throw new RuntimeException();
				}
			}
		}

		// all others need a single entry for the single part - multi-correct and file upload might expand this
		else
		{
			if (this.entries.size() < 1)
			{
				M_log.warn("verifyEntries: (other): no entries: " + " submission: " + this.getSubmission().getId() + " question: "
						+ this.getQuestion().getId());
				throw new RuntimeException();
			}

			if ((this.entries.size() > 1)
					&& ((question.getType() != QuestionType.multipleCorrect) && (question.getType() != QuestionType.fileUpload)))
			{
				M_log.warn("verifyEntries: (other): too many entries: " + this.entries.size() + " submission: "
						+ this.getSubmission().getId() + " question: " + this.getQuestion().getId());
				throw new RuntimeException();
			}

			// each entry needs to be to the single part
			for (SubmissionAnswerEntryImpl entry : this.entries)
			{
				if (!entry.questionPartId.equals(question.getPart().getId()))
				{
					M_log.warn("verifyEntries: (other): entry part: " + entry.questionPartId + " doesn't match question part: "
							+ question.getPart().getId() + " submission: " + this.getSubmission().getId() + " question: "
							+ this.getQuestion().getId());
					throw new RuntimeException();
				}
			}
		}
	}
}
