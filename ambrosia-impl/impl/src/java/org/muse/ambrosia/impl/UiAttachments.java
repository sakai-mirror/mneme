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

package org.muse.ambrosia.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sakaiproject.content.cover.ContentTypeImageService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.muse.ambrosia.api.Attachments;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiAttachments...
 */
public class UiAttachments extends UiController implements Attachments
{
	/** The attachments reference. */
	protected PropertyReference attachments = null;

	/** The context name for the currently iterated attachment. */
	protected String iteratorName = null;

	protected List<Navigation> navigations = new ArrayList<Navigation>();

	/** If set, show only the reference strings, with no checking. */
	protected boolean raw = false;

	/** If set, include the size display. */
	protected boolean size = false;

	/** If set, include a timestamp dispay. */
	protected boolean timestamp = false;

	/** The message that will provide title to display. */
	protected Message title = null;

	/**
	 * {@inheritDoc}
	 */
	public Attachments addNavigation(Navigation navigation)
	{
		navigations.add(navigation);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		// title
		if (this.title != null)
		{
			response.println("<div class=\"ambrosiaAttachmentsTitle\">" + Validator.escapeHtml(this.title.getMessage(context, focus)) + "</div>");
		}

		response.println("<ul class=\"ambrosiaAttachmentsList\">");

		// attachments
		if (this.attachments != null)
		{
			// get the attachments collection
			Object a = this.attachments.readObject(context, focus);

			if ((a != null) && (a instanceof Collection))
			{
				for (Object r : (Collection) a)
				{
					if (r instanceof Reference)
					{
						renderReference(context, response, (Reference) r);
					}
				}
			}

			// or array
			else if ((a != null) && (a.getClass().isArray()))
			{
				for (Object r : (Object[]) a)
				{
					if (r instanceof Reference)
					{
						renderReference(context, response, (Reference) r);
					}
				}
			}
		}

		response.println("</ul>");
	}

	/**
	 * {@inheritDoc}
	 */
	public Attachments setAttachments(PropertyReference attachments, String name)
	{
		this.attachments = attachments;
		this.iteratorName = name;

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attachments setRaw(boolean setting)
	{
		this.raw = setting;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attachments setSize(boolean setting)
	{
		this.size = setting;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attachments setTimestamp(boolean setting)
	{
		this.timestamp = setting;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Attachments setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * Render one reference.
	 * 
	 * @param context
	 *        The context.
	 * @param response
	 *        The output stream.
	 * @param ref
	 *        The reference.
	 */
	protected void renderReference(Context context, PrintWriter response, Reference ref)
	{
		// place the context item
		if (this.iteratorName != null)
		{
			// normally, we would use this.iteratorReference.getEncoding(context, ref), which would use ref's id as it's selector...
			// not good for a Reference -ggolden
			context.put(this.iteratorName, ref, this.attachments.getFullReference(context) + ".[" + ref.getReference() + "]");
		}

		// for raw, print only the reference
		if (this.raw)
		{
			response.print("<li>" + Validator.escapeHtml(ref.getReference()) + "</li>");
		}

		// otherwise format it
		else
		{
			// if we can't get the properties, assume that the attachment is to a deleted entity and skip it
			ResourceProperties props = ref.getProperties();
			if (props != null)
			{
				try
				{
					// for folders
					if (props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION))
					{
						response.print("<li><img src = \"/library/" + ContentTypeImageService.getContentTypeImage("folder")
								+ "\" border=\"0\" />");
					}

					// otherwise lookup the icon from the mime type
					else
					{
						String type = props.getProperty(ResourceProperties.PROP_CONTENT_TYPE);
						response.print("<li><img src = \"/library/image/" + ContentTypeImageService.getContentTypeImage(type)
								+ "\" border=\"0\" alt=\"" + type + "\"/>");
					}

					// the link
					response.print("<a href=\"" + ref.getUrl() + "\" target=\"_blank\" title=\""
							+ Validator.escapeHtml(props.getPropertyFormatted("DAV:displayname")) + "\">"
							+ Validator.escapeHtml(props.getPropertyFormatted("DAV:displayname")) + "</a>");

					// size
					if (this.size && (!props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION)))
					{
						response.print(" (" + props.getPropertyFormatted(ResourceProperties.PROP_CONTENT_LENGTH) + ")");
					}

					// timestamp
					if (this.timestamp && (!props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION)))
					{
						response.print(" (" + props.getPropertyFormatted(ResourceProperties.PROP_MODIFIED_DATE) + ")");
					}

					// navigations
					if (!this.navigations.isEmpty())
					{
						response.print("<div class=\"ambrosiaUnderNav\" />");
						for (Navigation navigation : this.navigations)
						{
							navigation.render(context, ref);
						}
						response.print("</div>");
					}

					response.println("</li>");
				}
				catch (EntityPropertyNotDefinedException e)
				{
				}
				catch (EntityPropertyTypeException e)
				{
				}
			}
		}

		// remove the context item
		if (this.iteratorName != null)
		{
			context.remove(this.iteratorName);
		}
	}
}
