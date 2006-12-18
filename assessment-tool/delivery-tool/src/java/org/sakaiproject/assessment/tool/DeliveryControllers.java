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
import org.sakaiproject.assessment.api.AssessmentAnswer;
import org.sakaiproject.assessment.api.AssessmentQuestion;
import org.sakaiproject.assessment.api.AssessmentSection;
import org.sakaiproject.assessment.api.FeedbackDelivery;
import org.sakaiproject.assessment.api.MultipleSubmissionSelectionPolicy;
import org.sakaiproject.assessment.api.QuestionType;
import org.sakaiproject.assessment.api.Submission;
import org.sakaiproject.assessment.api.SubmissionAnswer;
import org.sakaiproject.sludge.api.Context;
import org.sakaiproject.sludge.api.ContextInfoPropertyReference;
import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.Decision;
import org.sakaiproject.sludge.api.DecisionDelegate;
import org.sakaiproject.sludge.api.EntityList;
import org.sakaiproject.sludge.api.FormatDelegate;
import org.sakaiproject.sludge.api.Navigation;
import org.sakaiproject.sludge.api.UiService;
import org.sakaiproject.sludge.api.UserInfoPropertyReference;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.util.StringUtil;

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
								.setStyle(EntityList.Style.flat)
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
								.setStyle(EntityList.Style.flat)
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
												.setEnabled(ui.newDecision().setProperty(ui.newPropertyReference().setPropertyReference("assessment.feedbackNow"))))
										.setSortable(Boolean.TRUE)
										.addNavigation(
											ui.newNavigation()
												.setTitle("list-review-statistics")
												.setStyle(Navigation.Style.link)
												.setDestination(ui.newDestination().setDestination("/statistics/{0}", ui.newPropertyReference().setPropertyReference("id")))
												.setEnabled(
													ui.newDecision().setProperty(ui.newPropertyReference().setPropertyReference("assessment.feedbackNow")),
													ui.newDecision().setProperty(ui.newPropertyReference().setPropertyReference("assessment.feedbackShowStatistics")))))
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
							ui.newButtonBar()
								.add(
									ui.newNavigation()
										.setSubmit()
										.setTitle("enter-beginAssessment")
										.setStyle(Navigation.Style.button))
								.add(
									ui.newNavigation()
										.setDefault()
										.setTitle("cancel")
										.setStyle(Navigation.Style.button)
										.setDestination(ui.newDestination().setDestination("/list")))));
	}

	/**
	 * The question interface needs the following entities in the context:
	 * submission - the selected Submission object
	 * question - the current question
	 * feedback - a non-null value to indicate that we should show feedback
	 * answer - the SubmissionAnswer in the submission to this question.
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
					ui.newToolBar()
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("question-link-feedback")
								.setStyle(Navigation.Style.link)
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}/feedback",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("id")))
								.setEnabled(
										ui.newDecision()
											.setProperty(
												ui.newPropertyReference()
													.setEntityReference("submission"))
											.setDelegate(new ShowFeedbackChoiceDecision())))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("question-link-toc")
								.setStyle(Navigation.Style.link)
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
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("instructions"))
										.setEnabled(
												ui.newCompareDecision()
													.setEqualsConstant(
														QuestionType.matching.toString())
													.setProperty(
														ui.newBooleanPropertyReference()
															.setEntityReference("question")
															.setPropertyReference("type"))))
								.add(
									ui.newText()
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("part.title"))
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
													.setReversed(true)))
								.add(
									ui.newEntityList()
										.setStyle(EntityList.Style.form)
										.setEntityReference(ui.newPropertyReference().setEntityReference("question").setPropertyReference("part.answers"))
										.addColumn(
											ui.newHtmlPropertyColumn()
												.setProperty(null, ui.newPropertyReference().setFormatDelegate(new FormatAnswerCorrectFeedback()))
												.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")), null))
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
															.setPropertyReference("entryAnswerIds"))
													.setLabel("question-label",
														ui.newTextPropertyReference()
															.setPropertyReference("label")))
										.addColumn(
												ui.newPropertyColumn()
													.setProperty(
														ui.newTextPropertyReference()
															.setPropertyReference("text")))
										.addColumn(
												ui.newPropertyColumn()
													.setProperty(
														ui.newTextPropertyReference()
															.setPropertyReference("feedbackGeneral"))
													.setIncluded(ui.newDecision().setDelegate(new AnswerFeedbackDecision()), null))
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
															.setPropertyReference("type"))))
								.add(
									ui.newTextEdit()
										.setTitle("question-text")
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerText"))
										.setEnabled(
												ui.newCompareDecision()
													.setEqualsConstant(QuestionType.essay.toString())
													.setProperty(
														ui.newBooleanPropertyReference()
															.setEntityReference("question")
															.setPropertyReference("type"))))
								.add(
									ui.newFillIn()
										.setText(null, ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("part.title"))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerTexts"))
										.setCorrectMarker(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryCorrects"),
											"/icons/correct.gif",
											"question-correct",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowCorrectAnswer")))
										.setEnabled(
												ui.newCompareDecision()
													.setEqualsConstant(QuestionType.fillIn.toString(), QuestionType.numeric.toString())
													.setProperty(
														ui.newPropertyReference()
															.setEntityReference("question")
															.setPropertyReference("type"))))
								.add(
									ui.newMatch()
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerIds"))
										.setCorrectMarker(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryCorrects"),
											"/icons/correct.gif",
											"question-correct",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowCorrectAnswer")))
										.setFeedback(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("answerFeedbacks"),
											"question-match-answer-feedback",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowAnswerFeedback")))
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
												.setPropertyReference("label"))
										.setEnabled(
												ui.newCompareDecision()
													.setEqualsConstant(QuestionType.matching.toString())
													.setProperty(
														ui.newBooleanPropertyReference()
															.setEntityReference("question")
															.setPropertyReference("type"))))
								.add(
									ui.newTextEdit()
										.setTitle("question-rationale")
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("rationale"))
										.setEnabled(
												ui.newDecision()
													.setProperty(
														ui.newBooleanPropertyReference()
														.setEntityReference("question")
														.setPropertyReference("requireRationale"))))
								.add(
									ui.newSelection()
										.setTitle("question-mark-review")
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("markedForReview")))
								.add(
									ui.newText()
										.setText("question-answer-key", ui.newPropertyReference().setEntityReference("question").setPropertyReference("answerKey"))
										.setEnabled(
												ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
												ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
												ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowCorrectAnswer")),
												ui.newCompareDecision()
													.setEqualsConstant(
														QuestionType.fillIn.toString(),
														QuestionType.matching.toString(),
														QuestionType.multipleChoice.toString(),
														QuestionType.multipleCorrect.toString(),
														QuestionType.numeric.toString(),
														QuestionType.trueFalse.toString())
													.setProperty(ui.newBooleanPropertyReference().setEntityReference("question").setPropertyReference("type"))))
								.add(
									ui.newText()
										.setText("question-feedback", ui.newPropertyReference().setEntityReference("answer").setPropertyReference("questionFeedback"))
										.setEnabled(
												ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
												ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
												ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowQuestionFeedback"))))
								.add(
									ui.newText()
										.setText("question-model-answer", ui.newPropertyReference().setEntityReference("question").setPropertyReference("part.answer.text"))
										.setEnabled(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("question").setPropertyReference("section.assessment.feedbackShowQuestionFeedback")),
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(ui.newBooleanPropertyReference().setEntityReference("question").setPropertyReference("type"))))))
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("question-save-continue")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
										ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
										ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("assessmentOrdering.next.id")))
								.setEnabled(
										ui.newDecision()
											.setReversed(true)
											.setProperty(
												ui.newBooleanPropertyReference()
													.setEntityReference("question")
													.setPropertyReference("assessmentOrdering.isLast"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("question-save-submit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id")))
								.setEnabled(
										ui.newDecision()
											.setProperty(
												ui.newBooleanPropertyReference()
													.setEntityReference("question")
													.setPropertyReference("assessmentOrdering.isLast"))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("quesiton-save-prev")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
									ui.newTextPropertyReference().setEntityReference("question").setPropertyReference("assessmentOrdering.previous.id")))
								.setEnabled(
										ui.newDecision()
											.setReversed(true)
											.setProperty(
												ui.newBooleanPropertyReference()
													.setEntityReference("question")
													.setPropertyReference("assessmentOrdering.isFirst"))))	
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("quesiton-save-exit")
								.setStyle(Navigation.Style.button)
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
							ui.newButtonBar()
								.add(
									ui.newNavigation()
										.setDefault()
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
									ui.newIconKey()
										.setTitle("toc-key-title")
										.addIcon("/icons/unanswered.gif", ui.newMessage().setMessage("toc-key-unanswered"))
										.addIcon("/icons/markedforreview.gif", ui.newMessage().setMessage("toc-key-mark-for-review")))
								.add(
									ui.newSection()
										.setEntityReference(
											ui.newPropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("assessment.sections"))
										// focus is on AssessmentSection
										.add(
											ui.newEntityList()
												.setStyle(EntityList.Style.form)
												.setEntityReference(ui.newPropertyReference().setPropertyReference("questions"))
												.setTitle("toc-questions-title",
													// Part{0} - {1} - {2}/{3} Answered Questions, {4} Points
													ui.newPropertyReference().setPropertyReference("ordering.position"),
													ui.newPropertyReference().setPropertyReference("title"),
													ui.newPropertyReference().setFormatDelegate(new QuestionsAnswered()),
													ui.newPropertyReference().setPropertyReference("numQuestions"),
													ui.newPropertyReference().setFormatDelegate(new SectionScore()))
												// focus is on AssessmentQuestion
												.addColumn(
													ui.newHtmlPropertyColumn()
														.setProperty(null, ui.newPropertyReference().setFormatDelegate(new FormatQuestionDecoration())))
												.addColumn(
													ui.newPropertyColumn()
														.setProperty("toc-question-entry",
															// {num}. {title or instructions} ({points})
															ui.newPropertyReference().setPropertyReference("sectionOrdering.position"),
															ui.newTextPropertyReference().setPropertyReference("title"),
															ui.newPropertyReference().setFormatDelegate(new QuestionScore()))
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
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("toc-save-submit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setTitle("toc-save-exit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list"))));
	}

	// TODO: sludge column included take ... and this goes away
	public static class SubmissionScoreDecision implements DecisionDelegate
	{
		/**
		 * {@inheritDoc}
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
			if ((delivery == FeedbackDelivery.IMMEDIATE)
					|| ((delivery == FeedbackDelivery.BY_DATE) && ((feedbackDate == null) || (!(feedbackDate.after(TimeService
							.newTime()))))))
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

	// TODO: if we do OR in sludge enabled, this goes away
	/**
	 * if we are doing feedback just now, and we have show correct answer
	 */
	public static class ShowFeedbackChoiceDecision implements DecisionDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// reference is the submission
			if (decision.getProperty() == null) return false;
			Object o = decision.getProperty().readObject(context, focus);
			if (o == null) return false;
			if (!(o instanceof Submission)) return false;

			Submission submission = (Submission) o;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if we are doing feedback just now
			if (assessment.getFeedbackNow().booleanValue())
			{
				// if we are doing correct answer feedback or question level feedback
				if ((assessment.getFeedbackShowCorrectAnswer().booleanValue()) || (assessment.getFeedbackShowQuestionFeedback().booleanValue()))
				{
					return true;
				}
			}

			return false;
		}
	}

	public static class FeedbackDateDecision implements DecisionDelegate
	{
		/**
		 * {@inheritDoc}
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
		 * {@inheritDoc}
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
		 * {@inheritDoc}
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// focus is the submission
			if (focus == null) return false;
			if (!(focus instanceof Submission)) return false;

			Submission submission = (Submission) focus;
			Assessment assessment = submission.getAssessment();
			if (assessment == null) return false;

			// if multiple submission are allowed, and if we choose the lastest submission for grading
			Integer numAllowed = assessment.getNumSubmissionsAllowed();
			if (((numAllowed == null) || (numAllowed.intValue() > 1))
					&& (assessment.getMultipleSubmissionSelectionPolicy() == MultipleSubmissionSelectionPolicy.USE_LATEST))
				return true;

			return false;
		}
	}
	
	/**
	 * if we want feedback, are doing feedback, are doing answer feedback, and the question is multi choice or multi correct, and this answer is the one selected...
	 */
	public static class AnswerFeedbackDecision implements DecisionDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			if (context.get("feedback") == null) return false;

			// focus is the AssessmentAnswer
			if (focus == null) return false;
			if (!(focus instanceof AssessmentAnswer)) return false;

			AssessmentAnswer answer = (AssessmentAnswer) focus;
			AssessmentQuestion question = answer.getPart().getQuestion();
			Assessment assessment = question.getSection().getAssessment();

			if (!assessment.getFeedbackNow()) return false;
			if (!assessment.getFeedbackShowAnswerFeedback()) return false;

			if (!((question.getType() == QuestionType.multipleChoice) || (question.getType() == QuestionType.multipleCorrect)))
				return false;

			// for multipleChoice, this must be the answer selected by the entry of the submission answer
			if (question.getType() == QuestionType.multipleChoice)
			{
				SubmissionAnswer submissionAnswer = (SubmissionAnswer) context.get("answer");
				if (!StringUtil.contains(submissionAnswer.getEntryAnswerIds(), answer.getId())) return false;
			}

			return true;
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

	/**
	 * From a value which is an AssessmentSection, 'format' this into a value<br />
	 * that is the number of SubmissionAnswers in the "submission" that are to questions in this section.
	 */
	public static class QuestionsAnswered implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentSection)) return value.toString();

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return value.toString();
			Submission submission = (Submission) o;

			AssessmentSection section = (AssessmentSection) value;

			// count the questions answered
			int count = 0;

			// find the section's answers to AssessmentQuestions that are in this section.
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if (answer.getQuestion().getSection().equals(section))
				{
					count++;
				}
			}

			return Integer.toString(count);
		}
	}

	/**
	 * From a value which is an AssessmentSection, 'format' this into a value<br />
	 * that is the sum of score of SubmissionAnswers in the "submission" that are to questions in this section<br />
	 * (only if feedback is propert in this case) followed by the total points of all questions in this section.
	 */
	public static class SectionScore implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentSection)) return value.toString();
			AssessmentSection section = (AssessmentSection) value;

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return value.toString();
			Submission submission = (Submission) o;

			Assessment assessment = submission.getAssessment();
			if (assessment == null) return value.toString();

			// use the {}/{} format if doing feedback, or just {} if not.
			StringBuffer rv = new StringBuffer();

			// if we are doing feedback just now
			if (assessment.getFeedbackNow())
			{
				// if we are doing score feedback
				if (assessment.getFeedbackShowScore().booleanValue())
				{
					// add the sum of auto-scores for any answered question in this section
					float score = 0;

					// find the section's answers to AssessmentQuestions that are in this section.
					for (SubmissionAnswer answer : submission.getAnswers())
					{
						if (answer.getQuestion().getSection().equals(section))
						{
							score += answer.getAutoScore().floatValue();
						}
					}

					rv.append(Float.toString(score));
					rv.append('/');
				}
			}

			// add the total possible points for the section
			rv.append(section.getTotalPoints().toString());

			return rv.toString();
		}
	}

	/**
	 * From a value which is an AssessmentQuestion, 'format' this into a value<br />
	 * that is the score of the SubmissionAnswer in the "submission" that is to this question<br />
	 * (only if feedback is propert in this case) followed by the total points of the question.
	 */
	public static class QuestionScore implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentQuestion)) return value.toString();
			AssessmentQuestion question = (AssessmentQuestion) value;

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return value.toString();
			Submission submission = (Submission) o;

			Assessment assessment = submission.getAssessment();
			if (assessment == null) return value.toString();

			// use the {}/{} format if doing feedback, or just {} if not.
			StringBuffer rv = new StringBuffer();

			// if we are doing feedback just now
			if (assessment.getFeedbackNow())
			{
				// if we are doing question score feedback
				if (assessment.getFeedbackShowQuestionScore().booleanValue())
				{
					// the auto-scores for this answered question
					float score = 0;

					// find the section answer to this question (don't create it!)
					for (SubmissionAnswer answer : submission.getAnswers())
					{
						if (answer.getQuestion().equals(question))
						{
							score = answer.getAutoScore().floatValue();
							break;
						}
					}

					rv.append(Float.toString(score));
					rv.append('/');
				}
			}

			// add the possible points for the question
			rv.append(question.getPoints().toString());

			return rv.toString();
		}
	}

	/**
	 * From a value which is an AssessmentQuestion, 'format' this into the html for the icons<br />
	 * for 'unanswerd' or 'mark for review' for the related submission question.
	 */
	public static class FormatQuestionDecoration implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentQuestion)) return value.toString();
			AssessmentQuestion question = (AssessmentQuestion) value;

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return value.toString();
			Submission submission = (Submission) o;

			Assessment assessment = submission.getAssessment();
			if (assessment == null) return value.toString();

			// search for our answer without creating it, and if found check the mark for review setting
			boolean found = false;
			boolean markForReview = false;
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if (answer.getQuestion().equals(question))
				{
					found = true;
					markForReview = answer.getMarkedForReview().booleanValue();
				}
			}

			// if not found, use the unanswered icon
			if (!found)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/unanswered.gif\" alt=\"" + context.getMessages().getString("toc-key-unanswered") + "\" />";
			}

			// if found, and if mark for review, use that icon
			else if (markForReview)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/markedforreview.gif\" alt=\"" + context.getMessages().getString("toc-key-mark-for-review") + "\" />";
			}

			return null;
		}
	}
	
	/**
	 * From a value which is an AssessmentAnswer, 'format' this into the html for the icon if it is selected, correct, and if we are doing feedback.
	 */
	public static class FormatAnswerCorrectFeedback implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object focus)
		{
			if (focus == null) return null;
			if (!(focus instanceof AssessmentAnswer)) return null;
			AssessmentAnswer answer = (AssessmentAnswer) focus;

			// the question this is an answer to
			AssessmentQuestion question = answer.getPart().getQuestion();
			if (question == null) return null;

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return null;
			Submission submission = (Submission) o;

			Assessment assessment = submission.getAssessment();
			if (assessment == null) return null;

			// if we are doing feedback just now
			if (assessment.getFeedbackNow())
			{
				// if we are doing currect answer feedback
				if (assessment.getFeedbackShowCorrectAnswer().booleanValue())
				{
					// search for our answer without creating it, and if found check if it is this QuestionAnswer
					for (SubmissionAnswer subAnswer : submission.getAnswers())
					{
						// is this submission answer the answer to our assessment question answer's question?
						if (subAnswer.getQuestion().equals(question))
						{
							// is the submission answer selected this answer?
							if (StringUtil.contains(subAnswer.getEntryAnswerIds(), answer.getId()))
							{
								// is this a correct answer
								if (answer.getIsCorrect().booleanValue())
								{
									return "<img src=\"" + context.get("sakai.return.url") + "/icons/correct.gif\" alt=\""
											+ context.getMessages().getString("toc-alt-correct-answer") + "\" />";
								}
							}
						}
					}
				}
			}

			return null;
		}
	}
}
