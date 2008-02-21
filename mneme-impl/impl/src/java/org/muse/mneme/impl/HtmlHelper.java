/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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

package org.muse.mneme.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.HtmlCleaner;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.EscapeStrategy;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * HtmlHelper has some utility methods for working with user entered HTML.
 */
public class HtmlHelper
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(HtmlHelper.class);

	/**
	 * Clean some user entered HTML. Assures well formed XML. Assures all anchor tags have target=_blank.
	 * 
	 * @param source
	 *        The source HTML
	 * @return The cleaned up HTML.
	 */
	public static String clean(String source)
	{
		if (source == null) return null;

		try
		{
			// parse possibly dirty html
			HtmlCleaner cleaner = new HtmlCleaner(source);

			//cleaner.setRecognizeUnicodeChars(true);
			//cleaner.setAdvancedXmlEscape(true);
			cleaner.setTranslateSpecialEntities(true);

			// clean it up
			cleaner.clean();

			String pretty = cleaner.getPrettyXmlAsString();
			// System.out.println(pretty);

			// make a DOM for further processing
			Document doc = cleaner.createJDom();

			// assure target=_blank in all anchors
			XPath x = XPath.newInstance("//a");
			List l = x.selectNodes(doc);
			for (Object o : l)
			{
				Element e = (Element) o;
				e.setAttribute("target", "_blank");
			}

			// find the part we will save
			Element rvElement = null;
			XPath x2 = XPath.newInstance("/html/body");
			l = x2.selectNodes(doc);
			Element body = (Element) l.get(0);
			if ((body.getContent().size() == 1) && (body.getChildren().size() == 1)
					&& (((Element) (body.getChildren().get(0))).getName().equals("p")))
			{
				rvElement = (Element) body.getChildren().get(0);
			}

			// otherwise use the body, but change it to a paragraph
			else
			{
				body.setName("p");
				rvElement = body;
			}

			// write to a string
			// Format format = Format.getRawFormat().setEscapeStrategy(new EscapeStrategy()
			// {
			// public boolean shouldEscape(char arg0)
			// {
			// return false;
			// }
			// });
			XMLOutputter xmlOut = new XMLOutputter();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			xmlOut.output(rvElement, baos);
			String rv = baos.toString("UTF-8");

			return rv;
		}
		catch (IOException e)
		{
			M_log.warn(e);
		}
		catch (ParserConfigurationException e)
		{
			M_log.warn(e);
		}
		catch (JDOMException e)
		{
			M_log.warn(e);
		}

		return source;
	}
}
