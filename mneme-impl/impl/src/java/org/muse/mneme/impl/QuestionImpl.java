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
import org.muse.mneme.api.SubmissionService;
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

			List<PoolPick> questions = ((PartImpl) question.getPart()).getQuestionPickOrder();
			if (question.getId().equals(questions.get(0).getQuestionId())) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Boolean getIsLast()
		{
			if (question.partContext == null) return true;

			List<PoolPick> questions = ((PartImpl) question.getPart()).getQuestionPickOrder();
			if (question.getId().equals(questions.get(questions.size() - 1).getQuestionId())) return true;

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getNext()
		{
			if (question.partContext == null) return null;

			List<PoolPick> questions = ((PartImpl) question.getPart()).getQuestionPickOrder();
			int index = 0;
			for (PoolPick pick : questions)
			{
				if (pick.getQuestionId().equals(question.getId()))
				{
					break;
				}
				index++;
			}
			if (index == questions.size() - 1) return null;

			// TODO: set the question context (pool? from question?)
			return question.questionService.getQuestion(questions.get(index + 1).getQuestionId());
		}

		/**
		 * {@inheritDoc}
		 */
		public Integer getPosition()
		{
			if (question.partContext == null) return new Integer(1);

			List<PoolPick> questions = ((PartImpl) question.getPart()).getQuestionPickOrder();
			int index = 0;
			for (PoolPick pick : questions)
			{
				if (pick.getQuestionId().equals(question.getId()))
				{
					break;
				}
				index++;
			}

			return index + 1;
		}

		/**
		 * {@inheritDoc}
		 */
		public Question getPrevious()
		{
			if (question.partContext == null) return null;

			List<PoolPick> questions = ((PartImpl) question.getPart()).getQuestionPickOrder();
			int index = 0;
			for (PoolPick pick : questions)
			{
				if (pick.getQuestionId().equals(question.getId()))
				{
					break;
				}
				index++;
			}
			if (index == 0) return null;

			// TODO: set context (pool? from question?)
			return question.questionService.getQuestion(questions.get(index - 1).getQuestionId());
		}
	}

	protected MyAssessmentOrdering assessmentOrdering = new MyAssessmentOrdering(this);

	/** Track changes. */
	protected transient ChangeableImpl changed = new ChangeableImpl();

	/** Used only when historical and there's no pool to point to. */
	protected String context = null;

	protected AttributionImpl createdBy = new AttributionImpl(null);

	protected Boolean explainReason = Boolean.FALSE;

	protected String feedback = null;

	protected String hints = null;

	protected Boolean historical = Boolean.FALSE;

	protected String id = null;

	/** Stays TRUE until an end-user change to the object occurs, showing it was actually initially set. */
	protected Boolean mint = Boolean.TRUE;

	protected AttributionImpl modifiedBy = new AttributionImpl(null);

	protected transient Part partContext = null;

	protected MyPartOrdering partOrdering = new MyPartOrdering(this);

	protected transient String poolContext = null;

	protected String poolId = null;

	protected transient PoolService poolService = null;

	protected PresentationImpl presentation = null;

	protected TypeSpecificQuestion questionHandler = null;

	protected transient QuestionService questionService = null;

	protected transient Submission submissionContext = null;

	protected transient SubmissionService submissionService = null;

	protected String type = null;

	/**
	 * Construct.
	 * 
	 * @param poolService
	 *        the PoolService.
	 * @param questionService
	 *        The QuestionService.
	 * @param submissionService
	 *        The SubmissionService.
	 */
	public QuestionImpl(PoolService poolService, QuestionService questionService, SubmissionService submissionService)
	{
		this.poolService = poolService;
		this.questionService = questionService;
		this.submissionService = submissionService;
		this.presentation = new PresentationImpl(this.changed);
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
	public Ordering<Question> getAssessmentOrdering()
	{
		return this.assessmentOrdering;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContext()
	{
		if (this.getIsHistorical())
		{
			return this.context;
		}

		return this.getPool().getContext();
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
		return this.questionHandler.getDescription();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getExplainReason()
	{
		return this.explainReason;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFeedback()
	{
		return this.feedback;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getHasUnscoredSubmissions()
	{
		if (this.partContext != null)
		{
			return this.submissionService.getAssessmentQuestionHasUnscoredSubmissions(this.partContext.getAssessment(), this);
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getHints()
	{
		return this.hints;
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
	public Boolean getIsChanged()
	{
		return this.changed.getChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsHistorical()
	{
		return this.historical;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getMint()
	{
		return this.mint;
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
		// use the pool context if set
		if (this.poolContext != null)
		{
			return this.poolService.getPool(this.poolContext);
		}

		// historical questions live in no pool
		if (this.getIsHistorical())
		{
			return null;
		}

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
	public String getType()
	{
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTypeName()
	{
		return this.questionHandler.getPlugin().getTypeName();
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
	public int hashCode()
	{
		return this.id == null ? "null".hashCode() : this.id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setChanged()
	{
		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExplainReason(Boolean explainReason)
	{
		if (explainReason == null) throw new IllegalArgumentException();
		if (!Different.different(explainReason, this.explainReason)) return;

		this.explainReason = explainReason;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFeedback(String feedback)
	{
		if (!Different.different(feedback, this.feedback)) return;

		this.feedback = feedback;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setHints(String hints)
	{
		if (!Different.different(hints, this.hints)) return;

		this.hints = hints;

		this.changed.setChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPool(Pool pool)
	{
		if (pool == null) throw new IllegalArgumentException();
		if (!Different.different(pool.getId(), this.poolId)) return;

		this.poolId = pool.getId();

		this.changed.setChanged();
	}

	/**
	 * Clear the changed settings.
	 */
	protected void clearChanged()
	{
		this.changed.clearChanged();
	}

	/**
	 * Clear the mint setting.
	 */
	protected void clearMint()
	{
		this.mint = Boolean.FALSE;
	}

	/**
	 * Initialize the context.
	 * 
	 * @param context
	 *        The context.
	 */
	protected void initContext(String context)
	{
		this.context = context;
	}

	/**
	 * Establish the historical flag.
	 * 
	 * @param historical
	 *        The historical flag.
	 */
	protected void initHistorical(Boolean historical)
	{
		this.historical = historical;
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
	 * Establish the mint flag.
	 * 
	 * @param mint
	 *        The mint flag.
	 */
	protected void initMint(Boolean mint)
	{
		this.mint = mint;
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
	 * Establish the pool id.
	 * 
	 * @param poolId
	 *        The pool id.
	 */
	protected void initPool(String poolId)
	{
		this.poolId = poolId;
	}

	/**
	 * Initialize the pool context for this question.
	 * 
	 * @param poolId
	 *        The pool id.
	 */
	protected void initPoolContext(String poolId)
	{
		this.poolContext = poolId;
	}

	/**
	 * Initialize the Pool id.
	 * 
	 * @param id
	 *        The pool id.
	 */
	protected void initPoolId(String id)
	{
		this.poolId = id;
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

	/**
	 * Set this question to be historical; pick up a context, drop the pool reference.
	 */
	protected void setHistorical()
	{
		if (this.historical) return;

		// set the context to the pool's context
		this.context = this.getPool().getContext();

		// forget our original pool
		this.poolId = null;

		this.historical = Boolean.TRUE;
	}

	protected void set(QuestionImpl other)
	{
		if (other.questionHandler != null) this.questionHandler = (TypeSpecificQuestion) (other.questionHandler.clone(this));
		this.createdBy = new AttributionImpl((AttributionImpl) other.createdBy, null);
		this.context = other.context;
		this.explainReason = other.explainReason;
		this.feedback = other.feedback;
		this.hints = other.hints;
		this.historical = other.historical;
		this.id = other.id;
		this.mint = other.mint;
		this.modifiedBy = new AttributionImpl((AttributionImpl) other.modifiedBy, null);
		this.partContext = other.partContext;
		this.poolContext = other.poolContext;
		this.poolId = other.poolId;
		this.poolService = other.poolService;
		this.presentation = new PresentationImpl(other.presentation, this.changed);
		this.questionService = other.questionService;
		this.submissionContext = other.submissionContext;
		this.submissionService = other.submissionService;
		this.type = other.type;
	}
}
