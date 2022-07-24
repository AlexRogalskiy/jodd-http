// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.http;

import jodd.net.URLCoder;
import jodd.net.URLDecoder;
import jodd.util.StringPool;
import jodd.util.StringUtil;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Few HTTP utilities.
 */
public class HttpUtil {

	// ---------------------------------------------------------------- query

	/**
	 * Builds a query string from given query map.
	 */
	public static String buildQuery(final HttpMultiMap<?> queryMap, final String encoding) {
		if (queryMap.isEmpty()) {
			return StringPool.EMPTY;
		}

		final StringBuilder query = new StringBuilder();

		int count = 0;
		for (final Map.Entry<String, ?> entry : queryMap) {
			String key = entry.getKey();
			key = URLCoder.encodeQueryParam(key, Charset.forName(encoding));

			final Object value = entry.getValue();

			if (value == null) {
				if (count != 0) {
					query.append('&');
				}

				query.append(key);
				count++;
			} else {
				if (count != 0) {
					query.append('&');
				}

				query.append(key);
				count++;
				query.append('=');

				final String valueString = URLCoder.encodeQueryParam(value.toString(), Charset.forName(encoding));
				query.append(valueString);
			}
		}

		return query.toString();
	}

	/**
	 * Parses query from give query string. Values are optionally decoded.
	 */
	public static HttpMultiMap<String> parseQuery(final String query, final boolean decode) {

		final HttpMultiMap<String> queryMap = HttpMultiMap.newCaseInsensitiveMap();

		if (StringUtil.isBlank(query)) {
			return queryMap;
		}

		int lastNdx = 0;
		while (lastNdx < query.length()) {
			int ndx = query.indexOf('&', lastNdx);
			if (ndx == -1) {
				ndx = query.length();
			}

			final String paramAndValue = query.substring(lastNdx, ndx);

			ndx = paramAndValue.indexOf('=');

			if (ndx == -1) {
				queryMap.add(paramAndValue, null);
			}
			else {
				String name = paramAndValue.substring(0, ndx);
				if (decode) {
					name = URLDecoder.decodeQuery(name);
				}

				String value = paramAndValue.substring(ndx + 1);

				if (decode) {
					value = URLDecoder.decodeQuery(value);
				}

				queryMap.add(name, value);
			}
			lastNdx += paramAndValue.length() + 1;
		}

		return queryMap;
	}

	// ---------------------------------------------------------------- misc

	/**
	 * Makes nice header names.
	 */
	public static String prepareHeaderParameterName(final String headerName) {

		// special cases

		if (headerName.equals("etag")) {
			return HttpBase.HEADER_ETAG;
		}

		if (headerName.equals("www-authenticate")) {
			return "WWW-Authenticate";
		}

		final char[] name = headerName.toCharArray();

		boolean capitalize = true;

		for (int i = 0; i < name.length; i++) {
			final char c = name[i];

			if (c == '-') {
				capitalize = true;
				continue;
			}

			if (capitalize) {
				name[i] = Character.toUpperCase(c);
				capitalize = false;
			} else {
				name[i] = Character.toLowerCase(c);
			}
		}

		return new String(name);
	}

	// ---------------------------------------------------------------- content type

	/**
	 * Extracts media-type from value of "Content Type" header.
	 */
	public static String extractMediaType(final String contentType) {
		final int index = contentType.indexOf(';');

		if (index == -1) {
			return contentType;
		}

		return contentType.substring(0, index);
	}

	/**
	 * @see #extractHeaderParameter(String, String, char)
	 */
	public static String extractContentTypeCharset(final String contentType) {
		return extractHeaderParameter(contentType, "charset", ';');
	}

	// ---------------------------------------------------------------- keep-alive

	/**
	 * Extract keep-alive timeout.
	 */
	public static String extractKeepAliveTimeout(final String keepAlive) {
		return extractHeaderParameter(keepAlive, "timeout", ',');
	}

	public static String extractKeepAliveMax(final String keepAlive) {
		return extractHeaderParameter(keepAlive, "max", ',');
	}

	// ---------------------------------------------------------------- header

	/**
	 * Extracts header parameter. Returns <code>null</code>
	 * if parameter not found.
	 */
	public static String extractHeaderParameter(final String header, final String parameter, final char separator) {
		int index = 0;

		while (true) {
			index = header.indexOf(separator, index);

			if (index == -1) {
				return null;
			}

			index++;

			// skip whitespaces
			while (index < header.length() && header.charAt(index) == ' ') {
				index++;
			}

			int eqNdx = header.indexOf('=', index);

			if (eqNdx == -1) {
				return null;
			}

			final String paramName = header.substring(index, eqNdx);

			eqNdx++;

			if (!paramName.equalsIgnoreCase(parameter)) {
				index = eqNdx;
				continue;
			}

			final int endIndex = header.indexOf(';', eqNdx);

			if (endIndex == -1) {
				return header.substring(eqNdx);
			} else {
				return header.substring(eqNdx, endIndex);
			}
		}
	}

	// ---------------------------------------------------------------- url

	/**
	 * Determines if path is relative or absolute.
	 *
	 * https://datatracker.ietf.org/doc/html/rfc3986#section-4.2
	 *
	 * A relative reference that begins with a single slash character is
	 * termed an absolute-path reference.
	 *
	 * A relative reference that does
	 * not begin with a slash character is termed a relative-path reference.
	 *
	 * A path segment that contains a colon character (e.g., "this:that")
	 * cannot be used as the first segment of a relative-path reference, as
	 * it would be mistaken for a scheme name.
	 */
	public static boolean isAbsoluteUrl(String url) {
		if (url.startsWith(StringPool.SLASH)) {
			// A relative reference that begins with a single slash character is
			// termed an absolute-path reference.
			return false;
		}
		int slashIndex = url.indexOf('/');
		int colonIndex = url.indexOf(':');
		if (colonIndex == -1) {
			// no colon at all - it is a relative url
			return false;
		}
		// colon exist - is it after the slash?
		if (colonIndex > slashIndex) {
			return false;
		}
		return true;
	}

}
