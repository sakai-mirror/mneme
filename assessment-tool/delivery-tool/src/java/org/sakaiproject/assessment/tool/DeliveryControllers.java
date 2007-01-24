/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2007 The Sakai Foundation.
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
import org.sakaiproject.assessment.api.QuestionPresentation;
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
								.setIterator(ui.newPropertyReference().setEntityReference("assessments"), "assessment")
								.setTitle("list-take-instructions")
								.setEmptyTitle("list-take-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setEntityReference("assessment")
												.setPropertyReference("title"))
										.setEntityNavigation(
											ui.newEntityNavigation()
												.setDestination(ui.newDestination().setDestination("/enter/{0}", ui.newTextPropertyReference().setEntityReference("assessment").setPropertyReference("id"))))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("0").setProperty(ui.newPropertyReference().setEntityReference("assessment_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("assessment_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/0A{0}{1}",
													ui.newTextPropertyReference().setEntityReference("submission_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("submission_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/0D{0}{1}",
													ui.newTextPropertyReference().setEntityReference("submission_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("submission_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-dueDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setEntityReference("assessment")
												.setPropertyReference("dueDate")
												.setMissingText("na"))
										.setAlert(
											ui.newPastDateDecision()
												.setProperty(
													ui.newDatePropertyReference()
														.setEntityReference("assessment")
														.setPropertyReference("dueDate")))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("1").setProperty(ui.newPropertyReference().setEntityReference("assessment_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("assessment_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/1A{0}{1}",
													ui.newTextPropertyReference().setEntityReference("submission_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("submission_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/1D{0}{1}",
													ui.newTextPropertyReference().setEntityReference("submission_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("submission_sort_ad"))))))
				.add(
					ui.newSection()
						.setTitle("list-review-section-title")
						.add(
							ui.newEntityList()
								.setStyle(EntityList.Style.flat)
								.setIterator(ui.newPropertyReference().setEntityReference("submissions"), "submission")
								.setTitle("list-review-instructions")
								.setEmptyTitle("list-review-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("assessment.title"))
										.setEntityNavigation(
											ui.newEntityNavigation()
												.setDestination(ui.newDestination().setDestination("/review/{0}", ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id")))
												.setEnabled(ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("submission").setPropertyReference("assessment.feedbackNow"))))
										.addNavigation(
											ui.newNavigation()
												.setTitle("list-review-statistics")
												.setStyle(Navigation.Style.link)
												.setDestination(ui.newDestination().setDestination("/statistics/{0}", ui.newPropertyReference().setEntityReference("submission").setPropertyReference("id")))
												.setEnabled(
													ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("submission").setPropertyReference("assessment.feedbackNow")),
													ui.newDecision().setProperty(ui.newPropertyReference().setEntityReference("submission").setPropertyReference("assessment.feedbackShowStatistics"))))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("0").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}0A",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}0D",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-feedbackDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("assessment.feedbackDate")
												.setMissingText("immediate"))
										.setIncluded(ui.newDecision().setDelegate(new FeedbackDateDecision()), "na")
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("1").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}1A",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}1D",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-score")
										.setProperty(
											ui.newTextPropertyReference()
											 	.setEntityReference("submission")
												.setPropertyReference("totalScore"))
										.setIncluded(ui.newDecision().setDelegate(new SubmissionScoreDecision()), "na")
										.addFootnote(
											ui.newFootnote()
												.setText("list-review-footnote-highest")
												.setCriteria(ui.newDecision().setDelegate(new HighestGradeFootnoteDecision())))
										.addFootnote(
											ui.newFootnote()
												.setText("list-review-footnote-latest")
												.setCriteria(ui.newDecision().setDelegate(new LatestSubmissionFootnoteDecision())))
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("2").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}2A",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}2D",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-time")
										.setProperty(
											ui.newDurationPropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("elapsedTime")
												.setMissingText("na"))
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("3").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}3A",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}3D",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-submittedDue")
										.setProperty(
											ui.newDatePropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("submittedDate")
												.setMissingText("na"))
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("4").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setEntityReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}4A",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}4D",
													ui.newTextPropertyReference().setEntityReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setEntityReference("assessment_sort_ad"))))));
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
						.setTitle("enter-display-instructions", ui.newPropertyReference().setEntityReference("assessment").setPropertyReference("title"))
						.add(
							ui.newHtml()
								.setText(null, ui.newPropertyReference().setEntityReference("assessment").setPropertyReference("description")))
						.add(
							ui.newAttachments()
								.setTitle("enter-attachments")
								.setAttachments(ui.newPropertyReference().setEntityReference("assessment").setPropertyReference("attachments"), null)
								.setEnabled(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("assessment").setPropertyReference("attachments"))))
						.add(
							ui.newEntityDisplay()
								.setEntityReference(ui.newPropertyReference().setEntityReference("assessment"))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-contextTitle")
										.setProperty(
											ui.newContextInfoPropertyReference()
												.setSelector(ContextInfoPropertyReference.Selector.title)
												.setEntityReference("assessment")
												.setPropertyReference("context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setEntityReference("assessment")
												.setPropertyReference("createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setEntityReference("assessment")
												.setPropertyReference("title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-timeLimit")
										.setProperty(
											ui.newDurationPropertyReference()
												.setEntityReference("assessment")
												.setPropertyReference("timeLimit")
												.setMissingText("no-time-limit")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-numSubmissionsAllowed")
										.setProperty(
											ui.newTextPropertyReference()
												.setFormat("submissions")
												.setEntityReference("assessment")
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
												.setEntityReference("assessment")
												.setPropertyReference("autoSubmit")
												.setMissingText("unknown")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-feedback")
										.setProperty(
											ui.newPropertyReference()
												.setFormatDelegate(new FeedbackPropertyReference())
												.setEntityReference("assessment")
												.setPropertyReference("feedbackDelivery")))))
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
									.setDestination(ui.newDestination().setDestination("/list"))));
	}

	/**
	 * The question interface needs the following entities in the context:
	 * submission - the selected Submission object
	 * feedback - a non-null value to indicate that we should show feedback
	 * answers - collection of answers that are the questions to include
	 * question - for a single question page, the single question
	 * section - for a single section page, the single section
	 * review - set if we are in review mode
	 * questionSelector - the current question selector string
	 * 
	 * When decoding a response, we need in the context:
	 * answers - a collection to get the answer id(s) selected.
	 */
	public static Controller constructQuestion(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("question-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.setHeader("question-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(
					ui.newToolBar()
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("question-link-feedback")
								.setStyle(Navigation.Style.link)
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}/feedback",
									ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
									ui.newTextPropertyReference().setReference("questionSelector")))
								.setEnabled(
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("submission"))
										.setDelegate(new ShowFeedbackChoiceDecision()),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("review"))
											.setReversed()))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("question-link-toc")
								.setStyle(Navigation.Style.link)
								.setDestination(ui.newDestination().setDestination("/toc/{0}", ui.newTextPropertyReference().setReference("submission.id")))
								.setEnabled(
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("submission.assessment.randomAccess")),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("review"))
											.setReversed()))
						.add(
							ui.newNavigation()
								.setTitle("review-link-return")
								.setStyle(Navigation.Style.link)
								.setDestination(ui.newDestination().setDestination("/list"))
								.setEnabled(
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("review"))))
						.setEnabled(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed()))
				.add(
					ui.newNavigationBar()
						.setReturn(
							ui.newNavigation()
								.setTitle("review-link-return")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						//.setPrev(ui.newNavigation().setTitle("review-link-prev").setStyle(Navigation.Style.button).setReadOnly(ui.newConstantPropertyReference().setValue("true")))
						//.setNext(ui.newNavigation().setTitle("review-link-next").setStyle(Navigation.Style.button).setReadOnly(ui.newConstantPropertyReference().setValue("true")))
						.setEnabled(
							ui.newDecision()
								.setProperty(
									ui.newPropertyReference()
										.setReference("review"))))
				.add(
					ui.newSection()
						.setIterator(
							ui.newPropertyReference().setEntityReference("answers"), "answer")
						.setTitle("question-section-title",
							ui.newTextPropertyReference().setReference("answer.question.section.ordering.position"),
							ui.newTextPropertyReference().setReference("answer.question.section.assessment.numSections"),
							ui.newTextPropertyReference().setReference("answer.question.section.title"))
						.setAnchor("question-anchor", ui.newPropertyReference().setReference("answer.question.id"))
						.setTitleEnabled(
							ui.newOrDecision()
								.setOptions(
									ui.newHasValueDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("question")),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("answer.question.sectionOrdering.isFirst"))))
						.add(
							ui.newHtml()
								.setText(null, ui.newPropertyReference().setReference("answer.question.section.description")))
						.add(
							ui.newAttachments()
								.setTitle("enter-attachments")
								.setAttachments(ui.newPropertyReference().setReference("answer.question.section.attachments"), null)
								.setEnabled(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.attachments"))))
						.add(
							ui.newSection()
								.setTitle(null,ui.newTextPropertyReference().setReference("answer.question").setFormatDelegate(new FormatQuestionTitle()))
								.add(
									ui.newHtml()
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.instructions"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.matching.toString())
												.setProperty(
													ui.newPropertyReference().setReference("answer.question.type"))))
								.add(
									ui.newHtml()
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.part.title"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.fillIn.toString(),
													QuestionType.numeric.toString(),
													QuestionType.matching.toString())
												.setProperty(
													ui.newPropertyReference().setReference("answer.question.type"))
												.setReversed()))
								.add(
									ui.newAttachments()
										.setAttachments(ui.newPropertyReference().setReference("answer.question.attachments"), null)
										.setEnabled(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("answer.question.attachments"))))
								.add(
									ui.newEntityList()
										.setStyle(EntityList.Style.form)
										.setIterator(
											ui.newPropertyReference().setReference("answer.question.part.answers"), "qanswer")
										.addColumn(
											ui.newHtmlPropertyColumn()
												.setProperty(null, ui.newPropertyReference().setFormatDelegate(new FormatAnswerCorrectFeedback()))
												.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")), null))
										.addColumn(
											ui.newSelectionColumn()
												.setReadOnly(ui.newPropertyReference().setReference("review"))
												.setSingleSelectDecision(
													ui.newCompareDecision()
														.setEqualsConstant(
															QuestionType.multipleCorrect.toString())
														.setReversed()
														.setProperty(
															ui.newPropertyReference()
																.setReference("answer.question.type")))
												.setValueProperty(
													ui.newTextPropertyReference()
														.setEntityReference("qanswer")
														.setPropertyReference("id"))
												.setProperty(
													ui.newPropertyReference()
														.setEntityReference("answer")
														.setPropertyReference("entryAnswerIds"))
												.setLabel("question-label",
													ui.newTextPropertyReference()
														.setEntityReference("qanswer")
														.setPropertyReference("label")))
										.addColumn(
											ui.newHtmlPropertyColumn()
												.setProperty(
													ui.newTextPropertyReference()
														.setEntityReference("qanswer")
														.setPropertyReference("text")))
										.addColumn(
											ui.newHtmlPropertyColumn()
												.setProperty(
													ui.newTextPropertyReference()
														.setEntityReference("qanswer")
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
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newTextEdit()
										.setTitle("question-text")
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setSize(20, 80)
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("entryAnswerText"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFileUpload()
										.setTitle("question-upload-title")
										.setUpload("quesiton-upload-upload")
										.setProperty(ui.newPropertyReference().setReference("answer.uploadFile"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fileUpload.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newAttachments()
										.setAttachments(
											ui.newPropertyReference().setEntityReference("answer").setPropertyReference("entryAnswerAttachments"), "attachment")
										.setSize(false)
										.setTimestamp(true)
										.addNavigation(
											ui.newNavigation()
												.setTitle("question-upload-remove")
												.setStyle(Navigation.Style.link)
												.setSubmit()
												.setDestination(ui.newDestination().setDestination("/remove/{0}/{1}{2}",
													ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("id"),
													ui.newTextPropertyReference().setReference("answer.question.id"),
													ui.newTextPropertyReference().setReference("attachment.reference")))
												.setEnabled(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed()))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fileUpload.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFillIn()
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.part.title"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
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
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setWidth(20)
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fillIn.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFillIn()
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.part.title"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
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
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setWidth(10)
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.numeric.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newMatch()
										.setReadOnly(ui.newPropertyReference().setReference("review"))
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
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setFeedback(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("answerFeedbacks"),
											"question-match-answer-feedback",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowAnswerFeedback")))
										.setSelectText("question-select")
										.setParts(
											ui.newPropertyReference()
												.setReference("answer.question.parts"))
										.setPartsChoices(
											ui.newPropertyReference()
												.setEntityReference("part")
												.setPropertyReference("answers"))
										.setPartsTitle(
											ui.newPropertyReference()
												.setEntityReference("part")
												.setPropertyReference("title"))
										.setChoiceId(
											ui.newPropertyReference()
												.setEntityReference("choice")
												.setPropertyReference("id"))
										.setChoiceText(
											ui.newPropertyReference()
												.setEntityReference("choice")
												.setPropertyReference("text"))
										.setChoiceLabel(
											ui.newPropertyReference()
												.setEntityReference("choice")
												.setPropertyReference("label"))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.matching.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newTextEdit()
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setTitle("question-rationale")
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("rationale"))
										.setSize(5, 40)
										.setEnabled(
											ui.newDecision()
												.setProperty(
													ui.newBooleanPropertyReference()
													.setReference("answer.question.requireRationale"))))
								.add(
									ui.newSelection()
										.setTitle("question-mark-review")
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setProperty(
											ui.newPropertyReference()
												.setEntityReference("answer")
												.setPropertyReference("markedForReview"))
										.setEnabled(
											ui.newDecision()
												.setProperty(
													ui.newBooleanPropertyReference()
													.setReference("answer.question.section.assessment.randomAccess"))))
								.add(
									ui.newText()
										.setText("question-answer-key", ui.newPropertyReference().setReference("answer.question.answerKey"))
										.setEnabled(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")),
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.fillIn.toString(),
													QuestionType.matching.toString(),
													QuestionType.multipleChoice.toString(),
													QuestionType.multipleCorrect.toString(),
													QuestionType.numeric.toString(),
													QuestionType.trueFalse.toString())
												.setProperty(ui.newPropertyReference().setReference("answer.question.type"))))
								.add(
									ui.newHtml()
										.setText("question-feedback", ui.newPropertyReference().setEntityReference("answer").setPropertyReference("questionFeedback"))
										.setEnabled(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowQuestionFeedback"))))
								.add(
									ui.newHtml()
										.setText("question-model-answer", ui.newPropertyReference().setReference("answer.question.part.answer.text"))
										.setEnabled(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setEntityReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowQuestionFeedback")),
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(ui.newPropertyReference().setReference("answer.question.type"))))))
				.add(
					ui.newNavigationBar()
						.setReturn(
							ui.newNavigation()
								.setTitle("review-link-return")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						//.setPrev(ui.newNavigation().setTitle("review-link-prev").setStyle(Navigation.Style.button).setReadOnly(ui.newConstantPropertyReference().setValue("true")))
						//.setNext(ui.newNavigation().setTitle("review-link-next").setStyle(Navigation.Style.button).setReadOnly(ui.newConstantPropertyReference().setValue("true")))
						.setEnabled(
							ui.newDecision()
								.setProperty(
									ui.newPropertyReference()
										.setReference("review"))))
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("question-save-continue")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("question.assessmentOrdering.next.id")))
								.setEnabled(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("question")),
									ui.newDecision()
										.setReversed()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("question.assessmentOrdering.isLast"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("question-save-continue")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("section.ordering.next.id")))
								.setEnabled(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("section")),
									ui.newDecision()
										.setReversed()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("section.ordering.isLast"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("question-save-submit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",ui.newTextPropertyReference().setReference("submission.id")))
								.setEnabled(
									ui.newOrDecision()
										.setOptions(
											ui.newAndDecision()
												.setRequirements(
													ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("question")),
													ui.newDecision().setProperty(ui.newBooleanPropertyReference().setReference("question.assessmentOrdering.isLast"))),
											ui.newAndDecision()
												.setRequirements(
													ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("section")),
													ui.newDecision().setProperty(ui.newBooleanPropertyReference().setReference("section.ordering.isLast"))),
											ui.newAndDecision()
												.setRequirements(
													ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("question")),
													ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("section"))))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("quesiton-save-prev")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("question.assessmentOrdering.previous.id")))
								.setEnabled(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("question")),
									ui.newDecision()
										.setReversed()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("question.assessmentOrdering.isFirst")),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("submission.assessment.randomAccess"))))	
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("quesiton-save-prev")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("section.ordering.previous.id")))
								.setEnabled(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("section")),
									ui.newDecision()
										.setReversed()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("section.ordering.isFirst")),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("submission.assessment.randomAccess"))))	
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("quesiton-save-exit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list"))
								.setEnabled(
									ui.newDecision()
										.setDelegate(new SaveExitDecision())))
						.setEnabled(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed()));
	}

	/**
	 * The submit interface needs the following entities in the context:
	 * submission - the selected Submission object
	 */
	public static Controller constructSubmit(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("submit-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.setHeader("submit-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(
					ui.newAlert().setText("submit-alert"))
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("submit-save-submit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",ui.newTextPropertyReference().setReference("submission.id")))));
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
				.setTitle("exit-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.setHeader("exit-header")
				.add(
					ui.newSection()
						.setTitle("exit-section-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
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
												.setEntityReference("submission")
												.setPropertyReference("assessment.context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setEntityReference("submission")
												.setPropertyReference("assessment.createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setEntityReference("submission")
												.setPropertyReference("assessment.title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-numSubmissionsRemaining")
										.setProperty(
											ui.newTextPropertyReference()
												.setEntityReference("submission")
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
												.setEntityReference("submission")
												.setPropertyReference("confirmation")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("exit-display-submitted")
										.setProperty(
											ui.newDatePropertyReference()
												.setEntityReference("submission")
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
				.setHeader("toc-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(
					ui.newSection()
						.setTitle("toc-section-title",
							ui.newTextPropertyReference().setReference("submission.assessment.totalPoints"))
						.add(
							ui.newInstructions()
								.setText("toc-section-alert"))
						.add(
							ui.newIconKey()
								.setTitle("toc-key-title")
								.addIcon("/icons/unanswered.gif", ui.newMessage().setMessage("toc-key-unanswered"))
								.addIcon("/icons/markedforreview.gif", ui.newMessage().setMessage("toc-key-mark-for-review"))))
				.add(
					ui.newSection()
						.setIterator(
							ui.newPropertyReference()
								.setReference("submission.assessment.sections"), "section")
						.add(
							ui.newEntityList()
								.setStyle(EntityList.Style.form)
								.setIterator(
									ui.newPropertyReference().setReference("section.questions"), "question")
								.setTitle("toc-questions-title",
									// Part{0} - {1} - {2}/{3} Answered Questions, {4} Points
									ui.newPropertyReference().setReference("section.ordering.position"),
									ui.newPropertyReference().setReference("section.title"),
									ui.newPropertyReference().setFormatDelegate(new QuestionsAnswered()),
									ui.newPropertyReference().setReference("section.numQuestions"),
									ui.newPropertyReference().setFormatDelegate(new SectionScore()))
								.addColumn(
									ui.newHtmlPropertyColumn()
										.setProperty(null, ui.newPropertyReference().setFormatDelegate(new FormatQuestionDecoration()))
										.setWidth(16))
								.addColumn(
									ui.newPropertyColumn()
										.setProperty("toc-question-entry",
											// {num}. {title or instructions} ({points})
											ui.newPropertyReference().setFormatDelegate(new FormatQuestionNumber()),
											ui.newTextPropertyReference()
												.setMaxLength(60)
												.setStripHtml()
												.setReference("question.title"),
											ui.newPropertyReference().setFormatDelegate(new QuestionScore()))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/q questionId
												.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionPresentation.BY_QUESTION.toString())
												.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))
								.addColumn(
									ui.newPropertyColumn()
										.setProperty("toc-question-entry",
											// {num}. {title or instructions} ({points})
											ui.newPropertyReference().setFormatDelegate(new FormatQuestionNumber()),
											ui.newTextPropertyReference()
												.setMaxLength(60)
												.setStripHtml()
												.setReference("question.title"),
											ui.newPropertyReference().setFormatDelegate(new QuestionScore()))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/s sectionId
												.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.section.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionPresentation.BY_SECTION.toString())
												.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))
								.addColumn(
									ui.newPropertyColumn()
										.setProperty("toc-question-entry",
											// {num}. {title or instructions} ({points})
											ui.newPropertyReference().setFormatDelegate(new FormatQuestionNumber()),
											ui.newTextPropertyReference()
												.setMaxLength(60)
												.setStripHtml()
												.setReference("question.title"),
											ui.newPropertyReference().setFormatDelegate(new QuestionScore()))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/a
												.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setEnabled(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionPresentation.BY_ASSESSMENT.toString())
												.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))))
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("toc-save-submit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/exit/{0}",
									ui.newTextPropertyReference().setReference("submission.id"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setTitle("toc-save-exit")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list"))));
	}

	/**
	 * The remove interface needs the following entities in the context:
	 * submission - the submission being taken
	 * question - the current quesiton
	 * attachment - List containing he attachment selected for removal
	 */
	public static Controller constructRemove(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("remove-title")
				.setHeader("remove-header", ui.newTextPropertyReference().setEntityReference("submission").setPropertyReference("assessment.title"))
				.add(
					ui.newSection()
						.add(
							ui.newAlert()
								.setText("remove-confirm"))
						.add(
							ui.newAttachments()
								.setAttachments(ui.newPropertyReference().setEntityReference("attachment"), null)
								.setSize(false)
								.setTimestamp(true)))
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setEnabled(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_QUESTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))	
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setEnabled(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_SECTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))		
				.add(
					ui.newButtonBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setEnabled(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_ASSESSMENT.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation"))))

									
									;
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
				if ((assessment.getFeedbackShowCorrectAnswer().booleanValue())
						|| (assessment.getFeedbackShowQuestionFeedback().booleanValue()))
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

	/**
	 * decide about the save-exit button for the question interface
	 */
	public static class SaveExitDecision implements DecisionDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public boolean decide(Decision decision, Context context, Object focus)
		{
			// get question from context - if it's there, and it's the last, and we are linear (not random), block
			// if no question, get section from context - if it's there and it's the last, and we are linear (not randon), block
			// otherwise put in the button
			// TODO: proper section and assessment handling re: linear? -ggolden
			Object o = context.get("question");
			if ((o != null) && (o instanceof AssessmentQuestion))
			{
				AssessmentQuestion question = (AssessmentQuestion) o;
				if ((question.getAssessmentOrdering().getIsLast().booleanValue())
						&& (!question.getSection().getAssessment().getRandomAccess().booleanValue()))
				{
					return false;
				}

				return true;
			}

			// not question, try section
			o = context.get("section");
			if ((o != null) && (o instanceof AssessmentSection))
			{
				AssessmentSection section = (AssessmentSection) o;
				if ((section.getOrdering().getIsLast().booleanValue())
						&& (!section.getAssessment().getRandomAccess().booleanValue()))
				{
					return false;
				}

				return true;
			}

			// neither question or section, assume assessment
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

			// find the section's answers to AssessmentQuestions that are in this section and are considered answered.
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if (answer.getQuestion().getSection().equals(section) && answer.getIsAnswered().booleanValue())
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

			// search for our answer without creating it, and if found check the answered and mark for review setting
			boolean answered = false;
			boolean markForReview = false;
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if (answer.getQuestion().equals(question))
				{
					answered = answer.getIsAnswered();
					markForReview = answer.getMarkedForReview().booleanValue();
					break;
				}
			}

			// if mark for review, use that icon
			if (markForReview)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/markedforreview.gif\" alt=\""
						+ context.getMessages().getString("toc-key-mark-for-review") + "\" />";
			}

			// otherwise if not found, or not answered, use the unanswered icon
			else if (!answered)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/unanswered.gif\" alt=\""
						+ context.getMessages().getString("toc-key-unanswered") + "\" />";
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
								if ((answer.getIsCorrect() != null) && answer.getIsCorrect().booleanValue())
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

	/**
	 * Focus is the question - format the title, using either section or assessment based numbering.
	 */
	public static class FormatQuestionTitle implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentQuestion)) return null;

			AssessmentQuestion question = (AssessmentQuestion) value;
			Boolean continuous = question.getSection().getAssessment().getContinuousNumbering();

			Object[] args = new Object[3];
			if ((continuous != null) && (continuous.booleanValue()))
			{
				args[0] = question.getAssessmentOrdering().getPosition();
				args[1] = question.getSection().getAssessment().getNumQuestions();
			}
			else
			{
				args[0] = question.getSectionOrdering().getPosition();
				args[1] = question.getSection().getNumQuestions();
			}
			args[2] = question.getPoints();

			return context.getMessages().getFormattedMessage("question-question-title", args);
		}
	}

	/**
	 * Focus is the question - format the number as either per section of per assessment.
	 */
	public static class FormatQuestionNumber implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof AssessmentQuestion)) return null;

			AssessmentQuestion question = (AssessmentQuestion) value;
			Boolean continuous = question.getSection().getAssessment().getContinuousNumbering();

			Integer num = null;
			if ((continuous != null) && (continuous.booleanValue()))
			{
				num = question.getAssessmentOrdering().getPosition();
			}
			else
			{
				num = question.getSectionOrdering().getPosition();
			}
			return num.toString();
		}
	}
}
