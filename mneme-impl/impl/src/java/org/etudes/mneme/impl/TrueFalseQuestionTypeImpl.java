/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/etudes/mneme/branches/mn-1166/mneme-impl/impl/src/java/org/etudes/mneme/impl/TrueFalseQuestionTypeImpl.java $
 * $Id: TrueFalseQuestionTypeImpl.java 67735 2010-05-20 15:49:14Z ggolden@etudes.org $
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

import org.etudes.mneme.api.TrueFalseQuestion;

/**
 * TrueFalseQuestionImpl handles questions for the true false question type.
 */
public class TrueFalseQuestionTypeImpl extends QuestionImpl implements TrueFalseQuestion
{
	/**
	 * {@inheritDoc}
	 */
	public boolean getCorrectAnswer()
	{
		return Boolean.parseBoolean(((TrueFalseQuestionImpl) getTypeSpecificQuestion()).getCorrectAnswer());
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCorrectAnswer(boolean correctAnswer)
	{
		((TrueFalseQuestionImpl) getTypeSpecificQuestion()).setCorrectAnswer(String.valueOf(correctAnswer));
	}
}
