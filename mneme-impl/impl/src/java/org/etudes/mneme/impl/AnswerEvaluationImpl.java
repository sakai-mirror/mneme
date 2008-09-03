/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Portions completed before September 1, 2008 Copyright (c) 2007, 2008 Sakai Foundation,
 * licensed under the Educational Community License, Version 2.0
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.mneme.impl;

import org.apache.commons.fileupload.FileItem;
import org.etudes.mneme.api.Answer;
import org.etudes.mneme.api.AnswerEvaluation;
import org.etudes.mneme.api.AttachmentService;
import org.sakaiproject.entity.api.Reference;

/**
 * AnswerEvaluationImpl implements AnswerEvaluation
 */
public class AnswerEvaluationImpl extends EvaluationImpl implements AnswerEvaluation
{
	/** The answer this applies to. */
	protected Answer answer = null;

	protected AttachmentService attachmentService = null;

	/**
	 * Construct
	 * 
	 * @param answer
	 *        The answer this applies to.
	 */
	public AnswerEvaluationImpl(Answer answer, AttachmentService attachmentService)
	{
		super();
		this.answer = answer;
		this.attachmentService = attachmentService;
	}

	/**
	 * Construct as a copy of other.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public AnswerEvaluationImpl(AnswerEvaluationImpl other)
	{
		super();
		this.answer = other.answer;
		this.attachmentService = other.attachmentService;
		set(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Answer getAnswer()
	{
		return this.answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUpload(FileItem file)
	{
		Reference reference = this.attachmentService.addAttachment(AttachmentService.MNEME_APPLICATION, getAnswer().getSubmission().getAssessment()
				.getContext(), AttachmentService.SUBMISSIONS_AREA + "/" + getAnswer().getSubmission().getId(), true, file);
		if (reference != null)
		{
			this.attachments.add(reference);
			this.changed.setChanged();
		}
	}
}