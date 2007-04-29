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

package org.muse.mneme.tool;

import org.sakaiproject.util.StringUtil;

public class GenerateSpecs
{
	protected Integer assessmentsPerContext = new Integer(0);

	protected Integer contextStudents = new Integer(0);

	protected Integer contextsWithAssessments = new Integer(0);

	protected Integer itemsPerAssessment = new Integer(0);

	protected Integer submissionsPerStudent = new Integer(0);

	public GenerateSpecs()
	{
	}

	public GenerateSpecs(String value)
	{
		String[] values = StringUtil.split(value, "x");
		assessmentsPerContext = Integer.valueOf(values[0]);
		contextStudents = Integer.valueOf(values[1]);
		contextsWithAssessments = Integer.valueOf(values[2]);
		itemsPerAssessment = Integer.valueOf(values[3]);
		submissionsPerStudent = Integer.valueOf(values[4]);
	}

	public Integer getAssessmentsPerContext()
	{
		return assessmentsPerContext;
	}

	public Integer getContextStudents()
	{
		return contextStudents;
	}

	public Integer getContextsWithAssessments()
	{
		return contextsWithAssessments;
	}

	public Integer getItemsPerAssessment()
	{
		return itemsPerAssessment;
	}

	public Integer getSubmissionsPerStudent()
	{
		return submissionsPerStudent;
	}

	public void setAssessmentsPerContext(Integer value)
	{
		assessmentsPerContext = value;
	}

	public void setContextStudents(Integer value)
	{
		contextStudents = value;
	}

	public void setContextsWithAssessments(Integer value)
	{
		contextsWithAssessments = value;
	}

	public void setItemsPerAssessment(Integer value)
	{
		itemsPerAssessment = value;
	}

	public void setSubmissionsPerStudent(Integer value)
	{
		submissionsPerStudent = value;
	}

	public String toString()
	{
		return assessmentsPerContext.toString() + "x" + contextStudents.toString() + "x" + contextsWithAssessments.toString() + "x"
				+ itemsPerAssessment.toString() + "x" + submissionsPerStudent.toString();
	}
}
