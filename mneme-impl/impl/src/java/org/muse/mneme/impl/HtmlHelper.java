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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.TidyHelper;

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
/*
		try
		{
			// parse possibly dirty html
			TidyHelper tidy = new TidyHelper();
			ByteArrayInputStream bais = new ByteArrayInputStream(source.getBytes("UTF-8"));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(baos);
			tidy.setErrout(pw);
			tidy.setQuiet(true);
			tidy.setXHTML(true);
			// tidy.setRawOut(true);
			// tidy.setTidyMark(false);
			// tidy.setXmlOut(true);
			Document doc = tidy.parseDOM(bais, null);

			// assure target=_blank in all anchors
			XPath x = new DOMXPath("//a");
			List l = x.selectNodes(doc);
			for (Object o : l)
			{
				Element e = (Element) o;
				e.setAttribute("target", "_blank");
			}

			// find the part we will save
			Element rvElement = null;
			XPath x2 = new DOMXPath("/html/body");
			l = x2.selectNodes(doc);
			Element body = (Element) l.get(0);
			if ((body.getChildNodes().getLength() == 1) && (((Node) (body.getChildNodes().item(0))).getNodeType() == Node.ELEMENT_NODE)
					&& (((Node) (body.getChildNodes().item(0))).getNodeName().equals("p")))
			{
				rvElement = (Element) body.getChildNodes().item(0);
			}

			// otherwise use the body, but change it to a paragraph
			else
			{
				rvElement = doc.createElement("p");
				NodeList nodes = body.getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++)
				{
					Node child = nodes.item(i);
					rvElement.appendChild(child);
				}
			}

			baos = new ByteArrayOutputStream();
			tidy.pprintNode(tidy.getConfiguration(), rvElement, baos);
			String rv = baos.toString("UTF-8");
			rv = rv.trim();
//			rv = rv.replaceAll(">\n<", "><");
//			rv = rv.replaceAll(">\n", ">");
//			rv = rv.replaceAll("\n", " ");
			rv = rv.replaceAll("\n", " ");

			return rv;
		}
		catch (IOException e)
		{
			M_log.warn(e);
		}
		catch (JaxenException e)
		{
			M_log.warn(e);
		}
*/
		return source;
	}
}
