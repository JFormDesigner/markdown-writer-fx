/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
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

import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.markdownwriterfx.util.Utils;
import org.pegdown.Extensions;

/**
 * Options
 *
 * @author Karl Tauber
 */
public class Options
{
	private static Preferences options;

	public static void load(Preferences options) {
		Options.options = options;

		setLineSeparator(options.get("lineSeparator", null));
		setEncoding(options.get("encoding", null));
		setMarkdownExtensions(options.getInt("markdownExtensions", Extensions.ALL));
		setShowWhitespace(options.getBoolean("showWhitespace", false));
	}

	public static void save() {
		Utils.putPrefs(options, "lineSeparator", getLineSeparator(), null);
		Utils.putPrefs(options, "encoding", getEncoding(), null);
		Utils.putPrefsInt(options, "markdownExtensions", getMarkdownExtensions(), Extensions.ALL);
		Utils.putPrefsBoolean(options, "showWhitespace", isShowWhitespace(), false);
	}

	// 'lineSeparator' property
	private static final StringProperty lineSeparator = new SimpleStringProperty();
	public static String getLineSeparator() { return lineSeparator.get(); }
	public static void setLineSeparator(String lineSeparator) { Options.lineSeparator.set(lineSeparator); }
	public static StringProperty lineSeparatorProperty() { return lineSeparator; }

	// 'encoding' property
	private static final StringProperty encoding = new SimpleStringProperty();
	public static String getEncoding() { return encoding.get(); }
	public static void setEncoding(String encoding) { Options.encoding.set(encoding); }
	public static StringProperty encodingProperty() { return encoding; }

	// 'markdownExtensions' property
	private static final IntegerProperty markdownExtensions = new SimpleIntegerProperty();
	public static int getMarkdownExtensions() { return markdownExtensions.get(); }
	public static void setMarkdownExtensions(int markdownExtensions) { Options.markdownExtensions.set(markdownExtensions); }
	public static IntegerProperty markdownExtensionsProperty() { return markdownExtensions; }

	// 'showWhitespace' property
	private static final BooleanProperty showWhitespace = new SimpleBooleanProperty();
	public static boolean isShowWhitespace() { return showWhitespace.get(); }
	public static void setShowWhitespace(boolean showWhitespace) { Options.showWhitespace.set(showWhitespace); }
	public static BooleanProperty showWhitespaceProperty() { return showWhitespace; }
}
