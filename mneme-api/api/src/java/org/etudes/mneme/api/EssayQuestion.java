/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/etudes/mneme/branches/mn-1166/mneme-api/api/src/java/org/etudes/mneme/api/EssayQuestion.java $
 * $Id: EssayQuestion.java 65738 2010-01-14 17:41:22Z ggolden@etudes.org $
 ***********************************************************************************
 *
 * Copyright (c) 2011 Etudes, Inc.
 * 
 * Portions completed before September 1, 2008
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

package org.etudes.mneme.api;

/**
 * EssayQuestion handles questions for the essay question type
 */
public interface EssayQuestion extends Question
{
	/** An enumerate type that declares the types of submissions */
	public enum SubmissionType
	{
		attachments, both, inline, none;
	}

	/**
	 * @return the modelAnswer (rich text)
	 */
	public String getModelAnswer();

	/**
	 * @return the submission type
	 */
	public SubmissionType getSubmissionType();

	/**
	 * Set the model answer.
	 * 
	 * @param modelAnswer
	 *        The model answer. Must be well formed HTML or plain text.
	 */
	public void setModelAnswer(String modelAnswer);

	/**
	 * Set the submission type
	 * 
	 * @param setting
	 *        The submission type.
	 */
	public void setSubmissionType(SubmissionType setting);
}
