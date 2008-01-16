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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;

/**
 * The /swap_bulk view for the mneme admin tool.
 */
public class SwapBulkView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SwapBulkView.class);

	/** The security service. */
	protected SecurityService securityService = null;

	/** The site service. */
	protected SiteService siteService = null;

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

		String siteTitlePattern = params[2];

		// convert "*" to the db wildcard, %
		siteTitlePattern = siteTitlePattern.replaceAll("\\*", "%");

		// do the install
		String rv = swapBulk(siteTitlePattern);

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
	 * Remove Samigo from the context.
	 * 
	 * @param context
	 *        The context id.
	 */
	protected String removeSamigo(String context)
	{
		if (siteService.isSpecialSite(context))
		{
			return "Site " + context + " is special - skipping.";
		}

		try
		{
			Site site = siteService.getSite(context);

			for (Iterator i = site.getPages().iterator(); i.hasNext();)
			{
				SitePage page = (SitePage) i.next();
				String[] samToolIds = {"sakai.samigo"};
				Collection samTools = page.getTools(samToolIds);
				if (!samTools.isEmpty())
				{
					site.removePage(page);
					break;
				}
			}

			// TODO: remove Samigo permissions?
			// for (Iterator i = site.getRoles().iterator(); i.hasNext();)
			// {
			// Role role = (Role) i.next();
			// role.disallowFunction("mneme.grade");
			// role.disallowFunction("mneme.submit");
			// role.disallowFunction("mneme.manage");
			// }

			// // work around a "feature" of the Site impl - role changes do not trigger an azg save
			// site.setMaintainRole(site.getMaintainRole());

			// save the site
			siteService.save(site);

			return " - Samigo removed from site " + site.getTitle() + " (" + context + ")";
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
	 * Add Mneme to all the sites that meet the site title pattern.
	 * 
	 * @param siteTitlePattern
	 *        A where site.title like string for selecting the sites. "*" means all.
	 */
	protected String swapBulk(String siteTitlePattern)
	{
		StringBuffer rv = new StringBuffer();
		rv.append("Installing Test Center:<br />");

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT S.SITE_ID ");
		sql.append(" FROM SAKAI_SITE S ");
		sql.append(" LEFT OUTER JOIN SAKAI_SITE_TOOL A ON S.SITE_ID = A.SITE_ID AND A.REGISTRATION = 'sakai.samigo' ");
		sql.append(" WHERE A.REGISTRATION IS NOT NULL");
		sql.append(" AND S.TITLE LIKE ?");

		Object[] fields = new Object[1];
		fields[0] = siteTitlePattern;

		List sites = sqlService.dbRead(sql.toString(), fields, null);

		// for each one, install
		for (Iterator i = sites.iterator(); i.hasNext();)
		{
			String site = (String) i.next();
			String res = InstallView.installMneme(site);
			res += removeSamigo(site);
			rv.append(res);
			rv.append("<br />");
		}

		return rv.toString();
	}
}
