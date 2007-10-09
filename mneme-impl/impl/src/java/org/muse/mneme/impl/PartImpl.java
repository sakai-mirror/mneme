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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.Changeable;
import org.muse.mneme.api.Ordering;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * PartImpl implements Part
 */
public abstract class PartImpl implements Part
{
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

	private static Log M_log = LogFactory.getLog(PartImpl.class);

	protected transient AssessmentImpl assessment = null;

	protected String id = null;

	protected MyOrdering ordering = new MyOrdering(this);

	protected transient Changeable owner = null;

	protected PresentationImpl presentation = null;

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
	public PartImpl(AssessmentImpl assessment, QuestionService questionService, Changeable owner)
	{
		this.owner = owner;
		this.assessment = assessment;
		this.questionService = questionService;
		this.presentation = new PresentationImpl(this.owner);
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public PartImpl(PartImpl other, AssessmentImpl assessment, Changeable owner)
	{
		this.owner = owner;
		this.assessment = assessment;
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
		if ((this.id == null) || (((PartImpl) obj).id == null)) return false;
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
		// get the actual list of question picks
		List<PoolPick> questions = getQuestionPickOrder();

		// make sure this is one of our questions
		PoolPick found = null;
		for (PoolPick pick : questions)
		{
			if (pick.getQuestionId().equals(questionId))
			{
				found = pick;
				break;
			}
		}
		if (found == null) return null;

		QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(questionId);
		if (question == null)
		{
			M_log.warn("getQuestion: question not defined: " + questionId);
			return null;
		}

		// set the question contexts
		question.initSubmissionContext(this.assessment.getSubmissionContext());
		question.initPartContext(this);
		question.initPoolContext(found.getPoolId());

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Question> getQuestions()
	{
		List<PoolPick> order = getQuestionPickOrder();
		List<Question> rv = new ArrayList<Question>(order.size());
		for (PoolPick pick : order)
		{
			QuestionImpl question = (QuestionImpl) this.questionService.getQuestion(pick.getQuestionId());
			if (question != null)
			{
				// set the question contexts
				question.initSubmissionContext(this.assessment.getSubmissionContext());
				question.initPartContext(this);
				question.initPoolContext(pick.getPoolId());

				rv.add(question);
			}
		}

		return rv;
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
		return this.id == null ? "null".hashCode() : this.id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTitle(String title)
	{
		if (!Different.different(this.title, title)) return;

		this.title = title;

		this.owner.setChanged();
	}

	/**
	 * Check if the part depends on this pool.
	 * 
	 * @param pool
	 *        The pool.
	 * @param directOnly
	 *        If true, check only direct dependencies, else check direct or indirect.
	 * @return TRUE if the part depends on this pool, FALSE if not.
	 */
	protected abstract Boolean dependsOn(Pool pool, boolean directOnly);

	/**
	 * Check if the part depends on this question directly.
	 * 
	 * @param question
	 *        The question.
	 * @return TRUE if the part depends on this question directly, FALSE if not.
	 */
	protected abstract Boolean dependsOn(Question question);

	/**
	 * Get the list of question picks as they should be presented for the submission context.
	 * 
	 * @return The list of question picks as they should be presented for the submission context.
	 */
	protected abstract List<PoolPick> getQuestionPickOrder();

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
	 * Compute a seed based on the submission or part for randomization.
	 * 
	 * @return The seed based on the submission or part for randomization.
	 */
	protected long seed()
	{
		// set the seed based on the id of the submission context,
		// so each submission has a different unique ordering,
		// and the part id, so the randomization of questions in each part within the same submission differs
		long seed = 0;
		if (this.assessment.getSubmissionContext() != null)
		{
			seed = (this.assessment.getSubmissionContext().getId() + "_" + this.id).hashCode();
		}

		// if no submission context, just the part id
		else
		{
			seed = this.id.hashCode();
		}

		return seed;
	}

	/**
	 * Set as a copy of another.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(PartImpl other)
	{
		this.id = other.id;
		this.presentation = new PresentationImpl(other.presentation, this.owner);
		this.questionService = other.questionService;
		this.title = other.title;
	}

	/**
	 * Change any part references that are from to to.
	 * 
	 * @param from
	 *        The from pool.
	 * @param to
	 *        The to pool.
	 * @param directOnly
	 *        if true, switch only for direct (draw) dependencies, else switch those as well as (manual) question dependencies.
	 */
	protected abstract void switchPool(Pool from, Pool to, boolean directOnly);
}
