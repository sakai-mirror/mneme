/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008, 2009, 2010 Etudes, Inc.
 * 
 * Portions completed before September 1, 2008
 * Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

package org.etudes.mneme.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.ambrosia.api.Context;
import org.etudes.ambrosia.api.Paging;
import org.etudes.ambrosia.api.PopulatingSet;
import org.etudes.ambrosia.api.Value;
import org.etudes.ambrosia.api.PopulatingSet.Factory;
import org.etudes.ambrosia.api.PopulatingSet.Id;
import org.etudes.ambrosia.util.ControllerImpl;
import org.etudes.mneme.api.Assessment;
import org.etudes.mneme.api.AssessmentPermissionException;
import org.etudes.mneme.api.AssessmentService;
import org.etudes.mneme.api.AssessmentType;
import org.etudes.mneme.api.Submission;
import org.etudes.mneme.api.SubmissionService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
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

	/** Configuration: the page sizes for the view. */
	protected List<Integer> pageSizes = new ArrayList<Integer>();

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService serverConfigurationService = null;

	/** Submission Service */
	protected SubmissionService submissionService = null;

	/** Dependency: ToolManager */
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
		// [2]sort for /grades, [3]aid |optional->| [4]our sort, [5]our page, [6]our highest/all-for-uid
		if ((params.length < 4) || params.length > 7) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// grades sort parameter
		String gradesSortCode = params[2];
		context.put("sort_grades", gradesSortCode);

		// get Assessment
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("assessment", assessment);

		// sort parameter
		String sortCode = null;
		if (params.length > 4) sortCode = params[4];
		SubmissionService.FindAssessmentSubmissionsSort sort = getSort(assessment, context, sortCode);
		context.put("sort", sort.toString());

		// paging parameter
		String pagingParameter = null;
		if (params.length > 5) pagingParameter = params[5];
		if (pagingParameter == null)
		{
			pagingParameter = "1-" + Integer.toString(this.pageSizes.get(0));
		}

		// official or all
		Boolean official = Boolean.TRUE;
		String allUid = "official";
		if ((params.length > 6) && (!params[6].equals("official")))
		{
			allUid = params[6];
		}

		// for a survey, ignore official
		if (assessment.getType() == AssessmentType.survey)
		{
			official = Boolean.FALSE;
			allUid = null;
		}

		// get the size
		Integer maxSubmissions = this.submissionService.countAssessmentSubmissions(assessment, official, allUid);

		// paging
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxSubmissions);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// get all Assessment submissions
		List<Submission> submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, official, allUid,
				paging.getSize() == 0 ? null : paging.getCurrent(), paging.getSize() == 0 ? null : paging.getSize());
		context.put("submissions", submissions);
		context.put("view", allUid);

		// pages sizes
		if (this.pageSizes.size() > 1)
		{
			context.put("pageSizes", this.pageSizes);
		}

		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		String pageSize = StringUtil.trimToNull(this.serverConfigurationService.getString("pageSize@org.etudes.mneme.tool.GradeAssessmentView"));
		if (pageSize != null) setPageSize(pageSize);

		if (this.pageSizes.isEmpty())
		{
			this.pageSizes.add(Integer.valueOf(30));
			this.pageSizes.add(Integer.valueOf(100));
			this.pageSizes.add(Integer.valueOf(0));
		}

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// [2]sort for /grades, [3]aid |optional->| [4]our sort, [5]our page, [6]our all/highest
		if ((params.length < 4) || params.length > 7) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// for Adjust every student's test submission by
		Value submissionAdjustValue = this.uiService.newValue();
		context.put("submissionAdjust", submissionAdjustValue);

		// for "Adjust every student's test submission by" comments
		Value submissionAdjustCommentsValue = this.uiService.newValue();
		context.put("submissionAdjustComments", submissionAdjustCommentsValue);

		// setup the model: the assessment
		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// for the final scores
		PopulatingSet submissions = null;
		final SubmissionService submissionService = this.submissionService;
		submissions = uiService.newPopulatingSet(new Factory()
		{
			public Object get(String id)
			{
				Submission submission = submissionService.getSubmission(id);
				return submission;
			}
		}, new Id()
		{
			public String getId(Object o)
			{
				return ((Submission) o).getId();
			}
		});
		context.put("submissions", submissions);

		// read form
		String destination = this.uiService.decode(req, context);

		// save any final scores
		for (Iterator i = submissions.getSet().iterator(); i.hasNext();)
		{
			try
			{
				this.submissionService.evaluateSubmission((Submission) i.next());
			}
			catch (AssessmentPermissionException e)
			{
				M_log.warn("post: " + e);
			}
		}

		// apply the global adjustments
		String adjustScore = StringUtil.trimToNull(submissionAdjustValue.getValue());
		String adjustComments = StringUtil.trimToNull(submissionAdjustCommentsValue.getValue());
		if (adjustScore != null || adjustComments != null)
		{
			try
			{
				// parse the score
				Float score = null;
				if (adjustScore != null)
				{
					score = Float.parseFloat(adjustScore);
				}

				// apply (no release)
				this.submissionService.evaluateSubmissions(assessment, adjustComments, score);
			}
			catch (AssessmentPermissionException e)
			{
				M_log.warn("post: " + e);
			}
			catch (NumberFormatException e)
			{
			}
		}

		// release all evaluated
		if (destination.equals("RELEASEEVALUATED"))
		{
			try
			{
				this.submissionService.releaseSubmissions(assessment, Boolean.TRUE);
			}
			catch (AssessmentPermissionException e)
			{
				M_log.warn("post: " + e);
			}

			destination = context.getDestination();
		}

		else if (destination.equals("RELEASEALL"))
		{
			try
			{
				this.submissionService.releaseSubmissions(assessment, Boolean.FALSE);
			}
			catch (AssessmentPermissionException e)
			{
				M_log.warn("post: " + e);
			}

			destination = context.getDestination();
		}

		else if (destination.equals("SAVE"))
		{
			destination = context.getDestination();
		}

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
	 * Set the the page size for the view.
	 * 
	 * @param sizes
	 *        The the page sizes for the view - integers, comma separated.
	 */
	public void setPageSize(String sizes)
	{
		this.pageSizes.clear();
		String[] parts = StringUtil.split(sizes, ",");
		for (String part : parts)
		{
			this.pageSizes.add(Integer.valueOf(part));
		}
	}

	/**
	 * Set the ServerConfigurationService.
	 * 
	 * @param service
	 *        the ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		this.serverConfigurationService = service;
	}

	/**
	 * @param submissionService
	 *        the submissionService to set
	 */
	public void setSubmissionService(SubmissionService submissionService)
	{
		this.submissionService = submissionService;
	}

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}

	/**
	 * get the sort based on sort code
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param context
	 * @param sortCode
	 *        sort code
	 * @return SubmissionService.FindAssessmentSubmissionsSort
	 */
	protected SubmissionService.FindAssessmentSubmissionsSort getSort(Assessment assessment, Context context, String sortCode)
	{
		// default sort is user name ascending
		SubmissionService.FindAssessmentSubmissionsSort sort;
		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				context.put("sort_column", sortCode.charAt(0));
				context.put("sort_direction", sortCode.charAt(1));

				if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
				else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.status_a;
				else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.status_d;
				else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.final_a;
				else if ((sortCode.charAt(0) == '2') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.final_d;
				else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.evaluated_a;
				else if ((sortCode.charAt(0) == '3') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.evaluated_d;
				else if ((sortCode.charAt(0) == '4') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.released_a;
				else if ((sortCode.charAt(0) == '4') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.released_d;
				else if ((sortCode.charAt(0) == '5') && (sortCode.charAt(1) == 'A'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.sdate_a;
				else if ((sortCode.charAt(0) == '5') && (sortCode.charAt(1) == 'D'))
					sort = SubmissionService.FindAssessmentSubmissionsSort.sdate_d;
				else
				{
					throw new IllegalArgumentException();
				}
			}
			else
			{
				throw new IllegalArgumentException();
			}
		}
		else
		{
			// default sort: user name ascending for non anon, sdate asc for anon
			if (assessment.getAnonymous())
			{
				sort = SubmissionService.FindAssessmentSubmissionsSort.sdate_a;
				context.put("sort_column", '5');
				context.put("sort_direction", 'A');
			}
			else
			{
				sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
				context.put("sort_column", '0');
				context.put("sort_direction", 'A');
			}
		}

		return sort;
	}
}
