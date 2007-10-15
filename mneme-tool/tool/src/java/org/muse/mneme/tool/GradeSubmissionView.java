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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The /grading view for the mneme tool.
 */
public class GradeSubmissionView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GradeSubmissionView.class);

	/** Assessment service. */
	protected AssessmentService assessmentService = null;

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
		if (params.length != 7 && params.length != 8) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		boolean fromGradeByQuestion = false;

		if (context.getPreviousDestination().startsWith("/grade_question")) fromGradeByQuestion = true;

		// get Assessment - assessment id is in params at index 3
		Assessment assessment = this.assessmentService.getAssessment(params[3]);
		context.put("assessment", assessment);

		if (assessment == null)
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		Submission submission = null;

		if (fromGradeByQuestion)
		{
			String submissionId = context.getDestination().substring(context.getDestination().lastIndexOf("/") + 1);
			submission = this.submissionService.getSubmission(submissionId);
			context.put("fromGradeByQuestion", "TRUE");
		}
		else
		{
			// submission id is in params array at index 7
			submission = this.submissionService.getSubmission(params[7]);
			context.put("fromGradeByQuestion", "FALSE");
		}

		context.put("submission", submission);

		// collect all the answers for grading
		List<Answer> answers = new ArrayList<Answer>();
		for (Part part : submission.getAssessment().getParts().getParts())
		{
			for (Question question : part.getQuestions())
			{
				Answer answer = submission.getAnswer(question);
				answers.add(answer);
			}
		}

		context.put("answers", answers);

		List<Submission> submissions = null;

		if (!fromGradeByQuestion)
		{
			/*
			 * for previous and next - get submissions based on grade_assessment sort & view. sort is at index 4 and view is at index 6 in params
			 */
			if (params[6].equalsIgnoreCase("all"))
			{
				SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, params[4]);
				// get all Assessment submissions
				submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.FALSE, null, null);

			}
			else if (params[6].equalsIgnoreCase("highest"))
			{
				SubmissionService.FindAssessmentSubmissionsSort sort = getSort(context, params[4]);
				// get official Assessment submissions
				submissions = this.submissionService.findAssessmentSubmissions(assessment, sort, Boolean.TRUE, null, null);
			}
			else
			{
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
				return;
			}

			parsePreviousNext(context, submission, submissions);
		}

		// check for user permission to access the submission for grading
		if (!this.submissionService.allowEvaluate(submission))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// destination path for grade submission
		String destinationPath = context.getDestination();
		destinationPath = destinationPath.substring(destinationPath.indexOf("/", 1) + 1, destinationPath.lastIndexOf("/"));
		context.put("destinationPath", destinationPath);
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
		if (params.length != 7 && params.length != 8) throw new IllegalArgumentException();

		// check for user permission to access the assessments for grading
		if (!this.submissionService.allowEvaluate(toolManager.getCurrentPlacement().getContext()))
		{
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
			return;
		}

		// submission id is in params array at last index
		Submission submission = this.submissionService.getSubmission(params[params.length - 1]);
		context.put("submission", submission);

		if (submission == null)
		{
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.invalid)));
			return;
		}

		// read form
		String destination = this.uiService.decode(req, context);

		if (destination != null)
		{
			if (destination.startsWith("/grade_submission_save"))
			{
				try
				{
					saveGradedSubmission(submission);
				}
				catch (AssessmentPermissionException e)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				destination = destination.replace("grade_submission_save", "grade_assessment");
			}
			else if (destination.startsWith("/NEXT") || destination.startsWith("/PREV"))
			{
				try
				{
					saveGradedSubmission(submission);
				}
				catch (AssessmentPermissionException e)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}

				destination = destination.replace("NEXT:", "");
				destination = destination.replace("PREV:", "");
			}
			else if (destination.startsWith("/grade_question"))
			{
				try
				{
					saveGradedSubmission(submission);
				}
				catch (AssessmentPermissionException e)
				{
					res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/" + Errors.unauthorized)));
					return;
				}
			}
		}

		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, destination)));
	}

	/**
	 * save Graded Submission
	 * 
	 * @param submission
	 * @throws AssessmentPermissionException
	 */
	private void saveGradedSubmission(Submission submission) throws AssessmentPermissionException
	{
		// save submission
		if (submission.getAnswers() != null)
		{
			this.submissionService.evaluateSubmission(submission);
		}
	}

	/**
	 * get the sort based on sort code
	 * 
	 * @param context
	 * @param sortCode
	 *        sort code
	 * @return SubmissionService.FindAssessmentSubmissionsSort
	 */
	private SubmissionService.FindAssessmentSubmissionsSort getSort(Context context, String sortCode)
	{
		// default sort is user name ascending
		SubmissionService.FindAssessmentSubmissionsSort sort;
		if (sortCode != null)
		{
			if (sortCode.trim().length() == 2)
			{
				// 0 is title
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
			// default sort: user name ascending
			sort = SubmissionService.FindAssessmentSubmissionsSort.userName_a;
		}

		return sort;
	}

	/**
	 * parse for previous and next
	 * 
	 * @param context
	 * @param submission
	 * @param submissions
	 */
	private void parsePreviousNext(Context context, Submission submission, List<Submission> submissions)
	{
		// prev and next
		if (submissions != null)
		{
			Submission submissionPresent = null;
			for (int i = 0; i < submissions.size(); i++)
			{
				submissionPresent = submissions.get(i);
				if (submissionPresent.getId().equals(submission.getId()))
				{
					if (i == 0 && (i == submissions.size() - 1))
					{
						context.put("prevSubmission", "");
						context.put("nextSubmission", "");
					}
					else
					{
						// current is first submission
						if (i == 0)
						{
							Submission submissionNext = null;
							// loop for next submission
							for (int j = i + 1; j < submissions.size(); j++)
							{
								submissionNext = submissions.get(j);
								if (submissionNext.getIsComplete())
								{
									context.put("nextSubmission", submissionNext);
									break;
								}
							}
						}
						// current is last submission
						else if (i == submissions.size() - 1)
						{
							Submission submissionPrev = null;
							// loop for prev submission
							for (int j = i - 1; j >= 0; j--)
							{
								submissionPrev = submissions.get(j);
								if (submissionPrev.getIsComplete())
								{
									context.put("prevSubmission", submissionPrev);
									break;
								}
							}
						}
						// current is somewhere between first and last
						else
						{
							Submission submissionNext = null;
							// loop for next submission
							for (int j = i + 1; j < submissions.size(); j++)
							{
								submissionNext = submissions.get(j);
								if (submissionNext.getIsComplete())
								{
									context.put("nextSubmission", submissionNext);
									break;
								}
							}

							Submission submissionPrev = null;
							// loop for prev submission
							for (int j = i - 1; j >= 0; j--)
							{
								submissionPrev = submissions.get(j);
								if (submissionPrev.getIsComplete())
								{
									context.put("prevSubmission", submissionPrev);
									break;
								}
							}
						}
					}
					break;
				}
			}
		}
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

	/**
	 * @param toolManager
	 *        the toolManager to set
	 */
	public void setToolManager(ToolManager toolManager)
	{
		this.toolManager = toolManager;
	}
}
