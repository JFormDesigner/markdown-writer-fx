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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.markdownwriterfx.util.Utils;

/**
 * Options
 *
 * @author Karl Tauber
 */
public class Options
{
	public static final String DEF_MARKDOWN_FILE_EXTENSIONS = "*.md,*.markdown,*.txt";
	public enum RendererType { CommonMark, FlexMark };

	public static void load(Preferences options) {
		// load options
		setLineSeparator(options.get("lineSeparator", null));
		setEncoding(options.get("encoding", null));
		setMarkdownFileExtensions(options.get("markdownFileExtensions", DEF_MARKDOWN_FILE_EXTENSIONS));
		setMarkdownExtensions(Utils.getPrefsStrings(options, "markdownExtensions"));
		setMarkdownRenderer(Utils.getPrefsEnum(options, "markdownRenderer", RendererType.CommonMark));
		setShowWhitespace(options.getBoolean("showWhitespace", false));

		// save options on change
		lineSeparatorProperty().addListener((ob, o, n) -> {
			Utils.putPrefs(options, "lineSeparator", getLineSeparator(), null);
		});
		encodingProperty().addListener((ob, o, n) -> {
			Utils.putPrefs(options, "encoding", getEncoding(), null);
		});
		markdownFileExtensionsProperty().addListener((ob, o, n) -> {
			Utils.putPrefs(options, "markdownFileExtensions", getMarkdownFileExtensions(), DEF_MARKDOWN_FILE_EXTENSIONS);
		});
		markdownExtensionsProperty().addListener((ob, o, n) -> {
			Utils.putPrefsStrings(options, "markdownExtensions", getMarkdownExtensions());
		});
		markdownRendererProperty().addListener((ob, o, n) -> {
			Utils.putPrefsEnum(options, "markdownRenderer", getMarkdownRenderer(), RendererType.CommonMark);
		});
		showWhitespaceProperty().addListener((ob, o, n) -> {
			Utils.putPrefsBoolean(options, "showWhitespace", isShowWhitespace(), false);
		});
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

	// 'markdownFileExtensions' property
	private static final StringProperty markdownFileExtensions = new SimpleStringProperty();
	public static String getMarkdownFileExtensions() { return markdownFileExtensions.get(); }
	public static void setMarkdownFileExtensions(String markdownFileExtensions) { Options.markdownFileExtensions.set(markdownFileExtensions); }
	public static StringProperty markdownFileExtensionsProperty() { return markdownFileExtensions; }

	// 'markdownExtensions' property
	private static final ObjectProperty<String[]> markdownExtensions = new SimpleObjectProperty<>(new String[0]);
	public static String[] getMarkdownExtensions() { return markdownExtensions.get(); }
	public static void setMarkdownExtensions(String[] markdownExtensions) { Options.markdownExtensions.set(markdownExtensions); }
	public static ObjectProperty<String[]> markdownExtensionsProperty() { return markdownExtensions; }

	// 'markdownRenderer' property
	private static final ObjectProperty<RendererType> markdownRenderer = new SimpleObjectProperty<>(RendererType.CommonMark);
	public static RendererType getMarkdownRenderer() { return markdownRenderer.get(); }
	public static void setMarkdownRenderer(RendererType markdownRenderer) { Options.markdownRenderer.set(markdownRenderer); }
	public static ObjectProperty<RendererType> markdownRendererProperty() { return markdownRenderer; }

	// 'showWhitespace' property
	private static final BooleanProperty showWhitespace = new SimpleBooleanProperty();
	public static boolean isShowWhitespace() { return showWhitespace.get(); }
	public static void setShowWhitespace(boolean showWhitespace) { Options.showWhitespace.set(showWhitespace); }
	public static BooleanProperty showWhitespaceProperty() { return showWhitespace; }
}
