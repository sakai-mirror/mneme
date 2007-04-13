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

package org.muse.ambrosia.api;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.i18n.InternationalizedMessages;

/**
 * UiService ...
 */
public interface UiService
{
	/*************************************************************************************************************************************************
	 * Component factory methods
	 ************************************************************************************************************************************************/

	/**
	 * Construct a new Alert
	 * 
	 * @return a new Alert
	 */
	Alert newAlert();

	/**
	 * Construct a new Alias
	 * 
	 * @return a new Alias
	 */
	Alias newAlias();

	/**
	 * Construct a new AndDecision
	 * 
	 * @return a new AndDecision
	 */
	AndDecision newAndDecision();

	/**
	 * Construct a new Attachments
	 * 
	 * @return a new Attachments
	 */
	Attachments newAttachments();

	/**
	 * Construct a new AutoColumn
	 * 
	 * @return a new AutoColumn
	 */
	AutoColumn newAutoColumn();

	/**
	 * Construct a new BarChart
	 * 
	 * @return a new BarChart
	 */
	BarChart newBarChart();

	/**
	 * Construct a new BooleanPropertyReference
	 * 
	 * @return a new BooleanPropertyReference
	 */
	BooleanPropertyReference newBooleanPropertyReference();

	/**
	 * Construct a new CompareDecision
	 * 
	 * @return a new CompareDecision
	 */
	CompareDecision newCompareDecision();

	/**
	 * Construct a new ConstantPropertyReference
	 * 
	 * @return a new ConstantPropertyReference
	 */
	ConstantPropertyReference newConstantPropertyReference();

	/**
	 * Construct a new Container
	 * 
	 * @return a new Container
	 */
	Container newContainer();

	/**
	 * Construct a new Context
	 * 
	 * @return a new Context
	 */
	Context newContext();

	/**
	 * Construct a new ContextInfoPropertyReference
	 * 
	 * @return a new ContextInfoPropertyReference
	 */
	ContextInfoPropertyReference newContextInfoPropertyReference();

	/**
	 * Construct a new Controller
	 * 
	 * @return a new Controller
	 */
	Controller newController();

	/**
	 * Construct a new CountdownTimer
	 * 
	 * @return a new CountdownTimer
	 */
	CountdownTimer newCountdownTimer();

	/**
	 * Construct a new DatePropertyReference
	 * 
	 * @return a new DatePropertyReference
	 */
	DatePropertyReference newDatePropertyReference();

	/**
	 * Construct a new Decision
	 * 
	 * @return a new Decision
	 */
	Decision newDecision();

	/**
	 * Construct a new Decoder
	 * 
	 * @return a new Decoder
	 */
	Decoder newDecoder();

	/**
	 * Construct a new Destination
	 * 
	 * @return a new Destination
	 */
	Destination newDestination();

	/**
	 * Construct a new DistributionChart
	 * 
	 * @return a new DistributionChart
	 */
	DistributionChart newDistributionChart();

	/**
	 * Construct a new Divider
	 * 
	 * @return a new Divider
	 */
	Divider newDivider();

	/**
	 * Construct a new DurationPropertyReference
	 * 
	 * @return a new DurationPropertyReference
	 */
	DurationPropertyReference newDurationPropertyReference();

	/**
	 * Construct a new EntityDisplay
	 * 
	 * @return a new EntityDisplay
	 */
	EntityDisplay newEntityDisplay();

	/**
	 * Construct a new EntityList
	 * 
	 * @return a new EntityList
	 */
	EntityList newEntityList();

	/**
	 * Construct a new EntityListColumn
	 * 
	 * @return a new EntityListColumn
	 */
	EntityListColumn newEntityListColumn();

	/**
	 * Construct a new Evaluation
	 * 
	 * @return a new Evaluation
	 */
	Evaluation newEvaluation();

	/**
	 * Construct a new FileUpload
	 * 
	 * @return a new FileUpload
	 */
	FileUpload newFileUpload();

	/**
	 * Construct a new FillIn
	 * 
	 * @return a new FillIn
	 */
	FillIn newFillIn();

	/**
	 * Construct a new Footnote
	 * 
	 * @return a new Footnote
	 */
	Footnote newFootnote();

	/**
	 * Construct a new Gap
	 * 
	 * @return a new Gap
	 */
	Gap newGap();

	/**
	 * Construct a new HasValueDecision
	 * 
	 * @return a new HasValueDecision
	 */
	HasValueDecision newHasValueDecision();

	/**
	 * Construct a new HtmlPropertyReference
	 * 
	 * @return a new HtmlPropertyReference
	 */
	HtmlPropertyReference newHtmlPropertyReference();

	/**
	 * Construct a new UiIconKey
	 * 
	 * @return a new UiIconKey
	 */
	IconKey newIconKey();

	/**
	 * Construct a new IconPropertyReference
	 * 
	 * @return a new IconPropertyReference
	 */
	IconPropertyReference newIconPropertyReference();

	/**
	 * Construct a new Instructions
	 * 
	 * @return a new Instructions
	 */
	Instructions newInstructions();

	/**
	 * Construct a new Interface
	 * 
	 * @return a new Interface
	 */
	Interface newInterface();

	/**
	 * Construct a new Match
	 * 
	 * @return a new Match
	 */
	Match newMatch();

	/**
	 * Construct a new MenuBar
	 * 
	 * @return a new MenuBar
	 */
	MenuBar newMenuBar();

	/**
	 * Construct a new Message
	 * 
	 * @return a new Message
	 */
	Message newMessage();

	/**
	 * Construct a new Navigation
	 * 
	 * @return a new Navigation
	 */
	Navigation newNavigation();

	/**
	 * Construct a new NavigationBar
	 * 
	 * @return a new NavigationBar
	 */
	NavigationBar newNavigationBar();

	/**
	 * Construct a new OrDecision
	 * 
	 * @return a new OrDecision
	 */
	OrDecision newOrDecision();

	/**
	 * Construct a new PastDateDecision
	 * 
	 * @return a new PastDateDecision
	 */
	PastDateDecision newPastDateDecision();

	/**
	 * Construct a new Password
	 * 
	 * @return a new Password
	 */
	Password newPassword();

	/**
	 * Construct a new PropertyColumn
	 * 
	 * @return a new PropertyColumn
	 */
	PropertyColumn newPropertyColumn();

	/**
	 * Construct a new PropertyReference
	 * 
	 * @return a new PropertyReference
	 */
	PropertyReference newPropertyReference();

	/**
	 * Construct a new PropertyRow
	 * 
	 * @return a new PropertyRow
	 */
	PropertyRow newPropertyRow();

	/**
	 * Construct a new Section
	 * 
	 * @return a new Section
	 */
	Section newSection();

	/**
	 * Construct a new Selection
	 * 
	 * @return a new Selection
	 */
	Selection newSelection();

	/**
	 * Construct a new SelectionColumn
	 * 
	 * @return a new SelectionColumn
	 */
	SelectionColumn newSelectionColumn();

	/**
	 * Construct a new Text
	 * 
	 * @return a new Text
	 */
	Text newText();

	/**
	 * Construct a new TextEdit
	 * 
	 * @return a new TextEdit
	 */
	TextEdit newTextEdit();

	/**
	 * Construct a new TextPropertyReference
	 * 
	 * @return a new TextPropertyReference
	 */
	TextPropertyReference newTextPropertyReference();

	/**
	 * Construct a new UrlPropertyReference
	 * 
	 * @return a new UrlPropertyReference
	 */
	UrlPropertyReference newUrlPropertyReference();

	/**
	 * Construct a new UserInfoPropertyReference
	 * 
	 * @return a new UserInfoPropertyReference
	 */
	UserInfoPropertyReference newUserInfoPropertyReference();

	/**
	 * Construct a new Value
	 * 
	 * @return a new Value
	 */
	Value newValue();

	/*************************************************************************************************************************************************
	 * Response handling methods
	 ************************************************************************************************************************************************/

	/**
	 * Decode any input parametes from the request into the context.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param context
	 *        The context.
	 * @return The tool destination as encoded in the request.
	 */
	String decode(HttpServletRequest req, Context context);

	/**
	 * Dispatch the request to a static resource, if it is for one.<br />
	 * A static resource is a request to a path that looks like a pathed file name (with an extension).
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @param context
	 *        The servlet context.
	 * @param prefixes
	 *        A set of prefix strings which identify the paths that may contain resources.
	 * @return true if we dispatched, false if not.
	 */
	boolean dispatchResource(HttpServletRequest req, HttpServletResponse res, ServletContext context, Set<String> prefixes) throws IOException,
			ServletException;

	/**
	 * For an HTTP GET response, start the response and return the context that can be populated and sent into render() to complete the response.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @param messages
	 *        The message bundle.
	 * @param home
	 *        the home destination for the tool (if the destination is not specified in the request).
	 * @return The Context to use for further response processing.
	 */
	Context prepareGet(HttpServletRequest req, HttpServletResponse res, InternationalizedMessages messages, String home) throws IOException;

	/**
	 * For an HTTP POST response, start the response and return the context that can be populated and sent into decode() (then redirect) to complete
	 * the response.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @param messages
	 *        The message bundle.
	 * @param home
	 *        the home destination for the tool (if the destination is not specified in the request).
	 * @return The Context to use for further response processing.
	 */
	Context preparePost(HttpServletRequest req, HttpServletResponse res, InternationalizedMessages messages, String home);

	/**
	 * If the path is missing, redirect the request to either the current destination or the supplied home destination
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @param home
	 *        the home destination for the tool.
	 * @return true if redirected, false if not.
	 * @throws IOException
	 *         from the redirect.
	 */
	boolean redirectToCurrentDestination(HttpServletRequest req, HttpServletResponse res, String home) throws IOException;

	/**
	 * Render the response described in the ui component tree and context. Call prepareGet() first to get started and get the context.
	 * 
	 * @param ui
	 *        The top component of a ui tree.
	 * @param context
	 *        The context.
	 */
	void render(Controller ui, Context context);
}
