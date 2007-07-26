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

package org.muse.mneme.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.ReviewTiming;
import org.muse.mneme.api.SubmissionService;
import org.muse.mneme.api.AssessmentService;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class AssessmentStorageSample implements AssessmentStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentStorageSample.class);

	protected Map<String, AssessmentImpl> assessments = new HashMap<String, AssessmentImpl>();

	protected Object idGenerator = new Object();

	protected long nextId = 100;

	protected PoolService poolService = null;

	protected QuestionService questionService = null;

	protected SubmissionService submissionService = null;

	/**
	 * {@inheritDoc}
	 */
	public Boolean assessmentExists(String id)
	{
		fakeIt();

		return (this.assessments.get(id) != null);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessments(String context)
	{
		fakeIt();

		int i = 0;

		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(context))
			{
				i++;
			}
		}

		return i;
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl getAssessment(String id)
	{
		fakeIt();

		AssessmentImpl rv = this.assessments.get(id);
		if (rv == null)
		{
			rv = newAssessment();
			rv.initId(id);
		}
		else
		{
			rv = new AssessmentImpl(rv);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context, AssessmentService.AssessmentsSort sort)
	{
		fakeIt();

		List<Assessment> rv = new ArrayList<Assessment>();

		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(context))
			{
				rv.add(new AssessmentImpl(assessment));
			}
		}

		return rv;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl newAssessment()
	{
		return new AssessmentImpl(this.poolService, this.questionService, this.submissionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAssessment(AssessmentImpl assessment)
	{
		fakeIt();

		this.assessments.remove(assessment.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveAssessment(AssessmentImpl assessment)
	{
		fakeIt();

		// assign an id
		if (assessment.getId() == null)
		{
			long id = 0;
			synchronized (this.idGenerator)
			{
				id = this.nextId;
				this.nextId++;
			}
			assessment.initId("a" + Long.toString(id));
		}

		// TODO: part ids!

		this.assessments.put(assessment.getId(), new AssessmentImpl(assessment));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}

	protected void fakeIt()
	{
		if (this.assessments.isEmpty())
		{
			AssessmentImpl a = newAssessment();
			a.initId("a1");
			a.setActive(Boolean.TRUE);
			a.setContext("mercury");
			a.setCreatedBy("admin");
			a.setNumSubmissionsAllowed(1);
			a.setQuestionGrouping(QuestionGrouping.question);
			a.setRandomAccess(Boolean.TRUE);
			a.setTimeLimit(1200l * 1000l);
			a.setTitle("assessment one");
			a.setType(AssessmentType.test);
			// a.getAccess().setPassword("password");
			a.getCreatedBy().setUserId("admin");
			try
			{
				a.getDates().setOpenDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("06/01/07"));
				a.getDates().setDueDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("09/22/07"));
			}
			catch (ParseException e)
			{
			}
			a.getGrading().setAutoRelease(Boolean.TRUE);
			a.getGrading().setGradebookIntegration(Boolean.FALSE);
			a.getGrading().setShowIdentities(Boolean.TRUE);
			a.getPresentation().setText("This is assessment one.");
			a.getReview().setShowCorrectAnswer(Boolean.TRUE);
			a.getReview().setShowFeedback(Boolean.TRUE);
			a.getReview().setTiming(ReviewTiming.submitted);
			a.getSubmitPresentation().setText("Thanks for all the fish!");
			a.getParts().setContinuousNumbering(Boolean.TRUE);
			a.getParts().setShowPresentation(Boolean.TRUE);

			ManualPart p = a.getParts().addManualPart();
			p.setRandomize(Boolean.FALSE);
			p.setTitle("Part one");
			((PartImpl) p).initId("p1");
			p.addQuestion(this.questionService.getQuestion("q1"));
			p.addQuestion(this.questionService.getQuestion("q2"));
			p.getPresentation().setText("This is part one.");

			p = a.getParts().addManualPart();
			p.setRandomize(Boolean.FALSE);
			p.setTitle("Part two");
			((PartImpl) p).initId("p2");
			p.addQuestion(this.questionService.getQuestion("q3"));
			p.getPresentation().setText("This is part two.");

			this.assessments.put(a.getId(), a);

			//

			a = newAssessment();
			a.initId("a2");
			a.setActive(Boolean.TRUE);
			a.setContext("mercury");
			a.setCreatedBy("admin");
			a.setNumSubmissionsAllowed(5);
			a.setQuestionGrouping(QuestionGrouping.question);
			a.setRandomAccess(Boolean.TRUE);
			// a.setTimeLimit(1200l * 1000l);
			a.setTitle("assessment two");
			a.setType(AssessmentType.test);
			// a.getAccess().setPassword("password");
			a.getCreatedBy().setUserId("admin");
			try
			{
				a.getDates().setOpenDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("07/01/07"));
				a.getDates().setDueDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("08/15/07"));
			}
			catch (ParseException e)
			{
			}
			a.getGrading().setAutoRelease(Boolean.TRUE);
			a.getGrading().setGradebookIntegration(Boolean.FALSE);
			a.getGrading().setShowIdentities(Boolean.TRUE);
			a.getPresentation().setText("This is assessment two.");
			a.getReview().setShowCorrectAnswer(Boolean.TRUE);
			a.getReview().setShowFeedback(Boolean.TRUE);
			a.getReview().setTiming(ReviewTiming.submitted);
			a.getSubmitPresentation().setText("Have a nice day!");
			a.getParts().setContinuousNumbering(Boolean.TRUE);
			a.getParts().setShowPresentation(Boolean.TRUE);

			DrawPart p2 = a.getParts().addDrawPart();
			p2.addPool(this.poolService.getPool("b1"), 2);
			p2.setTitle("Part one");
			((PartImpl) p2).initId("p3");
			p2.getPresentation().setText("This is part one.");

			this.assessments.put(a.getId(), a);
		}
	}
}
