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

package org.markdownwriterfx.editor;

import java.util.function.IntFunction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

/**
 * @author Karl Tauber
 */
class LineNumberGutterFactory
	implements IntFunction<Node>
{
	private static final Insets INSETS = new Insets(0.0, 5.0, 0.0, 5.0);

    private final StyleClassedTextArea textArea;
    private final Val<Integer> lineCount;

	public LineNumberGutterFactory(StyleClassedTextArea textArea) {
		this.textArea = textArea;
		lineCount = LiveList.sizeOf(textArea.getParagraphs());
	}

	@Override
	public Node apply(int paragraphIndex) {
		int lineNo = paragraphIndex + 1;
		Val<String> text = lineCount.map(n -> {
			int digits = (int) Math.floor(Math.log10(textArea.getParagraphs().size())) + 1;
			return String.format("%" + digits + "d", lineNo);
		});

		Label label = new Label();
		label.textProperty().bind(text.conditionOnShowing(label));
		label.setPadding(INSETS);
		label.setAlignment(Pos.TOP_RIGHT);
		label.setMaxHeight(Double.MAX_VALUE);
		label.getStyleClass().add("lineno");
		return label;
	}
}
