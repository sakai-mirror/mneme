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

import org.muse.mneme.api.Attribution;
import org.muse.mneme.api.Ordering;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Presentation;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.TypeSpecificQuestion;

/**
 * QuestionImpl implements Question
 */
public class QuestionImpl implements Question
{
	public class MyAssessmentOrdering implements Ordering<Question>
	{
		protected QuestionImpl question = null;

		public MyAssessmentOrdering(QuestionImpl question)
		{
			this.question = question;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (question.partContext == null) return true;

			if (!question.getPart().getOrdering().getIsFirst()) return false;

			return question.getPartOrdering().getIsFirst();
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (question.partContext == null) return true;

			if (!question.getPart().getOrdering().getIsLast()) return false;

			return question.getPartOrdering().getIsLast();
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getNext()
		{
			if (question.partContext == null) return null;

			Question rv = question.getPartOrdering().getNext();
			if (rv != null) return rv;

			Part part = question.getPart().getOrdering().getNext();
			if (part == null) return null;

			return part.getFirstQuestion();
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (question.partContext == null) return new Integer(1);

			// position in this part
			int pos = question.getPartOrdering().getPosition();

			// count up questions in preceeding parts
			for (Part part : question.getPart().getAssessment().getParts().getParts())
			{
				if (part.equals(question.partContext)) break;
				pos += part.getNumQuestions();
			}

			return pos;
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getPrevious()
		{
			if (question.partContext == null) return null;

			Question rv = question.getPartOrdering().getPrevious();
			if (rv != null) return rv;

			Part part = question.getPart().getOrdering().getPrevious();
			if (part == null) return null;

			return part.getLastQuestion();
		}
	}

	public class MyPartOrdering implements Ordering<Question>
	{
		protected QuestionImpl question = null;

		public MyPartOrdering(QuestionImpl question)
		{
			this.question = question;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsFirst()
		{
			if (question.partContext == null) return true;

			List<String> questions = ((PartImpl) question.getPart()).getQuestionOrder();
			if (question.getId().equals(questions.get(0))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (question.partContext == null) return true;

			List<String> questions = ((PartImpl) question.getPart()).getQuestionOrder();
			if (question.getId().equals(questions.get(questions.size() - 1))) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getNext()
		{
			if (question.partContext == null) return null;

			List<String> questions = ((PartImpl) question.getPart()).getQuestionOrder();
			int index = questions.indexOf(question.getId());
			if (index == questions.size() - 1) return null;

			return question.questionService.getQuestion(questions.get(index + 1));
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (question.partContext == null) return new Integer(1);

			List<String> questions = ((PartImpl) question.getPart()).getQuestionOrder();
			int index = questions.indexOf(question.getId());

			return index + 1;
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getPrevious()
		{
			if (question.partContext == null) return null;

			List<String> questions = ((PartImpl) question.getPart()).getQuestionOrder();
			int index = questions.indexOf(question.getId());
			if (index == 0) return null;

			return question.questionService.getQuestion(questions.get(index - 1));
		}
	}

	protected MyAssessmentOrdering assessmentOrdering = new MyAssessmentOrdering(this);

	protected AttributionImpl createdBy = new AttributionImpl();

	protected String description = null;

	protected String id = null;

	protected AttributionImpl modifiedBy = new AttributionImpl();

	protected Part partContext = null;

	protected MyPartOrdering partOrdering = new MyPartOrdering(this);

	protected String poolId = null;

	protected transient PoolService poolService = null;

	protected PresentationImpl presentation = new PresentationImpl();

	protected TypeSpecificQuestion questionHandler = null;

	protected transient QuestionService questionService = null;

	protected Boolean requireRationale = null;

	protected Submission submissionContext = null;

	protected String type = null;

	protected String version = "only";

	/**
	 * Construct.
	 * 
	 * @param poolService
	 *        the PoolService.
	 * @param questionService
	 *        The QuestionService.
	 */
	public QuestionImpl(PoolService poolService, QuestionService questionService)
	{
		this.poolService = poolService;
		this.questionService = questionService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public QuestionImpl(QuestionImpl other)
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
		if ((this.id == null) || (((QuestionImpl) obj).id == null)) return false;
		return this.id.equals(((QuestionImpl) obj).id);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAnswerKey()
	{
		if (this.questionHandler == null) return null;
		return this.questionHandler.getAnswerKey();
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<Question> getAssessmentOrdering()
	{
		return this.assessmentOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getCreatedBy()
	{
		return this.createdBy;
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
	public String getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attribution getModifiedBy()
	{
		return this.modifiedBy;
	}

	/**
	 * {@inheritDoc}
	 */
	public Part getPart()
	{
		return this.partContext;
	}

	/**
	 * {@inheritDoc}
	 */
	public Ordering<Question> getPartOrdering()
	{
		return this.partOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pool getPool()
	{
		return this.poolService.getPool(this.poolId);
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
	public Boolean getRequireRationale()
	{
		return this.requireRationale;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType()
	{
		return this.type;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getTypeName()
	{
		return this.questionHandler.getTypeName();
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificQuestion getTypeSpecificQuestion()
	{
		return this.questionHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getVersion()
	{
		return this.version;
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
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPool(Pool pool)
	{
		if (pool == null) throw new IllegalArgumentException();
		this.poolId = pool.getId();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRequireRationale(Boolean rationale)
	{
		if (rationale == null) throw new IllegalArgumentException();
		this.requireRationale = rationale;
	}

	/**
	 * Initialize the id.
	 * 
	 * @param id
	 *        The id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Initialize the part context for this question - the part this question instance was created to support.
	 * 
	 * @param part
	 *        The Part.
	 */
	protected void initPartContext(Part part)
	{
		this.partContext = part;
	}

	/**
	 * Initialize the submission context for this question - the submission this question instance was created to support.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void initSubmissionContext(Submission submission)
	{
		this.submissionContext = submission;
	}

	/**
	 * Establish the type.
	 * 
	 * @param type
	 *        The type.
	 */
	protected void initType(String type)
	{
		this.type = type;
	}

	/**
	 * Establish the type-specific question handler.
	 * 
	 * @param questionHandler
	 *        The type-specific question handler.
	 */
	protected void initTypeSpecificQuestion(TypeSpecificQuestion questionHandler)
	{
		this.questionHandler = questionHandler;
	}

	protected void set(QuestionImpl other)
	{
		if (other.questionHandler != null) this.questionHandler = (TypeSpecificQuestion) (other.questionHandler.clone());
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy);
		this.description = other.description;
		this.id = other.id;
		this.modifiedBy = new AttributionImpl((AttributionImpl) other.modifiedBy);
		this.partContext = other.partContext;
		this.poolId = other.poolId;
		this.poolService = other.poolService;
		this.presentation = new PresentationImpl(other.presentation);
		this.requireRationale = other.requireRationale;
		this.questionService = other.questionService;
		this.submissionContext = other.submissionContext;
		this.type = other.type;
		this.version = other.version;
	}
}
