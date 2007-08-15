/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/muse/mneme/trunk/mneme-tool/tool/src/java/org/muse/mneme/tool/PoolsDeleteView.java $
 * $Id: PoolsDeleteView.java 11097 2007-08-08 15:57:11Z tannirumurthy@foothill.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
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

package org.muse.mneme.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.util.ControllerImpl;
import org.muse.mneme.api.AssessmentService;
import org.muse.mneme.api.AssessmentPermissionException;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.Part;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.util.Web;

/**
 * The pools delete view for the mneme tool.
 */
public class PartsDeleteView extends ControllerImpl {
	
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PartsDeleteView.class);

	/** Test Service */
	protected AssessmentService assessmentService = null;

	/**
	 * Shutdown.
	 */
	public void destroy() {
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(HttpServletRequest req, HttpServletResponse res,Context context, String[] params) throws IOException {
		
		String destination = this.uiService.decode(req, context);
		List<Part> parts = new ArrayList<Part>(0);

		String assessmentId = params[2];

		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null) {
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/"
					+ Errors.invalid)));
			return;
		}

		context.put("assessment", assessment);
		if(params.length > 3 && params[3] != null && params[3].length() !=0)
		{	
			String[] selectedPartIds = params[3].split("\\+");

			for (String selectedPartId : selectedPartIds) {
				if (selectedPartId != null) {
					Part part = assessment.getParts().getPart(selectedPartId);

					if (part == null) {
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req,
								"/error/" + Errors.invalid)));
						return;
					}

					// security check
					if (!assessmentService.allowRemoveAssessment(assessment, null)) {
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req,
								"/error/" + Errors.unauthorized)));
						return;
					}
					parts.add(part);
				}
			}
		}
		context.put("parts", parts);

		// render
		uiService.render(ui, context);
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init() {
		super.init();
		M_log.info("init()");
	}

	/**
	 * @return the assessmentService
	 */
	public AssessmentService getAssessmentService() {
		return this.assessmentService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void post(HttpServletRequest req, HttpServletResponse res,Context context, String[] params) throws IOException {
	
		if (params.length < 4)throw new IllegalArgumentException();

		String destination = this.uiService.decode(req, context);

		String assessmentId = params[2];

		Assessment assessment = assessmentService.getAssessment(assessmentId);
		if (assessment == null) {
			// redirect to error
			res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/"
					+ Errors.invalid)));
			return;
		}

		String[] selectedPartIds = params[3].split("\\+");
		if(selectedPartIds != null && selectedPartIds.length !=0)
		{	
			for (String selectedPartId : selectedPartIds) {
				if (selectedPartId != null) {
					Part part = assessment.getParts().getPart(selectedPartId);

					if (part == null) {
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req,
								"/error/" + Errors.invalid)));
						return;
					}

					// security check
					if (!assessmentService.allowRemoveAssessment(assessment, null)) {
						// redirect to error
						res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req,
								"/error/" + Errors.unauthorized)));
						return;
					}
					// remove part
					assessment.getParts().removePart(part);
				}
			}

			try {
				this.assessmentService.saveAssessment(assessment);
			} catch (AssessmentPermissionException e) {
				// redirect to error
				res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req, "/error/"
						+ Errors.unauthorized)));
				return;
			}
		}
		res.sendRedirect(res.encodeRedirectURL(Web.returnUrl(req,destination)));
	}

	/**
	 * @param assessmentService
	 *            the assessmentService to set
	 */
	public void setAssessmentService(AssessmentService assessmentService) {
		this.assessmentService = assessmentService;
	}
}
