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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decision;
import org.muse.ambrosia.util.DecisionDelegateImpl;
import org.muse.mneme.api.Assessment;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.PoolDraw;

/**
 * The "PoolReferencedDecision" decision delegate for the mneme tool.
 */
public class PoolReferencedDecisionDelegate extends DecisionDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(PoolReferencedDecisionDelegate.class);

	/**
	 * {@inheritDoc}
	 */
	public boolean decide(Decision decision, Context context, Object focus)
	{
		// true if the focus, a pool id, is used in any draws from any parts other than "part" in "assessment"

		// focus is the part id
		if (focus == null) return false;
		if (!(focus instanceof String)) return false;
		String poolId = (String) focus;

		// get the assessment
		Assessment assessment = (Assessment) context.get("assessment");
		if (assessment == null) return false;

		// get the part
		Part thisPart = (Part) context.get("part");
		if (thisPart == null) return false;

		// if the part can be found in the assessment draw parts other than "thisPart", it is referenced
		for (Part part : assessment.getParts().getParts())
		{
			if (part.equals(thisPart)) continue;

			if (part instanceof DrawPart)
			{
				for (PoolDraw draw : ((DrawPart) part).getDraws())
				{
					if (draw.getPoolId().equals(poolId))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		super.init();
		M_log.info("init()");
	}
}
