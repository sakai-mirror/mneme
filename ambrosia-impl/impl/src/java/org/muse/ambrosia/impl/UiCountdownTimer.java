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
import org.muse.ambrosia.api.CountdownTimer;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.Destination;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.PropertyReference;

/**
 * UiCountdownTimer implements CountdownTimer.
 */
public class UiCountdownTimer extends UiController implements CountdownTimer
{
	/** The tool destination for this navigation. */
	protected Destination destination = null;

	/** The disabled decision array. */
	protected Decision[] disabledDecision = null;

	/** Duration in ms of the entire timer. */
	protected PropertyReference duration = null;

	/** The message selector for the duration text. */
	protected Message durationMessage = null;

	/** The message selector for the hide button text. */
	protected Message hideMessage = null;

	/** The message selector for the remaining text. */
	protected Message remainingMessage = null;

	/** The message selector for the show button text. */
	protected Message showMessage = null;

	/** If we should submit to our destination or not. */
	protected boolean submit = false;

	/** The tight setting. */
	protected boolean tight = false;

	/** Time in ms from now till expire. */
	protected PropertyReference tillExpire = null;

	/** The message selector for the button title. */
	protected Message title = null;

	/** Duration of the warn zone at the end, in ms. */
	protected long warn = 60 * 1000;

	/** Width in pixels of the graphic display. */
	protected int width = 200;

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		// included?
		if (!isIncluded(context, focus)) return;

		PrintWriter response = context.getResponseWriter();

		// generate id
		int id = context.getUniqueId();

		// compute duration
		long duration = 0;
		if (this.duration != null)
		{
			try
			{
				duration = Long.parseLong(this.duration.read(context, focus));
			}
			catch (NumberFormatException e)
			{
			}
			catch (NullPointerException e)
			{
			}
		}

		// get our messages
		String hideText = null;
		if (this.hideMessage != null)
		{
			hideText = this.hideMessage.getMessage(context, focus);
		}
		String showText = null;
		if (this.showMessage != null)
		{
			showText = this.showMessage.getMessage(context, focus);
		}

		String durationText = "";
		if (this.durationMessage != null)
		{
			durationText = this.durationMessage.getMessage(context, focus);
		}
		String remainingText = "";
		if (this.remainingMessage != null)
		{
			remainingText = this.remainingMessage.getMessage(context, focus);
		}

		// compute tillExpire
		long tillExpire = 0;
		if (this.tillExpire != null)
		{
			try
			{
				tillExpire = Long.parseLong(this.tillExpire.read(context, focus));
			}
			catch (NumberFormatException e)
			{
			}
			catch (NullPointerException e)
			{
			}
		}

		// our elements
		response.println("<table id=\"timer_" + id + "\" cellspacing=\"" + (this.tight ? "0" : "2px") + "\" cellpadding=\"0\" border=\"0\">");
		response.println("<tr><td id=\"current_" + id + "\" style=\"text-align:left;font-size:0.8em;" + (this.tight ? "line-height:0.8em;" : "")
				+ "white-space:nowrap\">00:00:00</td><tr>");
		response.println("<tr><td style=\"margin:0;padding:0;\"><div id=\"holder_" + id + "\" style=\"width:" + this.width
				+ "px;height:12px;border:solid;border-color:#808080;border-width:thin;background-color:#E0E0E0;font-size:1px;\">");
		response
				.println("<div id=\"bar_" + id + "\" style=\"width:" + this.width + "px;height:12px;background-color:#66CD00;font-size:1px;\"></div>");
		response.println("</div></td></tr>");
		response.println("<tr><td id=\"total_" + id + "\" style=\"text-align:right;font-size:0.8em;" + (this.tight ? "line-height:0.8em;" : "")
				+ "white-space:nowrap\">00:00:00</td></tr></table>");

		if ((hideText != null) && (showText != null))
		{
			response.println("<input id=\"hideshow_" + id + "\" type=\"button\" onclick=\"hideShow_" + id + "()\" value = \"" + hideText + "\" />");
		}

		// our script

		// date to expire
		context.addScript("var target_" + id + " = 0;\n");

		// date to warn
		context.addScript("var warning_" + id + " = 0;\n");

		// we warned
		context.addScript("var warned_" + id + " = false;\n");

		// the setTimeout object
		context.addScript("var timeout_" + id + " = 0;\n");

		// the fixed width (px) of the display bar
		context.addScript("var holderWidth_" + id + " = " + this.width + ";\n");

		// time till expire
		context.addScript("var tillExpire_" + id + " = " + tillExpire + ";\n");

		// total duration of assessment (ms)
		context.addScript("var duration_" + id + " = " + duration + ";\n");

		// time from exipre for warning
		context.addScript("var warnZone_" + id + " = " + this.warn + ";\n");

		// text to go with the total display
		context.addScript("var durationText_" + id + " = \"" + durationText + "\";\n");

		// text to go with the remaining display
		context.addScript("var remainingText_" + id + " = \"" + remainingText + "\";\n");

		// text for hide
		context.addScript("var hideText_" + id + " = \"" + hideText + "\";\n");

		// text for show
		context.addScript("var showText_" + id + " = \"" + showText + "\";\n");

		context.addScript("function start_" + id + "()\n");
		context.addScript("{\n");
		// time out in 4 minutes
		context.addScript("	target_" + id + " = new Date();\n");
		context.addScript("	target_" + id + ".setTime(target_" + id + ".getTime() + tillExpire_" + id + ");\n");

		// warning when 60 seconds to go
		context.addScript("	warning_" + id + " = new Date()\n");
		context.addScript("	warning_" + id + ".setTime(target_" + id + ".getTime() - warnZone_" + id + ");\n");

		context.addScript("	document.getElementById('total_" + id + "').firstChild.nodeValue = durationText_" + id + ";\n");

		context.addScript("	document.getElementById('current_" + id + "').firstChild.nodeValue = remainingText_" + id + " + fmtTime_" + id
				+ "(duration_" + id + ");\n");
		context.addScript("	document.getElementById('bar_" + id + "').style.width = holderWidth_" + id + " + \"px\";\n");

		// if we are disabled, never call update
		if (!isDisabled(context, focus))
		{
			context.addScript("	update_" + id + "();\n");
		}
		context.addScript("}\n");

		context.addScript("function end_" + id + "()\n");
		context.addScript("{\n");
		context.addScript("	clearTimeout(timeout_" + id + ");\n");
		context.addScript("	timeout_" + id + " = 0;\n");
		context.addScript("}\n");

		context.addScript("function update_" + id + "()\n");
		context.addScript("{\n");
		context.addScript("	timeout_" + id + " = 0;\n");
		context.addScript("	var now = new Date();\n");
		context.addScript("	if (now >= target_" + id + ")\n");
		context.addScript("	{\n");
		context.addScript("		expire_" + id + "();\n");
		context.addScript("	}\n");
		context.addScript("	else\n");
		context.addScript("	{\n");
		context.addScript("		if (now >= warning_" + id + ")\n");
		context.addScript("		{\n");
		context.addScript("			warn_" + id + "();\n");
		context.addScript("		}\n");
		context.addScript("		format_" + id + "();\n");
		context.addScript("		timeout_" + id + " = setTimeout(\"update_" + id + "()\", 1000);\n");
		context.addScript("	}\n");
		context.addScript("}\n");

		context.addScript("function format_" + id + "()\n");
		context.addScript("{\n");
		context.addScript("	var diff = target_" + id + " - new Date();\n");
		context.addScript("	document.getElementById('current_" + id + "').firstChild.nodeValue = remainingText_" + id + " + fmtTime_" + id
				+ "(target_" + id + " - new Date());\n");
		context.addScript("	var pct = diff / duration_" + id + ";\n");
		context.addScript("	document.getElementById('bar_" + id + "').style.width = (holderWidth_" + id + " * pct) + \"px\";\n");
		context.addScript("}\n");

		context.addScript("function fmtTime_" + id + "(diff)\n");
		context.addScript("{\n");
		context.addScript("	var secs = Math.floor(diff / 1000);\n");
		context.addScript("	var mins = Math.floor(secs / 60);\n");
		context.addScript("	var hours = Math.floor(mins / 60);\n");
		context.addScript("	mins = mins - (hours * 60);\n");
		context.addScript("	secs = secs - (hours * 60 * 60) - (mins * 60);\n");
		context.addScript("	return d2_" + id + "(hours) + \":\" + d2_" + id + "(mins) + \":\" + d2_" + id + "(secs);\n");
		context.addScript("}\n");

		context.addScript("function d2_" + id + "(value)\n");
		context.addScript("{\n");
		context.addScript("	if (value < 10)\n");
		context.addScript("	{\n");
		context.addScript("		return \"0\" + value;\n");
		context.addScript("	}\n");
		context.addScript("	return \"\" + value;\n");
		context.addScript("}\n");

		context.addScript("function expire_" + id + "()\n");
		context.addScript("{\n");
		context.addScript("	document.getElementById('holder_" + id + "').style.backgroundColor=\"#ff0000\";\n");
		if (this.destination != null)
		{
			if (this.submit)
			{
				// submit the form, encoding the destination (if any) in the "destination_" hidden field
				context.addScript("	document." + context.getFormName() + ".destination_.value='"
						+ (this.destination != null ? this.destination.getDestination(context, focus) : "") + "';\n");
				context.addScript("	document." + context.getFormName() + ".submit();\n");
			}

			else
			{
				context.addScript("	document.location=\"" + context.get("sakai.return.url")
						+ (this.destination != null ? this.destination.getDestination(context, focus) : "") + "\";\n");
			}
		}
		context.addScript("}\n");

		context.addScript("function warn_" + id + "()\n");
		context.addScript("{\n");
		context.addScript("	if (!warned_" + id + ")\n");
		context.addScript("	{\n");
		context.addScript("		warned_" + id + " = true;\n");
		context.addScript("		document.getElementById('bar_" + id + "').style.backgroundColor=\"#ffff33\";\n");
		context.addScript("	}\n");
		context.addScript("}\n");

		if ((hideText != null) && (showText != null))
		{
			context.addScript("function hideShow_" + id + "()\n");
			context.addScript("{\n");
			context.addScript("	if (document.getElementById('timer_" + id + "').style.display == \"none\")\n");
			context.addScript("	{\n");
			context.addScript("		document.getElementById('timer_" + id + "').style.display = \"\";\n");
			context.addScript("		document.getElementById('hideshow_" + id + "').value = hideText_" + id + ";\n");
			context.addScript("	}\n");
			context.addScript("	else\n");
			context.addScript("	{\n");
			context.addScript("		document.getElementById('timer_" + id + "').style.display = \"none\";\n");
			context.addScript("		document.getElementById('hideshow_" + id + "').value = showText_" + id + ";\n");
			context.addScript("	}\n");
			context.addScript("}\n");
		}

		context.addScript("start_" + id + "();\n");
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setDisabled(Decision... decision)
	{
		this.disabledDecision = decision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setDuration(PropertyReference duration)
	{
		this.duration = duration;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setDurationMessage(String selector, PropertyReference... references)
	{
		this.durationMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setExpireDestination(Destination destination)
	{
		this.destination = destination;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setHideMessage(String selector, PropertyReference... references)
	{
		this.hideMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setIncluded(Decision... decision)
	{
		super.setIncluded(decision);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setRemainingMessage(String selector, PropertyReference... references)
	{
		this.remainingMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setShowMessage(String selector, PropertyReference... references)
	{
		this.showMessage = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setSubmit()
	{
		this.submit = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setTight()
	{
		this.tight = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setTimeTillExpire(PropertyReference time)
	{
		this.tillExpire = time;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setTitle(String selector, PropertyReference... references)
	{
		this.title = new UiMessage().setMessage(selector, references);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setWarnDuration(long duration)
	{
		this.warn = duration;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setWidth(int width)
	{
		this.width = width;
		return this;
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
