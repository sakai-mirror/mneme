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
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Ent;
import org.muse.mneme.api.ImportService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /import_tq_pool view for the mneme tool.
 */
public class ImportTqPoolView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ImportTqPoolView.class);

	/** Dependency: ImportService */
	protected ImportService importService = null;

	/** tool manager reference. */
	protected ToolManager toolManager = null;

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
		// [2] pools sort
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}
		String poolsSort = params[2];
		context.put("poolsSort", poolsSort);

		// the list of importable pools for this user
		List<Ent> pools = this.importService.getPools(null);
		context.put("pools", pools);

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
		// [2] pools sort
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}
		String poolsSort = params[2];

		Values selectedPools = this.uiService.newValues();
		context.put("selectedPools", selectedPools);

		// read the form
		String destination = uiService.decode(req, context);

		// import the pools
		if ("IMPORT".equals(destination))
		{
			for (String id : selectedPools.getValues())
			{
				this.importService.importPool(id, toolManager.getCurrentPlacement().getContext());
			}

			destination = "/pools/" + poolsSort;
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the ImportService
	 * 
	 * @param service
	 *        the ImportService.
	 */
	public void setImportService(ImportService service)
	{
		this.importService = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param manager
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager manager)
	{
		toolManager = manager;
	}
}
