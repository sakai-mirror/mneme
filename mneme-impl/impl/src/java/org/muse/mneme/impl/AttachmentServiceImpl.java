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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.Translation;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
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
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.InconsistentException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.id.api.IdManager;
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

	protected final static String PROP_UNIQUE_HOLDER = "attachment:unique";

	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	/** Dependency: ContentHostingService */
	protected ContentHostingService contentHostingService = null;

	/** Dependency: EntityManager */
	protected EntityManager entityManager = null;

	/** Dependency: IdManager. */
	protected IdManager idManager = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: ServerConfigurationService */
	protected ServerConfigurationService serverConfigurationService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/**
	 * {@inheritDoc}
	 */
	public Reference addAttachment(String application, String context, String prefix, boolean uniqueHolder, FileItem file)
	{
		String name = file.getName();
		if (name != null)
		{
			name = massageName(name);
		}

		String type = file.getContentType();

		// TODO: change to file.getInputStream() for after Sakai 2.3 more efficient support
		// InputStream body = file.getInputStream();
		byte[] body = file.get();

		long size = file.getSize();

		// detect no file selected
		if ((name == null) || (type == null) || (body == null) || (size == 0))
		{
			// TODO: if using input stream, close it
			// if (body != null) body.close();
			return null;
		}

		Reference rv = doAdd(name, type, body, size, application, context, prefix, uniqueHolder);

		// if this failed, and we are not using a uniqueHolder, try it with a uniqueHolder
		if ((rv == null) && !uniqueHolder)
		{
			rv = doAdd(name, type, body, size, application, context, prefix, true);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Reference addAttachment(String application, String context, String prefix, boolean uniqueHolder, Reference resourceRef)
	{
		// make sure we can read!
		pushAdvisor();

		try
		{
			// if from our docs, convert into a content hosting ref
			if (resourceRef.getType().equals(APPLICATION_ID))
			{
				resourceRef = entityManager.newReference(resourceRef.getId());
			}

			// make sure we can read!
			ContentResource resource = this.contentHostingService.getResource(resourceRef.getId());
			String type = resource.getContentType();
			long size = resource.getContentLength();
			byte[] body = resource.getContent();
			String name = resource.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);

			Reference rv = doAdd(name, type, body, size, application, context, prefix, uniqueHolder);

			// if this failed, and we are not using a uniqueHolder, try it with a uniqueHolder
			if ((rv == null) && !uniqueHolder)
			{
				rv = doAdd(name, type, body, size, application, context, prefix, true);
			}

			return rv;
		}
		catch (PermissionException e)
		{
			M_log.warn("addAttachment: " + e.toString());
		}
		catch (IdUnusedException e)
		{
			M_log.warn("addAttachment: " + e.toString());
		}
		catch (TypeException e)
		{
			M_log.warn("addAttachment: " + e.toString());
		}
		catch (ServerOverloadException e)
		{
			M_log.warn("addAttachment: " + e.toString());
		}
		finally
		{
			// clear the security advisor
			popAdvisor();
		}

		return null;
	}

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
		Reference contentHostingRef = entityManager.newReference(ref.getId());

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
		Reference contentHostingRef = entityManager.newReference(ref.getId());

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
		Reference contentHostingRef = entityManager.newReference(ref.getId());

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
		return serverConfigurationService.getAccessUrl() + ref.getReference();
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
					throw new EntityPermissionException(sessionManager.getCurrentSessionUserId(), "sampleAccess", ref.getReference());
				}

				// isolate the ContentHosting reference
				Reference contentHostingRef = entityManager.newReference(ref.getId());

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
	 * {@inheritDoc}
	 */
	public Reference getReference(String refString)
	{
		Reference ref = this.entityManager.newReference(refString);
		return ref;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> harvestAttachmentsReferenced(String data, boolean normalize)
	{
		Set<String> rv = new HashSet<String>();
		if (data == null) return rv;

		// pattern to find any src= or href= text
		// groups: 0: the whole matching text 1: src|href 2: the string in the quotes
		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"([^\"]*)\"");

		Matcher m = p.matcher(data);
		while (m.find())
		{
			if (m.groupCount() == 2)
			{
				String ref = m.group(2);

				// harvest any content hosting reference
				int index = ref.indexOf("/access/content/");
				if (index != -1)
				{
					// except for any in /user/ or /public/
					if (ref.indexOf("/access/content/user/") != -1)
					{
						index = -1;
					}
					else if (ref.indexOf("/access/content/public/") != -1)
					{
						index = -1;
					}
				}

				// harvest also the mneme docs references
				if (index == -1) index = ref.indexOf("/access/mneme/content/");
				
				// TODO: further filter to docs root and context (optional)
				if (index != -1)
				{
					// save just the reference part (i.e. after the /access);
					String refString = ref.substring(index + 7);

					// deal with %20 and other encoded URL stuff
					if (normalize)
					{
						try
						{
							refString = URLDecoder.decode(refString, "UTF-8");
						}
						catch (UnsupportedEncodingException e)
						{
							M_log.warn("harvestAttachmentsReferenced: " + e);
						}
					}

					rv.add(refString);
				}
			}
		}

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// register as an entity producer
			entityManager.registerEntityProducer(this, REFERENCE_ROOT);

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
	 * {@inheritDoc}
	 */
	public void removeAttachment(Reference ref)
	{
		pushAdvisor();
		String id = entityManager.newReference(ref.getId()).getId();
		try
		{
			// check if this has a unique containing collection
			ContentResource resource = this.contentHostingService.getResource(id);
			ContentCollection collection = resource.getContainingCollection();

			// remove the resource
			this.contentHostingService.removeResource(id);

			// if the collection was made just to hold the attachment, remove it as well
			if (collection.getProperties().getProperty(PROP_UNIQUE_HOLDER) != null)
			{
				this.contentHostingService.removeCollection(collection.getId());
			}
		}
		catch (PermissionException e)
		{
			M_log.warn("removeAttachment: " + e.toString());
		}
		catch (ServerOverloadException e)
		{
			M_log.warn("removeAttachment: " + e.toString());
		}
		catch (InUseException e)
		{
			M_log.warn("removeAttachment: " + e.toString());

		}
		catch (IdUnusedException e)
		{
			M_log.warn("removeAttachment: " + e.toString());
		}
		catch (TypeException e)
		{
			M_log.warn("removeAttachment: " + e.toString());
		}
		finally
		{
			popAdvisor();
		}
	}

	/**
	 * Dependency: ContentHostingService.
	 * 
	 * @param service
	 *        The ContentHostingService.
	 */
	public void setContentHostingService(ContentHostingService service)
	{
		contentHostingService = service;
	}

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		entityManager = service;
	}

	/**
	 * Set the IdManager
	 * 
	 * @param IdManager
	 *        The IdManager
	 */
	public void setIdManager(IdManager idManager)
	{
		this.idManager = idManager;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		securityService = service;
	}

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		serverConfigurationService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		sessionManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateEmbeddedReferences(String data, List<Translation> translations)
	{
		if (data == null) return data;
		if (translations == null) return data;

		// pattern to find any src= or href= text
		// groups: 0: the whole matching text 1: src|href 2: the string in the quotes
		Pattern p = Pattern.compile("(src|href)[\\s]*=[\\s]*\"([^\"]*)\"");

		Matcher m = p.matcher(data);
		StringBuffer sb = new StringBuffer();

		// process each "harvested" string (avoiding like strings that are not in src= or href= patterns)
		while (m.find())
		{
			if (m.groupCount() == 2)
			{
				String ref = m.group(2);

				// harvest any content hosting reference
				int index = ref.indexOf("/access/content/");
				if (index != -1)
				{
					// except for any in /user/ or /public/
					if (ref.indexOf("/access/content/user/") != -1)
					{
						index = -1;
					}
					else if (ref.indexOf("/access/content/public/") != -1)
					{
						index = -1;
					}
				}

				// harvest also the mneme docs references
				if (index == -1) index = ref.indexOf("/access/mneme/content/");

				if (index != -1)
				{
					// save just the reference part (i.e. after the /access);
					String normal = ref.substring(index + 7);

					// deal with %20 and other encoded URL stuff
					try
					{
						normal = URLDecoder.decode(normal, "UTF-8");
					}
					catch (UnsupportedEncodingException e)
					{
						M_log.warn("harvestAttachmentsReferenced: " + e);
					}

					// translate the normal form
					String translated = normal;
					for (Translation translation : translations)
					{
						translated = translation.translate(translated);
					}

					// if changed, replace
					if (!normal.equals(translated))
					{
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "=\"" + ref.substring(0, index + 7) + translated + "\""));
					}
				}
				else
				{
					m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
				}
			}
			else
			{
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
			}
		}

		m.appendTail(sb);

		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}

	/**
	 * Assure that a collection with this name exists in the container collection: create it if it is missing.
	 * 
	 * @param container
	 *        The full path of the container collection.
	 * @param name
	 *        The collection name to check and create (no trailing slash needed).
	 * @param uniqueHolder
	 *        true if the folder is being created solely to hold the attachment uniquely.
	 */
	protected void assureCollection(String container, String name, boolean uniqueHolder)
	{
		try
		{
			contentHostingService.getCollection(container + name + "/");
		}
		catch (IdUnusedException e)
		{
			try
			{
				ContentCollectionEdit edit = contentHostingService.addCollection(container + name + "/");
				ResourcePropertiesEdit props = edit.getPropertiesEdit();

				// set the alternate reference root so we get all requests
				props.addProperty(ContentHostingService.PROP_ALTERNATE_REFERENCE, AttachmentService.REFERENCE_ROOT);

				props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

				// mark it if it is a unique holder
				if (uniqueHolder)
				{
					props.addProperty(PROP_UNIQUE_HOLDER, PROP_UNIQUE_HOLDER);
				}

				contentHostingService.commitCollection(edit);
			}
			catch (IdUsedException e2)
			{
				// M_log.warn("init: creating our root collection: " + e2.toString());
			}
			catch (IdInvalidException e2)
			{
				M_log.warn("init: creating our root collection: " + e2.toString());
			}
			catch (PermissionException e2)
			{
				M_log.warn("init: creating our root collection: " + e2.toString());
			}
			catch (InconsistentException e2)
			{
				M_log.warn("init: creating our root collection: " + e2.toString());
			}
		}
		catch (TypeException e)
		{
			M_log.warn("init: checking our root collection: " + e.toString());
		}
		catch (PermissionException e)
		{
			M_log.warn("init: checking our root collection: " + e.toString());
		}
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
	 * Perform the add.
	 * 
	 * @param name
	 *        The file name.
	 * @param type
	 *        The mime type.
	 * @param body
	 *        The body bytes.
	 * @param size
	 *        The body size.
	 * @param application
	 *        The application prefix for the collection in private.
	 * @param context
	 *        The context associated with the attachment.
	 * @param prefix
	 *        Any prefix path for within the context are of the application in private.
	 * @param uniqueHolder
	 *        If true, a uniquely named folder is created to hold the resource.
	 * @return The Reference to the added attachment.
	 */
	protected Reference doAdd(String name, String type, byte[] body, long size, String application, String context, String prefix,
			boolean uniqueHolder)
	{
		pushAdvisor();

		// form the content hosting path, and make sure all the folders exist
		String contentPath = "/private/";
		assureCollection(contentPath, application, false);
		contentPath += application + "/";
		assureCollection(contentPath, context, false);
		contentPath += context + "/";
		if ((prefix != null) && (prefix.length() > 0))
		{
			assureCollection(contentPath, prefix, false);
			contentPath += prefix + "/";
		}
		if (uniqueHolder)
		{
			String uuid = this.idManager.createUuid();
			assureCollection(contentPath, uuid, true);
			contentPath += uuid + "/";
		}

		contentPath += name;

		try
		{
			ContentResourceEdit edit = contentHostingService.addResource(contentPath);
			edit.setContent(body);
			edit.setContentType(type);
			ResourcePropertiesEdit props = edit.getPropertiesEdit();

			// set the alternate reference root so we get all requests
			props.addProperty(ContentHostingService.PROP_ALTERNATE_REFERENCE, AttachmentService.REFERENCE_ROOT);

			props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

			contentHostingService.commitResource(edit);

			String ref = edit.getReference(ContentHostingService.PROP_ALTERNATE_REFERENCE);
			Reference reference = entityManager.newReference(ref);

			return reference;
		}
		catch (PermissionException e2)
		{
			M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		catch (IdUsedException e2)
		{
			// M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		catch (IdInvalidException e2)
		{
			M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		catch (InconsistentException e2)
		{
			M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		catch (ServerOverloadException e2)
		{
			M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		catch (OverQuotaException e2)
		{
			M_log.warn("addAttachment: creating our content: " + e2.toString());
		}
		finally
		{
			// try
			// {
			// // TODO: if using input stream
			// if (body != null) body.close();
			// }
			// catch (IOException e)
			// {
			// }

			popAdvisor();
		}

		return null;
	}

	/**
	 * Trim the name to only the characters after the last slash of either kind.<br />
	 * Remove junk from uploaded file names.
	 * 
	 * @param name
	 *        The string to trim.
	 * @return The trimmed string.
	 */
	protected String massageName(String name)
	{
		// if there are any slashes, forward or back, take from the last one found to the right as the name
		int pos = -1;
		for (int i = name.length() - 1; i >= 0; i--)
		{
			char c = name.charAt(i);
			if ((c == '/') || (c == '\\'))
			{
				pos = i + 1;
				break;
			}
		}

		if (pos != -1)
		{
			name = name.substring(pos);
		}

		return name;
	}

	/**
	 * Remove our security advisor.
	 */
	protected void popAdvisor()
	{
		securityService.popAdvisor();
	}

	/**
	 * Setup a security advisor.
	 */
	protected void pushAdvisor()
	{
		// setup a security advisor
		securityService.pushAdvisor(new SecurityAdvisor()
		{
			public SecurityAdvice isAllowed(String userId, String function, String reference)
			{
				return SecurityAdvice.ALLOWED;
			}
		});
	}
}
