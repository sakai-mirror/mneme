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

package org.muse.mneme.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.util.StringUtil;

/**
 * SqlHelper has some utility methods for working with database information.
 */
public class SqlHelper
{
	/**
	 * Decode an encoded string array.
	 * 
	 * @param data
	 *        The encoded data.
	 * @return The decoded string array.
	 */
	public static String[] decodeStringArray(String data)
	{
		if (data == null) return new String[0];

		// collect the strings
		List<String> strings = new ArrayList<String>();
		int pos = 0;
		while (pos < data.length())
		{
			// find the next length, bound by a":"
			int bound = data.indexOf(":", pos);
			if (bound == -1) break;

			// take the pos..bound characters (skipping the last one
			String length = data.substring(pos, bound);
			int len = Integer.valueOf(length);

			// take the characters from after bound, len of them
			String s = (len > 0) ? data.substring(bound + 1, bound + 1 + len) : null;
			pos = bound + 1 + len;

			strings.add(s);
		}

		String[] rv = strings.toArray(new String[strings.size()]);
		return rv;
	}

	/**
	 * Encode a string array into a single string
	 * 
	 * @param data
	 *        The data to encode.
	 * @return The encoded data.
	 */
	public static String encodeStringArray(String[] data)
	{
		if ((data == null) || (data.length == 0)) return null;
		StringBuilder rv = new StringBuilder();
		for (String s : data)
		{
			rv.append((s == null) ? "0" : Integer.toString(s.length()));
			rv.append(":");
			if (s != null) rv.append(s);
		}

		return rv.toString();
	}

	/**
	 * Read a Boolean (encoded as '0' '1') from the results set.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Boolean.
	 * @throws SQLException
	 */
	public static Boolean readBoolean(ResultSet result, int index) throws SQLException
	{
		String b = StringUtil.trimToNull(result.getString(index));
		if (b == null) return null;
		return Boolean.valueOf("1".equals(b) || "true".equals(b));
	}

	/**
	 * Read a Boolean (encoded in a bit or tinyint field) from the results set.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Boolean.
	 * @throws SQLException
	 */
	public static Boolean readBitBoolean(ResultSet result, int index) throws SQLException
	{
		Object o = result.getObject(index);
		if (o == null) return null;

		Boolean rv = null;
		if (o instanceof Boolean)
		{
			rv = (Boolean) o;
		}
		else if (o instanceof Integer)
		{
			rv = Boolean.valueOf(((Integer) o).intValue() == 1);
		}
		else if (o instanceof String)
		{
			if (((String) o).length() > 0)
			{
				rv = Boolean.valueOf(((String) o).charAt(0) == '\u0001');
			}
		}

		return rv;
	}

	/**
	 * Read a long from the result set, and convert to a null (if 0) or a Date.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Date or null.
	 * @throws SQLException
	 */
	public static Date readDate(ResultSet result, int index) throws SQLException
	{
		long time = result.getLong(index);
		if (time == 0) return null;
		return new Date(time);
	}

	/**
	 * Read an id encoded as an unsigned long from the results set. 0 is treated as null.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The id string.
	 * @throws SQLException
	 */
	public static String readId(ResultSet result, int index) throws SQLException
	{
		long l = result.getLong(index);
		if (l == 0) return null;
		return Long.valueOf(l).toString();
	}

	/**
	 * Read a Integer from the results set. null is treated as null.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Integer.
	 * @throws SQLException
	 */
	public static Integer readInteger(ResultSet result, int index) throws SQLException
	{
		String str = StringUtil.trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Integer.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Read a Long from the results set. null is treated as null.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Long.
	 * @throws SQLException
	 */
	public static Long readLong(ResultSet result, int index) throws SQLException
	{
		String str = StringUtil.trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Long.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Read a float from the results set. null is treated as null.
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The Float.
	 * @throws SQLException
	 */
	public static Float readFloat(ResultSet result, int index) throws SQLException
	{
		String str = StringUtil.trimToNull(result.getString(index));
		if (str == null) return null;
		try
		{
			return Float.valueOf(str);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Read a string from the result set, trimmed to null
	 * 
	 * @param results
	 *        The result set.
	 * @param index
	 *        The index.
	 * @return The String.
	 * @throws SQLException
	 */
	public static String readString(ResultSet result, int index) throws SQLException
	{
		return StringUtil.trimToNull(result.getString(index));
	}
}
