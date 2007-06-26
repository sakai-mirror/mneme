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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ViewImpl;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.util.Web;

/**
 * The /home view for the mneme test tool.
 */
public class HomeView extends ViewImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(HomeView.class);

	/** The security service. */
	protected SecurityService securityService = null;

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
		// if not logged in as the super user, we won't do anything
		if (!securityService.isSuperUser())
		{
			throw new IllegalArgumentException();
		}

		// no parameters expected
		if (params.length != 2)
		{
			throw new IllegalArgumentException();
		}

		context.put("gspecs", new GenerateSpecs());
		context.put("sspecs", new SimulateSpecs());
		context.put("ispecs", new InstallSpecs());

		// render
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();

		// uiHome = TestControllers.constructHome(uiService);

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		if (!context.getPostExpected())
		{
			throw new IllegalArgumentException();
		}

		// no parameters expected
		if (params.length != 2)
		{
			throw new IllegalArgumentException();
		}

		// read form
		GenerateSpecs gspecs = new GenerateSpecs();
		context.put("gspecs", gspecs);
		SimulateSpecs sspecs = new SimulateSpecs();
		context.put("sspecs", sspecs);
		InstallSpecs ispecs = new InstallSpecs();
		context.put("ispecs", ispecs);
		GbSpecs gbspecs = new GbSpecs();
		context.put("gbspecs", gbspecs);
		String destination = uiService.decode(req, context);

		// look for special codes in the destination
		if ("/generate".equals(destination))
		{
			// add the specs
			destination = destination + "/" + gspecs.toString();
		}

		else if ("/simulate".equals(destination))
		{
			// add the specs
			destination = destination + "/" + sspecs.toString();
		}

		else if ("/install".equals(destination))
		{
			// add the specs
			destination = destination + "/" + ispecs.toString();
		}

		else if ("/gb".equals(destination))
		{
			// add the specs
			destination = destination + "/" + gbspecs.toString();
		}

		// redirect to home
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the security service.
	 * 
	 * @param service
	 *        The security service.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}
}
