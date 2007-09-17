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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.Value;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionPlugin;
import org.muse.mneme.api.QuestionService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /pools view for the mneme tool.
 */
public class SelectQuestionType extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SelectQuestionType.class);
	
	/** Dependency: mneme service. */
	protected MnemeService mnemeService = null;
	
	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (params.length != 7)	throw new IllegalArgumentException();
		
		StringBuilder prevDestinationParamPath = new StringBuilder();
		prevDestinationParamPath.append(params[2]);
		for (int i=3; i<7; i++)
		{
			prevDestinationParamPath.append("/");
			prevDestinationParamPath.append(params[i]);
		}
		
		context.put("prevDestinationParamPath", prevDestinationParamPath.toString());
		
		//the question types
		List<QuestionPlugin> questionTypes = this.mnemeService.getQuestionPlugins();
		context.put("questionTypes", questionTypes);
		
		//for the selected question type
		Value value = this.uiService.newValue();
		context.put("selectedQuestionType", value);
		
		// render
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (params.length != 7)	throw new IllegalArgumentException();
		
		//for the selected question type
		Value value = this.uiService.newValue();
		context.put("selectedQuestionType", value);
		
		//read the form
		String destination = uiService.decode(req, context);
		
		String selectedQuestionType = value.getValue();
		
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, context.getDestination())));
	}

	/**
	 * @param mnemeService the mnemeService to set
	 */
	public void setMnemeService(MnemeService mnemeService)
	{
		this.mnemeService = mnemeService;
	}
}
