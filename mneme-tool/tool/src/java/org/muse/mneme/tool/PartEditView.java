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
import org.muse.ambrosia.api.Component;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.PopulatingSet;
import org.muse.ambrosia.api.PopulatingSet.Factory;
import org.muse.ambrosia.api.PopulatingSet.Id;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.Values;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.PoolDraw;
import org.muse.mneme.api.PoolService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;
import org.springframework.core.io.ClassPathResource;

/**
 * The /part_edit view for the mneme tool.
 */
public class PartEditView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PartEditView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	protected PoolService poolService = null;

	/** tool manager */
	protected ToolManager toolManager = null;

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
		// we need a two, 3 or 4 parameters (aid, pid, sort, paging (dpart))
		if (params.length != 5 && params.length != 6 && params.length != 7)
		{
			throw new IllegalArgumentException();
		}

		// Since we have two sorts here, we call this testsortcode
		context.put("testsortcode", params[2]);
		String assessmentId = params[3];
		String partId = params[4];
		// sort parameter
		String sortCode = null;
		if (params.length >= 6) sortCode = params[5];

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

		// find sort param
		String sortCode = null;
		PoolService.FindPoolsSort sort = PoolService.FindPoolsSort.subject_a;
		char sortCol = '0';
		char sortDir = 'A';

		if (params.length >= 6)
		{
			sortCode = params[5];
			if (sortCode != null && sortCode.length() == 2)
			{
				sortCol = sortCode.charAt(0);
				sortDir = sortCode.charAt(1);
				sort = findSortCode(sortCode);
			}
		}
		context.put("sort_column", sortCol);
		context.put("sort_direction", sortDir);

		// default paging
		String pagingParameter = null;
		if (params.length == 7)
		{
			pagingParameter = params[6];
		}

		if (pagingParameter == null)
		{
			// TODO: other than 2 size!
			pagingParameter = "1-2";
		}

		Integer maxPools = this.poolService.countPools(toolManager.getCurrentPlacement().getContext(), null, null);

		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxPools);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);
		context.put("pagingParameter", pagingParameter);

		// get the pool draw list - all the pools for the user (select, sort, page) crossed with this part's actual draws
		List<PoolDraw> draws = part.getDrawsForPools(toolManager.getCurrentPlacement().getContext(), null, sort, null, paging.getCurrent(), paging
				.getSize());

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

		// checkboxes to remove questions
		Values values = uiService.newValues();
		context.put("questionids", values);

		// render
		uiService.render(ui2, context);
	}

	private PoolService.FindPoolsSort findSortCode(String sortCode)
	{
		PoolService.FindPoolsSort sort = PoolService.FindPoolsSort.subject_a;
		// 0 is subject
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = PoolService.FindPoolsSort.subject_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = PoolService.FindPoolsSort.subject_d;
		}
		// 1 is title
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
		{
			sort = PoolService.FindPoolsSort.title_a;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
		{
			sort = PoolService.FindPoolsSort.title_d;
		}
		// 2 is points
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
		{
			sort = PoolService.FindPoolsSort.points_a;
		}
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
		{
			sort = PoolService.FindPoolsSort.points_d;
		}

		return sort;
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
		// we need a two, 3 or 4 parameters (aid, pid, sort, paging (dpart))
		if (params.length != 5 && params.length != 6 && params.length != 7)
		{
			throw new IllegalArgumentException();
		}

		String assessmentId = params[3];
		String partId = params[4];

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

		Values values = null;
			
		// based on the part type...
		PopulatingSet draws = null;
		if (part instanceof DrawPart)
		{
			final DrawPart dpart = (DrawPart) part;
			final PoolService pservice = this.poolService;
			draws = uiService.newPopulatingSet(new Factory()
			{
				public Object get(String id)
				{
					// add a draw to the part
					PoolDraw draw = dpart.addPool(pservice.getPool(id), 0);
					return draw;
				}
			}, new Id()
			{
				public String getId(Object o)
				{
					return ((PoolDraw) o).getPool().getId();
				}
			});

			context.put("draws", draws);
		}

		// for mpart, we need to collect the checkbox ids
		else
		{
			values = uiService.newValues();
			context.put("questionids", values);
		}

		// read the form
		String destination = uiService.decode(req, context);

		// filter out draw part draws that are no questions
		if (part instanceof DrawPart)
		{
			DrawPart dpart = (DrawPart) part;
			for (Iterator i = dpart.getDraws().iterator(); i.hasNext();)
			{
				PoolDraw draw = (PoolDraw) i.next();
				if (draw.getNumQuestions() == 0)
				{
					i.remove();
				}
			}
		}
		// process the ids into the destination for a redirect to the remove confirm view...
		else
		{
			if (destination.equals("/part_ques_delete"))
			{
				// get the ids
				String[] removeQuesIds = values.getValues();
				if (removeQuesIds != null && removeQuesIds.length != 0)
				{
					// remove questions from part
					StringBuffer path = new StringBuffer("/part_ques_delete/" + assessment.getId() + "/" + part.getId() + "/");
					String separator = "+";

					for (String removeQuesId : removeQuesIds)
					{
						path.append(removeQuesId);
						path.append(separator);
					}
					destination = path.toString();	
				}
			}
		}
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

	/**
	 * Set the PoolService.
	 * 
	 * @param service
	 *        The PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
