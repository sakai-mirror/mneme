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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.util.ViewImpl;
import org.sakaiproject.db.api.SqlService;

/**
 * The /install_all view for the mneme test tool.
 */
public class InstallAllView extends ViewImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(InstallAllView.class);

	/** The sql service. */
	protected SqlService sqlService = null;

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
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
	{
		// no parameters expected
		if (params.length != 2)
		{
			throw new IllegalArgumentException();
		}

		// do the install
		String rv = installMnemeAll();

		context.put("rv", rv);

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
		throw new IllegalArgumentException();
	}

	/**
	 * Set the sql service.
	 * 
	 * @param service
	 *        The sql serivce.
	 */
	public void setSqlService(SqlService service)
	{
		this.sqlService = service;
	}

	/**
	 * Add Mneme to all the sites that have Samigo.
	 */
	protected String installMnemeAll()
	{
		StringBuffer rv = new StringBuffer();
		rv.append("Installing Mneme:<br />");

		// find all the sites
		StringBuffer statement = new StringBuffer();
		statement.append("SELECT S.SITE_ID ");
		statement.append("FROM SAKAI_SITE S ");
		statement.append("LEFT OUTER JOIN SAKAI_SITE_TOOL A ON S.SITE_ID = A.SITE_ID AND A.REGISTRATION = 'sakai.samigo' ");
		statement.append("LEFT OUTER JOIN SAKAI_SITE_TOOL B ON S.SITE_ID = B.SITE_ID AND B.REGISTRATION = 'sakai.mneme' ");
		statement.append("WHERE A.REGISTRATION IS NOT NULL AND B.REGISTRATION IS NULL");
		List sites = sqlService.dbRead(statement.toString(), null, null);

		// for each one, install
		for (Iterator i = sites.iterator(); i.hasNext();)
		{
			String site = (String) i.next();
			String res = InstallView.installMneme(site);
			rv.append(site);
			rv.append(": ");
			rv.append(res);
			rv.append("<br />");
		}

		return rv.toString();
	}
}
