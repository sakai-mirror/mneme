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
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiNavigation presents a navigation control (button or text link) to the user. The result of the press is a navigation to some tool destination.
 */
public class UiNavigation extends UiController implements Navigation
{
	/** Message to form the access key. */
	protected Message accessKey = null;

	/** The icon for the "cancel" in a confirm. */
	protected String confirmCancelIcon = null;

	/** The message for the "cancel" in a confirm. */
	protected Message confirmCancelMsg = null;

	/** Decision to make a two step (confirm) button. */
	protected Decision confirmDecision = null;

	/** The confirm message. */
	protected Message confirmMsg = null;

	/** The default decision array. */
	protected Decision[] defaultDecision = null;

	/** If set to true, this is a default decision - overrides the defaultDecision set. */
	protected boolean defaultSet = false;

	/** The message selector for the button description. */
	protected Message description = null;

	/** The tool destination for this navigation. */
	protected Destination destination = null;

	/** The disabled decision array. */
	protected Decision[] disabledDecision = null;

	/** Full URL to the icon. */
	protected String icon = null;

	/** Icon placement: left or right. */
	protected IconStyle iconStyle = IconStyle.left;

	/** The display style. */
	protected Style style = Style.link;

	/** If true, we need to submit the form on the press. */
	protected boolean submit = false;

	/** The message selector for the button title. */
	protected Message title = null;

	/** Decision to force form validation when pressed. */
	protected Decision validationDecision = null;

	/**
	 * {@inheritDoc}
	 */
	public String getDestination(Context context, Object focus)
	{
		if (this.destination == null) return null;

		// included?
		if (!isIncluded(context, focus)) return null;

		// disabled?
		if (isDisabled(context, focus)) return null;

		return this.destination.getDestination(context, focus);
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		// disabled?
		boolean disabled = isDisabled(context, focus);

		// generate id
		// if this component has a name-id, and it has already been registered in the context, we are an alias:
		// use the registered value as id and skip our script / confirm generation.
		String id = null;
		boolean isAliasRendering = false;
		if (getId() != null)
		{
			id = context.getRegistration(getId());
			if (id != null) isAliasRendering = true;
		}

		if (id == null)
		{
			id = this.getClass().getSimpleName() + context.getUniqueId();

			// register if we have a name so any alias can use this same id
			if (getId() != null)
			{
				context.register(getId(), id);
			}
		}

		// is this a default choice?
		boolean dflt = isDefault(context, focus);

		// validate?
		boolean validate = false;
		if (this.validationDecision != null)
		{
			validate = this.validationDecision.decide(context, focus);
		}

		// title
		String title = "";
		if (this.title != null)
		{
			title = this.title.getMessage(context, focus);
		}

		// access key
		String accessKey = null;
		if (this.accessKey != null)
		{
			accessKey = this.accessKey.getMessage(context, focus);
		}

		// description
		String description = null;
		if (this.description != null)
		{
			description = this.description.getMessage(context, focus);
		}

		// make it a two step / confirm?
		boolean confirm = false;
		if (this.confirmDecision != null)
		{
			confirm = this.confirmDecision.decide(context, focus);
		}

		PrintWriter response = context.getResponseWriter();

		// generate the script and confirm stuff only if not an alias
		if (!isAliasRendering)
		{
			// our action javascript
			if (!disabled)
			{
				StringBuffer script = new StringBuffer();

				script.append("var enabled_" + id + "=" + (confirm ? "false" : "true") + ";\n");
				script.append("function cancel_" + id + "()\n");
				script.append("{\n");
				script.append("    enabled_" + id + "=false;\n");
				script.append("}\n");
				script.append("function act_" + id + "()\n");
				script.append("{\n");

				// enabled check
				script.append("  if (!enabled_" + id + ")\n");
				script.append("  {\n");

				if (confirm)
				{
					script.append("    enabled_" + id + "=true;\n");
					// script.append(" document.getElementById(\"confirm_" + id + "\").style.display=\"\";\n");
					script.append("    showConfirm('confirm_" + id + "');\n");
				}
				script.append("    return;\n");
				script.append("  }\n");

				// submitted already check
				script.append("  if (submitted)\n");
				script.append("  {\n");
				script.append("    return;\n");
				script.append("  }\n");

				if (this.submit)
				{
					// if we are doing validate, enable validation
					if (validate)
					{
						script.append("  enableValidate=true;\n");
					}

					// if we validate, put up the blocker and submit the form
					script.append("  if (validate())\n");
					script.append("  {\n");

					// the blocker
					// script.append(" document.getElementById('blocker').style.display=\"\";\n");

					// set that we submitted already
					script.append("    submitted=true;\n");

					// setup the destination
					script.append("    document." + context.getFormName() + ".destination_.value='"
							+ (this.destination != null ? this.destination.getDestination(context, focus) : "") + "';\n");

					// submit
					script.append("    document." + context.getFormName() + ".submit();\n");
					script.append("  }\n");
				}

				else
				{
					// the blocker
					// script.append(" document.getElementById('blocker').style.display=\"\";\n");

					// set that we submitted already
					script.append("  submitted=true;\n");

					// perform the navigation
					script.append("  document.location=\"" + context.get("sakai.return.url")
							+ (this.destination != null ? this.destination.getDestination(context, focus) : "") + "\";\n");
				}

				script.append("}\n");

				context.addScript(script.toString());
			}

			if (confirm)
			{
				response
						.println("<div class=\"ambrosiaConfirmPanel\" style=\"display:none; left:0px; top:0px; width:340px; height:120px\" id=\"confirm_"
								+ id + "\">");
				response.println("<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"><tr>");
				response.println("<td colspan=\"2\" style=\"padding:1em; white-space:normal; line-height:1em; \" align=\"left\">"
						+ this.confirmMsg.getMessage(context, focus) + "</td>");
				response.println("</tr><tr>");
				response.println("<td style=\"padding:1em\" align=\"left\"><input type=\"button\" value=\""
						+ this.confirmCancelMsg.getMessage(context, focus)
						+ "\" onclick=\"hideConfirm('confirm_"
						+ id
						+ "','cancel_"
						+ id
						+ "()');\" "
						+ ((this.confirmCancelIcon != null) ? "style=\"padding-left:2em; background: #eee url('" + context.get("sakai.return.url")
								+ this.confirmCancelIcon + "') .2em no-repeat;\"" : "") + "/></td>");
				response.println("<td style=\"padding:1em\" align=\"right\"><input type=\"button\" value=\"" + title
						+ "\" onclick=\"hideConfirm('confirm_" + id + "','act_" + id + "()');\" style=\"padding-left:2em; background: #eee url('"
						+ context.get("sakai.return.url") + this.icon + "') .2em no-repeat;\"/></td>");
				response.println("</tr></table></div>");
			}
		}

		switch (this.style)
		{
			case link:
			{
				if ((this.icon != null) && (this.iconStyle == IconStyle.left))
				{
					response.print("<img style=\"vertical-align:text-bottom; padding-right:0.3em;\" src=\"" + context.get("sakai.return.url")
							+ this.icon + "\" />");
				}

				if (!disabled) response.print("<a href=\"#\" onclick=\"act_" + id + "();\">");

				response.print(title);

				if (!disabled) response.print("</a>");

				if ((this.icon != null) && (this.iconStyle == IconStyle.right))
				{
					response.print("<img style=\"vertical-align:text-bottom; padding-left:0.3em;\" src=\"" + context.get("sakai.return.url")
							+ this.icon + "\" />");
				}

				response.println();

				break;
			}

			case button:
			{
				response
						.println("<input type=\"button\" "
								+ (dflt ? "class=\"active\"" : "")
								+ " name=\""
								+ id
								+ "\" id=\""
								+ id
								+ "\" value=\""
								+ title
								+ "\""
								+ (disabled ? " disabled=\"disabled\"" : "")
								+ " onclick=\"act_"
								+ id
								+ "();\" "
								+ ((accessKey == null) ? "" : "accesskey=\"" + accessKey.charAt(0) + "\" ")
								+ ((description == null) ? "" : "title=\"" + Validator.escapeHtml(description) + "\" ")
								+ (((this.icon != null) && (this.iconStyle == IconStyle.left)) ? "style=\"padding-left:2em; background: #eee url('"
										+ context.get("sakai.return.url") + this.icon + "') .2em no-repeat;\"" : "")
								+ (((this.icon != null) && (this.iconStyle == IconStyle.right)) ? "style=\"padding-left:.4em; padding-right:2em; background: #eee url('"
										+ context.get("sakai.return.url") + this.icon + "') right no-repeat;\""
										: "") + "/>");

				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setAccessKey(String selector, PropertyReference... references)
	{
		this.accessKey = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setConfirm(Decision decision, String cancelSelector, String cancelIcon, String msgSelector, PropertyReference... references)
	{
		this.confirmDecision = decision;
		this.confirmCancelMsg = new UiMessage().setMessage(cancelSelector);
		this.confirmCancelIcon = cancelIcon;
		this.confirmMsg = new UiMessage().setMessage(msgSelector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setDefault()
	{
		this.defaultSet = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setDefault(Decision... defaultDecision)
	{
		this.defaultDecision = defaultDecision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setDescription(String selector, PropertyReference... references)
	{
		this.description = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setDestination(Destination destination)
	{
		this.destination = destination;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setDisabled(Decision... decision)
	{
		this.disabledDecision = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setIcon(String icon, IconStyle style)
	{
		this.icon = icon;
		this.iconStyle = style;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setIncluded(Decision... decision)
	{
		super.setIncluded(decision);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setStyle(Navigation.Style style)
	{
		this.style = style;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setSubmit()
	{
		this.submit = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public Navigation setValidation(Decision decision)
	{
		this.validationDecision = decision;
		return this;
	}

	/**
	 * Check if this is a default choice.
	 * 
	 * @param context
	 *        The Context.
	 * @param focus
	 *        The object focus.
	 * @return true if this is a default choice, false if not.
	 */
	protected boolean isDefault(Context context, Object focus)
	{
		if (this.defaultSet) return true;
		if (this.defaultDecision == null) return false;
		for (Decision decision : this.defaultDecision)
		{
			if (!decision.decide(context, focus))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if this is a disabled.
	 * 
	 * @param context
	 *        The Context.
	 * @param focus
	 *        The object focus.
	 * @return true if this is a disabled false if not.
	 */
	protected boolean isDisabled(Context context, Object focus)
	{
		if (this.disabledDecision == null) return false;
		for (Decision decision : this.disabledDecision)
		{
			if (!decision.decide(context, focus))
			{
				return false;
			}
		}

		return true;
	}
}
