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

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.Destination;
import org.muse.ambrosia.api.FileUpload;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiFileUpload...
 */
public class UiFileUpload extends UiController implements FileUpload
{
	/** The decision to control the onEmptyAlert. */
	protected Decision onEmptyAlertDecision = null;

	/** The message for the onEmptyAlert. */
	protected Message onEmptyAlertMsg = null;

	/**
	 * The PropertyReference for encoding and decoding this selection - this is what will be updated with the end-user's uploaded
	 * file.
	 */
	protected PropertyReference propertyReference = null;

	/** The property reference to provide the read only setting. */
	protected PropertyReference readOnlyReference = null;

	/** The message that will provide title to display. */
	protected Message title = null;

	/** The include decision array for the title. */
	protected Decision[] titleIncluded = null;

	/** The tool destination for the upload button. */
	protected Destination uploadDestination = null;

	/** The message for an upload button - if null, no button. */
	protected Message uploadSubmit = null;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		// read only?
		boolean readOnly = false;
		if (this.readOnlyReference != null)
		{
			String value = this.readOnlyReference.read(context, focus);
			if (value != null)
			{
				readOnly = Boolean.parseBoolean(value);
			}
		}

		// alert if empty at submit?
		boolean onEmptyAlert = false;
		if (this.onEmptyAlertMsg != null)
		{
			onEmptyAlert = true;
			if (this.onEmptyAlertDecision != null)
			{
				onEmptyAlert = this.onEmptyAlertDecision.decide(context, focus);
			}
		}

		// get some ids
		int idRoot = context.getUniqueId();
		String id = this.getClass().getSimpleName() + "_" + idRoot;
		String decodeId = "file_" + idRoot;

		PrintWriter response = context.getResponseWriter();

		response.println("<div class=\"ambrosiaFileUpload\">");

		if (onEmptyAlert)
		{
			// this will become visible if a submit happens and the validation fails
			response.println("<div class=\"ambrosiaAlert\" style=\"display:none\" id=\"alert_" + id + "\">"
					+ Validator.escapeHtml(this.onEmptyAlertMsg.getMessage(context, focus)) + "</div>");
		}

		// title
		if ((this.title != null) && (isTitleIncluded(context, focus)))
		{
			response.println("<label for=\"" + id + "\">" + Validator.escapeHtml(this.title.getMessage(context, focus))
					+ "</label><br />");
		}

		// the file chooser
		if (!readOnly)
		{
			response.println("<input type=\"file\" name=\"" + id + "\" id=\"" + id + "\" class=\"upload\" />");

			// the decode directive
			if ((this.propertyReference != null) && (!readOnly))
			{
				response.println("<input type=\"hidden\" name=\"" + decodeId + "\" value =\"" + id + "\" />"
						+ "<input type=\"hidden\" name=\"" + "prop_" + decodeId + "\" value=\""
						+ this.propertyReference.getFullReference(context) + "\" />");
			}

			if (this.uploadSubmit != null)
			{
				// tool destination URL - right where we are
				String href = (String) context.get("sakai.destination.url");
				boolean dflt = false;

				response.println("<input type=\"submit\" " + (dflt ? "class=\"active\"" : "") + " name=\"" + id + "_u" + "\" id=\""
						+ id + "_u" + "\" value=\"" + Validator.escapeHtml(this.uploadSubmit.getMessage(context, focus)) + "\" />");

				// if we have a destination set, encode it
				if (this.uploadDestination != null)
				{
					response.println("<input type=\"hidden\" name =\"" + "destination_" + id + "\" value=\""
							+ this.uploadDestination.getDestination(context, focus) + "\" />");
				}
			}
			
			// for onEmptyAlert, add some client-side validation
			if ((onEmptyAlert)  && (!readOnly))
			{
				context.addValidation("	if (trim(document.getElementById('" + id + "').value) == \"\")\n" + "	{\n"
						+ "		if (document.getElementById('alert_" + id + "').style.display == \"none\")\n" + "		{\n"
						+ "			document.getElementById('alert_" + id + "').style.display = \"\";\n" + "			rv=false;\n" + "		}\n"
						+ "	}\n");
			}

			response.println("</div>");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setOnEmptyAlert(Decision decision, String selector, PropertyReference... references)
	{
		this.onEmptyAlertDecision = decision;
		this.onEmptyAlertMsg = new UiMessage().setMessage(selector, references);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setProperty(PropertyReference propertyReference)
	{
		this.propertyReference = propertyReference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setReadOnly(PropertyReference reference)
	{
		this.readOnlyReference = reference;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setTitleIncluded(Decision... decision)
	{
		this.titleIncluded = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileUpload setUpload(String selector, PropertyReference... references)
	{
		this.uploadSubmit = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * Check if this title is included.
	 * 
	 * @param context
	 *        The Context.
	 * @param focus
	 *        The object focus.
	 * @return true if included, false if not.
	 */
	protected boolean isTitleIncluded(Context context, Object focus)
	{
		if (this.titleIncluded == null) return true;
		for (Decision decision : this.titleIncluded)
		{
			if (!decision.decide(context, focus))
			{
				return false;
			}
		}

		return true;
	}
}
