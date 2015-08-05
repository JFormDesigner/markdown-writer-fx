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

package org.markdownwriterfx.preview;

import java.nio.file.Path;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import org.markdownwriterfx.util.Utils;
import org.parboiled.support.ToStringFormatter;
import org.parboiled.trees.GraphUtils;
import org.pegdown.ast.RootNode;

/**
 * Pegdown AST preview.
 * Prints the AST tree to a text area.
 *
 * @author Karl Tauber
 */
class ASTPreview
	implements MarkdownPreviewPane.Preview
{
	private final TextArea textArea = new TextArea();
	private ScrollBar vScrollBar;

	ASTPreview() {
		textArea.setEditable(false);
	}

	Node getNode() {
		return textArea;
	}

	@Override
	public void update(RootNode astRoot, Path path) {
		double scrollTop = textArea.getScrollTop();
		double scrollLeft = textArea.getScrollLeft();

		textArea.setText(GraphUtils.printTree(astRoot, new ToStringFormatter<>()));

		textArea.setScrollTop(scrollTop);
		textArea.setScrollLeft(scrollLeft);
	}

	@Override
	public void scrollY(double value) {
		if (vScrollBar == null)
			vScrollBar = Utils.findVScrollBar(textArea);
		if (vScrollBar == null)
			return;

		double maxValue = vScrollBar.maxProperty().get();
		vScrollBar.setValue(maxValue * value);
	}
}
