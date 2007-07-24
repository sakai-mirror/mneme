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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.util.Web;
import org.springframework.core.io.ClassPathResource;

/**
 * The /dpart_edit view for the mneme tool.
 */
public class PartEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PartEditView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** The UI (2). Used for manual parts (the main this.ui used for draw parts). */
	protected Component ui2 = null;

	/** The view declaration xml path. */
	protected String viewPath2 = null;

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
		// we need a two parameters (aid, pid)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[2];
		String partId = params[3];

		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		Part part = assessment.getParts().getPart(partId);
		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// security check
		if (!assessmentService.allowEditAssessment(assessment, null))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// based on the part type...
		if (part instanceof DrawPart)
		{
			getDraw(assessment, (DrawPart) part, req, res, context, params);
		}
		else
		{
			getManual(assessment, (ManualPart) part, req, res, context, params);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void getDraw(Assessment assessment, DrawPart part, HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
			throws IOException
	{
		context.put("assessment", assessment);
		context.put("part", part);

		// get the pool draw list - all the pools for the user (select, sort, page) crossed with this part's actual draws
		List<PoolDraw> draws = part.getDrawsForPools(null, PoolService.FindPoolsSort.subject_a, null);
		context.put("draws", draws);

		// render
		uiService.render(ui, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void getManual(Assessment assessment, ManualPart part, HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
			throws IOException
	{
		// collect information: the selected assessment
		context.put("assessment", assessment);
		context.put("part", part);

		// render
		uiService.render(ui2, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();

		// interface from XML in the class path
		if (this.viewPath != null)
		{
			try
			{
				ClassPathResource rsrc = new ClassPathResource(this.viewPath2);
				this.ui2 = uiService.newInterface(rsrc.getInputStream());
			}
			catch (IOException e)
			{
			}
		}

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// we need a two parameters (aid, pid)
		if (params.length != 4)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[2];
		String partId = params[3];

		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		Part part = assessment.getParts().getPart(partId);
		if (part == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// security check
		if (!assessmentService.allowEditAssessment(assessment, null))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// setup the model: the selected assessment
		context.put("assessment", assessment);
		context.put("part", part);

		// TODO: the draws...

		// read the form
		String destination = uiService.decode(req, context);

		// commit the save
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

	/**
	 * Set the class path to the components (2) XML declaration for the view.
	 * 
	 * @param path
	 *        The class path to the components (2) XML declaration for the view.
	 */
	public void setComponents2(String path)
	{
		this.viewPath2 = path;
	}
}