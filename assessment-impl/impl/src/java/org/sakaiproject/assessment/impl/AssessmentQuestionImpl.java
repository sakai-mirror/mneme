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

import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.Ordering;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;

public class AssessmentQuestionImpl implements AssessmentQuestion
{
	/** Ordering logic within the assessment. */
	public class MyAssessmentOrdering implements Ordering<AssessmentQuestion>
	{
		protected AssessmentQuestionImpl question = null;

		MyAssessmentOrdering(AssessmentQuestionImpl question)
		{
			this.question = question;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (question.getSectionOrdering().getIsFirst())
			{
				if (question.getSection().getAssessment() == null) return true;

				return question.getSection().getOrdering().getIsFirst();
			}

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (question.getSectionOrdering().getIsLast())
			{
				if (question.getSection().getAssessment() == null) return true;

				return question.getSection().getOrdering().getIsLast();
			}

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentQuestion getNext()
		{
			AssessmentQuestion next = question.getSectionOrdering().getNext();
			if (next == null)
			{
				// go to next section, if any
				AssessmentSection nextPart = question.getSection().getOrdering().getNext();
				if (nextPart != null)
				{
					next = nextPart.getFirstQuestion();
				}
			}

			return next;
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			// within the section
			Integer sectionOrdering = question.getSectionOrdering().getPosition();

			if (question.getSection() == null) return sectionOrdering;
			if (question.getSection().getAssessment() == null) return sectionOrdering;

			// count the questions in each section before this section
			int count = 0;
			for (AssessmentSection section : question.getSection().getAssessment().getSections())
			{
				if (section == question.getSection()) break;
				count += section.getNumQuestions();
			}

			return new Integer(count + sectionOrdering.intValue());
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentQuestion getPrevious()
		{
			AssessmentQuestion prev = question.getSectionOrdering().getPrevious();
			if (prev == null)
			{
				// go to prev section, if any
				AssessmentSection prevPart = question.getSection().getOrdering().getPrevious();
				if (prevPart != null)
				{
					prev = prevPart.getLastQuestion();
				}
			}

			return prev;
		}
	}

	/** Ordering logic within the section. */
	public class MySectionOrdering implements Ordering<AssessmentQuestion>
	{
		protected AssessmentQuestionImpl question = null;

		MySectionOrdering(AssessmentQuestionImpl question)
		{
			this.question = question;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (question.getSection() == null) return true;

			if (question.equals(question.getSection().getFirstQuestion())) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (question.getSection() == null) return true;

			if (question.equals(question.getSection().getLastQuestion())) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentQuestion getNext()
		{
			if (question.getSection() == null) return null;

			List<? extends AssessmentQuestion> questions = question.getSection().getQuestions();
			int index = questions.indexOf(question);
			if (index == questions.size() - 1) return null;

			return questions.get(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (question.getSection() == null) return new Integer(1);

			int index = question.getSection().getQuestions().indexOf(question);

			return new Integer(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentQuestion getPrevious()
		{
			if (question.getSection() == null) return null;

			List<? extends AssessmentQuestion> questions = question.getSection().getQuestions();
			int index = questions.indexOf(question);
			if (index == 0) return null;

			return questions.get(index - 1);
		}
	}

	protected transient MyAssessmentOrdering assessmentOrdering = new MyAssessmentOrdering(this);

	protected Boolean caseSensitive = null;

	protected String feedbackCorrect = null;

	protected String feedbackGeneral = null;

	protected String feedbackIncorrect = null;

	protected String id = null;

	protected String instructions = null;

	protected Boolean mutuallyExclusive = null;

	protected List<QuestionPartImpl> parts = new ArrayList<QuestionPartImpl>();

	protected Boolean randomAnswerOrdering = null;

	protected Boolean requireRationale = null;

	protected Float score = null;

	/** The back pointer to the section */
	protected transient AssessmentSectionImpl section = null;

	protected transient MySectionOrdering sectionOrdering = new MySectionOrdering(this);

	protected QuestionType type = null;

	/**
	 * Construct
	 */
	public AssessmentQuestionImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public AssessmentQuestionImpl(AssessmentQuestionImpl other)
	{
		setCaseSensitive(other.getCaseSensitive());
		setFeedbackCorrect(other.getFeedbackCorrect());
		setFeedbackGeneral(other.getFeedbackGeneral());
		setFeedbackIncorrect(other.getFeedbackIncorrect());
		initId(other.getId());
		setInstructions(other.getInstructions());
		setMutuallyExclusive(other.getMutuallyExclusive());
		setParts(other.getParts());
		setRandomAnswerOrder(other.getRandomAnswerOrder());
		setRequireRationale(other.getRequireRationale());
		setScore(other.getPoints());
		this.section = other.section;
		setType(other.getType());
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof AssessmentQuestion)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare? position?
		int compare = getSectionOrdering().getPosition().compareTo(((AssessmentQuestion) obj).getSectionOrdering().getPosition());

		return compare;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AssessmentQuestion)) return false;
		if (this == obj) return true;
		if (this.getId() == null) return false;
		if (((AssessmentQuestion) obj).getId() == null) return false;
		return ((AssessmentQuestion) obj).getId().equals(getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAnswer getAnswer(String answerId)
	{
		for (QuestionPartImpl part : this.parts)
		{
			AssessmentAnswer answer = part.getAnswer(answerId);
			if (answer != null) return answer;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKey()
	{
		return getAnswerKey(false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKeyAsAuthored()
	{
		return getAnswerKey(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<AssessmentQuestion> getAssessmentOrdering()
	{
		return this.assessmentOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getCaseSensitive()
	{
		if (this.caseSensitive == null) return Boolean.FALSE;
		return this.caseSensitive;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFeedbackCorrect()
	{
		return this.feedbackCorrect;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFeedbackGeneral()
	{
		return this.feedbackGeneral;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFeedbackIncorrect()
	{
		return this.feedbackIncorrect;
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
	public String getInstructions()
	{
		return this.instructions;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMutuallyExclusive()
	{
		if (this.mutuallyExclusive == null) return Boolean.FALSE;
		return this.mutuallyExclusive;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPart getPart()
	{
		return this.parts.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPart getPart(String partId)
	{
		for (QuestionPartImpl part : this.parts)
		{
			if (part.getId().equals(partId)) return part;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends QuestionPart> getParts()
	{
		return this.parts;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getPoints()
	{
		return this.score;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomAnswerOrder()
	{
		return this.randomAnswerOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRequireRationale()
	{
		return this.requireRationale;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSection getSection()
	{
		return this.section;
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<AssessmentQuestion> getSectionOrdering()
	{
		return this.sectionOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTitle()
	{
		// matching type uses instructions, all others use single part's title
		// fill-in and numeric replace {} with ____
		if (getType() == QuestionType.matching)
		{
			return getInstructions();
		}

		// otherwise we use the single part's title
		QuestionPart part = getPart();
		if (part != null)
		{
			String title = part.getTitle();
			if (title != null)
			{
				// numeric and matching's replacement
				if ((getType() == QuestionType.fillIn) || (getType() == QuestionType.numeric))
				{
					title = title.replaceAll("\\{\\}", "____");
				}
			}

			return title;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionType getType()
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
	public void setCaseSensitive(Boolean value)
	{
		this.caseSensitive = value;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackCorrect(String feedback)
	{
		this.feedbackCorrect = feedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackGeneral(String feedback)
	{
		this.feedbackGeneral = feedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedbackIncorrect(String feedback)
	{
		this.feedbackIncorrect = feedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInstructions(String instructions)
	{
		this.instructions = instructions;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMutuallyExclusive(Boolean value)
	{
		this.mutuallyExclusive = value;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setParts(List<? extends QuestionPart> parts)
	{
		this.parts.clear();

		if (parts == null)
		{
			return;
		}

		// deep copy
		for (QuestionPart part : parts)
		{
			QuestionPartImpl copy = new QuestionPartImpl((QuestionPartImpl) part);
			this.parts.add(copy);
			copy.initQuestion(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomAnswerOrder(Boolean setting)
	{
		this.randomAnswerOrdering = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequireRationale(Boolean setting)
	{
		this.requireRationale = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScore(Float score)
	{
		this.score = score;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setType(QuestionType type)
	{
		this.type = type;
	}

	/**
	 * Get the answer key, either in the current user's answer presentation order, or as authored.
	 * 
	 * @param asAuthored
	 *        if true, use the authored answer order, else use the current user's presentation order.
	 * @return The answer key.
	 */
	protected String getAnswerKey(boolean asAuthored)
	{
		// fill-in, numeric, true/false
		if ((this.type == QuestionType.fillIn) || (this.type == QuestionType.numeric) || (this.type == QuestionType.trueFalse))
		{
			// concat the various answer texts
			StringBuffer rv = new StringBuffer();

			// get each part's correct answer
			for (QuestionPart part : this.parts)
			{
				List<AssessmentAnswer> correct = part.getCorrectAnswers();
				if (correct != null)
				{
					for (AssessmentAnswer answer : correct)
					{
						rv.append(answer.getText() + ", ");
					}
				}
			}

			// remove the last two characters (the trailing ", ")
			rv.setLength(rv.length() - 2);

			return rv.toString();
		}

		// match (be concerned about asAuthored)
		if (this.type == QuestionType.matching)
		{
			// form the matches: the position:label
			StringBuffer rv = new StringBuffer();

			// get each part's correct answer label
			int pos = 1;
			for (QuestionPart part : this.parts)
			{
				rv.append(Integer.toString(pos++));
				rv.append(':');

				// as authored, or as displayed to the current user (possibly randomized for the user)
				List<? extends AssessmentAnswer> answers = asAuthored ? part.getAnswersAsAuthored() : part.getAnswers();
				for (AssessmentAnswer answer : answers)
				{
					if (answer.getIsCorrect().booleanValue())
					{
						rv.append(answer.getLabel());
					}
				}
				rv.append(", ");
			}

			// remove the last two characters (the trailing ", "
			rv.setLength(rv.length() - 2);

			return rv.toString();
		}

		// multi choice (be concerned about asAuthored)
		if ((this.type == QuestionType.multipleChoice) || (this.type == QuestionType.multipleCorrect))
		{
			// concat the various answer labels
			StringBuffer rv = new StringBuffer();

			// get each part's correct answer
			for (QuestionPart part : this.parts)
			{
				// as authored, or as displayed to the current user (possibly randomized for the user)
				List<? extends AssessmentAnswer> answers = asAuthored ? part.getAnswersAsAuthored() : part.getAnswers();
				for (AssessmentAnswer answer : answers)
				{
					if (answer.getIsCorrect().booleanValue())
					{
						rv.append(answer.getLabel() + ", ");
					}
				}
			}

			// remove the last two characters (the trailing ", "
			rv.setLength(rv.length() - 2);

			return rv.toString();
		}

		return null;
	}

	/**
	 * Establish the id.
	 * 
	 * @param id
	 *        The question id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Initialize the parts.
	 * 
	 * @param parts
	 *        The parts - these are taken exactly, not deep copied.
	 */
	protected void initParts(List<QuestionPartImpl> parts)
	{
		this.parts = parts;

		// set the back link
		for (QuestionPartImpl part : this.parts)
		{
			part.initQuestion(this);
		}
	}

	/**
	 * Establish the link to the containing section.
	 * 
	 * @param section
	 *        The containing section.
	 */
	protected void initSection(AssessmentSectionImpl section)
	{
		this.section = section;
	}
}
