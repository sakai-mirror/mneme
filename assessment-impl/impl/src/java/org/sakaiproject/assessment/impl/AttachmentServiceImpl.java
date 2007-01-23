/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.AssessmentService;
import org.sakaiproject.assessment.api.Attachment;
import org.sakaiproject.assessment.api.AttachmentService;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
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
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>
 * AssessmentServiceImpl is ...
 * </p>
 */
public class AttachmentServiceImpl implements AttachmentService, EntityProducer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AttachmentServiceImpl.class);

	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** stream content requests if true, read all into memory and send if false. */
	protected static final boolean STREAM_CONTENT = true;

	/** Dependency: AssessmentService: Note: dependent on the impl... */
	protected AssessmentServiceImpl m_assessmentService = null;

	/** A cache of attachments. */
	protected Cache m_cache = null;

	/** The # seconds between cache cleaning runs. */
	protected int m_cacheCleanerSeconds = 0;

	/** The # seconds to cache assessment reads. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

	/** Dependency: EntityManager */
	protected EntityManager m_entityManager = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

	/** Dependency: MemoryService */
	protected MemoryService m_memoryService = null;

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected ThreadLocalManager m_threadLocalManager = null;

	protected TimeService m_timeService = null;

	/**
	 * {@inheritDoc}
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		// TODO Auto-generated method stub
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
	public Attachment getAttachment(final Reference attachmentRef)
	{
		AttachmentImpl rv = null;

		// check the cache
		rv = getCachedAttachment(attachmentRef);
		if (rv != null) return rv;

		// read the attachment from the samigo media table
		String statement = "SELECT M.FILESIZE, M.MIMETYPE, M.FILENAME, M.LASTMODIFIEDDATE" + " FROM SAM_MEDIA_T M"
				+ " WHERE M.MEDIAID = ?";
		Object[] fields = new Object[1];
		fields[0] = attachmentRef.getId();
		List found = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					long contentLength = result.getLong(1);
					String contentType = result.getString(2);
					String name = result.getString(3);
					java.sql.Timestamp ts = result.getTimestamp(4, m_sqlService.getCal());

					Time timestamp = null;
					if (ts != null)
					{
						timestamp = m_timeService.newTime(ts.getTime());
					}

					return new AttachmentImpl(attachmentRef.getId(), new Long(contentLength), name, timestamp, contentType);
				}
				catch (SQLException e)
				{
					M_log.warn("getAttachment: " + e);
					return null;
				}
			}
		});

		if ((found != null) && (!found.isEmpty()))
		{
			if (found.size() > 1)
			{
				M_log.warn("getAttachment: more than one found: " + attachmentRef.getId());
			}

			rv = (AttachmentImpl) found.get(0);
		}

		// cache if found
		cacheAttachment(attachmentRef, rv);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAttachmentReference(String container, String id)
	{
		String ref = REFERENCE_ROOT + ((container == null) ? "" : ("/" + container + ((id == null) ? "" : ("/" + id))));
		return ref;
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Configuration
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		ResourcePropertiesEdit edit = new BaseResourcePropertiesEdit();
		Attachment a = getAttachment(ref);

		edit.addProperty(ResourceProperties.PROP_IS_COLLECTION, "false");
		edit.addProperty(ResourceProperties.PROP_CONTENT_TYPE, a.getType());
		edit.addProperty("DAV:displayname", a.getName());
		edit.addProperty(ResourceProperties.PROP_CONTENT_LENGTH, a.getLength().toString());
		edit.addProperty(ResourceProperties.PROP_MODIFIED_DATE, a.getTimestamp().toString());

		return edit;
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
			public void handleAccess(HttpServletRequest req, HttpServletResponse res, Reference ref,
					Collection copyrightAcceptedRefs) throws EntityPermissionException, EntityNotDefinedException,
					EntityAccessOverloadException, EntityCopyrightException
			{
				// get the submission (the refrence container) for security checks
				Submission submission = m_assessmentService.idSubmission(ref.getContainer());
				if (submission == null)
				{
					throw new EntityPermissionException(m_sessionManager.getCurrentSessionUserId(), ATTACHMENT_READ, ref
							.getReference());
				}

				// if the user is the submission user, pass security...
				if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
				{
					// user must have review or grading permission
					// TODO: for now, we use PUBLISH_PERMISSION... refine this
					if (!m_assessmentService.checkSecurity(m_sessionManager.getCurrentSessionUserId(),
							AssessmentService.PUBLISH_PERMISSION, submission.getAssessment().getContext(), m_assessmentService
									.getAssessmentReference(submission.getAssessment().getId())))
					{
						throw new EntityPermissionException(m_sessionManager.getCurrentSessionUserId(), ATTACHMENT_READ, ref
								.getReference());
					}
				}

				// get the attachment
				Attachment a = getAttachment(ref);
				if (a == null)
				{
					throw new EntityNotDefinedException(ref.getReference());
				}

				try
				{
					// changed to int from long because res.setContentLength won't take long param -- JE
					int len = a.getLength().intValue();
					String contentType = a.getType();

					// for url content type, encode a redirect to the body URL
					if (contentType.equalsIgnoreCase(ResourceProperties.TYPE_URL))
					{
						byte[] content = getAttachmentBody(a);
						if ((content == null) || (content.length == 0))
						{
							throw new IdUnusedException(ref.getReference());
						}

						String one = new String(content);
						String two = "";
						for (int i = 0; i < one.length(); i++)
						{
							if (one.charAt(i) == '+')
							{
								two += "%2b";
							}
							else
							{
								two += one.charAt(i);
							}
						}
						res.sendRedirect(two);
					}

					else
					{
						String fileName = a.getName();
						fileName = Validator.escapeResourceName(fileName);

						String disposition = null;
						if (Validator.letBrowserInline(contentType))
						{
							disposition = "inline; filename=\"" + fileName + "\"";
						}
						else
						{
							disposition = "attachment; filename=\"" + fileName + "\"";
						}

						// stream the content using a small buffer to keep memory managed
						if (STREAM_CONTENT)
						{
							InputStream content = null;
							OutputStream out = null;

							try
							{
								content = streamAttachmentBody(a);
								if (content == null)
								{
									throw new IdUnusedException(ref.getReference());
								}

								res.setContentType(contentType);
								res.addHeader("Content-Disposition", disposition);
								res.setContentLength(len);

								// set the buffer of the response to match what we are reading from the request
								if (len < STREAM_BUFFER_SIZE)
								{
									res.setBufferSize(len);
								}
								else
								{
									res.setBufferSize(STREAM_BUFFER_SIZE);
								}

								out = res.getOutputStream();

								// chunk
								byte[] chunk = new byte[STREAM_BUFFER_SIZE];
								int lenRead;
								while ((lenRead = content.read(chunk)) != -1)
								{
									out.write(chunk, 0, lenRead);
								}
							}
							catch (ServerOverloadException e)
							{
								throw e;
							}
							catch (Throwable ignore)
							{
							}
							finally
							{
								// be a good little program and close the stream - freeing up valuable system resources
								if (content != null)
								{
									content.close();
								}

								if (out != null)
								{
									try
									{
										out.close();
									}
									catch (Throwable ignore)
									{
									}
								}
							}
						}

						// read the entire content into memory and send it from there
						else
						{
							byte[] content = getAttachmentBody(a);
							if (content == null)
							{
								throw new IdUnusedException(ref.getReference());
							}

							res.setContentType(contentType);
							res.addHeader("Content-Disposition", disposition);
							res.setContentLength(len);

							// Increase the buffer size for more speed. - don't - we don't want a 20 meg buffer size,right? -ggolden
							// res.setBufferSize(len);

							OutputStream out = null;
							try
							{
								out = res.getOutputStream();
								out.write(content);
								out.flush();
								out.close();
							}
							catch (Throwable ignore)
							{
							}
							finally
							{
								if (out != null)
								{
									try
									{
										out.close();
									}
									catch (Throwable ignore)
									{
									}
								}
							}
						}
					}

					// track event
					// EventTrackingService.post(EventTrackingService.newEvent(EVENT_RESOURCE_READ, resource.getReference(), false));
				}
				catch (Throwable t)
				{
					throw new EntityNotDefinedException(ref.getReference());
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		// TODO Auto-generated method stub
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

			// <= 0 indicates no caching desired
			if ((m_cacheSeconds > 0) && (m_cacheCleanerSeconds > 0))
			{
				m_cache = m_memoryService.newHardCache(m_cacheCleanerSeconds, getAttachmentReference(null, null));
			}

			M_log.info("init(): caching minutes: " + m_cacheSeconds / 60 + " cache cleaner minutes: " + m_cacheCleanerSeconds / 60);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AttachmentService
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (reference.startsWith(REFERENCE_ROOT))
		{
			// we will get null, attachment, submission id, attachment id
			String id = null;
			String container = null;
			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);

			if (parts.length > 3)
			{
				container = parts[2];
				id = parts[3];
			}

			ref.set(APPLICATION_ID, null, id, container, null);

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAttachment(Reference attachment)
	{
		String statement = "DELETE FROM SAM_MEDIA_T WHERE MEDIAID = ?";
		Object[] fields = new Object[1];
		fields[0] = attachment.getId();

		m_sqlService.dbWrite(statement, fields);

		// generate an event
		m_eventTrackingService.post(m_eventTrackingService.newEvent(ATTACHMENT_DELETE, attachment.getReference(), true));
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		m_assessmentService = (AssessmentServiceImpl) service;
	}

	/**
	 * Set the # minutes between cache cleanings.
	 * 
	 * @param time
	 *        The # minutes between cache cleanings. (as an integer string).
	 */
	public void setCacheCleanerMinutes(String time)
	{
		m_cacheCleanerSeconds = Integer.parseInt(time) * 60;
	}

	/**
	 * Set the # minutes to cache.
	 * 
	 * @param time
	 *        The # minutes to cache a get (as an integer string).
	 */
	public void setCacheMinutes(String time)
	{
		m_cacheSeconds = Integer.parseInt(time) * 60;
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
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		m_eventTrackingService = service;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * EntityProducer
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Dependency: MemoryService.
	 * 
	 * @param service
	 *        The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
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
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		m_threadLocalManager = service;
	}

	/**
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Cache this attachment. Use the short-term cache if enable, else use the thread-local cache.
	 * 
	 * @param attachment
	 *        The attachment to cache.
	 */
	protected void cacheAttachment(Reference reference, AttachmentImpl attachment)
	{
		if (attachment == null) return;

		String ref = getAttachmentReference(reference.getContainer(), reference.getId());

		// if we are short-term caching
		if (m_cache != null)
		{
			m_cache.put(ref, attachment, m_cacheSeconds);
		}

		// else thread-local cache
		else
		{
			m_threadLocalManager.set(ref, attachment);
		}
	}

	/**
	 * Read the attachment's body from the database.
	 * 
	 * @param attachment
	 *        The attachment meta data.
	 * @return The attachment's body content as a byte array.
	 */
	protected byte[] getAttachmentBody(Attachment attachment)
	{
		// get the resource from the db
		String sql = "SELECT M.MEDIA" + " FROM SAM_MEDIA_T M " + " WHERE M.MEDIAID = ?";

		Object[] fields = new Object[1];
		fields[0] = attachment.getId();

		// create the body to read into
		byte[] body = new byte[attachment.getLength().intValue()];
		m_sqlService.dbReadBinary(sql, fields, body);

		return body;
	}

	/**
	 * Check the cache for the attachment. Use the short-term cache if enabled, else use the thread-local cache.
	 * 
	 * @param id
	 *        The attachment id.
	 * @return The actual attachment object cached, or null if not.
	 */
	protected AttachmentImpl getCachedAttachment(Reference reference)
	{
		String ref = getAttachmentReference(reference.getContainer(), reference.getId());

		// if we are short-term caching
		if (m_cache != null)
		{
			// if it is in there
			if (m_cache.containsKey(ref))
			{
				return (AttachmentImpl) m_cache.get(ref);
			}
		}

		// otherwise check the thread-local cache
		else
		{
			return (AttachmentImpl) m_threadLocalManager.get(ref);
		}

		return null;
	}

	/**
	 * Add a new attachment, returning the id
	 * 
	 * @param a
	 *        The attachment data
	 * @param body
	 *        The attachment body bytes.
	 * @return The new attachment id.
	 */
	protected String putAttachment(Attachment a, InputStream body, String answerId)
	{
		// ID column? For non sequence db vendors, it is defaulted
		Long id = m_sqlService.getNextSequence("SAM_MEDIA_ID_S", null);

		String statement = "INSERT INTO SAM_MEDIA_T"
				+ " (FILESIZE, MIMETYPE, FILENAME, CREATEDDATE, LASTMODIFIEDDATE, ISLINK, ISHTMLINLINE, DESCRIPTION, CREATEDBY, LASTMODIFIEDBY, STATUS, ITEMGRADINGID"
				+ ((id == null) ? "" : " ,MEDIAID") + " ,MEDIA)" + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?" + ((id == null) ? "" : ",?")
				+ ",?)";
		Object[] fields = new Object[(id == null) ? 12 : 13];
		fields[0] = a.getLength();
		fields[1] = a.getType();
		fields[2] = a.getName();
		fields[3] = a.getTimestamp() != null ? a.getTimestamp() : m_timeService.newTime();
		fields[4] = fields[3];
		fields[5] = new Integer(0);
		fields[6] = new Integer(0);
		fields[7] = "description";
		fields[8] = m_sessionManager.getCurrentSessionUserId();
		fields[9] = fields[8];
		fields[10] = new Integer(1);
		fields[11] = answerId;

		if (id != null)
		{
			fields[12] = id;
			m_sqlService.dbInsert(null, statement, fields, null, body, a.getLength().intValue());
		}
		else
		{
			id = m_sqlService.dbInsert(null, statement, fields, "MEDIAID", body, a.getLength().intValue());
		}

		return id.toString();
	}

	/**
	 * Stream the attachment's body from the database.
	 * 
	 * @param attachment
	 *        The attachment meta data.
	 * @return The attachment's body content in an input stream.
	 */
	protected InputStream streamAttachmentBody(Attachment attachment) throws ServerOverloadException
	{
		// get the resource from the db
		String sql = "SELECT M.MEDIA" + " FROM SAM_MEDIA_T M " + " WHERE M.MEDIAID = ?";

		Object[] fields = new Object[1];
		fields[0] = attachment.getId();

		// get the stream, set expectations that this could be big
		InputStream in = m_sqlService.dbReadBinary(sql, fields, true);

		return in;
	}
}
