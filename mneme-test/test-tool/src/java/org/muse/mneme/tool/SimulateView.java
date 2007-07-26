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
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.Question;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * The /simulate view for the mneme test tool.
 */
public class SimulateView extends ControllerImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SimulateView.class);

	/** The assessment service. */
	protected MnemeService assessmentService = null;

	/** The security service. */
	protected SecurityService securityService = null;

	/** The session manager. */
	protected SessionManager sessionManager = null;

	/** The ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

	/** The tool manager. */
	protected ToolManager toolManager = null;

	/** The user directory service. */
	protected UserDirectoryService userDirectoryService = null;

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
	public void get(HttpServletRequest req, HttpServletResponse res, Context context, String[] params)
	{
		// if not logged in as the super user, we won't do anything
		if (!securityService.isSuperUser())
		{
			throw new IllegalArgumentException();
		}

		// one parameters expected
		if (params.length != 3)
		{
			throw new IllegalArgumentException();
		}

		SimulateSpecs specs = new SimulateSpecs(params[2]);

		// do the simulation
		String rv = simulate(specs);

		context.put("rv", rv);

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
		throw new IllegalArgumentException();
	}

	/**
	 * Set the assessment service.
	 * 
	 * @param service
	 *        The assessment service.
	 */
	public void setAssessmentService(MnemeService service)
	{
		this.assessmentService = service;
	}

	/**
	 * Set the security service.
	 * 
	 * @param service
	 *        The security service.
	 */
	public void setSecurityService(SecurityService service)
	{
		this.securityService = service;
	}

	/**
	 * Set the session manager.
	 * 
	 * @param service
	 *        The session manager.
	 */
	public void setSessionManager(SessionManager service)
	{
		this.sessionManager = service;
	}

	/**
	 * Set the thread local manager.
	 * 
	 * @param service
	 *        The thread local manager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		this.threadLocalManager = service;
	}

	/**
	 * Set the tool manager.
	 * 
	 * @param service
	 *        The tool manager.
	 */
	public void setToolManager(ToolManager service)
	{
		this.toolManager = service;
	}

	/**
	 * Set the user directory service.
	 * 
	 * @param service
	 *        The user directory service.
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		this.userDirectoryService = service;
	}

	protected void answerEssay(Question question, Answer answer)
	{
		// answer.setEntryAnswerText("Essay text.");

		answerRationale(question, answer);
	}

	protected void answerFillIn(Question question, Answer answer)
	{
		// // The corrects list the answers (text). One part.
		// List<AssessmentAnswer> answers = question.getPart().getCorrectAnswers();
		// if ((answers == null) || (answers.isEmpty()))
		// {
		// return;
		// }
		//
		// // pack the answer text into an array
		// String[] answerIds = new String[answers.size()];
		// int index = 0;
		// for (AssessmentAnswer a : answers)
		// {
		// answerIds[index++] = a.getText();
		// }
		//
		// // set the answer
		// answer.setEntryAnswerTexts(answerIds);
		//
		// answerRationale(question, answer);
	}

	protected void answerMatch(Question question, Answer answer)
	{
		// // get a correct from each part
		// List<AssessmentAnswer> answers = new ArrayList<AssessmentAnswer>();
		// for (QuestionPart part : question.getParts())
		// {
		// List<AssessmentAnswer> partAnswers = part.getCorrectAnswers();
		// answers.addAll(partAnswers);
		// }
		//
		// // pack the answer ids into an array
		// String[] answerIds = new String[answers.size()];
		// int index = 0;
		// for (AssessmentAnswer a : answers)
		// {
		// answerIds[index++] = a.getId();
		// }
		//
		// // set the answer
		// answer.setEntryAnswerIds(answerIds);
		//
		// answerRationale(question, answer);
	}

	protected void answerRationale(Question question, Answer answer)
	{
		// // rational required?
		// if ((question.getRequireRationale() != null) && (question.getRequireRationale().booleanValue()))
		// {
		// answer.setRationale("rationale");
		// }
	}

	protected void answerTF_MC_Survey(Question question, Answer answer)
	{
		// // which is correct? One part.
		// List<AssessmentAnswer> answers = question.getPart().getCorrectAnswers();
		// if ((answers == null) || (answers.isEmpty()))
		// {
		// // no correct answer, pick one.
		// answers = new ArrayList<AssessmentAnswer>(1);
		// answers.add(question.getPart().getAnswers().get(0));
		// }
		//
		// // pack the answer ids into an array
		// String[] answerIds = new String[answers.size()];
		// int index = 0;
		// for (AssessmentAnswer a : answers)
		// {
		// answerIds[index++] = a.getId();
		// }
		//
		// // set the answer
		// answer.setEntryAnswerIds(answerIds);
		//
		// answerRationale(question, answer);
	}

	/**
	 * Establish this as the "current" user.
	 * 
	 * @param userEid
	 *        The user eid.
	 */
	protected User establishUser(User user, String userEid)
	{
		// get the postmaster user
		try
		{
			if (user == null)
			{
				user = userDirectoryService.getUserByEid(userEid);
			}

			Session s = sessionManager.getCurrentSession();
			if (s != null)
			{
				s.setUserId(user.getId());
				s.setUserEid(user.getEid());
			}

			return user;
		}
		catch (UserNotDefinedException e)
		{
			return null;
		}
	}

	protected String simulate(final SimulateSpecs specs)
	{
		final String sakaiContext = toolManager.getCurrentPlacement().getContext();
		final StringBuffer results = new StringBuffer();

		// create a thread for each user
		Thread[] threads = new Thread[specs.numUsers];
		for (int i = 0; i < specs.numUsers; i++)
		{
			final String userEid = "student" + (i + 1);

			threads[i] = new Thread(new Runnable()
			{
				public void run()
				{
					String rv = simulateSubmission(sakaiContext, userEid, specs.getThinkTime().intValue());
					results.append(rv + "<br />");
				};
			});
		}

		// start them all
		for (int i = 0; i < specs.numUsers; i++)
		{
			threads[i].start();

			// pause a bit between starts
			if (specs.getStartGap().intValue() > 0)
			{
				try
				{
					Thread.sleep(specs.getStartGap().intValue());
				}
				catch (InterruptedException ignore)
				{
				}
			}
		}

		// wait for them all
		for (int i = 0; i < specs.numUsers; i++)
		{
			try
			{
				threads[i].join();
			}
			catch (InterruptedException e)
			{
			}
		}

		return results.toString();
	}

	/**
	 * Submit the correct answers to a test for a user.
	 * 
	 * @param context
	 *        The context in which to find and submit an assessment.
	 * @param userEid
	 *        The user EID for which we will simulate a submission.
	 * @param thinkTime
	 *        The time (ms) to wait between seeing a question and answering it.
	 */
	protected String simulateSubmission(String context, String userEid, int thinkTime)
	{
		return "";
		// Time start = timeService.newTime();
		//
		// User currentUser = null;
		// try
		// {
		// currentUser = userDirectoryService.getUser(sessionManager.getCurrentSessionUserId());
		// }
		// catch (UserNotDefinedException e)
		// {
		// }
		//
		// try
		// {
		// User user = establishUser(null, userEid);
		//
		// // get the list of assessments (simulate list view)
		// MnemeService.GetUserContextSubmissionsSort sort = MnemeService.GetUserContextSubmissionsSort.status_d;
		// List<Submission> submissions = assessmentService.getUserContextSubmissions(context, null, sort);
		//
		// // shuffle these to get a random available one
		// Collections.shuffle(submissions, new Random(userEid.hashCode()));
		//
		// // clear the thread to simulate a new requst / response cycle
		// threadLocalManager.clear();
		// if (thinkTime > 0)
		// {
		// try
		// {
		// Thread.sleep(thinkTime);
		// }
		// catch (InterruptedException ignore)
		// {
		// }
		// }
		// establishUser(user, userEid);
		//
		// // pick one that is ready to take
		// String aid = null;
		// for (Submission submission : submissions)
		// {
		// AssessmentSubmissionStatus status = submission.getAssessmentSubmissionStatus();
		// if ((status == AssessmentSubmissionStatus.completeReady) || (status == AssessmentSubmissionStatus.ready))
		// {
		// aid = submission.getAssessment().getId();
		// break;
		// }
		// }
		// if (aid == null) return "no assessment";
		//
		// // simulate the enter view get
		//
		// // useless code
		// Assessment assessment = assessmentService.idAssessment(aid);
		// assessment.getIsClosed();
		// assessmentService.allowSubmit(assessment, null);
		// if ((assessment.getPassword() == null) && (assessment.getDescription() == null) && (assessment.getAttachments().isEmpty())
		// && (assessment.getRandomAccess().booleanValue()) && (assessment.getTimeLimit() == null))
		// {
		// }
		// // useless code
		//
		// // enter the submission (simulate enter view post)
		//
		// // useless code
		// assessment = assessmentService.idAssessment(aid);
		// assessment.getPassword();
		// // useless code
		//
		// String sid = null;
		// try
		// {
		// Submission submission = assessmentService.enterSubmission(assessmentService.idAssessment(aid), null);
		// sid = submission.getId();
		//
		// // useless code
		// assessment = submission.getAssessment();
		// QuestionPresentation presentation = assessment.getQuestionPresentation();
		// assessment.getRandomAccess();
		// // useless code
		// }
		// catch (AssessmentClosedException e)
		// {
		// return e.toString();
		// }
		// catch (AssessmentCompletedException e)
		// {
		// return e.toString();
		// }
		// catch (AssessmentPermissionException e)
		// {
		// return e.toString();
		// }
		//
		// if (sid == null) return "could not enter submission";
		//
		// // pick the first question
		// Question firstQuestion = assessmentService.idSubmission(sid).getAssessment().getFirstSection().getFirstQuestion();
		// if (firstQuestion == null) return "no first quesiton";
		// String qid = firstQuestion.getId();
		//
		// // loop for the entire test
		// int count = 0;
		// while (qid != null)
		// {
		// // emulate the question view get
		//
		// // useless code
		// Submission submission = assessmentService.idSubmission(sid);
		// submission.completeIfOver();
		// submission = assessmentService.idSubmission(sid);
		// assessmentService.allowCompleteSubmission(submission, null);
		// Question question = submission.getAssessment().getQuestion(qid);
		// question.getSection().getAssessment().getRandomAccess();
		// submission.getIsCompleteQuestion(question);
		// // useless code
		//
		// // clear the thread to simulate a new requst / response cycle
		// threadLocalManager.clear();
		// if (thinkTime > 0)
		// {
		// try
		// {
		// Thread.sleep(thinkTime);
		// }
		// catch (InterruptedException ignore)
		// {
		// }
		// }
		// establishUser(user, userEid);
		//
		// // emulate the quesiton view post
		// // form an answer to qid
		// submission = assessmentService.idSubmission(sid);
		// question = submission.getAssessment().getQuestion(qid);
		// Answer answer = submission.getAnswer(question);
		//
		// // answer the question
		// switch (question.getType())
		// {
		// case trueFalse:
		// case multipleChoice:
		// case multipleCorrect:
		// case survey:
		// {
		// answerTF_MC_Survey(question, answer);
		// break;
		// }
		//
		// case essay:
		// {
		// answerEssay(question, answer);
		// break;
		// }
		//
		// case fillIn:
		// case numeric:
		// {
		// answerFillIn(question, answer);
		// break;
		// }
		//
		// case matching:
		// {
		// answerMatch(question, answer);
		// break;
		// }
		// // TODO: fileUpload, audioRecording
		// }
		//
		// // get the next question
		// Question nextQuesiton = question.getAssessmentOrdering().getNext();
		// if (nextQuesiton != null)
		// {
		// qid = nextQuesiton.getId();
		// }
		// else
		// {
		// qid = null;
		// }
		//
		// // submit it
		// try
		// {
		// assessmentService.submitAnswer(answer, Boolean.TRUE, Boolean.FALSE);
		// count++;
		// }
		// catch (AssessmentClosedException e)
		// {
		// }
		// catch (SubmissionCompletedException e)
		// {
		// }
		// catch (AssessmentPermissionException e)
		// {
		// }
		// }
		//
		// // finish
		// try
		// {
		// assessmentService.completeSubmission(assessmentService.idSubmission(sid));
		// }
		// catch (AssessmentClosedException e)
		// {
		// return e.toString();
		// }
		// catch (SubmissionCompletedException e)
		// {
		// return e.toString();
		// }
		// catch (AssessmentPermissionException e)
		// {
		// return e.toString();
		// }
		//
		// Time end = timeService.newTime();
		// return "user: " + userEid + " submission: " + sid + " assessment: " + aid + " questions: " + count + " time(ms): "
		// + (end.getTime() - start.getTime());
		// }
		//
		// finally
		// {
		// threadLocalManager.clear();
		//
		// if (currentUser != null)
		// {
		// establishUser(currentUser, null);
		// }
		// }
	}
}
