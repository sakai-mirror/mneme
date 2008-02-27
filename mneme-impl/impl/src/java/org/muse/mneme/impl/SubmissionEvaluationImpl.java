/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import org.apache.commons.fileupload.FileItem;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionEvaluation;
import org.sakaiproject.entity.api.Reference;

/**
 * SubmissionEvaluationImpl implements SubmissionEvaluation
 */
public class SubmissionEvaluationImpl extends EvaluationImpl implements SubmissionEvaluation
{
	protected AttachmentService attachmentService = null;

	protected Submission submission = null;

	/**
	 * Construct.
	 * 
	 * @param submission
	 *        The submission this applies to.
	 */
	public SubmissionEvaluationImpl(Submission submission, AttachmentService attachmentService)
	{
		super();
		this.submission = submission;
		this.attachmentService = attachmentService;
	}

	/**
	 * Construct.
	 * 
	 * @param other
	 *        The other to copy.
	 */
	public SubmissionEvaluationImpl(SubmissionEvaluationImpl other, Submission submission)
	{
		super();
		this.submission = submission;
		this.attachmentService = other.attachmentService;
		set(other);
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
	public void setUpload(FileItem file)
	{
		Reference reference = this.attachmentService.addAttachment(AttachmentService.MNEME_APPLICATION, getSubmission().getAssessment().getContext(),
				AttachmentService.SUBMISSIONS_AREA, true, file);
		if (reference != null)
		{
			this.attachments.add(reference);
			this.changed.setChanged();
		}
	}
}
