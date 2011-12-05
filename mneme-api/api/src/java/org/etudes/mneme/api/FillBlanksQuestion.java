/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/etudes/mneme/branches/mn-1166/mneme-api/api/src/java/org/etudes/mneme/api/FillBlanksQuestion.java $
 * $Id: FillBlanksQuestion.java 65738 2010-01-14 17:41:22Z ggolden@etudes.org $
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
 * FillBlanksQuestion handles questions for the fill blanks question type.
 */
public interface FillBlanksQuestion extends Question
{
	/**
	 * Access order information.
	 * 
	 * @return The ordering.
	 */
	public String getAnyOrder();
	
	/**
	 * Access if case sensitive.
	 * 
	 * @return The case sensitive value.
	 */
	public String getCaseSensitive();
	
	/**
	 * Access if textual/numeric response.
	 * 
	 * @return The value of response.
	 */
	public String getResponseTextual();
	
	/**
	 * Access the question text.
	 * 
	 * @return The question text.
	 */
	public String getText();

	/**
	 * Set the ordering.
	 * 
	 * @param anyOrder
	 *        The ordering.
	 */
	public void setAnyOrder(String anyOrder);

	/**
	 * Set the case sensitive value.
	 * 
	 * @param caseSensitive
	 *        The case sensitive value.
	 */
	public void setCaseSensitive(String caseSensitive);
	
	/**
	 * Set the response type(textual/numeric).
	 * 
	 * @param responseTextual
	 *        The response type.
	 */
	public void setResponseTextual(String responseTextual);

	/**
	 * Set the question text.
	 * 
	 * @param text
	 *        The question text.
	 */
	public void setText(String text);
}
