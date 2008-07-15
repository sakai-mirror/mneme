/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.Translation;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.EntityTransferrer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * MnemeServiceImpl implements MnemeService
 */
public class MnemeTransferServiceImpl implements EntityTransferrer, EntityProducer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(MnemeTransferServiceImpl.class);

	/** Dependency: AssessmentService */
	protected AssessmentService assessmentService = null;

	/** Dependency: AttachmentService */
	protected AttachmentService attachmentService = null;

	/** Dependency: EntityManager. */
	protected EntityManager entityManager = null;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager threadLocalManager = null;

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
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		// TODO: localize
		return "Test Center";
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// entity producer registration (note: there is no reference root since we do no entities)
			entityManager.registerEntityProducer(this, "/mneme-NEVER");

			M_log.info("init():");
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
	public String[] myToolIds()
	{
		String[] toolIds = {"sakai.mneme"};
		return toolIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		return false;
	}

	/**
	 * Dependency: AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		assessmentService = service;
	}

	/**
	 * Dependency: AttachmentService.
	 * 
	 * @param service
	 *        The AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		attachmentService = service;
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
	 * Dependency: PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		poolService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		questionService = service;
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
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void transferCopyEntities(String fromContext, String toContext, List ids)
	{
		if (M_log.isDebugEnabled()) M_log.debug("copy from: " + fromContext + " to: " + toContext);

		// security check
		if (!this.securityService.checkSecurity(sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, toContext))
		{
			return;
		}

		// map from old pool ids to new ids
		Map<String, String> pidMap = new HashMap<String, String>();

		// map from old question ids to new ids
		Map<String, String> qidMap = new HashMap<String, String>();

		// get all pools in the context
		List<Pool> pools = this.poolService.findPools(fromContext, null, null);

		// get all the assessments
		List<Assessment> assessments = this.assessmentService.getContextAssessments(fromContext, null, Boolean.FALSE);

		// collect media references
		Set<String> refs = new HashSet<String>();

		for (Pool pool : pools)
		{
			List<String> qids = ((PoolImpl) pool).getAllQuestionIds(null, null);
			for (String qid : qids)
			{
				Question question = this.questionService.getQuestion(qid);
				// for (Reference attachment : question.getPresentation().getAttachments())
				// {
				// refs.add(attachment.getReference());
				// }
				refs.addAll(this.attachmentService.harvestAttachmentsReferenced(question.getPresentation().getText(), true));
				refs.addAll(this.attachmentService.harvestAttachmentsReferenced(question.getHints(), true));
				refs.addAll(this.attachmentService.harvestAttachmentsReferenced(question.getFeedback(), true));
				for (String data : question.getTypeSpecificQuestion().getData())
				{
					refs.addAll(this.attachmentService.harvestAttachmentsReferenced(data, true));
				}
			}
		}

		for (Assessment assessment : assessments)
		{
			// get this assessment's media refs instructions, part instructions, attachments
			// for (Reference attachment : assessment.getPresentation().getAttachments())
			// {
			// refs.add(attachment.getReference());
			// }
			refs.addAll(this.attachmentService.harvestAttachmentsReferenced(assessment.getPresentation().getText(), true));
			refs.addAll(this.attachmentService.harvestAttachmentsReferenced(assessment.getSubmitPresentation().getText(), true));

			for (Part part : assessment.getParts().getParts())
			{
				// for (Reference attachment : part.getPresentation().getAttachments())
				// {
				// refs.add(attachment.getReference());
				// }
				refs.addAll(this.attachmentService.harvestAttachmentsReferenced(part.getPresentation().getText(), true));
			}
		}

		// copy the attachments, creating translations
		List<Translation> translations = new ArrayList<Translation>();
		for (String refString : refs)
		{
			// move the referenced attachment into our docs area in this context
			Reference ref = this.attachmentService.getReference(refString);
			Reference attachment = this.attachmentService.addAttachment(AttachmentService.MNEME_APPLICATION, toContext, AttachmentService.DOCS_AREA,
					false, ref);
			if (attachment != null)
			{
				// make the translation
				Translation t = new TranslationImpl(ref.getReference(), attachment.getReference());
				translations.add(t);
			}
		}

		// copy each pool, with all questions
		for (Pool pool : pools)
		{
			Pool newPool = ((PoolServiceImpl) this.poolService).doCopyPool(toContext, pool, false, qidMap, false, translations);
			pidMap.put(pool.getId(), newPool.getId());
		}

		// copy each assessment
		for (Assessment assessment : assessments)
		{
			((AssessmentServiceImpl) this.assessmentService).doCopyAssessment(toContext, assessment, pidMap, qidMap, false, translations);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void transferCopyEntities(String fromContext, String toContext, List ids, boolean cleanup)
	{
		// TODO: implement cleanup?
		transferCopyEntities(fromContext, toContext, ids);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}
}
