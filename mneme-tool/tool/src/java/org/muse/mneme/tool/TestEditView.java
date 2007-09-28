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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Values;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentPolicyException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.sakaiproject.util.Web;

/**
 * The /test_edit view for the mneme tool.
 */
public class TestEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TestEditView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

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

		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[3];

		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// security check
		if (!assessmentService.allowEditAssessment(assessment))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// collect information: the selected assessment
		context.put("assessment", assessment);
		context.put("sortcode", params[2]);

		// value holders for the selection checkboxes
		Values values = this.uiService.newValues();
		context.put("ids", values);

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
		// we need a single parameter (aid)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[3];
		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// security check
		if (!assessmentService.allowEditAssessment(assessment))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// setup the model: the selected assessment
		context.put("assessment", assessment);

		// value holders for the selection checkboxes
		Values values = this.uiService.newValues();
		context.put("ids", values);

		// read the form
		String destination = uiService.decode(req, context);

		// commit the save
		try
		{
			// save assessment properties
			this.assessmentService.saveAssessment(assessment);

			if (destination.equals("DRAW"))
			{
				DrawPart dPart = assessment.getParts().addDrawPart();
				this.assessmentService.saveAssessment(assessment);
				// create url for draw
				destination = "/part_edit/" + params[2] + "/" + assessment.getId() + "/" + dPart.getId();
			}
			else if (destination.equals("MANUAL"))
			{
				ManualPart mPart = assessment.getParts().addManualPart();
				this.assessmentService.saveAssessment(assessment);
				// create url for manual
				destination = "/part_edit/" + params[2] + "/" + assessment.getId() + "/" + mPart.getId();
			}
			else if (destination.equals("/part_delete"))
			{
				// delete the parts
				StringBuffer path = new StringBuffer("/part_delete/" + params[2] + "/" + assessment.getId() + "/");
				String separator = "+";

				String[] deletePartIds = values.getValues();
				if (deletePartIds != null && deletePartIds.length != 0)
				{
					path.append(deletePartIds[0]);
					for (int i = 1; i < deletePartIds.length; i++)
					{
						path.append(separator);
						path.append(deletePartIds[i]);
					}
				}
				
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
				return;
			}
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

		// redirect to the next destination
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
}
