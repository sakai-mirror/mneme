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
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Ent;
import org.muse.mneme.api.ImportService;
import org.muse.mneme.api.Pool;
import org.muse.mneme.api.PoolService;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.QuestionService;
import org.muse.mneme.api.SecurityService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.StringUtil;

/**
 * <p>
 * ImportServiceImpl implements ImportService
 * </p>
 */
public class ImportServiceImpl implements ImportService
{
	public class SamigoQuestion
	{
		String answerMatchText;

		Integer answerSeq;

		Boolean caseSensitive;

		Boolean correct;

		String correctFeedback;

		Boolean exclusive;

		String generalFeedback;

		String incorrectFeedback;

		String instruction;

		String itemId;

		Question question;

		String questionChoiceText;

		Boolean randomize;

		Boolean reason;

		Float score;

		Integer type;
	}

	/** Our logger. */
	private static Log M_log = LogFactory.getLog(ImportServiceImpl.class);

	/** Messages bundle name. */
	protected String bundle = null;

	/** Dependency: EventTrackingService */
	protected EventTrackingService eventTrackingService = null;

	/** Messages. */
	protected transient InternationalizedMessages messages = null;

	/** Dependency: PoolService */
	protected PoolService poolService = null;

	/** Dependency: QuestionService */
	protected QuestionService questionService = null;

	/** Dependency: SecurityService */
	protected SecurityService securityService = null;

	/** Dependency: SessionManager */
	protected SessionManager sessionManager = null;

	/** Dependency: SqlService */
	protected SqlService sqlService = null;

	/** Dependency: ThreadLocalManager. */
	protected ThreadLocalManager threadLocalManager = null;

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
	public List<Ent> getPools(String userId)
	{
		if (userId == null) userId = sessionManager.getCurrentSessionUserId();

		List<Ent> rv = readSamigoPools(userId);

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void importPool(String id, String context)
	{
		// TODO: create the pool
		
		// TODO: import the questions

		M_log.info("importPool: id: " + id + " context: " + context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		M_log.info("init()");
	}

	/**
	 * Set the message bundle.
	 * 
	 * @param bundle
	 *        The message bundle.
	 */
	public void setBundle(String name)
	{
		this.bundle = name;
	}

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		eventTrackingService = service;
	}

	/**
	 * Set the PoolService.
	 * 
	 * @param service
	 *        the PoolService.
	 */
	public void setPoolService(PoolService service)
	{
		this.poolService = service;
	}

	/**
	 * Dependency: QuestionService.
	 * 
	 * @param service
	 *        The QuestionService.
	 */
	public void setQuestionService(QuestionService service)
	{
		this.questionService = service;
	}

	/**
	 * Dependency: SecurityService.
	 * 
	 * @param service
	 *        The SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		securityService = service;
	}

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param service
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager service)
	{
		sessionManager = service;
	}

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		sqlService = service;
	}

	/**
	 * Dependency: ThreadLocalManager.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setThreadLocalManager(ThreadLocalManager service)
	{
		threadLocalManager = service;
	}

	/**
	 * Read the Samigo pools for this user id.
	 * 
	 * @param userId
	 *        The user id.
	 * @return The list of Ents describing the pools for this user id.
	 */
	protected List<Ent> readSamigoPools(String userId)
	{
		final List<Ent> rv = new ArrayList<Ent>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT P.QUESTIONPOOLID, P.TITLE FROM SAM_QUESTIONPOOL_T P WHERE P.OWNERID=?");

		Object[] fields = new Object[1];
		fields[0] = StringUtil.trimToNull(userId);

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					String id = SqlHelper.readString(result, 1);
					String title = SqlHelper.readString(result, 2);

					Ent ent = new EntImpl(id, title);
					rv.add(ent);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSubmissions(submission): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Read the list of item (question) ids in a Samigo pool.
	 * 
	 * @param poolId
	 *        The Samigo pool id.
	 * @return A list of the item ids in the pool.
	 */
	protected void importSamigoQuestions(String poolId)
	{
		final Pool pool = this.poolService.getPool(poolId);
		final QuestionService qs = this.questionService;
		final List<SamigoQuestion> sqs = new ArrayList<SamigoQuestion>();

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(" P.ITEMID, I.TYPEID, I.SCORE, I.HASRATIONALE, I.INSTRUCTION, T.TEXT,");
		sql.append(" A.TEXT, A.SEQUENCE, A.ISCORRECT,");
		sql.append(" F1.TEXT, F2.TEXT, F3.TEXT,");
		sql.append(" M1.ENTRY, M2.ENTRY, M3.ENTRY");
		sql.append(" FROM SAM_QUESTIONPOOLITEM_T P");
		sql.append(" JOIN SAM_ITEM_T I ON P.ITEMID=I.ITEMID");
		sql.append(" JOIN SAM_ITEMTEXT_T T ON P.ITEMID=T.ITEMID");
		sql.append(" LEFT OUTER JOIN SAM_ANSWER_T A ON P.ITEMID=A.ITEMID AND ((I.TYPEID != 9) OR (A.ISCORRECT='1' AND A.ITEMTEXTID=T.ITEMTEXTID))");
		sql.append(" LEFT OUTER JOIN SAM_ITEMFEEDBACK_T F1 ON P.ITEMID=F1.ITEMID AND F1.TYPEID='INCORRECT FEEDBACK'");
		sql.append(" LEFT OUTER JOIN SAM_ITEMFEEDBACK_T F2 ON P.ITEMID=F2.ITEMID AND F2.TYPEID='CORRECT FEEDBACK'");
		sql.append(" LEFT OUTER JOIN SAM_ITEMFEEDBACK_T F3 ON P.ITEMID=F3.ITEMID AND F3.TYPEID='GENERAL FEEDBACK'");
		sql.append(" LEFT OUTER JOIN SAM_ITEMMETADATA_T M1 ON P.ITEMID=M1.ITEMID AND M1.LABEL='CASE_SENSITIVE'");
		sql.append(" LEFT OUTER JOIN SAM_ITEMMETADATA_T M2 ON P.ITEMID=M2.ITEMID AND M2.LABEL='MUTUALLY_EXCLUSIVE'");
		sql.append(" LEFT OUTER JOIN SAM_ITEMMETADATA_T M3 ON P.ITEMID=M3.ITEMID AND M3.LABEL='RANDOMIZE'");
		sql.append(" WHERE P.QUESTIONPOOLID=?");
		sql.append(" ORDER BY P.ITEMID ASC, A.SEQUENCE ASC ");

		Object[] fields = new Object[1];
		fields[0] = Long.valueOf(poolId);

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					SamigoQuestion sq = new SamigoQuestion();
					sqs.add(sq);

					sq.itemId = SqlHelper.readId(result, 1);
					sq.type = SqlHelper.readInteger(result, 2);
					sq.score = SqlHelper.readFloat(result, 3);
					sq.reason = SqlHelper.readBoolean(result, 4);
					sq.instruction = SqlHelper.readString(result, 5);
					sq.questionChoiceText = SqlHelper.readString(result, 6);
					sq.answerMatchText = SqlHelper.readString(result, 7);
					sq.answerSeq = SqlHelper.readInteger(result, 8);
					sq.correct = SqlHelper.readBoolean(result, 9);
					sq.incorrectFeedback = SqlHelper.readString(result, 10);
					sq.correctFeedback = SqlHelper.readString(result, 11);
					sq.generalFeedback = SqlHelper.readString(result, 12);
					sq.caseSensitive = SqlHelper.readBoolean(result, 13);
					sq.exclusive = SqlHelper.readBoolean(result, 14);
					sq.randomize = SqlHelper.readBoolean(result, 15);
					sq.question = null;

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("readSamigoQuestions: " + e);
					return null;
				}
			}
		});

		// start building up questions
		int lastItem = -1;
		for (Iterator i = sqs.iterator(); i.hasNext();)
		{
			SamigoQuestion sq = (SamigoQuestion) i.next();

			// make a question
			String mnemeType = null;
			switch (sq.type)
			{
				case 1:
				{
					mnemeType = "mneme:MultipleChoice";
					break;
				}
				case 3:
				{
					mnemeType = "mneme:LikertScale";
					break;
				}
				case 4:
				{
					mnemeType = "mneme:TrueFalse";
					break;
				}
				case 5:
				{
					mnemeType = "mneme:Essay";
					break;
				}
				case 6:
				{
					// upload
					mnemeType = "mneme:Essay";
					break;
				}
				case 8:
				{
					// text
					mnemeType = "mneme:FillBlanks";
					break;
				}
				case 9:
				{
					mnemeType = "mneme:Match";
					break;
				}
				case 11:
				{
					// numeric
					mnemeType = "mneme:FillBlanks";
					break;
				}
			}

			try
			{
				sq.question = qs.newQuestion(pool, mnemeType);
			}
			catch (AssessmentPermissionException e)
			{
			}

			if (sq.reason != null) sq.question.setExplainReason(sq.reason);

			if (sq.generalFeedback != null)
			{
				sq.question.setFeedback(sq.generalFeedback);
			}
			else if (sq.correctFeedback != null)
			{
				sq.question.setFeedback(sq.correctFeedback);
			}
			else if (sq.incorrectFeedback != null)
			{
				sq.question.setFeedback(sq.incorrectFeedback);
			}

			// type specific
			switch (sq.type)
			{
				case 1:
				{
					// mc
					if (sq.questionChoiceText != null) sq.question.getPresentation().setText(sq.questionChoiceText);

					MultipleChoiceQuestionImpl mc = (MultipleChoiceQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if (sq.randomize != null) mc.setShuffleChoices(sq.randomize.toString());

					// TODO: set options from this and further entries
					break;
				}
				case 3:
				{
					// Likert
					if (sq.questionChoiceText != null) sq.question.getPresentation().setText(sq.questionChoiceText);

					// TODO: what scale? Look at this and future entries...
					break;
				}
				case 4:
				{
					// tf
					if (sq.questionChoiceText != null) sq.question.getPresentation().setText(sq.questionChoiceText);

					// this and the next will have the "true" "false" in answer text, and one is correct
					TrueFalseQuestionImpl tf = (TrueFalseQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if ((sq.correct != null) && (sq.correct.booleanValue())) tf.setCorrectAnswer(sq.answerMatchText);

					// TODO: check the next one too
					break;
				}
				case 5:
				{
					// essay
					if (sq.questionChoiceText != null) sq.question.getPresentation().setText(sq.questionChoiceText);

					// model answer is in answer text
					EssayQuestionImpl e = (EssayQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if (sq.answerMatchText != null) e.setModelAnswer(sq.answerMatchText);
					e.setSubmissionType(EssayQuestionImpl.SubmissionType.inline);

					break;
				}
				case 6:
				{
					// upload
					if (sq.questionChoiceText != null) sq.question.getPresentation().setText(sq.questionChoiceText);

					// model answer is in answer text
					EssayQuestionImpl e = (EssayQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if (sq.answerMatchText != null) e.setModelAnswer(sq.answerMatchText);
					e.setSubmissionType(EssayQuestionImpl.SubmissionType.attachments);

					break;
				}
				case 8:
				{
					// fillin-text
					FillBlanksQuestionImpl fb = (FillBlanksQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if (sq.questionChoiceText != null) fb.setText(sq.questionChoiceText);
					fb.setResponseTextual("true");
					// TODO: the fill in values need to be harvested from the following entries

					if (sq.caseSensitive != null) fb.setCaseSensitive(sq.caseSensitive.toString());

					break;
				}
				case 9:
				{
					// match
					MatchQuestionImpl m = (MatchQuestionImpl) (sq.question.getTypeSpecificQuestion());

					// TODO:
					break;
				}
				case 11:
				{
					// fillin numeric
					FillBlanksQuestionImpl fb = (FillBlanksQuestionImpl) (sq.question.getTypeSpecificQuestion());
					if (sq.questionChoiceText != null) fb.setText(sq.questionChoiceText);
					fb.setResponseTextual("false");
					// TODO: the fill in values need to be harvested from the following entries

					if (sq.caseSensitive != null) fb.setCaseSensitive(sq.caseSensitive.toString());

					break;
				}
			}
		}

		// TODO: consolidate q? save q?

		// set the pool's points to the average
		float total = 0f;
		int count = 0;
		for (SamigoQuestion sq : sqs)
		{
			if (sq.score != null)
			{
				count++;
				total += sq.score.floatValue();
			}
		}
		if (count > 0)
		{
			Float average = Float.valueOf(total / count);
			pool.setPoints(average);
			try
			{
				this.poolService.savePool(pool);
			}
			catch (AssessmentPermissionException e)
			{
			}
		}
	}
}
