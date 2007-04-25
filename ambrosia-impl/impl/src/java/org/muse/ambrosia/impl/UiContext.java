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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.i18n.InternationalizedMessages;
import org.muse.ambrosia.api.Container;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.Controller;

/**
 * UiContext contains the runtime specific information needed with the UiController tree to work a response.<br />
 * This is a set of internationalized messages that the UI tree references, along with a map of named objects that the UI tree references.
 */
public class UiContext implements Context
{
	/** A print write to use to collect output rather than send it to the real writer. */
	protected PrintWriter collectingWriter = null;

	/** The stream collecting the bytes while collecting. */
	protected ByteArrayOutputStream collectionStream = null;

	/** The tool destination of the request. */
	protected String destination = null;

	/** named objects and encoding references. */
	protected Map<String, String> encodings = new HashMap<String, String>();

	/** The list of form element ids for the focus path. */
	protected List<String> focusIds = new ArrayList<String>();

	/** The name of the form that wraps the entire interface. */
	protected String formName = null;

	/** unique in-context id. */
	protected int id = 0;

	/** Internationalized messages. */
	protected InternationalizedMessages messages = null;

	/** named objects and values. */
	protected Map<String, Object> objects = new HashMap<String, Object>();

	/** If the post was expected or not. */
	protected boolean postExpected = false;

	/** The "current" destination when this request came in (i.e. where we just were). */
	protected String previousDestination = null;

	/** Registrations made by components. */
	protected Map<String, String> registrations = new HashMap<String, String>();

	/** Collect various javascript. */
	protected StringBuffer scriptCode = new StringBuffer();

	/** The top controller in the interface being rendered. */
	protected Controller ui = null;

	/** Place to collect validation javascript code. */
	protected StringBuffer validationCode = new StringBuffer();

	/** The writer on the response output stream. */
	protected PrintWriter writer = null;

	/**
	 * {@inheritDoc}
	 */
	public void addFocusId(String id)
	{
		this.focusIds.add(id);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addScript(String code)
	{
		this.scriptCode.append(code);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addValidation(String validation)
	{
		this.validationCode.append(validation);
	}

	/**
	 * {@inheritDoc}
	 */
	public void clear()
	{
		objects.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Controller> findControllers(String id)
	{
		if (this.ui == null) return new ArrayList<Controller>();

		if (this.ui instanceof Container) return ((Container) this.ui).findControllers(id);

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(String name)
	{
		if (name == null) return null;

		return objects.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCollected()
	{
		if (this.collectionStream == null) return null;

		String rv = null;

		// close the stream
		try
		{
			// flush
			this.collectingWriter.flush();

			// read
			rv = this.collectionStream.toString();

			// close the writer
			this.collectingWriter.close();

			// close the stream
			this.collectionStream.close();
		}
		catch (IOException e)
		{
		}

		// clear out of collecting mode
		this.collectionStream = null;
		this.collectingWriter = null;

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDestination()
	{
		return this.destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEncoding(String name)
	{
		if (name == null) return null;

		return encodings.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getFocusIds()
	{
		return this.focusIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFormName()
	{
		return this.formName;
	}

	/**
	 * {@inheritDoc}
	 */
	public InternationalizedMessages getMessages()
	{
		return messages;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getPostExpected()
	{
		return this.postExpected;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPreviousDestination()
	{
		return this.previousDestination;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRegistration(String componentId)
	{
		return registrations.get(componentId);
	}

	/**
	 * {@inheritDoc}
	 */
	public PrintWriter getResponseWriter()
	{
		if (this.collectingWriter != null) return this.collectingWriter;

		return this.writer;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getScript()
	{
		if (this.scriptCode.length() == 0) return null;

		return this.scriptCode.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public Controller getUi()
	{
		return this.ui;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getUniqueId()
	{
		return id++;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getValidation()
	{
		if (this.validationCode.length() == 0) return null;

		return this.validationCode.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, Object value)
	{
		if (name == null) return;

		if (value == null)
		{
			objects.remove(name);
			encodings.remove(name);
		}
		else
		{
			objects.put(name, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, Object value, String encoding)
	{
		if (name == null) return;

		if (value == null)
		{
			objects.remove(name);
			encodings.remove(name);
		}
		else
		{
			objects.put(name, value);
			encodings.put(name, encoding);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void register(String componentId, String value)
	{
		registrations.put(componentId, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(String name)
	{
		objects.remove(name);
		encodings.remove(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCollecting()
	{
		if (this.collectingWriter != null) return;

		this.collectionStream = new ByteArrayOutputStream();
		collectingWriter = new PrintWriter(this.collectionStream);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDestination(String destination)
	{
		this.destination = destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFormName(String name)
	{
		this.formName = name;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setMessages(InternationalizedMessages messages)
	{
		this.messages = messages;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPostExpected(boolean expected)
	{
		this.postExpected = expected;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreviousDestination(String destination)
	{
		this.previousDestination = destination;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setResponseWriter(PrintWriter writer)
	{
		this.writer = writer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUi(Controller ui)
	{
		this.ui = ui;
	}
}
