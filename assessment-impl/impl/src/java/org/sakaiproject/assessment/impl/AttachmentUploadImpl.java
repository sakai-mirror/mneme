/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Attachment;
import org.sakaiproject.assessment.api.AttachmentUpload;
import org.sakaiproject.assessment.api.SubmissionAnswer;

/**
 * <p>
 * AttachmentUploadImpl is ...
 * </p>
 */
public class AttachmentUploadImpl implements AttachmentUpload
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AttachmentUploadImpl.class);

	protected AttachmentServiceImpl service = null;

	protected SubmissionAnswerImpl answer = null;

	public AttachmentUploadImpl(AttachmentServiceImpl service, SubmissionAnswer answer)
	{
		this.service = service;
		this.answer = (SubmissionAnswerImpl) answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFile(FileItem file)
	{
		try
		{
			String name = file.getName();
			String type = file.getContentType();
			InputStream body = file.getInputStream();
			long size = file.getSize();
			
			// detect no file selected
			if ((name == null) || (type == null) || (body == null) || (size == 0)) return;

			Attachment a = new AttachmentImpl(null, size, name, null, type);
			String id = this.service.putAttachment(a, body, this.answer.id);

			// add an entry to the answer with this attachment
			SubmissionAnswerEntryImpl sample = answer.entries.get(0);
			SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl(sample);
			entry.setAssessmentAnswer(null);
			entry.setAnswerText(null);
			entry.initId(null);
			entry.initAutoScore(new Float(0));

			String refStr = this.service.getAttachmentReference(answer.getSubmission().getId(), id);
			entry.setAnswerText(refStr);

			entry.initAnswer(answer);
			answer.entries.add(entry);
		}
		catch (IOException e)
		{
			M_log.warn("setFile: " + e);
		}
	}
}
