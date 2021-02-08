/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.markdownwriterfx.options;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.options.Options.RendererType;

/**
 * Markdown extensions
 *
 * @author Karl Tauber
 */
public class MarkdownExtensions
{
	static final HashMap<String, String> displayNames = new HashMap<>();
	static final HashMap<String, String> commonmarkExtClasses = new HashMap<>();
	static final HashMap<String, String> flexmarkExtClasses = new HashMap<>();

	static {
		ResourceBundle bundle = ResourceBundle.getBundle("org.markdownwriterfx.MarkdownExtensions");
		for (String key : bundle.keySet()) {
			String value = bundle.getString(key);
			if (key.startsWith("commonmark.ext."))
				commonmarkExtClasses.put(key.substring("commonmark.ext.".length()), value);
			else if (key.startsWith("flexmark.ext."))
				flexmarkExtClasses.put(key.substring("flexmark.ext.".length()), value);
		}

		HashSet<String> ids = new HashSet<>();
		ids.addAll(commonmarkExtClasses.keySet());
		ids.addAll(flexmarkExtClasses.keySet());
		for (String id : ids)
			displayNames.put(id, Messages.get("MarkdownExtensionsPane.ext." + id));
	}

	public static String[] ids() {
		return displayNames.keySet().toArray(new String[displayNames.size()]);
	}

	public static String displayName(String id) {
		return displayNames.get(id);
	}

	public static boolean isAvailable(RendererType rendererType, String id) {
		switch (rendererType) {
			case CommonMark:	return commonmarkExtClasses.containsKey(id);
			case FlexMark:		return flexmarkExtClasses.containsKey(id);
			default:			return false;
		}
	}

	public static List<org.commonmark.Extension> getCommonmarkExtensions() {
		return createdExtensions(commonmarkExtClasses, null);
	}

	public static List<com.vladsch.flexmark.util.misc.Extension> getFlexmarkExtensions() {
		return createdExtensions(flexmarkExtClasses, null);
	}

	public static List<com.vladsch.flexmark.util.misc.Extension> getFlexmarkExtensions(RendererType rendererType) {
		return createdExtensions(flexmarkExtClasses, rendererType);
	}

	private static <E> ArrayList<E> createdExtensions(HashMap<String, String> extClasses, RendererType rendererType) {
		ArrayList<E> extensions = new ArrayList<>();
		for (String markdownExtension : Options.getMarkdownExtensions()) {
			if (rendererType != null && !isAvailable(rendererType, markdownExtension))
				continue;

			String extClassName = extClasses.get(markdownExtension);
			if (extClassName == null)
				continue; // extension not supported by renderer

			try {
				Class<?> cls = Class.forName(extClassName);
				Method createMethod = cls.getMethod("create");
				@SuppressWarnings("unchecked")
				E extension = (E) createMethod.invoke(null);
				extensions.add(extension);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return extensions;
	}
}
