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

import java.util.List;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Font;
import org.markdownwriterfx.util.PrefsBooleanProperty;
import org.markdownwriterfx.util.PrefsEnumProperty;
import org.markdownwriterfx.util.PrefsIntegerProperty;
import org.markdownwriterfx.util.PrefsStringProperty;
import org.markdownwriterfx.util.PrefsStringsProperty;
import org.markdownwriterfx.util.Utils;

/**
 * Options
 *
 * @author Karl Tauber
 */
public class Options
{
	public static final String[] DEF_FONT_FAMILIES = {
		"Consolas",
		"DejaVu Sans Mono",
		"Lucida Sans Typewriter",
		"Lucida Console",
	};

	public static final int DEF_FONT_SIZE = 12;
	public static final int MIN_FONT_SIZE = 8;
	public static final int MAX_FONT_SIZE = 36;
	public static final String DEF_MARKDOWN_FILE_EXTENSIONS = "*.md,*.markdown,*.txt";
	public enum RendererType { CommonMark, FlexMark };

	public static void load(Preferences options) {
		fontFamily.init(options, "fontFamily", null, value -> safeFontFamily(value));
		fontSize.init(options, "fontSize", DEF_FONT_SIZE);
		lineSeparator.init(options, "lineSeparator", null);
		encoding.init(options, "encoding", null);
		markdownFileExtensions.init(options, "markdownFileExtensions", DEF_MARKDOWN_FILE_EXTENSIONS);
		markdownExtensions.init(options, "markdownExtensions");
		markdownRenderer.init(options, "markdownRenderer", RendererType.CommonMark);
		showLineNo.init(options, "showLineNo", false);
		showWhitespace.init(options, "showWhitespace", false);
		setSpellChecker(options.getBoolean("spellChecker", true));
		spellCheckerProperty().addListener((ob, o, n) -> {
			Utils.putPrefsBoolean(options, "spellChecker", isSpellChecker(), true);
		});
	}

	/**
	 * Check whether font family is null or invalid (family not available on system)
	 * and search for an available family.
	 */
	private static String safeFontFamily(String fontFamily) {
		List<String> fontFamilies = Font.getFamilies();
		if (fontFamily != null && fontFamilies.contains(fontFamily))
			return fontFamily;

		for (String family : DEF_FONT_FAMILIES) {
			if (fontFamilies.contains(family))
				return family;
		}
		return "Monospaced";
	}

	// 'fontFamily' property
	private static final PrefsStringProperty fontFamily = new PrefsStringProperty();
	public static String getFontFamily() { return fontFamily.get(); }
	public static void setFontFamily(String fontFamily) { Options.fontFamily.set(fontFamily); }
	public static StringProperty fontFamilyProperty() { return fontFamily; }

	// 'fontSize' property
	private static final PrefsIntegerProperty fontSize = new PrefsIntegerProperty();
	public static int getFontSize() { return fontSize.get(); }
	public static void setFontSize(int fontSize) { Options.fontSize.set(Math.min(Math.max(fontSize,  MIN_FONT_SIZE), MAX_FONT_SIZE)); }
	public static IntegerProperty fontSizeProperty() { return fontSize; }

	// 'lineSeparator' property
	private static final PrefsStringProperty lineSeparator = new PrefsStringProperty();
	public static String getLineSeparator() { return lineSeparator.get(); }
	public static void setLineSeparator(String lineSeparator) { Options.lineSeparator.set(lineSeparator); }
	public static StringProperty lineSeparatorProperty() { return lineSeparator; }

	// 'encoding' property
	private static final PrefsStringProperty encoding = new PrefsStringProperty();
	public static String getEncoding() { return encoding.get(); }
	public static void setEncoding(String encoding) { Options.encoding.set(encoding); }
	public static StringProperty encodingProperty() { return encoding; }

	// 'markdownFileExtensions' property
	private static final PrefsStringProperty markdownFileExtensions = new PrefsStringProperty();
	public static String getMarkdownFileExtensions() { return markdownFileExtensions.get(); }
	public static void setMarkdownFileExtensions(String markdownFileExtensions) { Options.markdownFileExtensions.set(markdownFileExtensions); }
	public static StringProperty markdownFileExtensionsProperty() { return markdownFileExtensions; }

	// 'markdownExtensions' property
	private static final PrefsStringsProperty markdownExtensions = new PrefsStringsProperty();
	public static String[] getMarkdownExtensions() { return markdownExtensions.get(); }
	public static void setMarkdownExtensions(String[] markdownExtensions) { Options.markdownExtensions.set(markdownExtensions); }
	public static ObjectProperty<String[]> markdownExtensionsProperty() { return markdownExtensions; }

	// 'markdownRenderer' property
	private static final PrefsEnumProperty<RendererType> markdownRenderer = new PrefsEnumProperty<>();
	public static RendererType getMarkdownRenderer() { return markdownRenderer.get(); }
	public static void setMarkdownRenderer(RendererType markdownRenderer) { Options.markdownRenderer.set(markdownRenderer); }
	public static ObjectProperty<RendererType> markdownRendererProperty() { return markdownRenderer; }

	// 'showLineNo' property
	private static final PrefsBooleanProperty showLineNo = new PrefsBooleanProperty();
	public static boolean isShowLineNo() { return showLineNo.get(); }
	public static void setShowLineNo(boolean showLineNo) { Options.showLineNo.set(showLineNo); }
	public static BooleanProperty showLineNoProperty() { return showLineNo; }

	// 'showWhitespace' property
	private static final PrefsBooleanProperty showWhitespace = new PrefsBooleanProperty();
	public static boolean isShowWhitespace() { return showWhitespace.get(); }
	public static void setShowWhitespace(boolean showWhitespace) { Options.showWhitespace.set(showWhitespace); }
	public static BooleanProperty showWhitespaceProperty() { return showWhitespace; }

	// 'spellChecker' property
	private static final BooleanProperty spellChecker = new SimpleBooleanProperty();
	public static boolean isSpellChecker() { return spellChecker.get(); }
	public static void setSpellChecker(boolean spellChecker) { Options.spellChecker.set(spellChecker); }
	public static BooleanProperty spellCheckerProperty() { return spellChecker; }
}
