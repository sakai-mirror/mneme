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

package org.w3c.tidy;

import java.io.OutputStream;

/**
 * TidyHelper extends JTidy's Tidy class with new methods
 */
public class TidyHelper extends Tidy
{
	public void pprintNode(Configuration configuration, org.w3c.dom.Node node, OutputStream out)
	{
		Out o = new OutImpl();
		PPrint pprint;
		Node document;

		if (!(node instanceof DOMNodeImpl))
		{
			return;
		}
		document = ((DOMNodeImpl) node).adaptee;

		o.state = StreamIn.FSM_ASCII;
		o.encoding = configuration.CharEncoding;

		if (out != null)
		{
			pprint = new PPrint(configuration);
			o.out = out;

			if (configuration.XmlTags)
				pprint.printXMLTree(o, (short) 0, 0, null, document);
			else
				pprint.printTree(o, (short) 0, 0, null, document);

			pprint.flushLine(o, 0);
		}
	}
}
