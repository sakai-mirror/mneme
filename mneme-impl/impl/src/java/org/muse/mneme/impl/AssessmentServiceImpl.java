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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentStatus;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MnemeService;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionPart;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.SecurityService;
import org.muse.mneme.api.SubmissionService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;

/**
 * AssessmentServiceImpl implements AssessmentService.
 */
public class AssessmentServiceImpl implements AssessmentService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentServiceImpl.class);

	/** A cache of assessments. */
	protected Cache m_assessmentCache = null;

	/** The # seconds between cache cleaning runs. */
	protected int m_cacheCleanerSeconds = 0;

	/** The # seconds to cache assessment reads. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

	/** Dependency: EntityManager */
	protected EntityManager m_entityManager = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

	/** Dependency: MemoryService */
	protected MemoryService m_memoryService = null;

	/** Dependency: SecurityService */
	protected SecurityService m_securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/** Dependency: SubmissionService */
	protected SubmissionService m_submissionService = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager m_threadLocalManager = null;

	/** How long to wait (ms) between checks for timed-out submission in the db. 0 disables. */
	protected long m_timeoutCheckMs = 1000L * 300L;

	/** Dependency: TimeService */
	protected TimeService m_timeService = null;

	/**
	 * {@inheritDoc}
	 */
	public void addAssessment(final Assessment assessment) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("addAssessment: " + assessment.getId());

		// check permission - created by user must have PUBLISH_PERMISSION in the context of the assessment
		m_securityService.secure(m_sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, assessment.getContext());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				addAssessmentTx(assessment);
			}
		}, "addAssessment:" + assessment.getId());

		// event track it
		if (m_threadLocalManager.get("sakai.event.suppress") == null)
		{
			m_eventTrackingService.post(m_eventTrackingService.newEvent(MnemeService.TEST_ADD, getAssessmentReference(assessment.getId()), true));
		}

		// cache a copy
		cacheAssessment(new AssessmentImpl((AssessmentImpl) assessment));
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowAddAssessment(String context)
	{
		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = m_securityService.checkSecurity(m_sessionManager.getCurrentSessionUserId(), MnemeService.MANAGE_PERMISSION, context);

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context)
	{
		// check permission - user must have SUBMIT_PERMISSION in the context
		boolean ok = m_securityService.checkSecurity(m_sessionManager.getCurrentSessionUserId(), MnemeService.SUBMIT_PERMISSION, context);

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer countAssessments(String context)
	{
		String statement = "SELECT COUNT(P.ID) FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?";

		Object[] fields = new Object[2];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;
		List results = m_sqlService.dbRead(statement, fields, null);
		if (results.size() > 0)
		{
			return new Integer((String) results.get(0));
		}

		return new Integer(0);
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
	public Assessment getAssessment(final String id)
	{
		// TODO: should the main and question reads be in a single transaction? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("getAssessment: " + id);

		if (id == null) return null;

		// see what's cached
		AssessmentImpl cached = getCachedAssessment(id);

		// if not cached, cache a placeholder first
		if (cached == null)
		{
			cached = new AssessmentImpl(this);
			cached.initId(id);
			cacheAssessment(cached);
		}

		// lock and update the cached
		synchronized (cached)
		{
			// if we need to, read the main info
			if (!cached.isMainInited())
			{
				boolean found = readAssessmentMain(cached);
				if (!found) return null;
			}

			// if we need to, read the sections
			if (!cached.isSectionsInited())
			{
				readAssessmentSections(cached);
			}

			// if we need to, read the attachments
			if (!cached.isAttachmentsInited())
			{
				readAssessmentAttachments(cached);
			}

			// return a copy
			return new AssessmentImpl(cached);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Assessment> getContextAssessments(String context)
	{
		String statement = "SELECT P.ID" + " FROM SAM_PUBLISHEDASSESSMENT_T P "
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?";

		Object[] fields = new Object[2];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;

		final List<Assessment> rv = new ArrayList<Assessment>();
		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String assesmentId = result.getString(1);
					Assessment a = idAssessment(assesmentId);
					rv.add(a);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getContextAssessments: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment idAssessment(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("idAssessment: " + id);

		if (id == null) return null;

		// cached?
		AssessmentImpl assessment = getCachedAssessment(id);
		if (assessment != null)
		{
			// return a copy
			synchronized (assessment)
			{
				return new AssessmentImpl(assessment);
			}
		}

		// TODO: perhaps don't check, just set the id... then we need to support objects that have id set but are known to be bad...
		// -ggolden
		// check that it exists
		if (!checkAssessment(id)) return null;

		if (M_log.isDebugEnabled()) M_log.debug("idAssessment: creating: " + id);

		// setup a new assessment with only the id
		assessment = new AssessmentImpl(this);
		assessment.initId(id);

		// cache a copy
		cacheAssessment(new AssessmentImpl(assessment));

		return assessment;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// <= 0 indicates no caching desired
			if ((m_cacheSeconds > 0) && (m_cacheCleanerSeconds > 0))
			{
				// assessment and submissions caches, automatiaclly checking for expiration as configured mins, expire on events...
				m_assessmentCache = m_memoryService.newHardCache(m_cacheCleanerSeconds, getAssessmentReference(""));
			}

			M_log.info("init(): caching minutes: " + m_cacheSeconds / 60 + " cache cleaner minutes: " + m_cacheCleanerSeconds / 60
					+ " timout check seconds: " + m_timeoutCheckMs / 1000);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Assessment newAssessment()
	{
		AssessmentImpl assessment = new AssessmentImpl(this);
		assessment.setInited();

		return assessment;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentAnswer newAssessmentAnswer(QuestionPart part)
	{
		AssessmentAnswerImpl answer = new AssessmentAnswerImpl();
		answer.initPart((QuestionPartImpl) part);
		((QuestionPartImpl) part).answers.add(answer);

		return answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentQuestion newQuestion(AssessmentSection section)
	{
		AssessmentQuestionImpl question = new AssessmentQuestionImpl();
		question.initSection((AssessmentSectionImpl) section);
		((AssessmentSectionImpl) section).questions.add(question);

		return question;
	}

	/**
	 * {@inheritDoc}
	 */
	public QuestionPart newQuestionPart(AssessmentQuestion question)
	{
		QuestionPartImpl part = new QuestionPartImpl();
		part.initQuestion((AssessmentQuestionImpl) question);
		((AssessmentQuestionImpl) question).parts.add(part);

		return part;
	}

	/**
	 * {@inheritDoc}
	 */
	public AssessmentSection newSection(Assessment assessment)
	{
		AssessmentSectionImpl section = new AssessmentSectionImpl();
		section.initAssement((AssessmentImpl) assessment);
		((AssessmentImpl) assessment).sections.add(section);

		return section;
	}

	/**
	 * Set the # minutes between cache cleanings.
	 * 
	 * @param time
	 *        The # minutes between cache cleanings. (as an integer string).
	 */
	public void setCacheCleanerMinutes(String time)
	{
		m_cacheCleanerSeconds = Integer.parseInt(time) * 60;
	}

	/**
	 * Set the # minutes to cache.
	 * 
	 * @param time
	 *        The # minutes to cache a get (as an integer string).
	 */
	public void setCacheMinutes(String time)
	{
		m_cacheSeconds = Integer.parseInt(time) * 60;
	}

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		m_eventTrackingService = service;
	}

	/**
	 * Dependency: MemoryService.
	 * 
	 * @param service
	 *        The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		m_securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		m_sessionManager = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Dependency: SubmissionService.
	 * 
	 * @param service
	 *        The SubmissionService.
	 */
	public void setSubmissionService(SubmissionService service)
	{
		m_submissionService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The ThreadLocalManager.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		m_threadLocalManager = service;
	}

	/**
	 * Set the # seconds to wait between db checks for timed-out submissions.
	 * 
	 * @param time
	 *        The # seconds to wait between db checks for timed-out submissions.
	 */
	public void setTimeoutCheckSeconds(String time)
	{
		m_timeoutCheckMs = Integer.parseInt(time) * 1000L;
	}

	/**
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
	}

	/**
	 * Transaction code for addAssessment.
	 */
	protected void addAssessmentTx(Assessment assessment)
	{
		// we only work with our impl
		AssessmentImpl a = (AssessmentImpl) assessment;

		// TODO: validate that title is unique? in the db?

		// now
		Time now = m_timeService.newTime();

		// user
		String userId = m_sessionManager.getCurrentSessionUserId();

		// establish defaults
		// TODO: check context, others? check that the createdby is the current user?
		if (a.getStatus() == null) a.initStatus(AssessmentStatus.ACTIVE);
		if (a.getCreatedBy() == null) a.initCreatedBy(userId);

		// ID column? For non sequence db vendors, it is defaulted
		Long id = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", null);

		// Note: ID column is set to autoincrement... by using the special JDBC feature in dbInsert, we get the value just
		// allocated
		String statement = "INSERT INTO SAM_PUBLISHEDASSESSMENT_T"
				+ " (TITLE, DESCRIPTION, ASSESSMENTID, COMMENTS, TYPEID, INSTRUCTORNOTIFICATION, TESTEENOTIFICATION, MULTIPARTALLOWED,"
				+ " STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE" + ((id == null) ? "" : ", ID") + ")"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?" + ((id == null) ? "" : ",?") + ")";
		Object fields[] = new Object[(id == null) ? 13 : 14];
		fields[0] = a.getTitle();
		fields[1] = a.getDescription();
		fields[2] = ""; // TODO: reference to the base (not published) assessment... a.getId();
		fields[3] = ""; // TODO: comments
		fields[4] = new Integer(62); // TODO: type id
		fields[5] = new Integer(1); // TODO: instructor notification
		fields[6] = new Integer(1); // TODO: test notification
		fields[7] = new Integer(1); // TODO: multipart allowed
		fields[8] = a.getStatus().dbEncoding();
		fields[9] = a.getCreatedBy();
		fields[10] = now; // TODO: from a
		fields[11] = a.getCreatedBy(); // TODO: modifiedBy
		fields[12] = now; // TODO: from a

		if (id != null)
		{
			fields[13] = id;
			m_sqlService.dbWrite(statement, fields);
		}
		else
		{
			id = m_sqlService.dbInsert(null, statement, fields, "ID");
		}

		// we really need that id
		if (id == null) throw new RuntimeException("failed to insert published assessment");

		// update the id
		a.initId(id.toString());

		// each section
		int sectionPosition = 1;
		for (AssessmentSectionImpl section : a.sections)
		{
			// ID column? For non sequence db vendors, it is defaulted
			Long sectionId = m_sqlService.getNextSequence("SAM_PUBLISHEDSECTION_ID_S", null);

			statement = "INSERT INTO SAM_PUBLISHEDSECTION_T"
					+ " (ASSESSMENTID, DURATION, SEQUENCE, TITLE, DESCRIPTION, TYPEID, STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE"
					+ ((sectionId == null) ? "" : ", SECTIONID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?" + ((sectionId == null) ? "" : ",?") + ")";
			fields = new Object[(sectionId == null) ? 11 : 12];
			fields[0] = id;
			fields[1] = null;
			fields[2] = new Integer(sectionPosition++);
			fields[3] = section.getTitle(); // TODO: "Default"?
			fields[4] = section.getDescription();
			fields[5] = new Integer(21); // TODO: type?
			fields[6] = new Integer(1); // TODO: status?
			fields[7] = a.getCreatedBy();
			fields[8] = now; // TODO: a.getCreated();
			fields[9] = a.getCreatedBy(); // TODO: a.getModifiedBy()
			fields[10] = now; // TODO: a.getModified();

			if (sectionId != null)
			{
				fields[11] = sectionId;
				m_sqlService.dbWrite(statement, fields);
			}
			else
			{
				sectionId = m_sqlService.dbInsert(null, statement, fields, "SECTIONID");
			}

			// we really need that id
			if (sectionId == null) throw new RuntimeException("failed to insert section");
			section.initId(sectionId.toString());

			// ID for SAM_PUBLISHEDSECTIONMETADATA_T
			// Note: Samigo as of 2.3 has a bug - using the same sequence as for the ID of SAM_PUBLISHEDASSESSMENT_T -ggolden
			Long xid = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", null);

			statement = "INSERT INTO SAM_PUBLISHEDSECTIONMETADATA_T (SECTIONID, LABEL, ENTRY" + ((xid == null) ? "" : ", PUBLISHEDSECTIONMETADATAID")
					+ ") values (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
			fields = new Object[(xid == null) ? 3 : 4];
			fields[0] = sectionId;
			fields[1] = "AUTHOR_TYPE";
			// TODO: this is the draw from pool (value is 2) / authored question (value is 1) - authored for now
			fields[2] = "1";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", null);
			fields[1] = "QUESTIONS_ORDERING";
			fields[2] = ((section.getRandomQuestionOrder() == null) || (!section.getRandomQuestionOrder().booleanValue())) ? "1" : "2";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(statement, fields);
		}

		// Note: no id column for SAM_PUBLISHEDACCESSCONTROL_T
		statement = "INSERT INTO SAM_PUBLISHEDACCESSCONTROL_T"
				+ " (UNLIMITEDSUBMISSIONS, SUBMISSIONSALLOWED, SUBMISSIONSSAVED, ASSESSMENTFORMAT, BOOKMARKINGITEM, TIMELIMIT,"
				+ " TIMEDASSESSMENT, RETRYALLOWED, LATEHANDLING, STARTDATE, DUEDATE, SCOREDATE, FEEDBACKDATE, RETRACTDATE, AUTOSUBMIT,"
				+ " ITEMNAVIGATION, ITEMNUMBERING, SUBMISSIONMESSAGE, RELEASETO, USERNAME, PASSWORD, FINALPAGEURL, ASSESSMENTID)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		fields = new Object[23];
		fields[0] = new Integer(1);
		fields[1] = null;
		fields[2] = new Integer(1);
		fields[3] = ((assessment.getQuestionPresentation() != null) ? assessment.getQuestionPresentation().dbEncoding() : new Integer(1));
		fields[4] = null;
		fields[5] = new Integer(0);
		fields[6] = new Integer(0);
		fields[7] = null;
		fields[8] = ((assessment.getAllowLateSubmit() != null) && assessment.getAllowLateSubmit().booleanValue()) ? new Integer(1) : new Integer(0);
		fields[9] = assessment.getReleaseDate();
		fields[10] = assessment.getDueDate();
		fields[11] = null;
		fields[12] = assessment.getFeedbackDate();
		fields[13] = assessment.getRetractDate();
		fields[14] = new Integer(1);
		fields[15] = ((assessment.getRandomAccess() != null) && assessment.getRandomAccess().booleanValue()) ? new Integer(2) : new Integer(1);
		fields[16] = ((assessment.getContinuousNumbering() != null) && assessment.getContinuousNumbering().booleanValue()) ? new Integer(1)
				: new Integer(2);
		fields[17] = a.getSubmitMessage();
		fields[18] = a.getContext() + " site";
		fields[19] = "";
		fields[20] = a.getPassword();
		fields[21] = a.getSubmitUrl();
		fields[22] = id;
		m_sqlService.dbWrite(statement, fields);

		// Note: no id column for SAM_PUBLISHEDEVALUATION_T
		statement = "INSERT INTO SAM_PUBLISHEDEVALUATION_T"
				+ " (EVALUATIONCOMPONENTS, SCORINGTYPE, NUMERICMODELID, FIXEDTOTALSCORE, GRADEAVAILABLE, ISSTUDENTIDPUBLIC,"
				+ " ANONYMOUSGRADING, AUTOSCORING, TOGRADEBOOK, ASSESSMENTID)" + " VALUES (?,?,?,?,?,?,?,?,?,?)";
		fields = new Object[10];
		fields[0] = "";
		fields[1] = new Integer(1);
		fields[2] = "";
		fields[3] = null;
		fields[4] = null;
		fields[5] = null;
		fields[6] = new Integer(2); // 1-anon, 2-students id visible
		fields[7] = null;
		fields[8] = ((a.getGradebookIntegration() != null) && (a.gradebookIntegeration.booleanValue())) ? new Integer(1) : new Integer(2); // 1-to
		// gradebook,
		// 2-not
		fields[9] = id;
		m_sqlService.dbWrite(statement, fields);

		// Note: no id column for SAM_PUBLISHEDFEEDBACK_T
		statement = "INSERT INTO SAM_PUBLISHEDFEEDBACK_T"
				+ " (FEEDBACKDELIVERY, FEEDBACKAUTHORING, EDITCOMPONENTS, SHOWQUESTIONTEXT, SHOWSTUDENTRESPONSE,"
				+ " SHOWCORRECTRESPONSE, SHOWSTUDENTSCORE, SHOWSTUDENTQUESTIONSCORE, SHOWQUESTIONLEVELFEEDBACK,"
				+ " SHOWSELECTIONLEVELFEEDBACK, SHOWGRADERCOMMENTS, SHOWSTATISTICS, ASSESSMENTID)" + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		fields = new Object[13];
		fields[0] = assessment.getFeedbackDelivery() == null ? new Integer(1) : assessment.getFeedbackDelivery().dbEncoding();
		fields[1] = new Integer(1);
		fields[2] = new Integer(1);
		fields[3] = new Integer(1);
		fields[4] = new Integer(1);
		fields[5] = ((assessment.getFeedbackShowCorrectAnswer() == null) || (!assessment.getFeedbackShowCorrectAnswer().booleanValue())) ? new Integer(
				0)
				: new Integer(1);
		fields[6] = new Integer(1);
		fields[7] = ((assessment.getFeedbackShowQuestionScore() == null) || (!assessment.getFeedbackShowQuestionScore().booleanValue())) ? new Integer(
				0)
				: new Integer(1);
		fields[8] = ((assessment.getFeedbackShowQuestionFeedback() == null) || (!assessment.getFeedbackShowQuestionFeedback().booleanValue())) ? new Integer(
				0)
				: new Integer(1);
		fields[9] = ((assessment.getFeedbackShowAnswerFeedback() == null) || (!assessment.getFeedbackShowAnswerFeedback().booleanValue())) ? new Integer(
				0)
				: new Integer(1);
		fields[10] = new Integer(1);
		fields[11] = ((assessment.getFeedbackShowStatistics() == null) || (!assessment.getFeedbackShowStatistics().booleanValue())) ? new Integer(0)
				: new Integer(1);
		fields[12] = id;
		m_sqlService.dbWrite(statement, fields);

		// ID for SAM_PUBLISHEDMETADATA_T
		Long xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);

		statement = "INSERT INTO SAM_PUBLISHEDMETADATA_T (ASSESSMENTID, LABEL, ENTRY" + ((xid == null) ? "" : ", ASSESSMENTMETADATAID")
				+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
		fields = new Object[(xid == null) ? 3 : 4];
		fields[0] = id;
		fields[1] = "assessmentAuthor_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "retractDate_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "description_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "testeeIdentity_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "anonymousRelease_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "timedAssessment_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "displayNumbering_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "feedbackType_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "submissionModel_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "templateInfo_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "timedAssessmentAutoSubmit_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "releaseDate_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "releaseTo";
		fields[2] = "SITE_MEMBERS";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "authenticatedRelease_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "hasTimeAssessment";
		fields[2] = "false";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "displayChunking_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "metadataAssess_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "passwordRequired_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "lateHandling_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "recordedScore_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "author";
		fields[2] = "";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "itemAccessType_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "hasMetaDataForQuestions";
		fields[2] = "false";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "toGradebook_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "bgImage_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "dueDate_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "ipAccessType_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "finalPageURL_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "feedbackComponents_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "feedbackAuthoring_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "metadataQuestions_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "submissionMessage_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "bgColor_isInstructorEditable";
		fields[2] = "true";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", null);
		fields[1] = "ALIAS";
		fields[2] = IdManager.createUuid();
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		// ID for SAM_PUBLISHEDSECUREDIP_T
		xid = m_sqlService.getNextSequence("SAM_PUBLISHEDSECUREDIP_ID_S", null);

		statement = "INSERT INTO SAM_PUBLISHEDSECUREDIP_T (ASSESSMENTID, HOSTNAME, IPADDRESS" + ((xid == null) ? "" : ", IPADDRESSID")
				+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
		fields = new Object[(xid == null) ? 3 : 4];
		fields[0] = id;
		fields[1] = null;
		fields[2] = "";
		if (xid != null) fields[3] = xid;
		m_sqlService.dbWrite(statement, fields);

		// ID for SAM_AUTHZDATA_S
		xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", null);

		statement = "INSERT INTO SAM_AUTHZDATA_T (lockId, AGENTID, FUNCTIONID, QUALIFIERID, EFFECTIVEDATE, EXPIRATIONDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE, ISEXPLICIT"
				+ ((xid == null) ? "" : ", ID") + ")" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
		fields = new Object[(xid == null) ? 9 : 10];
		fields[0] = new Integer(0);
		fields[1] = a.getContext();
		fields[2] = "OWN_PUBLISHED_ASSESSMENT";
		fields[3] = id;
		fields[4] = null;
		fields[5] = null;
		fields[6] = "someone";
		fields[7] = now;
		fields[8] = null;
		if (xid != null) fields[9] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", null);
		fields[2] = "TAKE_PUBLISHED_ASSESSMENT";
		if (xid != null) fields[9] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", null);
		fields[2] = "VIEW_PUBLISHED_ASSESSMENT_FEEDBACK";
		if (xid != null) fields[9] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", null);
		fields[2] = "GRADE_PUBLISHED_ASSESSMENT";
		if (xid != null) fields[9] = xid;
		m_sqlService.dbWrite(statement, fields);

		xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", null);
		fields[2] = "VIEW_PUBLISHED_ASSESSMENT";
		if (xid != null) fields[9] = xid;
		m_sqlService.dbWrite(statement, fields);

		// questions - from each section
		for (AssessmentSectionImpl section : a.sections)
		{
			int questionPosition = 1;
			for (AssessmentQuestionImpl question : section.questions)
			{
				// write the question
				Long questionId = m_sqlService.getNextSequence("SAM_PUBITEM_ID_S", null);
				statement = "INSERT INTO SAM_PUBLISHEDITEM_T"
						+ " (SECTIONID, SEQUENCE, TYPEID, SCORE, HASRATIONALE, STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE, INSTRUCTION"
						+ ((questionId == null) ? "" : ", ITEMID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?" + ((questionId == null) ? "" : ",?")
						+ ")";
				fields = new Object[(questionId == null) ? 11 : 12];
				fields[0] = Integer.valueOf(section.getId());
				fields[1] = new Integer(questionPosition++);
				fields[2] = question.getType().getDbEncoding();
				fields[3] = question.getPoints();
				fields[4] = question.getRequireRationale();
				fields[5] = new Integer(1);
				fields[6] = userId;
				fields[7] = now;
				fields[8] = userId;
				fields[9] = now;
				fields[10] = question.getInstructions();

				if (questionId != null)
				{
					fields[11] = questionId;
					m_sqlService.dbWrite(statement, fields);
				}
				else
				{
					questionId = m_sqlService.dbInsert(null, statement, fields, "ITEMID");
				}

				// we really need that id
				if (questionId == null) throw new RuntimeException("failed to insert question");
				question.initId(questionId.toString());

				// parts (these go into the item text table)
				int sequence = 1;
				for (QuestionPart part : question.getParts())
				{
					Long partId = m_sqlService.getNextSequence("SAM_PUBITEMTEXT_ID_S", null);
					statement = "INSERT INTO SAM_PUBLISHEDITEMTEXT_T (ITEMID, SEQUENCE, TEXT" + ((partId == null) ? "" : ", ITEMTEXTID") + ")"
							+ " VALUES (?, ?, ?" + ((partId == null) ? "" : ",?") + ")";
					fields = new Object[(partId == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = new Integer(sequence++);
					fields[2] = part.getTitle();
					if (partId != null)
					{
						fields[3] = partId;
						m_sqlService.dbWrite(statement, fields);
					}
					else
					{
						partId = m_sqlService.dbInsert(null, statement, fields, "ITEMTEXTID");
					}
					((QuestionPartImpl) part).initId(partId.toString());

					// answers - from each part
					int answerPosition = 1;
					for (AssessmentAnswer answer : part.getAnswersAsAuthored())
					{
						Long answerId = m_sqlService.getNextSequence("SAM_PUBANSWER_ID_S", null);
						statement = "INSERT INTO SAM_PUBLISHEDANSWER_T" + " (ITEMTEXTID, ITEMID, TEXT, SEQUENCE, LABEL, ISCORRECT, SCORE"
								+ ((answerId == null) ? "" : ", ANSWERID") + ")" + " VALUES (?,?,?,?,?,?,?" + ((answerId == null) ? "" : ",?") + ")";
						fields = new Object[(answerId == null) ? 7 : 8];
						fields[0] = partId;
						fields[1] = questionId;
						fields[2] = answer.getText();
						fields[3] = new Integer(answerPosition++);
						fields[4] = answer.getLabel();
						fields[5] = answer.getIsCorrect();
						fields[6] = question.getPoints();

						if (answerId != null)
						{
							fields[7] = answerId;
							m_sqlService.dbWrite(statement, fields);
						}
						else
						{
							answerId = m_sqlService.dbInsert(null, statement, fields, "ANSWERID");
						}

						// we really need that id
						if (answerId == null) throw new RuntimeException("failed to insert answer");
						((AssessmentAnswerImpl) answer).initId(answerId.toString());

						// answer feedback
						if (answer.getFeedbackIncorrect() != null)
						{
							xid = m_sqlService.getNextSequence("SAM_PUBANSWERFEEDBACK_ID_S", null);
							statement = "INSERT INTO SAM_PUBLISHEDANSWERFEEDBACK_T (ANSWERID, TYPEID, TEXT"
									+ ((xid == null) ? "" : ", ANSWERFEEDBACKID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
							fields = new Object[(xid == null) ? 3 : 4];
							fields[0] = answerId;
							fields[1] = "InCorrect Feedback";
							fields[2] = answer.getFeedbackIncorrect();
							if (xid != null) fields[3] = xid;
							m_sqlService.dbWrite(statement, fields);
						}

						if (answer.getFeedbackCorrect() != null)
						{
							xid = m_sqlService.getNextSequence("SAM_PUBANSWERFEEDBACK_ID_S", null);
							statement = "INSERT INTO SAM_PUBLISHEDANSWERFEEDBACK_T (ANSWERID, TYPEID, TEXT"
									+ ((xid == null) ? "" : ", ANSWERFEEDBACKID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
							fields = new Object[(xid == null) ? 3 : 4];
							fields[0] = answerId;
							fields[1] = "Correct Feedback";
							fields[2] = answer.getFeedbackCorrect();
							if (xid != null) fields[3] = xid;
							m_sqlService.dbWrite(statement, fields);
						}

						if (answer.getFeedbackGeneral() != null)
						{
							xid = m_sqlService.getNextSequence("SAM_PUBANSWERFEEDBACK_ID_S", null);
							statement = "INSERT INTO SAM_PUBLISHEDANSWERFEEDBACK_T (ANSWERID, TYPEID, TEXT"
									+ ((xid == null) ? "" : ", ANSWERFEEDBACKID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
							fields = new Object[(xid == null) ? 3 : 4];
							fields[0] = answerId;
							fields[1] = "General Feedback";
							fields[2] = answer.getFeedbackGeneral();
							if (xid != null) fields[3] = xid;
							m_sqlService.dbWrite(statement, fields);
						}
					}
				}

				// question feedback
				if (question.getFeedbackIncorrect() != null)
				{
					xid = m_sqlService.getNextSequence("SAM_PUBITEMFEEDBACK_ID_S", null);
					statement = "INSERT INTO SAM_PUBLISHEDITEMFEEDBACK_T (ITEMID, TYPEID, TEXT" + ((xid == null) ? "" : ", ITEMFEEDBACKID")
							+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
					fields = new Object[(xid == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = "InCorrect Feedback";
					fields[2] = question.getFeedbackIncorrect();
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(statement, fields);
				}

				if (question.getFeedbackCorrect() != null)
				{
					xid = m_sqlService.getNextSequence("SAM_PUBITEMFEEDBACK_ID_S", null);
					statement = "INSERT INTO SAM_PUBLISHEDITEMFEEDBACK_T (ITEMID, TYPEID, TEXT" + ((xid == null) ? "" : ", ITEMFEEDBACKID")
							+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
					fields = new Object[(xid == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = "Correct Feedback";
					fields[2] = question.getFeedbackCorrect();
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(statement, fields);
				}

				if (question.getFeedbackGeneral() != null)
				{
					xid = m_sqlService.getNextSequence("SAM_PUBITEMFEEDBACK_ID_S", null);
					statement = "INSERT INTO SAM_PUBLISHEDITEMFEEDBACK_T (ITEMID, TYPEID, TEXT" + ((xid == null) ? "" : ", ITEMFEEDBACKID")
							+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
					fields = new Object[(xid == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = "General Feedback";
					fields[2] = question.getFeedbackGeneral();
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(statement, fields);
				}

				// question metadata
				xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", null);
				statement = "INSERT INTO SAM_PUBLISHEDITEMMETADATA_T (ITEMID, LABEL, ENTRY" + ((xid == null) ? "" : ", ITEMMETADATAID")
						+ ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
				fields = new Object[(xid == null) ? 3 : 4];
				fields[0] = questionId;
				fields[1] = "POOLID";
				fields[2] = "";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(statement, fields);

				xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", null);
				fields[1] = "MUTUALLY_EXCLUSIVE";
				fields[2] = ((question.getMutuallyExclusive() != null) && question.getMutuallyExclusive().booleanValue()) ? "true" : "false";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(statement, fields);

				xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", null);
				fields[1] = "PARTID";
				fields[2] = "1"; // TODO: ???
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(statement, fields);

				xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", null);
				fields[1] = "RANDOMIZE";
				fields[2] = ((question.getRandomAnswerOrder() != null) && question.getRandomAnswerOrder().booleanValue()) ? "true" : "false";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(statement, fields);

				xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", null);
				fields[1] = "CASE_SENSITIVE";
				fields[2] = ((question.getCaseSensitive() != null) && question.getCaseSensitive().booleanValue()) ? "true" : "false";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(statement, fields);
			}
		}

		// TODO: assessment attachments into SAM_PUBLISHEDATTACHMENT_T setting ATTACHMENTID, ATTACHMENTTYPE=1, RESOURCEID from
		// the attachment ref id, ASSESSMENTID
	}

	/**
	 * Cache this assessment. Use the short-term cache if enable, else use the thread-local cache.
	 * 
	 * @param assessment
	 *        The assessment to cache.
	 */
	protected void cacheAssessment(AssessmentImpl assessment)
	{
		String ref = getAssessmentReference(assessment.getId());

		// if we are short-term caching
		if (m_assessmentCache != null)
		{
			m_assessmentCache.put(ref, assessment, m_cacheSeconds);
		}

		// else thread-local cache
		else
		{
			m_threadLocalManager.set(ref, assessment);
		}
	}

	/**
	 * Check if an assessment is defined.
	 * 
	 * @param id
	 *        The assessment id to check.
	 */
	protected boolean checkAssessment(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("checkAssessment: " + id);

		String statement = "SELECT P.ID FROM SAM_PUBLISHEDASSESSMENT_T P WHERE P.ID = ?";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(id);

		List results = m_sqlService.dbRead(statement, fields, null);
		return !results.isEmpty();
	}

	/**
	 * Form an assessment reference for this assessment id.
	 * 
	 * @param assessmentId
	 *        the assessment id.
	 * @return the assessment reference for this assessment id.
	 */
	protected String getAssessmentReference(String assessmentId)
	{
		String ref = MnemeService.REFERENCE_ROOT + "/" + MnemeService.ASSESSMENT_TYPE + "/" + assessmentId;
		return ref;
	}

	/**
	 * Check the cache for the assessment. Use the short-term cache if enabled, else use the thread-local cache.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The actual assessment object cached, or null if not.
	 */
	protected AssessmentImpl getCachedAssessment(String id)
	{
		String ref = getAssessmentReference(id);

		// if we are short-term caching
		if (m_assessmentCache != null)
		{
			// if it is in there
			if (m_assessmentCache.containsKey(ref))
			{
				return (AssessmentImpl) m_assessmentCache.get(ref);
			}
		}

		// if not found, check the thread-local cache
		return (AssessmentImpl) m_threadLocalManager.get(ref);
	}

	/**
	 * Id each of the assessments in the id list
	 * 
	 * @param ids
	 *        The collection of assessment ids.
	 * @return A list of id'ed assessments, one for each id.
	 */
	protected List<Assessment> idAssessments(List<String> ids)
	{
		List<Assessment> rv = new ArrayList<Assessment>(ids.size());
		for (String id : ids)
		{
			rv.add(idAssessment(id));
		}

		return rv;
	}

	/**
	 * Check if this assessment is open for submission.
	 * 
	 * @param a
	 *        The assessment.
	 * @param asOf
	 *        The time to check.
	 * @param grace
	 *        #ms grace period to allow past hard deadlines.
	 * @return
	 */
	protected boolean isAssessmentOpen(Assessment a, Time asOf, long grace)
	{
		// if we have a release date and we are not there yet
		if ((a.getReleaseDate() != null) && (asOf.before(a.getReleaseDate()))) return false;

		// if we have a retract date and we are past it, considering grace
		if ((a.getRetractDate() != null) && (asOf.getTime() > (a.getRetractDate().getTime() + grace))) return false;

		// if we have a due date, are past it, and not accepting late submissions
		if ((a.getDueDate() != null) && (asOf.getTime() > (a.getDueDate().getTime() + grace))
				&& ((a.getAllowLateSubmit() == null) || (!a.getAllowLateSubmit().booleanValue()))) return false;

		return true;
	}

	/**
	 * Read the attachments of the assessment
	 * 
	 * @param assessment
	 *        The assessment impl with the id set to fill in.
	 */
	protected void readAssessmentAttachments(final AssessmentImpl assessment)
	{
		if (M_log.isDebugEnabled()) M_log.debug("readAssessmentAttachments: " + assessment.getId());

		if (assessment.getId() == null)
		{
			M_log.warn("readAssessmentAttachments: attempt to read with no id set");
			return;
		}

		// collect the attachments
		final List<Reference> attachments = new ArrayList<Reference>();

		// get the attachments
		String statement = "SELECT A.RESOURCEID" + " FROM SAM_PUBLISHEDATTACHMENT_T A" + " WHERE A.ASSESSMENTID = ?";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String refStr = result.getString(1);

					// assume a content ref
					refStr = "/content" + refStr;

					// make a reference
					Reference ref = m_entityManager.newReference(refStr);
					attachments.add(ref);
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentAttachments: " + e);
					return null;
				}
			}
		});

		// set these into the assessment
		assessment.initAttachments(attachments);

		// update the cache if cached
		AssessmentImpl cached = getCachedAssessment(assessment.getId());
		if (cached != null)
		{
			synchronized (cached)
			{
				cached.setAttachments(assessment);
			}
		}
	}

	/**
	 * Read the main parts of the assessment (not the questions)
	 * 
	 * @param assessment
	 *        The assessment impl with the id set to fill in.
	 * @return true if we read, false if we could not find the assessment.
	 */
	protected boolean readAssessmentMain(final AssessmentImpl assessment)
	{
		if (M_log.isDebugEnabled()) M_log.debug("readAssessmentMain: " + assessment.getId());
		if (assessment.getId() == null)
		{
			M_log.warn("readAssessmentMain: attempt to read with no id set");
			return false;
		}

		String statement = "SELECT P.TITLE, AD.AGENTID, PAC.DUEDATE, PAC.FEEDBACKDATE, PE.SCORINGTYPE, P.STATUS,"
				+ " PF.FEEDBACKDELIVERY, PF.SHOWSTUDENTSCORE, PF.SHOWSTATISTICS, P.CREATEDBY,"
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.TIMELIMIT, PAC.AUTOSUBMIT, PAC.STARTDATE, PAC.RETRACTDATE, PAC.LATEHANDLING,"
				+ " PF.SHOWSTUDENTQUESTIONSCORE, PF.SHOWCORRECTRESPONSE, PF.SHOWQUESTIONLEVELFEEDBACK, PF.SHOWSELECTIONLEVELFEEDBACK,"
				+ " PAC.ITEMNAVIGATION, PAC.ITEMNUMBERING, P.DESCRIPTION, PAC.ASSESSMENTFORMAT, PE.TOGRADEBOOK, PAC.SUBMISSIONMESSAGE, PAC.FINALPAGEURL, PAC.PASSWORD"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P" + " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ?"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON P.ID = PAC.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDFEEDBACK_T PF ON P.ID = PF.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON P.ID = PE.ASSESSMENTID" + " WHERE P.ID = ?";
		Object[] fields = new Object[2];
		fields[0] = "VIEW_PUBLISHED_ASSESSMENT";
		fields[1] = Integer.valueOf(assessment.getId());

		// collect an id if we need to update the SAM_PUBLISHEDEVALUATION_T SCORINGTYPE from 2 to 1
		final List<String> toConvert = new ArrayList<String>();

		List results = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String title = result.getString(1);
					String context = result.getString(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(4, m_sqlService.getCal());
					Time feedbackDate = null;
					if (ts != null)
					{
						feedbackDate = m_timeService.newTime(ts.getTime());
					}

					// check if we need to convert from latest to highest
					if (result.getInt(5) == 2)
					{
						toConvert.add(title);
					}

					// MultipleSubmissionSelectionPolicy mssPolicy = MultipleSubmissionSelectionPolicy.parse(result.getInt(5));
					MultipleSubmissionSelectionPolicy mssPolicy = MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED;

					AssessmentStatus status = AssessmentStatus.parse(result.getInt(6));

					FeedbackDelivery delivery = FeedbackDelivery.parse(result.getInt(7));
					// boolean showStudentScore = result.getBoolean(8);
					boolean showStatistics = result.getBoolean(9);
					String createdBy = result.getString(10);
					boolean unlimitedSubmissions = result.getBoolean(11);
					int submissionsAllowed = result.getInt(12);
					long timeLimit = result.getLong(13);
					int autoSubmit = result.getInt(14);

					ts = result.getTimestamp(15, m_sqlService.getCal());
					Time releaseDate = null;
					if (ts != null)
					{
						releaseDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(16, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}
					int allowLateSubmit = result.getInt(17);
					boolean showStudentQuestionScore = result.getBoolean(18);
					boolean showCorrectAnswer = result.getBoolean(19);
					boolean showQuestionFeedback = result.getBoolean(20);
					boolean showAnswerFeedback = result.getBoolean(21);
					int randomAccess = result.getInt(22);
					int continuousNumbering = result.getInt(23);
					String description = StringUtil.trimToNull(result.getString(24));
					QuestionPresentation presentation = QuestionPresentation.parse(result.getInt(25));
					int toGradebook = result.getInt(26);
					String submitMessage = StringUtil.trimToNull(result.getString(27));
					String submitUrl = StringUtil.trimToNull(result.getString(28));
					String password = StringUtil.trimToNull(result.getString(29));

					// it the submitUrl is just "http://" null it
					if ((submitUrl != null) && ("http://".equals(submitUrl))) submitUrl = null;

					// pack it into the assessment
					assessment.initAutoSubmit((autoSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);
					assessment.initContext(context);
					assessment.initCreatedBy(createdBy);
					assessment.initDueDate(dueDate);
					assessment.initFeedbackDate(feedbackDate);
					assessment.initFeedbackDelivery(delivery);
					assessment.initFeedbackShowStatistics(Boolean.valueOf(showStatistics));
					// assessment.initFeedbackShowScore(Boolean.valueOf(showStudentScore));
					assessment.initMultipleSubmissionSelectionPolicy(mssPolicy);
					assessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));
					assessment.initStatus(status);
					assessment.initTimeLimit(timeLimit == 0 ? null : new Long(timeLimit * 1000));
					assessment.initTitle(title);
					assessment.initReleaseDate(releaseDate);
					assessment.initRetractDate(retractDate);
					assessment.initAllowLateSubmit((allowLateSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);
					assessment.initFeedbackShowQuestionScore(Boolean.valueOf(showStudentQuestionScore));
					assessment.initFeedbackShowCorrectAnswer(Boolean.valueOf(showCorrectAnswer));
					assessment.initFeedbackShowQuestionFeedback(Boolean.valueOf(showQuestionFeedback));
					assessment.initFeedbackShowAnswerFeedback(Boolean.valueOf(showAnswerFeedback));
					assessment.initRandomAccess(Boolean.valueOf(randomAccess == 2));
					assessment.initContinuousNumbering(Boolean.valueOf(continuousNumbering == 1));
					assessment.initDescription(description);
					assessment.initQuestionPresentation(presentation);
					assessment.initGradebookIntegration(Boolean.valueOf(toGradebook == 1));
					assessment.initSubmitMessage(submitMessage);
					assessment.initSubmitUrl(submitUrl);
					assessment.initPassword(password);

					return assessment;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentMain: " + e);
					return null;
				}
			}
		});

		// if we detected an assessment set to latest mss policy, change it!
		if (!toConvert.isEmpty())
		{
			statement = "UPDATE SAM_PUBLISHEDEVALUATION_T SET SCORINGTYPE = 1 WHERE ASSESSMENTID = ?";
			fields = new Object[1];
			fields[0] = Integer.valueOf(assessment.getId());
			m_sqlService.dbWrite(statement, fields);
		}

		if (!results.isEmpty())
		{
			// update the cache if cached
			AssessmentImpl cached = getCachedAssessment(assessment.getId());
			if (cached != null)
			{
				synchronized (cached)
				{
					cached.setMain(assessment);
				}
			}

			return true;
		}

		// we didn't find it
		return false;
	}

	/**
	 * Read the sections and questions of the assessment (not the main) and their attachments
	 * 
	 * @param assessment
	 *        The assessment impl with the id set to fill in.
	 */
	protected void readAssessmentSections(final AssessmentImpl assessment)
	{
		// TODO: Transaction to assure a consistent read? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("readAssessmentSections: " + assessment.getId());

		if (assessment.getId() == null)
		{
			M_log.warn("readAssessmentSections: attempt to read with no id set");
			return;
		}

		// get the sections
		String statement = "SELECT P.SECTIONID, P.TITLE, P.DESCRIPTION, SMD1.ENTRY, SMD2.ENTRY, SMD3.ENTRY " + " FROM SAM_PUBLISHEDSECTION_T P"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDSECTIONMETADATA_T SMD1 ON P.SECTIONID = SMD1.SECTIONID AND SMD1.LABEL = 'QUESTIONS_ORDERING'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDSECTIONMETADATA_T SMD2 ON P.SECTIONID = SMD2.SECTIONID AND SMD2.LABEL = 'NUM_QUESTIONS_DRAWN'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDSECTIONMETADATA_T SMD3 ON P.SECTIONID = SMD3.SECTIONID AND SMD3.LABEL = 'RANDOMIZATION_TYPE'"
				+ " WHERE P.ASSESSMENTID = ? ORDER BY P.SEQUENCE ASC";
		Object[] fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String sectionId = result.getString(1);
					String title = result.getString(2);
					String description = result.getString(3);
					int questionOrdering = result.getInt(4);
					int numQuestionsDrawn = result.getInt(5);
					int randomizationType = result.getInt(6);

					// adjust the questionOrdering - if we are doing draw from pool limit, we should also randomize
					if (numQuestionsDrawn != 0) questionOrdering = 2;

					// pack it into an assessment section
					AssessmentSectionImpl section = new AssessmentSectionImpl();
					section.initId(sectionId);
					section.setTitle(title);
					section.setDescription(description);
					section.setRandomQuestionOrder((questionOrdering == 2) ? Boolean.TRUE : Boolean.FALSE);
					section.setQuestionLimit(numQuestionsDrawn > 0 ? new Integer(numQuestionsDrawn) : null);
					if (randomizationType != 1) section.randomizeOnlyByUser = Boolean.TRUE;

					// put the section into the assessment
					section.initAssement(assessment);
					assessment.sections.add(section);
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentSections: " + e);
					return null;
				}
			}
		});

		// mark the assessment as sections inited
		assessment.sectionsStatus = AssessmentImpl.PropertyStatus.inited;

		// get the questions
		statement = "SELECT PI.ITEMID, PI.HASRATIONALE, PI.SCORE, PI.INSTRUCTION, PI.TYPEID, PI.SECTIONID, MCS.ENTRY, MME.ENTRY, MMR.ENTRY,"
				+ " PF1.TEXT, PF2.TEXT, PF3.TEXT" + " FROM SAM_PUBLISHEDITEM_T PI"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON PI.SECTIONID = PS.SECTIONID AND PS.ASSESSMENTID = ?"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMMETADATA_T MCS ON PI.ITEMID = MCS.ITEMID AND MCS.LABEL = 'CASE_SENSITIVE'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMMETADATA_T MME ON PI.ITEMID = MME.ITEMID AND MME.LABEL = 'MUTUALLY_EXCLUSIVE'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMMETADATA_T MMR ON PI.ITEMID = MMR.ITEMID AND MMR.LABEL = 'RANDOMIZE'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMFEEDBACK_T PF1 ON PI.ITEMID = PF1.ITEMID AND PF1.TYPEID = 'Correct Feedback'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMFEEDBACK_T PF2 ON PI.ITEMID = PF2.ITEMID AND PF2.TYPEID = 'General Feedback'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMFEEDBACK_T PF3 ON PI.ITEMID = PF3.ITEMID AND PF3.TYPEID = 'InCorrect Feedback'"
				+ " ORDER BY PI.SEQUENCE ASC";
		fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.id);

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String questionId = result.getString(1);
					boolean hasRationale = result.getBoolean(2);
					float score = result.getFloat(3);
					String instructions = StringUtil.trimToNull(result.getString(4));
					int type = result.getInt(5);
					String sectionId = result.getString(6);
					String caseSensitive = result.getString(7);
					String mutuallyExclusive = result.getString(8);
					String randomize = result.getString(9);
					String correctFeedback = result.getString(10);
					String generalFeedback = result.getString(11);
					String incorrectFeedback = result.getString(12);

					// pack it into an assessment question
					AssessmentQuestionImpl question = new AssessmentQuestionImpl();
					question.initId(questionId);
					question.setRequireRationale(Boolean.valueOf(hasRationale));
					question.setScore(new Float(score));
					question.setInstructions(instructions);
					question.setType(QuestionType.valueOf(type));
					question.setCaseSensitive(caseSensitive == null ? null : Boolean.parseBoolean(caseSensitive));
					question.setMutuallyExclusive(mutuallyExclusive == null ? null : Boolean.parseBoolean(mutuallyExclusive));
					question.setFeedbackCorrect(correctFeedback);
					question.setFeedbackGeneral(generalFeedback);
					question.setFeedbackIncorrect(incorrectFeedback);
					question.setRandomAnswerOrder(((randomize != null) && randomize.equals("true")) ? Boolean.TRUE : Boolean.FALSE);

					// add the question to the appropriate section (sectionId)
					AssessmentSectionImpl section = (AssessmentSectionImpl) assessment.getSection(sectionId);
					if (section == null)
					{
						M_log.warn("readAssessmentParts: missing section to store question: section id: " + sectionId + " questionId: " + questionId);
					}
					else
					{
						question.initSection(section);
						section.questions.add(question);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentSections: " + e);
					return null;
				}
			}
		});

		// read the question parts (from Sam's item text table)
		statement = "SELECT PIT.ITEMTEXTID, PIT.ITEMID, PIT.TEXT" + " FROM SAM_PUBLISHEDITEMTEXT_T PIT"
				+ " INNER JOIN SAM_PUBLISHEDITEM_T PI ON PIT.ITEMID = PI.ITEMID"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON PI.SECTIONID = PS.SECTIONID AND PS.ASSESSMENTID = ?"
				+ " ORDER BY PIT.ITEMID ASC, PIT.SEQUENCE ASC";
		fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.id);

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String partId = result.getString(1);
					String questionId = result.getString(2);
					String title = result.getString(3);

					// pack into a question part
					QuestionPartImpl part = new QuestionPartImpl();
					part.initId(partId);
					part.setTitle(title);

					// add the part to the appropriate question (questionId)
					AssessmentQuestionImpl question = (AssessmentQuestionImpl) assessment.getQuestion(questionId);
					if (question == null)
					{
						M_log.warn("readAssessmentSections: missing question to store text: question id: " + questionId + " textId: " + partId);
					}
					else
					{
						part.initQuestion(question);
						question.parts.add(part);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentSections: " + e);
					return null;
				}
			}
		});

		// get the answers
		statement = "SELECT PA.ANSWERID, PA.ITEMID, PA.TEXT, PA.ISCORRECT, PA.LABEL, PA.ITEMTEXTID," + " PF1.TEXT, PF2.TEXT, PF3.TEXT"
				+ " FROM SAM_PUBLISHEDANSWER_T PA" + " INNER JOIN SAM_PUBLISHEDITEM_T PI ON PA.ITEMID = PI.ITEMID"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON PI.SECTIONID = PS.SECTIONID AND PS.ASSESSMENTID = ?"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDANSWERFEEDBACK_T PF1 ON PA.ANSWERID = PF1.ANSWERID AND PF1.TYPEID = 'Correct Feedback'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDANSWERFEEDBACK_T PF2 ON PA.ANSWERID = PF2.ANSWERID AND PF2.TYPEID = 'General Feedback'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDANSWERFEEDBACK_T PF3 ON PA.ANSWERID = PF3.ANSWERID AND PF3.TYPEID = 'InCorrect Feedback'"
				+ " ORDER BY PA.SEQUENCE ASC";
		fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.id);

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String answerId = result.getString(1);
					String questionId = result.getString(2);
					String text = StringUtil.trimToNull(result.getString(3));
					boolean isCorrect = result.getBoolean(4);
					// samigo puts out a blank (not null) label for cases where there should be no label (t/f)
					String label = StringUtil.trimToNull(result.getString(5));
					String partId = result.getString(6);
					String correctFeedback = result.getString(7);
					String generalFeedback = result.getString(8);
					String incorrectFeedback = result.getString(9);

					// find the question
					AssessmentQuestionImpl question = (AssessmentQuestionImpl) assessment.getQuestion(questionId);
					if (question == null)
					{
						M_log.warn("readAssessmentSections: missing question to store answer: question id: " + questionId + " answerId: " + answerId);
					}
					else
					{
						// find the part
						QuestionPartImpl part = (QuestionPartImpl) question.getPart(partId);
						if (part == null)
						{
							M_log.warn("readAssessmentSections: missing question part to store answer: question id: " + questionId + " partId: "
									+ partId + " answerId: " + answerId);
						}
						else
						{
							// pack it into an assessment answer
							AssessmentAnswerImpl answer = new AssessmentAnswerImpl();
							answer.initId(answerId);
							answer.setIsCorrect(Boolean.valueOf(isCorrect));
							answer.setText(text);
							answer.setLabel(label);
							answer.setFeedbackCorrect(correctFeedback);
							answer.setFeedbackGeneral(generalFeedback);
							answer.setFeedbackIncorrect(incorrectFeedback);

							// add to the part's answers
							answer.initPart(part);
							part.answers.add(answer);
						}
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentSections: " + e);
					return null;
				}
			}
		});

		// read the attachments for all sections (join with the sections table to be able to select for the entire assessment)
		statement = "SELECT A.RESOURCEID, A.SECTIONID" + " FROM SAM_PUBLISHEDATTACHMENT_T A"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T S ON A.SECTIONID = S.SECTIONID" + " WHERE S.ASSESSMENTID = ?";
		fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String refStr = result.getString(1);
					String sectionId = result.getString(2);

					// assume a content ref
					refStr = "/content" + refStr;

					// make a reference
					Reference ref = m_entityManager.newReference(refStr);

					// find the section
					AssessmentSectionImpl section = (AssessmentSectionImpl) assessment.getSection(sectionId);
					if (section != null)
					{
						// add it to the section's attachments
						section.initAddAttachment(ref);
					}
					else
					{
						M_log.warn("readAssessmentSections: missing section to add attachment: sectionId: " + sectionId + " ref: " + refStr);
					}
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentAttachments: " + e);
					return null;
				}
			}
		});

		// read the attachments for all questions (join with the items table and sections table to be able to select for the entire
		// assessment)
		statement = "SELECT A.RESOURCEID, A.ITEMID" + " FROM SAM_PUBLISHEDATTACHMENT_T A"
				+ " INNER JOIN SAM_PUBLISHEDITEM_T Q ON A.ITEMID = Q.ITEMID" + " INNER JOIN SAM_PUBLISHEDSECTION_T S ON Q.SECTIONID = S.SECTIONID"
				+ " WHERE S.ASSESSMENTID = ?";
		fields = new Object[1];
		fields[0] = Integer.valueOf(assessment.getId());

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String refStr = result.getString(1);
					String questionId = result.getString(2);

					// assume a content ref
					refStr = "/content" + refStr;

					// make a reference
					Reference ref = m_entityManager.newReference(refStr);

					// find the question
					AssessmentQuestionImpl question = (AssessmentQuestionImpl) assessment.getQuestion(questionId);
					if (question != null)
					{
						// add it to the question's attachments
						question.initAddAttachment(ref);
					}
					else
					{
						M_log.warn("readAssessmentSections: missing question to add attachment: questionId: " + questionId + " ref: " + refStr);
					}
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readAssessmentAttachments: " + e);
					return null;
				}
			}
		});

		// verify what we read
		for (Iterator i = assessment.sections.iterator(); i.hasNext();)
		{
			AssessmentSectionImpl section = (AssessmentSectionImpl) i.next();

			for (AssessmentQuestionImpl question : section.questions)
			{
				question.verifyQuestion();
			}

			// also makes sure each section has at least one question
			if (section.questions.isEmpty())
			{
				String msg = "readAssessmentSections: CORRECTED: section with no questions: section: " + section.id + " test: " + assessment.getId();
				M_log.info(msg);

				// remove the section
				i.remove();
				continue;
			}
			if ((section.getQuestionLimit() != null) && (section.getQuestionLimit().intValue() < 1))
			{
				String msg = "readAssessmentSections: CORRECTED: section with <1 limit: " + section.getQuestionLimit().intValue() + " section: "
						+ section.id + " test: " + assessment.getId();
				M_log.info(msg);

				// remove the section
				i.remove();
				continue;
			}
		}

		// update the cache if cached
		AssessmentImpl cached = getCachedAssessment(assessment.getId());
		if (cached != null)
		{
			synchronized (cached)
			{
				cached.setSections(assessment);
			}
		}
	}
}
