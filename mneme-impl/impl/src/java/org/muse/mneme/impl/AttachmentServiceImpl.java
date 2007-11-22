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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AttachmentService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityAccessOverloadException;
import org.sakaiproject.entity.api.EntityCopyrightException;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityNotDefinedException;
import org.sakaiproject.entity.api.EntityPermissionException;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * AttachmentServiceImpl implements AttachmentService.
 */
public class AttachmentServiceImpl implements AttachmentService, EntityProducer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AttachmentServiceImpl.class);

	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	/** Dependency: EntityManager */
	protected EntityManager m_entityManager = null;

	/** Dependency: SecurityService */
	protected SecurityService m_securityService = null;

	/** Dependency: ServerConfigurationService */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/**
	 * {@inheritDoc}
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		return null;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		// decide on security
		if (!checkSecurity(ref)) return null;

		// isolate the ContentHosting reference
		Reference contentHostingRef = m_entityManager.newReference(ref.getId());

		// setup a security advisor
		pushAdvisor();
		try
		{
			// make sure we have a valid ContentHosting reference with an entity producer we can talk to
			EntityProducer service = contentHostingRef.getEntityProducer();
			if (service == null) return null;

			// pass on the request
			return service.getEntity(contentHostingRef);
		}
		finally
		{
			// clear the security advisor
			popAdvisor();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		// Since we handle security ourself, we won't support anyone else asking
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		// decide on security
		if (!checkSecurity(ref)) return null;

		// isolate the ContentHosting reference
		Reference contentHostingRef = m_entityManager.newReference(ref.getId());

		// setup a security advisor
		pushAdvisor();
		try
		{
			// make sure we have a valid ContentHosting reference with an entity producer we can talk to
			EntityProducer service = contentHostingRef.getEntityProducer();
			if (service == null) return null;

			// pass on the request
			return service.getEntityDescription(contentHostingRef);
		}
		finally
		{
			// clear the security advisor
			popAdvisor();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		// decide on security
		if (!checkSecurity(ref)) return null;

		// isolate the ContentHosting reference
		Reference contentHostingRef = m_entityManager.newReference(ref.getId());

		// setup a security advisor
		pushAdvisor();
		try
		{
			// make sure we have a valid ContentHosting reference with an entity producer we can talk to
			EntityProducer service = contentHostingRef.getEntityProducer();
			if (service == null) return null;

			// pass on the request
			return service.getEntityResourceProperties(contentHostingRef);
		}
		finally
		{
			// clear the security advisor
			popAdvisor();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		return m_serverConfigurationService.getAccessUrl() + ref.getReference();
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return new HttpAccess()
		{
			public void handleAccess(HttpServletRequest req, HttpServletResponse res, Reference ref, Collection copyrightAcceptedRefs)
					throws EntityPermissionException, EntityNotDefinedException, EntityAccessOverloadException, EntityCopyrightException
			{
				// decide on security
				if (!checkSecurity(ref))
				{
					throw new EntityPermissionException(m_sessionManager.getCurrentSessionUserId(), "sampleAccess", ref.getReference());
				}

				// isolate the ContentHosting reference
				Reference contentHostingRef = m_entityManager.newReference(ref.getId());

				// setup a security advisor
				pushAdvisor();
				try
				{
					// make sure we have a valid ContentHosting reference with an entity producer we can talk to
					EntityProducer service = contentHostingRef.getEntityProducer();
					if (service == null) throw new EntityNotDefinedException(ref.getReference());

					// get the producer's HttpAccess helper, it might not support one
					HttpAccess access = service.getHttpAccess();
					if (access == null) throw new EntityNotDefinedException(ref.getReference());

					// let the helper do the work
					access.handleAccess(req, res, contentHostingRef, copyrightAcceptedRefs);
				}
				finally
				{
					// clear the security advisor
					popAdvisor();
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return null;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// register as an entity producer
			m_entityManager.registerEntityProducer(this, REFERENCE_ROOT);

			M_log.info("init()");
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (reference.startsWith(REFERENCE_ROOT))
		{
			// we will get null, sampleAccess, content, private, sampleAccess, <context>, test.txt
			// we will store the context, and the ContentHosting reference in our id field.
			String id = null;
			String context = null;
			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);

			if (parts.length > 5)
			{
				context = parts[5];
				id = "/" + StringUtil.unsplit(parts, 2, parts.length - 2, "/");
			}

			ref.set(APPLICATION_ID, null, id, null, context);

			return true;
		}

		return false;
	}

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		m_securityService = service;
	}

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		m_sessionManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}

	/**
	 * Check security for this entity.
	 * 
	 * @param ref
	 *        The Reference to the entity.
	 * @return true if allowed, false if not.
	 */
	protected boolean checkSecurity(Reference ref)
	{
		// TODO:
		return true;
	}

	/**
	 * Remove our security advisor.
	 */
	protected void popAdvisor()
	{
		m_securityService.popAdvisor();
	}

	/**
	 * Setup a security advisor.
	 */
	protected void pushAdvisor()
	{
		// setup a security advisor
		m_securityService.pushAdvisor(new SecurityAdvisor()
		{
			public SecurityAdvice isAllowed(String userId, String function, String reference)
			{
				return SecurityAdvice.ALLOWED;
			}
		});
	}

}
