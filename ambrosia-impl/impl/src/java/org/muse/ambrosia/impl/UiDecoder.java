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

package org.muse.ambrosia.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Decoder;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.util.StringUtil;

/**
 * UiDecoder decodes the posted interface.
 */
public class UiDecoder implements Decoder
{
	/**
	 * {@inheritDoc}
	 */
	public String decode(HttpServletRequest req, Context context)
	{
		// if no other destination set, use the current one
		String rv = context.getDestination();

		for (Object entry : req.getParameterMap().keySet())
		{
			String name = (String) entry;

			// look for the parameters starting with "decode"
			if (name.startsWith("decode_"))
			{
				// read this one's value, the name of the parameter holding the value (single value)
				String valueName = req.getParameter(name);
				if (valueName != null)
				{
					// find the property reference which will get set with the value (single value)
					String fullReference = req.getParameter("prop_" + name);
					if (fullReference != null)
					{
						// read the value (multiple values)
						String[] value = req.getParameterValues(valueName);

						// pickup and use the if-null value if null
						if (value == null)
						{
							String[] ifNullValue = req.getParameterValues("null_" + name);
							if (ifNullValue != null) value = ifNullValue;
						}

						// write it to the entity / property in the context
						// TODO: other types than string
						PropertyReference ref = new UiPropertyReference().setReference(fullReference);
						ref.write(context, value);
					}
				}
			}

			// or for submit parameters
			else if (name.startsWith("submit_"))
			{
				// use the name to find the destination field
				String destination = req.getParameter("destination_" + name);
				if (destination != null)
				{
					rv = StringUtil.trimToNull(destination);
				}
			}

			// if we see a "destination_" field with a value, use this as the destination
			else if (name.equals("destination_"))
			{
				String destination = req.getParameter("destination_");
				if (destination != null)
				{
					String dest = StringUtil.trimToNull(destination);
					if (dest != null)
					{
						rv = dest;
					}
				}
			}

			// for file items
			else if (name.startsWith("file_"))
			{
				// read this one's value, the name of the request attribute holding the file item
				String valueName = req.getParameter(name);
				if (valueName != null)
				{
					// find the property reference which will get set with the file
					String fullReference = req.getParameter("prop_" + name);
					if (fullReference != null)
					{
						// read the value
						FileItem item = (FileItem) req.getAttribute(valueName);

						// item may be missing if it was over size max
						if (item != null)
						{
							// write it to the entity / property in the context
							PropertyReference ref = new UiPropertyReference().setReference(fullReference);
							ref.write(context, item);
						}
					}
				}
			}
		}

		return rv;
	}
}
