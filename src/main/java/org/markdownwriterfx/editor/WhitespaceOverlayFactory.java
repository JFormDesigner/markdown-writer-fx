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

package org.markdownwriterfx.editor;

import java.util.ArrayList;
import java.util.Collection;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.text.Text;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledText;
import org.markdownwriterfx.editor.ParagraphOverlayGraphicFactory.OverlayFactory;

/**
 * Shows whitespace characters.
 *
 * @author Karl Tauber
 */
class WhitespaceOverlayFactory
	extends OverlayFactory
{
	@Override
	public Node[] createOverlayNodes(int paragraphIndex) {
		ObservableList<Paragraph<Collection<String>>> paragraphs = getTextArea().getParagraphs();
		Paragraph<Collection<String>> par = paragraphs.get(paragraphIndex);

		ArrayList<Node> nodes = new ArrayList<>();
		int segmentStart = 0;
		for(StyledText<Collection<String>> segment : par.getSegments()) {
			String text = segment.toString();
			int textLength = text.length();
			for (int i = 0; i < textLength; i++) {
				char ch = text.charAt(i);
				if (ch != ' ' && ch != '\t')
					continue;

				Rectangle2D bounds = getBounds(segmentStart + i, segmentStart + i + 1);

				nodes.add(createTextNode(
						(ch == ' ') ? "\u00B7" : "\u00BB",
						segment.getStyle(),
						bounds.getMinX(),
						bounds.getMinY()));
			}

			segmentStart += textLength;
		}

		if (paragraphIndex < paragraphs.size() - 1) {
			// all paragraphs except last one have line separators
			Rectangle2D bounds = getBounds(segmentStart - 1, segmentStart);

			nodes.add(createTextNode("\u00B6",
					par.getStyleAtPosition(segmentStart),
					bounds.getMaxX(),
					bounds.getMinY()));
		}

		return nodes.toArray(new Node[nodes.size()]);
	}

	private Text createTextNode(String text, Collection<String> styleClasses,
			double x, double y)
	{
		Text t = new Text(text);
		t.setTextOrigin(VPos.TOP);
		t.getStyleClass().add("text");
		t.setOpacity(0.3);
		t.getStyleClass().addAll(styleClasses);
		t.setLayoutX(x);
		t.setLayoutY(y);
		return t;
	}
}
