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
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.BorderPane;
import org.markdownwriterfx.options.Options.RendererType;
import com.vladsch.flexmark.ast.Node;

/**
 * Markdown preview pane.
 *
 * @author Karl Tauber
 */
public class MarkdownPreviewPane
{
	public enum Type { None, Web, Source, Ast };

	private final BorderPane pane = new BorderPane();
	private final WebViewPreview webViewPreview = new WebViewPreview();
	private final HtmlSourcePreview htmlSourcePreview = new HtmlSourcePreview();
	private final ASTPreview astPreview = new ASTPreview();

	private RendererType activeRendererType;
	private Renderer activeRenderer;
	private Preview activePreview;

	interface Renderer {
		void update(String markdownText, Node astRoot);
		String getHtml();
		String getAST();
	}

	interface Preview {
		javafx.scene.Node getNode();
		void update(Renderer fenderer, Path path);
		void scrollY(double value);
	}

	public MarkdownPreviewPane() {
		path.addListener((observable, oldValue, newValue) -> update() );
		markdownText.addListener((observable, oldValue, newValue) -> update() );
		markdownAST.addListener((observable, oldValue, newValue) -> update() );

		scrollY.addListener((observable, oldValue, newValue) -> {
			scrollY();
		});
	}

	public javafx.scene.Node getNode() {
		return pane;
	}

	public void setRendererType(RendererType rendererType) {
		if (rendererType == null)
			rendererType = RendererType.CommonMark;

		if (activeRendererType == rendererType)
			return;
		activeRendererType = rendererType;
		activePreview = null;

		switch (rendererType) {
			case CommonMark:	activeRenderer = new CommonmarkPreviewRenderer(); break;
			case FlexMark:		activeRenderer = new FlexmarkPreviewRenderer(); break;
		}
	}

	public void setType(Type type) {
		Preview preview;
		switch (type) {
			case Web:		preview = webViewPreview; break;
			case Source:	preview = htmlSourcePreview; break;
			case Ast:		preview = astPreview; break;
			default:		preview = null; break;
		}
		if (activePreview == preview)
			return;

		activePreview = preview;
		pane.setCenter((preview != null) ? preview.getNode() : null);

		update();
		scrollY();
	}

	private boolean updateRunLaterPending;
	private void update() {
		if (activePreview == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updateRunLaterPending)
			return;
		updateRunLaterPending = true;

		Platform.runLater(() -> {
			updateRunLaterPending = false;

			activeRenderer.update(getMarkdownText(), getMarkdownAST());
			activePreview.update(activeRenderer, getPath());
		});
	}

	private boolean scrollYrunLaterPending;
	private void scrollY() {
		if (activePreview == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (scrollYrunLaterPending)
			return;
		scrollYrunLaterPending = true;

		Platform.runLater(() -> {
			scrollYrunLaterPending = false;
			activePreview.scrollY(getScrollY());
		});
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public Path getPath() { return path.get(); }
	public void setPath(Path path) { this.path.set(path); }
	public ObjectProperty<Path> pathProperty() { return path; }

	// 'markdownText' property
	private final SimpleStringProperty markdownText = new SimpleStringProperty();
	public String getMarkdownText() { return markdownText.get(); }
	public void getMarkdownText(String text) { markdownText.set(text); }
	public SimpleStringProperty markdownTextProperty() { return markdownText; }

	// 'markdownAST' property
	private final ObjectProperty<Node> markdownAST = new SimpleObjectProperty<Node>();
	public Node getMarkdownAST() { return markdownAST.get(); }
	public void setMarkdownAST(Node astRoot) { markdownAST.set(astRoot); }
	public ObjectProperty<Node> markdownASTProperty() { return markdownAST; }

	// 'scrollY' property
	private final DoubleProperty scrollY = new SimpleDoubleProperty();
	public double getScrollY() { return scrollY.get(); }
	public void setScrollY(double value) { scrollY.set(value); }
	public DoubleProperty scrollYProperty() { return scrollY; }
}
