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
import java.util.List;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.api.PopulatingSet;
import org.muse.ambrosia.api.PopulatingSet.Factory;
import org.muse.ambrosia.api.PopulatingSet.Id;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.sakaiproject.tool.api.ToolManager;
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

		// sort parameter
		String sortCode = null;

		if (params.length == 3)
		{
			sortCode = params[2];
		}

		// default sort is title ascending
		AssessmentService.AssessmentsSort sort;

		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));

				// TODO: 0 is type, need to add code for processing it
				// 1 is title
				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
				{
					sort = AssessmentService.AssessmentsSort.type_a;
				}
				else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
				{
					sort = AssessmentService.AssessmentsSort.type_d;
				}
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
				{
					sort = AssessmentService.AssessmentsSort.title_a;
				}
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
				{
					sort = AssessmentService.AssessmentsSort.title_d;
				}
				// 2 is odate
				else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
				{
					sort = AssessmentService.AssessmentsSort.odate_a;
				}
				else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
				{
					sort = AssessmentService.AssessmentsSort.odate_d;
				}
				// 3 is ddate
				else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'A'))
				{
					sort = AssessmentService.AssessmentsSort.ddate_a;
				}
				else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'D'))
				{
					sort = AssessmentService.AssessmentsSort.ddate_d;
				}
				// 4 is active
				else if ((sortCode.charAt(0) == '4') && (sortCode.charAt(1) == 'A'))
				{
					sort = AssessmentService.AssessmentsSort.published_a;
				}
				else if ((sortCode.charAt(0) == '4') && (sortCode.charAt(1) == 'D'))
				{
					sort = AssessmentService.AssessmentsSort.published_d;
				}
				else
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
					return;
				}
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
				return;
			}
		}
		else
		{
			// default sort: title ascending
			sort = AssessmentService.AssessmentsSort.title_a;

			context.put("sort_column", '1');
			context.put("sort_direction", 'A');

		}

		// collect the assessments in this context
		List<Assessment> assessments = this.assessmentService.getContextAssessments(this.toolManager.getCurrentPlacement().getContext(), sort,
				Boolean.FALSE);
		context.put("assessments", assessments);

		// value holders for the selection checkboxes
		Values values = this.uiService.newValues();
		context.put("ids", values);

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

		// security check
		if (!assessmentService.allowManageAssessments(this.toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// throw new IllegalArgumentException();
		// for the selected tests to delete
		Values values = this.uiService.newValues();
		context.put("ids", values);
		// read the form
		String destination = uiService.decode(req, context);
		if (destination != null)
		{
			System.out.println("Dest is " + destination);
			// save date changes regardless of where user goes
			if (!saveDates(req, res, context)) return;

			// for an add
			if (destination.startsWith("/assessment_add"))
			{
				StringBuffer path = new StringBuffer();
				// create a new test
				try
				{
					Assessment assessment = this.assessmentService.newAssessment(this.toolManager.getCurrentPlacement().getContext());

					// redirect to edit for this assessment
					path.append("/assessment_edit/");
					// for sort code
					if (params.length == 3)
					{
						path.append(params[2]);
						path.append("/");
					}
					else
					{
						// default sort - title ascending
						path.append("1A");
						path.append("/");
					}
					path.append(assessment.getId());
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}

			else if (destination.startsWith("/ARCHIVE"))
			{
				// archive these tests, here and now
				String[] selectedTestIds = values.getValues();
				for (String id : selectedTestIds)
				{
					Assessment assessment = this.assessmentService.getAssessment(id);
					if (assessment != null)
					{
						assessment.setArchived(Boolean.TRUE);
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
				}
			}
			else if (destination.startsWith("/assessment_unpublish"))
			{
				String[] selectedTestIds = values.getValues();
				// delete the tests with ids
				StringBuffer path = new StringBuffer();
				String separator = "+";

				if (selectedTestIds != null && (selectedTestIds.length > 0))
				{
					path.append(destination);
					path.append("/");

					// for sort code
					if (params.length == 3)
					{
						path.append(params[2]);
						path.append("/");
					}
					else
					{
						// default sort - title ascending
						path.append("1A");
						path.append("/");
					}
					path.append(selectedTestIds[0]);
					for (int i = 1; i < selectedTestIds.length; i++)
					{
						path.append(separator);
						path.append(selectedTestIds[i]);
					}
					System.out.println("path is " + path.toString());
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}
			else if (destination.trim().equalsIgnoreCase("/assessments_delete"))
			{
				String[] selectedTestIds = values.getValues();
				// delete the tests with ids
				StringBuffer path = new StringBuffer();
				String separator = "+";

				if (selectedTestIds != null && (selectedTestIds.length > 0))
				{
					path.append(destination);
					path.append("/");

					// for sort code
					if (params.length == 3)
					{
						path.append(params[2]);
						path.append("/");
					}
					else
					{
						// default sort - title ascending
						path.append("1A");
						path.append("/");
					}
					path.append(selectedTestIds[0]);
					for (int i = 1; i < selectedTestIds.length; i++)
					{
						path.append(separator);
						path.append(selectedTestIds[i]);
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
				}
			}

			else if (destination.trim().startsWith("/COPY"))
			{
				try
				{
					Assessment assessment = this.assessmentService.getAssessment(destination.substring(destination.lastIndexOf("/") + 1));
					if (assessment != null)
					{
						this.assessmentService.copyAssessment(toolManager.getCurrentPlacement().getContext(), assessment);
					}
				}
				catch (AssessmentPermissionException e)
				{
					// redirect to error
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}

			else
			{
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
				return;
			}
		}

		if (params.length == 3)
			destination = "/assessments/" + params[2];
		else
			destination = "/assessments/";

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	private boolean saveDates(HttpServletRequest req, HttpServletResponse res, Context context) throws IOException
	{
		PopulatingSet assessments = null;
		final AssessmentService assessmentService = this.assessmentService;
		assessments = uiService.newPopulatingSet(new Factory()
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
		String destination = uiService.decode(req, context);

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
				return false;
			}
			catch (AssessmentPolicyException e)
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.policy)));
				return false;
			}
		}

		return true;
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
