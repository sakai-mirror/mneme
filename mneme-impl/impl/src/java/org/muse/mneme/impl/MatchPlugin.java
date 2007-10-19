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
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.ResourceLoader;

/**
 * MatchPlugin handles the match question type.
 */
public class MatchPlugin implements QuestionPlugin
{
	private static Log M_log = LogFactory.getLog(MatchPlugin.class);

	/** Messages bundle name. */
	protected String bundle = null;

	/** Localized messages. */
	protected InternationalizedMessages messages = null;

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
	public Integer getPopularity()
	{
		return Integer.valueOf(50);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType()
	{
		return "mneme:Match";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTypeName()
	{
		return this.messages.getString("name");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// messages
		if (this.bundle != null) this.messages = new ResourceLoader(this.bundle);

		// register with Mneme as a question plugin
		this.mnemeService.registerQuestionPlugin(this);

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificAnswer newAnswer(Answer answer)
	{
		return new MatchAnswerImpl(answer);
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeSpecificQuestion newQuestion(Question question)
	{
		return new MatchQuestionImpl(this, this.messages, this.uiService, question);
	}

	/**
	 * Set the message bundle.
	 * 
	 * @param bundle
	 *        The message bundle.
	 */
	public void setBundle(String name)
	{
		this.bundle = name;
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
