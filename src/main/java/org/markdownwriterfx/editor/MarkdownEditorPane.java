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

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.editor.FindReplacePane.HitsChangeListener;
import org.markdownwriterfx.editor.MarkdownSyntaxHighlighter.ExtraStyledRanges;
import org.markdownwriterfx.options.MarkdownExtensions;
import org.markdownwriterfx.options.Options;

/**
 * Markdown editor pane.
 *
 * Uses flexmark-java (https://github.com/vsch/flexmark-java) for parsing markdown.
 *
 * @author Karl Tauber
 */
public class MarkdownEditorPane
{
	private final BorderPane borderPane;
	private final StyleClassedTextArea textArea;
	private final ParagraphOverlayGraphicFactory overlayGraphicFactory;
	private LineNumberGutterFactory lineNumberGutterFactory;
	private WhitespaceOverlayFactory whitespaceOverlayFactory;
	private final SmartEdit smartEdit;

	private final FindReplacePane findReplacePane;
	private final HitsChangeListener findHitsChangeListener;
	private Parser parser;
	private final InvalidationListener optionsListener;
	private String lineSeparator = getLineSeparatorOrDefault();

	public MarkdownEditorPane() {
		textArea = new StyleClassedTextArea(false);
		textArea.setWrapText(true);
		textArea.getStyleClass().add("markdown-editor");
		textArea.getStylesheets().add("org/markdownwriterfx/editor/MarkdownEditor.css");

		textArea.textProperty().addListener((observable, oldText, newText) -> {
			textChanged(newText);
		});

		smartEdit = new SmartEdit(this, textArea);

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(ENTER),					smartEdit::enterPressed),
			consume(keyPressed(D, SHORTCUT_DOWN),		smartEdit::deleteLine),
			consume(keyPressed(PLUS, SHORTCUT_DOWN),	this::increaseFontSize),
			consume(keyPressed(MINUS, SHORTCUT_DOWN),	this::decreaseFontSize),
			consume(keyPressed(DIGIT0, SHORTCUT_DOWN),	this::resetFontSize),
			consume(keyPressed(W, ALT_DOWN),			this::showWhitespace)
		));

		// add listener to update 'scrollY' property
		ChangeListener<Double> scrollYListener = (observable, oldValue, newValue) -> {
			double value = textArea.estimatedScrollYProperty().getValue().doubleValue();
			double maxValue = textArea.totalHeightEstimateProperty().getOrElse(0.).doubleValue() - textArea.getHeight();
			scrollY.set((maxValue > 0) ? Math.min(Math.max(value / maxValue, 0), 1) : 0);
		};
		textArea.estimatedScrollYProperty().addListener(scrollYListener);
		textArea.totalHeightEstimateProperty().addListener(scrollYListener);

		// create scroll pane
		VirtualizedScrollPane<StyleClassedTextArea> scrollPane = new VirtualizedScrollPane<StyleClassedTextArea>(textArea);

		// create border pane
		borderPane = new BorderPane(scrollPane);

		overlayGraphicFactory = new ParagraphOverlayGraphicFactory(textArea);
		textArea.setParagraphGraphicFactory(overlayGraphicFactory);
		updateFont();
		updateShowLineNo();
		updateShowWhitespace();

		// find/replace
		findReplacePane = new FindReplacePane(textArea);
		findHitsChangeListener = this::findHitsChanged;
		findReplacePane.addListener(findHitsChangeListener);
		findReplacePane.visibleProperty().addListener((ov, oldVisible, newVisible) -> {
			if (!newVisible)
				borderPane.setBottom(null);
		});

		// listen to option changes
		optionsListener = e -> {
			if (textArea.getScene() == null)
				return; // editor closed but not yet GCed

			if (e == Options.fontFamilyProperty() || e == Options.fontSizeProperty())
				updateFont();
			else if (e == Options.showLineNoProperty())
				updateShowLineNo();
			else if (e == Options.showWhitespaceProperty())
				updateShowWhitespace();
			else if (e == Options.markdownRendererProperty() || e == Options.markdownExtensionsProperty()) {
				// re-process markdown if markdown extensions option changes
				parser = null;
				textChanged(textArea.getText());
			}
		};
		WeakInvalidationListener weakOptionsListener = new WeakInvalidationListener(optionsListener);
		Options.fontFamilyProperty().addListener(weakOptionsListener);
		Options.fontSizeProperty().addListener(weakOptionsListener);
		Options.markdownRendererProperty().addListener(weakOptionsListener);
		Options.markdownExtensionsProperty().addListener(weakOptionsListener);
		Options.showLineNoProperty().addListener(weakOptionsListener);
		Options.showWhitespaceProperty().addListener(weakOptionsListener);
	}

	private void updateFont() {
		textArea.setStyle("-fx-font-family: '" + Options.getFontFamily()
				+ "'; -fx-font-size: " + Options.getFontSize() );
	}

	public javafx.scene.Node getNode() {
		return borderPane;
	}

	public UndoManager getUndoManager() {
		return textArea.getUndoManager();
	}

	public SmartEdit getSmartEdit() {
		return smartEdit;
	}

	public void requestFocus() {
		Platform.runLater(() -> textArea.requestFocus());
	}

	private String getLineSeparatorOrDefault() {
		String lineSeparator = Options.getLineSeparator();
		return (lineSeparator != null) ? lineSeparator : System.getProperty( "line.separator", "\n" );
	}

	private String determineLineSeparator(String str) {
		int strLength = str.length();
		for (int i = 0; i < strLength; i++) {
			char ch = str.charAt(i);
			if (ch == '\n')
				return (i > 0 && str.charAt(i - 1) == '\r') ? "\r\n" : "\n";
		}
		return getLineSeparatorOrDefault();
	}

	// 'markdown' property
	public String getMarkdown() {
		String markdown = textArea.getText();
		if (!lineSeparator.equals("\n"))
			markdown = markdown.replace("\n", lineSeparator);
		return markdown;
	}
	public void setMarkdown(String markdown) {
		// remember old selection range and scrollY
		IndexRange oldSelection = textArea.getSelection();
		double oldScrollY = textArea.getEstimatedScrollY();

		// replace text
		lineSeparator = determineLineSeparator(markdown);
		textArea.replaceText(markdown);

		// restore old selection range and scrollY
		textArea.selectRange(oldSelection.getStart(), oldSelection.getEnd());
		Platform.runLater(() -> {
			textArea.setEstimatedScrollY(oldScrollY);
		});
	}
	public ObservableValue<String> markdownProperty() { return textArea.textProperty(); }

	// 'markdownText' property
	private final ReadOnlyStringWrapper markdownText = new ReadOnlyStringWrapper();
	public String getMarkdownText() { return markdownText.get(); }
	public ReadOnlyStringProperty markdownTextProperty() { return markdownText.getReadOnlyProperty(); }

	// 'markdownAST' property
	private final ReadOnlyObjectWrapper<Node> markdownAST = new ReadOnlyObjectWrapper<>();
	public Node getMarkdownAST() { return markdownAST.get(); }
	public ReadOnlyObjectProperty<Node> markdownASTProperty() { return markdownAST.getReadOnlyProperty(); }

	// 'scrollY' property
	private final ReadOnlyDoubleWrapper scrollY = new ReadOnlyDoubleWrapper();
	public double getScrollY() { return scrollY.get(); }
	public ReadOnlyDoubleProperty scrollYProperty() { return scrollY.getReadOnlyProperty(); }

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	public Path getPath() { return path.get(); }
	public void setPath(Path path) { this.path.set(path); }
	public ObjectProperty<Path> pathProperty() { return path; }

	Path getParentPath() {
		Path path = getPath();
		return (path != null) ? path.getParent() : null;
	}

	private void textChanged(String newText) {
		if (borderPane.getBottom() != null) {
			findReplacePane.removeListener(findHitsChangeListener);
			findReplacePane.textChanged();
			findReplacePane.addListener(findHitsChangeListener);
		}

		Node astRoot = parseMarkdown(newText);
		applyHighlighting(astRoot);

		markdownText.set(newText);
		markdownAST.set(astRoot);
	}

	private void findHitsChanged() {
		applyHighlighting(markdownAST.get());
	}

	private Node parseMarkdown(String text) {
		if (parser == null) {
			parser = Parser.builder()
				.extensions(MarkdownExtensions.getFlexmarkExtensions(Options.getMarkdownRenderer()))
				.build();
		}
		return parser.parse(text);
	}

	private void applyHighlighting(Node astRoot) {
		List<ExtraStyledRanges> extraStyledRanges = findReplacePane.hasHits()
			? Arrays.asList(
				new ExtraStyledRanges("hit", findReplacePane.getHits()),
				new ExtraStyledRanges("hit-active", Arrays.asList(findReplacePane.getActiveHit())))
			: null;

		MarkdownSyntaxHighlighter.highlight(textArea, astRoot, extraStyledRanges);
	}

	private void increaseFontSize(KeyEvent e) {
		Options.setFontSize(Options.getFontSize() + 1);
	}

	private void decreaseFontSize(KeyEvent e) {
		Options.setFontSize(Options.getFontSize() - 1);
	}

	private void resetFontSize(KeyEvent e) {
		Options.setFontSize(Options.DEF_FONT_SIZE);
	}

	private void showWhitespace(KeyEvent e) {
		Options.setShowWhitespace(!Options.isShowWhitespace());
	}

	private void updateShowLineNo() {
		boolean showLineNo = Options.isShowLineNo();
		if (showLineNo && lineNumberGutterFactory == null) {
			lineNumberGutterFactory = new LineNumberGutterFactory(textArea);
			overlayGraphicFactory.addGutterFactory(lineNumberGutterFactory);
		} else if (!showLineNo && lineNumberGutterFactory != null) {
			overlayGraphicFactory.removeGutterFactory(lineNumberGutterFactory);
			lineNumberGutterFactory = null;
		}
	}

	private void updateShowWhitespace() {
		boolean showWhitespace = Options.isShowWhitespace();
		if (showWhitespace && whitespaceOverlayFactory == null) {
			whitespaceOverlayFactory = new WhitespaceOverlayFactory();
			overlayGraphicFactory.addOverlayFactory(whitespaceOverlayFactory);
		} else if (!showWhitespace && whitespaceOverlayFactory != null) {
			overlayGraphicFactory.removeOverlayFactory(whitespaceOverlayFactory);
			whitespaceOverlayFactory = null;
		}
	}

	public void undo() {
		textArea.getUndoManager().undo();
	}

	public void redo() {
		textArea.getUndoManager().redo();
	}

	public void find(boolean replace) {
		if (borderPane.getBottom() == null)
			borderPane.setBottom(findReplacePane.getNode());

		findReplacePane.show(replace);
	}
}
