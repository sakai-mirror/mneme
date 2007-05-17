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
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;
import org.muse.ambrosia.util.ViewImpl;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;

/**
 * The /install view for the mneme test tool.
 */
public class InstallView extends ViewImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(InstallView.class);

	/** The site service. */
	protected static SiteService siteService = null;

	/** The tool manager. */
	protected static ToolManager toolManager = null;

	/**
	 * Add Mneme to the named site if T&Q is there.
	 * 
	 * @param context
	 *        The site id.
	 */
	protected static String installMneme(String context)
	{
		String rv = "installed in site " + context;

		// get the Test Center tool
		Tool tcTool = toolManager.getTool("sakai.mneme");

		// get the site, add a new page, modify the t&q tool permission
		try
		{
			Site site = siteService.getSite(context);
			boolean samFound = false;

			// find the site page with Mneme already
			boolean mnemeFound = false;
			for (Iterator i = site.getPages().iterator(); i.hasNext();)
			{
				SitePage page = (SitePage) i.next();
				String[] mnemeToolIds = {"sakai.mneme"};
				Collection mnemeTools = page.getTools(mnemeToolIds);
				if (!mnemeTools.isEmpty())
				{
					mnemeFound = true;
					break;
				}
			}

			if (mnemeFound)
			{
				return "Test Center already installed in site " + context;
			}

			// find the site page with T&Q
			for (Iterator i = site.getPages().iterator(); i.hasNext();)
			{
				SitePage page = (SitePage) i.next();
				String[] samToolIds = {"sakai.samigo"};
				Collection samTools = page.getTools(samToolIds);
				if (!samTools.isEmpty())
				{
					samFound = true;

					// add a new page just after this one
					SitePage newPage = site.addPage();
					newPage.setTitle(tcTool.getTitle());
					newPage.setPosition(page.getPosition() + 1);

					// add the tool
					ToolConfiguration config = newPage.addTool();
					config.setTitle(tcTool.getTitle());
					config.setTool("sakai.mneme", tcTool);

					// set the tool permission
					config.getPlacementConfig().put("functions.require", "mneme.submit");

					// set the T&Q tool permission
					((ToolConfiguration) samTools.iterator().next()).getPlacementConfig().put("functions.require", "assessment.createAssessment");

					break;
				}
			}

			if (samFound)
			{
				// add permissions to realm
				for (Iterator i = site.getRoles().iterator(); i.hasNext();)
				{
					Role role = (Role) i.next();
					if (role.isAllowed("assessment.createAssessment"))
					{
						role.allowFunction("mneme.manage");
						role.allowFunction("mneme.grade");
					}
					else if (role.isAllowed("assessment.takeAssessment"))
					{
						role.allowFunction("mneme.submit");
					}
				}

				// work around an "issue" in the Site impl - role changes do not trigger an azg save
				site.setMaintainRole(site.getMaintainRole());

				// save the site
				siteService.save(site);
			}
			else
			{
				rv = "T&Q not found in site " + context;
			}
		}
		catch (IdUnusedException e)
		{
			rv = e.toString();
		}
		catch (PermissionException e)
		{
			rv = e.toString();
		}

		return rv;
	}

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

		// one parameter expected
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		InstallSpecs specs = new InstallSpecs(params[2]);

		// do the install
		String rv = installMneme(specs.getContext());

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
	 * Set the security service.
	 * 
	 * @param service
	 *        The security service.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * Set the site service.
	 * 
	 * @param service
	 *        the site service.
	 */
	public void setSiteService(SiteService service)
	{
		this.siteService = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param service
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager service)
	{
		this.toolManager = service;
	}
}
