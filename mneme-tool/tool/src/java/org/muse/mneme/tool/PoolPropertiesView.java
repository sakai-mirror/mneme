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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * 
 */
public class PoolPropertiesView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolPropertiesView.class);

	/** Pool Service */
	protected PoolService poolService = null;

	/** tool manager reference. */
	protected ToolManager toolManager = null;

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (params.length != 3) throw new IllegalArgumentException();

		Pool pool = this.poolService.getPool(params[2]);
		context.put("pool", pool);

		// get the subjects
		List<String> subjects = poolService.getSubjects(toolManager.getCurrentPlacement().getContext(), null);
		context.put("subjects", subjects);

		// render
		uiService.render(ui, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (params.length != 3) throw new IllegalArgumentException();

		// setup the model: the selected pool
		Pool pool = this.poolService.getPool(params[2]);
		context.put("pool", pool);

		// read the form
		String destination = uiService.decode(req, context);

		if (pool != null)
		{
			try
			{
				if (pool.getTitle() == null)
					pool.setTitle("");
				
				if (pool.getSubject() == null)
					pool.setSubject("");
				
				this.poolService.savePool(pool, toolManager.getCurrentPlacement().getContext());
			}
			catch (AssessmentPermissionException e)
			{
				//redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}			
		}

		destination = "/pools/";
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));

	}

	/**
	 * @param poolService
	 *        the poolService to set
	 */
	public void setPoolService(PoolService poolService)
	{
		this.poolService = poolService;
	}

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}

}
