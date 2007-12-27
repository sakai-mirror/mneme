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
 * The /remove view for the mneme test tool.
 */
public class RemoveView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(RemoveView.class);

	/** The site service. */
	protected static SiteService siteService = null;

	/** The tool manager. */
	protected static ToolManager toolManager = null;

	/**
	 * Remove Mneme from the context.
	 * 
	 * @param context
	 *        The context id.
	 */
	protected static String removeMneme(String context)
	{
		// get the Test Center tool
		Tool tcTool = toolManager.getTool("sakai.mneme");

		try
		{
			Site site = siteService.getSite(context);

			// find the site page with Mneme already
			boolean removed = false;
			for (Iterator i = site.getPages().iterator(); i.hasNext();)
			{
				SitePage page = (SitePage) i.next();
				String[] mnemeToolIds = {"sakai.mneme"};
				Collection mnemeTools = page.getTools(mnemeToolIds);
				if (!mnemeTools.isEmpty())
				{
					// remove it
					site.removePage(page);
					removed = true;
					break;
				}
			}

			if (!removed) return "Test Center not in site " + site.getTitle() + " (" + context + ")";

			// clear the T&Q tool "functions.require"
			for (Iterator i = site.getPages().iterator(); i.hasNext();)
			{
				SitePage page = (SitePage) i.next();
				String[] samToolIds = {"sakai.samigo"};
				Collection samTools = page.getTools(samToolIds);
				if (!samTools.isEmpty())
				{
					((ToolConfiguration) samTools.iterator().next()).getPlacementConfig().remove("functions.require");
					break;
				}
			}

			// remove our permissions
			for (Iterator i = site.getRoles().iterator(); i.hasNext();)
			{
				Role role = (Role) i.next();
				role.disallowFunction("mnene.grade");
				role.disallowFunction("mnene.submit");
				role.disallowFunction("mnene.manage");
			}

			// work around a "feature" of the Site impl - role changes do not trigger an azg save
			site.setMaintainRole(site.getMaintainRole());

			// save the site
			siteService.save(site);

			return "Test Center removed from site " + site.getTitle() + " (" + context + ")";
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

		// do the remove
		String rv = removeMneme(contextId);

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
