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
				.setTitle("home-section-title")
				.add(
					ui.newSection()
						.add(
							ui.newText()
								.setText("home-generate"))
						.add(
							ui.newTextEdit()
								.setProperty(ui.newPropertyReference().setReference("specs.contextsWithAssessments"))
								.setTitle("home-contextsWithAssessments"))
						.add(
							ui.newTextEdit()
								.setProperty(ui.newPropertyReference().setReference("specs.assessmentsPerContext"))
								.setTitle("home-assessmentsPerContext"))
						.add(
							ui.newTextEdit()
								.setProperty(ui.newPropertyReference().setReference("specs.submissionsPerStudent"))
								.setTitle("home-submissionsPerStudent"))
						.add(
							ui.newTextEdit()
								.setProperty(ui.newPropertyReference().setReference("specs.contextStudents"))
								.setTitle("home-contextStudents"))
						.add(
							ui.newTextEdit()
								.setProperty(ui.newPropertyReference().setReference("specs.itemsPerAssessment"))
								.setTitle("home-itemsPerAssessment")))
				.add(
					ui.newButtonBar()
						.add(ui.newNavigation()
							.setSubmit()
							.setTitle("home-generate")
							.setStyle(Navigation.Style.button)));
	}
}
