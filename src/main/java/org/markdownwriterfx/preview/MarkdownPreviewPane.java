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
import org.markdownwriterfx.Messages;
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
	private final TabPane tabPane = new TabPane();
	private final WebViewPreview webViewPreview = new WebViewPreview();
	private final HtmlSourcePreview htmlSourcePreview = new HtmlSourcePreview();
	private final ASTPreview astPreview = new ASTPreview();

	interface Preview {
		void update(RootNode astRoot, Path path);
		void scrollY(double value);
	}

	public MarkdownPreviewPane() {
		tabPane.setSide(Side.BOTTOM);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		Tab webViewTab = new Tab(Messages.get("MarkdownPreviewPane.webViewTab"), webViewPreview.getNode());
		webViewTab.setUserData(webViewPreview);
		tabPane.getTabs().add(webViewTab);

		Tab htmlSourceTab = new Tab(Messages.get("MarkdownPreviewPane.htmlSourceTab"), htmlSourcePreview.getNode());
		htmlSourceTab.setUserData(htmlSourcePreview);
		tabPane.getTabs().add(htmlSourceTab);

		Tab astTab = new Tab(Messages.get("MarkdownPreviewPane.astTab"), astPreview.getNode());
		astTab.setUserData(astPreview);
		tabPane.getTabs().add(astTab);

		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
			update();
			scrollY();
		});

		path.addListener((observable, oldValue, newValue) -> {
			update();
		});

		markdownAST.addListener((observable, oldValue, newValue) -> {
			update();
		});

		scrollY.addListener((observable, oldValue, newValue) -> {
			scrollY();
		});
	}

	public Node getNode() {
		return tabPane;
	}

	private Preview getActivePreview() {
		return (Preview) tabPane.getSelectionModel().getSelectedItem().getUserData();
	}

	private void update() {
		getActivePreview().update(getMarkdownAST(), getPath());
	}

	private boolean scrollYrunLaterPending;
	private void scrollY() {
		// avoid too many (and useless) runLater() invocations
		if (scrollYrunLaterPending)
			return;
		scrollYrunLaterPending = true;

		Platform.runLater(() -> {
			scrollYrunLaterPending = false;
			getActivePreview().scrollY(getScrollY());
		});
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public Path getPath() { return path.get(); }
	public void setPath(Path path) { this.path.set(path); }
	public ObjectProperty<Path> pathProperty() { return path; }

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
