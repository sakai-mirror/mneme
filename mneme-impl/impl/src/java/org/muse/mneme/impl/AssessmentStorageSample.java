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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAccess;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentType;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionGrouping;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.ReviewTiming;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.util.StringUtil;

/**
 * QuestionStorageSample defines a sample storage for questions.
 */
public class AssessmentStorageSample implements AssessmentStorage
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentStorageSample.class);

	protected Map<String, AssessmentImpl> assessments = new HashMap<String, AssessmentImpl>();

	protected AssessmentService assessmentService = null;

	protected boolean fakedAlready = false;

	protected Object idGenerator = new Object();

	protected long nextAccessId = 100;

	protected long nextAssessmentId = 100;

	protected long nextPartId = 100;

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
			if (assessment.getContext().equals(context) && !assessment.getArchived() && !assessment.isHistorical())
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
	public List<Assessment> getArchivedAssessments(String context)
	{
		fakeIt();

		List<Assessment> rv = new ArrayList<Assessment>();

		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(context) && assessment.getArchived())
			{
				rv.add(new AssessmentImpl(assessment));
			}
		}

		// sort - archive date ascending
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = ((Assessment) arg0).getDates().getArchivedDate().compareTo(((Assessment) arg1).getDates().getArchivedDate());
				return rv;
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl getAssessment(String id)
	{
		if (id == null) return null;

		fakeIt();

		AssessmentImpl rv = this.assessments.get(id);
		if (rv != null)
		{
			rv = new AssessmentImpl(rv);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context, final AssessmentService.AssessmentsSort sort, Boolean publishedOnly)
	{
		fakeIt();

		List<Assessment> rv = new ArrayList<Assessment>();

		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(context) && !assessment.getArchived() && !assessment.isHistorical())
			{
				// filter out unpublished if requested
				if (publishedOnly)
				{
					if (!assessment.getPublished()) continue;
				}
				rv.add(new AssessmentImpl(assessment));
			}
		}

		// sort
		Collections.sort(rv, new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				int rv = 0;
				switch (sort)
				{
					case published_a:
					{
						rv = ((Assessment) arg0).getPublished().compareTo(((Assessment) arg1).getPublished());
						break;
					}
					case published_d:
					{
						rv = -1 * ((Assessment) arg0).getPublished().compareTo(((Assessment) arg1).getPublished());
						break;
					}
					case title_a:
					{
						String s0 = StringUtil.trimToZero(((Assessment) arg0).getTitle());
						String s1 = StringUtil.trimToZero(((Assessment) arg1).getTitle());
						rv = s0.compareToIgnoreCase(s1);
						break;
					}
					case title_d:
					{
						String s0 = StringUtil.trimToZero(((Assessment) arg0).getTitle());
						String s1 = StringUtil.trimToZero(((Assessment) arg1).getTitle());
						rv = -1 * s0.compareToIgnoreCase(s1);
						break;
					}
					case type_a:
					{
						rv = ((Assessment) arg0).getType().getSortValue().compareTo(((Assessment) arg1).getType().getSortValue());
						break;
					}
					case type_d:
					{
						rv = -1 * ((Assessment) arg0).getType().getSortValue().compareTo(((Assessment) arg1).getType().getSortValue());
						break;
					}
					case odate_a:
					{
						// no open date sorts low
						if (((Assessment) arg0).getDates().getOpenDate() == null)
						{
							if (((Assessment) arg1).getDates().getOpenDate() == null) return 0;
							return -1;
						}
						if (((Assessment) arg1).getDates().getOpenDate() == null)
						{
							return 1;
						}
						rv = ((Assessment) arg0).getDates().getOpenDate().compareTo(((Assessment) arg1).getDates().getOpenDate());
						break;
					}
					case odate_d:
					{
						// no open date sorts low
						if (((Assessment) arg0).getDates().getOpenDate() == null)
						{
							if (((Assessment) arg1).getDates().getOpenDate() == null) return 0;
							return 1;
						}
						if (((Assessment) arg1).getDates().getOpenDate() == null)
						{
							return -1;
						}
						rv = -1 * ((Assessment) arg0).getDates().getOpenDate().compareTo(((Assessment) arg1).getDates().getOpenDate());
						break;
					}
					case ddate_a:
					{
						// no open date sorts high
						if (((Assessment) arg0).getDates().getDueDate() == null)
						{
							if (((Assessment) arg1).getDates().getDueDate() == null) return 0;
							return 1;
						}
						if (((Assessment) arg1).getDates().getDueDate() == null)
						{
							return -1;
						}
						rv = ((Assessment) arg0).getDates().getDueDate().compareTo(((Assessment) arg1).getDates().getDueDate());
						break;
					}
					case ddate_d:
					{
						// no open date sorts high
						if (((Assessment) arg0).getDates().getDueDate() == null)
						{
							if (((Assessment) arg1).getDates().getDueDate() == null) return 0;
							return -1;
						}
						if (((Assessment) arg1).getDates().getDueDate() == null)
						{
							return 1;
						}
						rv = -1 * ((Assessment) arg0).getDates().getDueDate().compareTo(((Assessment) arg1).getDates().getDueDate());
						break;
					}
				}

				return rv;
			}
		});

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
	public Boolean liveDependencyExists(Pool pool, boolean directOnly)
	{
		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(pool.getContext()) && assessment.getIsLive())
			{
				// if the asssessment's parts use this pool
				for (Part part : assessment.getParts().getParts())
				{
					if (((PartImpl) part).dependsOn(pool, directOnly))
					{
						return Boolean.TRUE;
					}
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean liveDependencyExists(Question question)
	{
		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(question.getPool().getContext()) && assessment.getIsLive())
			{
				// if the asssessment's parts use this question
				for (Part part : assessment.getParts().getParts())
				{
					if (((PartImpl) part).dependsOn(question))
					{
						return Boolean.TRUE;
					}
				}
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl newAssessment()
	{
		return new AssessmentImpl(this.assessmentService, this.poolService, this.questionService, this.submissionService);
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentImpl newAssessment(AssessmentImpl assessment)
	{
		return new AssessmentImpl(assessment);
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
		boolean idsNeeded = false;
		if (assessment.getId() == null)
		{
			long id = 0;
			synchronized (this.idGenerator)
			{
				id = this.nextAssessmentId;
				this.nextAssessmentId++;
			}
			assessment.initId("a" + Long.toString(id));

			// we will generate new ids for parts even if they have old ones (for making copies of assessments)
			idsNeeded = true;
		}

		// assign part ids
		for (Part part : assessment.getParts().getParts())
		{
			if (idsNeeded || (part.getId() == null))
			{
				long id = 0;
				synchronized (this.idGenerator)
				{
					id = this.nextPartId;
					this.nextPartId++;
				}
				((PartImpl) part).initId("p" + Long.toString(id));
			}
		}

		// assign special access ids
		for (AssessmentAccess access : assessment.getSpecialAccess().getAccess())
		{
			if (idsNeeded || (access.getId() == null))
			{
				long id = 0;
				synchronized (this.idGenerator)
				{
					id = this.nextAccessId;
					this.nextAccessId++;
				}
				((AssessmentAccessImpl) access).initId("x" + Long.toString(id));
			}
		}

		// save a copy
		AssessmentImpl copy = new AssessmentImpl(assessment);

		this.assessments.put(assessment.getId(), copy);
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
	 * Set the QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * Set the SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		this.submissionService = service;
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchLiveDependency(Pool from, Pool to, boolean directOnly)
	{
		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(from.getContext()) && assessment.getIsLive())
			{
				// if the asssessment's parts use this pool
				for (Part part : assessment.getParts().getParts())
				{
					if (((PartImpl) part).dependsOn(from, directOnly))
					{
						((PartImpl) part).switchPool(from, to, directOnly);
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void switchLiveDependency(Question from, Question to)
	{
		for (AssessmentImpl assessment : this.assessments.values())
		{
			if (assessment.getContext().equals(from.getPool().getContext()) && assessment.getIsLive())
			{
				// if the asssessment's manual parts use this question
				for (Part part : assessment.getParts().getParts())
				{
					if (part instanceof ManualPart)
					{
						if (((ManualPartImpl) part).dependsOn(from))
						{
							((ManualPartImpl) part).switchQuestion(from, to);
						}
					}
				}
			}
		}
	}

	protected void fakeIt()
	{
		if (!fakedAlready)
		{
			fakedAlready = true;

			Date now = new Date();

			AssessmentImpl a = newAssessment();
			a.initId("a1");
			a.setPublished(Boolean.TRUE);
			a.setContext("mercury");
			a.getCreatedBy().setUserId("admin");
			a.setTries(1);
			a.setQuestionGrouping(QuestionGrouping.question);
			a.setRandomAccess(Boolean.TRUE);
			a.setTimeLimit(1200l * 1000l);
			a.setTitle("assessment one");
			a.setType(AssessmentType.test);
			// a.getAccess().setPassword("password");
			a.getCreatedBy().setUserId("admin");
			a.getCreatedBy().setDate(now);
			a.getModifiedBy().setUserId("admin");
			a.getModifiedBy().setDate(now);
			try
			{
				a.getDates().setOpenDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("09/01/07"));
				a.getDates().setDueDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("10/22/07"));
			}
			catch (ParseException e)
			{
			}
			a.getGrading().setAutoRelease(Boolean.TRUE);
			a.getGrading().setGradebookIntegration(Boolean.FALSE);
			a.getGrading().setAnonymous(Boolean.TRUE);
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
			p.addQuestion(this.questionService.getQuestion("q3"));
			p.addQuestion(this.questionService.getQuestion("q4"));
			p.getPresentation().setText("This is part one.");

			p = a.getParts().addManualPart();
			p.setRandomize(Boolean.FALSE);
			p.setTitle("Part two");
			((PartImpl) p).initId("p2");

			p.addQuestion(this.questionService.getQuestion("q5"));
			p.addQuestion(this.questionService.getQuestion("q7"));
			p.addQuestion(this.questionService.getQuestion("q8"));
			p.getPresentation().setText("This is part two.");

			a.clearChanged();
			this.assessments.put(a.getId(), a);

			//

			a = newAssessment();
			a.initId("a2");
			a.setPublished(Boolean.TRUE);
			a.setContext("mercury");
			a.getCreatedBy().setUserId("admin");
			a.setTries(5);
			a.setQuestionGrouping(QuestionGrouping.question);
			a.setRandomAccess(Boolean.TRUE);
			// a.setTimeLimit(1200l * 1000l);
			a.setTitle("assessment two");
			a.setType(AssessmentType.test);
			// a.getAccess().setPassword("password");
			a.getCreatedBy().setUserId("admin");
			a.getCreatedBy().setDate(now);
			a.getModifiedBy().setUserId("admin");
			a.getModifiedBy().setDate(now);
			try
			{
				a.getDates().setOpenDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("09/01/07"));
				a.getDates().setDueDate(DateFormat.getDateInstance(DateFormat.SHORT).parse("10/15/07"));
			}
			catch (ParseException e)
			{
			}
			a.getGrading().setAutoRelease(Boolean.TRUE);
			a.getGrading().setGradebookIntegration(Boolean.FALSE);
			a.getGrading().setAnonymous(Boolean.TRUE);
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

			a.clearChanged();
			this.assessments.put(a.getId(), a);

			//

			a = newAssessment();
			a.initId("a3");
			a.setPublished(Boolean.TRUE);
			a.setContext("mercury");
			a.getCreatedBy().setUserId("admin");
			a.setTries(5);
			a.setQuestionGrouping(QuestionGrouping.question);
			a.setRandomAccess(Boolean.TRUE);
			// a.setTimeLimit(1200l * 1000l);
			a.setTitle("assessment three");
			a.setType(AssessmentType.test);
			// a.getAccess().setPassword("password");
			a.getCreatedBy().setUserId("admin");
			a.getCreatedBy().setDate(now);
			a.getModifiedBy().setUserId("admin");
			a.getModifiedBy().setDate(now);
			a.getGrading().setAutoRelease(Boolean.TRUE);
			a.getGrading().setGradebookIntegration(Boolean.FALSE);
			a.getGrading().setAnonymous(Boolean.TRUE);
			a.getPresentation().setText("This is assessment three.");
			a.getReview().setShowCorrectAnswer(Boolean.TRUE);
			a.getReview().setShowFeedback(Boolean.TRUE);
			a.getReview().setTiming(ReviewTiming.submitted);
			a.getSubmitPresentation().setText("Have a nice day!");
			a.getParts().setContinuousNumbering(Boolean.TRUE);
			a.getParts().setShowPresentation(Boolean.TRUE);

			p = a.getParts().addManualPart();
			p.addQuestion(this.questionService.getQuestion("q3"));
			p.addQuestion(this.questionService.getQuestion("q4"));
			p.setTitle("Part one");
			((PartImpl) p).initId("p4");
			p.getPresentation().setText("This is part 1.");

			a.clearChanged();
			this.assessments.put(a.getId(), a);
		}
	}
}
