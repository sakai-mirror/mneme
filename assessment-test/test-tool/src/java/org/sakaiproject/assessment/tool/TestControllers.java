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

import org.sakaiproject.sludge.api.Controller;
import org.sakaiproject.sludge.api.Navigation;
import org.sakaiproject.sludge.api.UiService;

/**
 * Assessment delivery tool controllers.
 */
public class TestControllers
{
	// Note: do not allow an automatic formatter to format this file! -ggolden

	/**
	 */
	public static Controller constructHome(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("home-title")
				.setHeader("home-header")
				.add(
					ui.newSection()
						.setTitle("home-generate-title")
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("gspecs.contextsWithAssessments"))
								.setTitle("home-contextsWithAssessments"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("gspecs.assessmentsPerContext"))
								.setTitle("home-assessmentsPerContext"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("gspecs.submissionsPerStudent"))
								.setTitle("home-submissionsPerStudent"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("gspecs.contextStudents"))
								.setTitle("home-contextStudents"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("gspecs.itemsPerAssessment"))
								.setTitle("home-itemsPerAssessment"))
						.add(
							ui.newNavigationBar()
								.add(ui.newNavigation()
									.setSubmit()
									.setDestination(ui.newDestination().setDestination("/generate"))
									.setTitle("home-generate")
									.setStyle(Navigation.Style.button)))
						)
				.add(
					ui.newSection()
						.setTitle("home-simulate-title")
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("sspecs.numUsers"))
								.setTitle("home-numUsers"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("sspecs.startGap"))
								.setTitle("home-startGap"))
						.add(
							ui.newTextEdit()
								.setSize(1, 12)
								.setProperty(ui.newPropertyReference().setReference("sspecs.thinkTime"))
								.setTitle("home-thinkTime"))
						.add(
							ui.newNavigationBar()
								.add(ui.newNavigation()
									.setSubmit()
									.setDestination(ui.newDestination().setDestination("/simulate"))
									.setTitle("home-simulate")
									.setStyle(Navigation.Style.button)))
						);
	}

	/**
	 */
	public static Controller constructGenerate(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("generate-title")
				.setHeader("generate-header")
				.add(
					ui.newInstructions().setText(null, ui.newPropertyReference().setReference("rv")))
				.add(
					ui.newNavigationBar()
						.add(ui.newNavigation()
							.setDestination(ui.newDestination().setDestination("/home"))
							.setTitle("return")
							.setStyle(Navigation.Style.button)));
	}

	/**
	 */
	public static Controller constructSimulate(UiService ui)
	{
		return
			ui.newInterface()
				.setTitle("simulate-title")
				.setHeader("simulate-header")
				.add(
					ui.newInstructions().setText(null, ui.newPropertyReference().setReference("rv")))
				.add(
					ui.newNavigationBar()
						.add(ui.newNavigation()
							.setDestination(ui.newDestination().setDestination("/home"))
							.setTitle("return")
							.setStyle(Navigation.Style.button)));
	}
}
