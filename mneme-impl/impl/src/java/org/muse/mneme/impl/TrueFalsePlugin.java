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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.UiService;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.TypeSpecificAnswer;
import org.muse.mneme.api.TypeSpecificQuestion;

/**
 * TrueFalsePlugin handles the true/false question type.
 */
public class TrueFalsePlugin implements QuestionPlugin
{
	private static Log M_log = LogFactory.getLog(TrueFalsePlugin.class);

	protected MnemeService mnemeService = null;

	/** Dependency: The UI service (Ambrosia). */
	protected UiService uiService = null;

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType()
	{
		return "mneme:TrueFalse";
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// register with Mneme as a question plugin
		this.mnemeService.registerQuestionPlugin(this);

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificAnswer newAnswer(Answer answer)
	{
		return new TrueFalseAnswerImpl(answer);
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificQuestion newQuestion(Question question)
	{
		return new TrueFalseQuestionImpl(uiService, question);
	}

	/**
	 * Dependency: MnemeService.
	 * 
	 * @param service
	 *        The MnemeService.
	 */
	public void setMnemeService(MnemeService service)
	{
		this.mnemeService = service;
	}

	/**
	 * Set the UI service.
	 * 
	 * @param service
	 *        The UI service.
	 */
	public void setUi(UiService service)
	{
		this.uiService = service;
	}

}
