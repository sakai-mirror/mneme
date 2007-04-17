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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.AssessmentAnswer;
import org.muse.mneme.api.AssessmentClosedException;
import org.muse.mneme.api.AssessmentCompletedException;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.AssessmentQuestion;
import org.muse.mneme.api.AssessmentSection;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentStatus;
import org.muse.mneme.api.AttachmentService;
import org.muse.mneme.api.FeedbackDelivery;
import org.muse.mneme.api.MultipleSubmissionSelectionPolicy;
import org.muse.mneme.api.QuestionPart;
import org.muse.mneme.api.QuestionPresentation;
import org.muse.mneme.api.QuestionType;
import org.muse.mneme.api.Submission;
import org.muse.mneme.api.SubmissionAnswer;
import org.muse.mneme.api.SubmissionCompletedException;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;

/**
 * <p>
 * AssessmentServiceImpl is ...
 * </p>
 */
public class AssessmentServiceImpl implements AssessmentService, Runnable
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentServiceImpl.class);

	/** A cache of assessments. */
	protected Cache m_assessmentCache = null;

	/** A cache of submissions. */
	protected Cache m_submissionCache = null;

	/** The number of ms we allow answers and completions of submissions after hard deadlines. */
	protected final long GRACE = 2 * 60 * 1000;

	/*************************************************************************************************************************************************
	 * Abstractions, etc.
	 ************************************************************************************************************************************************/

	/**
	 * Check the security for this user doing this function withing this context.
	 * 
	 * @param userId
	 *        the user id.
	 * @param function
	 *        the function.
	 * @param context
	 *        The context.
	 * @param ref
	 *        The entity reference.
	 * @return true if the user has permission, false if not.
	 */
	protected boolean checkSecurity(String userId, String function, String context, String ref)
	{
		// check for super user
		if (m_securityService.isSuperUser(userId)) return true;

		// check for the user / function / context-as-site-authz

		// form the azGroups for a context-as-implemented-by-site (Note the *lack* of direct dependency on Site, i.e. we stole the
		// code!)
		Collection azGroups = new Vector(2);
		azGroups.add("/site/" + context);
		azGroups.add("!site.helper");

		boolean rv = m_securityService.unlock(userId, function, ref, azGroups);
		return rv;
	}

	/**
	 * Check security and throw if not satisfied
	 * 
	 * @param userId
	 *        the user id.
	 * @param function
	 *        the function.
	 * @param context
	 *        The context.
	 * @param ref
	 *        The entity reference.
	 * @throws AssessmentPermissionException
	 *         if security is not satisfied.
	 */
	protected void secure(String userId, String function, String context, String ref) throws AssessmentPermissionException
	{
		if (!checkSecurity(userId, function, context, ref))
		{
			throw new AssessmentPermissionException(userId, function, context);
		}
	}

	/*************************************************************************************************************************************************
	 * Dependencies
	 ************************************************************************************************************************************************/

	/** Dependency: AttachmentService */
	protected AttachmentService m_attachmentService = null;

	/** Dependency: EntityManager */
	protected EntityManager m_entityManager = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

	/** Dependency: GradebookExternalAssessmentService */
	// for 2.4 only: protected GradebookExternalAssessmentService m_gradebookService = null;
	protected GradebookService m_gradebookService = null;

	/** Dependency: MemoryService */
	protected MemoryService m_memoryService = null;

	/** Dependency: SecurityService */
	protected SecurityService m_securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/** Dependency: ThreadLocalManager */
	protected ThreadLocalManager m_threadLocalManager = null;

	/** Dependency: TimeService */
	protected TimeService m_timeService = null;

	/**
	 * Dependency: AttachmentService.
	 * 
	 * @param service
	 *        The AttachmentService.
	 */
	public void setAttachmentService(AttachmentService service)
	{
		m_attachmentService = service;
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
	 * Dependency: GradebookService.
	 * 
	 * @param service
	 *        The GradebookService.
	 */
	public void setGradebookService(/* for 2.4 only: GradebookExternalAssessmentService */GradebookService service)
	{
		m_gradebookService = service;
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
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
	}

	/*************************************************************************************************************************************************
	 * Configuration
	 ************************************************************************************************************************************************/

	/** The # seconds between cache cleaning runs. */
	protected int m_cacheCleanerSeconds = 0;

	/** The # seconds to cache assessment reads. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

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

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/** How long to wait (ms) between checks for timed-out submission in the db. 0 disables. */
	protected long m_timeoutCheckMs = 1000L * 300L;

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

	/*************************************************************************************************************************************************
	 * Init and Destroy
	 ************************************************************************************************************************************************/

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		// stop the checking thread
		stop();

		M_log.info("destroy()");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				m_sqlService.ddl(this.getClass().getClassLoader(), "mneme");
			}

			// <= 0 indicates no caching desired
			if ((m_cacheSeconds > 0) && (m_cacheCleanerSeconds > 0))
			{
				// assessment and submissions caches, automatiaclly checking for expiration as configured mins, expire on events...
				m_assessmentCache = m_memoryService.newHardCache(m_cacheCleanerSeconds, getAssessmentReference(""));

				m_submissionCache = new SubmissionCacheImpl(m_memoryService, m_eventTrackingService, m_cacheCleanerSeconds,
						getSubmissionReference(""), ":");
				// m_memoryService.newHardCache(m_cacheCleanerSeconds, getSubmissionReference(""));
			}

			// start the checking thread
			if (m_timeoutCheckMs > 0)
			{
				start();
			}

			M_log.info("init(): caching minutes: " + m_cacheSeconds / 60 + " cache cleaner minutes: " + m_cacheCleanerSeconds / 60
					+ " timout check seconds: " + m_timeoutCheckMs / 1000);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/*************************************************************************************************************************************************
	 * AssessmentService implementation
	 ************************************************************************************************************************************************/

	/*************************************************************************************************************************************************
	 * Assessment Access
	 ************************************************************************************************************************************************/

	/**
	 * TODO: Note: assessments ids are (for now) assumed to be published - the Samigo 1 data model does not have a unique assessment id across
	 * published and non-published.
	 */

	/**
	 * Form an assessment reference for this assessment id.
	 * 
	 * @param assessmentId
	 *        the assessment id.
	 * @return the assessment reference for this assessment id.
	 */
	protected String getAssessmentReference(String assessmentId)
	{
		String ref = REFERENCE_ROOT + "/" + ASSESSMENT_TYPE + "/" + assessmentId;
		return ref;
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
		fields[0] = id;

		List results = m_sqlService.dbRead(statement, fields, null);
		return !results.isEmpty();
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
		fields[1] = assessment.getId();

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

					MultipleSubmissionSelectionPolicy mssPolicy = MultipleSubmissionSelectionPolicy.parse(result.getInt(5));

					AssessmentStatus status = AssessmentStatus.parse(result.getInt(6));

					FeedbackDelivery delivery = FeedbackDelivery.parse(result.getInt(7));
					boolean showStudentScore = result.getBoolean(8);
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
		fields[0] = assessment.getId();

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
		fields[0] = assessment.id;

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
		fields[0] = assessment.id;

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
		fields[0] = assessment.id;

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
		fields[0] = assessment.getId();

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
		fields[0] = assessment.getId();

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
		fields[0] = assessment.getId();

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

	/*************************************************************************************************************************************************
	 * Submission Access
	 ************************************************************************************************************************************************/

	/**
	 * Form a submission reference for this submission id.
	 * 
	 * @param submissionId
	 *        the submission id.
	 * @return the submission reference for this submission id.
	 */
	protected String getSubmissionReference(String submissionId)
	{
		String ref = REFERENCE_ROOT + "/" + SUBMISSION_TYPE + "/" + submissionId;
		return ref;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission idSubmission(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("idSubmission: " + id);

		if (id == null) return null;

		// cached?
		SubmissionImpl submission = getCachedSubmission(id);
		if (submission != null)
		{
			// return a copy
			synchronized (submission)
			{
				return new SubmissionImpl(submission);
			}
		}

		// TODO: perhaps don't check... (see idAssessment) -ggolden
		// check that it exists
		if (!checkSubmission(id)) return null;

		if (M_log.isDebugEnabled()) M_log.debug("idSubmission: creating: " + id);

		// setup a new assessment with only the id
		submission = new SubmissionImpl(this);
		submission.initId(id);

		// cache a copy
		cacheSubmission(new SubmissionImpl(submission));

		return submission;
	}

	/**
	 * Id each of the submission in the id list
	 * 
	 * @param ids
	 *        The collection of submission ids.
	 * @return A collection if id'ed submission, one for each id.
	 */
	protected List<Submission> idSubmissions(List<String> ids)
	{
		List<Submission> rv = new ArrayList<Submission>(ids.size());
		for (String id : ids)
		{
			rv.add(idSubmission(id));
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission getSubmission(final String id)
	{
		// TODO: should the main and answers reads be done in a single transaction? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("getSubmission: " + id);

		if (id == null) return null;

		// cached?
		SubmissionImpl cached = getCachedSubmission(id);

		// if not cached, cache a placeholder
		if (cached == null)
		{
			cached = new SubmissionImpl(this);
			cached.initId(id);
			cacheSubmission(cached);
		}

		synchronized (cached)
		{
			// if we need to, read the main info
			if (!cached.isMainInited())
			{
				boolean found = readSubmissionMain(cached);
				if (!found) return null;
			}

			// if we need to, read the answers
			if (!cached.isAnswersInited())
			{
				readSubmissionAnswers(cached);
			}

			// return a copy so we don't return the cache
			return new SubmissionImpl(cached);
		}
	}

	/**
	 * Read the main parts of the submission (not the answers)
	 * 
	 * @param submission
	 *        The submission impl with the id set to fill in.
	 * @return true if we read, false if we could not find the submission.
	 */
	protected boolean readSubmissionMain(final SubmissionImpl submission)
	{
		if (M_log.isDebugEnabled()) M_log.debug("readSubmissionMain: " + submission.getId());

		if (submission.getId() == null)
		{
			M_log.warn("readSubmissionMain: attempt to read with no id set");
			return false;
		}

		String statement = "SELECT AG.PUBLISHEDASSESSMENTID, AG.TOTALOVERRIDESCORE, AG.SUBMITTEDDATE, AG.AGENTID, AG.FORGRADE, AG.ATTEMPTDATE, AG.STATUS, AG.COMMENTS"
				+ " FROM SAM_ASSESSMENTGRADING_T AG" + " WHERE AG.ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();

		List results = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String aid = result.getString(1);
					float manualScore = result.getFloat(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					String userId = result.getString(4);
					boolean complete = result.getBoolean(5);

					ts = result.getTimestamp(6, m_sqlService.getCal());
					Time startDate = null;
					if (ts != null)
					{
						startDate = m_timeService.newTime(ts.getTime());
					}

					int status = result.getInt(7);
					String comments = result.getString(8);

					// pack it into a submission
					submission.initAssessmentId(aid);
					submission.initIsComplete(Boolean.valueOf(complete));
					submission.initStartDate(startDate);
					submission.initStatus(new Integer(status));
					submission.initSubmittedDate(submittedDate);
					submission.initUserId(userId);
					submission.initEvalScore(new Float(manualScore));
					submission.initEvalComment(StringUtil.trimToNull(comments));

					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("getSubmission: " + e);
					return null;
				}
			}
		});

		if (!results.isEmpty())
		{
			// update the cache if cached
			SubmissionImpl cached = getCachedSubmission(submission.getId());
			if (cached != null)
			{
				synchronized (cached)
				{
					cached.setMain(submission);
				}
			}

			return true;
		}

		// we didn't find it
		return false;
	}

	/**
	 * Read the answers of the submission (not the main)
	 * 
	 * @param submission
	 *        The submission impl with the id set to fill in.
	 */
	protected void readSubmissionAnswers(final SubmissionImpl submission)
	{
		// TODO: Transaction to assure a consistent read? -ggolden

		if (M_log.isDebugEnabled()) M_log.debug("readSubmissionAnswers: " + submission.getId());

		if (submission.getId() == null)
		{
			M_log.warn("readSubmissionAnswers: attempt to read with no id set");
			return;
		}

		// mark the submission as inited for the answers, so the methods we are about to call don't try to re-read the answers
		submission.answersStatus = SubmissionImpl.PropertyStatus.inited;

		// read the answers.
		// The PUBLISHEDITEMTEXTID points to a question part, and we want to read the entiries in question part sequence order,
		// so we join to the SAM_PUBLISHEDITEMTEXT_T (i.e. question parts) table and order by the sequence there
		String statement = "SELECT I.ITEMGRADINGID, I.SUBMITTEDDATE, I.PUBLISHEDANSWERID, I.RATIONALE, I.ANSWERTEXT, I.REVIEW, I.PUBLISHEDITEMID, I.AUTOSCORE, I.PUBLISHEDITEMTEXTID, I.COMMENTS"
				+ " FROM SAM_ITEMGRADING_T I"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMTEXT_T PIT ON I.PUBLISHEDITEMTEXTID = PIT.ITEMTEXTID"
				+ " WHERE I.ASSESSMENTGRADINGID = ?" + " ORDER BY PIT.SEQUENCE ASC";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();

		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String id = result.getString(1);

					java.sql.Timestamp ts = result.getTimestamp(2, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					String answerId = result.getString(3);
					String rationale = result.getString(4);
					String answerText = result.getString(5);
					boolean markedForReview = result.getBoolean(6);
					String questionId = result.getString(7);
					float autoScore = result.getFloat(8);
					String questionPartId = result.getString(9);
					String comments = result.getString(10);

					// do we have the answer to this question yet?
					SubmissionAnswerImpl answer = submission.findAnswer(questionId);

					// if not, make one and save it in the submission
					if (answer == null)
					{
						answer = new SubmissionAnswerImpl();
						answer.initQuestionId(questionId);
						answer.setRationale(rationale);
						answer.setMarkedForReview(Boolean.valueOf(markedForReview));
						answer.setSubmittedDate(submittedDate);
						answer.initEvalComments(StringUtil.trimToNull(comments));
						answer.id = id;

						answer.initSubmission(submission);
						submission.answers.add(answer);
					}

					// add an entry to the answer
					SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
					entry.initQuestionPartId(questionPartId);
					entry.initAssessmentAnswerId(answerId);
					entry.setAnswerText(answerText);
					entry.initId(id);
					entry.initAutoScore(new Float(autoScore));

					entry.initAnswer(answer);
					answer.entries.add(entry);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissionAnswers: " + e);
					return null;
				}
			}
		});

		// TODO: deduce the answer eval scores - the difference between the set auto score and the computed auto score
		for (SubmissionAnswerImpl answer : submission.answers)
		{
			// see what we read in from the db
			float dbScore = answer.countAutoScore();

			// do the auto-score, setting the proper auto score
			scoreAnswer(answer);

			// see what we have different, that's the eval score
			float newScore = answer.countAutoScore();
			if (newScore != dbScore)
			{
				answer.initEvalScore(new Float(dbScore - newScore));
			}
		}

		// read the uploaded attachments for the answers, and fill out the entries to hold their refs
		statement = "SELECT M.MEDIAID, M.FILENAME, I.PUBLISHEDITEMID" + " FROM SAM_MEDIA_T M"
				+ " INNER JOIN SAM_ITEMGRADING_T I ON M.ITEMGRADINGID = I.ITEMGRADINGID" + " WHERE I.ASSESSMENTGRADINGID = ?"
				+ " ORDER BY M.CREATEDDATE ASC";
		fields = new Object[1];
		fields[0] = submission.getId();
		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String mediaId = result.getString(1);
					String name = result.getString(2);
					String questionId = result.getString(3);

					// we should already have an answer
					SubmissionAnswerImpl answer = submission.findAnswer(questionId);
					if (answer != null)
					{
						// we should have at least one entry
						SubmissionAnswerEntryImpl entry = answer.entries.get(0);
						if (entry != null)
						{
							// use this if the answer text is not a reference
							SubmissionAnswerEntryImpl newEntry = null;
							if ((entry.getAnswerText() == null) || (!entry.getAnswerText().startsWith("/")))
							{
								newEntry = entry;
							}

							// otherwise make a new entry
							else
							{
								newEntry = new SubmissionAnswerEntryImpl(entry);
								newEntry.setAssessmentAnswer(null);
								newEntry.setAnswerText(null);
								newEntry.initId(null);
								newEntry.initAnswer(answer);
								newEntry.initAutoScore(new Float(0));
								answer.entries.add(newEntry);
							}

							// set the reference to the attachment into the answer text
							String refStr = m_attachmentService.getAttachmentReference(submission.getId(), mediaId, name);
							newEntry.setAnswerText(refStr);
						}

						else
						{
							M_log.warn("readSubmissionAnswers: missing entry for answer to question for attachment: questionId: " + questionId
									+ " mediaId: " + mediaId);
						}
					}
					else
					{
						M_log.warn("readSubmissionAnswers: missing answer to question for attachment: questionId: " + questionId + " mediaId: "
								+ mediaId);
					}

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissionAnswers: " + e);
					return null;
				}
			}
		});

		// verify the answers
		for (SubmissionAnswerImpl answer : submission.answers)
		{
			answer.verifyEntries();
		}

		// update the cache if cached
		SubmissionImpl cached = getCachedSubmission(submission.getId());
		if (cached != null)
		{
			synchronized (cached)
			{
				cached.setAnswers(submission);
			}
		}
	}

	/**
	 * Check the cache for the submission. Use the short-term cache if enabled, else use the thread-local cache.
	 * 
	 * @param id
	 *        The submission id.
	 * @return The actual submission object cached, or null if not.
	 */
	protected SubmissionImpl getCachedSubmission(String id)
	{
		// Note: untill Samigo is no longer messing with the db behind our back, so that we can't keep our cache valid,
		// don't use anything from the cache that might have been modified by Samigo; such as completed submissions.
		// The thread-local cache is ok to use whatever we got.

		String ref = getSubmissionReference(id);

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			// if it is in there
			if (m_submissionCache.containsKey(ref))
			{
				SubmissionImpl s = (SubmissionImpl) m_submissionCache.get(ref);
				if (s != null)
				{
					// only incomplete, please
					if ((s.isComplete == null) || (!s.isComplete.booleanValue()))
					{
						return s;
					}
				}
			}
		}

		// if not found, check the thread-local cache
		return (SubmissionImpl) m_threadLocalManager.get(ref);
	}

	/**
	 * Cache this submission. Use the short-term cache if enable, else use the thread-local cache.
	 * 
	 * @param submission
	 *        The submission to cache.
	 */
	protected void cacheSubmission(SubmissionImpl submission)
	{
		String ref = getSubmissionReference(submission.getId());

		// Note: we thread-local cache always, even if we are otherwise caching
		m_threadLocalManager.set(ref, submission);

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			m_submissionCache.put(ref, submission, m_cacheSeconds);
		}
	}

	/**
	 * Clear this submission from the cache.
	 * 
	 * @param id
	 *        The submission id.
	 */
	protected void unCacheSubmission(String id)
	{
		String ref = getSubmissionReference(id);

		// Note: the cache will clear when the event is processed...
		// if (m_submissionCache != null) m_submissionCache.remove(ref);

		// clear the thread-local cache
		m_threadLocalManager.set(ref, null);
	}

	/**
	 * Check if a submission is defined
	 * 
	 * @param id
	 *        The submission id to check.
	 */
	protected boolean checkSubmission(String id)
	{
		if (M_log.isDebugEnabled()) M_log.debug("checkSubmission: " + id);

		String statement = "SELECT AG.ASSESSMENTGRADINGID FROM SAM_ASSESSMENTGRADING_T AG WHERE AG.ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;

		List results = m_sqlService.dbRead(statement, fields, null);
		return !results.isEmpty();
	}

	/*************************************************************************************************************************************************
	 * Delivery Support
	 ************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(Assessment assessment, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// if we have done this already this thread, use it
		String key = "coundRemainingSubmissions_" + assessment.getId() + "_" + userId;
		Integer count = (Integer) m_threadLocalManager.get(key);
		if (count != null) return count;

		final Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("countRemainingSubmissions: assessment: " + assessment.getId() + " userId: " + userId + " asOf: " + asOf);

		// we need the assessment's dates, and late handling policy, and the # submissions allowed
		// we need the user's count of completed submissions to this assessment
		// TODO: we need to know if the user has submit permissions at all

		String statement = "SELECT PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING, COUNT(AG.PUBLISHEDASSESSMENTID)"
				+ " FROM SAM_PUBLISHEDACCESSCONTROL_T PAC"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true)
				+ " WHERE PAC.ASSESSMENTID = ?"
				+ " GROUP BY AG.PUBLISHEDASSESSMENTID, PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING";

		Object[] fields = new Object[3];
		fields[0] = assessment.getId();
		fields[1] = userId;
		fields[2] = assessment.getId();

		List rv = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					boolean unlimitedSubmissions = result.getBoolean(1);

					int submissionsAllowed = result.getInt(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time startDate = null;
					if (ts != null)
					{
						startDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(4, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(5, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					int lateHandling = result.getInt(6);

					int submissionsMade = result.getInt(7);

					// if before start date, or after retract date, we are done
					if ((startDate != null) && asOf.before(startDate)) return new Integer(0);
					if ((retractDate != null) && asOf.after(retractDate)) return new Integer(0);

					// if after due date and we are not taking late submissions, we are done
					if ((dueDate != null) && (lateHandling != 1) && asOf.after(dueDate)) return new Integer(0);

					// if unlimited, return a -1
					if (unlimitedSubmissions) return new Integer(-1);

					// return the difference between the number taken already and the max
					int left = submissionsAllowed - submissionsMade;

					if (left <= 0) return new Integer(0);

					return new Integer(left);
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentDueDate: " + e);
					return null;
				}
			}
		});

		count = (rv.size() == 0) ? null : (Integer) rv.get(0);

		if (count != null)
		{
			m_threadLocalManager.set(key, count);
		}

		return count;
	}

	protected enum GetAvailableAssessmentsSort
	{
		dueDate_a, dueDate_d, title_a, title_d
	}

	protected enum GetOfficialSubmissionsSort
	{
		feedbackDate_a, feedbackDate_d, score_a, score_d, submittedDate_a, submittedDate_d, time_a, time_d, title_a, title_d
	}

	/**
	 * Find the assessments that are available for taking in this context by this user. Consider:
	 * <ul>
	 * <li>published assessments</li>
	 * <li>assessments in this context</li>
	 * <li>assessments this user has permission to take</li>
	 * <li>assessments that are released as of the time specified and not yet retracted</li>
	 * <li>assessments that, based on the due date and late submission policy, still can be submitted to</li>
	 * <li>assessments that, based on their re-submit policy and count, and this user's count of submissions, can be submitted to again by this user</li>
	 * <li>(assessments that accept late submissions and are past due date that the use has submitted to already are not included)</li>
	 * </ul>
	 * 
	 * @param context
	 *        The context to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @param sort
	 *        The sort for the return List (title_a if null).
	 * @return A List<Assessment> of the assessments that qualify, sorted, or an empty collection if none do.
	 */
	protected List<Assessment> getAvailableAssessments(final String context, String userId, GetAvailableAssessmentsSort sort)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled()) M_log.debug("getAvailableAssessmentsIds: context: " + context + " userId: " + userId + " asOf: " + asOf);

		// Notes: "in context"
		// SAM_ASSESSMENTBASE_T has the defined assessments (ID)
		// SAM_PUBLISHEDASSESSMENT_T has the main published assessment info, referring back to the SAM_ASSESSMENTBASE_T table's
		// entry with (ASSESSMENTID)
		// SAM_AUTHZDATA_T maps permissions (FUNCTIONID = TAKE_PUBLISHED_ASSESSMENT) for sites (AGENTID) to assessments by published
		// assessment id (QUALIFIERID)
		// SAM_AUTHZDATA_T QUALIFIERID can be either base assessment or published assessment; the FUNCTIONID for published are a
		// separate set than from base (EDIT_ASSESSMENT)

		// Notes: released and not yet retracted
		// SAM_PUBLISHEDACCESSCONTROL_T joins with the SAM_PUBLISHEDASSESSMENT_T (ASSESSMENTID) to define the active period
		// (STARTDATE) and (RETRACTDATE)
		// either may be null
		// we want now to be >= startdate and < retractdate (edges?)

		// Note: due date and last policy
		// SAM_PUBLISHEDACCESSCONTROL_T joins with the SAM_PUBLISHEDASSESSMENT_T (ASSESSMENTID) to define the due date (DUEDATE) -
		// may be null
		// and (LATEHANDLING) is 1 to allow late submissions, 2 to not allow late submissions

		// Note: join with submissions
		// SAM_ASSESSMENTGRADING_T joins in on PUBLISHEDASSESSMENTID to the SAM_PUBLISHEDASSESSMENT_T table for each submission
		// A left outer join gives us a record for each assessment, even with no submissions, and multiple records, one for each
		// submission.
		// The GROUP BY lets us get a count of submissions and collapses the records down to one per assessment
		// We need the inner select so we can compute the counts, then filter out those that have reached their submit limit
		// Counting the AG.PUBLISHEDASSESSMENTID column gives us an accurate count of how many submissions - if there are none, this
		// will end up null and give a 0 count.

		// Note: number of submissions allowed
		// SAM_PUBLISHEDACCESSCONTROL_T SUBMISSIONSALLOWED is null for unlimited, or has a count

		// Note: extra info
		// anticipating that we need the title and duedate (etc) for each assessment, we get it here and cache it so we can return
		// it later in the thread

		// figure sort sql
		String sortSql = null;
		if (sort == null)
		{
			sortSql = "X.TITLE ASC";
		}
		else
		{
			switch (sort)
			{
				case title_a:
				{
					sortSql = "X.TITLE ASC";
					break;
				}

				case title_d:
				{
					sortSql = "X.TITLE DESC";
					break;
				}

				case dueDate_a:
				{
					sortSql = "X.DUEDATE DESC";
					break;
				}

				case dueDate_d:
				{
					sortSql = "X.DUEDATE DESC";
					break;
				}
			}
		}

		String statement = "SELECT X.ID, X.TITLE, X.DUEDATE FROM ("
				+ " SELECT P.ID ID, COUNT(AG.PUBLISHEDASSESSMENTID) SUBMITTED, PAC.SUBMISSIONSALLOWED ALLOWED, P.TITLE TITLE, PAC.DUEDATE DUEDATE"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON"
				+ "      P.ID = PAC.ASSESSMENTID AND (PAC.STARTDATE IS NULL OR ? >= PAC.STARTDATE) AND (PAC.RETRACTDATE IS NULL OR ? < PAC.RETRACTDATE) AND (PAC.DUEDATE IS NULL OR ? < PAC.DUEDATE OR PAC.LATEHANDLING = 1)"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON P.ID = AG.PUBLISHEDASSESSMENTID AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " GROUP BY P.ID, PAC.SUBMISSIONSALLOWED, P.TITLE, PAC.DUEDATE" + " ) X"
				+ " WHERE (X.ALLOWED IS NULL OR X.SUBMITTED < X.ALLOWED)" + " ORDER BY " + sortSql;

		Object[] fields = new Object[6];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;
		fields[2] = asOf;
		fields[3] = asOf;
		fields[4] = asOf;
		fields[5] = userId;

		final AssessmentServiceImpl service = this;
		final List<String> ids = new ArrayList<String>();
		List rv = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String assessmentId = result.getString(1);
					String title = result.getString(2);

					java.sql.Timestamp ts = result.getTimestamp(3, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					// create or update these properties in the assessment cache
					AssessmentImpl cached = getCachedAssessment(assessmentId);
					if (cached == null)
					{
						// cache an empty one
						cached = new AssessmentImpl(service);
						cached.initId(assessmentId);
						cached.initContext(context);
						cacheAssessment(cached);
					}
					synchronized (cached)
					{
						cached.initTitle(title);
						cached.initDueDate(dueDate);
					}

					// record the id
					ids.add(assessmentId);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getAvailableAssessmentsIds: " + e);
					return null;
				}
			}
		});

		// id each of the returned assessment ids
		return idAssessments(ids);
	}

	/**
	 * Find the submissions to assignments in this context made by this user. Consider:
	 * <ul>
	 * <li>published assessments</li>
	 * <li>assessments in this context</li>
	 * <li>assessments this user can submit to and have submitted to</li>
	 * <li>the one (of many for this user) submission that will be the official (graded) (depending on the assessment settings, and submission time
	 * and score)</li>
	 * </ul>
	 * 
	 * @param context
	 *        The context to use.
	 * @param userId
	 *        The user id - if null, use the current user.
	 * @param sort
	 *        The sort for the return list (title_a if null).
	 * @return A List<Submission> of the submissions that are the offical submissions for assessments in the context by this user, sorted.
	 */
	protected List<Submission> getOfficialSubmissions(final String context, String userId, GetOfficialSubmissionsSort sort)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("getOfficialSubmissionsIds: context: " + context + " userId: " + userId);

		// Note: submissions
		// SAM_ASSESSMENTGRADING_T lists submissions to assessments
		// the user id is in the AGENTID column, the assessment is in the PUBLISHEDASSESSMENTID column

		// Notes: "in context"
		// SAM_AUTHZDATA_T maps permissions (FUNCTIONID = TAKE_PUBLISHED_ASSESSMENT) for sites (AGENTID) to assessments by published
		// assessment id (QUALIFIERID)

		// Notes: official submission
		// Of many submissions, either the latest or the highest graded is used
		// SAM_PUBLISHEDEVALUATION_T, joined in by the published assessment id, has the SCORINGTYPE column, 1=highest, 2=latest
		// (as known by MultipleSubmissionSelectionPolicy)

		// Note: finding the max totalScore
		// joining the grading table (left outer) to itself, mathing on the published assessment, where the left totalScore < right
		// totalScore, then selecting the
		// records where the right id is null will pick those grading records that are the maximum totalScore for each assessment...
		// (http://www.artfulsoftware.com/queries.php#7)
		// When joining, we have to make sure we specify the full criteria against the grading table to avoid stray records slipping
		// into the join
		// (we might need to add the context criteria too, which is another join?)
		// But, since each assessment might have a seperate criteria, and the selection criteria is rather complex, here we get all
		// the submissions and do our own filtering

		// Note: complete v.s. in progress submissions
		// the FORGRADE boolean is set when the submission is complete, false while it is in progress - these don't count.

		// figure sort
		String sortSql = null;
		if (sort == null)
		{
			sortSql = "P.TITLE ASC";
		}
		else
		{
			switch (sort)
			{
				case title_a:
				{
					sortSql = "P.TITLE ASC";
					break;
				}

				case title_d:
				{
					sortSql = "P.TITLE DESC";
					break;
				}
				case feedbackDate_a:
				{
					sortSql = "PAC.FEEDBACKDATE ASC";
					break;
				}

				case feedbackDate_d:
				{
					sortSql = "PAC.FEEDBACKDATE DESC";
					break;
				}

				case score_a:
				{
					sortSql = "AG.FINALSCORE ASC";
					break;
				}

				case score_d:
				{
					sortSql = "AG.FINALSCORE DESC";
					break;
				}
				case time_a:
				{
					// TODO:
					sortSql = "AG.SUBMITTEDDATE-AG.ATTEMPTDATE ASC";
					break;
				}

				case time_d:
				{
					// TODO:
					sortSql = "AG.SUBMITTEDDATE-AG.ATTEMPTDATE DESC";
					break;
				}
				case submittedDate_a:
				{
					sortSql = "AG.SUBMITTEDDATE ASC";
					break;
				}

				case submittedDate_d:
				{
					sortSql = "AG.SUBMITTEDDATE DESC";
					break;
				}
			}
		}

		String statement = "SELECT AG.ASSESSMENTGRADINGID, AG.PUBLISHEDASSESSMENTID, P.TITLE, AG.FINALSCORE, AG.ATTEMPTDATE,"
				+ " PAC.FEEDBACKDATE, AG.SUBMITTEDDATE, PE.SCORINGTYPE,"
				+ " PF.FEEDBACKDELIVERY, PF.SHOWSTUDENTSCORE, PF.SHOWSTATISTICS, AG.FORGRADE," + " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED"
				+ " FROM SAM_ASSESSMENTGRADING_T AG"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON AG.PUBLISHEDASSESSMENTID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDASSESSMENT_T P ON AG.PUBLISHEDASSESSMENTID = P.ID"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON AG.PUBLISHEDASSESSMENTID = PAC.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDFEEDBACK_T PF ON AG.PUBLISHEDASSESSMENTID = PF.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON AG.PUBLISHEDASSESSMENTID = PE.ASSESSMENTID"
				+ " WHERE AG.AGENTID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(true) + " ORDER BY " + sortSql;

		Object[] fields = new Object[3];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;
		fields[2] = userId;

		final AssessmentServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String submissionId = result.getString(1);
					String publishedAssessmentId = result.getString(2);
					String title = result.getString(3);
					float score = result.getFloat(4);

					java.sql.Timestamp ts = result.getTimestamp(5, m_sqlService.getCal());
					Time attemptDate = null;
					if (ts != null)
					{
						attemptDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(6, m_sqlService.getCal());
					Time feedbackDate = null;
					if (ts != null)
					{
						feedbackDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(7, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					int mssPolicy = result.getInt(8);
					FeedbackDelivery feedbackDelivery = FeedbackDelivery.parse(result.getInt(9));
					boolean showScore = result.getBoolean(10);
					boolean showStatistics = result.getBoolean(11);
					boolean complete = result.getBoolean(12);
					boolean unlimitedSubmissions = result.getBoolean(13);
					int submissionsAllowed = result.getInt(14);

					// create or update these properties in the submission cache
					SubmissionImpl cachedSubmission = getCachedSubmission(submissionId);
					if (cachedSubmission == null)
					{
						// cache an empty, but complete, one
						cachedSubmission = new SubmissionImpl(service);
						cachedSubmission.initId(submissionId);
						cacheSubmission(cachedSubmission);
					}
					synchronized (cachedSubmission)
					{
						cachedSubmission.initAssessmentId(publishedAssessmentId);
						cachedSubmission.initTotalScore(score);
						cachedSubmission.initStartDate(attemptDate);
						cachedSubmission.initSubmittedDate(submittedDate);
						cachedSubmission.initIsComplete(Boolean.valueOf(complete));
					}

					// create or update these properties in the assessment cache
					AssessmentImpl cachedAssessment = getCachedAssessment(publishedAssessmentId);
					if (cachedAssessment == null)
					{
						// cache an empty one
						cachedAssessment = new AssessmentImpl(service);
						cachedAssessment.initId(publishedAssessmentId);
						cachedAssessment.initContext(context);
						cacheAssessment(cachedAssessment);
					}
					synchronized (cachedAssessment)
					{
						cachedAssessment.initTitle(title);
						cachedAssessment.initFeedbackDate(feedbackDate);
						cachedAssessment.initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.parse(mssPolicy));
						cachedAssessment.initFeedbackDelivery(feedbackDelivery);
						// cachedAssessment.initFeedbackShowScore(Boolean.valueOf(showScore));
						cachedAssessment.initFeedbackShowStatistics(Boolean.valueOf(showStatistics));
						cachedAssessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));
					}

					// return the id
					return submissionId;
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentDueDate: " + e);
					return null;
				}
			}
		});

		// pick the one official from this many-list for each assessment
		List<String> ids = new ArrayList<String>();

		while (all.size() > 0)
		{
			// take the first one out
			String sid = (String) all.remove(0);
			String aid = idSubmission(sid).getAssessment().getId();
			MultipleSubmissionSelectionPolicy policy = idAssessment(aid).getMultipleSubmissionSelectionPolicy();
			Object value = (policy == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED) ? (Object) (((SubmissionImpl) idSubmission(sid))
					.getTotalScore()) : (Object) idSubmission(sid).getSubmittedDate();

			// remove all others with this one's assessment id - keeping the one that will be best
			for (Iterator i = all.iterator(); i.hasNext();)
			{
				String candidateId = (String) i.next();
				if (idSubmission(candidateId).getAssessment().getId().equals(aid))
				{
					// take this one out
					i.remove();

					// see if this wins over the best so far
					if (policy == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED)
					{
						// for totalScore, if the winner so far is smaller or equal to the new, use the new (the later one for a tie
						// is the later submission based on our sort)
						if (((Float) value).floatValue() <= ((SubmissionImpl) idSubmission(candidateId)).getTotalScore().floatValue())
						{
							// switch to this one
							value = ((SubmissionImpl) idSubmission(candidateId)).getTotalScore();
							sid = candidateId;
						}
					}
					else
					{
						// for submission, use the latest one
						if (((Time) value).before(idSubmission(candidateId).getSubmittedDate()))
						{
							// switch to this one
							value = idSubmission(candidateId).getSubmittedDate();
							sid = candidateId;
						}
					}
				}
			}

			// keep the winner
			ids.add(sid);
		}

		// id the selected submissions
		return idSubmissions(ids);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Submission> getUserContextSubmissions(final String context, String userId, GetUserContextSubmissionsSort sort)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();
		final String fUserId = userId;

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled()) M_log.debug("getUserContextSubmissions: context: " + context + " userId: " + userId);

		// figure sort
		String sortSql = null;
		if (sort == null)
		{
			sortSql = "P.TITLE ASC";
		}
		else
		{
			switch (sort)
			{
				case title_a:
				{
					sortSql = "P.TITLE ASC";
					break;
				}

				case title_d:
				{
					sortSql = "P.TITLE DESC";
					break;
				}

				case dueDate_a:
				{
					sortSql = "PAC.DUEDATE ASC, P.TITLE ASC";
					break;
				}

				case dueDate_d:
				{
					sortSql = "PAC.DUEDATE DESC, P.TITLE DESC";
					break;
				}

				// status ascending, i.e. boring to important, is backed by date boring (later) to important (sooner), i.e. date desc
				// status is always read ascending, reversed when we sort it later
				case status_a:
				case status_d:
				{
					sortSql = "PAC.DUEDATE DESC, P.TITLE DESC";
					break;
				}
			}
		}

		String statement = "SELECT AG.ASSESSMENTGRADINGID, P.ID, P.TITLE, AG.FINALSCORE, AG.ATTEMPTDATE,"
				+ " PAC.FEEDBACKDATE, AG.SUBMITTEDDATE, PE.SCORINGTYPE, PF.FEEDBACKDELIVERY, AG.FORGRADE,"
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.TIMELIMIT, PAC.DUEDATE, PAC.LATEHANDLING, PAC.RETRACTDATE, PE.TOGRADEBOOK,"
				+ " SUM(PI.SCORE)"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON P.ID = PAC.ASSESSMENTID AND (PAC.RETRACTDATE IS NULL OR ? < PAC.RETRACTDATE)"
				+ " INNER JOIN SAM_PUBLISHEDFEEDBACK_T PF ON P.ID = PF.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON P.ID = PE.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON P.ID = PS.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDITEM_T PI ON PS.SECTIONID = PI.SECTIONID"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON P.ID = AG.PUBLISHEDASSESSMENTID AND AG.AGENTID = ?"
				+ " GROUP BY AG.ASSESSMENTGRADINGID, P.ID, P.TITLE, AG.FINALSCORE, AG.ATTEMPTDATE,"
				+ " PAC.FEEDBACKDATE, AG.SUBMITTEDDATE, PE.SCORINGTYPE, PF.FEEDBACKDELIVERY, PF.SHOWSTUDENTSCORE, PF.SHOWSTATISTICS, AG.FORGRADE,"
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.STARTDATE, PAC.TIMELIMIT, PAC.DUEDATE, PAC.LATEHANDLING, PAC.RETRACTDATE, PE.TOGRADEBOOK"
				+ " ORDER BY " + sortSql;

		Object[] fields = new Object[4];
		fields[0] = "TAKE_PUBLISHED_ASSESSMENT";
		fields[1] = context;
		fields[2] = asOf;
		fields[3] = userId;

		final AssessmentServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String submissionId = result.getString(1);
					String publishedAssessmentId = result.getString(2);
					String title = result.getString(3);
					float score = result.getFloat(4);

					java.sql.Timestamp ts = result.getTimestamp(5, m_sqlService.getCal());
					Time attemptDate = null;
					if (ts != null)
					{
						attemptDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(6, m_sqlService.getCal());
					Time feedbackDate = null;
					if (ts != null)
					{
						feedbackDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(7, m_sqlService.getCal());
					Time submittedDate = null;
					if (ts != null)
					{
						submittedDate = m_timeService.newTime(ts.getTime());
					}

					int mssPolicy = result.getInt(8);
					FeedbackDelivery feedbackDelivery = FeedbackDelivery.parse(result.getInt(9));
					boolean complete = result.getBoolean(10);
					boolean unlimitedSubmissions = result.getBoolean(11);
					int submissionsAllowed = result.getInt(12);

					ts = result.getTimestamp(13, m_sqlService.getCal());
					Time releaseDate = null;
					if (ts != null)
					{
						releaseDate = m_timeService.newTime(ts.getTime());
					}

					long timeLimit = result.getLong(14);

					ts = result.getTimestamp(15, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					int allowLateSubmit = result.getInt(16);

					ts = result.getTimestamp(17, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					int toGradebook = result.getInt(18);
					float points = result.getFloat(19);

					// for the non-submissions, create an non-null id
					if (submissionId == null)
					{
						submissionId = publishedAssessmentId + fUserId;
					}

					// create or update these properties in the submission cache
					SubmissionImpl cachedSubmission = getCachedSubmission(submissionId);
					if (cachedSubmission == null)
					{
						// cache an empty, but complete, one
						cachedSubmission = new SubmissionImpl(service);
						cachedSubmission.initId(submissionId);
						cacheSubmission(cachedSubmission);
					}
					synchronized (cachedSubmission)
					{
						cachedSubmission.initAssessmentId(publishedAssessmentId);
						cachedSubmission.initTotalScore(score);
						cachedSubmission.initStartDate(attemptDate);
						cachedSubmission.initSubmittedDate(submittedDate);
						cachedSubmission.initIsComplete(Boolean.valueOf(complete));
						cachedSubmission.initUserId(fUserId);
					}

					// create or update these properties in the assessment cache
					AssessmentImpl cachedAssessment = getCachedAssessment(publishedAssessmentId);
					if (cachedAssessment == null)
					{
						// cache an empty one
						cachedAssessment = new AssessmentImpl(service);
						cachedAssessment.initId(publishedAssessmentId);
						cacheAssessment(cachedAssessment);
					}
					synchronized (cachedAssessment)
					{
						cachedAssessment.initContext(context);
						cachedAssessment.initTitle(title);
						cachedAssessment.initFeedbackDate(feedbackDate);
						cachedAssessment.initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.parse(mssPolicy));
						cachedAssessment.initFeedbackDelivery(feedbackDelivery);
						cachedAssessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));
						cachedAssessment.initReleaseDate(releaseDate);
						cachedAssessment.initTimeLimit(timeLimit == 0 ? null : new Long(timeLimit * 1000));
						cachedAssessment.initDueDate(dueDate);
						cachedAssessment.initAllowLateSubmit((allowLateSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);
						cachedAssessment.initRetractDate(retractDate);
						cachedAssessment.initGradebookIntegration(Boolean.valueOf(toGradebook == 1));
						cachedAssessment.initTotalPoints(new Float(points));
					}

					// return a copy of the cached submission
					return new SubmissionImpl(cachedSubmission);
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentDueDate: " + e);
					return null;
				}
			}
		});

		// pick one for each assessment - the one in progress, or the official complete one
		List<Submission> official = new ArrayList<Submission>();

		while (all.size() > 0)
		{
			// take the first one out
			Submission submission = (Submission) all.remove(0);

			// check if this is over time limit / deadline
			if (submission.getIsOver(asOf, 0))
			{
				// complete this one, using the exact 'over' date for the final date
				Time over = submission.getWhenOver();
				completeTheSubmission(over, submission);

				// update what we read (completeTheSubmission uncaches, so we own this submission object now)
				((SubmissionImpl) submission).initStatus(new Integer(1));
				((SubmissionImpl) submission).initIsComplete(Boolean.TRUE);
				((SubmissionImpl) submission).initSubmittedDate(over);

				// recache a copy
				cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));
			}

			// set it's sibling count to 1 (itself), or 0 if it's not really there
			int count = 0;
			if (submission.getStartDate() != null)
			{
				count = 1;
			}
			((SubmissionImpl) submission).initSiblingCount(new Integer(count));

			String aid = submission.getAssessment().getId();
			MultipleSubmissionSelectionPolicy policy = idAssessment(aid).getMultipleSubmissionSelectionPolicy();
			Object value = (policy == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED) ? (Object) (((SubmissionImpl) submission).getTotalScore())
					: (Object) submission.getSubmittedDate();

			// remove all others with this one's assessment id - keeping the one that will be best
			for (Iterator i = all.iterator(); i.hasNext();)
			{
				SubmissionImpl candidateSub = (SubmissionImpl) i.next();
				if (candidateSub.getAssessment().getId().equals(aid))
				{
					// take this one out
					i.remove();

					// check if this is over time limit / deadline
					if (candidateSub.getIsOver(asOf, 0))
					{
						// complete this one, using the exact 'over' date for the final date
						Time over = candidateSub.getWhenOver();
						completeTheSubmission(over, candidateSub);

						// update what we read
						candidateSub.initStatus(new Integer(1));
						candidateSub.initIsComplete(Boolean.TRUE);
						candidateSub.initSubmittedDate(over);

						// recache a copy
						cacheSubmission(new SubmissionImpl(candidateSub));
					}

					// count as a sibling if not unstarted
					count = 0;
					if (candidateSub.getStartDate() != null)
					{
						count = 1;
					}

					// if the winner so far is in progress, it remains the winner
					if ((submission.getIsComplete() == null) || (!submission.getIsComplete()))
					{
						// keep the submission we already have, add a sibling for this one we are skipping
						((SubmissionImpl) submission).initSiblingCount(new Integer(submission.getSiblingCount().intValue() + count));
					}

					// if this one is in progress, it wins
					else if ((candidateSub.getIsComplete() == null) || (!candidateSub.getIsComplete()))
					{
						// transfer sibling count
						candidateSub.initSiblingCount(new Integer(submission.getSiblingCount().intValue() + count));
						submission = candidateSub;
					}

					// see if this wins over the best so far
					else if (policy == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED)
					{
						// for totalScore, if the winner so far is smaller or equal to the new, use the new (the later one for a tie
						// is the later submission based on our sort)
						if (((Float) value).floatValue() <= ((SubmissionImpl) candidateSub).getTotalScore().floatValue())
						{
							// switch to this one
							value = ((SubmissionImpl) candidateSub).getTotalScore();

							candidateSub.initSiblingCount(new Integer(submission.getSiblingCount().intValue() + count));
							submission = candidateSub;
						}
						else
						{
							// keep the submission we already have, add a sibling for this one we are skipping
							((SubmissionImpl) submission).initSiblingCount(new Integer(submission.getSiblingCount().intValue() + count));
						}
					}
					else
					{
						// use the latest one
						if (((Time) value).before(candidateSub.getSubmittedDate()))
						{
							// switch to this one
							value = candidateSub.getSubmittedDate();

							candidateSub.initSiblingCount(new Integer(submission.getSiblingCount().intValue() + 1));
							submission = candidateSub;
						}
						else
						{
							// keep the submission we already have, add a sibling for this one we are skipping
							((SubmissionImpl) submission).initSiblingCount(new Integer(submission.getSiblingCount().intValue() + count));
						}
					}
				}
			}

			// keep the winner
			official.add(submission);
		}

		// id the selected submissions

		// if sorting by status, do that sort
		if (sort == GetUserContextSubmissionsSort.status_a || sort == GetUserContextSubmissionsSort.status_d)
		{
			official = sortByStatus(sort, official);
		}

		return official;
	}

	/**
	 * Sort a list of submissions by their status.
	 * 
	 * @param sort
	 *        The sort (status_a or status_d)
	 * @param submissions
	 *        The submission list to sort.
	 * @return The sorted list of submissions.
	 */
	protected List<Submission> sortByStatus(GetUserContextSubmissionsSort sort, List<Submission> submissions)
	{
		// the easy cases
		if ((submissions == null) || (submissions.size() < 2)) return submissions;

		List<Submission> rv = new ArrayList<Submission>();

		// sort order (a) other, future, over, complete, completeReady, ready, inProgress, inProgressAlert
		List<Submission> other = new ArrayList<Submission>();
		List<Submission> future = new ArrayList<Submission>();
		List<Submission> over = new ArrayList<Submission>();
		List<Submission> complete = new ArrayList<Submission>();
		List<Submission> completeReady = new ArrayList<Submission>();
		List<Submission> ready = new ArrayList<Submission>();
		List<Submission> inProgress = new ArrayList<Submission>();
		List<Submission> inProgressAlert = new ArrayList<Submission>();

		for (Submission s : submissions)
		{
			switch (s.getAssessmentSubmissionStatus())
			{
				case other:
				{
					other.add(s);
					break;
				}

				case future:
				{
					future.add(s);
					break;
				}

				case over:
				{
					over.add(s);
					break;
				}

				case complete:
				{
					complete.add(s);
					break;
				}

				case completeReady:
				{
					completeReady.add(s);
					break;
				}

				case ready:
				{
					ready.add(s);
					break;
				}

				case inProgress:
				{
					inProgress.add(s);
					break;
				}

				case inProgressAlert:
				{
					inProgressAlert.add(s);
					break;
				}
			}
		}

		// order ascending
		rv.addAll(other);
		rv.addAll(future);
		rv.addAll(over);
		rv.addAll(complete);
		rv.addAll(completeReady);
		rv.addAll(ready);
		rv.addAll(inProgress);
		rv.addAll(inProgressAlert);

		// reverse if descending
		if (sort == GetUserContextSubmissionsSort.status_d)
		{
			Collections.reverse(rv);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getAssessmentScores(Assessment assessment)
	{
		// TODO: Warning - this query is showing serious performance problems (Oracle)

		final List<Float> rv = new ArrayList<Float>();

		String statement = "SELECT AG.FINALSCORE" + " FROM SAM_ASSESSMENTGRADING_T AG" + " WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " ORDER BY AG.FINALSCORE";

		Object[] fields = new Object[1];
		fields[0] = assessment.getId();

		final AssessmentServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					float score = result.getFloat(1);
					rv.add(new Float(score));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getAssessmentScores: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Float> getQuestionScores(String questionId)
	{
		// TODO: Warning - this query is showing serious performance problems (Oracle)
		final List<Float> rv = new ArrayList<Float>();

		String statement = "SELECT SUM(IG.AUTOSCORE+IG.OVERRIDESCORE) AS SCORE" + " FROM SAM_ITEMGRADING_T IG"
				+ " INNER JOIN SAM_ASSESSMENTGRADING_T AG ON IG.ASSESSMENTGRADINGID = AG.ASSESSMENTGRADINGID"
				+ " WHERE IG.PUBLISHEDITEMID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(true) + " GROUP BY IG.ASSESSMENTGRADINGID"
				+ " ORDER BY SCORE";

		Object[] fields = new Object[1];
		fields[0] = questionId;

		final AssessmentServiceImpl service = this;
		List all = m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					float score = result.getFloat(1);
					rv.add(new Float(score));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getQuestionScores: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/*************************************************************************************************************************************************
	 * Authoring Support
	 ************************************************************************************************************************************************/

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
	public AssessmentSection newSection(Assessment assessment)
	{
		AssessmentSectionImpl section = new AssessmentSectionImpl();
		section.initAssement((AssessmentImpl) assessment);
		((AssessmentImpl) assessment).sections.add(section);

		return section;
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
	public Boolean allowAddAssessment(String context)
	{
		// check permission - user must have MANAGE_PERMISSION in the context
		boolean ok = checkSecurity(m_sessionManager.getCurrentSessionUserId(), MANAGE_PERMISSION, context, getAssessmentReference(""));

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowListDeliveryAssessment(String context)
	{
		// check permission - user must have SUBMIT_PERMISSION in the context
		boolean ok = checkSecurity(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, context, getAssessmentReference(""));

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addAssessment(final Assessment assessment) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("addAssessment: " + assessment.getId());

		// check permission - created by user must have PUBLISH_PERMISSION in the context of the assessment
		secure(m_sessionManager.getCurrentSessionUserId(), MANAGE_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

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
			m_eventTrackingService.post(m_eventTrackingService.newEvent(TEST_ADD, getAssessmentReference(assessment.getId()), true));
		}

		// cache a copy
		cacheAssessment(new AssessmentImpl((AssessmentImpl) assessment));
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
				fields[0] = section.getId();
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

	/*************************************************************************************************************************************************
	 * Submission Support
	 ************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public Submission newSubmission(Assessment assessment)
	{
		SubmissionImpl submission = new SubmissionImpl(this);
		submission.setInited();

		submission.setAssessment(assessment);

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionAnswer newSubmissionAnswer(Submission submission, AssessmentQuestion question)
	{
		SubmissionAnswerImpl answer = new SubmissionAnswerImpl();
		answer.initSubmission((SubmissionImpl) submission);
		((SubmissionImpl) submission).answers.add(answer);

		answer.initQuestion(question);

		return answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addSubmission(final Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		// TODO: update the date to now? That would block past / future dating for special purposes... -ggolden

		// TODO: check for important values, such as assessment, user, dates... -ggolden

		Assessment assessment = submission.getAssessment();

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(submission.getUserId(), SUBMIT_PERMISSION, getAssessmentReference(assessment.getId()));
		}

		// check permission - submission user must have SUBMIT_PERMISSION in the context of the assessment
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, submission.getSubmittedDate(), 0)) throw new AssessmentClosedException();

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment, submission.getUserId());
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		if (M_log.isDebugEnabled()) M_log.debug("addSubmission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				addSubmissionTx(submission);
			}
		}, "addSubmission:" + submission.getId());

		// cache a copy
		cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));

		// event track it
		if (m_threadLocalManager.get("sakai.event.suppress") == null)
		{
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_ADD, getSubmissionReference(submission.getId()), true));
		}
	}

	/**
	 * The transaction for addSubmission
	 */
	protected void addSubmissionTx(Submission submission)
	{
		// TODO: eval? skip it?

		// we only work with our impl
		SubmissionImpl s = (SubmissionImpl) submission;

		// ID column? For non sequence db vendors, it is defaulted
		Long id = m_sqlService.getNextSequence("SAM_ASSESSMENTGRADING_ID_S", null);

		// Note: ASSESSMENTGRADINGID column is set to autoincrement... by using the special JDBC feature in dbInsert, we get the
		// value just allocated
		String statement = "INSERT INTO SAM_ASSESSMENTGRADING_T"
				+ " (PUBLISHEDASSESSMENTID, AGENTID, SUBMITTEDDATE, ISLATE, FORGRADE, TOTALAUTOSCORE,"
				+ " TOTALOVERRIDESCORE, FINALSCORE, STATUS, ATTEMPTDATE, TIMEELAPSED" + ((id == null) ? "" : ", ASSESSMENTGRADINGID") + ")"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?" + ((id == null) ? "" : ",?") + ")";
		Object fields[] = new Object[(id == null) ? 11 : 12];
		fields[0] = s.getAssessmentId();
		fields[1] = s.getUserId();
		fields[2] = s.getSubmittedDate();
		fields[3] = new Integer(0); // TODO: islate
		fields[4] = s.getIsComplete().booleanValue() ? new Integer(1) : new Integer(0);
		fields[5] = s.getAnswersAutoScore();
		fields[6] = new Float(0); // TODO: from evaluation, the total of all manual scores for the submission
		fields[7] = fields[5]; // TODO: from evaluation, the total score, auto plus manual...
		fields[8] = s.getStatus();
		fields[9] = s.getStartDate();
		fields[10] = (s.getElapsedTime() == null) ? null : (s.getElapsedTime() / 1000);

		if (id != null)
		{
			fields[11] = id;
			m_sqlService.dbWrite(statement, fields);
		}
		else
		{
			id = m_sqlService.dbInsert(null, statement, fields, "ASSESSMENTGRADINGID");
		}

		// we really need that id
		if (id == null) throw new RuntimeException("failed to insert submission");

		// update the id
		s.initId(id.toString());

		// answers
		for (SubmissionAnswerImpl answer : s.answers)
		{
			// each answer has one or more entries
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

				statement = "INSERT INTO SAM_ITEMGRADING_T"
						+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
						+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
						+ " VALUES (?,?,?,?,?,?,?,?,?,?" + ((answerId == null) ? "" : ",?") + ")";
				fields = new Object[(answerId == null) ? 10 : 11];
				fields[0] = answer.getSubmission().getId();
				fields[1] = answer.getQuestionId();
				// if the entry's assessment answer is null, use the single part id
				fields[2] = (entry.getAssessmentAnswer() != null) ? entry.getAssessmentAnswer().getPart().getId() : answer.getQuestion().getPart()
						.getId();
				fields[3] = s.getUserId();
				fields[4] = answer.getSubmittedDate();
				fields[5] = (entry.getAssessmentAnswer() == null) ? null : entry.getAssessmentAnswer().getId();
				fields[6] = answer.getRationale();
				fields[7] = entry.getAnswerText();
				fields[8] = entry.getAutoScore();
				fields[9] = new Float(0); // TODO: manual score from evaluation for this answer (divided up over the entries)

				if (answerId != null)
				{
					fields[10] = answerId;
					m_sqlService.dbWrite(statement, fields);
				}
				else
				{
					answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
				}

				// we really need that id
				if (answerId == null) throw new RuntimeException("failed to insert submission answer");
				entry.initId(answerId.toString());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(Assessment assessment, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled())
			M_log.debug("allowSubmit: assessment: " + ((assessment == null) ? "null" : assessment.getId()) + " user: " + userId);

		Boolean rv = Boolean.FALSE;
		if (assessment != null)
		{
			// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
			if (checkSecurity(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, assessment.getContext(),
					getAssessmentReference(assessment.getId())))
			{
				// check that the assessment is currently open for submission
				// if there is an in-progress submission, but it's too late now... this would catch it
				if (isAssessmentOpen(assessment, m_timeService.newTime(), 0))
				{
					// see if the user has a submission in progress
					Submission submission = getSubmissionInProgress(assessment, userId);
					if (submission != null)
					{
						rv = Boolean.TRUE;
					}

					// if not, can we make one? Check if there are remaining submissions for this user
					// (also checks that the assessment is open)
					else
					{
						Integer count = countRemainingSubmissions(assessment, userId);
						if ((count != null) && (count.intValue() != 0))
						{
							rv = Boolean.TRUE;
						}
					}
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowCompleteSubmission(Submission submission, String userId)
	{
		Boolean rv = Boolean.FALSE;

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (submission != null)
		{
			// make sure the user is this submission's user
			if (submission.getUserId().equals(userId))
			{
				// make sure the submission is incomplete
				if ((submission.getIsComplete() == null) || (!submission.getIsComplete().booleanValue()))
				{
					Assessment assessment = submission.getAssessment();
					if (assessment != null)
					{
						// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
						if (checkSecurity(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, assessment.getContext(),
								getAssessmentReference(assessment.getId())))
						{
							// check that the assessment is currently open for submission
							if (isAssessmentOpen(assessment, m_timeService.newTime(), GRACE))
							{
								rv = Boolean.TRUE;
							}
						}
					}
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowReviewSubmission(Submission submission, String userId)
	{
		Boolean rv = Boolean.FALSE;

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (submission != null)
		{
			// make sure the user is this submission's user
			if (submission.getUserId().equals(userId))
			{
				// make sure the submission is complete
				if ((submission.getIsComplete() != null) && (submission.getIsComplete().booleanValue()))
				{
					rv = Boolean.TRUE;
				}
			}
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Submission enterSubmission(Assessment a, String userId) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		if (a == null) return null;

		// trust only the id of the assessment passed in - get fresh and trusted additional information
		Assessment assessment = idAssessment(a.getId());

		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled()) M_log.debug("enterSubmission: assessment: " + assessment.getId() + " user: " + userId + " asOf: " + asOf);

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		secure(userId, SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, asOf, 0)) throw new AssessmentClosedException();

		// see if we have one already
		Submission submissionInProgress = getSubmissionInProgress(assessment, userId);
		if (submissionInProgress != null)
		{
			// event track it (not a modify event)
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_CONTINUE, getSubmissionReference(submissionInProgress.getId()),
					false));

			return submissionInProgress;
		}

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment, userId);
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		// TODO: it is possible to make too many submissions for the assessment.
		// If this method is entered concurrently for the same user and assessment, the previous count check might fail.
		final Submission submission = newSubmission(assessment);
		submission.setUserId(userId);
		submission.setStatus(new Integer(0));
		submission.setIsComplete(Boolean.FALSE);
		submission.setStartDate(asOf);

		if (M_log.isDebugEnabled()) M_log.debug("addSubmission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				addSubmissionTx(submission);
			}
		}, "addSubmission:" + submission.getId());

		// cache a copy
		cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_ENTER, getSubmissionReference(submission.getId()), true));

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(SubmissionAnswer answer, Boolean completeAnswer, Boolean completSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		List<SubmissionAnswer> answers = new ArrayList<SubmissionAnswer>(1);
		answers.add(answer);
		submitAnswers(answers, completeAnswer, completSubmission);
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswers(final List<SubmissionAnswer> answers, Boolean completeAnswers, final Boolean completSubmission)
			throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException
	{
		if ((answers == null) || (answers.size() == 0)) return;

		// TODO: one transaction, or separate ones?

		// trust only the answer information passed in, and the submission id it points to - get fresh and trusted additional
		// information
		Submission submission = idSubmission(answers.get(0).getSubmission().getId());
		Assessment assessment = submission.getAssessment();

		// a submission from the cache to update and re-cache
		SubmissionImpl recache = null;

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswer: submission: " + submission.getId() + " complete?: " + Boolean.toString(completSubmission) + " asOf: " + asOf);

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, getAssessmentReference(assessment
					.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not
		// submission)
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		// Note: we accept answers up to GRACE ms after any hard deadlilne
		if (!isAssessmentOpen(assessment, asOf, GRACE)) throw new AssessmentClosedException();

		// update the submission parameter for the caller
		submission.setSubmittedDate(asOf);
		for (SubmissionAnswer answer : answers)
		{
			// mark a submitted date only if the answer is complete
			if ((completeAnswers != null) && (completeAnswers.booleanValue()))
			{
				answer.setSubmittedDate(asOf);
			}

			// auto-score
			answer.autoScore();
		}

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				submitAnswersTx(answers, completSubmission);
			}
		}, "submitAnswers:" + submission.getId());

		// if complete and the assessment is integrated into the Gradebook, record the grade
		if ((completSubmission != null) && completSubmission.booleanValue())
		{
			recordInGradebook(submission, true);
		}

		// collect the cached submission, before the event clears it
		recache = getCachedSubmission(submission.getId());

		// event track it (one for each answer)
		for (SubmissionAnswer answer : answers)
		{
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_ANSWER, getSubmissionReference(submission.getId()) + ":"
					+ answer.getQuestion().getId(), true));
		}

		// track if we are complete
		if ((completSubmission != null) && completSubmission.booleanValue())
		{
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()), true));
		}

		// the submission is altered by this - clear the cache (or update)
		unCacheSubmission(submission.getId());

		// recache (this object that used to be in the cache is no longer in the cache, so we are the only owner)
		if (recache != null)
		{
			// if the cached submission has had its answers read, we will update them
			if (recache.isAnswersInited())
			{
				for (SubmissionAnswer answer : answers)
				{
					// This new answer for it's question id should replace an existing on in the submission, or, be added to the
					// answers
					SubmissionAnswerImpl old = recache.findAnswer(answer.getQuestion().getId());
					if (old != null)
					{
						recache.answers.remove(old);
					}
					recache.answers.add((SubmissionAnswerImpl) answer);
				}
			}

			recache.initSubmittedDate(asOf);

			if ((completSubmission != null) && (completSubmission.booleanValue()))
			{
				recache.initStatus(new Integer(1));
				recache.initIsComplete(Boolean.TRUE);
			}

			// cache the object
			cacheSubmission(recache);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void submitAnswersTx(List<SubmissionAnswer> answers, Boolean completSubmission)
	{
		for (SubmissionAnswer answer : answers)
		{
			// unwrap file upload attachments from multiple entries to just one
			// TODO: do this when save submission also
			List<SubmissionAnswerEntryImpl> entries = ((SubmissionAnswerImpl) answer).entries;
			if (answer.getQuestion().getType() == QuestionType.fileUpload)
			{
				SubmissionAnswerEntryImpl sample = entries.get(0);
				entries = new ArrayList<SubmissionAnswerEntryImpl>(1);
				SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl(sample);
				entry.setAnswerText(null);
				entry.setAssessmentAnswer(null);

				// set the entry id to the id we saved in the answer
				entry.id = ((SubmissionAnswerImpl) answer).id;
				entries.add(entry);
			}

			// create submission answer record(s) if needed
			for (SubmissionAnswerEntryImpl entry : entries)
			{
				if (entry.getId() == null)
				{
					Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

					// this will score the answer based on values in the database
					String statement = "INSERT INTO SAM_ITEMGRADING_T"
							+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
							+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE, REVIEW" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?," + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
							// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ ((answerId == null) ? "" : ",?") + ")";
					Object[] fields = new Object[(answerId == null) ? 10 : 11];
					fields[0] = answer.getSubmission().getId();
					fields[1] = answer.getQuestion().getId();
					fields[2] = entry.getQuestionPart().getId();
					fields[3] = answer.getSubmission().getUserId();
					fields[4] = answer.getSubmittedDate();
					fields[5] = (entry.getAssessmentAnswer() == null) ? null : entry.getAssessmentAnswer().getId();
					fields[6] = answer.getRationale();
					fields[7] = entry.getAnswerText();
					fields[8] = entry.getAutoScore();
					fields[9] = new Float(0);

					if (answerId != null)
					{
						fields[10] = answerId;
						if (!m_sqlService.dbWrite(statement, fields))
						{
							// TODO: better exception
							throw new RuntimeException("submitAnswer: dbWrite Failed");
						}
					}
					else
					{
						answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
						if (answerId == null)
						{
							// TODO: better exception
							throw new RuntimeException("submitAnswers: dbInsert Failed");
						}
					}

					// set the id into the answer
					if (answerId == null) throw new RuntimeException("failed to insert submission answer");
					entry.initId(answerId.toString());
					if (((SubmissionAnswerImpl) answer).id == null) ((SubmissionAnswerImpl) answer).id = answerId.toString();
				}

				// otherwise update the submission answer record
				else
				{
					String statement = "UPDATE SAM_ITEMGRADING_T"
							+ " SET SUBMITTEDDATE = ?, PUBLISHEDANSWERID = ?, PUBLISHEDITEMTEXTID = ?, RATIONALE = ?, ANSWERTEXT = ?, AUTOSCORE = ?,"
							+ " REVIEW = " + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
							// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ " WHERE ITEMGRADINGID = ?";
					// TODO: for added security, add to WHERE: AND ASSESSMENTGRADINGID = ?answer.getSubmissionId() AND
					// PUBLISHEDITEMID = ?answer.getQuestionId() -ggolden
					Object[] fields = new Object[7];
					fields[0] = answer.getSubmittedDate();
					fields[1] = (entry.getAssessmentAnswer() == null) ? null : entry.getAssessmentAnswer().getId();
					fields[2] = entry.getQuestionPart().getId();
					fields[3] = answer.getRationale();
					fields[4] = entry.getAnswerText();
					fields[5] = entry.getAutoScore();
					fields[6] = entry.getId();

					if (!m_sqlService.dbWrite(statement, fields))
					{
						// TODO: better exception
						throw new RuntimeException("submitAnswers: dbWrite Failed");
					}
				}
			}

			// for any entries unused that have an id, delete them
			for (SubmissionAnswerEntryImpl entry : ((SubmissionAnswerImpl) answer).recycle)
			{
				if (entry.getId() != null)
				{
					String statement = "DELETE FROM SAM_ITEMGRADING_T WHERE ITEMGRADINGID = ?";
					Object[] fields = new Object[1];
					fields[0] = entry.getId();
					if (!m_sqlService.dbWrite(statement, fields))
					{
						// TODO: better exception
						throw new RuntimeException("submitAnswers: dbWrite Failed");
					}
				}
			}

			// clear the unused now we have deleted what we must
			((SubmissionAnswerImpl) answer).recycle.clear();
		}

		Submission submission = idSubmission(answers.get(0).getSubmission().getId());

		// if complete, update the STATUS to 1 and the FORGRADE to TRUE... always update the date
		// Note: for Samigo compat., we need to update the scores in the SAM_ASSESSMENTGRADING_T based on the sums of the item
		// scores
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T"
				+ " SET SUBMITTEDDATE = ?,"
				+ " TOTALAUTOSCORE = (SELECT SUM(AUTOSCORE)+SUM(OVERRIDESCORE) FROM SAM_ITEMGRADING_T WHERE ASSESSMENTGRADINGID = ?),"
				+ " FINALSCORE = TOTALAUTOSCORE+TOTALOVERRIDESCORE"
				+ (((completSubmission != null) && completSubmission.booleanValue()) ? (" ,STATUS = 1, FORGRADE = " + m_sqlService
						.getBooleanConstant(true)) : "") + " WHERE ASSESSMENTGRADINGID = ?";
		Object[] fields = new Object[3];
		fields[0] = submission.getSubmittedDate();
		fields[1] = submission.getId();
		fields[2] = submission.getId();
		if (!m_sqlService.dbWrite(statement, fields))
		{
			// TODO: better exception
			throw new RuntimeException("submitAnswers: dbWrite Failed");
		}
	}

	/**
	 * Record this submission in the gradebook. Assume it has complete and ready to go.
	 * 
	 * @param submission
	 *        The submission to record in the gradebook.
	 * @param refresh
	 *        if true, get the latest score from the db, if false, use the final score from the submission.
	 */
	protected void recordInGradebook(Submission submission, boolean refresh)
	{
		Assessment assessment = submission.getAssessment();

		// if the assessment is integrated into the Gradebook, record the grade
		if ((assessment.getGradebookIntegration() != null) && assessment.getGradebookIntegration().booleanValue())
		{
			// is there a gradebook? - we could just not care here, save all those GB db calls, and let it throw later
			// but that forces us to do our single read for score -ggolden
			if (true /* m_gradebookService.isGradebookDefined(assessment.getContext()) */)
			{
				Double points = null;

				if (refresh)
				{
					// read the final score
					String statement = "SELECT FINALSCORE FROM SAM_ASSESSMENTGRADING_T WHERE ASSESSMENTGRADINGID = ?";
					Object[] fields = new Object[1];
					fields[0] = submission.getId();
					final List<String> scores = new ArrayList<String>(1);
					m_sqlService.dbRead(statement, fields, new SqlReader()
					{
						public Object readSqlResultRecord(ResultSet result)
						{
							try
							{
								String score = result.getString(1);
								scores.add(score);
								return null;
							}
							catch (SQLException e)
							{
								M_log.warn("submitAnswers: " + e);
								return null;
							}
						}
					});

					if (scores.size() == 1)
					{
						points = Double.valueOf(scores.get(0));
					}
				}

				else
				{
					points = submission.getTotalScore().doubleValue();
				}

				// post it
				try
				{
					m_gradebookService.updateExternalAssessmentScore(assessment.getContext(), assessment.getId(), submission.getUserId(), points);
				}
				catch (GradebookNotFoundException e)
				{
					// if there's no gradebook for this context, oh well...
					M_log.warn("recordInGradebook: (no gradebook for context): " + e);
				}
				catch (AssessmentNotFoundException e)
				{
					// if the assessment has not been registered in gb, this is a problem
					M_log.warn("recordInGradebook: (assessment has not been registered in context's gb): " + e);
				}
			}
		}
	}

	/**
	 * Add a single entry to the answer to reserve an id for this answer.
	 * 
	 * @param answer
	 *        The submission answer.
	 */
	protected void reserveAnswer(final SubmissionAnswerImpl answer)
	{
		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				reserveAnswerTx(answer);
			}
		}, "reserveAnswer: " + answer.getSubmission().getId());

		// collect the cached submission, before the event clears it
		SubmissionImpl recache = getCachedSubmission(answer.getSubmission().getId());

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_ANSWER, getSubmissionReference(answer.getSubmission().getId()) + ":"
				+ answer.getQuestion().getId(), true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(answer.getSubmission().getId());

		// recache (this object used to be in the cache but is no longer, so we are the only owner)
		if (recache != null)
		{
			// if the cached submission has had its answers read, we will update it and re-cache
			if (recache.isAnswersInited())
			{
				// This new answer for it's question id should replace an existing on in the submission, or, be added to the answers
				SubmissionAnswerImpl old = recache.findAnswer(answer.getQuestion().getId());
				if (old != null)
				{
					recache.answers.remove(old);
				}
				recache.answers.add(answer);
			}

			// cache the object
			cacheSubmission(recache);
		}
	}

	/**
	 * Transaction code for reserveAnswer.
	 * 
	 * @param answer
	 *        The submission answer.
	 */
	protected void reserveAnswerTx(SubmissionAnswerImpl answer)
	{
		Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", null);

		// this will score the answer based on values in the database
		String statement = "INSERT INTO SAM_ITEMGRADING_T"
				+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
				+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE, REVIEW" + ((answerId == null) ? "" : ", ITEMGRADINGID") + ")"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?," + m_sqlService.getBooleanConstant(answer.getMarkedForReview())
				// TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
				+ ((answerId == null) ? "" : ",?") + ")";
		Object[] fields = new Object[(answerId == null) ? 10 : 11];
		fields[0] = answer.getSubmission().getId();
		fields[1] = answer.getQuestion().getId();
		answer.getQuestion().getPart().getId();
		fields[2] = answer.getQuestion().getPart().getId();
		fields[3] = answer.getSubmission().getUserId();
		fields[4] = answer.getSubmittedDate();
		fields[5] = null;
		fields[6] = answer.getRationale();
		fields[7] = null;
		fields[8] = new Float(0);
		fields[9] = new Float(0);

		if (answerId != null)
		{
			fields[10] = answerId;
			if (!m_sqlService.dbWrite(statement, fields))
			{
				// TODO: better exception
				throw new RuntimeException("reserveAnswer: dbWrite Failed");
			}
		}
		else
		{
			answerId = m_sqlService.dbInsert(null, statement, fields, "ITEMGRADINGID");
			if (answerId == null)
			{
				// TODO: better exception
				throw new RuntimeException("reserveAnswer: dbInsert Failed");
			}
		}

		// set the id into the answer
		if (answerId == null) throw new RuntimeException("failed to insert submission answer");
		answer.id = answerId.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(final Submission s) throws AssessmentPermissionException, AssessmentClosedException, SubmissionCompletedException
	{
		// trust only the submission id passed in - get fresh and trusted additional information
		Submission submission = idSubmission(s.getId());
		Assessment assessment = submission.getAssessment();

		// the current time
		final Time asOf = m_timeService.newTime();

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, getAssessmentReference(assessment
					.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not
		// submission)
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, asOf, GRACE)) throw new AssessmentClosedException();

		if (M_log.isDebugEnabled()) M_log.debug("completeSubmission: submission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				completeSubmissionTx(asOf, s.getId());
			}
		}, "completeSubmission:" + s.getId());

		// record in the gradebook if so configured
		recordInGradebook(submission, true);

		// update the submission parameter for the caller
		s.setSubmittedDate(asOf);
		s.setStatus(new Integer(1));
		s.setIsComplete(Boolean.TRUE);

		// collect the cached submission, before the event clears it
		SubmissionImpl recache = getCachedSubmission(s.getId());

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_COMPLETE, getSubmissionReference(submission.getId()), true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(submission.getId());

		// recache (this object used to be in the cache, but has been cleared, so we are the only owner)
		if (recache != null)
		{
			recache.initSubmittedDate(asOf);
			recache.initStatus(new Integer(1));
			recache.initIsComplete(Boolean.TRUE);

			// cache the object
			cacheSubmission(recache);
		}
	}

	/**
	 * Transaction code for completeSubmission.
	 */
	protected void completeSubmissionTx(Time asOf, String submissionId)
	{
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T" + " SET SUBMITTEDDATE = ?, STATUS = 1, FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " WHERE ASSESSMENTGRADINGID = ?";
		Object fields[] = new Object[2];
		fields[0] = asOf;
		fields[1] = submissionId;
		m_sqlService.dbWrite(statement, fields);
	}

	/**
	 * Score a submission answer.
	 * 
	 * @param answer
	 *        The answer to score.
	 */
	protected void scoreAnswer(SubmissionAnswerImpl answer)
	{
		AssessmentQuestion question = answer.getQuestion();
		// types that score: fillIn, matching, multipleChoice, multipleCorrect, numeric, trueFalse

		// first, score each entry
		int pos = 1;
		for (SubmissionAnswerEntryImpl entry : answer.entries)
		{
			// score the entry.
			float score = 0;
			if (question.getPoints() > 0)
			{
				// trueFalse scoring
				if (question.getType() == QuestionType.trueFalse)
				{
					// question score if correct, 0 if not
					if (entry.getIsCorrect().booleanValue())
					{
						score = question.getPoints();
					}
				}

				// multipleChoice scoring
				else if (question.getType() == QuestionType.multipleChoice)
				{
					// question score if correct, 0 if not
					if (entry.getIsCorrect().booleanValue())
					{
						score = question.getPoints();
					}
				}

				// multipleAnswer scoring
				else if (question.getType() == QuestionType.multipleCorrect)
				{
					// give a % (score / # answers marked correct) for a correct answer, or negative that for incorrect
					float correctScore = 0;
					float incorrectScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
						incorrectScore = -1 * correctScore;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
					else
					{
						score = incorrectScore;
					}
				}

				// fillIn scoring
				else if (question.getType() == QuestionType.fillIn)
				{
					// give a % (score / # answers marked correct) for each correct entry, 0 for incorrect
					float correctScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}

				// numeric scoring
				else if (question.getType() == QuestionType.numeric)
				{
					// give a % (score / # answers marked correct) for each correct entry, 0 for incorrect
					float correctScore = 0;
					// Note: assume a single part question
					int numCorrectAnswers = question.getPart().getCorrectAnswers().size();

					if (numCorrectAnswers > 0)
					{
						correctScore = question.getPoints() / numCorrectAnswers;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}

				// matching scoring
				else if (question.getType() == QuestionType.matching)
				{
					// give a % (score / # parts) for each correct entry, 0 for incorrect
					float correctScore = 0;
					int numParts = question.getParts().size();

					if (numParts > 0)
					{
						correctScore = question.getPoints() / numParts;
					}

					if (entry.getIsCorrect().booleanValue())
					{
						score = correctScore;
					}
				}
			}

			// set it
			entry.initAutoScore(score);

			pos++;
		}

		// if the answer now has a negative auto-score, adjust to 0
		while (answer.countAutoScore() < 0)
		{
			boolean found = false;
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				// remove a negative scoring
				if (entry.getAutoScore() < 0)
				{
					entry.initAutoScore(new Float(0));
					found = true;
					break;
				}
			}

			if (!found)
			{
				// trouble: no more entries to clear, still negative
				M_log.warn("scoreAnswers: ran out of entries to clear to get to 0 score: submissionId: " + answer.getSubmission().getId()
						+ " questionId: " + question.getId());
				break;
			}
		}

		// mutually exclusive check
		if ((question.getType() == QuestionType.fillIn) && (question.getMutuallyExclusive().booleanValue()))
		{
			// check all but the last entry, looking down the list for a match
			// if this answer matches any following answer, and their question text also matches, this answer gets zero'ed out
			for (int i = 0; i < answer.entries.size() - 1; i++)
			{
				SubmissionAnswerEntryImpl entry = answer.entries.get(i);

				// if this entry is already 0, skip it
				if (entry.getAutoScore() > 0)
				{
					// this is the question text that must match some entry-down-below's question text
					String entryQuestionText = entry.getAssessmentAnswer().getText();

					// look down the list
					for (int j = i + 1; j < answer.entries.size(); j++)
					{
						// compare to this entry
						SubmissionAnswerEntryImpl compareEntry = answer.entries.get(j);

						// they need to be the same (i.e. !different) based on our case sensitive (the method takes ignore case, so
						// we reverse)
						if (!StringUtil.different(entry.getAnswerText(), compareEntry.getAnswerText(), !question.getCaseSensitive().booleanValue()))
						{
							// we will check against this other question's text, exactly
							String compareEntryQuestionText = compareEntry.getAssessmentAnswer().getText();
							if (entryQuestionText.equals(compareEntryQuestionText))
							{
								// we have a later match, so we zero score the entry
								entry.initAutoScore(new Float(0));

								// stop checking this entry
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Check a submission answer for complete correctness
	 * 
	 * @param answer
	 *        The answer to score.
	 * @return true if the answer is completely correct, false if not.
	 */
	protected boolean checkAnswer(SubmissionAnswerImpl answer)
	{
		AssessmentQuestion question = answer.getQuestion();

		// trueFalse / multipleChoice - one entry to check
		if ((question.getType() == QuestionType.trueFalse) || (question.getType() == QuestionType.multipleChoice))
		{
			if (answer.entries.get(0).getIsCorrect().booleanValue())
			{
				return true;
			}

			return false;
		}

		// multipleAnswer - only the correct choices should be selected
		else if (question.getType() == QuestionType.multipleCorrect)
		{
			// these correct answers must be selected
			List<AssessmentAnswer> correct = question.getPart().getCorrectAnswers();

			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				if (correct.contains(entry.getAssessmentAnswer()))
				{
					correct.remove(entry.getAssessmentAnswer());
				}

				// otherwise we found an incorrect entry
				else
				{
					return false;
				}
			}

			// if we have an entry for each correct, this is correct
			if (correct.isEmpty()) return true;

			return false;
		}

		// fillIn / numeric / matching - all entries must be correct
		else if ((question.getType() == QuestionType.fillIn) || (question.getType() == QuestionType.numeric)
				|| (question.getType() == QuestionType.matching))
		{
			for (SubmissionAnswerEntryImpl entry : answer.entries)
			{
				if (!entry.getIsCorrect().booleanValue())
				{
					return false;
				}
			}

			// mutually exclusive check
			if ((question.getType() == QuestionType.fillIn) && (question.getMutuallyExclusive().booleanValue()))
			{
				// check all but the last entry, looking down the list for a match
				// if this answer matches any following answer, and their question text also matches, this answer gets zero'ed out
				for (int i = 0; i < answer.entries.size() - 1; i++)
				{
					SubmissionAnswerEntryImpl entry = answer.entries.get(i);

					// this is the question text that must match some entry-down-below's question text
					String entryQuestionText = entry.getAssessmentAnswer().getText();

					// look down the list
					for (int j = i + 1; j < answer.entries.size(); j++)
					{
						// compare to this entry
						SubmissionAnswerEntryImpl compareEntry = answer.entries.get(j);

						// they need to be the same (i.e. !different) based on our case sensitive (the method takes ignore case, so
						// we reverse)
						if (!StringUtil.different(entry.getAnswerText(), compareEntry.getAnswerText(), !question.getCaseSensitive().booleanValue()))
						{
							// we will check against this other question's text, exactly
							String compareEntryQuestionText = compareEntry.getAssessmentAnswer().getText();
							if (entryQuestionText.equals(compareEntryQuestionText))
							{
								// we have a later match, so this is not correct
								return false;
							}
						}
					}
				}
			}

			return true;
		}

		return false;
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
	 * Check if the user has an open submission to this assessment, and return it if found.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param userId
	 *        The user id.
	 * @return The open submission for this user to this assessment, if found, or null if not.
	 */
	protected Submission getSubmissionInProgress(Assessment assessment, String userId)
	{
		Submission rv = null;

		// see if we have one already
		String statement = "SELECT AG.ASSESSMENTGRADINGID" + " FROM SAM_ASSESSMENTGRADING_T AG"
				+ " WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(false);
		// TODO: order by id asc so we always use the lowest (oldest) if there are ever two open?
		Object[] fields = new Object[2];
		fields[0] = assessment.getId();
		fields[1] = userId;
		List results = m_sqlService.dbRead(statement, fields, null);
		if (results.size() > 0)
		{
			// we have one
			if (results.size() > 1)
				M_log.warn("getSubmissionInProgress: multiple incomplete submissions: " + results.size() + " aid: " + assessment.getId()
						+ " userId: " + userId);
			rv = idSubmission((String) results.get(0));
		}

		return rv;
	}

	/*************************************************************************************************************************************************
	 * Runnable - checking thread
	 ************************************************************************************************************************************************/

	/** The checker thread. */
	protected Thread m_thread = null;

	/** The thread quit flag. */
	protected boolean m_threadStop = false;

	/**
	 * Start the clean and report thread.
	 */
	protected void start()
	{
		m_threadStop = false;

		m_thread = new Thread(this, getClass().getName());
		m_thread.start();
	}

	/**
	 * Stop the clean and report thread.
	 */
	protected void stop()
	{
		if (m_thread == null) return;

		// signal the thread to stop
		m_threadStop = true;

		// wake up the thread
		m_thread.interrupt();

		m_thread = null;
	}

	/**
	 * Run the event checking thread.
	 */
	public void run()
	{
		// since we might be running while the component manager is still being created and populated, such as at server startup,
		// wait here for a complete component manager
		ComponentManager.waitTillConfigured();

		// loop till told to stop
		while ((!m_threadStop) && (!Thread.currentThread().isInterrupted()))
		{
			try
			{
				// get a list of submission ids that are open, timed, and well expired considering double our grace period, or open and past a retract
				// date or hard deadline
				List<Submission> submissions = getTimedOutSubmissions(2 * GRACE);

				// for each one, close it if it is still open
				for (Submission submission : submissions)
				{
					// we need to establish the "current" user to be the submission user
					// so that various attributions of the complete process have the proper user
					String user = submission.getUserId();
					Session s = m_sessionManager.getCurrentSession();
					if (s != null)
					{
						s.setUserId(user);
					}
					else
					{
						M_log.warn("run - no SessionManager.getCurrentSession, cannot set to user");
					}

					// complete this submission, using the exact 'over' date for the final date
					Time over = submission.getWhenOver();
					completeTheSubmission(over, submission);
				}
			}
			catch (Throwable e)
			{
				M_log.warn("run: will continue: ", e);
			}
			finally
			{
				// clear out any current current bindings
				m_threadLocalManager.clear();
			}

			// take a small nap
			try
			{
				Thread.sleep(m_timeoutCheckMs);
			}
			catch (Exception ignore)
			{
			}
		}
	}

	/**
	 * Find the submissions that are open, timed, and well expired, or open and past a retract date or hard deadline
	 * 
	 * @param grace
	 *        The number of ms past the time limit that the submission's elapsed time must be to qualify
	 * @return A List of the submissions that are open, timed, and well expired.
	 */
	protected List<Submission> getTimedOutSubmissions(final long grace)
	{
		if (M_log.isDebugEnabled()) M_log.debug("getTimedOutSubmissions");

		final Time asOf = m_timeService.newTime();

		// select all the open submissions (or, just the TIMELIMIT > 0 or DUEDATE not null or RETRACTDATE not null?)
		String statement = "SELECT AG.ASSESSMENTGRADINGID, AG.ATTEMPTDATE, AG.AGENTID, AG.FINALSCORE, AG.PUBLISHEDASSESSMENTID,"
				+ " PAC.TIMELIMIT, PAC.DUEDATE, PAC.RETRACTDATE, PAC.LATEHANDLING" + " FROM SAM_ASSESSMENTGRADING_T AG"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON AG.PUBLISHEDASSESSMENTID = PAC.ASSESSMENTID" + " WHERE AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(false);

		Object[] fields = new Object[0];

		final AssessmentServiceImpl service = this;
		final List<Submission> rv = new ArrayList<Submission>();
		m_sqlService.dbRead(statement, fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String submissionId = result.getString(1);

					java.sql.Timestamp ts = result.getTimestamp(2, m_sqlService.getCal());
					Time attemptDate = null;
					if (ts != null)
					{
						attemptDate = m_timeService.newTime(ts.getTime());
					}

					String userId = result.getString(3);
					float score = result.getFloat(4);
					String publishedAssessmentId = result.getString(5);

					// convert to ms from seconds
					long timeLimit = result.getLong(6) * 1000;

					ts = result.getTimestamp(7, m_sqlService.getCal());
					Time dueDate = null;
					if (ts != null)
					{
						dueDate = m_timeService.newTime(ts.getTime());
					}

					ts = result.getTimestamp(8, m_sqlService.getCal());
					Time retractDate = null;
					if (ts != null)
					{
						retractDate = m_timeService.newTime(ts.getTime());
					}

					boolean allowLate = (result.getInt(9) == 1);

					// see if we want this one
					boolean selected = false;

					// for timed, if the elapsed time since their start is well past the time limit
					if ((timeLimit > 0) && (attemptDate != null) && ((asOf.getTime() - attemptDate.getTime()) > (timeLimit + grace)))
					{
						selected = true;
					}

					// for past retract date
					if ((retractDate != null) && (asOf.getTime() > (retractDate.getTime() + grace)))
					{
						selected = true;
					}

					// for past hard due date
					if ((dueDate != null) && (!allowLate) && (asOf.getTime() > (dueDate.getTime() + grace)))
					{
						selected = true;
					}

					if (selected)
					{
						// create or update these properties in the submission cache
						SubmissionImpl cachedSubmission = getCachedSubmission(submissionId);
						if (cachedSubmission == null)
						{
							// cache an empty one
							cachedSubmission = new SubmissionImpl(service);
							cachedSubmission.initId(submissionId);
							cacheSubmission(cachedSubmission);
						}
						synchronized (cachedSubmission)
						{
							cachedSubmission.initAssessmentId(publishedAssessmentId);
							cachedSubmission.initTotalScore(score);
							cachedSubmission.initStartDate(attemptDate);
							cachedSubmission.initUserId(userId);
							cachedSubmission.initIsComplete(Boolean.FALSE);
						}

						rv.add(new SubmissionImpl(cachedSubmission));

						// create or update these properties in the assessment cache
						AssessmentImpl cachedAssessment = getCachedAssessment(publishedAssessmentId);
						if (cachedAssessment == null)
						{
							// cache an empty one
							cachedAssessment = new AssessmentImpl(service);
							cachedAssessment.initId(publishedAssessmentId);
							cacheAssessment(cachedAssessment);
						}
						synchronized (cachedAssessment)
						{
							cachedAssessment.initTimeLimit(timeLimit == 0 ? null : new Long(timeLimit));
							cachedAssessment.initDueDate(dueDate);
							cachedAssessment.initAllowLateSubmit(Boolean.valueOf(allowLate));
							cachedAssessment.initRetractDate(retractDate);
						}
					}
					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("getTimedOutSubmissions: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Mark the submission as auto-complete as of now.
	 * 
	 * @param asOf
	 *        The effective time of the completion.
	 * @param submission
	 *        The submission.
	 * @return true if it was successful, false if not.
	 */
	protected boolean completeTheSubmission(Time asOf, final Submission submission)
	{
		// the current time if not set
		if (asOf == null) asOf = m_timeService.newTime();
		final Time fAsOf = asOf;

		if (M_log.isDebugEnabled()) M_log.debug("completeTheSubmission: submission: " + submission.getId());

		// run our save code in a transaction that will restart on deadlock
		// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
		m_sqlService.transact(new Runnable()
		{
			public void run()
			{
				completeTheSubmissionTx(fAsOf, submission.getId());
			}
		}, "completeSubmission:" + submission.getId());

		// record in the gradebook if so configured, using data only from the submission, no db refresh
		recordInGradebook(submission, false);

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMISSION_AUTO_COMPLETE, getSubmissionReference(submission.getId()), true));

		// the submission is altered by this - clear the cache
		unCacheSubmission(submission.getId());

		return true;
	}

	/**
	 * The transaction code for completeTheSubmission.
	 */
	protected void completeTheSubmissionTx(Time asOf, String submissionId)
	{
		String statement = "UPDATE SAM_ASSESSMENTGRADING_T" + " SET SUBMITTEDDATE = ?, STATUS = 1, FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " WHERE ASSESSMENTGRADINGID = ? AND FORGRADE = " + m_sqlService.getBooleanConstant(false);
		Object fields[] = new Object[2];
		fields[0] = asOf;
		fields[1] = submissionId;
		m_sqlService.dbWrite(statement, fields);
	}
}
