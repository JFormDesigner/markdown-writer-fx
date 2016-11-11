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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.dialogs.ImageDialog;
import org.markdownwriterfx.dialogs.LinkDialog;
import org.markdownwriterfx.options.MarkdownExtensions;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.util.Utils;

/**
 * Markdown editor pane.
 *
 * Uses flexmark-java (https://github.com/vsch/flexmark-java) for parsing markdown.
 *
 * @author Karl Tauber
 */
public class MarkdownEditorPane
{
	private static final Pattern AUTO_INDENT_PATTERN = Pattern.compile(
			"(\\s*[*+-]\\s+|\\s*[0-9]+\\.\\s+|\\s+)(.*)");

	private final VirtualizedScrollPane<StyleClassedTextArea> scrollPane;
	private final StyleClassedTextArea textArea;
	private final ParagraphOverlayGraphicFactory overlayGraphicFactory;
	private WhitespaceOverlayFactory whitespaceOverlayFactory;
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

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(ENTER),				this::enterPressed),
			consume(keyPressed(D, SHORTCUT_DOWN),	this::deleteLine),
			consume(keyPressed(W, ALT_DOWN),		this::showWhitespace)
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
		scrollPane = new VirtualizedScrollPane<StyleClassedTextArea>(textArea);

		overlayGraphicFactory = new ParagraphOverlayGraphicFactory(textArea);
		textArea.setParagraphGraphicFactory(overlayGraphicFactory);
		updateFont();
		updateShowWhitespace();

		// listen to option changes
		optionsListener = e -> {
			if (textArea.getScene() == null)
				return; // editor closed but not yet GCed

			if (e == Options.fontFamilyProperty() || e == Options.fontSizeProperty())
				updateFont();
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
		Options.showWhitespaceProperty().addListener(weakOptionsListener);
	}

	private void updateFont() {
		textArea.setStyle("-fx-font-family: '" + Options.getFontFamily()
				+ "'; -fx-font-size: " + Options.getFontSize() );
	}

	public javafx.scene.Node getNode() {
		return scrollPane;
	}

	public UndoManager getUndoManager() {
		return textArea.getUndoManager();
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
		lineSeparator = determineLineSeparator(markdown);
		textArea.replaceText(markdown);
		textArea.selectRange(0, 0);
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

	private Path getParentPath() {
		Path path = getPath();
		return (path != null) ? path.getParent() : null;
	}

	private void textChanged(String newText) {
		Node astRoot = parseMarkdown(newText);
		applyHighlighting(astRoot);

		markdownText.set(newText);
		markdownAST.set(astRoot);
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
		MarkdownSyntaxHighlighter.highlight(textArea, astRoot);
	}

	private void enterPressed(KeyEvent e) {
		String currentLine = textArea.getText(textArea.getCurrentParagraph());

		String newText = "\n";
		Matcher matcher = AUTO_INDENT_PATTERN.matcher(currentLine);
		if (matcher.matches()) {
			if (!matcher.group(2).isEmpty()) {
				// indent new line with same whitespace characters and list markers as current line
				newText = newText.concat(matcher.group(1));
			} else {
				// current line contains only whitespace characters and list markers
				// --> empty current line
				int caretPosition = textArea.getCaretPosition();
				textArea.selectRange(caretPosition - currentLine.length(), caretPosition);
			}
		}
		textArea.replaceSelection(newText);
	}

	private void deleteLine(KeyEvent e) {
		int start = textArea.getCaretPosition() - textArea.getCaretColumn();
		int end = start + textArea.getParagraph(textArea.getCurrentParagraph()).length() + 1;
		textArea.deleteText(start, end);
	}

	private void showWhitespace(KeyEvent e) {
		Options.setShowWhitespace(!Options.isShowWhitespace());
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

	public void surroundSelection(String leading, String trailing) {
		surroundSelection(leading, trailing, null);
	}

	public void surroundSelection(String leading, String trailing, String hint) {
		// Note: not using textArea.insertText() to insert leading and trailing
		//       because this would add two changes to undo history

		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();

		String selectedText = textArea.getSelectedText();

		// remove leading and trailing whitespaces from selected text
		String trimmedSelectedText = selectedText.trim();
		if (trimmedSelectedText.length() < selectedText.length()) {
			start += selectedText.indexOf(trimmedSelectedText);
			end = start + trimmedSelectedText.length();
		}

		// remove leading whitespaces from leading text if selection starts at zero
		if (start == 0)
			leading = Utils.ltrim(leading);

		// remove trailing whitespaces from trailing text if selection ends at text end
		if (end == textArea.getLength())
			trailing = Utils.rtrim(trailing);

		// remove leading line separators from leading text
		// if there are line separators before the selected text
		if (leading.startsWith("\n")) {
			for (int i = start - 1; i >= 0 && leading.startsWith("\n"); i--) {
				if (!"\n".equals(textArea.getText(i, i + 1)))
					break;
				leading = leading.substring(1);
			}
		}

		// remove trailing line separators from trailing or leading text
		// if there are line separators after the selected text
		boolean trailingIsEmpty = trailing.isEmpty();
		String str = trailingIsEmpty ? leading : trailing;
		if (str.endsWith("\n")) {
			for (int i = end; i < textArea.getLength() && str.endsWith("\n"); i++) {
				if (!"\n".equals(textArea.getText(i, i + 1)))
					break;
				str = str.substring(0, str.length() - 1);
			}
			if (trailingIsEmpty)
				leading = str;
			else
				trailing = str;
		}

		int selStart = start + leading.length();
		int selEnd = end + leading.length();

		// insert hint text if selection is empty
		if (hint != null && trimmedSelectedText.isEmpty()) {
			trimmedSelectedText = hint;
			selEnd = selStart + hint.length();
		}

		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		// replace text and update selection
		textArea.replaceText(start, end, leading + trimmedSelectedText + trailing);
		textArea.selectRange(selStart, selEnd);
	}

	public void insertLink() {
		LinkDialog dialog = new LinkDialog(getNode().getScene().getWindow(), getParentPath());
		dialog.showAndWait().ifPresent(result -> {
			textArea.replaceSelection(result);
		});
	}

	public void insertImage() {
		ImageDialog dialog = new ImageDialog(getNode().getScene().getWindow(), getParentPath());
		dialog.showAndWait().ifPresent(result -> {
			textArea.replaceSelection(result);
		});
	}
}
