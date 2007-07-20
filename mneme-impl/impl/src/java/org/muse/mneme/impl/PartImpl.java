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
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.Ordering;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * PartImpl implements Part
 */
public abstract class PartImpl implements Part
{
	private static Log M_log = LogFactory.getLog(PartImpl.class);

	public class MyOrdering implements Ordering<Part>
	{
		protected PartImpl part = null;

		protected List<Part> parts = null;

		public MyOrdering(PartImpl part)
		{
			this.part = part;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (parts == null) return true;

			if (part.equals(parts.get(0))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (parts == null) return true;

			if (part.equals(parts.get(parts.size() - 1))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Part getNext()
		{
			if (parts == null) return null;

			int index = parts.indexOf(part);
			if (index == parts.size() - 1) return null;

			return parts.get(index + 1);
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (parts == null) return new Integer(1);

			int index = parts.indexOf(part);

			return index + 1;
		}

		/**
		 * {@inheritDoc}
		 */
		public Part getPrevious()
		{
			if (parts == null) return null;

			int index = parts.indexOf(part);
			if (index == 0) return null;

			return parts.get(index - 1);
		}

		/**
		 * Initialize the parts list that contains this part.
		 * 
		 * @param parts
		 *        The parts list that contains this part.
		 */
		protected void initParts(List<Part> parts)
		{
			this.parts = parts;
		}
	}

	protected transient AssessmentImpl assessment = null;

	protected String id = null;

	protected MyOrdering ordering = new MyOrdering(this);

	protected PresentationImpl presentation = new PresentationImpl();

	protected QuestionService questionService = null;

	protected String title = null;

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param questionService
	 *        The QuestionService.
	 */
	public PartImpl(AssessmentImpl assessment, QuestionService questionService)
	{
		this.assessment = assessment;
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PartImpl(PartImpl other)
	{
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		// two PartImpls are equals if they have the same id
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		return this.id.equals(((PartImpl) obj).id);
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
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<Part> getOrdering()
	{
		return this.ordering;
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
	public Question getQuestion(String questionId)
	{
		// get the actual list of questions
		List<String> questions = getQuestionOrder();

		// make sure this is one of our questions
		if (!questions.contains(questionId)) return null;

		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(questionId);
		if (question == null)
		{
			M_log.warn("getQuestion: question not defined: " + questionId);
			return null;
		}

		// set the assessment, part and submission context
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);

		return question;
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
	public int hashCode()
	{
		return this.id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * Get the list of questions as they should be presented for the submission context.
	 * 
	 * @return The list of questions as they should be presented for the submission context.
	 */
	protected abstract List<String> getQuestionOrder();

	/**
	 * Establish the assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void initAssessment(AssessmentImpl assessment)
	{
		this.assessment = assessment;
	}

	/**
	 * Set the parts list that contains this part.
	 * 
	 * @param parts
	 *        The parts list that contains this part.
	 */
	protected void initContainer(List<Part> parts)
	{
		this.ordering.initParts(parts);
	}

	/**
	 * Establish the id.
	 * 
	 * @param id
	 *        The part id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PartImpl other)
	{
		this.assessment = other.assessment;
		this.id = other.id;
		this.presentation = new PresentationImpl(other.presentation);
		this.questionService = other.questionService;
		this.title = other.title;
	}
}
