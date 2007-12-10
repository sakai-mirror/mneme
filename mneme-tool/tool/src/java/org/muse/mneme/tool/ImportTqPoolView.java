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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.sakaiproject.util.Web;

/**
 * The /import_tq_pool view for the mneme tool.
 */
public class ImportTqPoolView extends ControllerImpl
{
	public class IdTitle
	{
		protected String id;

		protected String title;

		public IdTitle(String id, String title)
		{
			setId(id);
			setTitle(title);
		}

		/**
		 * @return the id
		 */
		public String getId()
		{
			return this.id;
		}

		/**
		 * @return the title
		 */
		public String getTitle()
		{
			return this.title;
		}

		/**
		 * @param id
		 *        the id to set
		 */
		public void setId(String id)
		{
			this.id = id;
		}

		/**
		 * @param title
		 *        the title to set
		 */
		public void setTitle(String title)
		{
			this.title = title;
		}
	}

	/** Our log. */
	private static Log M_log = LogFactory.getLog(ImportTqPoolView.class);

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

		// the list of Samigo pools for this user
		// TODO:
		List<IdTitle> pools = new ArrayList<IdTitle>();
		pools.add(new IdTitle("0", "EECS 100 - Assignments"));
		pools.add(new IdTitle("2", "EECS 100 - Tests"));
		pools.add(new IdTitle("3", "EECS 200 - Assignments"));
		pools.add(new IdTitle("4", "EECS 200 - Tests"));

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
		// String poolsSort = params[2];
		// context.put("poolsSort", poolsSort);

		Values selectedPools = this.uiService.newValues();
		context.put("selectedPools", selectedPools);

		// read the form
		String destination = uiService.decode(req, context);

		// TODO: import the pools

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}
}
