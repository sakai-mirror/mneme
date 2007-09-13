/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/GradesView.java $
 * $Id: GradesView.java 11566 2007-09-11 20:45:50Z maheshwarirashmi@foothill.edu $
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
import org.muse.mneme.api.AssessmentService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradesView extends ControllerImpl
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

		// default sort is type ascending
		AssessmentService.AssessmentsSort sort = AssessmentService.AssessmentsSort.type_a;
		context.put("sort_column", '0');
		context.put("sort_direction", 'A');

		if (params.length == 3)
		{
			sortCode = params[2];
			if (sortCode != null && sortCode.length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));
				sort = findSortCode(sortCode);
			}
		}

		// collect the assessments in this context
		List<Assessment> assessments = this.assessmentService.getContextAssessments(this.toolManager.getCurrentPlacement().getContext(), sort,
				Boolean.TRUE);
		context.put("assessments", assessments);

		// render
		uiService.render(ui, context);
	}

	private AssessmentService.AssessmentsSort findSortCode(String sortCode)
	{
		AssessmentService.AssessmentsSort sort = null;
		// 0 is title
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = AssessmentService.AssessmentsSort.type_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = AssessmentService.AssessmentsSort.type_d;
		}
		// 1 is odate
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
		{
			sort = AssessmentService.AssessmentsSort.odate_a;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
		{
			sort = AssessmentService.AssessmentsSort.odate_d;
		}
		// 2 is ddate
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
		{
			sort = AssessmentService.AssessmentsSort.ddate_a;
		}
		else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
		{
			sort = AssessmentService.AssessmentsSort.ddate_d;
		}

		return sort;
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
		// read the form
		String destination = uiService.decode(req, context);

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
