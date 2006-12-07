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

package org.sakaiproject.assessment.tool;

import org.sakaiproject.assessment.api.Assessment;
import org.sakaiproject.assessment.api.FeedbackDelivery;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.sludge.api.Context;
import org.sakaiproject.sludge.api.ContextInfoPropertyReference;
import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.Decision;
import org.sakaiproject.sludge.api.DecisionDelegate;
import org.sakaiproject.sludge.api.FormatDelegate;
import org.sakaiproject.sludge.api.Navigation;
import org.sakaiproject.sludge.api.UiService;
import org.sakaiproject.sludge.api.UserInfoPropertyReference;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;

/**
 * Assessment delivery tool controllers.
 */
public class DeliveryControllers
{
	// Note: do not allow an automatic formatter to format this file! -ggolden

	/**
	 * The list interface needs the following entities in the context:
	 * assessments - a List of assessments that the current user can question
	 * submissions - a list of submissions that the current user has submitted
	 */
	public static Controller constructList(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("list-title")
				.setHeader("list-header")
				.add(
					ui.newSection()
						.setTitle("list-take-section-title")
						.add(
							ui.newEntityList()
								.setEntityReference(ui.newPropertyReference().setEntityReference("assessments"))
								.setTitle("list-take-instructions")
								.setEmptyTitle("list-take-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("title"))
										.setSortable(Boolean.TRUE)
										.setEntityNavigation(
												ui.newEntityNavigation()
													.setDestination(ui.newDestination().setDestination("/enter/{0}", ui.newTextPropertyReference().setPropertyReference("id")))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-dueDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setPropertyReference("dueDate")
												.setMissingText("na"))
										.setSortable(Boolean.TRUE)
										.setAlert(
											ui.newPastDateDecision()
												.setProperty(
													ui.newDatePropertyReference()
														.setPropertyReference("dueDate"))))))
				.add(
					ui.newSection()
						.setTitle("list-review-section-title")
						.add(
							ui.newEntityList()
								.setEntityReference(ui.newPropertyReference().setEntityReference("submissions"))
								.setTitle("list-review-instructions")
								.setEmptyTitle("list-review-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("assessment.title"))
										.setEntityNavigation(
											ui.newEntityNavigation()
												.setDestination(ui.newDestination().setDestination("/review/{0}", ui.newTextPropertyReference().setPropertyReference("id")))
												.setEnabled(ui.newDecision().setDelegate(new ReviewDecision())))
										.setSortable(Boolean.TRUE)
										.addNavigation(
											ui.newNavigation()
												.setTitle("list-review-statistics")
												.setStyle(Navigation.Style.link)
												.setDestination(ui.newDestination().setDestination("/statistics/{0}", ui.newPropertyReference().setPropertyReference("id")))
												.setEnabled(ui.newDecision().setDelegate(new ShowStatisticsDecision()))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-feedbackDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setPropertyReference("assessment.feedbackDate")
												.setMissingText("immediate"))
										.setSortable(Boolean.TRUE)
										.setIncluded(ui.newDecision().setDelegate(new FeedbackDateDecision()), "na"))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-score")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("totalScore"))
										.setSortable(Boolean.TRUE)
										.setIncluded(ui.newDecision().setDelegate(new SubmissionScoreDecision()), "na")
										.addFootnote(
												ui.newFootnote()
													.setText("list-review-footnote-highest")
													.setCriteria(ui.newDecision().setDelegate(new HighestGradeFootnoteDecision())))
										.addFootnote(
												ui.newFootnote()
													.setText("list-review-footnote-latest")
													.setCriteria(ui.newDecision().setDelegate(new LatestSubmissionFootnoteDecision()))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-time")
										.setProperty(
											ui.newDurationPropertyReference()
												.setPropertyReference("elapsedTime")
												.setMissingText("na"))
										.setSortable(Boolean.TRUE))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-submittedDue")
										.setProperty(
											ui.newDatePropertyReference()
												.setPropertyReference("submittedDate")
												.setMissingText("na"))
										.setSortable(Boolean.TRUE))));
	}

	/**
	 * The enter interface needs the following entities in the context:
	 * assessment - the selected Assessment object
	 * remainingSubmissions - Integer count of remaining submissions allowed to the current user for the selected assessment
	 */
	public static Controller constructEnter(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("enter-title", ui.newTextPropertyReference().setEntityReference("assessment").setPropertyReference("title"))
				.setHeader("enter-header")
				.add(
					ui.newSection()
						.add(
							ui.newEntityDisplay()
								.setEntityReference(ui.newPropertyReference().setEntityReference("assessment"))
								.setTitle("enter-display-instructions", ui.newTextPropertyReference().setEntityReference("assessment").setPropertyReference("title"))
								// TODO: need Description/Intro row ???
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-contextTitle")
										.setProperty(
											ui.newContextInfoPropertyReference()
												.setSelector(ContextInfoPropertyReference.Selector.title)
												.setPropertyReference("context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setPropertyReference("createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-timeLimit")
										.setProperty(
											ui.newDurationPropertyReference()
												.setPropertyReference("timeLimit")
												.setMissingText("no-time-limit")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-numSubmissionsAllowed")
										.setProperty(
											ui.newTextPropertyReference()
												.setFormat("submissions")
												.setPropertyReference("numSubmissionsAllowed")
												.setMissingText("unlimited")
												.addProperty(
													ui.newTextPropertyReference()
														.setEntityReference("remainingSubmissions"))))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-autoSubmit")
										.setProperty(
											ui.newBooleanPropertyReference()
												.setText("enabled", "disabled")
												.setPropertyReference("autoSubmit")
												.setMissingText("unknown")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-feedback")
										.setProperty(
											ui.newPropertyReference()
												.setFormatDelegate(new FeedbackPropertyReference())
												.setPropertyReference("feedbackDelivery"))))
						.add(
							ui.newSection()
								.add(
									ui.newSubmission()
										.setTitle("enter-beginAssessment")
										.setStyle(org.sakaiproject.sludge.api.Submission.Style.button))
								.add(
									ui.newNavigation()
										.setTitle("cancel")
										.setStyle(Navigation.Style.button)
										.setDestination(ui.newDestination().setDestination("/list")))));
	}

	/**
	 * The question interface needs the following entities in the context:
	 * submission - the selected Submission object
	 * question - the current question
	 * 
	 * When decoding a response, we need in the context:
	 * answer - a collection to get the answer id(s) selected.
	 */
	public static Controller constructQuestion(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("question-title", ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.title"))
				.setHeader("question-header", ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.title"))
				.add(
					ui.newSection()
						.add(ui.newSubmission()
							.setTitle("question-link-feedback")
							.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)	// TODO link, feedback in destination
							.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
								ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
								ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("id"))))
						.add(ui.newSubmission()
							.setTitle("question-link-toc")
							.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)	// TODO link
							.setDestination(ui.newDestination().setDestination("/toc/{0}", ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id")))))
				.add(
					ui.newSection()
						.setTitle("question-section-title",
							ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.ordering.position"),
							ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.numSections"),
							ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.title"))
						.add(
							ui.newSection()
								.setTitle("question-question-title",
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("sectionOrdering.position"),
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("section.numQuestions"),
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("points")
									)
								.add(
									ui.newText()
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.matching.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type")))
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("instructions")))
								.add(
									ui.newText()
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.fillIn.toString(),
													QuestionType.numeric.toString(),
													QuestionType.matching.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type"))
												.setReversed(true))
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("part.title")))
								.add(
									ui.newEntityList()
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.multipleChoice.toString(),
													QuestionType.multipleCorrect.toString(),
													QuestionType.survey.toString(),
													QuestionType.trueFalse.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type")))
										.setEntityReference(ui.newPropertyReference().setEntityReference("question").setPropertyReference("part.answers"))
										.addColumn(
												ui.newSelectionColumn()
													.setSingleSelectDecision(
														ui.newCompareDecision()
															.setEqualsConstant(
																QuestionType.multipleCorrect.toString())
															.setReversed(true)
															.setProperty(
																ui.newBooleanPropertyReference()
																	.setEntityReference("question")
																	.setPropertyReference("type")))
													.setValueProperty(
														ui.newTextPropertyReference()
															.setPropertyReference("id"))
													.setProperty(
														ui.newPropertyReference()
															.setEntityReference("answer")
															.setPropertyReference("entryAnswerIds")))
										.addColumn(
												ui.newPropertyColumn()
												.setProperty("question-label",
													ui.newTextPropertyReference()
														.setPropertyReference("label"))
												.setIncluded(
													ui.newHasValueDecision()
														.setProperty(
															ui.newPropertyReference()
																.setPropertyReference("label")), null))
										.addColumn(
												ui.newPropertyColumn()
													.setProperty(
														ui.newTextPropertyReference()
															.setPropertyReference("text"))))
								.add(
									ui.newTextEdit()
										.setTitle("question-text")
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type")))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerText")))
								.add(
									ui.newFillIn()
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("part.title"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fillIn.toString(), QuestionType.numeric.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type")))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerTexts")))
								.add(
									ui.newMatch()
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.matching.toString())
												.setProperty(
													ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("type")))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerIds"))
										.setSelectText("question-select")
										.setParts(
											ui.newPropertyReference()
												.setEntityReference("question")
												.setPropertyReference("parts"))
										.setPartsChoices(
											ui.newPropertyReference()
												.setPropertyReference("answers"))
										.setPartsTitle(
											ui.newPropertyReference()
												.setPropertyReference("title"))
										.setChoiceId(
											ui.newPropertyReference()
												.setPropertyReference("id"))
										.setChoiceText(
											ui.newPropertyReference()
												.setPropertyReference("text"))
										.setChoiceLabel(
											ui.newPropertyReference()
												.setPropertyReference("label")))
								.add(
									ui.newTextEdit()
										.setTitle("question-rationale")
										.setEnabled(
											ui.newDecision()
												.setProperty(
													ui.newBooleanPropertyReference()
													.setEntityReference("question")
													.setPropertyReference("requireRationale")))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("rationale")))
								.add(
									ui.newSelection()
										.setTitle("question-mark-review")
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("markedForReview")))))
				.add(
					ui.newSection()
						.add(
							ui.newSubmission()
								.setTitle("question-save-continue")
								.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)
								.setEnabled(
									ui.newDecision()
										.setReversed(true)
										.setProperty(
											ui.newBooleanPropertyReference()
												.setEntityReference("question")
												.setPropertyReference("assessmentOrdering.isLast")))
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
										ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
										ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("assessmentOrdering.next.id"))))
						.add(
							ui.newSubmission()
								.setTitle("question-save-submit")
								.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)
								.setEnabled(
									ui.newDecision()
										// TODO: what's the criteria for this?
										.setProperty(
											ui.newBooleanPropertyReference()
												.setEntityReference("question")
												.setPropertyReference("assessmentOrdering.isLast")))
								.setDestination(ui.newDestination().setDestination("/exit/{0}",ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"))))
						.add(
							ui.newSubmission()
								.setTitle("quesiton-save-prev")
								.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)
								.setEnabled(
									ui.newDecision()
										.setReversed(true)
										.setProperty(
											ui.newBooleanPropertyReference()
												.setEntityReference("question")
												.setPropertyReference("assessmentOrdering.isFirst")))
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("assessmentOrdering.previous.id"))))	
						.add(
							ui.newSubmission()
								.setTitle("quesiton-save-exit")
								.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)
								.setDestination(ui.newDestination().setDestination("/list"))));
	}

	/**
	 * The exit interface needs the following entities in the context:
	 * submission - the completed submission
	 * remainingSubmissions - Integer count of remaining submissions allowed to the current user for the selected assessment
	 */
	public static Controller constructExit(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("exit-title", ui.newTextPropertyReference().setEntityReference("assessment").setPropertyReference("title"))
				.setHeader("exit-header")
				.add(
					ui.newSection()
						.setTitle("exit-section-title", ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("assessment.title"))
						.add(
							ui.newEntityDisplay()
								.setEntityReference(ui.newPropertyReference().setEntityReference("submission"))
								.setTitle("exit-display-instructions")
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-contextTitle")
										.setProperty(
											ui.newContextInfoPropertyReference()
												.setSelector(ContextInfoPropertyReference.Selector.title)
												.setPropertyReference("assessment.context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setPropertyReference("assessment.createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("assessment.title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-numSubmissionsRemaining")
										.setProperty(
											ui.newTextPropertyReference()
												.setPropertyReference("assessment.numSubmissionsAllowed")
												.setFormat("exit-submissions-remaining")
												//.setMissingValues("-1")
												.setMissingText("unlimited")
												.addProperty(
														ui.newTextPropertyReference()
															.setEntityReference("remainingSubmissions"))))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-confirmation")
										.setProperty(
											ui.newPropertyReference()
												.setPropertyReference("confirmation")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-submitted")
										.setProperty(
											ui.newDatePropertyReference()
												.setPropertyReference("submittedDate"))))
						.add(
							ui.newSection()
								.add(
									ui.newNavigation()
										.setTitle("return")
										.setStyle(Navigation.Style.button)
										.setDestination(ui.newDestination().setDestination("/list")))));
	}

	/**
	 * The toc interface needs the following entities in the context:
	 * submission - the completed submission
	 */
	public static Controller constructToc(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("toc-title")
				.setHeader("toc-header", ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("assessment.title"))
				.add(
					ui.newSection()
						.setTitle("toc-section-title",
							ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("assessment.totalPoints"))
						.add(
							ui.newInstructions()
								.setText("toc-section-alert"))
						.add(
							ui.newSection()
								.add(
									ui.newInstructions()
										.setText("toc-key-placeholder"))
								.add(
									ui.newSection()
										.setEntityReference(
											ui.newPropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("assessment.sections"))
										// focus is on AssessmentSection
										.add(
											ui.newEntityList()
												.setEntityReference(ui.newPropertyReference().setPropertyReference("questions"))
												.setTitle("toc-questions-title",
													// Part{0} - {1} - {2}/{3} Answered Questions, {4}/{5} Points
													ui.newPropertyReference().setPropertyReference("ordering.position"),
													ui.newPropertyReference().setPropertyReference("title"),
													// TODO: how manu questions answers in the submission in this section?
													ui.newPropertyReference().setPropertyReference("ordering.position"),
													ui.newPropertyReference().setPropertyReference("numQuestions"),
													// TODO: submission score in this section
													ui.newPropertyReference().setPropertyReference("ordering.position"),
													ui.newPropertyReference().setPropertyReference("totalPoints"))
												// focus is on AssessmentQuestion
												.addColumn(
													ui.newPropertyColumn()
														.setProperty("toc-question-entry",
															// {num}. {title or instructions} ({score}/{total points})
															ui.newPropertyReference().setPropertyReference("sectionOrdering.position"),
															// TODO: some q types use q.instructions
															ui.newTextPropertyReference().setPropertyReference("part.title"),
															// TODO: submission score for this q
															ui.newTextPropertyReference().setPropertyReference("sectionOrdering.position"),
															ui.newTextPropertyReference().setPropertyReference("points"))
														.setEntityNavigation(
															ui.newEntityNavigation()
																// destination is /question/sid/aqid
																.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
																	ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
																	ui.newTextPropertyReference().setPropertyReference("id"))))
														)
										)
									)
							)
					)
				.add(
					ui.newSection()
						.add(
							ui.newSubmission()
								.setTitle("toc-save-submit")
								.setStyle(org.sakaiproject.sludge.api.Submission.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"))))
						.add(
							ui.newNavigation()
								.setTitle("toc-save-exit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list"))));
	}

	public static class SubmissionScoreDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if we are doing feedback just now
			FeedbackDelivery delivery = assessment.getFeedbackDelivery();
			Time feedbackDate = assessment.getFeedbackDate();
			if (	(delivery == FeedbackDelivery.IMMEDIATE)
				||	(		(delivery == FeedbackDelivery.BY_DATE)
						&&	((feedbackDate == null) || (!(feedbackDate.after(TimeService.newTime()))))
					)
				)
			{
				// if we are doing score feedback
				if (assessment.getFeedbackShowScore().booleanValue())
				{
					// show the score
					return true;
				}
			}
			
			return false;
		}
	}

	public static class ShowStatisticsDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if we are doing feedback just now
			FeedbackDelivery delivery = assessment.getFeedbackDelivery();
			Time feedbackDate = assessment.getFeedbackDate();
			if (	(delivery == FeedbackDelivery.IMMEDIATE)
				||	(		(delivery == FeedbackDelivery.BY_DATE)
						&&	((feedbackDate == null) || (!(feedbackDate.after(TimeService.newTime()))))
					)
				)
			{
				// if we are doing statistics feedback
				if (assessment.getFeedbackShowStatistics().booleanValue())
				{
					// show the score
					return true;
				}
			}
			
			return false;
		}
	}

	public static class ReviewDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if we are doing feedback just now
			FeedbackDelivery delivery = assessment.getFeedbackDelivery();
			Time feedbackDate = assessment.getFeedbackDate();
			if (	(delivery == FeedbackDelivery.IMMEDIATE)
				||	(		(delivery == FeedbackDelivery.BY_DATE)
						&&	((feedbackDate == null) || (!(feedbackDate.after(TimeService.newTime()))))
					)
				)
			{
				return true;
			}
			
			return false;
		}
	}

	public static class FeedbackDateDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if we are doing feedback ever
			FeedbackDelivery delivery = assessment.getFeedbackDelivery();
			return delivery != FeedbackDelivery.NONE;
		}
	}

	public static class HighestGradeFootnoteDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if multiple submission are allowed, and if we choose the highest score for grading
			Integer numAllowed = assessment.getNumSubmissionsAllowed();
			if (((numAllowed == null) || (numAllowed.intValue() > 1))
					&& (assessment.getMultipleSubmissionSelectionPolicy() == MultipleSubmissionSelectionPolicy.USE_HIGHEST_GRADED))
				return true;

			return false;
		}
	}

	public static class LatestSubmissionFootnoteDecision implements DecisionDelegate
	{
		/**
		 * Make the decision:
		 * 
		 * @param context
		 *        The UiContext.
		 * @param entity
		 *        The entity to get the selector value from.
		 * @return True if the entity has the selector and it evaluates to a boolean TRUE value, false if not.
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if multiple submission are allowed, and if we choose the highest score for grading
			Integer numAllowed = assessment.getNumSubmissionsAllowed();
			if (((numAllowed == null) || (numAllowed.intValue() > 1))
					&& (assessment.getMultipleSubmissionSelectionPolicy() == MultipleSubmissionSelectionPolicy.USE_LATEST))
				return true;

			return false;
		}
	}

	public static class FeedbackPropertyReference implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof FeedbackDelivery)) return value.toString();
			
			FeedbackDelivery delivery = (FeedbackDelivery) value;

			if (delivery == FeedbackDelivery.IMMEDIATE) return context.getMessages().getString("immediate");
			if (delivery == FeedbackDelivery.NONE) return context.getMessages().getString("none");

			// it's by date, return that date
			Time release = ((Assessment) context.get("assessment")).getFeedbackDate();
			if (release != null) return release.toStringLocalFull();

			return context.getMessages().getString("unknown");
		}
	}
}
