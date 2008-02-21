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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;

/**
 * Test HtmlHelper.
 */
public class HtmlHelperTest extends TestCase
{
	/** Logger. */
	private static final Log log = LogFactory.getLog(HtmlHelperTest.class);

	/**
	 * @param arg0
	 */
	public HtmlHelperTest(String arg0)
	{
		super(arg0);
	}

	public void testA() throws Exception
	{
		String source = "<p>test &lt;b&gt;</p>";
		String cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p>test &lt;b&gt;</p>"));
	}

	public void testClean() throws Exception
	{
		String expected = "<p>some html</p>";

		String source = "<p>some html</p>";
		String cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<p>some html";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "some html";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = null;
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned == null);

		source = "<p>some html</p>      ";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "         <p>some html</p>      ";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<P>some html</P>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<P >some html</P >";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<P >some html<  /P >";
		cleaned = HtmlHelper.clean(source);
		assertFalse(cleaned, compare(cleaned, expected));

		source = "<p>some html</p></p></p></p>";
		cleaned = HtmlHelper.clean(source);
		//assertTrue(cleaned, compare(cleaned, expected));

		source = "<p>some html</p><p>more html</p>";
		cleaned = HtmlHelper.clean(source);
		assertFalse(cleaned, cleaned.equals(expected));
		//assertTrue(cleaned, compare(cleaned, "<p><p>some html</p><p>more html</p></p>"));

		source = "<P>some html</P>and then some";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p><p>some html</p>and then some</p>"));

		source = "leading text <P>some html</P> and then some";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p>leading text <p>some html</p> and then some</p>"));

		source = "leading text     <P>some html</P>     and then some";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p>leading text     <p>some html</p>     and then some</p>"));

		source = "<P>some html</P>and then some<div>some in a div";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p><p>some html</p>and then some<div>some in a div</div></p>"));

		source = "<P>some html</P>and then some<div>some in a div</p>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p><p>some html</p>and then some<div>some in a div</div></p>"));

		source = "<P>some html</P>and then some<div>some in a div</div>and more";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p><p>some html</p>and then some<div>some in a div</div>and more</p>"));

		source = "<p>test &lt;b&gt;</p>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p>test &lt;b&gt;</p>"));
	}

	public void testCleanTiming() throws Exception
	{
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			String source = "<p>some html</p>";
			String cleaned = HtmlHelper.clean(source);
			assertTrue(compare(source, cleaned));
		}
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("100 cleanings in " + elapsed + " (ms)");
	}

	public void testTarget() throws Exception
	{
		String expected = "<p>some text <a href=\"some.url\" target=\"_blank\">the link</a></p>";

		String source = expected;
		String cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<p>some text <a href=\"some.url\" target=\"help\">the link</a></p>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<p>some text <a href=\"some.url\">the link</a></p>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, expected));

		source = "<p>some text <a href=\"some.url\">the link</a><a href=\"some.url\">the link</a></p>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned,
				"<p>some text <a href=\"some.url\" target=\"_blank\">the link</a><a href=\"some.url\" target=\"_blank\">the link</a></p>"));

		source = "<a href=\"some.url\">the link</a>";
		cleaned = HtmlHelper.clean(source);
		assertTrue(cleaned, compare(cleaned, "<p><a href=\"some.url\" target=\"_blank\">the link</a></p>"));
	}

	protected boolean compare(String source, String other)
	{
		Document sourceDoc = Xml.readDocumentFromString(source);
		String sourceOut = Xml.writeDocumentToString(sourceDoc);

		Document otherDoc = Xml.readDocumentFromString(other);
		String otherOut = Xml.writeDocumentToString(otherDoc);

		return sourceOut.equals(otherOut);
	}

	/**
	 * @param arg0
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	/**
	 * @param arg0
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
}
