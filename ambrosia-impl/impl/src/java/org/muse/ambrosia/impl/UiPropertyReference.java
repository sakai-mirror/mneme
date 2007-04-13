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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.muse.ambrosia.api.Context;
import org.muse.ambrosia.api.FormatDelegate;
import org.muse.ambrosia.api.PropertyReference;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.util.StringUtil;

/**
 * UiPropertyReference accesses a particular selector of a model entity. If the reference is not set, we attempt to just get the object itself.<br />
 * Nested references are supported. For example, "foo.bar" means call the entity.getFoo(), and with the result of that, call getBar().<br />
 * Missing values can be set to return a specified message.
 */
public class UiPropertyReference implements PropertyReference
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(PropertyReference.class);

	/** The entity reference. */
	protected String entityReference = null;

	/** The message selector for a format display. */
	protected String format = null;

	/** A delegate to do the formatting. */
	protected FormatDelegate formatDelegate = null;

	/** The text (message selector) to use if a selector value cannot be found or is null. */
	protected String missingText = null;

	/** The list of values to be considered missing, along with null. */
	protected String[] missingValues = null;

	/** The list of other properties to combine into a formatted display. */
	protected List<PropertyReference> properties = new ArrayList<PropertyReference>();

	/** The entity selector reference. */
	protected String propertyReference = null;

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference addProperty(PropertyReference property)
	{
		properties.add(property);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEncoding(Context context, Object focus, int index)
	{
		// start with a full reference
		StringBuffer rv = new StringBuffer();
		rv.append(getFullReference(context));

		// add the special object selector
		rv.append(".[");

		// // get the object's id
		// String id = (String) getValue(focus, "id");
		// if (id != null)
		// {
		// rv.append(id);
		// }

		// append the index
		rv.append(Integer.toString(index));

		rv.append("]");

		return rv.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFullReference(Context context)
	{
		// if the entity reference has a encode value in context, use that instead of the entity reference value
		String entityRefName = this.entityReference;
		if (entityRefName != null)
		{
			String encoding = context.getEncoding(entityRefName);
			if (encoding != null)
			{
				entityRefName = encoding;
			}
		}

		return (entityRefName == null ? "" : entityRefName)
				+ (this.propertyReference == null ? "" : ("." + this.propertyReference));
	}

	/**
	 * {@inheritDoc}
	 */
	public String read(Context context, Object focus)
	{
		// read the object
		Object value = readObject(context, focus);

		// return the missing text if defined
		if (missing(value)) return missingValue(context);

		// format
		String formatted = format(context, value);

		// if we are not specially formatted, we are done
		if (this.format == null) return formatted;

		// use all properties for the format, starting with the main one
		Object[] args = new Object[1 + properties.size()];
		args[0] = formatted;
		int i = 1;
		for (PropertyReference prop : properties)
		{
			args[i++] = prop.read(context, focus);
		}

		// format the group
		String finalFormat = context.getMessages().getFormattedMessage(this.format, args);
		return finalFormat;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object readObject(Context context, Object focus)
	{
		// wean off of null entity refs
		if (this.entityReference == null)
		{
			// using a format delegate it's ok
			if (this.formatDelegate == null)
			{
				M_log.warn("read: no entity reference: property reference: " + this.propertyReference);
			}
		}

		// use the focus if we don't have a reference defined
		Object entity = focus;

		if (this.entityReference != null)
		{
			entity = (Object) context.get(entityReference);
		}

		if (entity == null) return null;

		// pull out the value object
		Object value = getNestedValue(entity, false);

		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference setFormat(String format)
	{
		this.format = format;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference setFormatDelegate(FormatDelegate formatter)
	{
		this.formatDelegate = formatter;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference setMissingText(String text)
	{
		this.missingText = text;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference setMissingValues(String... values)
	{
		missingValues = values;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PropertyReference setReference(String fullReference)
	{
		// split out the entity and property references
		int pos = fullReference.indexOf(".");
		if (pos > -1)
		{
			this.entityReference = fullReference.substring(0, pos);
			this.propertyReference = fullReference.substring(pos + 1);
		}
		
		// if this is just an entity reference
		else
		{
			this.entityReference = fullReference;
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(Context context, FileItem value)
	{
		// start with our entity from the contxt
		Object entity = null;
		if (this.entityReference != null)
		{
			entity = (Object) context.get(entityReference);
		}

		if (entity == null) return;

		// read all the way to one property short of the end - that's the object we are writing to
		Object target = getNestedValue(entity, true);

		// write value to the property of target that is the last dotted component of our property reference
		int pos = this.propertyReference.lastIndexOf(".");
		String lastProperty = this.propertyReference.substring(pos + 1);

		setFileValue(target, lastProperty, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(Context context, String... value)
	{
		// start with our entity from the contxt
		Object entity = null;
		if (this.entityReference != null)
		{
			entity = (Object) context.get(entityReference);
		}

		if (entity == null) return;

		// read all the way to one property short of the end - that's the object we are writing to
		Object target = getNestedValue(entity, true);

		// write value to the property of target that is the last dotted component of our property reference
		int pos = this.propertyReference.lastIndexOf(".");
		String lastProperty = this.propertyReference.substring(pos + 1);

		setValue(target, lastProperty, value);
	}

	/**
	 * Format the value found into a display string.
	 * 
	 * @param context
	 *        The Context.
	 * @param value
	 *        The value.
	 * @return The value formatted into a display string.
	 */
	protected String format(Context context, Object value)
	{
		// TODO: "" instead of null?
		String rv = null;

		if (this.formatDelegate != null)
		{
			rv = this.formatDelegate.format(context, value);
		}
		else
		{
			if (value != null)
			{
				rv = value.toString();
			}
		}

		return rv;
	}

	/**
	 * Get the index item from the collection.
	 * 
	 * @param collection
	 *        The collection (Collection or array)
	 * @param index
	 *        The index.
	 * @return The indexed item from the collection, or null if not found or not a collection
	 */
	protected Object getIndexValue(Object collection, int index)
	{
		if (collection == null) return null;

		if (collection instanceof List)
		{
			List l = (List) collection;
			if ((index >= 0) && (index < l.size()))
			{
				return l.get(index);
			}
		}

		else if (collection.getClass().isArray())
		{
			Object[] a = (Object[]) collection;
			if ((index >= 0) && (index < a.length))
			{
				return a[index];
			}
		}

		return null;
	}

	/**
	 * Read the configured selector value from the entity. Support "." nesting of selector values.
	 * 
	 * @param entity
	 *        The entity to read from.
	 * @return The selector value object found, or null if not.
	 */
	protected Object getNestedValue(Object entity, boolean skipLast)
	{
		// if no property defined, used the entity
		if (this.propertyReference == null) return entity;

		// if not nested, return a simple dereference (unless we are skiping the last, which in this is all, so return the entity)
		if (this.propertyReference.indexOf(".") == -1)
		{
			if (skipLast) return entity;
			return getValue(entity, this.propertyReference);
		}

		String[] nesting = this.propertyReference.split("\\.");
		Object current = entity;
		for (String s : nesting)
		{
			// if last and we want to skip last, get out
			if ((skipLast) && (s == nesting[nesting.length - 1])) break;

			current = getValue(current, s);

			// early exit if we run out of values
			if (current == null) break;
		}

		return current;
	}

	/**
	 * Read the configured selector value from the entity.
	 * 
	 * @param entity
	 *        The entity to read from.
	 * @param selector
	 *        The selector name.
	 * @return The selector value object found, or null if not.
	 */
	protected Object getValue(Object entity, String property)
	{
		// if no selector named, just use the entity
		if (property == null) return entity;

		// if the property is an index reference
		if (property.startsWith("[") && property.endsWith("]"))
		{
			return getIndexValue(entity, Integer.parseInt(property.substring(1, property.length() - 1)));
		}

		// form a "getFoo()" based getter method name
		StringBuffer getter = new StringBuffer("get" + property);
		getter.setCharAt(3, getter.substring(3, 4).toUpperCase().charAt(0));

		try
		{
			// use this form, providing the getter name and no setter, so we can support properties that are read-only
			PropertyDescriptor pd = new PropertyDescriptor(property, entity.getClass(), getter.toString(), null);
			Method read = pd.getReadMethod();
			Object value = read.invoke(entity, (Object[]) null);
			return value;
		}
		catch (IntrospectionException ie)
		{
			M_log.warn("getValue: method: " + property + " object: " + entity.getClass() + " : " + ie);
			return null;
		}
		catch (IllegalAccessException ie)
		{
			M_log.warn("getValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
			return null;
		}
		catch (IllegalArgumentException ie)
		{
			M_log.warn("getValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
			return null;
		}
		catch (InvocationTargetException ie)
		{
			M_log.warn("getValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
			return null;
		}
	}

	/**
	 * Check if this value is considered a missing value.
	 * 
	 * @param value
	 *        The value to test.
	 * @return true if this is considered missing, false if not.
	 */
	protected boolean missing(Object value)
	{
		if (value == null) return true;

		if ((this.missingValues != null) && (StringUtil.contains(this.missingValues, value.toString()))) return true;

		return false;
	}

	/**
	 * Return the value for when we are missing the value - either null or the missing value message.
	 * 
	 * @param context
	 *        The Context.
	 * @return The value for when we are missing the value - either null or the missing value message.
	 */
	protected String missingValue(Context context)
	{
		if (this.missingText == null) return null;

		return context.getMessages().getString(this.missingText);
	}

	/**
	 * Write the value for FileItem (commons file upload) values.
	 * 
	 * @param entity
	 *        The entity to write to.
	 * @param property
	 *        The property to set.
	 * @param value
	 *        The value to write.
	 */
	protected void setFileValue(Object entity, String property, FileItem value)
	{
		// form a "setFoo()" based setter method name
		StringBuffer setter = new StringBuffer("set" + property);
		setter.setCharAt(3, setter.substring(3, 4).toUpperCase().charAt(0));

		try
		{
			// use this form, providing the getter name and no setter, so we can support properties that are read-only
			PropertyDescriptor pd = new PropertyDescriptor(property, entity.getClass(), null, setter.toString());
			Method write = pd.getWriteMethod();
			Object[] params = new Object[1];

			Class[] paramTypes = write.getParameterTypes();
			if ((paramTypes != null) && (paramTypes.length == 1))
			{
				// single value boolean
				if (paramTypes[0] != FileItem.class)
				{
					M_log.warn("setFileValue: target not expecting FileItem: " + entity.getClass() + " " + property);
					return;
				}

				params[0] = value;
				write.invoke(entity, params);
			}
			else
			{
				M_log.warn("setFileValue: method: " + property + " object: " + entity.getClass()
						+ " : no one parameter setter method defined");
			}
		}
		catch (IntrospectionException ie)
		{
			M_log.warn("setFileValue: method: " + property + " object: " + entity.getClass() + " : " + ie);
		}
		catch (IllegalAccessException ie)
		{
			M_log.warn("setFileValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
		catch (IllegalArgumentException ie)
		{
			M_log.warn("setFileValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
		catch (InvocationTargetException ie)
		{
			M_log.warn("setFileValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
	}

	/**
	 * Write the value.
	 * 
	 * @param entity
	 *        The entity to write to.
	 * @param property
	 *        The property to set.
	 * @param value
	 *        The value to write.
	 */
	protected void setValue(Object entity, String property, String[] value)
	{
		// form a "setFoo()" based setter method name
		StringBuffer setter = new StringBuffer("set" + property);
		setter.setCharAt(3, setter.substring(3, 4).toUpperCase().charAt(0));

		try
		{
			// use this form, providing the getter name and no setter, so we can support properties that are read-only
			PropertyDescriptor pd = new PropertyDescriptor(property, entity.getClass(), null, setter.toString());
			Method write = pd.getWriteMethod();
			Object[] params = new Object[1];

			Class[] paramTypes = write.getParameterTypes();
			if ((paramTypes != null) && (paramTypes.length == 1))
			{
				// single value boolean
				if (paramTypes[0] == Boolean.class)
				{
					params[0] = (value != null) ? Boolean.valueOf(StringUtil.trimToZero(value[0])) : null;
				}

				// multiple value boolean
				else if (paramTypes[0] == Boolean[].class)
				{
					if (value != null)
					{
						Boolean[] values = new Boolean[value.length];
						for (int i = 0; i < value.length; i++)
						{
							values[i] = Boolean.valueOf(StringUtil.trimToZero(value[i]));
						}
						params[0] = values;
					}
					else
					{
						params[0] = null;
					}
				}

				// single value long
				else if (paramTypes[0] == Long.class)
				{
					params[0] = (value != null) ? Long.valueOf(StringUtil.trimToZero(value[0])) : null;
				}

				// multiple value long
				else if (paramTypes[0] == Long[].class)
				{
					if (value != null)
					{
						Long[] values = new Long[value.length];
						for (int i = 0; i < value.length; i++)
						{
							values[i] = Long.valueOf(StringUtil.trimToZero(value[i]));
						}
						params[0] = values;
					}
					else
					{
						params[0] = null;
					}
				}

				// single value int
				else if (paramTypes[0] == Integer.class)
				{
					params[0] = (value != null) ? Integer.valueOf(StringUtil.trimToZero(value[0])) : null;
				}

				// multiple value int
				else if (paramTypes[0] == Integer[].class)
				{
					if (value != null)
					{
						Integer[] values = new Integer[value.length];
						for (int i = 0; i < value.length; i++)
						{
							values[i] = Integer.valueOf(StringUtil.trimToZero(value[i]));
						}
						params[0] = values;
					}
					else
					{
						params[0] = null;
					}
				}

				// single value Time
				else if (paramTypes[0] == Time.class)
				{
					// TODO: what format?
					params[0] = (value != null) ? TimeService.newTimeGmt(StringUtil.trimToZero(value[0])) : null;
				}

				// multiple value Time
				else if (paramTypes[0] == Time[].class)
				{
					if (value != null)
					{
						Time[] values = new Time[value.length];
						for (int i = 0; i < value.length; i++)
						{
							// TODO: what format?
							values[i] = TimeService.newTimeGmt(StringUtil.trimToZero(value[i]));
						}
						params[0] = values;
					}
					else
					{
						params[0] = null;
					}
				}

				// single value string
				else if (paramTypes[0] == String.class)
				{
					params[0] = (value != null) ? StringUtil.trimToNull(value[0]) : null;
				}

				// multiple value string
				else if (paramTypes[0] == String[].class)
				{
					// trim it
					if (value != null)
					{
						for (int i = 0; i < value.length; i++)
						{
							value[i] = StringUtil.trimToNull(value[i]);
						}
					}

					params[0] = value;
				}

				// TODO: other types
				else
				{
					M_log.warn("setValue: unhandled setter parameter type - not set: " + paramTypes[0]);
					return;
				}

				write.invoke(entity, params);
			}
			else
			{
				M_log.warn("setValue: method: " + property + " object: " + entity.getClass()
						+ " : no one parameter setter method defined");
			}
		}
		catch (IntrospectionException ie)
		{
			M_log.warn("setValue: method: " + property + " object: " + entity.getClass() + " : " + ie);
		}
		catch (IllegalAccessException ie)
		{
			M_log.warn("setValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
		catch (IllegalArgumentException ie)
		{
			M_log.warn("setValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
		catch (InvocationTargetException ie)
		{
			M_log.warn("setValue: method: " + property + " object: " + entity.getClass() + " :" + ie);
		}
	}
}
