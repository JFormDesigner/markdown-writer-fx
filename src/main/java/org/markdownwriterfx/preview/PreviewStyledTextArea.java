/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
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

package org.markdownwriterfx.preview;

import java.util.Collection;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.markdownwriterfx.options.Options;

/**
 * A styled text area for preview.
 *
 * It uses the same font as the editor.
 *
 * @author Karl Tauber
 */
class PreviewStyledTextArea
	extends StyleClassedTextArea
{
	private final InvalidationListener optionsListener;

	PreviewStyledTextArea() {
		setEditable(false);
		setFocusTraversable(false);
		getStyleClass().add("padding");

		updateFont();

		optionsListener = e -> updateFont();
		WeakInvalidationListener weakOptionsListener = new WeakInvalidationListener(optionsListener);
		Options.fontFamilyProperty().addListener(weakOptionsListener);
		Options.fontSizeProperty().addListener(weakOptionsListener);
	}

	private void updateFont() {
		setStyle("-fx-font-family: '" + Options.getFontFamily()
				+ "'; -fx-font-size: " + Options.getFontSize() );
	}

	void replaceText(String text, StyleSpans<? extends Collection<String>> styleSpans) {
		// remember old selection range and scrollY
		IndexRange oldSelection = getSelection();
		double oldScrollY = getEstimatedScrollY();

		// replace text and styles
		replaceText(text);
		if (styleSpans != null)
			setStyleSpans(0, styleSpans);

		// restore old selection range and scrollY
		selectRange(oldSelection.getStart(), oldSelection.getEnd());
		Platform.runLater(() -> {
			setEstimatedScrollY(oldScrollY);
		});
	}
}
