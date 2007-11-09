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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muse.ambrosia.api.UiService;
import org.muse.ambrosia.api.Value;
import org.muse.mneme.api.Answer;
import org.muse.mneme.api.Question;
import org.muse.mneme.api.TypeSpecificAnswer;
import org.muse.mneme.impl.MatchQuestionImpl.MatchQuestionPair;

/**
 * MatchAnswerImpl handles answers for the match question type.
 */
public class MatchAnswerImpl implements TypeSpecificAnswer
{
	/** The answer this is a helper for. */
	protected transient Answer answer = null;

	/** The answers: a map between a pair id and a choice id (the choice is stored in the Value). */
	protected Map<String, Value> answerData = new HashMap<String, Value>();

	/** The answer before possible modification. */
	protected Map<String, Value> priorAnswer = new HashMap<String, Value>();

	/** Dependency: The UI service (Ambrosia). */
	protected transient UiService uiService = null;

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 * @param other
	 *        The other to copy.
	 */
	public MatchAnswerImpl(Answer answer, MatchAnswerImpl other)
	{
		this.answer = answer;
		this.answerData = new HashMap<String, Value>(other.answerData.size());
		this.priorAnswer = new HashMap<String, Value>(other.answerData.size());
		for (Map.Entry entry : other.answerData.entrySet())
		{
			Value v = this.uiService.newValue();
			v.setValue(((Value) entry.getValue()).getValue());
			this.answerData.put((String) entry.getKey(), v);

			v = this.uiService.newValue();
			v.setValue(((Value) entry.getValue()).getValue());
			this.priorAnswer.put((String) entry.getKey(), v);
		}

		this.uiService = other.uiService;
	}

	/**
	 * Construct.
	 * 
	 * @param answer
	 *        The answer this is a helper for.
	 */
	public MatchAnswerImpl(Answer answer, UiService uiService)
	{
		this.answer = answer;
		this.uiService = uiService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearIsChanged()
	{
		this.priorAnswer = new HashMap<String, Value>(this.answerData);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object clone(Answer answer)
	{
		try
		{
			// get an exact, bit-by-bit copy
			Object rv = super.clone();

			// deep copy
			if (this.answerData != null)
			{
				((MatchAnswerImpl) rv).answerData = new HashMap<String, Value>(this.answerData.size());
				((MatchAnswerImpl) rv).priorAnswer = new HashMap<String, Value>(this.answerData.size());
				for (Map.Entry entry : this.answerData.entrySet())
				{
					Value v = this.uiService.newValue();
					v.setValue(((Value) entry.getValue()).getValue());
					((MatchAnswerImpl) rv).answerData.put((String) entry.getKey(), v);

					v = this.uiService.newValue();
					v.setValue(((Value) entry.getValue()).getValue());
					((MatchAnswerImpl) rv).priorAnswer.put((String) entry.getKey(), v);
				}
			}

			((MatchAnswerImpl) rv).answer = answer;

			return rv;
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	public Map<String, Value> getAnswer()
	{
		if (this.answerData.isEmpty())
		{
			// populate with null values for each pair
			List<MatchQuestionPair> pairs = ((MatchQuestionImpl) this.answer.getQuestion().getTypeSpecificQuestion()).getPairs();
			for (MatchQuestionPair pair : pairs)
			{
				this.answerData.put(pair.getId(), this.uiService.newValue());
			}
		}

		return this.answerData;
	}

	/**
	 * {@inheritDoc}
	 */
	public Float getAutoScore()
	{
		Question question = this.answer.getQuestion();

		// the defined pairs
		List<MatchQuestionPair> pairs = ((MatchQuestionImpl) question.getTypeSpecificQuestion()).getPairs();

		// each correct / incorrect uses a portion of the total points
		float partial = (pairs.size() > 0) ? question.getPool().getPoints() / pairs.size() : 0f;

		float total = 0f;
		for (MatchQuestionPair pair : pairs)
		{
			// get the answer for this pair
			Value selection = this.answerData.get(pair.getId());
			if (selection != null)
			{
				String value = selection.getValue();
				if ((value != null) && value.equals(pair.getCorrectChoiceId()))
				{
					total += partial;
				}
				else
				{
					total -= partial;
				}
			}
		}

		// floor at 0
		if (total < 0f) total = 0f;

		return total;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsAnswered()
	{
		for (Object value : this.answerData.values())
		{
			if (((Value) value).getValue() != null)
			{
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean getIsChanged()
	{
		if (this.answerData.size() != this.priorAnswer.size()) return Boolean.TRUE;

		for (Map.Entry entry : this.answerData.entrySet())
		{
			if (Different.different(entry.getValue(), this.priorAnswer.get(entry.getKey()))) return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}
}
