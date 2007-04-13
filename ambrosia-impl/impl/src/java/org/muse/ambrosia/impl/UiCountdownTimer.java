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
		response.println("<table id=\"timer_" + id + "\" cellspacing=\"2px\" cellpadding=\"0\" border=\"0\">");
		response.println("<tr><td id=\"current_" + id + "\" style=\"text-align:left;font-size: 0.8em;\">00:00:00</td><tr>");
		response.println("<tr><td><div id=\"holder_" + id + "\" style=\"width:" + this.width
				+ "px; height:12px; border:solid; border-color:#808080; border-width:thin; background-color:#E0E0E0; font-size:1px;\">");
		response.println("<div id=\"bar_" + id + "\" style=\"width:" + this.width
				+ "px; height:12px; background-color:#66CD00; font-size:1px;\"></div>");
		response.println("</div></td></tr>");
		response.println("<tr><td id=\"total_" + id + "\" style=\"text-align:right;font-size: 0.8em;\">00:00:00</td></tr></table>");

		if ((hideText != null) && (showText != null))
		{
			response.println("<input id=\"hideshow_" + id + "\" type=\"button\" onclick=\"hideShow_" + id + "()\" value = \"" + hideText + "\" />");
		}

		// our script
		response.println("<script language=\"JavaScript\">");

		// date to expire
		response.println("var target_" + id + " = 0;");

		// date to warn
		response.println("var warning_" + id + " = 0;");

		// we warned
		response.println("var warned_" + id + " = false;");

		// the setTimeout object
		response.println("var timeout_" + id + " = 0;");

		// the fixed width (px) of the display bar
		response.println("var holderWidth_" + id + " = " + this.width + ";");

		// time till expire
		response.println("var tillExpire_" + id + " = " + tillExpire + ";");

		// total duration of assessment (ms)
		response.println("var duration_" + id + " = " + duration + ";");

		// time from exipre for warning
		response.println("var warnZone_" + id + " = " + this.warn + ";");

		// text to go with the total display
		response.println("var durationText_" + id + " = \"" + durationText + "\";");

		// text to go with the remaining display
		response.println("var remainingText_" + id + " = \"" + remainingText + "\";");

		// text for hide
		response.println("var hideText_" + id + " = \"" + hideText + "\";");

		// text for show
		response.println("var showText_" + id + " = \"" + showText + "\";");

		response.println("function start_" + id + "()");
		response.println("{");
		// time out in 4 minutes
		response.println("	target_" + id + " = new Date();");
		response.println("	target_" + id + ".setTime(target_" + id + ".getTime() + tillExpire_" + id + ");");

		// warning when 60 seconds to go
		response.println("	warning_" + id + " = new Date()");
		response.println("	warning_" + id + ".setTime(target_" + id + ".getTime() - warnZone_" + id + ");");

		response.println("	document.getElementById('total_" + id + "').firstChild.nodeValue = durationText_" + id + ";");

		response.println("	document.getElementById('current_" + id + "').firstChild.nodeValue = remainingText_" + id + " + fmtTime_" + id + "(duration_" + id + ");");
		response.println("	document.getElementById('bar_" + id + "').style.width = holderWidth_" + id + " + \"px\";");

		// if we are disabled, never call update
		if (!isDisabled(context, focus))
		{
			response.println("	update_" + id + "();");
		}
		response.println("}");

		response.println("function end_" + id + "()");
		response.println("{");
		response.println("	clearTimeout(timeout_" + id + ");");
		response.println("	timeout_" + id + " = 0;");
		response.println("}");

		response.println("function update_" + id + "()");
		response.println("{");
		response.println("	timeout_" + id + " = 0;");
		response.println("	var now = new Date();");
		response.println("	if (now >= target_" + id + ")");
		response.println("	{");
		response.println("		expire_" + id + "();");
		response.println("	}");
		response.println("	else");
		response.println("	{");
		response.println("		if (now >= warning_" + id + ")");
		response.println("		{");
		response.println("			warn_" + id + "();");
		response.println("		}");
		response.println("		format_" + id + "();");
		response.println("		timeout_" + id + " = setTimeout(\"update_" + id + "()\", 1000);");
		response.println("	}");
		response.println("}");

		response.println("function format_" + id + "()");
		response.println("{");
		response.println("	var diff = target_" + id + " - new Date();");
		response.println("	document.getElementById('current_" + id + "').firstChild.nodeValue = remainingText_" + id + " + fmtTime_" + id
				+ "(target_" + id + " - new Date());");
		response.println("	var pct = diff / duration_" + id + ";");
		response.println("	document.getElementById('bar_" + id + "').style.width = (holderWidth_" + id + " * pct) + \"px\";");
		response.println("}");

		response.println("function fmtTime_" + id + "(diff)");
		response.println("{");
		response.println("	var secs = Math.floor(diff / 1000);");
		response.println("	var mins = Math.floor(secs / 60);");
		response.println("	var hours = Math.floor(mins / 60);");
		response.println("	mins = mins - (hours * 60);");
		response.println("	secs = secs - (hours * 60 * 60) - (mins * 60);");
		response.println("	return d2_" + id + "(hours) + \":\" + d2_" + id + "(mins) + \":\" + d2_" + id + "(secs);");
		response.println("}");

		response.println("function d2_" + id + "(value)");
		response.println("{");
		response.println("	if (value < 10)");
		response.println("	{");
		response.println("		return \"0\" + value;");
		response.println("	}");
		response.println("	return \"\" + value;");
		response.println("}");

		response.println("function expire_" + id + "()");
		response.println("{");
		response.println("	document.getElementById('holder_" + id + "').style.backgroundColor=\"#ff0000\";");
		if (this.destination != null)
		{
			// submit the form, encoding the destination (if any) in the "destination_" hidden field
			response.println("	document." + context.getFormName() + ".destination_.value='"
					+ (this.destination != null ? this.destination.getDestination(context, focus) : "") + "';");
			response.println("	document." + context.getFormName() + ".submit();");
		}
		response.println("}");

		response.println("function warn_" + id + "()");
		response.println("{");
		response.println("	if (!warned_" + id + ")");
		response.println("	{");
		response.println("		warned_" + id + " = true;");
		response.println("		document.getElementById('bar_" + id + "').style.backgroundColor=\"#ffff33\";");
		response.println("	}");
		response.println("}");

		if ((hideText != null) && (showText != null))
		{
			response.println("function hideShow_" + id + "()");
			response.println("{");
			response.println("	if (document.getElementById('timer_" + id + "').style.display == \"none\")");
			response.println("	{");
			response.println("		document.getElementById('timer_" + id + "').style.display = \"\";");
			response.println("		document.getElementById('hideshow_" + id + "').value = hideText_" + id + ";");
			response.println("	}");
			response.println("	else");
			response.println("	{");
			response.println("		document.getElementById('timer_" + id + "').style.display = \"none\";");
			response.println("		document.getElementById('hideshow_" + id + "').value = showText_" + id + ";");
			response.println("	}");
			response.println("}");
		}

		response.println("start_" + id + "();");

		response.println("</script>");
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

	/** The disabled decision array. */
	protected Decision[] disabledDecision = null;

	/**
	 * {@inheritDoc}
	 */
	public CountdownTimer setDisabled(Decision... decision)
	{
		this.disabledDecision = decision;
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
