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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.Ordering;

/**
 * <p>
 * AssessmentImpl is ...
 * </p>
 */
public class AssessmentSectionImpl implements AssessmentSection
{
	public class MyOrdering implements Ordering<AssessmentSection>
	{
		protected AssessmentSectionImpl section = null;

		public MyOrdering(AssessmentSectionImpl section)
		{
			this.section = section;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (section.getAssessment() == null) return true;

			if (section.equals(section.getAssessment().getSections().get(0))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (section.getAssessment() == null) return true;

			if (section.equals(section.getAssessment().getSections().get(section.getAssessment().getSections().size() - 1)))
				return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentSection getNext()
		{
			if (section.getAssessment() == null) return null;

			int index = section.getAssessment().getSections().indexOf(section);
			if (index == section.getAssessment().getSections().size() - 1) return null;

			return section.getAssessment().getSections().get(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (section.getAssessment() == null) return new Integer(1);

			int index = section.getAssessment().getSections().indexOf(section);

			return new Integer(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public AssessmentSection getPrevious()
		{
			if (section.getAssessment() == null) return null;

			int index = section.getAssessment().getSections().indexOf(section);
			if (index == 0) return null;

			return section.getAssessment().getSections().get(index - 1);
		}
	}

	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentSectionImpl.class);

	/** The back pointer to the assessment. */
	protected transient AssessmentImpl assessment = null;;

	protected String description = null;

	protected String id = null;

	protected MyOrdering ordering = new MyOrdering(this);

	protected List<AssessmentQuestionImpl> questions = new ArrayList<AssessmentQuestionImpl>();

	protected Boolean randomQuestionOrdering = null;

	protected String title = null;

	/**
	 * Construct
	 */
	public AssessmentSectionImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	protected AssessmentSectionImpl(AssessmentSectionImpl other)
	{
		this.assessment = other.assessment;
		this.description = other.description;
		this.id = other.id;
		this.randomQuestionOrdering = other.randomQuestionOrdering;
		this.title = other.title;
		setQuestions(other.questions);
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof AssessmentSection)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare? position?
		int compare = getOrdering().getPosition().compareTo(((AssessmentSection) obj).getOrdering().getPosition());

		return compare;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AssessmentSection)) return false;
		if (this == obj) return true;
		return ((AssessmentSection) obj).getId().equals(getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment getAssessment()
	{
		return this.assessment;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription()
	{
		return this.description;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getFirstQuestion()
	{
		if (this.questions.isEmpty()) return null;

		// based on the possibly random order
		return getQuestions().get(0);
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
	public AssessmentQuestion getLastQuestion()
	{
		if (this.questions.isEmpty()) return null;

		// based on the possibly random order
		return getQuestions().get(this.questions.size() - 1);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		return this.questions.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<AssessmentSection> getOrdering()
	{
		return this.ordering;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion getQuestion(String questionId)
	{
		if (questionId == null) return null;

		for (AssessmentQuestion question : this.questions)
		{
			if (question.getId().equals(questionId)) return question;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentQuestion> getQuestions()
	{
		// copy the questions
		List<AssessmentQuestionImpl> rv = new ArrayList<AssessmentQuestionImpl>(this.questions);

		// randomize if needed
		if ((this.randomQuestionOrdering != null) && (this.randomQuestionOrdering.booleanValue()))
		{
			// set the seed based on the current user id
			long seed = this.assessment.service.m_sessionManager.getCurrentSessionUserId().hashCode();

			// mix up the questions
			Collections.shuffle(rv, new Random(seed));
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AssessmentQuestion> getQuestionsAsAuthored()
	{
		return this.questions;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getRandomQuestionOrder()
	{
		return this.getRandomQuestionOrder();
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
	public Float getTotalPoints()
	{
		float total = 0;
		for (AssessmentQuestion question : this.questions)
		{
			total += question.getPoints();
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
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestions(List<? extends AssessmentQuestion> questions)
	{
		this.questions.clear();
		if (questions == null) return;

		// deep copy
		this.questions = new ArrayList<AssessmentQuestionImpl>();
		for (AssessmentQuestion question : questions)
		{
			AssessmentQuestionImpl copy = new AssessmentQuestionImpl((AssessmentQuestionImpl) question);
			copy.initSection(this);

			this.questions.add(copy);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRandomQuestionOrder(Boolean setting)
	{
		this.randomQuestionOrdering = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * Establish the link to the containing assessment.
	 * 
	 * @param assessment
	 *        The containing assessment.
	 */
	protected void initAssement(AssessmentImpl assessment)
	{
		this.assessment = assessment;
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
	 * Initialize the questions.
	 * 
	 * @param questions
	 *        The questions - these are taken exactly, not deep copied.
	 */
	protected void initQuestions(List<AssessmentQuestionImpl> questions)
	{
		this.questions = questions;

		// set the back link
		for (AssessmentQuestionImpl question : this.questions)
		{
			question.initSection(this);
		}
	}
}
