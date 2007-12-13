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

package org.muse.mneme.api;

import java.util.List;

/**
 * ImportService provides support for import into Mnene.
 */
public interface ImportService
{
	/**
	 * The the samigo pools available to this user.
	 * 
	 * @param userId
	 *        The user id.
	 * @return A list of Samigo pool "Ent"s (id and description).
	 */
	List<Ent> getSamigoPools(String userId);

	/**
	 * The the sites where this user has Samigo authoring permissions.
	 * 
	 * @param userId
	 *        The user id.
	 * @return A list of Samigo site "Ent"s (id and description).
	 */
	List<Ent> getSamigoSites(String userId);

	/**
	 * Import the Samigo pool with this id into this context
	 * 
	 * @param id
	 *        The id of the Samigo pool to import.
	 * @param context
	 *        The context where the new pool will live.
	 * @throws AssessmentPermissionException
	 *         if the user does not have permission to create pools and questions.
	 */
	void importPool(String id, String context) throws AssessmentPermissionException;
}
