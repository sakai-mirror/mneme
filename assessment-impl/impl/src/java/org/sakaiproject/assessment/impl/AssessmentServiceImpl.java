/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assessment.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentClosedException;
import org.sakaiproject.assessment.api.AssessmentCompletedException;
import org.sakaiproject.assessment.api.AssessmentPermissionException;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.AssessmentService;
import org.sakaiproject.assessment.api.AssessmentStatus;
import org.sakaiproject.assessment.api.FeedbackDelivery;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.assessment.api.QuestionPart;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.assessment.api.SubmissionAnswerEntry;
import org.sakaiproject.assessment.api.SubmissionCompletedException;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
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
 * <p>
 * AssessmentServiceImpl is ...
 * </p>
 */
public class AssessmentServiceImpl implements AssessmentService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssessmentServiceImpl.class);

	/** A cache of assessments. */
	protected Cache m_assessmentCache = null;

	/** A cache of submissions. */
	protected Cache m_submissionCache = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

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

		// form the azGroups for a context-as-implemented-by-site (Note the *lack* of direct dependency on Site, i.e. we stole the code!)
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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected ThreadLocalManager m_threadLocalManager = null;

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

	protected TimeService m_timeService = null;

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

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

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

	/** Dependency: SessionManager */
	protected SessionManager m_sessionManager = null;

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

	/** Dependency: MemoryService */
	protected MemoryService m_memoryService = null;

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

	/** Dependency: SecurityService */
	protected SecurityService m_securityService = null;

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

	/** Dependency: EventTrackingService */
	protected EventTrackingService m_eventTrackingService = null;

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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Configuration
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** The # seconds to cache assessment reads. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

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

	/** The # seconds between cache cleaning runs. */
	protected int m_cacheCleanerSeconds = 0;

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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

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

				m_submissionCache = m_memoryService.newHardCache(m_cacheCleanerSeconds, getSubmissionReference(""));
			}

			M_log.info("init(): caching minutes: " + m_cacheSeconds / 60 + " cache cleaner minutes: " + m_cacheCleanerSeconds / 60);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssessmentService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Assessment Access
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** TODO: Note: assessments ids are (for now) assumed to be published - the Samigo 1 data model does not have a unique assessment id across published and non-published. */

	/**
	 * Form an assessment reference for this assessment id.
	 * 
	 * @param assessmentId
	 *        the assessment id.
	 * @return the assessment reference for this assessment id.
	 */
	protected String getAssessmentReference(String assessmentId)
	{
		String ref = "/assessment/" + assessmentId;
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
			return new AssessmentImpl(assessment);
		}

		// TODO: perhaps don't check, just set the id... then we need to support objects that have id set but are known to be bad... -ggolden
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
	 * Id each of the assessments in the id collection
	 * 
	 * @param ids
	 *        The collection of assessment ids.
	 * @return A collection if id'ed assessments, one for each id.
	 */
	protected Collection<Assessment> idAssessments(Collection<String> ids)
	{
		Collection<Assessment> rv = new ArrayList<Assessment>(ids.size());
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

		// cached?
		AssessmentImpl cached = getCachedAssessment(id);
		AssessmentImpl assessment = cached;

		// if not cached, get started with an identification
		if (assessment == null)
		{
			assessment = new AssessmentImpl(this);
			assessment.initId(id);
		}

		// if we need to, read the main info
		if (!assessment.isMainInited())
		{
			boolean found = readAssessmentMain(assessment);
			if (!found) return null;
		}

		// if we need to, read the sections
		if (!assessment.isSectionsInited())
		{
			readAssessmentSections(assessment);
		}

		// if it was not already cached, cache it, otherwise, we already updated the actual cached object
		if (cached == null)
		{
			// it was not cached, so we need to cache it
			cacheAssessment(assessment);
		}

		// return a copy so we don't return the cache
		return new AssessmentImpl(assessment);
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

		// otherwise check the thread-local cache
		else
		{
			return (AssessmentImpl) m_threadLocalManager.get(ref);
		}

		return null;
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
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED, PAC.TIMELIMIT, PAC.AUTOSUBMIT, PAC.STARTDATE, PAC.RETRACTDATE, PAC.LATEHANDLING"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ?"
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
					int timeLimit = result.getInt(13);
					int autoSubmit = result.getInt(14);
					int allowLateSubmit = result.getInt(15);

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

					// pack it into the assessment
					assessment.initAutoSubmit((autoSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);
					assessment.initContext(context);
					assessment.initCreatedBy(createdBy);
					assessment.initDueDate(dueDate);
					assessment.initFeedbackDate(feedbackDate);
					assessment.initFeedbackDelivery(delivery);
					assessment.initFeedbackShowStatistics(Boolean.valueOf(showStatistics));
					assessment.initFeedbackShowScore(Boolean.valueOf(showStudentScore));
					assessment.initMultipleSubmissionSelectionPolicy(mssPolicy);
					assessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));
					assessment.initStatus(status);
					assessment.initTimeLimit(timeLimit == 0 ? null : new Integer(timeLimit));
					assessment.initTitle(title);
					assessment.initReleaseDate(releaseDate);
					assessment.initRetractDate(retractDate);
					assessment.initAllowLateSubmit((allowLateSubmit == 1) ? Boolean.TRUE : Boolean.FALSE);

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
				cached.setMain(assessment);
			}

			return true;
		}

		// we didn't find it
		return false;
	}

	/**
	 * Read the sections and questions of the assessment (not the main)
	 * 
	 * @param assessment
	 *        The assessment impl with the id set to fill in.
	 */
	protected void readAssessmentSections(final AssessmentImpl assessment)
	{
		if (M_log.isDebugEnabled()) M_log.debug("readAssessmentSections: " + assessment.getId());

		if (assessment.getId() == null)
		{
			M_log.warn("readAssessmentSections: attempt to read with no id set");
			return;
		}

		// get the sections
		String statement = "SELECT P.SECTIONID, P.TITLE, P.DESCRIPTION FROM SAM_PUBLISHEDSECTION_T P WHERE P.ASSESSMENTID = ? ORDER BY P.SEQUENCE ASC";
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

					// pack it into an assessment section
					AssessmentSectionImpl section = new AssessmentSectionImpl();
					section.initId(sectionId);
					section.setTitle(title);
					section.setDescription(description);

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
		statement = "SELECT PI.ITEMID, PI.HASRATIONALE, PI.SCORE, PI.INSTRUCTION, PI.TYPEID, PI.SECTIONID, MCS.ENTRY, MME.ENTRY"
				+ " FROM SAM_PUBLISHEDITEM_T PI"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON PI.SECTIONID = PS.SECTIONID AND PS.ASSESSMENTID = ?"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMMETADATA_T MCS ON PI.ITEMID = MCS.ITEMID AND MCS.LABEL = 'CASE_SENSITIVE'"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDITEMMETADATA_T MME ON PI.ITEMID = MME.ITEMID AND MME.LABEL = 'MUTUALLY_EXCLUSIVE'"
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

					// pack it into an assessment question
					AssessmentQuestionImpl question = new AssessmentQuestionImpl();
					question.initId(questionId);
					question.setRequireRationale(Boolean.valueOf(hasRationale));
					question.setScore(new Float(score));
					question.setInstructions(instructions);
					question.setType(QuestionType.valueOf(type));
					question.setCaseSensitive(caseSensitive == null ? null : Boolean.parseBoolean(caseSensitive));
					question.setMutuallyExclusive(mutuallyExclusive == null ? null : Boolean.parseBoolean(mutuallyExclusive));

					// add the question to the appropriate section (sectionId)
					AssessmentSectionImpl section = (AssessmentSectionImpl) assessment.getSection(sectionId);
					if (section == null)
					{
						M_log.warn("readAssessmentParts: missing section to store question: section id: " + sectionId
								+ " questionId: " + questionId);
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
						M_log.warn("readAssessmentSections: missing question to store text: question id: " + questionId
								+ " textId: " + partId);
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
		statement = "SELECT PA.ANSWERID, PA.ITEMID, PA.TEXT, PA.ISCORRECT, PA.LABEL, PA.ITEMTEXTID"
				+ " FROM SAM_PUBLISHEDANSWER_T PA" + " INNER JOIN SAM_PUBLISHEDITEM_T PI ON PA.ITEMID = PI.ITEMID"
				+ " INNER JOIN SAM_PUBLISHEDSECTION_T PS ON PI.SECTIONID = PS.SECTIONID AND PS.ASSESSMENTID = ?"
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
					String text = result.getString(3);
					boolean isCorrect = result.getBoolean(4);
					String label = result.getString(5);
					String partId = result.getString(6);

					// find the question
					AssessmentQuestionImpl question = (AssessmentQuestionImpl) assessment.getQuestion(questionId);
					if (question == null)
					{
						M_log.warn("readAssessmentSections: missing question to store answer: question id: " + questionId
								+ " answerId: " + answerId);
					}
					else
					{
						// find the part
						QuestionPartImpl part = (QuestionPartImpl) question.getPart(partId);
						if (part == null)
						{
							M_log.warn("readAssessmentSections: missing question part to store answer: question id: " + questionId
									+ " partId: " + partId + " answerId: " + answerId);
						}
						else
						{
							// pack it into an assessment answer
							AssessmentAnswerImpl answer = new AssessmentAnswerImpl();
							answer.initId(answerId);
							answer.setIsCorrect(Boolean.valueOf(isCorrect));
							answer.setText(text);
							answer.setLabel(label);

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

		// update the cache if cached
		AssessmentImpl cached = getCachedAssessment(assessment.getId());
		if (cached != null)
		{
			cached.setSections(assessment);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Submission Access
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Form a submission reference for this submission id.
	 * 
	 * @param submissionId
	 *        the submission id.
	 * @return the submission reference for this assessment id.
	 */
	protected String getSubmissionReference(String submissionId)
	{
		String ref = "/submission/" + submissionId;
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
			return new SubmissionImpl(submission);
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
	 * Id each of the submission in the id collection
	 * 
	 * @param ids
	 *        The collection of submission ids.
	 * @return A collection if id'ed submission, one for each id.
	 */
	protected Collection<Submission> idSubmissions(Collection<String> ids)
	{
		Collection<Submission> rv = new ArrayList<Submission>(ids.size());
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
		SubmissionImpl submission = cached;

		// if not cached, get started with an identification
		if (submission == null)
		{
			submission = new SubmissionImpl(this);
			submission.initId(id);
		}

		// if we need to, read the main info
		if (!submission.isMainInited())
		{
			boolean found = readSubmissionMain(submission);
			if (!found) return null;
		}

		// if we need to, read the answers
		if (!submission.isAnswersInited())
		{
			readSubmissionAnswers(submission);
		}

		// if it was not already cached, cache it, otherwise, we already updated the actual cached object
		if (cached == null)
		{
			// it was not cached, so we need to cache it
			cacheSubmission(submission);
		}

		// return a copy so we don't return the cache
		return new SubmissionImpl(submission);
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

		String statement = "SELECT AG.PUBLISHEDASSESSMENTID, AG.TOTALOVERRIDESCORE, AG.SUBMITTEDDATE, AG.AGENTID, AG.FORGRADE, AG.ATTEMPTDATE, AG.STATUS"
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

					// pack it into a submission
					submission.initAssessmentId(aid);
					submission.initIsComplete(Boolean.valueOf(complete));
					submission.initStartDate(startDate);
					submission.initStatus(new Integer(status));
					submission.initSubmittedDate(submittedDate);
					submission.initUserId(userId);

					// TODO: the manual score, and the coment (not yet in the SELECT list) can form the evaluation for this...

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
				cached.setMain(submission);
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
		if (M_log.isDebugEnabled()) M_log.debug("readSubmissionAnswers: " + submission.getId());

		if (submission.getId() == null)
		{
			M_log.warn("readSubmissionAnswers: attempt to read with no id set");
			return;
		}

		// mark the submission as inited for the answers, so the methods we are about to call don't try to re-read the answers
		submission.answersStatus = SubmissionImpl.PropertyStatus.inited;

		// read the answers
		String statement = "SELECT I.ITEMGRADINGID, I.SUBMITTEDDATE, I.PUBLISHEDANSWERID, I.RATIONALE, I.ANSWERTEXT, I.REVIEW, I.PUBLISHEDITEMID, I.AUTOSCORE"
				+ " FROM SAM_ITEMGRADING_T I"
				+ " LEFT OUTER JOIN SAM_PUBLISHEDANSWER_T A ON I.PUBLISHEDANSWERID = A.ANSWERID"
				+ " WHERE ASSESSMENTGRADINGID = ?" + " ORDER BY A.SEQUENCE ASC";
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

						answer.initSubmission(submission);
						submission.answers.add(answer);
					}

					// add an entry to the answer
					SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
					entry.setAssessmentAnswerId(answerId);
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

		// update the cache if cached
		SubmissionImpl cached = getCachedSubmission(submission.getId());
		if (cached != null)
		{
			cached.setAnswers(submission);
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
		String ref = getSubmissionReference(id);

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			// if it is in there
			if (m_submissionCache.containsKey(ref))
			{
				return (SubmissionImpl) m_submissionCache.get(ref);
			}
		}

		// otherwise check the thread-local cache
		else
		{
			return (SubmissionImpl) m_threadLocalManager.get(ref);
		}

		return null;
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

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			m_submissionCache.put(ref, submission, m_cacheSeconds);
		}

		// else thread-local cache
		else
		{
			m_threadLocalManager.set(ref, submission);
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

		// if we are short-term caching
		if (m_submissionCache != null)
		{
			// Note: the cache will clear when the event is processed...
			// m_submissionCache.remove(ref);
		}

		// else thread-local cache
		else
		{
			m_threadLocalManager.set(ref, null);
		}
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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Delivery Support
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public Integer countRemainingSubmissions(String assessmentId, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		final Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("countRemainingSubmissions: assessment: " + assessmentId + " userId: " + userId + " asOf: " + asOf);

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
		fields[0] = assessmentId;
		fields[1] = userId;
		fields[2] = assessmentId;

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

		return (rv.size() == 0) ? null : (Integer) rv.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<Assessment> getAvailableAssessments(final String context, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		// the current time
		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("getAvailableAssessmentsIds: context: " + context + " userId: " + userId + " asOf: " + asOf);

		// Notes: "in context"
		// SAM_ASSESSMENTBASE_T has the defined assessments (ID)
		// SAM_PUBLISHEDASSESSMENT_T has the main published assessment info, referring back to the SAM_ASSESSMENTBASE_T table's entry with (ASSESSMENTID)
		// SAM_AUTHZDATA_T maps permissions (FUNCTIONID = TAKE_PUBLISHED_ASSESSMENT) for sites (AGENTID) to assessments by published assessment id (QUALIFIERID)
		// SAM_AUTHZDATA_T QUALIFIERID can be either base assessment or published assessment; the FUNCTIONID for published are a separate set than from base (EDIT_ASSESSMENT)

		// Notes: released and not yet retracted
		// SAM_PUBLISHEDACCESSCONTROL_T joins with the SAM_PUBLISHEDASSESSMENT_T (ASSESSMENTID) to define the active period (STARTDATE) and (RETRACTDATE)
		// either may be null
		// we want now to be >= startdate and < retractdate (edges?)

		// Note: due date and last policy
		// SAM_PUBLISHEDACCESSCONTROL_T joins with the SAM_PUBLISHEDASSESSMENT_T (ASSESSMENTID) to define the due date (DUEDATE) - may be null
		// and (LATEHANDLING) is 1 to allow late submissions, 2 to not allow late submissions

		// Note: join with submissions
		// SAM_ASSESSMENTGRADING_T joins in on PUBLISHEDASSESSMENTID to the SAM_PUBLISHEDASSESSMENT_T table for each submission
		// A left outer join gives us a record for each assessment, even with no submissions, and multiple records, one for each submission.
		// The GROUP BY lets us get a count of submissions and collapses the records down to one per assessment
		// We need the inner select so we can compute the counts, then filter out those that have reached their submit limit
		// Counting the AG.PUBLISHEDASSESSMENTID column gives us an accurate count of how many submissions - if there are none, this will end up null and give a 0 count.

		// Note: number of submissions allowed
		// SAM_PUBLISHEDACCESSCONTROL_T SUBMISSIONSALLOWED is null for unlimited, or has a count

		// Note: extra info
		// anticipating that we need the title and duedate (etc) for each assessment, we get it here and cache it so we can return it later in the thread

		String statement = "SELECT X.ID, X.TITLE, X.DUEDATE FROM ("
				+ " SELECT P.ID ID, COUNT(AG.PUBLISHEDASSESSMENTID) SUBMITTED, PAC.SUBMISSIONSALLOWED ALLOWED, P.TITLE TITLE, PAC.DUEDATE DUEDATE"
				+ " FROM SAM_PUBLISHEDASSESSMENT_T P"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON P.ID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON"
				+ "      P.ID = PAC.ASSESSMENTID AND (PAC.STARTDATE IS NULL OR ? >= PAC.STARTDATE) AND (PAC.RETRACTDATE IS NULL OR ? < PAC.RETRACTDATE) AND (PAC.DUEDATE IS NULL OR ? < PAC.DUEDATE OR PAC.LATEHANDLING = 1)"
				+ " LEFT OUTER JOIN SAM_ASSESSMENTGRADING_T AG ON P.ID = AG.PUBLISHEDASSESSMENTID AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(true) + " GROUP BY P.ID, PAC.SUBMISSIONSALLOWED, P.TITLE, PAC.DUEDATE" + " ) X"
				+ " WHERE (X.ALLOWED IS NULL OR X.SUBMITTED < X.ALLOWED)";

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
					cached.initTitle(title);
					cached.initDueDate(dueDate);

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
	 * {@inheritDoc}
	 */
	public Collection<Submission> getOfficialSubmissions(final String context, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		if (M_log.isDebugEnabled()) M_log.debug("getOfficialSubmissionsIds: context: " + context + " userId: " + userId);

		// Note: submissions
		// SAM_ASSESSMENTGRADING_T lists submissions to assessments
		// the user id is in the AGENTID column, the assessment is in the PUBLISHEDASSESSMENTID column

		// Notes: "in context"
		// SAM_AUTHZDATA_T maps permissions (FUNCTIONID = TAKE_PUBLISHED_ASSESSMENT) for sites (AGENTID) to assessments by published assessment id (QUALIFIERID)

		// Notes: official submission
		// Of many submissions, either the latest or the highest graded is used
		// SAM_PUBLISHEDEVALUATION_T, joined in by the published assessment id, has the SCORINGTYPE column, 1=highest, 2=latest
		// (as known by MultipleSubmissionSelectionPolicy)

		// Note: finding the max totalScore
		// joining the grading table (left outer) to itself, mathing on the published assessment, where the left totalScore < right totalScore, then selecting the
		// records where the right id is null will pick those grading records that are the maximum totalScore for each assessment... (http://www.artfulsoftware.com/queries.php#7)
		// When joining, we have to make sure we specify the full criteria against the grading table to avoid stray records slipping into the join
		// (we might need to add the context criteria too, which is another join?)
		// But, since each assessment might have a seperate criteria, and the selection criteria is rather complex, here we get all the submissions and do our own filtering

		// Note: complete v.s. in progress submissions
		// the FORGRADE boolean is set when the submission is complete, false while it is in progress - these don't count.

		String statement = "SELECT AG.ASSESSMENTGRADINGID, AG.PUBLISHEDASSESSMENTID, P.TITLE, AG.FINALSCORE, AG.ATTEMPTDATE,"
				+ " PAC.FEEDBACKDATE, AG.SUBMITTEDDATE, PE.SCORINGTYPE,"
				+ " PF.FEEDBACKDELIVERY, PF.SHOWSTUDENTSCORE, PF.SHOWSTATISTICS, AG.FORGRADE,"
				+ " PAC.UNLIMITEDSUBMISSIONS, PAC.SUBMISSIONSALLOWED"
				+ " FROM SAM_ASSESSMENTGRADING_T AG"
				+ " INNER JOIN SAM_AUTHZDATA_T AD ON AG.PUBLISHEDASSESSMENTID = AD.QUALIFIERID AND AD.FUNCTIONID = ? AND AD.AGENTID = ?"
				+ " INNER JOIN SAM_PUBLISHEDASSESSMENT_T P ON AG.PUBLISHEDASSESSMENTID = P.ID"
				+ " INNER JOIN SAM_PUBLISHEDACCESSCONTROL_T PAC ON AG.PUBLISHEDASSESSMENTID = PAC.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDFEEDBACK_T PF ON AG.PUBLISHEDASSESSMENTID = PF.ASSESSMENTID"
				+ " INNER JOIN SAM_PUBLISHEDEVALUATION_T PE ON AG.PUBLISHEDASSESSMENTID = PE.ASSESSMENTID"
				+ " WHERE AG.AGENTID = ? AND AG.FORGRADE = " + m_sqlService.getBooleanConstant(true)
				+ " ORDER BY AG.SUBMITTEDDATE ASC";

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
						// cache an empty one
						cachedSubmission = new SubmissionImpl(service);
						cachedSubmission.initId(submissionId);
						cacheSubmission(cachedSubmission);
					}
					cachedSubmission.initAssessmentId(publishedAssessmentId);
					cachedSubmission.initTotalScore(score);
					cachedSubmission.initStartDate(attemptDate);
					cachedSubmission.initSubmittedDate(submittedDate);
					cachedSubmission.initIsComplete(Boolean.valueOf(complete));

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
					cachedAssessment.initTitle(title);
					cachedAssessment.initFeedbackDate(feedbackDate);
					cachedAssessment.initMultipleSubmissionSelectionPolicy(MultipleSubmissionSelectionPolicy.parse(mssPolicy));
					cachedAssessment.initFeedbackDelivery(feedbackDelivery);
					cachedAssessment.initFeedbackShowScore(Boolean.valueOf(showScore));
					cachedAssessment.initFeedbackShowStatistics(Boolean.valueOf(showStatistics));
					cachedAssessment.initNumSubmissionsAllowed(unlimitedSubmissions ? null : new Integer(submissionsAllowed));

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
		Collection<String> ids = new ArrayList<String>();

		while (all.size() > 0)
		{
			// take the first one out
			String sid = (String) all.remove(0);
			String aid = idSubmission(sid).getAssessment().getId();
			MultipleSubmissionSelectionPolicy policy = idAssessment(aid).getMultipleSubmissionSelectionPolicy();
			Object value = (policy == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED) ? (Object) (((SubmissionImpl) idSubmission(sid))
					.getTotalScore())
					: (Object) idSubmission(sid).getSubmittedDate();

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
						// for totalScore, if the winner so far is smaller or equal to the new, use the new (the later one for a tie is the later submission based on our sort)
						if (((Float) value).floatValue() <= ((SubmissionImpl) idSubmission(candidateId)).getTotalScore()
								.floatValue())
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

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Authoring Support
	 *********************************************************************************************************************************************************************************************************************************************************/

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
		// check permission - cuser must have PUBLISH_PERMISSION in the context
		boolean ok = checkSecurity(m_sessionManager.getCurrentSessionUserId(), PUBLISH_PERMISSION, context,
				getAssessmentReference(""));

		return Boolean.valueOf(ok);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addAssessment(Assessment assessment) throws AssessmentPermissionException
	{
		if (M_log.isDebugEnabled()) M_log.debug("addAssessment: " + assessment.getId());

		// we only work with our impl
		AssessmentImpl a = (AssessmentImpl) assessment;

		// TODO: validate that title is unique? in the db?

		// now
		Time now = m_timeService.newTime();

		// user
		String userId = m_sessionManager.getCurrentSessionUserId();

		// establish defaults
		// TODO: check context, others? check that the createdby is the current user?
		if (a.getStatus() == null) a.setStatus(AssessmentStatus.ACTIVE);
		if (a.getCreatedBy() == null) a.setCreatedBy(userId);

		// check permission - created by user must have PUBLISH_PERMISSION in the context of the assessment
		secure(m_sessionManager.getCurrentSessionUserId(), PUBLISH_PERMISSION, assessment.getContext(),
				getAssessmentReference(assessment.getId()));

		// persist - all in one transaction
		Connection connection = null;
		boolean wasCommit = true;
		try
		{
			connection = m_sqlService.borrowConnection();
			wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// ID column? For non sequence db vendors, it is defaulted
			Long id = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", connection);

			// Note: ID column is set to autoincrement... by using the special JDBC feature in dbInsert, we get the value just allocated
			String statement = "INSERT INTO SAM_PUBLISHEDASSESSMENT_T"
					+ " (TITLE, ASSESSMENTID, DESCRIPTION, COMMENTS, TYPEID, INSTRUCTORNOTIFICATION, TESTEENOTIFICATION, MULTIPARTALLOWED,"
					+ " STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE" + ((id == null) ? "" : ", ID") + ")"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?" + ((id == null) ? "" : ",?") + ")";
			Object fields[] = new Object[(id == null) ? 13 : 14];
			fields[0] = a.getTitle();
			fields[1] = ""; // TODO: reference to the base (not published) assessment... a.getId();
			fields[2] = ""; // TODO: description
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
				m_sqlService.dbWrite(connection, statement, fields);
			}
			else
			{
				id = m_sqlService.dbInsert(connection, statement, fields, "ID");
			}

			// we really need that id
			if (id == null) throw new Exception("failed to insert published assessment");

			// update the id
			a.initId(id.toString());

			// each section
			for (AssessmentSectionImpl section : a.sections)
			{
				// ID column? For non sequence db vendors, it is defaulted
				Long sectionId = m_sqlService.getNextSequence("SAM_PUBLISHEDSECTION_ID_S", connection);

				statement = "INSERT INTO SAM_PUBLISHEDSECTION_T"
						+ " (ASSESSMENTID, DURATION, SEQUENCE, TITLE, DESCRIPTION, TYPEID, STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE"
						+ ((sectionId == null) ? "" : ", SECTIONID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?"
						+ ((sectionId == null) ? "" : ",?") + ")";
				fields = new Object[(sectionId == null) ? 11 : 12];
				fields[0] = id;
				fields[1] = null;
				fields[2] = section.getOrdering().getPosition();
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
					m_sqlService.dbWrite(connection, statement, fields);
				}
				else
				{
					sectionId = m_sqlService.dbInsert(connection, statement, fields, "SECTIONID");
				}

				// we really need that id
				if (sectionId == null) throw new Exception("failed to insert section");
				section.initId(sectionId.toString());

				// ID for SAM_PUBLISHEDSECTIONMETADATA_T
				// Note: Samigo as of 2.3 has a bug - using the same sequence as for the ID of SAM_PUBLISHEDASSESSMENT_T -ggolden
				// SAM_PUBLISHEDSECTIONMETADATA_ID_S ? Oracle does not like this..
				Long xid = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", connection);

				// TODO: are these constants, or do these need modeling?

				statement = "INSERT INTO SAM_PUBLISHEDSECTIONMETADATA_T (SECTIONID, LABEL, ENTRY"
						+ ((xid == null) ? "" : ", PUBLISHEDSECTIONMETADATAID") + ") values (?, ?, ?" + ((xid == null) ? "" : ",?")
						+ ")";
				fields = new Object[(xid == null) ? 3 : 4];
				fields[0] = sectionId;
				fields[1] = "AUTHOR_TYPE";
				fields[2] = "1";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(connection, statement, fields);

				xid = m_sqlService.getNextSequence("SAM_PUBLISHEDASSESSMENT_ID_S", connection);
				fields[1] = "QUESTIONS_ORDERING";
				fields[2] = "1";
				if (xid != null) fields[3] = xid;
				m_sqlService.dbWrite(connection, statement, fields);
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
			fields[3] = new Integer(1);
			fields[4] = null;
			fields[5] = new Integer(0);
			fields[6] = new Integer(0);
			fields[7] = null;
			fields[8] = ((assessment.getAllowLateSubmit() != null) && assessment.getAllowLateSubmit().booleanValue()) ? new Integer(
					1)
					: new Integer(0);
			fields[9] = assessment.getReleaseDate();
			fields[10] = assessment.getDueDate();
			fields[11] = null;
			fields[12] = assessment.getFeedbackDate();
			fields[13] = assessment.getRetractDate();
			fields[14] = new Integer(1);
			fields[15] = new Integer(2);
			fields[16] = new Integer(1);
			fields[17] = "";
			fields[18] = a.getContext() + " site";
			fields[19] = "";
			fields[20] = "";
			fields[21] = "";
			fields[22] = id;
			m_sqlService.dbWrite(connection, statement, fields);

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
			fields[8] = new Integer(1); // 1-to gradebook
			fields[9] = id;
			m_sqlService.dbWrite(connection, statement, fields);

			// Note: no id column for SAM_PUBLISHEDFEEDBACK_T
			statement = "INSERT INTO SAM_PUBLISHEDFEEDBACK_T"
					+ " (FEEDBACKDELIVERY, FEEDBACKAUTHORING, EDITCOMPONENTS, SHOWQUESTIONTEXT, SHOWSTUDENTRESPONSE,"
					+ " SHOWCORRECTRESPONSE, SHOWSTUDENTSCORE, SHOWSTUDENTQUESTIONSCORE, SHOWQUESTIONLEVELFEEDBACK,"
					+ " SHOWSELECTIONLEVELFEEDBACK, SHOWGRADERCOMMENTS, SHOWSTATISTICS, ASSESSMENTID)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
			fields = new Object[13];
			fields[0] = new Integer(1);
			fields[1] = new Integer(1);
			fields[2] = new Integer(1);
			fields[3] = new Integer(1);
			fields[4] = new Integer(1);
			fields[5] = new Integer(1);
			fields[6] = new Integer(1);
			fields[7] = new Integer(1);
			fields[8] = new Integer(1);
			fields[9] = new Integer(1);
			fields[10] = new Integer(1);
			fields[11] = new Integer(1);
			fields[12] = id;
			m_sqlService.dbWrite(connection, statement, fields);

			// ID for SAM_PUBLISHEDMETADATA_T
			Long xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);

			statement = "INSERT INTO SAM_PUBLISHEDMETADATA_T (ASSESSMENTID, LABEL, ENTRY"
					+ ((xid == null) ? "" : ", ASSESSMENTMETADATAID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
			fields = new Object[(xid == null) ? 3 : 4];
			fields[0] = id;
			fields[1] = "assessmentAuthor_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "retractDate_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "description_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "testeeIdentity_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "anonymousRelease_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "timedAssessment_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "displayNumbering_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "feedbackType_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "submissionModel_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "templateInfo_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "timedAssessmentAutoSubmit_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "releaseDate_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "releaseTo";
			fields[2] = "SITE_MEMBERS";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "authenticatedRelease_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "hasTimeAssessment";
			fields[2] = "false";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "displayChunking_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "metadataAssess_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "passwordRequired_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "lateHandling_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "recordedScore_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "author";
			fields[2] = "";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "itemAccessType_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "hasMetaDataForQuestions";
			fields[2] = "false";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "toGradebook_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "bgImage_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "dueDate_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "ipAccessType_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "finalPageURL_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "feedbackComponents_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "feedbackAuthoring_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "metadataQuestions_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "submissionMessage_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "bgColor_isInstructorEditable";
			fields[2] = "true";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDMETADATA_ID_S", connection);
			fields[1] = "ALIAS";
			fields[2] = IdManager.createUuid();
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			// ID for SAM_PUBLISHEDSECUREDIP_T
			xid = m_sqlService.getNextSequence("SAM_PUBLISHEDSECUREDIP_ID_S", connection);

			statement = "INSERT INTO SAM_PUBLISHEDSECUREDIP_T (ASSESSMENTID, HOSTNAME, IPADDRESS"
					+ ((xid == null) ? "" : ", IPADDRESSID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
			fields = new Object[(xid == null) ? 3 : 4];
			fields[0] = id;
			fields[1] = null;
			fields[2] = "";
			if (xid != null) fields[3] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			// ID for SAM_AUTHZDATA_S
			xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", connection);

			statement = "INSERT INTO SAM_AUTHZDATA_T (lockId, AGENTID, FUNCTIONID, QUALIFIERID, EFFECTIVEDATE, EXPIRATIONDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE, ISEXPLICIT"
					+ ((xid == null) ? "" : ", ID")
					+ ")"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?"
					+ ((xid == null) ? "" : ",?")
					+ ")";
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
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", connection);
			fields[2] = "TAKE_PUBLISHED_ASSESSMENT";
			if (xid != null) fields[9] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", connection);
			fields[2] = "VIEW_PUBLISHED_ASSESSMENT_FEEDBACK";
			if (xid != null) fields[9] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", connection);
			fields[2] = "GRADE_PUBLISHED_ASSESSMENT";
			if (xid != null) fields[9] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			xid = m_sqlService.getNextSequence("SAM_AUTHZDATA_S", connection);
			fields[2] = "VIEW_PUBLISHED_ASSESSMENT";
			if (xid != null) fields[9] = xid;
			m_sqlService.dbWrite(connection, statement, fields);

			// questions - from each section
			for (AssessmentSectionImpl section : a.sections)
			{
				for (AssessmentQuestionImpl question : section.questions)
				{
					// write the question
					Long questionId = m_sqlService.getNextSequence("SAM_PUBITEM_ID_S", connection);
					statement = "INSERT INTO SAM_PUBLISHEDITEM_T"
							+ " (SECTIONID, SEQUENCE, TYPEID, SCORE, HASRATIONALE, STATUS, CREATEDBY, CREATEDDATE, LASTMODIFIEDBY, LASTMODIFIEDDATE, INSTRUCTION"
							+ ((questionId == null) ? "" : ", ITEMID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?"
							+ ((questionId == null) ? "" : ",?") + ")";
					fields = new Object[(questionId == null) ? 11 : 12];
					fields[0] = section.getId();
					fields[1] = question.getSectionOrdering().getPosition();
					fields[2] = question.getType().getDbEncoding();
					fields[3] = question.getScore();
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
						m_sqlService.dbWrite(connection, statement, fields);
					}
					else
					{
						questionId = m_sqlService.dbInsert(connection, statement, fields, "ITEMID");
					}

					// we really need that id
					if (questionId == null) throw new Exception("failed to insert question");
					question.initId(questionId.toString());

					// parts (these go into the item text table)
					int sequence = 1;
					for (QuestionPart part : question.getParts())
					{
						Long partId = m_sqlService.getNextSequence("SAM_PUBITEMTEXT_ID_S", connection);
						statement = "INSERT INTO SAM_PUBLISHEDITEMTEXT_T (ITEMID, SEQUENCE, TEXT"
								+ ((partId == null) ? "" : ", ITEMTEXTID") + ")" + " VALUES (?, ?, ?"
								+ ((partId == null) ? "" : ",?") + ")";
						fields = new Object[(partId == null) ? 3 : 4];
						fields[0] = questionId;
						fields[1] = new Integer(sequence++);
						fields[2] = part.getTitle();
						if (partId != null)
						{
							fields[3] = partId;
							m_sqlService.dbWrite(connection, statement, fields);
						}
						else
						{
							partId = m_sqlService.dbInsert(connection, statement, fields, "ITEMTEXTID");
						}
						((QuestionPartImpl) part).initId(partId.toString());

						// answers - from each part
						int i = 0;
						for (AssessmentAnswer answer : part.getAnswers())
						{
							Long answerId = m_sqlService.getNextSequence("SAM_PUBANSWER_ID_S", connection);
							statement = "INSERT INTO SAM_PUBLISHEDANSWER_T"
									+ " (ITEMTEXTID, ITEMID, TEXT, SEQUENCE, LABEL, ISCORRECT, SCORE"
									+ ((answerId == null) ? "" : ", ANSWERID") + ")" + " VALUES (?,?,?,?,?,?,?"
									+ ((answerId == null) ? "" : ",?") + ")";
							fields = new Object[(answerId == null) ? 7 : 8];
							fields[0] = partId;
							fields[1] = questionId;
							fields[2] = answer.getText();
							fields[3] = answer.getPosition();
							fields[4] = answer.getLabel();
							fields[5] = answer.getIsCorrect();
							fields[6] = question.getScore();

							if (answerId != null)
							{
								fields[7] = answerId;
								m_sqlService.dbWrite(connection, statement, fields);
							}
							else
							{
								answerId = m_sqlService.dbInsert(connection, statement, fields, "ANSWERID");
							}

							// we really need that id
							if (answerId == null) throw new Exception("failed to insert answer");
							((AssessmentAnswerImpl) answer).initId(answerId.toString());
						}
					}

					// question feedback
					xid = m_sqlService.getNextSequence("SAM_PUBITEMFEEDBACK_ID_S", connection);
					statement = "INSERT INTO SAM_PUBLISHEDITEMFEEDBACK_T (ITEMID, TYPEID, TEXT"
							+ ((xid == null) ? "" : ", ITEMFEEDBACKID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
					fields = new Object[(xid == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = "InCorrect Feedback";
					fields[2] = "Incorrect Answer Feedback";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					xid = m_sqlService.getNextSequence("SAM_PUBITEMFEEDBACK_ID_S", connection);
					fields[1] = "Correct Feedback";
					fields[2] = "Correct Answer Feedback";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					// question metadata
					xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", connection);
					statement = "INSERT INTO SAM_PUBLISHEDITEMMETADATA_T (ITEMID, LABEL, ENTRY"
							+ ((xid == null) ? "" : ", ITEMMETADATAID") + ") VALUES (?, ?, ?" + ((xid == null) ? "" : ",?") + ")";
					fields = new Object[(xid == null) ? 3 : 4];
					fields[0] = questionId;
					fields[1] = "POOLID";
					fields[2] = "";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", connection);
					fields[1] = "MUTUALLY_EXCLUSIVE";
					fields[2] = "false";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", connection);
					fields[1] = "PARTID";
					fields[2] = "1";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", connection);
					fields[1] = "RANDOMIZE";
					fields[2] = "false";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);

					xid = m_sqlService.getNextSequence("SAM_PUBITEMMETADATA_ID_S", connection);
					fields[1] = "CASE_SENSITIVE";
					fields[2] = "false";
					if (xid != null) fields[3] = xid;
					m_sqlService.dbWrite(connection, statement, fields);
				}
			}

			connection.commit();

			// event track it
			m_eventTrackingService.post(m_eventTrackingService.newEvent(ASSESSMENT_PUBLISH, getSubmissionReference(assessment
					.getId()), true));

			// cache a copy
			cacheAssessment(new AssessmentImpl((AssessmentImpl) assessment));
		}
		catch (Exception e)
		{
			if (connection != null)
			{
				try
				{
					connection.rollback();
				}
				catch (Exception ee)
				{
					M_log.warn("addAssessment: rollback: " + ee);
				}
			}
			M_log.warn("addAssessment: " + e);
		}
		finally
		{
			if (connection != null)
			{
				// restore autocommit, if it was not false
				try
				{
					if (wasCommit) connection.setAutoCommit(wasCommit);
				}
				catch (Exception e)
				{
					M_log.warn("addAssessment, while setting auto commit: " + e);
				}

				// return the connetion
				m_sqlService.returnConnection(connection);
			}
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Submission Support
	 *********************************************************************************************************************************************************************************************************************************************************/

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
	public SubmissionAnswer newSubmissionAnswer(Submission submission)
	{
		SubmissionAnswerImpl answer = new SubmissionAnswerImpl();
		answer.initSubmission((SubmissionImpl) submission);
		((SubmissionImpl) submission).answers.add(answer);

		return answer;
	}

	/**
	 * {@inheritDoc}
	 */
	public SubmissionAnswerEntry newEntry(SubmissionAnswer answer)
	{
		SubmissionAnswerEntryImpl entry = new SubmissionAnswerEntryImpl();
		entry.initAnswer((SubmissionAnswerImpl) answer);
		((SubmissionAnswerImpl) answer).entries.add(entry);

		return entry;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addSubmission(Submission submission) throws AssessmentPermissionException, AssessmentClosedException,
			AssessmentCompletedException
	{
		// TODO: update the date to now? That would block past / future dating for special purposes... -ggolden

		// TODO: check for important values, such as assessment, user, dates... -ggolden

		Assessment assessment = submission.getAssessment();

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(submission.getUserId(), SUBMIT_PERMISSION, getAssessmentReference(assessment
					.getId()));
		}

		// check permission - submission user must have SUBMIT_PERMISSION in the context of the assessment
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, submission.getSubmittedDate())) throw new AssessmentClosedException();

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment.getId(), submission.getUserId());
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		addSubmission(submission, null);

		// event track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMIT_ADD, getSubmissionReference(submission.getId()), true));
	}

	/**
	 * Add a submission, possibly using an established connection / transaction
	 * 
	 * @param submission
	 *        The submission.
	 * @param conn
	 *        The connection, or null to use a new one.
	 */
	protected void addSubmission(Submission submission, Connection conn)
	{
		if (M_log.isDebugEnabled()) M_log.debug("addSubmission: " + submission.getId());

		// TODO: eval? skip it?

		// we only work with our impl
		SubmissionImpl s = (SubmissionImpl) submission;

		// persist - all in one transaction
		Connection connection = null;
		boolean wasCommit = true;
		try
		{
			// use the passed connection, or get and prep a new one
			if (conn == null)
			{
				connection = m_sqlService.borrowConnection();
				wasCommit = connection.getAutoCommit();
				connection.setAutoCommit(false);
			}
			else
			{
				connection = conn;
			}

			// ID column? For non sequence db vendors, it is defaulted
			Long id = m_sqlService.getNextSequence("SAM_ASSESSMENTGRADING_ID_S", connection);

			// Note: ASSESSMENTGRADINGID column is set to autoincrement... by using the special JDBC feature in dbInsert, we get the value just allocated
			String statement = "INSERT INTO SAM_ASSESSMENTGRADING_T"
					+ " (PUBLISHEDASSESSMENTID, AGENTID, SUBMITTEDDATE, ISLATE, FORGRADE, TOTALAUTOSCORE,"
					+ " TOTALOVERRIDESCORE, FINALSCORE, STATUS, ATTEMPTDATE, TIMEELAPSED"
					+ ((id == null) ? "" : ", ASSESSMENTGRADINGID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?"
					+ ((id == null) ? "" : ",?") + ")";
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
			fields[10] = s.getElapsedTime();

			if (id != null)
			{
				fields[11] = id;
				m_sqlService.dbWrite(connection, statement, fields);
			}
			else
			{
				id = m_sqlService.dbInsert(connection, statement, fields, "ASSESSMENTGRADINGID");
			}

			// we really need that id
			if (id == null) throw new Exception("failed to insert submission");

			// update the id
			s.initId(id.toString());

			// answers
			for (SubmissionAnswerImpl answer : s.answers)
			{
				// each answer has one or more entries
				for (SubmissionAnswerEntryImpl entry : answer.entries)
				{
					Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", connection);

					statement = "INSERT INTO SAM_ITEMGRADING_T"
							+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
							+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE" + ((answerId == null) ? "" : ", ITEMGRADINGID")
							+ ")" + " VALUES (?,?,?,?,?,?,?,?,?,?" + ((answerId == null) ? "" : ",?") + ")";
					fields = new Object[(answerId == null) ? 10 : 11];
					fields[0] = answer.getSubmission().getId();
					fields[1] = answer.getQuestionId();
					// if the entry's assessment answer is null, use the single part id
					fields[2] = (entry.getAssessmentAnswer() != null) ? entry.getAssessmentAnswer().getPart().getId() : answer
							.getQuestion().getPart().getId();
					fields[3] = s.getUserId();
					fields[4] = answer.getSubmittedDate();
					fields[5] = entry.getAssessmentAnswerId();
					fields[6] = answer.getRationale();
					fields[7] = entry.getAnswerText();
					fields[8] = entry.getAutoScore();
					fields[9] = new Float(0); // TODO: manual score from evaluation for this answer (divided up over the entries)

					if (answerId != null)
					{
						fields[10] = answerId;
						m_sqlService.dbWrite(connection, statement, fields);
					}
					else
					{
						answerId = m_sqlService.dbInsert(connection, statement, fields, "ITEMGRADINGID");
					}

					// we really need that id
					if (answerId == null) throw new Exception("failed to insert submission answer");
					entry.initId(answerId.toString());
				}
			}

			// commit only if we are using a new connection
			if (conn == null)
			{
				connection.commit();
			}
		}
		catch (Exception e)
		{
			// rollback only if we are on a new connection
			if (conn == null)
			{
				if (connection != null)
				{
					try
					{
						connection.rollback();
					}
					catch (Exception ee)
					{
						M_log.warn("addSubmission: rollback: " + ee);
					}
				}
			}
			M_log.warn("addSubmission: " + e);
		}
		finally
		{
			// cleanup and return, only of we are on a new connection
			if (conn == null)
			{
				if (connection != null)
				{
					// restore autocommit, if it was not false
					try
					{
						if (wasCommit) connection.setAutoCommit(wasCommit);
					}
					catch (Exception e)
					{
						M_log.warn("addSubmission, while setting auto commit: " + e);
					}

					// return the connetion
					m_sqlService.returnConnection(connection);
				}
			}
		}

		// cache a copy
		cacheSubmission(new SubmissionImpl((SubmissionImpl) submission));
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean allowSubmit(String assessmentId, String userId)
	{
		// if null, get the current user id
		if (userId == null) userId = m_sessionManager.getCurrentSessionUserId();

		Boolean rv = Boolean.FALSE;
		if (assessmentId != null)
		{
			Assessment assessment = idAssessment(assessmentId);
			if (assessment != null)
			{
				// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
				if (checkSecurity(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION, assessment.getContext(),
						getAssessmentReference(assessment.getId())))
				{
					// check that the assessment is currently open for submission
					// if there is an in-progress submission, but it's too late now... this would catch it
					if (isAssessmentOpen(assessment, m_timeService.newTime()))
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
							Integer count = countRemainingSubmissions(assessment.getId(), userId);
							if ((count != null) && (count.intValue() != 0))
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

		if (M_log.isDebugEnabled())
			M_log.debug("enterSubmission: assessment: " + assessment.getId() + " user: " + userId + " asOf: " + asOf);

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment
		secure(userId, SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, asOf)) throw new AssessmentClosedException();

		// see if we have one already
		Submission submission = getSubmissionInProgress(assessment, userId);
		if (submission != null)
		{
			// event track it (not a modify event)
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMIT_REENTER, getSubmissionReference(submission.getId()),
					false));

			return submission;
		}

		// if not, can we make one? Check if there are remaining submissions for this user
		Integer count = countRemainingSubmissions(assessment.getId(), userId);
		if ((count == null) || (count.intValue() == 0))
		{
			throw new AssessmentCompletedException();
		}

		// TODO: it is possible to make too many submissions for the assessment.
		// If this method is entered concurrently for the same user and assessment, the previous count check might fail.
		submission = newSubmission(assessment);
		submission.setUserId(userId);
		submission.setStatus(new Integer(0));
		submission.setIsComplete(Boolean.FALSE);
		submission.setStartDate(asOf);

		addSubmission(submission, null);

		// event track it
		m_eventTrackingService
				.post(m_eventTrackingService.newEvent(SUBMIT_ENTER, getSubmissionReference(submission.getId()), true));

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	public void submitAnswer(SubmissionAnswer answer, Boolean completSubmission) throws AssessmentPermissionException,
			AssessmentClosedException, SubmissionCompletedException
	{
		// TODO: one transaction, or separate ones?

		// trust only the answer information passed in, and the submission id it points to - get fresh and trusted additional information
		Submission submission = idSubmission(answer.getSubmission().getId());
		Assessment assessment = submission.getAssessment();

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		Time asOf = m_timeService.newTime();

		if (M_log.isDebugEnabled())
			M_log.debug("submitAnswer: submission: " + submission.getId() + " complete?: " + Boolean.toString(completSubmission)
					+ " asOf: " + asOf);

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION,
					getAssessmentReference(assessment.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not submission)
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, asOf)) throw new AssessmentClosedException();

		// update the submission parameter for the caller
		answer.setSubmittedDate(asOf);
		answer.getSubmission().setSubmittedDate(asOf);

		// auto-score
		answer.autoScore();

		Connection connection = null;
		boolean wasCommit = true;
		try
		{
			connection = m_sqlService.borrowConnection();
			wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// create submission answer record(s) if needed
			for (SubmissionAnswerEntryImpl entry : ((SubmissionAnswerImpl) answer).entries)
			{
				if (entry.getId() == null)
				{
					Long answerId = m_sqlService.getNextSequence("SAM_ITEMGRADING_ID_S", connection);

					// this will score the answer based on values in the database
					String statement = "INSERT INTO SAM_ITEMGRADING_T"
							+ " (ASSESSMENTGRADINGID, PUBLISHEDITEMID, PUBLISHEDITEMTEXTID, AGENTID, SUBMITTEDDATE, PUBLISHEDANSWERID,"
							+ " RATIONALE, ANSWERTEXT, AUTOSCORE, OVERRIDESCORE, REVIEW"
							+ ((answerId == null) ? "" : ", ITEMGRADINGID") + ")" + " VALUES (?,?,?,?,?,?,?,?,?,?,"
							+ m_sqlService.getBooleanConstant(answer.getMarkedForReview()) // TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ ((answerId == null) ? "" : ",?") + ")";
					Object[] fields = new Object[(answerId == null) ? 10 : 11];
					fields[0] = answer.getSubmission().getId();
					fields[1] = answer.getQuestion().getId();
					// if the entry's assessment answer is null, use the single part id
					fields[2] = (entry.getAssessmentAnswer() != null) ? entry.getAssessmentAnswer().getPart().getId() : answer
							.getQuestion().getPart().getId();
					fields[3] = answer.getSubmission().getUserId();
					fields[4] = answer.getSubmittedDate();
					fields[5] = entry.getAssessmentAnswerId();
					fields[6] = answer.getRationale();
					fields[7] = entry.getAnswerText();
					fields[8] = entry.getAutoScore();
					fields[9] = new Float(0);

					if (answerId != null)
					{
						fields[10] = answerId;
						if (!m_sqlService.dbWrite(connection, statement, fields))
						{
							// TODO: better exception
							throw new Exception("submitAnswer: dbWrite Failed");
						}
					}
					else
					{
						answerId = m_sqlService.dbInsert(connection, statement, fields, "ITEMGRADINGID");
						if (answerId == null)
						{
							// TODO: better exception
							throw new Exception("submitAnswer: dbInsert Failed");
						}
					}

					// set the id into the answer
					if (answerId == null) throw new Exception("failed to insert submission answer");
					entry.initId(answerId.toString());
				}

				// otherwise update the submission answer record
				else
				{
					String statement = "UPDATE SAM_ITEMGRADING_T"
							+ " SET SUBMITTEDDATE = ?, PUBLISHEDANSWERID = ?, PUBLISHEDITEMTEXTID = ?, RATIONALE = ?, ANSWERTEXT = ?, AUTOSCORE = ?,"
							+ " REVIEW = " + m_sqlService.getBooleanConstant(answer.getMarkedForReview()) // TODO: it would be nice if our ? / Boolean worked with bit fields -ggolden
							+ " WHERE ITEMGRADINGID = ?";
					// TODO: for added security, add to WHERE: AND ASSESSMENTGRADINGID = ?answer.getSubmissionId() AND PUBLISHEDITEMID = ?answer.getQuestionId() -ggolden
					Object[] fields = new Object[7];
					fields[0] = answer.getSubmittedDate();
					fields[1] = entry.getAssessmentAnswerId();
					// if the entry's assessment answer is null, use the single part id
					fields[2] = (entry.getAssessmentAnswer() != null) ? entry.getAssessmentAnswer().getPart().getId() : answer
							.getQuestion().getPart().getId();
					fields[3] = answer.getRationale();
					fields[4] = entry.getAnswerText();
					fields[5] = entry.getAutoScore();
					fields[6] = entry.getId();

					if (!m_sqlService.dbWrite(connection, statement, fields))
					{
						// TODO: better exception
						throw new Exception("submitAnswer: dbWrite Failed");
					}
				}
			}

			// for any entries unused that have an id, delete them
			for (SubmissionAnswerEntryImpl entry : ((SubmissionAnswerImpl) answer).unusedEntries)
			{
				if (entry.getId() != null)
				{
					String statement = "DELETE FROM SAM_ITEMGRADING_T WHERE ITEMGRADINGID = ?";
					Object[] fields = new Object[1];
					fields[0] = entry.getId();
					if (!m_sqlService.dbWrite(connection, statement, fields))
					{
						// TODO: better exception
						throw new Exception("submitAnswer: dbWrite Failed");
					}
				}
			}

			// clear the unused now we have deleted what we must
			((SubmissionAnswerImpl) answer).unusedEntries.clear();

			// if complete, update the STATUS to 1 and the FORGRADE to TRUE... always update the date
			// Note: for Samigo compat., we need to update the scores in the SAM_ASSESSMENTGRADING_T based on the sums of the item scores
			String statement = "UPDATE SAM_ASSESSMENTGRADING_T"
					+ " SET SUBMITTEDDATE = ?,"
					+ " TOTALAUTOSCORE = (SELECT SUM(AUTOSCORE)+SUM(OVERRIDESCORE) FROM SAM_ITEMGRADING_T WHERE ASSESSMENTGRADINGID = ?),"
					+ " FINALSCORE = TOTALAUTOSCORE+TOTALOVERRIDESCORE"
					+ (((completSubmission != null) && completSubmission.booleanValue()) ? (" ,STATUS = 1, FORGRADE = " + m_sqlService
							.getBooleanConstant(true))
							: "") + " WHERE ASSESSMENTGRADINGID = ?";
			Object[] fields = new Object[3];
			fields[0] = answer.getSubmission().getSubmittedDate();
			fields[1] = answer.getSubmission().getId();
			fields[2] = answer.getSubmission().getId();
			if (!m_sqlService.dbWrite(connection, statement, fields))
			{
				// TODO: better exception
				throw new Exception("submitAnswer: dbWrite Failed");
			}

			// commit
			connection.commit();

			// event track it
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMIT_ANSWER, getSubmissionReference(submission.getId()),
					true));

			// track if we are complete
			if ((completSubmission != null) && completSubmission.booleanValue())
			{
				m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMIT_COMPLETE, getSubmissionReference(submission
						.getId()), true));
			}
		}
		catch (Exception e)
		{
			if (connection != null)
			{
				try
				{
					connection.rollback();
				}
				catch (Exception ee)
				{
					M_log.warn("submitAnswer: rollback: " + ee);
				}
			}
			M_log.warn("submitAnswer: " + e);
		}
		finally
		{
			if (connection != null)
			{
				// restore autocommit, if it was not false
				try
				{
					if (wasCommit) connection.setAutoCommit(wasCommit);
				}
				catch (Exception e)
				{
					M_log.warn("submitAnswer, while setting auto commit: " + e);
				}

				// return the connetion
				m_sqlService.returnConnection(connection);
			}
		}

		// the submission is altered by this - clear the cache (or update)
		unCacheSubmission(submission.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void completeSubmission(Submission s) throws AssessmentPermissionException, AssessmentClosedException,
			SubmissionCompletedException
	{
		// trust only the submission id passed in - get fresh and trusted additional information
		Submission submission = idSubmission(s.getId());
		Assessment assessment = submission.getAssessment();

		// the current time
		Time asOf = m_timeService.newTime();

		// make sure this is an incomplete submission
		if ((submission.getIsComplete() == null) || (submission.getIsComplete().booleanValue()))
		{
			throw new SubmissionCompletedException();
		}

		// check that the current user is the submission user
		if (!submission.getUserId().equals(m_sessionManager.getCurrentSessionUserId()))
		{
			throw new AssessmentPermissionException(m_sessionManager.getCurrentSessionUserId(), SUBMIT_PERMISSION,
					getAssessmentReference(assessment.getId()));
		}

		// check permission - userId must have SUBMIT_PERMISSION in the context of the assessment (use the assessment as ref, not submission)
		secure(submission.getUserId(), SUBMIT_PERMISSION, assessment.getContext(), getAssessmentReference(assessment.getId()));

		// check that the assessment is currently open for submission
		if (!isAssessmentOpen(assessment, asOf)) throw new AssessmentClosedException();

		if (M_log.isDebugEnabled()) M_log.debug("completeSubmission: submission: " + submission.getId());

		Connection connection = null;
		boolean wasCommit = true;
		try
		{
			connection = m_sqlService.borrowConnection();
			wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			String statement = "UPDATE SAM_ASSESSMENTGRADING_T" + " SET SUBMITTEDDATE = ?, STATUS = 1, FORGRADE = "
					+ m_sqlService.getBooleanConstant(true) + " WHERE ASSESSMENTGRADINGID = ?";
			Object fields[] = new Object[2];
			fields[0] = asOf;
			fields[1] = submission.getId();
			if (!m_sqlService.dbWrite(connection, statement, fields))
			{
				throw new Exception("completeSubmission: dbWrite Failed");
			}

			// commit
			connection.commit();

			// update the submission parameter for the caller
			s.setSubmittedDate(asOf);
			s.setStatus(new Integer(1));
			s.setIsComplete(Boolean.TRUE);

			// event track it
			m_eventTrackingService.post(m_eventTrackingService.newEvent(SUBMIT_COMPLETE,
					getSubmissionReference(submission.getId()), true));
		}
		catch (Exception e)
		{
			if (connection != null)
			{
				try
				{
					connection.rollback();
				}
				catch (Exception ee)
				{
					M_log.warn("completeSubmission: rollback: " + ee);
				}
			}
			M_log.warn("completeSubmission: " + e);
		}
		finally
		{
			if (connection != null)
			{
				// restore autocommit, if it was not false
				try
				{
					if (wasCommit) connection.setAutoCommit(wasCommit);
				}
				catch (Exception e)
				{
					M_log.warn("completeSubmission, while setting auto commit: " + e);
				}

				// return the connetion
				m_sqlService.returnConnection(connection);
			}
		}

		// the submission is altered by this - clear the cache (or update?)
		unCacheSubmission(submission.getId());
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
			if (question.getScore() > 0)
			{
				// trueFalse scoring
				if (question.getType() == QuestionType.trueFalse)
				{
					// question score if correct, 0 if not
					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if (questionAnswer != null)
					{
						if (questionAnswer.getIsCorrect())
						{
							score = question.getScore();
						}
					}
				}

				// multipleChoice scoreing
				else if (question.getType() == QuestionType.multipleChoice)
				{
					// question score if correct, 0 if not
					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if (questionAnswer != null)
					{
						if (questionAnswer.getIsCorrect())
						{
							score = question.getScore();
						}
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
						correctScore = question.getScore() / numCorrectAnswers;
						incorrectScore = -1 * correctScore;
					}

					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if (questionAnswer != null)
					{
						if (questionAnswer.getIsCorrect())
						{
							score = correctScore;
						}
						else
						{
							score = incorrectScore;
						}
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
						correctScore = question.getScore() / numCorrectAnswers;
					}

					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if ((questionAnswer != null) && (questionAnswer.getText() != null) && (entry.getAnswerText() != null))
					{
						if (isFillInAnswerCorrect(entry.getAnswerText(), questionAnswer.getText(), question.getCaseSensitive()
								.booleanValue()))
						{
							score = correctScore;
						}
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
						correctScore = question.getScore() / numCorrectAnswers;
					}

					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if ((questionAnswer != null) && (questionAnswer.getText() != null) && (entry.getAnswerText() != null))
					{
						if (isNumericAnswerCorrect(entry.getAnswerText(), questionAnswer.getText()))
						{
							score = correctScore;
						}
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
						correctScore = question.getScore() / numParts;
					}

					// if the entry's position within the entries matches its answer's position within the answers, it is correct
					AssessmentAnswer questionAnswer = entry.getAssessmentAnswer();
					if (questionAnswer != null)
					{
						if (questionAnswer.getIsCorrect().booleanValue())
						{
							score = correctScore;
						}
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
				M_log.warn("scoreAnswers: ran out of entries to clear to get to 0 score: submissionId: "
						+ answer.getSubmission().getId() + " questionId: " + question.getId());
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

						// they need to be the same (i.e. !different) based on our case sensitive (the method takes ignore case, so we reverse)
						if (!StringUtil.different(entry.getAnswerText(), compareEntry.getAnswerText(), !question.getCaseSensitive()
								.booleanValue()))
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
	 * Figure out if a fill-in answer is correct.
	 * 
	 * @param answer
	 *        The given answer.
	 * @param correct
	 *        The correct answer pattern (with option bars and wild cards).
	 * @param caseSensitive
	 *        if we should be case sensitive.
	 * @return true if the answer is correct, false if not
	 */
	protected boolean isFillInAnswerCorrect(String answer, String correct, boolean caseSensitive)
	{
		// get the set of valid answers from the correct answer pattern (each one may have wild cards)
		String[] valid = correct.split("\\|");
		for (String test : valid)
		{
			// prepare the test as a regex, quoting all non-wildcards, changing the wildcard "*" into a regex ".+"
			StringBuffer regex = new StringBuffer();
			String[] parts = test.replaceAll("\\*", "|*|").split("\\|");
			for (String part : parts)
			{
				if ("*".equals(part))
				{
					regex.append(".+");
				}
				else
				{
					regex.append(Pattern.quote(part));
				}
			}
			Pattern p = Pattern.compile(regex.toString(), ((!caseSensitive) ? Pattern.CASE_INSENSITIVE : 0));

			// test
			Matcher m = p.matcher(answer);
			boolean result = m.matches();

			if (result) return true;
		}

		return false;
	}

	/**
	 * Figure out if a fill-in numeric answer is correct.
	 * 
	 * @param answer
	 *        The given answer.
	 * @param correct
	 *        The correct answer pattern (with option bars).
	 * @return true if the answer is correct, false if not
	 */
	protected boolean isNumericAnswerCorrect(String answer, String correct)
	{
		try
		{
			// allow dot or comma for decimal point
			answer = answer.replace(',', '.');
			correct = correct.replace(',', '.');

			// answer needs to become a float (allow dot or comma for decimal point)
			float answerValue = Float.parseFloat(answer);

			// form the range of correct answers
			Float[] range = new Float[2];

			// if there's a bar in the correct pattern, split and use the first two as the range
			if (correct.indexOf("|") != -1)
			{
				String[] parts = correct.split("\\|");
				range[0] = Float.parseFloat(parts[0]);
				range[1] = Float.parseFloat(parts[1]);

				// make sure [0] <= [1]
				if (range[0].floatValue() > range[1].floatValue())
				{
					Float hold = range[0];
					range[0] = range[1];
					range[1] = hold;
				}
			}

			// otherwise use the single value for both sides of the range
			else
			{
				range[0] = range[1] = Float.parseFloat(correct);
			}

			// test
			if ((answerValue >= range[0].floatValue()) && (answerValue <= range[1].floatValue()))
			{
				return true;
			}
		}
		catch (NumberFormatException e)
		{
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
	 * @return
	 */
	protected boolean isAssessmentOpen(Assessment a, Time asOf)
	{
		// if we have a release date and we are not there yet
		if ((a.getReleaseDate() != null) && (asOf.before(a.getReleaseDate()))) return false;

		// if we have a retract date and we are past it
		if ((a.getRetractDate() != null) && (!asOf.before(a.getRetractDate()))) return false;

		// if we have a due date, are past it, and not accepting late submissions
		if ((a.getDueDate() != null) && (!asOf.before(a.getDueDate()))
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
				+ " WHERE AG.PUBLISHEDASSESSMENTID = ? AND AG.AGENTID = ? AND AG.FORGRADE = "
				+ m_sqlService.getBooleanConstant(false);
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
}
