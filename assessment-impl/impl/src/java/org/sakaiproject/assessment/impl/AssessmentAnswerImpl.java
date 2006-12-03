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

import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.QuestionPart;

public class AssessmentAnswerImpl implements AssessmentAnswer
{
	protected String id = null;

	protected Boolean isCorrect = null;

	protected String label = null;

	/** The back pointer to the assessment question part. */
	protected transient QuestionPartImpl part = null;

	protected String text = null;

	/**
	 * Construct
	 */
	public AssessmentAnswerImpl()
	{
	}

	/**
	 * Construct as a deep copy of another
	 */
	public AssessmentAnswerImpl(AssessmentAnswerImpl other)
	{
		initId(other.getId());
		setIsCorrect(other.getIsCorrect());
		setLabel(other.getLabel());
		this.part = other.part;
		setText(other.getText());
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof AssessmentAnswer)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// TODO: how to compare? sequence?
		int compare = getPosition().compareTo(((AssessmentAnswer) obj).getPosition());

		return compare;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AssessmentAnswer)) return false;
		if (this == obj) return true;
		return ((AssessmentAnswer) obj).getId().equals(getId());
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
	public Boolean getIsCorrect()
	{
		return this.isCorrect;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return this.label;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPart getPart()
	{
		return this.part;
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer getPosition()
	{
		int index = this.part.getAnswers().indexOf(this);

		return new Integer(index + 1);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText()
	{
		return this.text;
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
	public void setIsCorrect(Boolean setting)
	{
		this.isCorrect = setting;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Establish the answer id.
	 * 
	 * @param id
	 *        The answer id.
	 */
	protected void initId(String id)
	{
		this.id = id;
	}

	/**
	 * Establish the link to the containing question part.
	 * 
	 * @param part
	 *        The containing part.
	 */
	protected void initPart(QuestionPartImpl part)
	{
		this.part = part;
	}
}
