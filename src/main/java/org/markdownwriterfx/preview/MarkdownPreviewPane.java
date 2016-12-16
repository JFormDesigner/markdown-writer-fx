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
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.BorderPane;
import org.markdownwriterfx.options.Options.RendererType;
import org.markdownwriterfx.util.Range;
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
	private final WebViewPreview webViewPreview = new WebViewPreview(this);
	private final HtmlSourcePreview htmlSourcePreview = new HtmlSourcePreview();
	private final ASTPreview astPreview = new ASTPreview();

	private RendererType activeRendererType;
	private Renderer activeRenderer;
	private Preview activePreview;

	interface Renderer {
		void update(String markdownText, Node astRoot);
		String getHtml(boolean source);
		String getAST();
		List<Range> findSequences(int startOffset, int endOffset);
	}

	interface Preview {
		javafx.scene.Node getNode();
		void update(Renderer fenderer, Path path);
		void scrollY(double value);
		void selectionChanged(IndexRange range);
	}

	public MarkdownPreviewPane() {
		pane.getStyleClass().add("preview-pane");

		path.addListener((observable, oldValue, newValue) -> update() );
		markdownText.addListener((observable, oldValue, newValue) -> update() );
		markdownAST.addListener((observable, oldValue, newValue) -> update() );
		scrollY.addListener((observable, oldValue, newValue) -> scrollY());
		selection.addListener((observable, oldValue, newValue) -> selectionChanged());
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

			activeRenderer.update(markdownText.get(), markdownAST.get());
			activePreview.update(activeRenderer, path.get());
			activePreview.selectionChanged(selection.get());
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
			activePreview.scrollY(scrollY.get());
		});
	}

	private void selectionChanged() {
		if (activePreview == null)
			return;

		activePreview.selectionChanged(selection.get());
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public ObjectProperty<Path> pathProperty() { return path; }

	// 'markdownText' property
	private final SimpleStringProperty markdownText = new SimpleStringProperty();
	public SimpleStringProperty markdownTextProperty() { return markdownText; }

	// 'markdownAST' property
	private final ObjectProperty<Node> markdownAST = new SimpleObjectProperty<>();
	public ObjectProperty<Node> markdownASTProperty() { return markdownAST; }

	// 'scrollY' property
	private final DoubleProperty scrollY = new SimpleDoubleProperty();
	public DoubleProperty scrollYProperty() { return scrollY; }

	// 'selection' property
	private final ObjectProperty<IndexRange> selection = new SimpleObjectProperty<>();
	public ObjectProperty<IndexRange> selectionProperty() { return selection; }
}
