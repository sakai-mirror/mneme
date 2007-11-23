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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.TypeSpecificAnswer;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InconsistentException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.util.StringUtil;

/**
 * EssayAnswerImpl handles answers for the essay question type.
 */
public class EssayAnswerImpl implements TypeSpecificAnswer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(EssayAnswerImpl.class);

	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The String answer as entered by the user. */
	protected String answerData = null;

	/** Set when the answer has been changed. */
	protected boolean changed = false;

	/** Dependency: ContentHostingService */
	protected ContentHostingService contentHostingService = null;

	/** Dependency: IdManager. */
	protected IdManager idManager = null;

	/** The uploaded file references. */
	List<Reference> uploads = new ArrayList<Reference>();

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 * @param other
	 *        The other to copy.
	 */
	public EssayAnswerImpl(Answer answer, EssayAnswerImpl other)
	{
		this.answer = answer;
		this.answerData = other.answerData;
		this.uploads.addAll(other.uploads);
		this.changed = other.changed;
		this.idManager = other.idManager;
		this.contentHostingService = other.contentHostingService;
	}

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public EssayAnswerImpl(Answer answer, IdManager idManager, ContentHostingService contentHostingService)
	{
		this.answer = answer;
		this.idManager = idManager;
		this.contentHostingService = contentHostingService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearIsChanged()
	{
		this.changed = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone(Answer answer)
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// deep copy
			((EssayAnswerImpl) rv).uploads = new ArrayList<Reference>(this.uploads);

			((EssayAnswerImpl) rv).answer = answer;

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void consolidate(String destination)
	{
		// check for remove
		if (destination.startsWith("STAY_REMOVE:"))
		{
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length == 2)
			{
				for (Iterator i = this.uploads.iterator(); i.hasNext();)
				{
					Reference ref = (Reference) i.next();
					if (ref.getReference().equals(parts[1]))
					{
						i.remove();

						// TODO: actually delete it!

						this.changed = true;
					}
				}
			}
		}
	}

	/**
	 * @return The answerData.
	 */
	public String getAnswerData()
	{
		return this.answerData;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		// there is no auto scoring for essays
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswered()
	{
		return ((((EssayQuestionImpl) this.answer.getQuestion().getTypeSpecificQuestion()).getSubmissionType() == EssayQuestionImpl.SubmissionType.none)
				|| (this.answerData != null) || (!this.uploads.isEmpty()));
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		return this.changed;
	}

	/**
	 * Access the already uploaded items as attachment references.
	 * 
	 * @return The List of References to the already uploaded items.
	 */
	public List<Reference> getUploaded()
	{
		return new ArrayList<Reference>(this.uploads);
	}

	/**
	 * Set the answerData
	 * 
	 * @param answerData
	 */
	public void setAnswerData(String answerData)
	{
		if (!Different.different(this.answerData, answerData)) return;

		this.answerData = answerData;
		this.changed = true;
	}

	/**
	 * Accept a file upload from the user.
	 * 
	 * @param file
	 *        The file.
	 */
	public void setUpload(FileItem file)
	{
		try
		{
			String name = file.getName();
			String type = file.getContentType();
			InputStream body = file.getInputStream();
			long size = file.getSize();

			// detect no file selected
			if ((name == null) || (type == null) || (body == null) || (size == 0))
			{
				if (body != null) body.close();
				return;
			}

			String ref = "/private/";
			// make sure our root area exists (assume private exists)
			assureCollection(ref, AttachmentService.REFERENCE_ROOT_NAME);

			ref += AttachmentService.REFERENCE_ROOT_NAME + "/";
			// make sure the context area exists.
			assureCollection(ref, this.answer.getSubmission().getAssessment().getContext());
			ref += this.answer.getSubmission().getAssessment().getContext() + "/";

			// make sure the submissions area exists
			assureCollection(ref, "submissions");
			ref += "submissions" + "/";

			// make sure the submission's area exists
			assureCollection(ref, this.answer.getSubmission().getId());
			ref += this.answer.getSubmission().getId() + "/";

			// make sure the submission's answer area exists
			assureCollection(ref, this.answer.getId());
			ref += this.answer.getId() + "/";

			// to support the same name twice, use a unique id for this one
			String uid = this.idManager.createUuid();
			assureCollection(ref, uid);
			ref += uid + "/";

			// add the file name
			ref += name;

			// write the resource
			try
			{
				ContentResourceEdit edit = contentHostingService.addResource(ref);
				edit.setContent(body);
				edit.setContentType(type);
				ResourcePropertiesEdit props = edit.getPropertiesEdit();

				// set the alternate reference root so we get all requests
				props.addProperty(ContentHostingService.PROP_ALTERNATE_REFERENCE, AttachmentService.REFERENCE_ROOT);

				props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

				contentHostingService.commitResource(edit);
				ref = edit.getReference(ContentHostingService.PROP_ALTERNATE_REFERENCE);
			}
			catch (PermissionException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}
			catch (IdUsedException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}
			catch (IdInvalidException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}
			catch (InconsistentException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}
			catch (ServerOverloadException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}
			catch (OverQuotaException e2)
			{
				M_log.warn("setUpload: creating our content: " + e2.toString());
			}

			Reference reference = EntityManager.newReference(ref);
			this.uploads.add(reference);

			this.changed = true;
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * Assure that a collection with this name exists in the container collection: create it if it is missing.
	 * 
	 * @param container
	 *        The full path of the container collection.
	 * @param name
	 *        The collection name to check and create.
	 */
	protected void assureCollection(String container, String name)
	{
		try
		{
			contentHostingService.getCollection(container + name);
		}
		catch (IdUnusedException e)
		{
			try
			{
				ContentCollectionEdit edit = contentHostingService.addCollection(container + name);
				ResourcePropertiesEdit props = edit.getPropertiesEdit();

				// set the alternate reference root so we get all requests
				props.addProperty(ContentHostingService.PROP_ALTERNATE_REFERENCE, AttachmentService.REFERENCE_ROOT);

				props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

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
}
