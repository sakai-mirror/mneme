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
import org.muse.ambrosia.util.FormatDelegateImpl;
import org.muse.mneme.api.DrawPart;
import org.muse.mneme.api.ManualPart;
import org.muse.mneme.api.Part;
import org.muse.mneme.api.PoolDraw;

/**
 * The "FormatPartSummary" format delegate for the mneme tool.
 */
public class FormatPartSummaryDelegate extends FormatDelegateImpl
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(FormatPartSummaryDelegate.class);

	/**
	 * Shutdown.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**
	 * {@inheritDoc}
	 */
	public String format(Context context, Object value)
	{
		// value is a part
		if (value == null) return null;
		if (!(value instanceof Part)) return value.toString();
		Part part = (Part) value;

		if (part instanceof ManualPart)
		{
			// manual-part-summary=Manually Selected Questions ({0})
			Object[] args = new Object[1];
			args[0] = ((ManualPart) part).getNumQuestions().toString();
			return context.getMessages().getFormattedMessage("manual-part-summary", args);
		}

		if (part instanceof DrawPart)
		{
			// draw-part-summary=Random from Pool{0} ({1}): {2}
			DrawPart p = (DrawPart) part;
			Object[] args = new Object[3];

			args[0] = ((p.getDraws().size() == 1) ? "" : "s");
			args[1] = p.getNumQuestions().toString();
			
			StringBuffer buf = new StringBuffer();
			for (PoolDraw draw : p.getDraws())
			{
				buf.append(draw.getPool().getTitle());
				buf.append(", ");
			}
			if (buf.length() > 0) buf.setLength(buf.length()-2);
			args[2] = buf.toString();

			return context.getMessages().getFormattedMessage("draw-part-summary", args);
		}

		return null;
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
