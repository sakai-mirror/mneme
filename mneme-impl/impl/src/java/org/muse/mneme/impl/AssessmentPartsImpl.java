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

import org.muse.mneme.api.AssessmentParts;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;

/**
 * AssessmentPartsImpl implements AssessmentParts
 */
public class AssessmentPartsImpl implements AssessmentParts
{
	protected Boolean continuousNumbering = Boolean.FALSE;

	protected List<Part> parts = new ArrayList<Part>();

	protected PoolService poolService = null;

	protected QuestionService questionService = null;

	protected Boolean showPresentation = Boolean.FALSE;

	protected transient AssessmentImpl assessment = null;

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AssessmentPartsImpl(AssessmentImpl assessment, AssessmentPartsImpl other)
	{
		set(assessment, other);
	}

	/**
	 * Construct.
	 * 
	 * @param assessment
	 *        The assessment this is the parts for.
	 * @param assessmentService
	 *        The AssessmentService.
	 * @param questionService
	 *        The QuestionService.
	 * @param poolService
	 *        The PoolService.
	 */
	public AssessmentPartsImpl(AssessmentImpl assessment, QuestionService questionService, PoolService poolService)
	{
		this.assessment = assessment;
		this.questionService = questionService;
		this.poolService = poolService;
	}

	/**
	 * {@inheritDoc}
	 */
	public DrawPart addDrawPart()
	{
		// create the new part
		DrawPart rv = new DrawPartImpl(this.assessment, this.questionService, this.poolService);

		// add it to the list
		this.parts.add(rv);
		((PartImpl) rv).initContainer(this.parts);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public ManualPart addManualPart()
	{
		// create the new part
		ManualPart rv = new ManualPartImpl(this.assessment, this.questionService);

		// add it to the list
		this.parts.add(rv);
		((PartImpl) rv).initContainer(this.parts);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getContinuousNumbering()
	{
		return this.continuousNumbering;
	}

	/**
	 * {@inheritDoc}
	 */
	public Part getFirst()
	{
		return this.parts.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getNumQuestions()
	{
		int rv = 0;
		for (Part part : this.parts)
		{
			rv += part.getNumQuestions();
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Part getPart(String id)
	{
		if (id == null) throw new IllegalArgumentException();
		for (Part part : this.parts)
		{
			if (part.getId().equals(id)) return part;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends Part> getParts()
	{
		return this.parts;
	}

	/**
	 * {@inheritDoc}
	 */
	public Question getQuestion(String questionId)
	{
		if (questionId == null) throw new IllegalArgumentException();
		for (Part part : this.parts)
		{
			Question question = part.getQuestion(questionId);
			if (question != null) return question;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends Question> getQuestions()
	{
		List<Question> rv = new ArrayList<Question>();
		for (Part part : this.parts)
		{
			rv.addAll(part.getQuestions());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getShowPresentation()
	{
		return this.showPresentation;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getSize()
	{
		return this.parts.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getTotalPoints()
	{
		float rv = 0f;
		for (Part part : this.parts)
		{
			rv += part.getTotalPoints();
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePart(Part part)
	{
		this.parts.remove(part);
		((PartImpl) part).initContainer(null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContinuousNumbering(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.continuousNumbering = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setShowPresentation(Boolean setting)
	{
		if (setting == null) throw new IllegalArgumentException();
		this.showPresentation = setting;
	}

	/**
	 * Set as a copy of another (deep copy).
	 * 
	 * @param other
	 *        The other to copy.
	 */
	protected void set(AssessmentImpl assessment, AssessmentPartsImpl other)
	{
		this.assessment = assessment;
		this.continuousNumbering = other.continuousNumbering;
		this.parts = new ArrayList<Part>(other.parts.size());
		this.showPresentation = other.showPresentation;
		this.questionService = other.questionService;
		this.poolService = other.poolService;

		for (Part part : other.parts)
		{
			if (part instanceof ManualPartImpl)
			{
				PartImpl newPart = new ManualPartImpl((ManualPartImpl) part);
				newPart.initContainer(this.parts);
				newPart.initAssessment(this.assessment);
				this.parts.add(newPart);
			}
			else if (part instanceof DrawPartImpl)
			{
				PartImpl newPart = new DrawPartImpl((DrawPartImpl) part);
				newPart.initContainer(this.parts);
				newPart.initAssessment(this.assessment);
				this.parts.add(newPart);
			}

			// TODO: else?
		}
	}
}