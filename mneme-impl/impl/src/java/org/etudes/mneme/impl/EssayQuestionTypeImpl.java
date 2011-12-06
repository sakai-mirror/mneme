/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/etudes/mneme/branches/mn-1166/mneme-impl/impl/src/java/org/etudes/mneme/impl/EssayQuestionTypeImpl.java $
 * $Id: EssayQuestionTypeImpl.java 67735 2010-05-20 15:49:14Z ggolden@etudes.org $
 ***********************************************************************************
 *
 * Copyright (c) 2011 Etudes, Inc.
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

package org.etudes.mneme.impl;

import org.etudes.mneme.api.EssayQuestion;
import org.etudes.mneme.api.EssayQuestion.SubmissionType;

/**
 * EssayQuestionImpl handles questions for the essay question type
 */
public class EssayQuestionTypeImpl extends QuestionImpl implements EssayQuestion
{
	/**
	 * {@inheritDoc}
	 */
	public String getModelAnswer()
	{
		return ((EssayQuestionImpl) getTypeSpecificQuestion()).getModelAnswer();
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionType getSubmissionType()
	{
		switch (((EssayQuestionImpl) getTypeSpecificQuestion()).getSubmissionType())
		{
			case attachments:
			{
				return SubmissionType.attachments;
			}
			case both:
			{
				return SubmissionType.both;
			}
			case inline:
			{
				return SubmissionType.inline;
			}
			case none:
			{
				return SubmissionType.none;
			}
		}
		return SubmissionType.none;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModelAnswer(String modelAnswer)
	{
		((EssayQuestionImpl) getTypeSpecificQuestion()).setModelAnswer(modelAnswer);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionType(SubmissionType setting)
	{
		switch (setting)
		{
			case attachments:
			{
				((EssayQuestionImpl) getTypeSpecificQuestion()).setSubmissionType(EssayQuestionImpl.SubmissionType.attachments);
				break;
			}
			case both:
			{
				((EssayQuestionImpl) getTypeSpecificQuestion()).setSubmissionType(EssayQuestionImpl.SubmissionType.both);
				break;
			}
			case inline:
			{
				((EssayQuestionImpl) getTypeSpecificQuestion()).setSubmissionType(EssayQuestionImpl.SubmissionType.inline);
				break;
			}
			case none:
			{
				((EssayQuestionImpl) getTypeSpecificQuestion()).setSubmissionType(EssayQuestionImpl.SubmissionType.none);
				break;
			}
		}
	}
}
