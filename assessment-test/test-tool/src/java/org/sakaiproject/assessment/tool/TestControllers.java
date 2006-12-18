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
	 * The enter interface needs the following entities in the context:
	 * assessment - the selected Assessment object
	 * remainingSubmissions - Integer count of remaining submissions allowed to the current user for the selected assessment
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
							ui.newNavigation()
								.setSubmit()
								.setDefault()
								.setTitle("home-generate")
								.setStyle(Navigation.Style.button))
					);

	}
}
