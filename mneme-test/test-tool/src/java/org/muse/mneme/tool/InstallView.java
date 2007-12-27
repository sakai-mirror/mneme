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
import org.muse.ambrosia.util.ControllerImpl;
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
 * The /install view for the mneme admin tool.
 */
public class InstallView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(InstallView.class);

	/** The site service. */
	protected static SiteService siteService = null;

	/** The tool manager. */
	protected static ToolManager toolManager = null;

	/**
	 * Add Mneme to the named context.
	 * 
	 * @param context
	 *        The context id.
	 */
	protected static String installMneme(String context)
	{
		if (siteService.isSpecialSite(context))
		{
			return "Site " + context + " is special - skipping.";
		}

		// get the Test Center tool
		Tool tcTool = toolManager.getTool("sakai.mneme");

		try
		{
			Site site = siteService.getSite(context);

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
				return "Test Center already installed in site " + site.getTitle() + " (" + context + ")";
			}

			// add a new page
			SitePage newPage = site.addPage();
			newPage.setTitle(tcTool.getTitle());
			// TODO: newPage.setPosition(?);

			// add the tool
			ToolConfiguration config = newPage.addTool();
			config.setTitle(tcTool.getTitle());
			config.setTool("sakai.mneme", tcTool);

			// add permissions to realm
			for (Iterator i = site.getRoles().iterator(); i.hasNext();)
			{
				Role role = (Role) i.next();
				if (manageRole(role.getId()))
				{
					role.allowFunction("mneme.manage");
					role.allowFunction("mneme.grade");
				}
				else if (submitRole(role.getId()))
				{
					role.allowFunction("mneme.submit");
				}
			}

			// work around a "feature" of the Site impl - role changes do not trigger an azg save
			site.setMaintainRole(site.getMaintainRole());

			// save the site
			siteService.save(site);

			return "Test Center installed in site " + site.getTitle() + " (" + context + ")";
		}
		catch (IdUnusedException e)
		{
			return e.toString();
		}
		catch (PermissionException e)
		{
			return e.toString();
		}
	}

	/**
	 * Is the roleId a mneme manage role?
	 * 
	 * @param roleId
	 *        The role id.
	 * @return true if this is a manage role, false if not.
	 */
	protected static boolean manageRole(String roleId)
	{
		if (roleId.equalsIgnoreCase("maintain")) return true;
		if (roleId.equalsIgnoreCase("instructor")) return true;
		if (roleId.equalsIgnoreCase("teaching assistant")) return true;

		return false;
	}

	/**
	 * Is the roleId a mneme submit role?
	 * 
	 * @param roleId
	 *        The role id.
	 * @return true if this is a submit role, false if not.
	 */
	protected static boolean submitRole(String roleId)
	{
		if (roleId.equalsIgnoreCase("access")) return true;
		if (roleId.equalsIgnoreCase("student")) return true;

		return false;
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

		String contextId = params[2];

		// do the install
		String rv = installMneme(contextId);

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
