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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;

/**
 * <p>
 * ImportServiceImpl implements ImportService
 * </p>
 */
public class ImportServiceImpl implements ImportService
{
	public class PoolInfo
	{
		String description = null;

		String title = null;
	}

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
	public void importPool(String id, String context) throws AssessmentPermissionException
	{
		// create the pool
		Pool pool = createPool(id, context);

		if (pool != null)
		{
			// import the questions
			importSamigoQuestions(id, pool);
		}
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// messages
		if (this.bundle != null) this.messages = new ResourceLoader(this.bundle);

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
	 * Add a formatted date to a source string, using a message selector.
	 * 
	 * @param selector
	 *        The message selector.
	 * @param source
	 *        The original string.
	 * @param date
	 *        The date to format.
	 * @return The source and date passed throught the selector message.
	 */
	protected String addDate(String selector, String source, Date date)
	{
		// format the date
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		String fmt = format.format(date);

		// the args
		Object[] args = new Object[2];
		args[0] = source;
		args[1] = fmt;

		// format the works
		String rv = this.messages.getFormattedMessage(selector, args);

		return rv;
	}

	/**
	 * Create an essay question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @param upload
	 *        true for an upload type, false for inline.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createEssay(SamigoQuestion[] these, Pool pool, boolean upload) throws AssessmentPermissionException
	{
		// validate: fist questionChoiceText for the question text not null
		boolean valid = (these[0].questionChoiceText != null);

		// there must be only one entry
		if (valid)
		{
			valid = (these.length == 1);
		}

		if (!valid)
		{
			M_log.info("createEssay: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:Essay");
		EssayQuestionImpl e = (EssayQuestionImpl) (question.getTypeSpecificQuestion());

		// set the text
		question.getPresentation().setText(these[0].questionChoiceText);

		// model answer
		if (these[0].answerMatchText != null)
		{
			e.setModelAnswer(these[0].answerMatchText);
		}

		// type
		e.setSubmissionType(upload ? EssayQuestionImpl.SubmissionType.attachments : EssayQuestionImpl.SubmissionType.inline);

		return question;
	}

	/**
	 * Create a fillin question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @param text
	 *        true if text, false if numeric.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createFillin(SamigoQuestion[] these, Pool pool, boolean text) throws AssessmentPermissionException
	{
		// validate: fist questionChoiceText for the question text not null
		boolean valid = (these[0].questionChoiceText != null);

		// answerMatchText from all for the choices not null
		if (valid)
		{
			for (int index = 0; index < these.length; index++)
			{
				if (these[index].answerMatchText == null)
				{
					valid = false;
					break;
				}
			}
		}

		if (!valid)
		{
			M_log.info("createFillin: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:FillBlanks");
		FillBlanksQuestionImpl f = (FillBlanksQuestionImpl) (question.getTypeSpecificQuestion());

		// detect these[0].exclusive and translate to our "any order"
		f.setAnyOrder(Boolean.FALSE.toString());
		if ((these[0].exclusive != null) && (these[0].exclusive.booleanValue()))
		{
			// if we find that all the fill-in correct patterns are the same,
			// and there are is one for each answer (comma separated),
			// spread them out, one per fill-in, and set any-order
			boolean mutualExclusive = true;
			for (int index = 1; index < these.length; index++)
			{
				if (!these[index].answerMatchText.equals(these[0].answerMatchText))
				{
					mutualExclusive = false;
					break;
				}
			}

			if (mutualExclusive)
			{
				String[] parts = these[0].answerMatchText.split("\\|");
				if ((parts != null) && (parts.length == these.length))
				{
					for (int index = 0; index < these.length; index++)
					{
						these[index].answerMatchText = parts[index];
					}

					f.setAnyOrder(Boolean.TRUE.toString());
				}
			}
		}

		// case sensitive
		if (these[0].caseSensitive != null) f.setCaseSensitive(these[0].caseSensitive.toString());

		// text or numeric
		f.setResponseTextual(Boolean.toString(text));

		// recreate the text, fillin in the "{}" with these answerMatchText

		String questionText = these[0].questionChoiceText;
		for (int index = 0; index < these.length; index++)
		{
			questionText = questionText.replaceFirst("\\{\\}", "{" + these[index].answerMatchText + "}");
		}

		// set the text
		f.setText(questionText);

		return question;
	}

	/**
	 * Create a Likert scale question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createLikert(SamigoQuestion[] these, Pool pool) throws AssessmentPermissionException
	{
		// validate: fist questionChoiceText for the question text not null
		boolean valid = (these[0].questionChoiceText != null);

		// recognize a scale
		String scale = null;
		// "0" for our 5 point "strongly-agree"
		// "1" for our 4 point "excellent"
		// "2" for our 3 point "above-average"
		// "3" for our 2 point "yes"
		// "4" for our 5 point "5"
		// "5" for our 2 point "rocks"

		// 3 choices is below/average/above or disagree/undecided/agree
		if (these.length == 3)
		{
			if ("Below Average".equals(these[0].answerMatchText))
			{
				scale = "2";
			}
			else
			{
				scale = "0";
			}
		}

		// 2 is yes/no, or agree / disagree
		else if (these.length == 2)
		{
			if ("No".equals(these[0].answerMatchText))
			{
				scale = "3";
			}

			else
			{
				scale = "0";
			}
		}

		// 5 is strongly agree -> strongly disagree or unacceptable/below average/average/above average/excelent
		// or 1..5
		else if (these.length == 5)
		{
			if ("1".equals(these[0].answerMatchText))
			{
				scale = "4";
			}
			else if ("Strongly Disagree".equals(these[0].answerMatchText))
			{
				scale = "0";
			}
			else
			{
				scale = "1";
			}
		}

		// 10 is 1..10
		else if (these.length == 10)
		{
			scale = "4";
		}

		if (scale == null)
		{
			valid = false;
		}

		if (!valid)
		{
			M_log.info("createLikert: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:LikertScale");
		LikertScaleQuestionImpl l = (LikertScaleQuestionImpl) (question.getTypeSpecificQuestion());

		// set the text
		question.getPresentation().setText(these[0].questionChoiceText);

		// set the scale
		l.setScale(scale);

		return question;
	}

	/**
	 * Create a match question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createMatch(SamigoQuestion[] these, Pool pool) throws AssessmentPermissionException
	{
		// validate: fist instruction for the question text not null
		boolean valid = (these[0].instruction != null);

		// answerMatchText and questionChoiceText from all for the match and choices not null
		if (valid)
		{
			for (int index = 0; index < these.length; index++)
			{
				if ((these[index].answerMatchText == null) || (these[index].questionChoiceText == null))
				{
					valid = false;
					break;
				}
			}
		}

		if (!valid)
		{
			M_log.info("createMatch: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:Match");
		MatchQuestionImpl m = (MatchQuestionImpl) (question.getTypeSpecificQuestion());

		// set the text
		question.getPresentation().setText(these[0].instruction);

		// set the # pairs
		m.consolidate("INIT:" + these.length);

		// set the pair values
		List<MatchQuestionImpl.MatchQuestionPair> pairs = m.getPairs();
		for (int index = 0; index < these.length; index++)
		{
			pairs.get(index).setChoice(these[index].questionChoiceText);
			pairs.get(index).setMatch(these[index].answerMatchText);
		}

		return question;
	}

	/**
	 * Create a multiple choice question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @param multiAllowed
	 *        true if we can have multiple answers, false if not.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createMc(SamigoQuestion[] these, Pool pool, boolean multiAllowed) throws AssessmentPermissionException
	{
		// validate: fist questionChoiceText for the question text not null
		boolean valid = (these[0].questionChoiceText != null);

		// Note: answerMatchText may actually be null
		// we must have one or more marked correct
		boolean multiCorrect = false;
		if (valid)
		{
			boolean seenCorrect = false;
			for (int index = 0; index < these.length; index++)
			{
				if (these[index].answerMatchText != null)
				{
					if ((these[index].correct != null) && (these[index].correct.booleanValue()))
					{
						if (seenCorrect) multiCorrect = true;
						seenCorrect = true;
					}
				}
			}
			if (!seenCorrect)
			{
				valid = false;
			}
		}

		if (valid && multiCorrect && !multiAllowed)
		{
			valid = false;
		}

		if (!valid)
		{
			M_log.info("createMc: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:MultipleChoice");
		MultipleChoiceQuestionImpl mc = (MultipleChoiceQuestionImpl) (question.getTypeSpecificQuestion());

		// set the text
		question.getPresentation().setText(these[0].questionChoiceText);

		// randomize
		if (these[0].randomize != null) mc.setShuffleChoices(these[0].randomize.toString());

		// single / multiple select
		mc.setSingleCorrect(Boolean.toString(!multiCorrect));

		// set the choices
		List<String> choices = new ArrayList<String>();
		for (int index = 0; index < these.length; index++)
		{
			if (these[index].answerMatchText != null)
			{
				choices.add(these[index].answerMatchText);
			}
		}
		mc.setAnswerChoices(choices);

		// corrects
		Set<Integer> correctAnswers = new HashSet<Integer>();
		List<MultipleChoiceQuestionImpl.MultipleChoiceQuestionChoice> choicesAuthored = mc.getChoicesAsAuthored();
		int authoredIndex = 0;
		for (int index = 0; index < these.length; index++)
		{
			if (these[index].answerMatchText != null)
			{
				if ((these[index].correct != null) && (these[index].correct.booleanValue()))
				{
					correctAnswers.add(Integer.valueOf(choicesAuthored.get(authoredIndex).getId()));
				}
				authoredIndex++;
			}
		}
		mc.setCorrectAnswerSet(correctAnswers);

		return question;
	}

	/**
	 * Create a pool in Mneme from Samigo pool information.
	 * 
	 * @param poolId
	 *        The Samigo pool id.
	 * @return The pool, or null if it was not created.
	 */
	protected Pool createPool(String poolId, final String context) throws AssessmentPermissionException
	{
		// read the details
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT P.TITLE, P.DESCRIPTION FROM SAM_QUESTIONPOOL_T P WHERE P.QUESTIONPOOLID=?");

		Object[] fields = new Object[1];
		fields[0] = StringUtil.trimToNull(poolId);
		final PoolInfo info = new PoolInfo();

		this.sqlService.dbRead(sql.toString(), fields, new SqlReader()
		{
			public Object readSqlResultRecord(ResultSet result)
			{
				try
				{
					info.title = SqlHelper.readString(result, 1);
					info.description = SqlHelper.readString(result, 2);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("createPool: reading pool details: " + e);
					return null;
				}
			}
		});

		if (info.title != null)
		{
			Pool pool = this.poolService.newPool(context);
			pool.setTitle(addDate("import-text", info.title, new Date()));
			pool.setDescription(info.description);
			this.poolService.savePool(pool);

			return pool;
		}

		return null;
	}

	/**
	 * Create a true false question from Samigo data.
	 * 
	 * @param these
	 *        The Samigo data entries.
	 * @param pool
	 *        The pool for the question.
	 * @return The question, or null if it was not made
	 * @throws AssessmentPermissionException
	 */
	protected Question createTf(SamigoQuestion[] these, Pool pool) throws AssessmentPermissionException
	{
		// validate: fist questionChoiceText for the question text not null
		boolean valid = (these[0].questionChoiceText != null);

		// there must be two of these
		if (valid)
		{
			valid = (these.length == 2);
		}

		// they must have "true" and "false" marked in their answerMatchText
		if (valid)
		{
			valid = ("true".equals(these[0].answerMatchText) && "false".equals(these[1].answerMatchText));
		}

		// one of these must be marked correct
		if (valid)
		{
			int count = 0;
			if ((these[0].correct != null) && (these[0].correct.booleanValue())) count++;
			if ((these[1].correct != null) && (these[1].correct.booleanValue())) count++;
			valid = (count == 1);
		}

		if (!valid)
		{
			M_log.info("createTf: invalid samigo question: " + these[0].itemId);
			return null;
		}

		// create the question
		Question question = this.questionService.newQuestion(pool, "mneme:TrueFalse");
		TrueFalseQuestionImpl tf = (TrueFalseQuestionImpl) (question.getTypeSpecificQuestion());

		// set the text
		question.getPresentation().setText(these[0].questionChoiceText);

		// the correct answer
		tf.setCorrectAnswer((((these[0].correct != null) && (these[0].correct.booleanValue()))) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());

		return question;
	}

	/**
	 * Read the list of item (question) ids in a Samigo pool.
	 * 
	 * @param poolId
	 *        The Samigo pool id.
	 * @param pool
	 *        The pool for the questions.
	 * @return A list of the item ids in the pool.
	 */
	protected void importSamigoQuestions(String poolId, Pool pool) throws AssessmentPermissionException
	{
		final QuestionService qs = this.questionService;

		// read the questions
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
					sq.reason = SqlHelper.readBitBoolean(result, 4);
					sq.instruction = SqlHelper.readString(result, 5);
					sq.questionChoiceText = SqlHelper.readString(result, 6);
					sq.answerMatchText = SqlHelper.readString(result, 7);
					sq.answerSeq = SqlHelper.readInteger(result, 8);
					sq.correct = SqlHelper.readBitBoolean(result, 9);
					sq.incorrectFeedback = SqlHelper.readString(result, 10);
					sq.correctFeedback = SqlHelper.readString(result, 11);
					sq.generalFeedback = SqlHelper.readString(result, 12);
					sq.caseSensitive = SqlHelper.readBoolean(result, 13);
					sq.exclusive = SqlHelper.readBoolean(result, 14);
					sq.randomize = SqlHelper.readBoolean(result, 15);

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("importSamigoQuestions: " + e);
					return null;
				}
			}
		});

		// build the questions
		float total = 0f;
		int count = 0;
		for (int i = 0; i < sqs.size(); i++)
		{
			SamigoQuestion sq = sqs.get(i);

			// accumulate the score for an average for the pool
			if (sq.score != null)
			{
				count++;
				total += sq.score.floatValue();
			}

			// get the rest
			int next = i + 1;
			for (; next < sqs.size(); next++)
			{
				SamigoQuestion sqNext = sqs.get(next);
				if (!sqNext.itemId.equals(sq.itemId))
				{
					next--;
					break;
				}
			}
			if (next == sqs.size()) next--;

			// we have from i .. next, inclusive
			SamigoQuestion[] these = new SamigoQuestion[(next - i) + 1];
			for (int index = i; index <= next; index++)
			{
				these[index - i] = sqs.get(index);
			}
			i = next;

			Question question = null;
			switch (sq.type)
			{
				case 1:
				{
					// single correct
					question = createMc(these, pool, false);

					break;
				}
				case 2:
				{
					// multi correct
					question = createMc(these, pool, true);

					break;
				}
				case 3:
				{
					// mnemeType = "mneme:LikertScale";
					question = createLikert(these, pool);
					break;
				}
				case 4:
				{
					question = createTf(these, pool);
					break;
				}
				case 5:
				{
					// inline essay
					question = createEssay(these, pool, false);
					break;
				}
				case 6:
				{
					// upload essay
					question = createEssay(these, pool, true);
					break;
				}
				case 8:
				{
					// text
					question = createFillin(these, pool, true);
					break;
				}
				case 9:
				{
					question = createMatch(these, pool);
					break;
				}
				case 11:
				{
					// numeric
					question = createFillin(these, pool, false);
					break;
				}
			}

			if (question != null)
			{
				// reason
				if (sq.reason != null) question.setExplainReason(sq.reason);

				// feedback
				if (sq.generalFeedback != null)
				{
					question.setFeedback(sq.generalFeedback);
				}
				else if (sq.correctFeedback != null)
				{
					question.setFeedback(sq.correctFeedback);
				}
				else if (sq.incorrectFeedback != null)
				{
					question.setFeedback(sq.incorrectFeedback);
				}

				// save
				question.getTypeSpecificQuestion().consolidate("");
				this.questionService.saveQuestion(question);
			}
		}

		// set the pool's points to the average
		if (count > 0)
		{
			Float average = Float.valueOf(total / count);
			pool.setPoints(average);
			this.poolService.savePool(pool);
		}
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
}
