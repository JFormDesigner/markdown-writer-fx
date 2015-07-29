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

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import org.pegdown.ast.RootNode;

/**
 * Markdown preview pane.
 *
 * Uses pegdown AST.
 *
 * @author Karl Tauber
 */
public class MarkdownPreviewPane
{
	private final WebViewPreview webViewPreview = new WebViewPreview();
	private final HtmlSourcePreview htmlSourcePreview = new HtmlSourcePreview();
	private final ASTPreview astPreview = new ASTPreview();
	private TabPane tabPane;

	public MarkdownPreviewPane() {
		markdownAST.addListener((observable, oldValue, newValue) -> {
			webViewPreview.update(newValue);
			htmlSourcePreview.update(newValue);
			astPreview.update(newValue);
		});

		scrollY.addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				webViewPreview.scrollY(newValue.doubleValue());
				htmlSourcePreview.scrollY(newValue.doubleValue());
				astPreview.scrollY(newValue.doubleValue());
			});
		});
	}

	public Node getNode() {
		if(tabPane == null) {
			tabPane = new TabPane();
			tabPane.setSide(Side.BOTTOM);
			tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

			Tab webViewTab = new Tab("Preview", webViewPreview.getNode());
			tabPane.getTabs().add(webViewTab);

			Tab htmlSourceTab = new Tab("HTML Source", htmlSourcePreview.getNode());
			tabPane.getTabs().add(htmlSourceTab);

			Tab astTab = new Tab("Markdown AST", astPreview.getNode());
			tabPane.getTabs().add(astTab);
		}
		return tabPane;
	}

	// 'markdownAST' property
	private final ObjectProperty<RootNode> markdownAST = new SimpleObjectProperty<RootNode>();
	public RootNode getMarkdownAST() { return markdownAST.get(); }
	public void setMarkdownAST(RootNode astRoot) { markdownAST.set(astRoot); }
	public ObjectProperty<RootNode> markdownASTProperty() { return markdownAST; }

	// 'scrollY' property
	private final DoubleProperty scrollY = new SimpleDoubleProperty();
	public double getScrollY() { return scrollY.get(); }
	public void setScrollY(double value) { scrollY.set(value); }
	public DoubleProperty scrollYProperty() { return scrollY; }
}
