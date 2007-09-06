/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/ambrosia/trunk/ambrosia-impl/impl/src/java/org/muse/ambrosia/impl/UiValue.java $
 * $Id: UiValue.java 9047 2007-04-13 00:21:17Z ggolden@umich.edu $
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
import org.sakaiproject.tool.api.SessionManager;
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

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

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
		List<Assessment> nodelAssessments = new ArrayList<Assessment>(0);

		String selectedTestIds[] = params[3].split("\\+");

		for (String selectTestId : selectedTestIds)
		{
			Assessment assessment = null;

			if (selectTestId != null && selectTestId.trim().length() > 0)
			{
				// get the test and add to the list to show
				assessment = this.assessmentService.getAssessment(selectTestId);
				if (assessment != null)
				{
					if (this.assessmentService.allowRemoveAssessment(assessment, sessionManager.getCurrentSessionUserId()))
					{
						assessments.add(assessment);
					}
					else
					{
						nodelAssessments.add(assessment);
					}

				}
			}
		}

		context.put("tests", assessments);
		context.put("nodeltests", nodelAssessments);
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

		if (destination != null && (destination.trim().startsWith("/tests_delete")))
		{
			StringBuffer path = new StringBuffer();
			String separator = "+";
			try
			{
				String selectedTestIds[] = params[3].split("\\+");

				if (selectedTestIds != null && (selectedTestIds.length > 0))
				{
					// path.append(destination);

					path.append("/tests/" + params[2]);
					for (String selectedTestId : selectedTestIds)
					{
						Assessment assessment = this.assessmentService.getAssessment(selectedTestId);
						if (assessment != null)
						{
							if (this.assessmentService.allowRemoveAssessment(assessment, sessionManager.getCurrentSessionUserId()))
							{
								this.assessmentService.removeAssessment(assessment);
								// path.append(selectedTestId);
								// path.append(separator);
							}
						}
					}

					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, path.toString())));
					return;
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
	 * @param sessionManager
	 *        the SessionManager.
	 */
	public void setSessionManager(SessionManager sessionManager)
	{
		this.sessionManager = sessionManager;
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
