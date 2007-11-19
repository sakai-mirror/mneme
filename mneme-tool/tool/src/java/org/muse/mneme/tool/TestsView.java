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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.PopulatingSet;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.api.PopulatingSet.Factory;
import org.muse.ambrosia.api.PopulatingSet.Id;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /tests view for the mneme tool.
 */
public class TestsView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TestsView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** tool manager reference. */
	protected ToolManager toolManager = null;

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// sort (optional)
		if ((params.length != 2) && (params.length != 3))
		{
			throw new IllegalArgumentException();
		}

		// default is due date, ascending
		String sortCode = (params.length > 2) ? params[2] : "0A";
		if (sortCode.length() != 2)
		{
			throw new IllegalArgumentException();
		}

		// due (0), open (1), title (2), publish (3), view/type (4)
		AssessmentService.AssessmentsSort sort = null;
		if (sortCode.charAt(0) == '0')
		{
			if (sortCode.charAt(1) == 'A')
			{
				sort = AssessmentService.AssessmentsSort.ddate_a;
			}
			else
			{
				sort = AssessmentService.AssessmentsSort.ddate_d;
			}
		}
		else if (sortCode.charAt(0) == '1')
		{
			if (sortCode.charAt(1) == 'A')
			{
				sort = AssessmentService.AssessmentsSort.odate_a;
			}
			else
			{
				sort = AssessmentService.AssessmentsSort.odate_d;
			}
		}
		else if (sortCode.charAt(0) == '2')
		{
			if (sortCode.charAt(1) == 'A')
			{
				sort = AssessmentService.AssessmentsSort.title_a;
			}
			else
			{
				sort = AssessmentService.AssessmentsSort.title_d;
			}

		}
		else if (sortCode.charAt(0) == '3')
		{
			if (sortCode.charAt(1) == 'A')
			{
				sort = AssessmentService.AssessmentsSort.published_a;
			}
			else
			{
				sort = AssessmentService.AssessmentsSort.published_d;
			}
		}
		else if (sortCode.charAt(0) == '4')
		{
			if (sortCode.charAt(1) == 'A')
			{
				sort = AssessmentService.AssessmentsSort.type_a;
			}
			else
			{
				sort = AssessmentService.AssessmentsSort.type_d;
			}
		}
		else
		{
			throw new IllegalArgumentException();
		}
		context.put("sort_column", sortCode.charAt(0));
		context.put("sort_direction", sortCode.charAt(1));

		// collect the assessments in this context
		List<Assessment> assessments = this.assessmentService.getContextAssessments(this.toolManager.getCurrentPlacement().getContext(), sort,
				Boolean.FALSE);
		context.put("assessments", assessments);

		// disable the tool navigation to this view
		context.put("disableAssessments", Boolean.TRUE);

		// render
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// sort (optional)
		if ((params.length != 2) && (params.length != 3))
		{
			throw new IllegalArgumentException();
		}

		// default is due date, ascending
		String sort = (params.length > 2) ? params[2] : "0A";

		// security check
		if (!assessmentService.allowManageAssessments(this.toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// for the selected select
		Values values = this.uiService.newValues();
		context.put("ids", values);

		// for the dates
		final AssessmentService assessmentService = this.assessmentService;
		PopulatingSet assessments = uiService.newPopulatingSet(new Factory()
		{
			public Object get(String id)
			{
				// add a draw to the part
				Assessment assessment = assessmentService.getAssessment(id);
				return assessment;
			}
		}, new Id()
		{
			public String getId(Object o)
			{
				return ((Assessment) o).getId();
			}
		});
		context.put("assessments", assessments);

		// read the form
		String destination = uiService.decode(req, context);

		// save the dates
		for (Iterator i = assessments.getSet().iterator(); i.hasNext();)
		{
			Assessment assessment = (Assessment) i.next();
			try
			{
				this.assessmentService.saveAssessment(assessment);
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
			catch (AssessmentPolicyException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.policy)));
				return;
			}
		}

		// for an add
		if (destination.equals("ADD"))
		{
			try
			{
				Assessment assessment = this.assessmentService.newAssessment(this.toolManager.getCurrentPlacement().getContext());
				destination = "/assessment_edit/" + sort + "/" + assessment.getId();
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		else if (destination.equals("ARCHIVE"))
		{
			for (String id : values.getValues())
			{
				Assessment assessment = this.assessmentService.getAssessment(id);
				if (assessment != null)
				{
					assessment.setArchived(Boolean.TRUE);
					try
					{
						this.assessmentService.saveAssessment(assessment);
						destination = context.getDestination();
					}
					catch (AssessmentPermissionException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
						return;
					}
					catch (AssessmentPolicyException e)
					{
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.policy)));
						return;
					}
				}
			}
		}

		else if (destination.equals("UNPUBLISH"))
		{
			// build up list of ids separated by "+" for the unpublish view destination
			StringBuilder buf = new StringBuilder();
			for (String id : values.getValues())
			{
				buf.append(id);
				buf.append("+");
			}
			if (buf.length() > 1) buf.setLength(buf.length() - 1);

			destination = "/assessment_unpublish/" + sort + "/" + buf.toString();
		}

		else if (destination.equals("DELETE"))
		{
			// build up list of ids separated by "+" for the delete view destination
			StringBuilder buf = new StringBuilder();
			for (String id : values.getValues())
			{
				buf.append(id);
				buf.append("+");
			}
			if (buf.length() > 1) buf.setLength(buf.length() - 1);

			destination = "/assessments_delete/" + sort + "/" + buf.toString();
		}

		else if (destination.startsWith("DUPLICATE:"))
		{
			String[] parts = StringUtil.split(destination, ":");
			if (parts.length != 2)
			{
				throw new IllegalArgumentException();
			}
			String aid = parts[1];
			try
			{
				Assessment assessment = this.assessmentService.getAssessment(aid);
				if (assessment == null)
				{
					throw new IllegalArgumentException();
				}
				this.assessmentService.copyAssessment(toolManager.getCurrentPlacement().getContext(), assessment);
				destination = context.getDestination();
			}
			catch (AssessmentPermissionException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
				return;
			}
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the AssessmentService.
	 * 
	 * @param service
	 *        The AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param manager
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager manager)
	{
		toolManager = manager;
	}
}
