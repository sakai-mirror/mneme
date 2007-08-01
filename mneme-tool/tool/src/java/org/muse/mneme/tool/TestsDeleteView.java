/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/TestsDeleteView.java $
 * $Id: TestsDeleteView.java 10793 2007-07-25 00:08:59Z tannirumurthy@foothill.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The tests delete view for the mneme tool.
 */
public class TestsDeleteView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(TestsDeleteView.class);

	/** Test Service */
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
		String destination = this.uiService.decode(req, context);

		if (params.length < 3) throw new IllegalArgumentException();

		List<Assessment> assessments = new ArrayList<Assessment>(0);
		StringBuffer deleteTestIds = new StringBuffer();

		// test id's are in the params array from the index 3
		for (int i = 3; i < params.length; i++)
		{
			// get the test and add to the list to show
			Assessment assessment = this.assessmentService.getAssessment(params[i]);

			if (assessment != null) assessments.add(assessment);
		}

		context.put("tests", assessments);
		// sort code
		context.put("sortcode", params[2]);

		uiService.render(ui, context);
	}

	/**
	 * @return the assessmentService
	 */
	public AssessmentService getAssessmentService()
	{
		return this.assessmentService;
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
		if (params.length < 3) throw new IllegalArgumentException();

		String destination = this.uiService.decode(req, context);

		if (destination != null && (destination.trim().equalsIgnoreCase("/tests_delete")))
		{
			try
			{
				// assessment id's are in the params array from the index 3
				for (int i = 3; i < params.length; i++)
				{
					Assessment assessment = this.assessmentService.getAssessment(params[i]);
					if (assessment != null)
					{
						this.assessmentService.removeAssessment(assessment);
					}
				}
			}
			catch (Exception e)
			{
				if (M_log.isErrorEnabled()) M_log.error(e.toString());
				e.printStackTrace();
			}
		}
		destination = "/tests/" + params[2];
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * @param assessmentService
	 *        the assessmentService to set
	 */
	public void setAssessmentService(AssessmentService assessmentService)
	{
		this.assessmentService = assessmentService;
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
