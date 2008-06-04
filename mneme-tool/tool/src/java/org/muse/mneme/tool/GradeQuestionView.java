/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Paging;
import org.muse.ambrosia.api.PopulatingSet;
import org.muse.ambrosia.api.PopulatingSet.Factory;
import org.muse.ambrosia.api.PopulatingSet.Id;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradeQuestionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeQuestionView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

	/** AttachmentService service. */
	protected AttachmentService attachmentService = null;

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
		// [2]grades sort code, [3]aid, [4]qid, |optional ->| [5]sort, [6]page
		if (params.length < 5) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// the sort for the grades view
		context.put("sort_grades", params[2]);

		// assessment
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		if (assessment == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("assessment", assessment);

		// question
		Question question = assessment.getParts().getQuestion(params[4]);
		if (question == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("question", question);

		// sort code
		String sortCode = null;
		if (params.length > 5)
		{
			sortCode = params[5];
		}
		if (sortCode == null)
		{
			if (assessment.getAnonymous())
			{
				sortCode = "1A";
			}
			else
			{
				sortCode = "0A";
			}
		}

		// parse into a sort
		SubmissionService.FindAssessmentSubmissionsSort sort = null;
		if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'A'))
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
		}
		else if ((sortCode.charAt(0) == '0') && (sortCode.charAt(1) == 'D'))
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.userName_d;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'A'))
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.final_a;
		}
		else if ((sortCode.charAt(0) == '1') && (sortCode.charAt(1) == 'D'))
		{
			sort = SubmissionService.FindAssessmentSubmissionsSort.final_d;
		}
		if (sort == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}
		context.put("sort_column", sortCode.charAt(0));
		context.put("sort_direction", sortCode.charAt(1));

		// get the size - from all submissions
		Integer maxAnswers = this.submissionService.countSubmissionAnswers(assessment, question);

		// paging parameter
		String pagingParameter = null;
		if (params.length > 6) pagingParameter = params[6];
		if (pagingParameter == null)
		{
			pagingParameter = "1-" + Integer.toString(this.pageSizes.get(0));
		}
		Paging paging = uiService.newPaging();
		paging.setMaxItems(maxAnswers);
		paging.setCurrentAndSize(pagingParameter);
		context.put("paging", paging);

		// get the answers - from all submissions
		List<Answer> answers = this.submissionService.findSubmissionAnswers(assessment, question, sort, paging.getSize() == 0 ? null : paging
				.getCurrent(), paging.getSize() == 0 ? null : paging.getSize());
		context.put("answers", answers);

		// so we know we are grading
		context.put("grading", Boolean.TRUE);

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
		String pageSize = StringUtil.trimToNull(this.serverConfigurationService.getString("pageSize@org.muse.mneme.tool.GradeQuestionView"));
		if (pageSize != null) setPageSize(pageSize);

		if (this.pageSizes.isEmpty())
		{
			this.pageSizes.add(Integer.valueOf(10));
		}

		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res, Context context, String[] params) throws IOException
	{
		// [2]grades sort code, [3]aid, [4]qid, |optional ->| [5]sort, [6]page
		if (params.length < 5) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		final SubmissionService submissionService = this.submissionService;
		PopulatingSet answers = uiService.newPopulatingSet(new Factory()
		{
			public Object get(String id)
			{
				Answer answer = submissionService.getAnswer(id);
				return answer;
			}
		}, new Id()
		{
			public String getId(Object o)
			{
				return ((Answer) o).getId();
			}
		});
		context.put("answers", answers);

		// read form
		String destination = this.uiService.decode(req, context);

		// post-process the answers
		for (Object o : answers.getSet())
		{
			Answer a = (Answer) o;
			a.getTypeSpecificAnswer().consolidate(destination);
		}

		// check for remove
		if (destination.startsWith("REMOVE:"))
		{
			String[] parts = StringUtil.splitFirst(destination, ":");
			if (parts.length == 2)
			{
				parts = StringUtil.splitFirst(parts[1], ":");
				if (parts.length == 2)
				{
					// find the answer, id=parts[0], ref=parts[1]
					for (Object o : answers.getSet())
					{
						Answer answer = (Answer) o;
						if (answer.getId().equals(parts[0]))
						{
							Reference ref = this.attachmentService.getReference(parts[1]);
							answer.getEvaluation().removeAttachment(ref);
							this.attachmentService.removeAttachment(ref);
							break;
						}
					}
				}
			}

			destination = context.getDestination();
		}

		else if (destination.startsWith("STAY_"))
		{
			String[] parts = StringUtil.splitFirst(destination, ":");

			destination = context.getDestination();

			if (parts.length == 2)
			{
				destination += "#" + parts[1];
			}
		}

		// save
		try
		{
			this.submissionService.evaluateAnswers(answers.getSet());
		}
		catch (AssessmentPermissionException e)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unexpected)));
			return;
		}

		// if there was an upload error, send to the upload error
		if ((req.getAttribute("upload.status") != null) && (!req.getAttribute("upload.status").equals("ok")))
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.upload + "/" + req.getAttribute("upload.limit"))));
			return;
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * Set the AssessmentService.
	 * 
	 * @param service
	 *        the AssessmentService.
	 */
	public void setAssessmentService(AssessmentService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Set the AttachmentService.
	 * 
	 * @param service
	 *        the AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		this.attachmentService = service;
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
	 * Set the SubmissionService.
	 * 
	 * @param service
	 *        the SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}

	/**
	 * Set the ToolManager.
	 * 
	 * @param service
	 *        the ToolManager.
	 */
	public void setToolManager(ToolManager service)
	{
		this.toolManager = service;
	}
}
