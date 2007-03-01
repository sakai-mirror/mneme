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
import org.sakaiproject.assessment.api.SubmissionExpiration;
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
								.setIterator(ui.newPropertyReference().setReference("assessments"), "assessment")
								.setTitle("list-take-instructions")
								.setEmptyTitle("list-take-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("assessment.title"))
										.setEntityNavigation(
											ui.newEntityNavigation()
												.setDestination(ui.newDestination().setDestination("/enter/{0}", ui.newTextPropertyReference().setReference("assessment.id"))))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("0").setProperty(ui.newPropertyReference().setReference("assessment_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("assessment_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/0A{0}{1}",
													ui.newTextPropertyReference().setReference("submission_sort_choice"),
													ui.newTextPropertyReference().setReference("submission_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/0D{0}{1}",
													ui.newTextPropertyReference().setReference("submission_sort_choice"),
													ui.newTextPropertyReference().setReference("submission_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-take-dueDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setReference("assessment.dueDate")
												.setMissingText("na"))
										.setAlert(
											ui.newPastDateDecision()
												.setProperty(
													ui.newDatePropertyReference()
														.setReference("assessment.dueDate")))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("1").setProperty(ui.newPropertyReference().setReference("assessment_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("assessment_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/1A{0}{1}",
													ui.newTextPropertyReference().setReference("submission_sort_choice"),
													ui.newTextPropertyReference().setReference("submission_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/1D{0}{1}",
													ui.newTextPropertyReference().setReference("submission_sort_choice"),
													ui.newTextPropertyReference().setReference("submission_sort_ad"))))))
				.add(
					ui.newSection()
						.setTitle("list-review-section-title")
						.add(
							ui.newEntityList()
								.setStyle(EntityList.Style.flat)
								.setIterator(ui.newPropertyReference().setReference("submissions"), "submission")
								.setTitle("list-review-instructions")
								.setEmptyTitle("list-review-empty")
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("submission.assessment.title"))
										.setEntityNavigation(
											ui.newEntityNavigation()
												.setDestination(ui.newDestination().setDestination("/review/{0}", ui.newTextPropertyReference().setReference("submission.id")))
												.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.feedbackNow"))))
//										.addNavigation(
//											ui.newNavigation()
//												.setTitle("list-review-statistics")
//												.setStyle(Navigation.Style.link)
//												.setDestination(ui.newDestination().setDestination("/statistics/{0}", ui.newPropertyReference().setReference("submission.id")))
//												.setIncluded(
//													ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.feedbackNow")),
//													ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.feedbackShowStatistics"))))
										.setSorting(
											ui.newCompareDecision().setEqualsConstant("0").setProperty(ui.newPropertyReference().setReference("submission_sort_choice")),
											ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}0A",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}0D",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-feedbackDate")
										.setProperty(
											ui.newDatePropertyReference()
												.setReference("submission.assessment.feedbackDate")
												.setMissingText("immediate"))
										.setIncluded(ui.newDecision().setDelegate(new FeedbackDateDecision()), "na")
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("1").setProperty(ui.newPropertyReference().setReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}1A",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}1D",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-score")
										.setProperty(
											ui.newTextPropertyReference()
											 	.setReference("submission.totalScore"))
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
												ui.newCompareDecision().setEqualsConstant("2").setProperty(ui.newPropertyReference().setReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}2A",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}2D",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-time")
										.setProperty(
											ui.newDurationPropertyReference()
												.setReference("submission.elapsedTime")
												.setMissingText("na"))
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("3").setProperty(ui.newPropertyReference().setReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}3A",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}3D",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad"))))
								.addColumn(
									ui.newPropertyColumn()
										.setTitle("list-review-submittedDue")
										.setProperty(
											ui.newDatePropertyReference()
												.setReference("submission.submittedDate")
												.setMissingText("na"))
										.setSorting(
												ui.newCompareDecision().setEqualsConstant("4").setProperty(ui.newPropertyReference().setReference("submission_sort_choice")),
												ui.newCompareDecision().setEqualsConstant("A").setProperty(ui.newPropertyReference().setReference("submission_sort_ad")))
										.setSortIcons("/icons/sortascending.gif", ui.newMessage().setMessage("asc"), "/icons/sortdescending.gif", ui.newMessage().setMessage("desc"))
										.setSortDestination(
											ui.newDestination() /* a */
												.setDestination(
													"/list/{0}{1}4A",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad")),
											ui.newDestination() /* d */
												.setDestination(
													"/list/{0}{1}4D",
													ui.newTextPropertyReference().setReference("assessment_sort_choice"),
													ui.newTextPropertyReference().setReference("assessment_sort_ad"))))));
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
				.setTitle("enter-title", ui.newTextPropertyReference().setReference("assessment.title"))
				.setHeader("enter-header")
				.add(
					ui.newSection()
						.add(
							ui.newInstructions()
								.setText("linear-instructions")
								.setIncluded(
									ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("assessment.randomAccess")))))
				.add(
					ui.newSection()
						.setTitle("enter-display-instructions", ui.newPropertyReference().setReference("assessment.title"))
						.add(
							ui.newText()
								.setText(null, ui.newHtmlPropertyReference().setReference("assessment.description")))
						.add(
							ui.newAttachments()
								.setTitle("enter-attachments")
								.setAttachments(ui.newPropertyReference().setReference("assessment.attachments"), null)
								.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("assessment.attachments"))))
						.add(
							ui.newEntityDisplay()
								.setEntityReference(ui.newPropertyReference().setReference("assessment"))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-contextTitle")
										.setProperty(
											ui.newContextInfoPropertyReference()
												.setSelector(ContextInfoPropertyReference.Selector.title)
												.setReference("assessment.context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setReference("assessment.createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("assessment.title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-timeLimit")
										.setProperty(
											ui.newDurationPropertyReference()
												.setReference("assessment.timeLimit")
												.setMissingText("no-time-limit")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-autoSubmit")
										.setProperty(
											ui.newBooleanPropertyReference()
												.setText("enabled", "disabled")
												.setReference("assessment.autoSubmit")
												.setMissingText("unknown"))
										.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("assessment.timeLimit"))))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-numSubmissionsAllowed")
										.setProperty(
											ui.newTextPropertyReference()
												.setFormat("submissions")
												.setReference("assessment.numSubmissionsAllowed")
												.setMissingText("unlimited")
												.addProperty(
													ui.newTextPropertyReference()
														.setReference("remainingSubmissions"))))
								.addRow(
									ui.newPropertyRow()
										.setTitle("enter-display-feedback")
										.setProperty(
											ui.newPropertyReference()
												.setFormatDelegate(new FeedbackPropertyReference())
												.setReference("assessment.feedbackDelivery")))))
					.add(
						ui.newNavigationBar()
							.add(
								ui.newNavigation()
									.setSubmit()
									.setTitle("begin")
									.setIcon("/icons/begin.gif",Navigation.IconStyle.left)
									.setAccessKey("begin-access")
									.setDescription("begin-description")
									.setStyle(Navigation.Style.button))
							.add(
								ui.newNavigation()
									.setDefault()
									.setTitle("cancel")
									.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
									.setAccessKey("cancel-access")
									.setDescription("cancel-description")
									.setStyle(Navigation.Style.button)
									.setDestination(ui.newDestination().setDestination("/list")))
							.setId("nav"));
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
	 * finishReady - if finish is allowed to submit
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
				.add(ui.newAlias().setTo("nav"))
				.add(
					ui.newSection()
						.add(
							ui.newInstructions()
								.setText("linear-instructions")
								.setIncluded(
									ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
									ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")))))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-duration", ui.newDurationPropertyReference().setConcise().setReference("submission.expiration.limit"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.timeLimit.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-due", ui.newDatePropertyReference().setReference("submission.expiration.time"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.closedDate.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newSection()
						.setTitle("question-total-score", ui.newTextPropertyReference().setReference("submission").setFormatDelegate(new SubmissionScore(true)))
//						.add(
//							ui.newDistributionChart()
//								.setData(ui.newPropertyReference().setReference("submission.assessment.scores"))
//								.setWidth(44)
//								.setHeight(22)
//								.setClump(5)
//								.setMark(ui.newPropertyReference().setReference("submission.totalScore"))
//								.setMax(ui.newPropertyReference().setReference("submission.assessment.totalPoints")))
						.add(
							ui.newSection()
								.setTitle("question-eval-overall-comment-title")
								.add(
									ui.newEvaluation()
										.setIcon("/icons/note.png", "comments")
										.setText("question-eval-overall-comment", ui.newTextPropertyReference().setReference("submission.evalComment")))
								.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.evalComment"))))
						.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review"))))
				.add(
					ui.newSection()
						.setIterator(
							ui.newPropertyReference().setReference("answers"), "answer")
						.setTitle("question-section-title",
							ui.newTextPropertyReference().setReference("answer.question.section.ordering.position"),
							ui.newTextPropertyReference().setReference("answer.question.section.assessment.numSections"),
							ui.newTextPropertyReference().setReference("answer.question.section.title"),
							ui.newPropertyReference().setReference("answer.question.section").setFormatDelegate(new SectionScore(true)))
						.setAnchor("question-anchor", ui.newPropertyReference().setReference("answer.question.id"))
						.setTitleIncluded(
							ui.newAndDecision()
								.setRequirements(
									ui.newOrDecision()
										.setOptions(
											ui.newHasValueDecision()
												.setProperty(
													ui.newPropertyReference()
														.setReference("question")),
											ui.newDecision()
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.sectionOrdering.isFirst"))),
									ui.newCompareDecision()
										.setEqualsConstant("Default")
										.setReversed()
										.setProperty(ui.newPropertyReference().setReference("answer.question.section.title"))))
						.add(
							ui.newText()
								.setText(null, ui.newHtmlPropertyReference().setReference("answer.question.section.description")))
						.add(
							ui.newAttachments()
								.setTitle("enter-attachments")
								.setAttachments(ui.newPropertyReference().setReference("answer.question.section.attachments"), null)
								.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.attachments"))))
						.add(
							ui.newSection()
								.setTitle(null,ui.newTextPropertyReference().setReference("answer.question").setFormatDelegate(new FormatQuestionTitle()))
//								.add(
//									ui.newDistributionChart()
//										.setData(ui.newPropertyReference().setReference("answer.question.scores"))
//										.setWidth(44)
//										.setHeight(22)
//										.setClump(5)
//										.setMark(ui.newPropertyReference().setReference("answer.totalScore"))
//										.setMax(ui.newPropertyReference().setReference("answer.question.points"))
//										.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review"))))
								.add(
									ui.newText()
										.setText(null, ui.newHtmlPropertyReference().setReference("answer.question.instructions"))
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(
													QuestionType.matching.toString())
												.setProperty(
													ui.newPropertyReference().setReference("answer.question.type"))))
								.add(
									ui.newText()
										.setText(null, ui.newHtmlPropertyReference().setReference("answer.question.part.title"))
										.setIncluded(
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
										.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("answer.question.attachments"))))
								.add(
									ui.newEntityList()
										.setStyle(EntityList.Style.form)
										.setIterator(
											ui.newPropertyReference().setReference("answer.question.part.answers"), "qanswer")
										.addColumn(
											ui.newPropertyColumn()
												.setProperty(null, ui.newHtmlPropertyReference().setFormatDelegate(new FormatAnswerCorrectFeedback()))
												.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")), null))
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
														.setReference("qanswer.id"))
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.entryAnswerIds"))
												.setOnEmptyAlert(
													ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
													"question-select-on-empty-alert")
												.setLabel("question-label",
													ui.newTextPropertyReference()
														.setReference("qanswer.label")))
										.addColumn(
											ui.newPropertyColumn()
												.setProperty(
													ui.newHtmlPropertyReference()
														.setReference("qanswer.text")))
										.addColumn(
											ui.newPropertyColumn()
												.setProperty(
													ui.newHtmlPropertyReference()
														.setReference("qanswer.feedbackGeneral"))
												.setIncluded(ui.newDecision().setDelegate(new AnswerFeedbackDecision()), null))
										.setIncluded(
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
										.setOnEmptyAlert(
											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
											"question-essay-on-empty-alert")
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setSize(20, 80)
										.setProperty(
											ui.newPropertyReference()
												.setReference("answer.entryAnswerText"))
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFileUpload()
										.setOnEmptyAlert(
											ui.newAndDecision().setRequirements(
												ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
												ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("answer.entryAnswerAttachments"))),
											"question-upload-on-empty-alert")
										.setTitle("question-upload-title")
										.setTitleIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed())
										.setUpload("quesiton-upload-upload")
										.setProperty(ui.newPropertyReference().setReference("answer.uploadFile"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fileUpload.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newAttachments()
										.setAttachments(
											ui.newPropertyReference().setReference("answer.entryAnswerAttachments"), "attachment")
										.setSize(false)
										.setTimestamp(true)
										.addNavigation(
											ui.newNavigation()
												.setTitle("question-upload-remove")
												.setStyle(Navigation.Style.link)
												.setSubmit()
												.setDestination(ui.newDestination().setDestination("/remove/{0}/{1}{2}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("answer.question.id"),
													ui.newTextPropertyReference().setReference("attachment.reference")))
												.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed()))
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fileUpload.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFillIn()
										.setOnEmptyAlert(
											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
											"question-fillin-on-empty-alert")
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.part.title"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setProperty(
											ui.newPropertyReference()
												.setReference("answer.entryAnswerTexts"))
										.setCorrectMarker(
											ui.newPropertyReference()
												.setReference("answer.entryCorrects"),
											"/icons/correct.gif",
											"question-correct",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setWidth(20)
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.fillIn.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newFillIn()
										.setOnEmptyAlert(
											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
											"question-fillin-on-empty-alert")
										.setText(null, ui.newTextPropertyReference().setReference("answer.question.part.title"))
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setProperty(
											ui.newPropertyReference()
												.setReference("answer.entryAnswerTexts"))
										.setCorrectMarker(
											ui.newPropertyReference()
												.setReference("answer.entryCorrects"),
											"/icons/correct.gif",
											"question-correct",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setWidth(10)
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.numeric.toString())
												.setProperty(
													ui.newPropertyReference()
														.setReference("answer.question.type"))))
								.add(
									ui.newMatch()
										.setOnEmptyAlert(
											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
											"question-match-on-empty-alert")
										.setReadOnly(ui.newPropertyReference().setReference("review"))
										.setProperty(
											ui.newPropertyReference()
												.setReference("answer.entryAnswerIds"))
										.setCorrectMarker(
											ui.newPropertyReference()
												.setReference("answer.entryCorrects"),
											"/icons/correct.gif",
											"question-correct",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowCorrectAnswer")))
										.setFeedback(
											ui.newPropertyReference()
												.setReference("answer.answerFeedbacks"),
											"question-match-answer-feedback",
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowAnswerFeedback")))
										.setSelectText("question-select")
										.setParts(
											ui.newPropertyReference()
												.setReference("answer.question.parts"))
										.setPartsChoices(
											ui.newPropertyReference()
												.setReference("part.answers"))
										.setPartsTitle(
											ui.newPropertyReference()
												.setReference("part.title"))
										.setChoiceId(
											ui.newPropertyReference()
												.setReference("choice.id"))
										.setChoiceText(
											ui.newPropertyReference()
												.setReference("choice.text"))
										.setChoiceLabel(
											ui.newPropertyReference()
												.setReference("choice.label"))
										.setIncluded(
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
												.setReference("answer.rationale"))
										.setSize(5, 40)
//										.setOnEmptyAlert(
//											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")),
//											"question-rationale-on-empty-alert")
										.setIncluded(
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
												.setReference("answer.markedForReview"))
										.setIncluded(
											ui.newDecision().setProperty(ui.newBooleanPropertyReference().setReference("answer.question.section.assessment.randomAccess")),
											ui.newOrDecision().setOptions(
												ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed(),
												ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.markedForReview")))))
								.add(
									ui.newText()
										.setText("question-answer-key", ui.newHtmlPropertyReference().setReference("answer.question.answerKey"))
										.setIncluded(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
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
									ui.newText()
										.setText("question-feedback", ui.newHtmlPropertyReference().setReference("answer.questionFeedback"))
										.setIncluded(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowQuestionFeedback"))))
								.add(
									ui.newText()
										.setText("question-model-answer", ui.newHtmlPropertyReference().setReference("answer.question.part.answer.text"))
										.setIncluded(
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("feedback")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackNow")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("answer.question.section.assessment.feedbackShowQuestionFeedback")),
											ui.newCompareDecision()
												.setEqualsConstant(QuestionType.essay.toString())
												.setProperty(ui.newPropertyReference().setReference("answer.question.type"))))
								.add(
									ui.newSection()
										.add(
											ui.newEvaluation()
												.setIcon("/icons/note.png", "comments")
												.setText("question-eval-question-comment", ui.newTextPropertyReference().setReference("answer.evalComment")))
										.setIncluded(
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")),
											ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("answer.evalComment"))))))
				.add(ui.newGap())
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setTitle("prev")
								.setAccessKey("prev-access")
								.setDescription("review-prev-description")
								.setIcon("/icons/prev.gif",Navigation.IconStyle.left)
								.setStyle(Navigation.Style.button)
								.setDisabled(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true"))))
						.add(
								ui.newNavigation()
								.setTitle("return")
								.setAccessKey("return-access")
								.setDescription("return-description")
								.setIcon("/icons/return.png",Navigation.IconStyle.left)
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						.add(
							ui.newNavigation()
								.setTitle("next")
								.setAccessKey("next-access")
								.setDescription("next-description")
								.setIcon("/icons/next.gif",Navigation.IconStyle.right)
								.setStyle(Navigation.Style.button)
								.setDisabled(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true"))))
						.setIncluded(
							ui.newDecision()
								.setProperty(
									ui.newPropertyReference()
										.setReference("review")))
						.setId("nav"))
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setSubmit()
								.setIcon("/icons/prev.gif",Navigation.IconStyle.left)
								.setTitle("prev")
								.setAccessKey("prev-access")
								.setDescription("prev-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("question.assessmentOrdering.previous.id")))
								.setDisabled(
									ui.newOrDecision()
										.setOptions(
											ui.newDecision()
												.setProperty(
													ui.newBooleanPropertyReference()
														.setReference("question.assessmentOrdering.isFirst")),
											ui.newDecision()
												.setReversed()
												.setProperty(
													ui.newPropertyReference()
														.setReference("submission.assessment.randomAccess"))))
								.setIncluded(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("question"))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setIcon("/icons/prev.gif",Navigation.IconStyle.left)
								.setTitle("prev")
								.setAccessKey("prev-access")
								.setDescription("prev-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("section.ordering.previous.id")))
								.setDisabled(
									ui.newOrDecision()
										.setOptions(
											ui.newDecision()
												.setProperty(
													ui.newBooleanPropertyReference()
														.setReference("section.ordering.isFirst")),
											ui.newDecision()
												.setReversed()
												.setProperty(
													ui.newPropertyReference()
														.setReference("submission.assessment.randomAccess"))))
								.setIncluded(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("section"))))	
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("prev")
								.setAccessKey("prev-access")
								.setDescription("prev-description")
								.setIcon("/icons/prev.gif",Navigation.IconStyle.left)
								.setStyle(Navigation.Style.button)
								.setDisabled(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true")))
								.setIncluded(
									ui.newAndDecision().setRequirements(
										ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("question")),
										ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("section")))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("toc")
								.setAccessKey("toc-access")
								.setDescription("toc-description")
								.setIcon("/icons/contents.gif",Navigation.IconStyle.left)
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/toc/{0}", ui.newTextPropertyReference().setReference("submission.id")))
								.setDisabled(
									ui.newDecision()
										.setReversed()
										.setProperty(
											ui.newPropertyReference()
												.setReference("submission.assessment.randomAccess"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setValidation(ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")))
								.setTitle("next")
								.setAccessKey("next-access")
								.setDescription("next-description")
								.setIcon("/icons/next.gif",Navigation.IconStyle.right)
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("question.assessmentOrdering.next.id")))
								.setDisabled(
									ui.newDecision()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("question.assessmentOrdering.isLast")))
								.setIncluded(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("question"))))
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("next")
								.setAccessKey("next-access")
								.setDescription("next-description")
								.setIcon("/icons/next.gif",Navigation.IconStyle.right)
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("section.ordering.next.id")))
								.setDisabled(
									ui.newDecision()
										.setProperty(
											ui.newBooleanPropertyReference()
												.setReference("section.ordering.isLast")))
								.setIncluded(
									ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("section"))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("next")
								.setAccessKey("next-access")
								.setDescription("next-description")
								.setIcon("/icons/next.gif",Navigation.IconStyle.right)
								.setStyle(Navigation.Style.button)
								.setDisabled(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true")))
								.setIncluded(
									ui.newAndDecision().setRequirements(
										ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("question")),
										ui.newHasValueDecision().setReversed().setProperty(ui.newPropertyReference().setReference("section")))))
						.add(
							ui.newDivider())
						.add(
							ui.newNavigation()
								.setSubmit()
								.setIcon("/icons/exit.gif",Navigation.IconStyle.left)
								.setTitle("save-exit")
								.setDescription("save-exit-description")
								.setAccessKey("save-exit-access")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setAccessKey("finish-exam-access")
								.setDescription("finish-exam-description")
								.setIcon("/icons/finish.gif",Navigation.IconStyle.left)
								.setTitle("finish-exam")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/final_review/{0}",ui.newTextPropertyReference().setReference("submission.id")))
								.setIncluded(
									ui.newAndDecision().setRequirements(
										ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("finishReady")),
										ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setTitle("finish-exam")
								.setAccessKey("finish-exam-access")
								.setDescription("finish-exam-description")
								.setIcon("/icons/finish.gif",Navigation.IconStyle.left)
								.setConfirm(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true")))
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/submitted/{0}",ui.newTextPropertyReference().setReference("submission.id")))
								.setIncluded(
									ui.newOrDecision().setOptions(
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("finishReady")),
											ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")))))
						.add(
							ui.newNavigation()
								.setSubmit()
								.setRight()
								.setTitle("question-link-feedback")
								.setStyle(Navigation.Style.link)
								.setDestination(ui.newDestination().setDestination("/question/{0}/{1}/feedback",
									ui.newTextPropertyReference().setReference("submission.id"),
									ui.newTextPropertyReference().setReference("questionSelector")))
								.setIncluded(
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("submission"))
										.setDelegate(new ShowFeedbackChoiceDecision()),
									ui.newDecision()
										.setProperty(
											ui.newPropertyReference()
												.setReference("review"))
											.setReversed()))
						.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("review")).setReversed())
						.setId("nav"));
	}

	/**
	 * The exit interface needs the following entities in the context:
	 * submission - the completed submission
	 * remainingSubmissions - Integer count of remaining submissions allowed to the current user for the selected assessment
	 */
	public static Controller constructSubmitted(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("submitted-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.setHeader("submitted-header")
				.add(
					ui.newSection()
						.setTitle("submitted-section-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
						.add(ui.newInstructions().setText("submitted-display-instructions"))
						.add(
							ui.newInstructions()
								.setText("submitted-display-message", ui.newPropertyReference().setReference("submission.assessment.submitMessage"))
								.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.submitMessage"))))
						.add(
							ui.newInstructions()
								.setText("submitted-final-url", ui.newUrlPropertyReference().setReference("submission.assessment.submitUrl"))
								.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.submitUrl"))))
						.add(
							ui.newEntityDisplay()
								.setEntityReference(ui.newPropertyReference().setReference("submission"))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-contextTitle")
										.setProperty(
											ui.newContextInfoPropertyReference()
												.setSelector(ContextInfoPropertyReference.Selector.title)
												.setReference("submission.assessment.context")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-createdBy")
										.setProperty(
											ui.newUserInfoPropertyReference()
												.setSelector(UserInfoPropertyReference.Selector.displayName)
												.setReference("submission.assessment.createdBy")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-title")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("submission.assessment.title")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-numSubmissionsRemaining")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("submission.assessment.numSubmissionsAllowed")
												.setFormat("submitted-submissions-remaining")
												//.setMissingValues("-1")
												.setMissingText("unlimited")
												.addProperty(
														ui.newTextPropertyReference()
															.setReference("remainingSubmissions"))))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-confirmation")
										.setProperty(
											ui.newTextPropertyReference()
												.setReference("submission.confirmation")))
								.addRow(
									ui.newPropertyRow()
										.setTitle("submitted-display-submitted")
										.setProperty(
											ui.newDatePropertyReference()
												.setReference("submission.submittedDate")))))
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setTitle("return")
								.setAccessKey("return-access")
								.setDescription("return-description")
								.setIcon("/icons/return.png",Navigation.IconStyle.left)
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						.setId("nav"));
	}

	/**
	 * The toc interface needs the following entities in the context:
	 * submission - the completed submission
	 * finalReview - if we are in final review mode
	 */
	public static Controller constructToc(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("toc-title", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.setHeader("toc-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(ui.newAlias().setTo("nav"))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-duration", ui.newDurationPropertyReference().setConcise().setReference("submission.expiration.limit"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.timeLimit.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-due", ui.newDatePropertyReference().setReference("submission.expiration.time"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.closedDate.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newAlert()
						.setText("final-instructions")
						.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("finalReview"))))
				.add(
					ui.newSection()
						.setTitle("toc-section-title",
							// {0} - {1}/{2} Answered Questions, {3} Points
							ui.newPropertyReference().setReference("submission.assessment.title"),
							ui.newPropertyReference().setReference("submission.assessment").setFormatDelegate(new QuestionsAnswered()),
							ui.newPropertyReference().setReference("submission.assessment.numQuestions"),
							ui.newPropertyReference().setReference("submission").setFormatDelegate(new SubmissionScore(false)))
					.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess"))))
				.add(
					ui.newSection()
						.add(
							ui.newEntityList()
								.setStyle(EntityList.Style.form)
								.setIterator(
									ui.newPropertyReference().setReference("submission.assessment.questions"), "question")
								.addHeading(
									ui.newAndDecision()
										.setRequirements(
											ui.newCompareDecision()
												.setEqualsConstant("Default")
												.setReversed()
												.setProperty(ui.newPropertyReference().setReference("question.section.title")),
											ui.newDecision().setProperty(ui.newPropertyReference().setReference("question.sectionOrdering.isFirst"))),
									"toc-questions-title",
									// Part{0} - {1} - {2}/{3} Answered Questions, {4} Points
									ui.newPropertyReference().setReference("question.section.ordering.position"),
									ui.newPropertyReference().setReference("question.section.title"),
									ui.newPropertyReference().setFormatDelegate(new QuestionsAnswered()),
									ui.newPropertyReference().setReference("question.section.numQuestions"),
									ui.newPropertyReference().setReference("question.section").setFormatDelegate(new SectionScore(false)))
								.addColumn(
									ui.newPropertyColumn()
										.setProperty(null, ui.newHtmlPropertyReference().setFormatDelegate(new FormatQuestionDecoration()))
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
											ui.newPropertyReference().setFormatDelegate(new QuestionScore(false)))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/q questionId
												.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setIncluded(
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
											ui.newPropertyReference().setFormatDelegate(new QuestionScore(false)))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/s sectionId
												.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.section.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setIncluded(
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
											ui.newPropertyReference().setFormatDelegate(new QuestionScore(false)))
										.setEntityNavigation(
											ui.newEntityNavigation()
												// destination is /question/sid/a
												.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
													ui.newTextPropertyReference().setReference("submission.id"),
													ui.newTextPropertyReference().setReference("question.id"))))
										.setIncluded(
											ui.newCompareDecision()
												.setEqualsConstant(QuestionPresentation.BY_ASSESSMENT.toString())
												.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))))
					.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess"))))
				.add(
					ui.newSection()
						.add(
							ui.newIconKey()
								.setTitle("toc-key-title")
								.addIcon("/icons/unanswered.png", ui.newMessage().setMessage("toc-key-unanswered"))
								.addIcon("/icons/markedforreview.png", ui.newMessage().setMessage("toc-key-mark-for-review"))
								.addIcon("/icons/missingrationale.png", ui.newMessage().setMessage("toc-key-rationale"))
							.setIncluded(ui.newDecision().setProperty(ui.newPropertyReference().setReference("submission.assessment.randomAccess")))))
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setIcon("/icons/exit.gif",Navigation.IconStyle.left)
								.setTitle("save-exit")
								.setDescription("save-exit-description")
								.setAccessKey("save-exit-access")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/list")))
						.add(
							ui.newNavigation()
								.setTitle("finish-exam")
								.setDescription("finish-exam-description")
								.setIcon("/icons/finish.gif",Navigation.IconStyle.left)
								.setTitle("finish-exam")
								.setSubmit()
								.setConfirm(ui.newDecision().setProperty(ui.newConstantPropertyReference().setValue("true")))
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id"))))
						.setId("nav"));
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
				.setHeader("remove-header", ui.newTextPropertyReference().setReference("submission.assessment.title"))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-duration", ui.newDurationPropertyReference().setConcise().setReference("submission.expiration.limit"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.timeLimit.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newCountdownTimer()
						//.setHideMessage("timer-hide")
						//.setShowMessage("timer-show")
						.setDurationMessage("timer-due", ui.newDatePropertyReference().setReference("submission.expiration.time"))
						.setRemainingMessage("timer-remaining")
						.setDuration(ui.newPropertyReference().setReference("submission.expiration.limit"))
						.setTimeTillExpire(ui.newPropertyReference().setReference("submission.expiration.duration"))
						.setExpireDestination(
							ui.newDestination().setDestination("/submitted/{0}", ui.newTextPropertyReference().setReference("submission.id")))
						.setIncluded(
							ui.newDecision().setReversed().setProperty(ui.newPropertyReference().setReference("review")),
							ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("submission.expiration")),
							ui.newCompareDecision().setEqualsConstant(SubmissionExpiration.Cause.closedDate.toString()).setProperty(ui.newPropertyReference().setReference("submission.expiration.cause"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert()
								.setText("remove-confirm"))
						.add(
							ui.newAttachments()
								.setAttachments(ui.newPropertyReference().setReference("attachment"), null)
								.setSize(false)
								.setTimestamp(true)))
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/q{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_QUESTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"))	
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/s{1}#{2}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.section.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_SECTION.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"))		
				.add(
					ui.newNavigationBar()
						.add(
							ui.newNavigation()
								.setDefault()
								.setSubmit()
								.setTitle("remove")
								.setIcon("/icons/remove.gif",Navigation.IconStyle.left)
								.setAccessKey("remove-access")
								.setDescription("remove-upload-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.add(
							ui.newNavigation()
								.setTitle("cancel")
								.setIcon("/icons/cancel.gif",Navigation.IconStyle.left)
								.setAccessKey("cancel-access")
								.setDescription("remove-cancel-description")
								.setStyle(Navigation.Style.button)
								.setDestination(ui.newDestination().setDestination("/question/{0}/a#{1}",
									ui.newPropertyReference().setReference("submission.id"),
									ui.newPropertyReference().setReference("question.id"))))
						.setIncluded(
								ui.newCompareDecision()
									.setEqualsConstant(QuestionPresentation.BY_ASSESSMENT.toString())
									.setProperty(ui.newPropertyReference().setReference("submission.assessment.questionPresentation")))
						.setId("nav"));
	}

	/**
	 * The enter interface needs the following entities in the context:
	 * invalidUrl - indicates that we had an invalid URL, and what it was
	 * inavlidPost - if set, indicates that we had a post.
	 * unauthorized - if set, indicates that we had an unauthorized request.
	 * testUrl if we have another re-entry point, in the test in progress.
	 */
	public static Controller constructError(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("error-title")
				.setHeader("error-header")
				.add(
					ui.newSection()
						.add(
							ui.newAlert().setText("error-invalid-url-alert", ui.newPropertyReference().setReference("invalidUrl")))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("invalidUrl"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert().setText("error-invalid-post"))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("invalidPost"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert().setText("error-unauthorized"))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("unauthorized"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert().setText("error-unexpected"))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("unexpected"))))
				.add(
					ui.newSection()
						.add(
							ui.newAlert().setText("error-upload", ui.newPropertyReference().setReference("uploadMax")))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("upload"))))
				.add(
					ui.newSection()
						.add(
							ui.newInstructions().setText("error-recovery-test"))
						.add(
							ui.newNavigation()
								.setDestination(ui.newDestination().setDestination("{0}", ui.newPropertyReference().setReference("testUrl")))
								.setDefault()
								.setStyle(Navigation.Style.button)
								.setTitle("enter"))
						.setIncluded(ui.newHasValueDecision().setProperty(ui.newPropertyReference().setReference("testUrl"))))
				.add(
					ui.newSection()
						.add(
							ui.newInstructions().setText("error-recovery"))
						.add(
							ui.newNavigation()
								.setDestination(ui.newDestination().setDestination("/list"))
								.setDefault()
								.setStyle(Navigation.Style.button)
								.setTitle("return")));
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
	 * From a value which is an AssessmentSection (or Assessment), 'format' this into a value<br />
	 * that is the number of SubmissionAnswers in the "submission" that are to questions in this section (or assessment) that are answered.<br />
	 */
	public static class QuestionsAnswered implements FormatDelegate
	{
		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;

			Object o = context.get("submission");
			if (!(o instanceof Submission)) return value.toString();
			Submission submission = (Submission) o;

			// if focused on a section, we pick only that section's questions, else we use them all
			AssessmentSection section = null;
			if (value instanceof AssessmentSection)
			{
				section = (AssessmentSection) value;
			}

			// count the questions answered
			int count = 0;

			// find the section's answers to AssessmentQuestions that are in this section and are considered answered.
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if ((section == null || answer.getQuestion().getSection().equals(section)) && answer.getIsAnswered().booleanValue())
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
		/** If set, we need the "review" context variable set to show the score. */
		protected boolean needReview = false;

		public SectionScore(boolean needReview)
		{
			this.needReview = needReview;
		}

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

			Boolean review = (Boolean) context.get("review");

			// if we are doing feedback just now, and if we are needing review and it's set
			if (assessment.getFeedbackNow() && (!this.needReview || ((review != null) && review.booleanValue())))
			{
				// if we are doing score feedback
				if (assessment.getFeedbackShowScore().booleanValue())
				{
					// add the sum of scores for any answered question in this section
					float score = 0;

					// find the section's answers to AssessmentQuestions that are in this section.
					for (SubmissionAnswer answer : submission.getAnswers())
					{
						if (answer.getQuestion().getSection().equals(section))
						{
							score += answer.getTotalScore().floatValue();
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
	 * From a value which is an Submission, 'format' this into a value<br />
	 * that is the total score of the submission, if feedback is set, followed by the total points of the submission.
	 */
	public static class SubmissionScore implements FormatDelegate
	{
		/** If set, we need the "review" context variable set to show the score. */
		protected boolean needReview = false;

		public SubmissionScore(boolean needReview)
		{
			this.needReview = needReview;
		}

		/**
		 * {@inheritDoc}
		 */
		public String format(Context context, Object value)
		{
			if (value == null) return null;
			if (!(value instanceof Submission)) return value.toString();
			Submission submission = (Submission) value;

			Assessment assessment = submission.getAssessment();
			if (assessment == null) return value.toString();

			// use the {}/{} format if doing feedback, or just {} if not.
			StringBuffer rv = new StringBuffer();

			Boolean review = (Boolean) context.get("review");

			// if we are doing feedback just now, and if we are needing review and it's set
			if (assessment.getFeedbackNow() && (!this.needReview || ((review != null) && review.booleanValue())))
			{
				// if we are doing score feedback
				if (assessment.getFeedbackShowScore().booleanValue())
				{
					// the total score
					Float score = submission.getTotalScore();

					rv.append(score.toString());
					rv.append('/');
				}
			}

			// add the total possible points for the assessment
			rv.append(submission.getAssessment().getTotalPoints().toString());

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
		/** If set, we need the "review" context variable set to show the score. */
		protected boolean needReview = false;

		public QuestionScore(boolean needReview)
		{
			this.needReview = needReview;
		}

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

			Boolean review = (Boolean) context.get("review");

			// if we are doing feedback just now, and if we are needing review and it's set
			if (assessment.getFeedbackNow() && (!this.needReview || ((review != null) && review.booleanValue())))
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
							score = answer.getTotalScore().floatValue();
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
	 * for 'unanswerd' or 'mark for review' or 'rationale empty' for the related submission question.
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
			boolean missingRationale = false;
			for (SubmissionAnswer answer : submission.getAnswers())
			{
				if (answer.getQuestion().equals(question))
				{
					answered = answer.getIsAnswered();
					markForReview = answer.getMarkedForReview().booleanValue();
					missingRationale = ((question.getRequireRationale() != null) && (question.getRequireRationale().booleanValue()) && (StringUtil
							.trimToNull(answer.getRationale()) == null));
					break;
				}
			}

			// if not found, or not answered, use the unanswered icon
			if (!answered)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/unanswered.png\" alt=\""
						+ context.getMessages().getString("toc-key-unanswered") + "\" />";
			}

			// if mark for review, use that icon
			else if (markForReview)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/markedforreview.png\" alt=\""
						+ context.getMessages().getString("toc-key-mark-for-review") + "\" />";
			}

			// if rationale is needed and not present, use the no-rationale icon
			else if (missingRationale)
			{
				return "<img src=\"" + context.get("sakai.return.url") + "/icons/missingrationale.png\" alt=\""
						+ context.getMessages().getString("toc-key-rationale") + "\" />";
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

			// use the QuestionScore formater to get the points with possible score
			QuestionScore qs = new QuestionScore(true);
			args[2] = qs.format(context, value);

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
