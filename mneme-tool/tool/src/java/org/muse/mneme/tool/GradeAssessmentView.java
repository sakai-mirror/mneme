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
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradeAssessmentView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeAssessmentView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** Submission Service */
	protected SubmissionService submissionService = null;

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
		if (params.length != 4) throw new IllegalArgumentException();
		
		 //TODO: add check for user permission to access the assessments for grading
		
		//grades sort parameter is in params array at index 2
		String gradesSortCode = params[2];
		context.put("gradesSortCode", gradesSortCode);

		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		// get Assessment submissions
		List submissions = this.submissionService.findAssessmentSubmissions(assessment, null, Boolean.TRUE, null, null);
		context.put("submissions", submissions);

		Integer maxSubmissions = 0;
		if (submissions != null) maxSubmissions = submissions.size();

		String pagingParameter = "1-30";
		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxSubmissions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

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
		if (params.length != 4) throw new IllegalArgumentException();
		
		//read form
		String destination = this.uiService.decode(req, context);
		
		destination = "/grades/" + params[2];
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
	 * @param submissionService
	 *        the submissionService to set
	 */
	public void setSubmissionService(SubmissionService submissionService)
	{
		this.submissionService = submissionService;
	}
}
