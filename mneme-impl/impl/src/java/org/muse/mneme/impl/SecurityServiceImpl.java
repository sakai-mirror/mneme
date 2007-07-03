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

package org.muse.mneme.impl;

import java.util.Collection;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.SecurityService;
import org.sakaiproject.site.api.SiteService;

/**
 * SecurityServiceImpl implements Mneme's SecurityService
 */
public class SecurityServiceImpl implements SecurityService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(SecurityServiceImpl.class);

	/** Dependency: SecurityService */
	protected org.sakaiproject.authz.api.SecurityService m_securityService = null;

	/** Dependency: SiteService */
	protected SiteService m_siteService = null;

	/**
	 * Check the security for this user doing this function withing this context.
	 * 
	 * @param userId
	 *        the user id.
	 * @param function
	 *        the function.
	 * @param context
	 *        The context.
	 * @param ref
	 *        The entity reference.
	 * @return true if the user has permission, false if not.
	 */
	public boolean checkSecurity(String userId, String function, String context)
	{
		// check for super user
		if (m_securityService.isSuperUser(userId)) return true;

		// check for the user / function / context-as-site-authz
		// use the site ref for the security service (used to cache the security calls in the security service)
		String siteRef = m_siteService.siteReference(context);

		// form the azGroups for a context-as-implemented-by-site (Note the *lack* of direct dependency on Site, i.e. we stole the
		// code!)
		Collection azGroups = new Vector(2);
		azGroups.add(siteRef);
		azGroups.add("!site.helper");

		String rev = "/site/" + context;

		boolean rv = m_securityService.unlock(userId, function, siteRef, azGroups);
		return rv;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * Check security and throw if not satisfied
	 * 
	 * @param userId
	 *        the user id.
	 * @param function
	 *        the function.
	 * @param context
	 *        The context.
	 * @param ref
	 *        The entity reference.
	 * @throws AssessmentPermissionException
	 *         if security is not satisfied.
	 */
	public void secure(String userId, String function, String context) throws AssessmentPermissionException
	{
		if (!checkSecurity(userId, function, context))
		{
			throw new AssessmentPermissionException(userId, function, context);
		}
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(org.sakaiproject.authz.api.SecurityService service)
	{
		m_securityService = service;
	}

	/**
	 * Dependency: SiteService.
	 * 
	 * @param service
	 *        The SiteService.
	 */
	public void setSiteService(SiteService service)
	{
		m_siteService = service;
	}
}
