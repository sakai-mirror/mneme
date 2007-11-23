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

import org.muse.mneme.api.Answer;
import org.muse.mneme.api.AttachmentService;
import org.sakaiproject.entity.api.Reference;

/**
 * TaskAnswerImpl handles answers for the Task question type.
 */
public class TaskAnswerImpl extends EssayAnswerImpl
{
	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 * @param attachmentService
	 *        The AttachmentService dependency.
	 */
	public TaskAnswerImpl(Answer answer, AttachmentService attachmentService)
	{
		super(answer, attachmentService);
	}

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 * @param other
	 *        The other to copy.
	 */
	public TaskAnswerImpl(Answer answer, TaskAnswerImpl other)
	{
		super(answer, other);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone(Answer answer)
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// deep copy
			((TaskAnswerImpl) rv).uploads = new ArrayList<Reference>(this.uploads);

			((TaskAnswerImpl) rv).answer = answer;

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}
}
