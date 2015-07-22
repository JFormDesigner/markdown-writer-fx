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

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

/**
 * Markdown editor pane.
 *
 * Uses pegdown (https://github.com/sirthias/pegdown) for parsing markdown.
 *
 * @author Karl Tauber
 */
public class MarkdownEditorPane
{
	private final StyleClassedTextArea textArea;
	private final ReadOnlyObjectWrapper<RootNode> markdownAST = new ReadOnlyObjectWrapper<>();
	private PegDownProcessor pegDownProcessor;

	public MarkdownEditorPane() {
		textArea = new StyleClassedTextArea();
		textArea.setWrapText(true);
		textArea.getStyleClass().add("markdown-editor");
		textArea.getStylesheets().add("org/markdownwriterfx/editor/MarkdownEditor.css");

		textArea.textProperty().addListener((observable, oldText, newText) -> {
			RootNode astRoot = parseMarkdown(newText);
			applyHighlighting(astRoot);
			markdownAST.set(astRoot);
		});
	}

	public Node getNode() {
		return textArea;
	}

	// markdown property
	public String getMarkdown() { return textArea.getText(); }
	public void setMarkdown(String markdown) { textArea.replaceText(markdown); }
	public ObservableValue<String> markdownProperty() { return textArea.textProperty(); }

	// markdownAST property
	public RootNode getMarkdownAST() { return markdownAST.get(); }
	public ReadOnlyObjectProperty<RootNode> markdownASTProperty() { return markdownAST.getReadOnlyProperty(); }

	private RootNode parseMarkdown(String text) {
		if(pegDownProcessor == null)
			pegDownProcessor = new PegDownProcessor(Extensions.ALL);
		return pegDownProcessor.parseMarkdown(text.toCharArray());
	}

	private void applyHighlighting(RootNode astRoot) {
		MarkdownSyntaxHighlighter.highlight(textArea, astRoot);
	}
}
