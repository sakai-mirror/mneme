/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2010 Etudes, Inc.
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

package org.etudes.mneme.impl;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.etudes.mneme.api.Answer;
import org.etudes.mneme.api.Assessment;
import org.etudes.mneme.api.Part;
import org.etudes.mneme.api.Question;
import org.etudes.mneme.api.Submission;
import org.etudes.mneme.api.TypeSpecificAnswer;
import org.sakaiproject.i18n.InternationalizedMessages;
import org.sakaiproject.util.FormattedText;

/**
 * ResultsFormatterImpl ...
 */
public class ResultsFormatterImpl
{
	/** Matches \r */
	private static Pattern M_patternCR = Pattern.compile("\\r");

	private static Pattern M_patternNBSP = Pattern.compile("&nbsp;");

	/** Messages. */
	protected transient InternationalizedMessages messages = null;

	public ResultsFormatterImpl(InternationalizedMessages messages)
	{
		this.messages = messages;
	}

	/**
	 * Format and send a results email for this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	public String formatResults(Assessment assessment, List<Submission> submissions)
	{
		StringBuilder content = new StringBuilder();

		// the assessment title, points
		content.append("<p>");
		content.append(format("results-assessment-title", assessment.getTitle()));
		if (assessment.getHasPoints())
		{
			content.append("<br />");
			content.append(format("results-assessment-total-points", assessment.getParts().getTotalPoints()));
		}
		content.append("<br />\n");

		// date prepared
		content.append(format("results-date", formatDate(new Date())));
		content.append("</p>\n");

		// for each part
		for (Part part : assessment.getParts().getParts())
		{
			// part title, count, instructions
			content.append("<p>");
			if (part.getTitle() != null)
			{
				content.append(format("results-part-title", part.getOrdering().getPosition(), assessment.getParts().getSize(), part.getTitle()));
			}
			else
			{
				content.append(format("results-part-title-no-title", part.getOrdering().getPosition(), assessment.getParts().getSize()));
			}

			if (assessment.getParts().getShowPresentation())
			{
				content.append("<br />");
				content.append(stripHtml(part.getPresentation().getText()));
			}
			content.append("</p>\n");

			// for each question
			for (Question question : part.getQuestionsUsed())
			{
				Integer count = countQuestionSeen(question, submissions);

				// just skip questions not seen at all
				if (count == 0) continue;

				String answersText = (count == 1) ? format("results-answer") : format("results-answers", count);
				String pointsText = (question.getPoints() > 0) ? format("results-worth", question.getPoints()) : "";

				content
						.append("<div style=\"background: #EEEEEE;border: 1px solid #bbb;padding-left: 0.5em;font-weight: bold;line-height: 1.5em;\">");
				content.append(format("results-question-header", pointsText, answersText));

				content.append("</div>\n");

				// question text
				content.append("<p>");
				if (question.getTypeSpecificQuestion().getUseQuestionPresentation())
				{
					content.append(stripHtml(question.getPresentation().getText()));
				}
				else
				{
					content.append(question.getDescription());
				}
				content.append("</p>");

				// summary of submissions for this question

				// for t/f, likert, mc - for each option, show correct markings, text, # of submissions picking this one, and %
				if (question.getTypeSpecificQuestion() instanceof TrueFalseQuestionImpl)
				{
					TrueFalseQuestionImpl tsq = (TrueFalseQuestionImpl) question.getTypeSpecificQuestion();
					content.append("<table>\n");
					for (TrueFalseQuestionImpl.TrueFalseQuestionChoice choice : tsq.getChoices())
					{
						content.append("<tr><td>");
						content.append(stripHtml(choice.getText()));
						content.append("</td><td>");
						content.append(formatCountPercent(question, submissions, choice.getId()));
						content.append("</td></tr>\n");
					}

					// unanswered
					content.append("<tr><td colspan=\"3\">&nbsp;</td></tr><tr><td>");
					content.append(format("results-unanswered"));
					content.append("</td><td>");
					content.append(formatUnanswered(question, submissions));
					content.append("</td></tr>\n");

					content.append("</table>\n");
				}

				else if (question.getTypeSpecificQuestion() instanceof MultipleChoiceQuestionImpl)
				{
					MultipleChoiceQuestionImpl tsq = (MultipleChoiceQuestionImpl) question.getTypeSpecificQuestion();
					content.append("<table>\n");
					for (MultipleChoiceQuestionImpl.MultipleChoiceQuestionChoice choice : tsq.getChoices())
					{
						content.append("<tr><td>");
						content.append(stripHtml(choice.getText()));
						content.append("</td><td>");
						content.append(formatCountPercent(question, submissions, choice.getId()));
						content.append("</td></tr>\n");
					}

					// unanswered
					content.append("<tr><td colspan=\"3\">&nbsp;</td></tr><tr><td>");
					content.append(format("results-unanswered"));
					content.append("</td><td>");
					content.append(formatUnanswered(question, submissions));
					content.append("</td></tr>\n");

					content.append("</table>\n");
				}

				else if (question.getTypeSpecificQuestion() instanceof LikertScaleQuestionImpl)
				{
					LikertScaleQuestionImpl tsq = (LikertScaleQuestionImpl) question.getTypeSpecificQuestion();
					content.append("<table>\n");
					for (LikertScaleQuestionImpl.LikertScaleQuestionChoice choice : tsq.getChoices())
					{
						content.append("<tr><td>");
						content.append(stripHtml(choice.getText()));
						content.append("</td><td>");
						content.append(formatCountPercent(question, submissions, choice.getId()));
						content.append("</td></tr>\n");
					}

					// unanswered
					content.append("<tr><td colspan=\"3\">&nbsp;</td></tr><tr><td>");
					content.append(format("results-unanswered"));
					content.append("</td><td>");
					content.append(formatUnanswered(question, submissions));
					content.append("</td></tr>\n");

					content.append("</table>\n");
				}

				// for essay, task - list each submission's answer inline (attachments?)
				else if ((question.getTypeSpecificQuestion() instanceof EssayQuestionImpl)
						|| (question.getTypeSpecificQuestion() instanceof TaskQuestionImpl))
				{
					formatTextResponses(content, question, submissions);
				}

				// for matching... ???

				// for fill-in... ???

				content.append("</p>\n");
			}

			if (!part.getOrdering().getIsLast())
			{
				content.append("<hr />\n");
			}
		}

		return content.toString();
	}

	/**
	 * Count the # submissions that saw this question.
	 * 
	 * @param question
	 *        The question.
	 * @param submissions
	 *        The submissions to the assessment.
	 * @return The # submissions that saw this question.
	 */
	protected Integer countQuestionSeen(Question question, List<Submission> submissions)
	{
		int count = 0;
		for (Submission s : submissions)
		{
			if (s.getIsPhantom()) continue;
			if (!s.getIsComplete()) continue;

			Answer a = s.getAnswer(question);
			if (a != null)
			{
				count++;
			}
		}

		return Integer.valueOf(count);
	}

	/**
	 * Format a message with the list of arguments.
	 * 
	 * @param key
	 *        The message bundle key.
	 * @param args
	 *        Any number of arguments for the formatted message.
	 * @return The formatted message.
	 */
	protected String format(String key, Object... args)
	{
		return this.messages.getFormattedMessage(key, args);
	}

	/**
	 * Count the answers to this question that match this target value, formatting count and percent for display.
	 * 
	 * @param question
	 *        The question.
	 * @param submissions
	 *        The submissions to the assessment that has this question.
	 * @param target
	 *        The target answer value.
	 * @return The formatted count and percent.
	 */
	protected String formatCountPercent(Question question, List<Submission> submissions, String target)
	{
		int count = 0;
		int total = 0;
		for (Submission s : submissions)
		{
			if (s.getIsPhantom()) continue;
			if (!s.getIsComplete()) continue;

			Answer a = s.getAnswer(question);
			if (a != null)
			{
				total++;

				if (a.getIsAnswered())
				{
					// does the answer's value match our target answer?
					// Note: assume that the answer is one of the getData() strings
					String[] answers = a.getTypeSpecificAnswer().getData();
					if ((answers != null) && (answers.length > 0))
					{
						for (int i = 0; i < answers.length; i++)
						{
							if (answers[i].equals(target))
							{
								count++;
							}
						}
					}
				}
			}
		}

		if (total > 0)
		{
			int pct = (count * 100) / total;

			return format("results-format-count", Integer.valueOf(pct), Integer.valueOf(count));
		}

		return "";
	}

	/**
	 * Prepare a display string for the date.
	 * 
	 * @param date
	 *        The date.
	 * @return The display string for the date.
	 */
	protected String formatDate(Date date)
	{
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
		String display = format.format(date);

		// remove seconds
		int i = display.lastIndexOf(":");
		if ((i == -1) || ((i + 3) >= display.length())) return display;

		String rv = display.substring(0, i) + display.substring(i + 3);
		return rv;
	}

	/**
	 * Format the inline answers to this question from the submissions, followed by a count of unanswered.
	 * 
	 * @param question
	 *        The question.
	 * @param submissions
	 *        The submissions to the assessment that has this question.
	 * @param target
	 *        The target answer value.
	 * @return The formatted count and percent.
	 */
	protected void formatTextResponses(StringBuilder content, Question question, List<Submission> submissions)
	{
		int count = 0;
		int total = 0;
		for (Submission s : submissions)
		{
			if (s.getIsPhantom()) continue;
			if (!s.getIsComplete()) continue;

			Answer a = s.getAnswer(question);
			if (a != null)
			{
				total++;

				if (a.getIsAnswered())
				{
					count++;

					TypeSpecificAnswer tsa = a.getTypeSpecificAnswer();
					if (tsa instanceof EssayAnswerImpl)
					{
						String text = ((EssayAnswerImpl) tsa).getAnswerData();
						content.append("<hr>\n");
						content.append(stripHtml(text));
						content.append("\n");
					}
				}
			}
		}

		if (total == count)
		{
			content.append("<hr>\n");
			content.append(format("results-none-unanswered"));
			content.append("\n");
		}

		else
		{
			int pct = ((total - count) * 100) / total;

			content.append("<hr>\n");
			content.append(format("results-count-unanswered", Integer.valueOf(pct), Integer.valueOf(total - count)));
			content.append("\n");
		}
	}

	/**
	 * Count the # submissions that saw this question but did not answer.
	 * 
	 * @param question
	 *        The question.
	 * @param submissions
	 *        The submissions to the assessment.
	 * @return The # submissions that saw this question but did not answer.
	 */
	protected String formatUnanswered(Question question, List<Submission> submissions)
	{
		int count = 0;
		int total = 0;
		for (Submission s : submissions)
		{
			if (s.getIsPhantom()) continue;
			if (!s.getIsComplete()) continue;

			Answer a = s.getAnswer(question);
			if (a != null)
			{
				total++;

				if (!a.getIsAnswered())
				{
					count++;
				}
			}
		}

		if (total > 0)
		{
			int pct = (count * 100) / total;

			return format("results-format-count", Integer.valueOf(pct), Integer.valueOf(count));
		}

		return "";
	}

	/**
	 * Remove the html from the source.
	 * 
	 * @param source
	 *        The source string.
	 * @return The source with html removed.
	 */
	protected String stripHtml(String source)
	{
		// remove \r - convertFormattedTextToPlaintext won't do this.
		source = M_patternCR.matcher(source).replaceAll("");

		// &nbsp; is common, and convertFormattedTextToPlaintext() will replace it with a strange character
		source = M_patternNBSP.matcher(source).replaceAll(" ");

		source = FormattedText.convertFormattedTextToPlaintext(source);
		return source;
	}
}
