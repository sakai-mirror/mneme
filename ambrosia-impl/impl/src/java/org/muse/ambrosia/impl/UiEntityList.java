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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.api.EntityList;
import org.muse.ambrosia.api.EntityListColumn;
import org.muse.ambrosia.api.Footnote;
import org.muse.ambrosia.api.Message;
import org.muse.ambrosia.api.Navigation;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.Validator;

/**
 * UiEntityList presents a multi-column multi-item listing of entites from the model.<br />
 * setEntityReferemce() sets the reference to the Collection of model items, one for each row.<br />
 * addColumn() sets the columns, each a UiPropertyColumn, that references some selector of the entities in the model. Columns may be sortable.<br />
 * setSelection() sets the SelectionController. If set, then each row can be selected by the user. When selected, the identity of the item is reported
 * as part of the tool destination of the user request.<br />
 * setEmptyTitle() sets an alternate title to use if the list is empty.<br />
 * setIncluded() establishes include control over the entire list.
 */
public class UiEntityList extends UiController implements EntityList
{
	/** Columns for this list. */
	protected List<EntityListColumn> columns = new ArrayList<EntityListColumn>();

	/** Text message to use if there are no items to show in the list. */
	protected Message emptyTitle = null;

	/** A single decision for each possible heading - order matches that in headingMessages. */
	protected List<Decision> headingDecisions = new ArrayList<Decision>();

	/** A message for each possible heading - order matches that in headingDecisions. */
	protected List<Message> headingMessages = new ArrayList<Message>();

	/** A navigation for each possible heading - order matches that in headingDecisions. */
	protected List<Navigation> headingNavigations = new ArrayList<Navigation>();

	/** The inclusion decision for each entity. */
	protected Decision included = null;

	/** The context name for the current iteration object. */
	protected String iteratorName = null;

	/** The reference to an entity to iterate over. */
	protected PropertyReference iteratorReference = null;

	/** Rendering style. */
	protected Style style = Style.flat;

	/** The message for the title. */
	protected Message title = null;

	/** The include decision array for the title. */
	protected Decision[] titleIncluded = null;

	/**
	 * {@inheritDoc}
	 */
	public EntityList addColumn(EntityListColumn column)
	{
		this.columns.add(column);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList addHeading(Decision decision, Navigation navigation)
	{
		this.headingDecisions.add(decision);
		this.headingMessages.add(null);
		this.headingNavigations.add(navigation);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList addHeading(Decision decision, String selector, PropertyReference... properties)
	{
		this.headingDecisions.add(decision);
		this.headingMessages.add(new UiMessage().setMessage(selector, properties));
		this.headingNavigations.add(null);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(Context context, Object focus)
	{
		PrintWriter response = context.getResponseWriter();

		// included?
		if (!isIncluded(context, focus)) return;

		// get an id
		int idRoot = context.getUniqueId();

		// the data
		Collection data = (Collection) this.iteratorReference.readObject(context, focus);
		boolean empty = ((data == null) || (data.isEmpty()));

		// columns one time text
		for (EntityListColumn c : this.columns)
		{
			// included?
			if (!c.included(context)) continue;

			String text = c.getPrefixText(context, focus, idRoot);
			if (text != null)
			{
				response.println(text);
			}
		}

		// title, if there is one and there is data
		if ((this.title != null) && (!empty) && (isTitleIncluded(context, focus)))
		{
			response.println("<div class =\"ambrosiaInstructions\">" + Validator.escapeHtml(this.title.getMessage(context, focus)) + "</div>");
		}

		// empty title, if there is no data
		if ((this.emptyTitle != null) && empty)
		{
			response.println("<div class =\"ambrosiaInstructions\">" + Validator.escapeHtml(this.emptyTitle.getMessage(context, focus)) + "</div>");
		}

		// start the table
		response.println("<table class=\"ambrosiaEntityList "
				+ ((this.style == Style.flat) ? "ambrosiaEntityListFlat" : "ambrosiaEntityListForm") + "\" cellpadding=\"0\" cellspacing=\"0\" >");

		// columns headers
		int cols = 0;
		response.println("<thead><tr>");
		for (EntityListColumn c : this.columns)
		{
			// included?
			if (!c.included(context)) continue;

			cols++;

			Message title = c.getTitle();
			if (title != null)
			{
				// if this is the sort column
				if ((c.getSortingDecision() != null) && (c.getSortingDecision().decide(context, focus)))
				{
					// show the asc or desc... each a nav to the sort asc or desc
					boolean asc = true;
					if ((c.getSortingAscDecision() != null) && (!c.getSortingAscDecision().decide(context, focus)))
					{
						asc = false;
					}

					String icon = null;
					String iconAlt = null;
					if (asc)
					{
						icon = c.getSortAscIcon();
						if (c.getSortAscMsg() != null) iconAlt = c.getSortAscMsg().getMessage(context, focus);
					}
					else
					{
						icon = c.getSortDescIcon();
						if (c.getSortDescMsg() != null) iconAlt = c.getSortDescMsg().getMessage(context, focus);
					}

					String destination = null;
					if (asc)
					{
						// we are already ascending, so encode the descending destination
						if (c.getSortDestinationDesc() != null) destination = c.getSortDestinationDesc().getDestination(context, focus);
					}
					else
					{
						// we are already descending, so encode the ascending destination
						if (c.getSortDestinationAsc() != null) destination = c.getSortDestinationAsc().getDestination(context, focus);
					}

					// link to the dest, with the title and the icon
					String href = context.get("sakai.return.url") + destination;
					response.println("<th scope=\"col\""
							+ (c.getCentered() ? " style=\"text-align:center\"" : "")
							+ "><a href=\""
							+ href
							+ "\">"
							+ Validator.escapeHtml(title.getMessage(context, focus))
							+ ((icon != null) ? ("&nbsp;<img src=\"" + context.get("sakai.return.url") + icon + "\""
									+ ((iconAlt != null) ? ("alt=\"" + Validator.escapeHtml(iconAlt) + "\"") : "") + " />") : "") + "</a></th>");
				}

				// not currently sorting... can we sort?
				else if ((c.getSortingDecision() != null) && (c.getSortingAscDecision() != null) && (c.getSortDestinationAsc() != null)
						&& (c.getSortDestinationDesc() != null))
				{
					// link to asc dest
					String href = context.get("sakai.return.url") + c.getSortDestinationAsc().getDestination(context, focus);
					response.println("<th scope=\"col\"" + (c.getCentered() ? " style=\"text-align:center\"" : "") + "><a href=\"" + href + "\">"
							+ Validator.escapeHtml(title.getMessage(context, focus)) + "</a></th>");
				}

				// no sort
				else
				{
					response.println("<th scope=\"col\"" + (c.getCentered() ? " style=\"text-align:center\"" : "") + ">"
							+ Validator.escapeHtml(title.getMessage(context, focus)) + "</th>");
				}
			}
		}
		response.println("</tr></thead>");

		// keep track of footnotes we need to display after the list, mapped to the footmark used in the columns
		Map<Footnote, String> footnotes = new HashMap<Footnote, String>();

		// The mark characters for footnotes... TODO: better? -ggolden
		String footnoteMarks = "*^@$&!#";

		// track the row number (0 based)
		int row = -1;

		// data
		if (!empty)
		{
			int index = -1;
			for (Object entity : data)
			{
				index++;

				// place the context item
				if (this.iteratorName != null)
				{
					context.put(this.iteratorName, entity, this.iteratorReference.getEncoding(context, entity, index));
				}

				// check if this entity is to be included
				if ((this.included != null) && (!this.included.decide(context, entity))) continue;

				// track the row number
				row++;

				// insert any heading that applies, each as a separate row
				int h = 0;
				for (Decision headingDecision : this.headingDecisions)
				{
					if (headingDecision.decide(context, entity))
					{
						Message headingMessage = this.headingMessages.get(h);
						if (headingMessage != null)
						{
							String heading = headingMessage.getMessage(context, entity);
							if (heading != null)
							{
								response.println("<tr><td style=\"padding:1em;\" colspan=\"" + cols + "\">" + heading + "</td></tr>");
							}
						}

						else
						{
							Navigation nav = this.headingNavigations.get(h);
							if (nav != null)
							{
								response.print("<tr><td style=\"padding:1em;\" colspan=\"" + cols + "\">");
								nav.render(context, entity);
								response.println("</td></tr>");
							}
						}
					}
					h++;
				}

				response.println("<tr>");
				for (EntityListColumn c : this.columns)
				{
					// included?
					if (!c.included(context)) continue;

					response.print("<td style=\"");
					if (c.getWidth() != null)
					{
						response.print("width:" + c.getWidth().toString() + "px;");
					}
					else if (c.getWidthEm() != null)
					{
						response.print("width:" + c.getWidthEm().toString() + "em;");
					}
					if (c.getIsNoWrap())
					{
						response.print("white-space:nowrap;");
					}
					if (c.getCentered())
					{
						response.print("text-align:center;");
					}
					response.print("vertical-align:middle;\">");

					// if the entity is to be included in this column
					if (c.getIsEntityIncluded(context, entity))
					{
						// get our navigation anchor href, and if we are doing selection or not for this entity
						String href = c.getEntityNavigationDestination(context, entity);
						if (href != null)
						{
							// add in the return URL root
							href = context.get("sakai.return.url") + href;
						}

						// if we are linking
						if (href != null)
						{
							response.print("<a style=\"text-decoration:none !important\" href=\"" + href + "\">");
						}

						// get the column's value for display
						String value = c.getDisplayText(context, entity, row, idRoot);

						// alert?
						boolean alert = c.alert(context, entity);

						// the display
						if (alert) response.print("<span class=\"ambrosiaAlertColor\">");

						if (value != null) response.print(value);

						if (alert) response.print("</span>");

						if (href != null)
						{
							response.print("</a>");
						}

						// footnote?
						for (Footnote footnote : c.getFootnotes())
						{
							if (footnote.apply(context, entity))
							{
								// have we dont this one yet? Add it if needed
								String mark = footnotes.get(footnote);
								if (mark == null)
								{
									mark = footnoteMarks.substring(0, 1);
									footnoteMarks = footnoteMarks.substring(1);
									footnotes.put(footnote, mark);
								}

								// mark the output
								response.print(" " + mark);
							}
						}

						// navigations
						if (!c.getNavigations().isEmpty())
						{
							response.print("<div class=\"ambrosiaUnderNav\" style=\"line-height: 1em;\">");
							for (Navigation navigation : c.getNavigations())
							{
								navigation.render(context, entity);
							}
							response.print("</div>");
						}
					}

					// otherwise show a message
					else if (c.getNotIncludedText() != null)
					{
						response.print(context.getMessages().getString(c.getNotIncludedText()));
					}

					response.println("</td>");
				}
				response.println("</tr>");

				// remove the context item
				if (this.iteratorName != null)
				{
					context.remove(this.iteratorName);
				}
			}
		}

		response.println("</table>");

		// columns one time text
		for (EntityListColumn c : this.columns)
		{
			// included?
			if (!c.included(context)) continue;

			String text = c.getOneTimeText(context, focus, idRoot, row + 1);
			if (text != null)
			{
				response.println(text);
			}
		}

		// footnotes
		for (Footnote f : footnotes.keySet())
		{
			if (f.getText() != null)
			{
				response.println("<div class =\"ambrosiaInstructions\">" + footnotes.get(f) + " "
						+ Validator.escapeHtml(f.getText().getMessage(context, focus)) + "</div>");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setEmptyTitle(String selector, PropertyReference... properties)
	{
		this.emptyTitle = new UiMessage().setMessage(selector, properties);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setEntityIncluded(Decision inclusionDecision)
	{
		this.included = inclusionDecision;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setIterator(PropertyReference reference, String name)
	{
		this.iteratorReference = reference;
		this.iteratorName = name;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setStyle(Style style)
	{
		this.style = style;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setTitle(String selector, PropertyReference... properties)
	{
		this.title = new UiMessage().setMessage(selector, properties);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityList setTitleIncluded(Decision... decision)
	{
		this.titleIncluded = decision;
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
